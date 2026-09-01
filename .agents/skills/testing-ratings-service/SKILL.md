---
name: testing-ratings-service
description: How to run and browser-verify the Northgate ratings-service locally (Java 8 Spring Boot), including how to exercise POST/DELETE/custom-header endpoints from a browser without devtools, and how to A/B prove a behaviour change against the pre-change build.
---

# Verifying ratings-service in a browser

The `.devin/playbooks/finding-to-merged-pr.md` step 7 requires a browser-driven, annotated recording
of `/actuator/health`, `/api/ratings/NG-1001`, `/api/ratings/search?q=Bra` plus the endpoints touched.
This skill covers how to actually do that.

## Build & run

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64   # default `java` on the box is 17 and will not run this
cd <repo root> && mvn -B -o clean package             # -o (offline): Maven Central is 429 rate-limited here;
                                                      # ~/.m2/settings.xml mirrors central to a GCS mirror
java -jar ratings-service/target/ratings-service.jar   # port 8080, H2 in-memory, seeded from resources/data.sql
```

Startup takes ~20-30s. Health check: `curl -s localhost:8080/actuator/health` → `{"status":"UP",...}`.

Seed data (resources/data.sql): NG-1001 Alderney Power Holdings A-/stable, NG-1002 Bramfield Logistics
BBB/negative, NG-1003 Carrow Bank plc AA-/stable, ... NG-1008. Grades are mutated by any override/grade
call, so re-read expected values from a fresh start or re-check `data.sql` before asserting.

## Browser harness for POST / DELETE / custom headers (no devtools needed)

Browsers can only navigate GETs and cannot set headers like `X-Internal-Admin`. Instead of using the
devtools console (confusing in a recording), serve a small static HTML page with buttons **from the app's
own origin** so `fetch()` calls to relative paths have no CORS problem:

```bash
mkdir -p /tmp/harness   # put index.html here: buttons calling fetch(path,{method,headers,body})
                        # and printing "HTTP STATUS: <n>" plus the raw response text into a <pre>
java -jar ratings-service/target/ratings-service.jar \
  --spring.web.resources.static-locations=file:/tmp/harness/ \
  --spring.resources.static-locations=file:/tmp/harness/
```

Then open `http://localhost:8080/index.html`. Printing the *raw* response text (not a re-serialized
object) is what proves frozen-contract key order, e.g. `RatingsController.updateGrade` must return
exactly `{"issuerId":...,"grade":...,"outlook":...,"status":"OK"}` in that order.

## A/B proof that behaviour actually changed

Playbook step 7 asks you to show the behaviour changed, not just that the new build looks sane. Build the
pre-change commit in a worktree and run it on another port, then show the same URL in both:

```bash
git worktree add /tmp/old-build HEAD~1
cd /tmp/old-build && JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn -B -o -q clean package -DskipTests
java -jar ratings-service/target/ratings-service.jar --server.port=8081
# ... verify, then: git worktree remove --force /tmp/old-build
```

For authorization fixes this is the discriminating test: old port renders data, new port renders 403.
Spring's `sendError(SC_FORBIDDEN)` renders the Whitelabel Error Page on navigation and a JSON body
(`{"status":403,"error":"Forbidden",...}`) on `fetch`, so assert on the status line, not the body style.
Also always test the header set to a wrong value (`false`), not just absent — a presence-only check would
pass the "absent" case and still be broken.

## Devin Secrets Needed

None; everything runs locally with in-memory H2.
