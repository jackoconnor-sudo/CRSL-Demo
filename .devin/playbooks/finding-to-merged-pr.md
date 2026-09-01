# One finding to one merged pull request

## Overview

Take a single scanner finding, or a single root cause group that one change closes, and
produce a reviewable pull request. One finding per session. If the prompt names more than
one unrelated finding, fix the first and say in the pull request description which others
were left, so each change gets reviewed on its own merits.

Fix the root cause, not the line the scanner pointed at. If the same defect exists in five
other methods of the same class, fix all six and say so.

## Setup

    JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64   # or any Java 8 JDK
    mvn -B clean verify
    python3 security/gate_check.py               # fails on a clean checkout, expected

Confirm the build is green *before* you change anything. If it is not, stop and report
that instead — a pre-existing failure invalidates every claim you would make later.

## Procedure

1. **Locate the finding.** Find it in the export under `security/`. Read the flagged code
   and everything that calls it. Establish that untrusted input reaches it and write down
   the path from the entry point. If you conclude the finding is not real, do not change
   the code: report it with the line that makes it safe and stop.

2. **Write a failing test first.** Add a test that fails against the current code for the
   reason in the finding. Run it and paste the failure. A test that passes before your
   change proves nothing. For image and dependency findings where a unit test is not
   meaningful, use the corresponding `security/gate_check.py` condition as the failing
   check, and paste its output before and after.

3. **Fix the root cause.** Keep the change scoped to this finding. Do not reformat
   surrounding code, do not upgrade unrelated dependencies, and do not fix a second
   finding you noticed on the way — note it for a separate session instead.

4. **Do not break existing clients.** Response shapes on this service are contracts.
   `RatingsApiTest.updateGradeResponseShapeIsFrozen` exists because a 2019 desktop client
   reads that object positionally. Never change a response body, key order, status code or
   header to make a fix easier. If the finding cannot be fixed without a contract change,
   stop and report that trade-off rather than making the change.

5. **Do not weaken tests.** Existing tests stay as they are. If one now fails, the fix is
   wrong or the test encodes the defect — in the second case, say so explicitly and get
   agreement before touching it.

6. **Verify.**

       mvn -B clean verify
       python3 security/generate_findings.py     # line anchors move when code moves
       python3 security/gate_check.py

   All existing tests plus your new one pass. The gate condition for this finding has
   flipped to pass; other conditions may still fail and that is expected. Commit the
   regenerated exports if the line numbers changed.

   For any change touching the `Dockerfile` or dependencies, also build the image:

       docker build -f ratings-service/Dockerfile -t northgate/ratings-service:1.4.2 .

7. **Verify the running service still works, and record it.** This is required for every
   change, not just dependency work.

       java -jar ratings-service/target/ratings-service.jar

   Start a screen recording, then drive the service in a browser: `/actuator/health`,
   `/api/ratings/NG-1001`, `/api/ratings/search?q=Bra`, and the endpoints your change
   touches. Annotate each check. Confirm the responses are byte-identical in shape to
   before your change, and that the behaviour the finding was about has actually changed.
   Attach the recording to the pull request as evidence of no regression.

8. **Open the pull request.** One finding, one branch, one pull request.

## Pull request description

- The finding: scanner, rule, file and line.
- The reachability path in one or two lines: how untrusted input gets there.
- What the root cause was, and every location fixed, not just the flagged one.
- The test that fails before and passes after, named.
- Findings closed by this change, with the count if it closes a group.
- Verification output: `mvn -B clean verify`, the gate condition, the image build if
  relevant, and the screen recording.
- Anything deliberately left alone, and why.

Keep it short. A reviewer should be able to decide from the description whether the
approach is right before reading the diff.
