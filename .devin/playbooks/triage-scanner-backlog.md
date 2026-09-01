# Triage a scanner backlog

## Overview

Turn a pile of scanner findings into a remediation plan ordered by root cause, with
reachability established for each group and false positives adjudicated. This playbook
produces a plan and nothing else. It does not change code and it does not open a pull
request.

The output is meant to be read by an engineer who has to decide what gets worked on this
week and defend that order in a review.

## Procedure

1. **Read the exports before reading the code.** Every file under `security/`. Note the
   scanner, the rule, the file, the line and the severity for each finding. Reconcile the
   totals you count against `security/README.md`; if they differ, say so, because a
   mismatch means the exports are stale relative to the code.

2. **Group by root cause, not by rule and not by file.** A group is a set of findings that
   one change closes. Name the single change for each group. Findings that resolve to a
   version property in the root `pom.xml`, or to the base image in
   `ratings-service/Dockerfile`, are one group each no matter how many rules fired.
   Report the finding count per group. Expect the counts to be extremely uneven.

3. **Establish reachability for each group.** For every group, trace whether untrusted
   input can actually get to the code, and write down the path: the entry point, the
   controller method, and the call chain to the flagged line. Classify each group as:
   - reachable from an unauthenticated request,
   - reachable only from an authenticated or internal caller,
   - not reachable from any request path (batch jobs, dead code, test-only), or
   - not a code path at all (build, image, configuration).
   Say which one and why. "Probably reachable" is not an answer; follow the calls.

4. **Adjudicate false positives.** For any finding you believe is not real, quote the
   specific line of code that makes it safe and explain what the scanner could not see —
   typically that a value is server-side, enumerated, or checked against a whitelist
   before it reaches the flagged expression. A false positive claim without a quoted line
   is not accepted. If you are not certain, mark it as needs-verification rather than
   guessing, and say what you would need to look at.

   Declining to change safe code is a result. Do not pad the count of real issues, and do
   not "fix" code you have just argued is correct.

5. **Find what the scanners missed.** Read the request path end to end and report defects
   no export mentions, especially in authorization and error handling. Missing controls
   do not appear in dataflow findings. This section is usually the most valuable part of
   the plan.

6. **Order the work.** Sequence the groups by findings closed per unit of review risk, and
   state explicitly where you have deviated from severity order and why. A high-severity
   finding that is unreachable ranks below a medium one on an unauthenticated path.
   Note which groups can proceed in parallel and which must be sequential because they
   touch the same file.

## Deliverable

A single message, in this order:

1. Table of root cause groups: group, single change that closes it, finding count,
   reachability class, severity spread.
2. Findings you assess as false positives, each with the file, line, the quoted line of
   code that makes it safe, and what the scanner missed. Include anything you are marking
   needs-verification.
3. Defects the scanners did not report.
4. Recommended order of work, with the reasoning for any departure from severity order,
   and which groups are independent enough to run in parallel sessions.
5. What you could not determine and what you would need to determine it.

Do not open a pull request. Do not edit any file, including the exports. If you believe
the exports are stale, say so in the deliverable rather than regenerating them.
