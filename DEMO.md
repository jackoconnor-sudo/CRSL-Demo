# Northgate Ratings Platform: walkthrough

Shareable. This is the version to hand to someone outside Cognition, or to paste into a Devin
account as context. It describes the fixture and how to run it, and nothing else.

Northgate Ratings Platform is a synthetic Java 8 service, built to look like a service that has
been in production since 2019 and maintained since by people who did not write it. Every
finding in `security/` is fabricated. No credential in the repository is real, and no live
scanner, pipeline or cluster is connected to it.

## What is in here

| Path | What it is |
| ---- | ---------- |
| `ratings-service/` | Spring Boot 2.3 service: issuer ratings lookup, export, legacy XML feed ingestion, session handling |
| `pom.xml` | Parent POM. Six dependency versions pinned centrally in a 2021 refresh that stopped halfway |
| `ratings-service/Dockerfile` | `openjdk:8u282-jdk-buster`, Debian buster, runs as root |
| `security/` | Fabricated Fortify, SonarQube, JFrog X-ray and Prisma exports: 148 findings |
| `security/gate_check.py` | Deterministic gate that stands in for a live Sonar or X-ray gate |
| `.github/workflows/` | Build plus gate, and a workflow that starts one Devin session per failing gate condition |
| `azure-pipelines.yml`, `ado/` | The same flow expressed for Azure DevOps, plus Boards work item seeds |
| `deploy/` | Kubernetes manifests, an Argo CD application, and a Grafana dashboard |
| `.devin/` | Playbooks and a scan profile |

## How the 148 findings break down

- 67 are Debian buster OS packages inherited from the base image. One Dockerfile line closes
  all of them.
- 32 are Maven dependencies, all resolving to version properties in the root `pom.xml`. The
  child module never overrides them, so the fix is central. Log4Shell and Spring4Shell are both
  in this set.
- The rest are application findings: SQL injection, XXE, hardcoded credentials, MD5 and DES,
  a shell command built from a request parameter, path traversal on export download, Java
  deserialization of a session cookie, and an authorization filter that logs whether the caller
  is an admin and then lets every request through.
- Six are not real. Three of them cluster on `ReportQueryBuilder`, which builds SQL by
  concatenation and is safe, because each fragment is either a server-side enum or checked
  against a whitelist.

The false positives are there on purpose. Correctly declining to change safe code is a result,
not a gap.

## Running it

Build and test:

    mvn -B clean verify
    java -jar ratings-service/target/ratings-service.jar

The gate, which fails on a clean checkout:

    python3 security/gate_check.py

Regenerate the scanner exports after moving code around, so the line references stay accurate:

    python3 security/generate_findings.py

Container:

    docker build -f ratings-service/Dockerfile -t northgate/ratings-service:1.4.2 .

## Running it with Devin

Connect the repository, index it in DeepWiki, then create three playbooks from
`.devin/playbooks/`. The prompts stay short because the playbook carries the procedure.

**Understand it first.** Ask DeepWiki where authorization is enforced on the admin endpoints
and what happens to a request with no `X-Internal-Admin` header. The answer is a real defect,
found by reading the code rather than by a scanner.

**Triage before remediating**, on the `triage-scanner-backlog` playbook:

> Triage the scanner exports under `security/` against this repository and produce the plan:
> group by root cause, establish reachability for each group, and adjudicate anything you
> believe is a false positive with the line of code that makes it safe. Do not open a PR.

**Then remediate, one session per finding**, on `finding-to-merged-pr`:

> Fix the missing authorization check on `/api/admin/*` reported by Fortify at
> `ratings-service/src/main/java/com/northgate/ratings/config/AdminApiFilter.java:37`, keeping
> the response contract for existing clients intact. Add a test that fails before your change.

> Replace the end of life base image in `ratings-service/Dockerfile` so the 67 Debian buster
> findings in `security/jfrog/xray-ratings-service.json` clear, keeping the service on Java 8
> and adding a non-root user. Verify `mvn -B clean verify` and the image build still work.

One finding per session is deliberate. Batching produces a single PR that changes
authorization, cryptography and a base image at once, which no reviewer can sign off in one
pass.

**Brownfield delivery**, on `work-item-to-change`, using the user stories in
`ado/work-items.csv`:

> Implement the "Persist every grade change to a rating history table" user story from
> `ado/work-items.csv`, honouring the constraints listed in its description. Then implement
> `GET /api/ratings/{issuerId}/history` from the following user story, with tests for the
> not-found and empty-history cases.

That item is constrained on purpose. The `updateGrade` response shape is frozen because a 2019
desktop client parses it positionally, the schema goes through `schema.sql` rather than a
migration tool, and the existing tests have to stay green.

**Gate failures.** `security-gate` fails on a clean checkout.
`.github/workflows/gate-failure-to-devin.yml` reacts to that by creating one session per
failing condition, and defaults to a dry run so you can inspect the payloads first. The Azure
DevOps equivalent is the `remediate` stage in `azure-pipelines.yml`.

## Deployment

Optional, and the repository stands on its own without it. `deploy/README.md` has a kind-based
local path. The manifests deliberately carry the container finding forward: nothing overrides
the root user from the base image, and the ingress no longer sets the header the authorization
filter assumes.

## Measuring it

Four numbers are worth tracking against a repository like this one, and all four should be
gated on merged work rather than on opened pull requests, since a fix nobody merged is not a
fix:

- Merged remediation PRs per week, and findings closed per week, reported separately. One base
  image change closes 67 findings, which makes a chart of closures alone look excellent for one
  week and broken afterwards.
- Cost per merged PR and per closed finding, rather than total spend. Root-cause work looks
  unremarkable on the first measure and very good on the second.
- Time from a finding first appearing in the scanner to the fix being merged.
- Time from a work item starting to the change being deployed.

Review and merge capacity is what caps all of them. Generating a patch is not the constraint.
