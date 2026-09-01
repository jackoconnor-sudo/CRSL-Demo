#!/usr/bin/env python3
"""The security gate.

This stands in for a live Sonar quality gate and an X-ray policy. It is deterministic and
it reads the working tree, not a server, so it can be run offline and it turns green when
the code is actually fixed rather than when a report is edited.

Each condition names the scanner findings it accounts for, so a failure can be handed to
one remediation session without further triage.

    python3 security/gate_check.py            # human readable, exit 1 when failing
    python3 security/gate_check.py --json     # machine readable report on stdout

Writes security/gate-report.json on every run.
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
POM = os.path.join(ROOT, "pom.xml")
DOCKERFILE = os.path.join(ROOT, "ratings-service", "Dockerfile")
SRC = os.path.join(ROOT, "ratings-service", "src", "main")
MAVEN_NS = {"m": "http://maven.apache.org/POM/4.0.0"}

# Distributions whose security support has ended. A base image on any of these fails the
# gate regardless of what the package scanner reports.
EOL_BASE_IMAGES = {
    "openjdk:8u282-jdk-buster": "Debian 10 (buster), out of LTS support since June 2024",
    "openjdk:8-jdk-buster": "Debian 10 (buster), out of LTS support since June 2024",
    "openjdk:8-jre-buster": "Debian 10 (buster), out of LTS support since June 2024",
    "openjdk:8-jdk-stretch": "Debian 9 (stretch), out of support since June 2022",
    "debian:buster": "out of LTS support since June 2024",
    "debian:10": "out of LTS support since June 2024",
}

# property name -> (lowest version that clears every X-ray violation against it, findings)
DEPENDENCY_FLOOR = {
    "log4j2.version": ("2.17.1", 5),
    "jackson-bom.version": ("2.12.6", 8),
    "commons-collections.version": ("3.2.2", 2),
    "commons-fileupload.version": ("1.5", 2),
    "guava.version": ("32.0.0", 3),
    "httpclient.version": ("4.5.13", 1),
}
SPRING_BOOT_FLOOR = ("2.7.18", 11)


def read(path):
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read()


def source(*parts):
    return read(os.path.join(SRC, *parts))


def version_tuple(raw):
    cleaned = re.split(r"[-.]RELEASE", raw)[0]
    cleaned = re.sub(r"-(jre|android|SNAPSHOT|rc\d*|M\d+)$", "", cleaned, flags=re.IGNORECASE)
    parts = []
    for chunk in re.split(r"[.\-]", cleaned):
        parts.append(int(chunk) if chunk.isdigit() else 0)
    while len(parts) < 4:
        parts.append(0)
    return tuple(parts[:4])


def pom_properties():
    tree = ET.parse(POM)
    root = tree.getroot()
    properties = {}
    node = root.find("m:properties", MAVEN_NS)
    if node is not None:
        for child in node:
            properties[child.tag.split("}")[-1]] = (child.text or "").strip()
    parent = root.find("m:parent", MAVEN_NS)
    if parent is not None:
        properties["__parent_version__"] = parent.find("m:version", MAVEN_NS).text.strip()
    return properties


def base_image():
    for line in read(DOCKERFILE).splitlines():
        if line.strip().upper().startswith("FROM "):
            return line.split()[1]
    return ""


def conditions():
    """Every gate condition, in the order the report prints them."""
    results = []

    def add(key, title, failing, detail, findings, owner):
        results.append({
            "id": key,
            "title": title,
            "status": "fail" if failing else "pass",
            "detail": detail,
            "findings_accounted_for": findings,
            "remediation_owner": owner,
        })

    # 1. Base image.
    image = base_image()
    eol_reason = EOL_BASE_IMAGES.get(image)
    add("base-image-eol",
        "Container base image is in support",
        eol_reason is not None,
        "ratings-service/Dockerfile uses {0}{1}".format(image, ": " + eol_reason if eol_reason else ""),
        67 if eol_reason else 0,
        "platform")

    # 2. Container user.
    dockerfile = read(DOCKERFILE)
    runs_as_root = not re.search(r"^\s*USER\s+(?!root\s*$)\S+", dockerfile, re.MULTILINE)
    add("container-runs-as-root",
        "Container declares a non-root user",
        runs_as_root,
        "no USER instruction in ratings-service/Dockerfile" if runs_as_root
        else "USER instruction present",
        0,
        "platform")

    # 3. Secrets in the image and in configuration.
    secret_pattern = re.compile(r"ngw_live_[0-9a-f]{32}")
    leaked = []
    for relative in ("ratings-service/Dockerfile",
                     "ratings-service/src/main/resources/application.yml",
                     "ratings-service/src/main/java/com/northgate/ratings/integration/WarehouseClient.java"):
        if secret_pattern.search(read(os.path.join(ROOT, relative))):
            leaked.append(relative)
    add("hardcoded-secrets",
        "No credentials committed to the repository or baked into the image",
        bool(leaked),
        "warehouse API token found in: {0}".format(", ".join(leaked)) if leaked else "clean",
        7 if leaked else 0,
        "application")

    # 4. Dependency floors.
    properties = pom_properties()
    stale = []
    stale_findings = 0
    for name, (floor, count) in DEPENDENCY_FLOOR.items():
        current = properties.get(name)
        if current is None:
            stale.append("{0} is not pinned in pom.xml".format(name))
            stale_findings += count
        elif version_tuple(current) < version_tuple(floor):
            stale.append("{0}={1} (needs >= {2})".format(name, current, floor))
            stale_findings += count
    boot = properties.get("__parent_version__", "0")
    if version_tuple(boot) < version_tuple(SPRING_BOOT_FLOOR[0]):
        stale.append("spring-boot-starter-parent={0} (needs >= {1})".format(boot, SPRING_BOOT_FLOOR[0]))
        stale_findings += SPRING_BOOT_FLOOR[1]
    add("vulnerable-dependencies",
        "No dependency below its fixed version",
        bool(stale),
        "; ".join(stale) if stale else "all pinned versions clear their advisories",
        stale_findings,
        "platform")

    # 5. SQL injection.
    repository = source("java", "com", "northgate", "ratings", "repository", "RatingsRepository.java")
    concatenated_sql = re.findall(r"\"(?:SELECT|UPDATE|DELETE|INSERT)[^\"]*\"\s*\+", repository)
    add("sql-injection",
        "No SQL statement is built from request values by concatenation",
        bool(concatenated_sql),
        "{0} concatenated statements in RatingsRepository".format(len(concatenated_sql))
        if concatenated_sql else "all statements parameterised",
        6 if concatenated_sql else 0,
        "application")

    # 6. Authorization on the admin surface.
    admin_filter = source("java", "com", "northgate", "ratings", "config", "AdminApiFilter.java")
    rejects = bool(re.search(r"(sendError|setStatus)\s*\(\s*(HttpServletResponse\.)?SC_(FORBIDDEN|UNAUTHORIZED)", admin_filter)) \
        or "SC_FORBIDDEN" in admin_filter or "SC_UNAUTHORIZED" in admin_filter
    add("admin-authorization",
        "The admin filter rejects unauthorised callers",
        not rejects,
        "AdminApiFilter evaluates the admin flag and calls chain.doFilter unconditionally"
        if not rejects else "unauthorised callers are rejected",
        2 if not rejects else 0,
        "application")

    # 7. XXE.
    feed = source("java", "com", "northgate", "ratings", "feed", "LegacyFeedParser.java")
    hardened = "disallow-doctype-decl" in feed or "setExpandEntityReferences(false)" in feed
    add("xml-external-entities",
        "XML parsing is hardened against external entities",
        not hardened,
        "DocumentBuilderFactory is used without disallow-doctype-decl" if not hardened
        else "doctype declarations are rejected",
        3 if not hardened else 0,
        "application")

    # 8. Deserialization of the session cookie.
    codec = source("java", "com", "northgate", "ratings", "security", "SessionCookieCodec.java")
    deserialises = "ObjectInputStream" in codec
    add("unsafe-deserialization",
        "No Java deserialization of client supplied data",
        deserialises,
        "SessionCookieCodec reads the session cookie with ObjectInputStream" if deserialises
        else "the session cookie is not deserialised",
        3 if deserialises else 0,
        "application")

    # 9. Command construction.
    export_controller = source("java", "com", "northgate", "ratings", "controller", "ExportController.java")
    shell_from_request = "/bin/sh" in export_controller and "+ format" in export_controller
    add("command-injection",
        "No shell command is assembled from request values",
        shell_from_request,
        "ExportController builds a /bin/sh -c command from the format and desk parameters"
        if shell_from_request else "export invocation does not interpolate request values",
        2 if shell_from_request else 0,
        "application")

    # 10. Export download paths.
    traversable = "new File(exportDir + \"/\" + name)" in export_controller
    add("path-traversal",
        "Export downloads stay inside the export directory",
        traversable,
        "ExportController resolves the name parameter against the export directory without "
        "canonicalising it" if traversable else "download paths are canonicalised and checked",
        3 if traversable else 0,
        "application")

    # 11. Weak cryptography.
    digest = source("java", "com", "northgate", "ratings", "crypto", "LegacyDigest.java")
    weak = [name for name in ("MD5", "DES") if '"{0}'.format(name) in digest]
    add("weak-cryptography",
        "No broken hash or cipher in use",
        bool(weak),
        "LegacyDigest uses {0}".format(" and ".join(weak)) if weak else "clean",
        6 if weak else 0,
        "application")

    return results


def main():
    parser = argparse.ArgumentParser(description="Northgate security gate")
    parser.add_argument("--json", action="store_true", help="print the report as JSON")
    args = parser.parse_args()

    results = conditions()
    failing = [c for c in results if c["status"] == "fail"]
    report = {
        "gate": "northgate-security-gate",
        "repository": "northgate-ratings-platform",
        "result": "fail" if failing else "pass",
        "conditions_total": len(results),
        "conditions_failing": len(failing),
        "findings_accounted_for": sum(c["findings_accounted_for"] for c in failing),
        "conditions": results,
    }

    with open(os.path.join(ROOT, "security", "gate-report.json"), "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
        handle.write("\n")

    if args.json:
        print(json.dumps(report, indent=2))
    else:
        print("northgate security gate: {0}".format(report["result"].upper()))
        print("")
        for condition in results:
            mark = "FAIL" if condition["status"] == "fail" else "pass"
            print("  [{0}] {1}".format(mark, condition["title"]))
            print("         {0}".format(condition["detail"]))
            if condition["status"] == "fail" and condition["findings_accounted_for"]:
                print("         accounts for {0} scanner findings".format(
                    condition["findings_accounted_for"]))
        print("")
        print("{0} of {1} conditions failing, {2} scanner findings attributable to them".format(
            len(failing), len(results), report["findings_accounted_for"]))

    return 1 if failing else 0


if __name__ == "__main__":
    sys.exit(main())
