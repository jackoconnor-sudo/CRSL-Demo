# security/

Fabricated scanner exports for the ratings service, plus the gate that consumes them.

Nothing in here came from a live scanner. The findings are invented; the code they point
at is not. Every application finding is anchored to a snippet of source and the line
numbers are resolved from the working tree by `generate_findings.py`, so the references
stay accurate when code moves.

## The exports

| File | Stands in for | Contents |
| ---- | ------------- | -------- |
| `fortify/fortify-ratings-service.json` | Fortify SCA | 42 application findings, dataflow and structural |
| `sonarqube/sonar-issues.json` | SonarQube issue search API | 7 application findings |
| `jfrog/xray-ratings-service.json` | JFrog X-ray artifact scan | 32 Maven dependency violations, 67 Debian package violations |
| `prisma/prisma-image-scan.json` | Prisma Cloud image scan | 4 image compliance checks, not counted in the 148 |

148 findings: 49 application, 32 Maven, 67 Debian.

## How they group

- The 67 Debian findings all come from `openjdk:8u282-jdk-buster`. One `FROM` line closes the group.
- The 32 Maven findings all resolve to a version property in the root `pom.xml` or to the
  `spring-boot-starter-parent` version declared in the same file. The child module never
  overrides them, so the fix is central.
- The 49 application findings are a much smaller number of root causes. Six of them are
  not real.

## Regenerating

    python3 security/generate_findings.py

The script asserts the counts, so a mistake in the catalogue fails the run rather than
quietly changing the size of the backlog. If it reports that an anchor is missing, the
code moved and the catalogue entry needs the new snippet.

## The gate

    python3 security/gate_check.py          # human readable
    python3 security/gate_check.py --json   # machine readable

Eleven conditions, evaluated against the working tree rather than against the exports, so
the gate turns green when the code is fixed. It fails on a clean checkout. Each condition
names the number of scanner findings attributable to it, which is what the remediation
workflow uses to fan out one session per failing condition.

The gate deliberately does not cover everything. The findings it leaves alone are the
low and medium application findings, including the six that are not real: deciding what
to do about those is triage work, not gate work.
