#!/usr/bin/env python3
"""Regenerates the scanner exports under security/.

The exports are fabricated. They are not the output of a live Fortify, SonarQube, JFrog
X-ray or Prisma run. What is real is the code they point at: every application finding is
anchored to a snippet of source, and the line numbers in the exports are resolved from the
working tree every time this script runs. Move code around, run this, and the references
stay honest.

    python3 security/generate_findings.py

The counts are asserted at the end of the run, so a mistake in the catalogue fails loudly
rather than quietly changing the size of the backlog.
"""

import hashlib
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SECURITY = os.path.join(ROOT, "security")

SCAN_DATE = "2026-02-17T02:14:38Z"
PROJECT = "ratings-service"
PROJECT_VERSION = "1.4.2"
IMAGE = "northgate/ratings-service:1.4.2"

SRC = "ratings-service/src/main/java/com/northgate/ratings"


def line_of(rel_path, anchor, occurrence=1):
    """Return the 1-based line number of the occurrence'th line containing anchor."""
    path = os.path.join(ROOT, rel_path)
    seen = 0
    with open(path, "r", encoding="utf-8") as handle:
        for number, text in enumerate(handle, start=1):
            if anchor in text:
                seen += 1
                if seen == occurrence:
                    return number
    raise SystemExit(
        "anchor not found in {0}: {1!r}\n"
        "The code moved in a way the catalogue did not follow. Update the anchor in "
        "security/generate_findings.py.".format(rel_path, anchor)
    )


def issue_id(*parts):
    return hashlib.sha1("|".join(str(p) for p in parts).encode("utf-8")).hexdigest()[:16].upper()


# ---------------------------------------------------------------------------
# Application findings. (file, anchor, occurrence, category, cwe, severity, abstract)
# ---------------------------------------------------------------------------

REPO = SRC + "/repository/RatingsRepository.java"
RQB = SRC + "/report/ReportQueryBuilder.java"
FEED = SRC + "/feed/LegacyFeedParser.java"
WAREHOUSE = SRC + "/integration/WarehouseClient.java"
DIGEST = SRC + "/crypto/LegacyDigest.java"
SESSION_CTRL = SRC + "/controller/SessionController.java"
SESSION_CODEC = SRC + "/security/SessionCookieCodec.java"
EXPORT_CTRL = SRC + "/controller/ExportController.java"
RATINGS_CTRL = SRC + "/controller/RatingsController.java"
ADMIN_CTRL = SRC + "/controller/AdminController.java"
ADMIN_FILTER = SRC + "/config/AdminApiFilter.java"
NIGHTLY = SRC + "/service/NightlyJobRunner.java"
ERRORS = SRC + "/config/ApiExceptionHandler.java"
APP_YML = "ratings-service/src/main/resources/application.yml"
DOCKERFILE = "ratings-service/Dockerfile"

FORTIFY_ISSUES = [
    # SQL injection, one root cause, six sinks.
    (REPO, "WHERE issuer_id = '\" + issuerId", 1, "SQL Injection", 89, "Critical",
     "The issuerId path variable reaches a statement built by concatenation."),
    (REPO, "issuer_name LIKE '%\" + namePattern", 1, "SQL Injection", 89, "Critical",
     "The q request parameter reaches a statement built by concatenation."),
    (REPO, "\" AND sector = '\" + sector", 1, "SQL Injection", 89, "Critical",
     "The sector request parameter is appended to the search predicate."),
    (REPO, "WHERE grade IN (\" + quoteCsv", 1, "SQL Injection", 89, "Critical",
     "quoteCsv quotes but does not escape; the grades parameter reaches the statement."),
    (REPO, "UPDATE ratings SET grade = '", 1, "SQL Injection", 89, "Critical",
     "grade and outlook from the request body reach an UPDATE built by concatenation."),
    (REPO, "SELECT COUNT(*) FROM ratings WHERE sector = '", 1, "SQL Injection", 89, "High",
     "The sector value reaches a COUNT statement built by concatenation."),

    # The cluster the triage step is expected to decline.
    (RQB, "return \"SELECT \" + dimension.column()", 1, "SQL Injection", 89, "High",
     "A SELECT statement is assembled by concatenation in rollup."),
    (RQB, "return \"SELECT \" + columns + \" FROM ratings WHERE \"", 1, "SQL Injection", 89, "High",
     "A SELECT statement is assembled by concatenation in projection."),
    (RQB, "return \"SELECT \" + column + \", COUNT(*) AS n, MAX", 1, "SQL Injection", 89, "Medium",
     "A SELECT statement is assembled by concatenation in coverage."),

    # XXE.
    (FEED, "DocumentBuilderFactory.newInstance();", 1, "XML External Entity Injection", 611, "Critical",
     "DocumentBuilderFactory is used without disallow-doctype-decl on request supplied XML."),
    (FEED, "Document document = factory.newDocumentBuilder()", 1, "XML External Entity Injection", 611, "Critical",
     "The normalise path parses request supplied XML with an unhardened factory."),
    (FEED, "TransformerFactory.newInstance().newTransformer()", 1, "XML External Entity Injection", 611, "High",
     "TransformerFactory is created without ACCESS_EXTERNAL_DTD restrictions."),

    # Hardcoded credentials.
    (WAREHOUSE, "WAREHOUSE_PASSWORD = ", 1, "Password Management: Hardcoded Password", 798, "Critical",
     "A service account password is compiled into the artifact."),
    (WAREHOUSE, "WAREHOUSE_API_TOKEN = ", 1, "Key Management: Hardcoded Encryption Key", 798, "Critical",
     "A live warehouse API token is compiled into the artifact."),
    (DIGEST, "FIELD_KEY = ", 1, "Key Management: Hardcoded Encryption Key", 321, "Critical",
     "The field encryption key is a string constant."),
    (SESSION_CTRL, "OPS_CONSOLE_PASSWORD_HASH = ", 1, "Password Management: Hardcoded Password", 798, "High",
     "The ops console credential is compared against a constant digest."),
    (APP_YML, "password: R4tings-app-2019", 1, "Password Management: Password in Configuration File", 260, "High",
     "The datasource password is committed to the repository."),
    (APP_YML, "api-token: ", 1, "Password Management: Password in Configuration File", 260, "High",
     "The warehouse API token is committed to the repository."),
    (DOCKERFILE, "NORTHGATE_WAREHOUSE_API_TOKEN=", 1, "Password Management: Password in Configuration File", 798, "High",
     "The warehouse API token is baked into an image layer as an environment variable."),

    # Weak cryptography.
    (DIGEST, "MessageDigest.getInstance(\"MD5\")", 1, "Weak Cryptographic Hash", 328, "High",
     "Passwords are hashed with MD5 and an unsalted single pass."),
    (DIGEST, "MessageDigest.getInstance(\"MD5\")", 2, "Weak Cryptographic Hash", 328, "Medium",
     "Values are fingerprinted with MD5."),
    (SESSION_CTRL, "LegacyDigest.hashPassword(password, user)", 1, "Weak Password Storage", 916, "High",
     "Authentication compares an MD5 digest of the submitted password."),
    (DIGEST, "Cipher.getInstance(\"DES/ECB/PKCS5Padding\")", 1, "Weak Encryption", 327, "High",
     "Field encryption uses DES in ECB mode."),
    (DIGEST, "Cipher.getInstance(\"DES/ECB/PKCS5Padding\")", 2, "Weak Encryption", 327, "High",
     "Field decryption uses DES in ECB mode."),
    (DIGEST, "SecretKeyFactory.getInstance(\"DES\")", 1, "Weak Encryption: Insecure Key Length", 326, "Medium",
     "A 56 bit DES key is derived from a string constant."),

    # Command injection.
    (EXPORT_CTRL, "String command = \"/opt/northgate/bin/export.sh", 1, "Command Injection", 78, "Critical",
     "The format and desk request parameters are concatenated into a shell command."),
    (EXPORT_CTRL, "Runtime.getRuntime().exec(new String[] {\"/bin/sh\", \"-c\", command})", 1, "Command Injection", 78, "Critical",
     "The concatenated command is executed through /bin/sh -c."),

    # Path manipulation.
    (EXPORT_CTRL, "File file = new File(exportDir + \"/\" + name)", 1, "Path Manipulation", 22, "High",
     "The name request parameter is concatenated into a filesystem path."),
    (EXPORT_CTRL, "Files.readAllBytes(file.toPath())", 1, "Path Manipulation", 22, "High",
     "File contents selected by the caller are returned to the caller."),
    (EXPORT_CTRL, "File dir = new File(exportDir + \"/\" + subdir)", 1, "Path Manipulation", 22, "Medium",
     "The subdir request parameter is concatenated into a filesystem path."),
    (EXPORT_CTRL, "\"attachment; filename=\" + name", 1, "Header Manipulation", 113, "Medium",
     "An unvalidated request parameter is written into a response header."),

    # Deserialization.
    (SESSION_CODEC, "new ObjectInputStream(new ByteArrayInputStream(raw))", 1, "Dynamic Code Evaluation: Unsafe Deserialization", 502, "Critical",
     "A cookie supplied by the client is deserialized with no allowlist."),
    (SESSION_CODEC, "return (SessionState) in.readObject()", 1, "Dynamic Code Evaluation: Unsafe Deserialization", 502, "Critical",
     "readObject runs before the cast is checked."),
    (SESSION_CTRL, "codec.decode(cookie)", 1, "Dynamic Code Evaluation: Unsafe Deserialization", 502, "High",
     "The whoami endpoint deserializes the session cookie on an unauthenticated path."),

    # Authorization.
    (ADMIN_FILTER, "chain.doFilter(request, response)", 1, "Missing Authorization Check", 862, "Critical",
     "The filter evaluates the admin condition and then forwards every request regardless."),
    (ADMIN_CTRL, "public Map<String, Object> override(", 1, "Missing Authorization Check", 862, "High",
     "A grade override endpoint relies entirely on the filter for authorization."),

    # Log forging.
    (ADMIN_FILTER, "LOG.info(\"admin check user=\"", 1, "Log Forging", 117, "Medium",
     "The X-Forwarded-User header is written to the log without neutralising newlines."),
    (RATINGS_CTRL, "LOG.info(\"rating lookup issuer=\"", 1, "Log Forging", 117, "Medium",
     "The issuerId and requestedBy values are written to the log unneutralised."),
    (WAREHOUSE, "LOG.warn(\"warehouse lookup failed for \"", 1, "Log Forging", 117, "Low",
     "The issuerId value is written to the log unneutralised."),

    # The second cluster the triage step is expected to decline.
    (NIGHTLY, "Runtime.getRuntime().exec(new String[] {\"/opt/northgate/bin/refresh-cache.sh\"})", 1, "Command Injection", 78, "High",
     "A process is created from a Runtime.exec call."),
    (NIGHTLY, "new File(SNAPSHOT_DIR, UUID.randomUUID().toString()", 1, "Path Manipulation", 22, "Medium",
     "A filesystem path is assembled by concatenation."),
    (NIGHTLY, "LOG.info(\"nightly snapshot starting for dimension \"", 1, "Log Forging", 117, "Low",
     "A value is written to the log without neutralising newlines."),
]

SONAR_ISSUES = [
    (DIGEST, "SESSION_RANDOM = new Random()", 1, "java:S2245", "CRITICAL", "VULNERABILITY",
     "Make sure that using this pseudorandom number generator is safe here.", ["cwe", "owasp-a3"]),
    (DIGEST, "Long.toHexString(SESSION_RANDOM.nextLong())", 1, "java:S2245", "CRITICAL", "VULNERABILITY",
     "Session identifiers are derived from java.util.Random.", ["cwe", "owasp-a3"]),
    (ERRORS, "body.put(\"errorId\", errorId)", 1, "java:S1989", "MAJOR", "VULNERABILITY",
     "Do not return a stack trace in an API response.", ["cwe", "owasp-a3", "error-handling"]),
    (ERRORS, "body.put(\"message\", message)", 1, "java:S4507", "MINOR", "CODE_SMELL",
     "Make sure this debug feature is deactivated before delivering the code in production.",
     ["cwe", "owasp-a5"]),
    (SESSION_CTRL, "Cookie cookie = new Cookie(SessionCookieCodec.COOKIE_NAME", 1, "java:S3330", "MAJOR",
     "VULNERABILITY", "Add the \"HttpOnly\" attribute to this cookie.", ["cwe", "owasp-a3", "privacy"]),
    (APP_YML, "http-only: false", 1, "java:S3330", "MAJOR", "VULNERABILITY",
     "Session cookies are configured without HttpOnly.", ["cwe", "owasp-a3"]),
    (APP_YML, "secure: false", 1, "java:S2092", "MAJOR", "VULNERABILITY",
     "Session cookies are configured without the secure flag.", ["cwe", "owasp-a3"]),
]

# ---------------------------------------------------------------------------
# Dependency findings. Every one of these resolves to a version property in the root
# pom.xml, or to the spring-boot-starter-parent version declared in the same file.
# ---------------------------------------------------------------------------

MAVEN_VULNS = [
    ("org.apache.logging.log4j:log4j-core", "2.14.1", "CVE-2021-44228", "Critical", 10.0, "2.15.0",
     "Log4Shell: JNDI lookup in message parameters allows remote code execution.", "log4j2.version"),
    ("org.apache.logging.log4j:log4j-core", "2.14.1", "CVE-2021-45046", "Critical", 9.0, "2.16.0",
     "Incomplete fix for CVE-2021-44228 in certain non-default configurations.", "log4j2.version"),
    ("org.apache.logging.log4j:log4j-core", "2.14.1", "CVE-2021-45105", "High", 5.9, "2.17.0",
     "Uncontrolled recursion in self-referential lookups causes denial of service.", "log4j2.version"),
    ("org.apache.logging.log4j:log4j-core", "2.14.1", "CVE-2021-44832", "Medium", 6.6, "2.17.1",
     "JDBC Appender allows remote code execution when configuration is attacker controlled.", "log4j2.version"),
    ("org.apache.logging.log4j:log4j-api", "2.14.1", "CVE-2021-45105", "Medium", 5.9, "2.17.0",
     "Bundled with the vulnerable log4j-core release train.", "log4j2.version"),
    ("org.springframework:spring-beans", "5.2.9.RELEASE", "CVE-2022-22965", "Critical", 9.8, "5.3.18",
     "Spring4Shell: data binding on JDK 9+ allows remote code execution.", "spring-boot parent"),
    ("org.springframework:spring-core", "5.2.9.RELEASE", "CVE-2022-22950", "Medium", 5.4, "5.3.17",
     "SpEL expression causes denial of service.", "spring-boot parent"),
    ("org.springframework:spring-core", "5.2.9.RELEASE", "CVE-2021-22118", "High", 7.8, "5.3.7",
     "WebFlux local privilege escalation.", "spring-boot parent"),
    ("org.springframework:spring-web", "5.2.9.RELEASE", "CVE-2016-1000027", "Critical", 9.8, "6.0.0",
     "HttpInvokerServiceExporter deserializes untrusted input.", "spring-boot parent"),
    ("org.springframework:spring-webmvc", "5.2.9.RELEASE", "CVE-2020-5421", "Medium", 6.5, "5.2.9",
     "RFD protection bypass via jsessionid path parameter.", "spring-boot parent"),
    ("org.springframework:spring-expression", "5.2.9.RELEASE", "CVE-2023-20863", "Medium", 5.3, "5.3.27",
     "SpEL expression denial of service.", "spring-boot parent"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2019-14540", "Critical", 9.8, "2.9.10.1",
     "Polymorphic typing gadget in hikari-config allows deserialization attack.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2019-16335", "Critical", 9.8, "2.9.10.1",
     "Polymorphic typing gadget in HikariDataSource.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2019-17267", "High", 8.1, "2.9.10.1",
     "Polymorphic typing gadget in ehcache.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2020-8840", "Critical", 9.8, "2.9.10.3",
     "Polymorphic typing gadget in xbean-reflect.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2020-9546", "Critical", 9.8, "2.9.10.4",
     "Polymorphic typing gadget in shaded-hikari-config.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2020-10650", "Critical", 9.8, "2.9.10.4",
     "Polymorphic typing gadget in ignite-jta.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2020-36518", "High", 7.5, "2.12.6",
     "Deeply nested input causes stack exhaustion.", "jackson-bom.version"),
    ("com.fasterxml.jackson.core:jackson-databind", "2.9.10", "CVE-2021-20190", "High", 8.1, "2.9.10.7",
     "Polymorphic typing gadget in jackson-databind default typing.", "jackson-bom.version"),
    ("commons-collections:commons-collections", "3.2.1", "CVE-2015-6420", "Critical", 9.8, "3.2.2",
     "InvokerTransformer allows remote code execution during deserialization.", "commons-collections.version"),
    ("commons-collections:commons-collections", "3.2.1", "CVE-2017-15708", "Critical", 9.8, "3.2.2",
     "Unsafe deserialization gadget chain reachable through Java serialization.", "commons-collections.version"),
    ("commons-fileupload:commons-fileupload", "1.3.2", "CVE-2016-1000031", "Critical", 9.8, "1.3.3",
     "DiskFileItem allows remote code execution during deserialization.", "commons-fileupload.version"),
    ("commons-fileupload:commons-fileupload", "1.3.2", "CVE-2023-24998", "High", 7.5, "1.5",
     "Unlimited number of request parts causes denial of service.", "commons-fileupload.version"),
    ("com.google.guava:guava", "24.1-jre", "CVE-2018-10237", "Medium", 5.9, "24.1.1-jre",
     "Unbounded memory allocation in AtomicDoubleArray deserialization.", "guava.version"),
    ("com.google.guava:guava", "24.1-jre", "CVE-2020-8908", "Low", 3.3, "30.0-jre",
     "Files.createTempDir creates a world readable directory.", "guava.version"),
    ("com.google.guava:guava", "24.1-jre", "CVE-2023-2976", "High", 7.1, "32.0.0-jre",
     "FileBackedOutputStream writes to a world readable temporary directory.", "guava.version"),
    ("org.apache.httpcomponents:httpclient", "4.5.5", "CVE-2020-13956", "Medium", 5.3, "4.5.13",
     "Misinterpretation of malformed authority component in request URIs.", "httpclient.version"),
    ("org.apache.tomcat.embed:tomcat-embed-core", "9.0.38", "CVE-2020-13943", "Medium", 4.3, "9.0.39",
     "HTTP/2 request mix-up between streams.", "spring-boot parent"),
    ("org.apache.tomcat.embed:tomcat-embed-core", "9.0.38", "CVE-2021-25122", "High", 7.5, "9.0.43",
     "Request mix-up when h2c direct connection upgrade is used.", "spring-boot parent"),
    ("org.apache.tomcat.embed:tomcat-embed-core", "9.0.38", "CVE-2022-42252", "High", 7.5, "9.0.68",
     "Request smuggling when rejectIllegalHeader is false.", "spring-boot parent"),
    ("org.yaml:snakeyaml", "1.26", "CVE-2022-1471", "Critical", 9.8, "2.0",
     "SnakeYaml constructor allows remote code execution on untrusted input.", "spring-boot parent"),
    ("com.h2database:h2", "1.4.200", "CVE-2021-42392", "Critical", 9.8, "2.0.206",
     "JNDI in the H2 console allows remote code execution.", "spring-boot parent"),
]

# Debian buster packages inherited from openjdk:8u282-jdk-buster. One Dockerfile line closes all of
# these, which is the point of the group.
DEBIAN_VULNS = [
    ("openssl", "1.1.1d-0+deb10u3", "CVE-2022-0778", "High", 7.5),
    ("openssl", "1.1.1d-0+deb10u3", "CVE-2021-3711", "Critical", 9.8),
    ("openssl", "1.1.1d-0+deb10u3", "CVE-2021-3712", "High", 7.4),
    ("libssl1.1", "1.1.1d-0+deb10u3", "CVE-2022-1292", "Critical", 9.8),
    ("libssl1.1", "1.1.1d-0+deb10u3", "CVE-2023-0286", "High", 7.4),
    ("libc6", "2.28-10", "CVE-2021-33574", "Critical", 9.8),
    ("libc6", "2.28-10", "CVE-2021-35942", "High", 9.1),
    ("libc6", "2.28-10", "CVE-2022-23219", "Critical", 9.8),
    ("libc6", "2.28-10", "CVE-2023-4911", "High", 7.8),
    ("libc-bin", "2.28-10", "CVE-2021-3999", "High", 7.8),
    ("glibc", "2.28-10", "CVE-2020-1751", "High", 7.0),
    ("zlib1g", "1:1.2.11.dfsg-1", "CVE-2018-25032", "High", 7.5),
    ("zlib1g", "1:1.2.11.dfsg-1", "CVE-2022-37434", "Critical", 9.8),
    ("bash", "5.0-4", "CVE-2019-18276", "High", 7.8),
    ("bash", "5.0-4", "CVE-2022-3715", "Medium", 5.5),
    ("curl", "7.64.0-4+deb10u1", "CVE-2021-22946", "High", 7.5),
    ("curl", "7.64.0-4+deb10u1", "CVE-2022-32221", "Critical", 9.8),
    ("libcurl4", "7.64.0-4+deb10u1", "CVE-2023-27536", "High", 7.5),
    ("libcurl4", "7.64.0-4+deb10u1", "CVE-2023-38545", "Critical", 9.8),
    ("libxml2", "2.9.4+dfsg1-7+b3", "CVE-2022-40303", "High", 7.5),
    ("libxml2", "2.9.4+dfsg1-7+b3", "CVE-2022-40304", "High", 7.5),
    ("libxml2", "2.9.4+dfsg1-7+b3", "CVE-2021-3517", "High", 8.6),
    ("libxml2", "2.9.4+dfsg1-7+b3", "CVE-2021-3518", "High", 8.8),
    ("perl", "5.28.1-6", "CVE-2020-10543", "High", 8.2),
    ("perl", "5.28.1-6", "CVE-2020-10878", "High", 8.6),
    ("perl-base", "5.28.1-6", "CVE-2020-12723", "High", 7.5),
    ("apt", "1.8.2.1", "CVE-2020-27350", "High", 7.8),
    ("apt", "1.8.2.1", "CVE-2020-3810", "Medium", 6.4),
    ("systemd", "241-7~deb10u4", "CVE-2021-33910", "High", 7.5),
    ("systemd", "241-7~deb10u4", "CVE-2022-3821", "Medium", 5.5),
    ("libsystemd0", "241-7~deb10u4", "CVE-2023-26604", "High", 7.8),
    ("tar", "1.30+dfsg-6", "CVE-2021-20193", "Medium", 5.5),
    ("gzip", "1.9-3", "CVE-2022-1271", "High", 8.8),
    ("e2fsprogs", "1.44.5-1+deb10u3", "CVE-2022-1304", "High", 7.8),
    ("libcom-err2", "1.44.5-1+deb10u3", "CVE-2019-5094", "Medium", 5.5),
    ("krb5-locales", "1.17-3", "CVE-2021-36222", "High", 7.5),
    ("libk5crypto3", "1.17-3", "CVE-2022-42898", "High", 8.8),
    ("libgssapi-krb5-2", "1.17-3", "CVE-2023-36054", "High", 7.5),
    ("libgcrypt20", "1.8.4-5", "CVE-2021-40528", "Medium", 5.9),
    ("libgnutls30", "3.6.7-4+deb10u6", "CVE-2021-20231", "Critical", 9.8),
    ("libgnutls30", "3.6.7-4+deb10u6", "CVE-2021-20232", "Critical", 9.8),
    ("libsqlite3-0", "3.27.2-3", "CVE-2020-13435", "Medium", 5.5),
    ("libexpat1", "2.2.6-2", "CVE-2022-25235", "Critical", 9.8),
    ("libexpat1", "2.2.6-2", "CVE-2022-25236", "Critical", 9.8),
    ("libexpat1", "2.2.6-2", "CVE-2022-40674", "Critical", 9.8),
    ("libncursesw6", "6.1+20181013-2", "CVE-2021-39537", "High", 7.8),
    ("libtinfo6", "6.1+20181013-2", "CVE-2022-29458", "High", 7.1),
    ("libssh2-1", "1.8.0-2.1", "CVE-2019-17498", "High", 8.1),
    ("libnettle6", "3.4.1-1", "CVE-2021-3580", "High", 7.5),
    ("libldap-2.4-2", "2.4.47+dfsg-3+deb10u2", "CVE-2020-36230", "Critical", 9.1),
    ("libldap-2.4-2", "2.4.47+dfsg-3+deb10u2", "CVE-2022-29155", "Critical", 9.8),
    ("libpcre3", "2:8.39-12", "CVE-2020-14155", "Medium", 5.3),
    ("login", "1:4.5-1.1", "CVE-2019-19882", "High", 7.8),
    ("passwd", "1:4.5-1.1", "CVE-2023-4641", "Medium", 5.5),
    ("util-linux", "2.33.1-0.1", "CVE-2021-37600", "Medium", 5.5),
    ("libtasn1-6", "4.13-3", "CVE-2021-46848", "Critical", 9.1),
    ("libidn2-0", "2.0.5-1+deb10u1", "CVE-2019-12290", "High", 8.1),
    ("binutils", "2.31.1-16", "CVE-2021-20197", "Medium", 6.3),
    ("gcc-8-base", "8.3.0-6", "CVE-2018-12886", "High", 8.1),
    ("git", "1:2.20.1-2+deb10u3", "CVE-2022-24765", "High", 7.8),
    ("git", "1:2.20.1-2+deb10u3", "CVE-2023-25652", "High", 7.5),
    ("mercurial", "4.8.2-1", "CVE-2022-24070", "Critical", 9.8),
    ("libfreetype6", "2.9.1-3", "CVE-2020-15999", "High", 8.8),
    ("libpng16-16", "1.6.36-6", "CVE-2019-7317", "High", 8.1),
    ("libtiff5", "4.1.0+git191117-2", "CVE-2022-0561", "High", 7.5),
    ("libglib2.0-0", "2.58.3-2", "CVE-2021-27219", "Critical", 9.8),
    ("openjdk-8-jdk-headless", "8u242-b08-1~deb9u1", "CVE-2022-21449", "High", 7.5),
]

PRISMA_COMPLIANCE = [
    ("41", "Image should be created with a non-root user", "high",
     "The image runs as root. No USER instruction is present in ratings-service/Dockerfile."),
    ("448", "Container images should not contain secrets", "high",
     "Environment variable NORTHGATE_WAREHOUSE_API_TOKEN carries a live looking credential."),
    ("531", "Base image is end of life", "critical",
     "openjdk:8u282-jdk-buster resolves to Debian 10 (buster), which left LTS support in June 2024."),
    ("59", "Image should not expose the JVM debug port or management endpoints publicly", "medium",
     "Spring Boot actuator endpoints are exposed with management.endpoints.web.exposure.include=*."),
]


def build_fortify():
    issues = []
    for path, anchor, occurrence, category, cwe, severity, abstract in FORTIFY_ISSUES:
        line = line_of(path, anchor, occurrence)
        issues.append({
            "issueInstanceId": issue_id("fortify", path, anchor, occurrence),
            "category": category,
            "kingdom": KINGDOMS.get(category, "Input Validation and Representation"),
            "friority": severity,
            "analyzer": "Structural" if category.startswith("Password") else "Dataflow",
            "cwe": "CWE-{0}".format(cwe),
            "confidence": 4.8 if severity in ("Critical", "High") else 3.6,
            "impact": 4.0 if severity in ("Critical", "High") else 2.5,
            "primaryLocation": {"filePath": path, "lineNumber": line},
            "abstract": abstract,
            "scanDate": SCAN_DATE,
            "engineVersion": "22.2.0.0007",
            "auditStatus": "Not Reviewed",
        })
    return {
        "reportDefinition": "Northgate application security review",
        "projectName": PROJECT,
        "projectVersion": PROJECT_VERSION,
        "scanDate": SCAN_DATE,
        "issueCount": len(issues),
        "issues": issues,
    }


KINGDOMS = {
    "SQL Injection": "Input Validation and Representation",
    "XML External Entity Injection": "Input Validation and Representation",
    "Command Injection": "Input Validation and Representation",
    "Path Manipulation": "Input Validation and Representation",
    "Header Manipulation": "Input Validation and Representation",
    "Log Forging": "Input Validation and Representation",
    "Password Management: Hardcoded Password": "Security Features",
    "Password Management: Password in Configuration File": "Environment",
    "Key Management: Hardcoded Encryption Key": "Security Features",
    "Weak Cryptographic Hash": "Security Features",
    "Weak Password Storage": "Security Features",
    "Weak Encryption": "Security Features",
    "Weak Encryption: Insecure Key Length": "Security Features",
    "Dynamic Code Evaluation: Unsafe Deserialization": "Input Validation and Representation",
    "Missing Authorization Check": "Security Features",
}


def build_sonar():
    issues = []
    for path, anchor, occurrence, rule, severity, kind, message, tags in SONAR_ISSUES:
        line = line_of(path, anchor, occurrence)
        issues.append({
            "key": issue_id("sonar", path, anchor, occurrence),
            "rule": rule,
            "severity": severity,
            "component": "northgate:{0}".format(path),
            "project": "northgate",
            "line": line,
            "message": message,
            "type": kind,
            "effort": "30min",
            "tags": tags,
            "creationDate": SCAN_DATE,
            "status": "OPEN",
        })
    return {
        "paging": {"pageIndex": 1, "pageSize": 100, "total": len(issues)},
        "analysisDate": SCAN_DATE,
        "qualityGate": "Northgate Way",
        "total": len(issues),
        "issues": issues,
    }


def build_xray():
    violations = []
    for coordinates, version, cve, severity, cvss, fixed, summary, source in MAVEN_VULNS:
        group, artifact = coordinates.split(":")
        violations.append({
            "issue_id": "XRAY-{0}".format(issue_id("maven", coordinates, cve)),
            "severity": severity,
            "type": "security",
            "summary": summary,
            "cves": [{"cve": cve, "cvss_v3_score": cvss}],
            "impacted_artifact": {
                "type": "maven",
                "name": coordinates,
                "group": group,
                "artifact": artifact,
                "version": version,
            },
            "fixed_versions": [fixed],
            "version_source": source,
            "component_path": "northgate-ratings-platform:ratings-service:{0}".format(PROJECT_VERSION),
        })
    for package, version, cve, severity, cvss in DEBIAN_VULNS:
        violations.append({
            "issue_id": "XRAY-{0}".format(issue_id("deb", package, cve)),
            "severity": severity,
            "type": "security",
            "summary": "{0} {1} is affected by {2}.".format(package, version, cve),
            "cves": [{"cve": cve, "cvss_v3_score": cvss}],
            "impacted_artifact": {
                "type": "debian",
                "name": package,
                "version": version,
                "distro": "debian:10",
            },
            "fixed_versions": [],
            "version_source": "base image openjdk:8u282-jdk-buster",
            "component_path": "docker://{0}".format(IMAGE),
        })
    return {
        "scan_id": "d3f01a6c-9c2f-4d31-9a53-1f0c1a2b8e77",
        "scan_date": SCAN_DATE,
        "artifact": "docker://{0}".format(IMAGE),
        "base_image": "openjdk:8u282-jdk-buster",
        "base_image_distro": "Debian GNU/Linux 10 (buster)",
        "total_violations": len(violations),
        "violations": violations,
    }


def build_prisma():
    return {
        "scanTime": SCAN_DATE,
        "results": [{
            "id": "sha256:1c9f2d3ab41d9d6f3fbb7c5b7a2d0c2b1a9e8f7d6c5b4a3928170615243f0e9d",
            "name": IMAGE,
            "distro": "Debian GNU/Linux 10 (buster)",
            "distroRelease": "buster",
            "eolDate": "2024-06-30",
            "baseImage": "openjdk:8u282-jdk-buster",
            "runAsRoot": True,
            "compliances": [
                {
                    "id": identifier,
                    "title": title,
                    "severity": severity,
                    "description": description,
                }
                for identifier, title, severity, description in PRISMA_COMPLIANCE
            ],
            "complianceDistribution": {
                "critical": sum(1 for c in PRISMA_COMPLIANCE if c[2] == "critical"),
                "high": sum(1 for c in PRISMA_COMPLIANCE if c[2] == "high"),
                "medium": sum(1 for c in PRISMA_COMPLIANCE if c[2] == "medium"),
                "low": 0,
            },
            "vulnerabilityDistribution": {
                "critical": sum(1 for v in DEBIAN_VULNS if v[3] == "Critical"),
                "high": sum(1 for v in DEBIAN_VULNS if v[3] == "High"),
                "medium": sum(1 for v in DEBIAN_VULNS if v[3] == "Medium"),
                "low": sum(1 for v in DEBIAN_VULNS if v[3] == "Low"),
                "total": len(DEBIAN_VULNS),
            },
            "note": ("Package level detail for this image is reported by X-ray; the entries "
                     "here are configuration and compliance checks and are not counted in the "
                     "148 findings."),
        }],
    }


def write(relative, payload):
    path = os.path.join(SECURITY, relative)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, sort_keys=False)
        handle.write("\n")
    return path


def main():
    fortify = build_fortify()
    sonar = build_sonar()
    xray = build_xray()
    prisma = build_prisma()

    written = [
        write("fortify/fortify-ratings-service.json", fortify),
        write("sonarqube/sonar-issues.json", sonar),
        write("jfrog/xray-ratings-service.json", xray),
        write("prisma/prisma-image-scan.json", prisma),
    ]

    app = fortify["issueCount"] + sonar["total"]
    maven = len(MAVEN_VULNS)
    debian = len(DEBIAN_VULNS)
    total = app + maven + debian

    for path in written:
        print("wrote {0}".format(os.path.relpath(path, ROOT)))
    print("application findings : {0} (fortify {1}, sonarqube {2})".format(
        app, fortify["issueCount"], sonar["total"]))
    print("maven dependencies   : {0}".format(maven))
    print("debian packages      : {0}".format(debian))
    print("total                : {0}".format(total))

    expected = {"app": 49, "maven": 32, "debian": 67, "total": 148}
    actual = {"app": app, "maven": maven, "debian": debian, "total": total}
    if actual != expected:
        print("catalogue drift: expected {0}, produced {1}".format(expected, actual), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
