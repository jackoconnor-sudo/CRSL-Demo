# Ten minute run of show

Internal. `DEMO.md` is the version to hand to the customer; this is the one you drive from.

The argument, in one line: **the scanner backlog is not 148 problems, it is about a dozen,
and the value is in the grouping, the reachability call and the merged pull request — not
in the patch.**

Everything below is rehearsed against a repository that fails its own gate on a clean
checkout. Remediation always lands on a branch, never on `main`, so the fixture stays red
between runs. To reset after a run: `./demo/reset.sh` to preview, `./demo/reset.sh --apply`
to close the demo pull requests, delete their branches, and put `main` back on the
`demo-baseline` tag.

## Before the call

- [ ] Repository connected to the Devin org, and indexed in DeepWiki. Indexing is not
      instant; do it the day before.
- [ ] Three playbooks created from `.devin/playbooks/`.
- [ ] Two sessions **pre-run and left open in tabs**: the triage session and one
      remediation session that has already produced a pull request. Live sessions are the
      demo, but a ten minute slot cannot absorb a cold start.
- [ ] `security-gate` shown failing on `main` in Actions, with the fan-out workflow's dry
      run summary from a previous run available.
- [ ] Local terminal in the repository, build already warm (`mvn -B clean verify` once).
- [ ] Decide in advance: GitHub **or** Azure DevOps. Do not show both.
- [ ] `./demo/reset.sh` run after the previous rehearsal, so `main` is on `demo-baseline`
      and no stale pull requests are open.
- [ ] Deployment section: skip unless the customer asked. It costs three minutes and
      proves the least interesting claim.

## The ten minutes

| Time | Beat | What you do | The line |
| ---- | ---- | ----------- | -------- |
| 0:00 | Set the scene | `README.md` and `security/README.md`. 148 findings, four scanners, nobody who wrote this service still works here. | "This is the repository nobody wants the ticket for." |
| 1:00 | Understand before touching | DeepWiki: *where is authorization enforced on the admin endpoints, and what happens to a request with no `X-Internal-Admin` header?* Then open `AdminApiFilter.java` and read line by line: the header is evaluated, logged, and the chain is called anyway. | "No scanner in that directory reported this. It came from reading the code." |
| 2:30 | Triage | Open the pre-run triage session. Show the root cause table: 67 findings behind one `FROM` line, 32 behind version properties in one POM, 49 application findings collapsing to a handful of causes. | "148 findings, about a dozen decisions." |
| 4:00 | The false positives | In the same session, the `ReportQueryBuilder` adjudication. It builds SQL by concatenation and three scanners flagged it; every interpolated value is a server-side enum or whitelisted column. Show the quoted line. | "It declined to change safe code, and told you which line makes it safe. That is a result, not a gap." |
| 5:00 | One finding, one pull request | Open the pre-run remediation pull request for the missing authorization check: the failing test written first, the fix, the frozen response contract still intact, the gate condition flipped. | "One finding per session, on purpose. Nobody can review authorization, cryptography and a base image in one pass." |
| 6:30 | The cheap 67 | The base image change: one line, 67 findings closed, Java 8 kept, non-root user added, image still builds. | "Same amount of review, forty five times the closure. That is what the grouping bought." |
| 7:30 | Gate to session | Actions: `security-gate` red on a clean checkout, then the fan-out workflow's job summary with one session payload per failing condition, dry run. | "The gate does not file a ticket. It opens the sessions, one per root cause, and defaults to a dry run so you can read the payloads first." |
| 8:30 | Brownfield, not just security | `ado/work-items.csv`, the grade history story. Read the constraints out loud: frozen response shape with a positional 2019 client behind it, schema by hand through `schema.sql`, no migration tool, existing tests stay green. Show the pull request honouring all four. | "The constraints are the job. Anyone can add a table." |
| 9:30 | How you would measure it | `deploy/grafana/ratings-remediation-dashboard.json`. Merged pull requests and findings closed, reported separately. Cost per merged pull request and per closed finding. Review queue. | "Gate all of it on merged work. And watch the review queue, because that is what caps you, not the patch." |

## If you have longer

- The 30 minute version: run one remediation session live from a cold start, on the base
  image finding. It is the most reliable to watch because the verification is a build.
- Run the Devin code scan from `.devin/scan-profile.yml` alongside the fabricated exports
  and compare the two lists. The two edges — findings that are not real, defects no
  scanner raised — are the whole argument, in one screen.
- `deploy/README.md` on kind, with Argo syncing the merged manifest change.

## Questions you will get

**"Are these real findings?"** No. Every export under `security/` is fabricated and no
scanner ran. The *code* they point at is real and the line numbers are resolved from the
working tree, which is why the references are accurate. Say this before you are asked.

**"Would it have found the authorization defect on its own?"** That is what the DeepWiki
beat is for, and it is worth being precise: the defect is found by reading the request
path, not by pattern matching, which is also why the scanners in `security/` missed it.

**"What stops it fixing the false positives?"** Nothing structural — it is a judgement,
which is why the triage playbook demands the line of code that makes the code safe and
rejects a false positive claim without one.

**"How do we know nothing broke?"** The frozen contract test, the recording attached to
each pull request, and the fact that no existing test was edited. Point at
`RatingsApiTest.updateGradeResponseShapeIsFrozen`.

**"How much did that cost?"** Take it to cost per closed finding rather than per session,
and use the base image change as the example.

## Do not

- Do not run the whole gate remediation in one session to save time. The single enormous
  pull request is the anti-pattern this repository exists to argue against.
- Do not promise the deployment section unless you have brought up kind beforehand.
- Do not describe the findings as production data, or the credentials as real.
