# A work item to a deployed change

## Overview

Implement one work item from `ado/work-items.csv` in a service that has been in production
since 2019. The constraints in the item's description are not suggestions — they are the
reason the work is hard, and they come from systems that are not in this repository.

The measure of success is not that the feature works. It is that the feature works and
nothing that was working before has changed.

## Setup

    JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64
    mvn -B clean verify

Green before you start, or stop and report it.

## Procedure

1. **Read the work item in full**, including the acceptance criteria and every constraint
   in the description. Restate the constraints in your own words before writing code and
   say, for each one, how your plan honours it. If two constraints conflict, say so and
   ask rather than choosing silently.

2. **Read the code the item touches.** Find the existing patterns and follow them. This
   service uses hand-written JDBC through `RatingsRepository`, schema in
   `ratings-service/src/main/resources/schema.sql`, and no migration tool. Do not
   introduce JPA, Flyway, Liquibase, Lombok or a new persistence style to make your change
   more comfortable. Matching the surrounding code is worth more here than improving it.

3. **Respect frozen contracts.** Where the item says a response shape is frozen, it means
   the JSON body is identical key for key, in the same order, with the same types. A
   client outside this repository reads it positionally. Adding a field is a breaking
   change. `RatingsApiTest.updateGradeResponseShapeIsFrozen` is the check; if your change
   makes it fail, your change is wrong.

4. **Schema changes go through `schema.sql`.** It is applied by hand against staging and
   production, so it must stay idempotent — `CREATE TABLE IF NOT EXISTS`, no destructive
   statements, no reliance on execution order beyond what is already there. State in the
   pull request what the DBA has to run and whether it is safe to run twice.

5. **Transactional integrity.** Where the item says a partial write is a defect, make it
   impossible: the new write and the existing update either both happen or neither does.
   Prove it with a test that forces the second write to fail and asserts the first was
   rolled back.

6. **Do not edit existing tests.** They encode the behaviour of live clients. Add tests;
   leave the old ones alone. If an existing test fails, the implementation is wrong.

7. **Cover the acceptance criteria explicitly.** Every criterion gets a test, including the
   unglamorous ones — not-found, empty-result, and the difference between them. An empty
   history is a 200 with an empty array, not a 404; that distinction is exactly the kind of
   thing that gets it wrong in review.

8. **Verify.**

       mvn -B clean verify
       python3 security/gate_check.py

   All tests pass. The gate is no worse than before your change; if you have introduced a
   new gate failure, fix it before opening the pull request.

9. **Verify the running service and record it.** Required for every code change.

       java -jar ratings-service/target/ratings-service.jar

   Start a screen recording, then in a browser: check `/actuator/health`, exercise the
   endpoints the item adds or changes, and exercise `/api/ratings/NG-1001` and
   `/api/ratings/search?q=Bra` to show the untouched paths still respond exactly as
   before. Annotate each check as it happens, including the frozen response body. Attach
   the recording to the pull request as proof of no regression.

10. **Open the pull request**, one work item per pull request.

## Pull request description

- The work item title, and the constraints it imposed.
- How each constraint is honoured, in one line each.
- What the DBA has to apply from `schema.sql`, and confirmation it is idempotent.
- The acceptance criteria, each mapped to the test that covers it.
- Evidence the frozen contract is intact.
- Verification output and the screen recording.
- What you did not do, and anything you would want confirmed before this reaches
  production.
