# Northgate Ratings Platform

> **This repository is synthetic.** It is a fixture built to look like a service that has
> been in production since 2019 and maintained since by people who did not write it. Every
> finding under `security/` is fabricated, no credential in the repository is real, and no
> live scanner, pipeline or cluster is connected to it. It contains deliberately vulnerable
> code and must not be deployed anywhere reachable. See [`DEMO.md`](DEMO.md) for what it is
> for, and `security/README.md` for how the findings are constructed.

Issuer credit ratings lookup, export and legacy feed ingestion for the credit desk.

## Layout

| Path | What it is |
| ---- | ---------- |
| `ratings-service/` | Spring Boot 2.3 service: ratings lookup, export, legacy XML feed ingestion, session handling |
| `pom.xml` | Parent POM. Six dependency versions pinned centrally in a 2021 refresh that stopped halfway |
| `ratings-service/Dockerfile` | `openjdk:8u282-jdk-buster`, Debian buster, runs as root |
| `security/` | Fabricated Fortify, SonarQube, JFrog X-ray and Prisma exports: 148 findings, plus the gate |
| `.github/workflows/` | Build plus gate, and a workflow that starts one Devin session per failing gate condition |
| `azure-pipelines.yml`, `ado/` | The same flow for Azure DevOps, plus Boards work item seeds |
| `deploy/` | Kubernetes manifests, an Argo CD application, a Grafana dashboard. Optional |
| `.devin/` | Playbooks and a scan profile |

## Running it

Java 8 and Maven. Python 3.8 or later for the gate.

    mvn -B clean verify
    java -jar ratings-service/target/ratings-service.jar

    curl localhost:8080/api/ratings/NG-1001
    curl 'localhost:8080/api/ratings/search?q=Bra'
    curl localhost:8080/actuator/health

The gate, which fails on a clean checkout by design:

    python3 security/gate_check.py

Regenerate the exports after changing source, since the findings carry line numbers
resolved from the working tree:

    python3 security/generate_findings.py

Image:

    docker build -f ratings-service/Dockerfile -t northgate/ratings-service:1.4.2 .

## API

| Method | Path | Notes |
| ------ | ---- | ----- |
| `GET` | `/api/ratings/{issuerId}` | 404 for an unknown issuer |
| `GET` | `/api/ratings/search?q=&sector=` | Partial name match. `sector` is undocumented and unvalidated |
| `POST` | `/api/ratings/{issuerId}/grade` | **Response shape is frozen.** A 2019 desktop client parses it positionally |
| `POST` | `/api/exports/run` | Runs `export.sh` |
| `GET` | `/api/exports/download?name=` | |
| `GET` | `/api/reports/rollup?by=&window=` | `by` and `window` are server-side enums |
| `GET` | `/api/reports/projection?columns=` | Columns checked against a whitelist |
| `POST` | `/api/feed/xml` | Overnight legacy feed ingestion |
| `GET`/`POST` | `/api/session/*` | Cookie-based session state |
| `*` | `/api/admin/*` | Reads `X-Internal-Admin`. Read `config/AdminApiFilter.java` before assuming that means it is enforced |

## Things a new maintainer should know

- Schema changes go into `ratings-service/src/main/resources/schema.sql`, which the desk
  DBA applies by hand. There is no migration tool, so the file has to stay idempotent.
- The response body of the grade update is a contract with a client nobody here maintains.
  `RatingsApiTest.updateGradeResponseShapeIsFrozen` is there to stop it drifting.
- The 2021 dependency refresh pinned six versions in the parent POM and then stopped. The
  child module does not override them, so those pins are still where the versions come
  from.
- `security/gate_check.py` evaluates the working tree rather than the exports, so it turns
  green when the code is actually fixed.
