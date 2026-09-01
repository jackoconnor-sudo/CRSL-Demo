# .devin/

Playbooks and a scan profile for working this repository with Devin.

## Playbooks

Create these three in the app, then keep the prompts short — the playbook carries the
procedure, so the prompt only has to name the finding or work item.

| File | Playbook | Used for |
| ---- | -------- | -------- |
| `playbooks/triage-scanner-backlog.md` | `triage-scanner-backlog` | Turning `security/` into a plan. Produces no code. |
| `playbooks/finding-to-merged-pr.md` | `finding-to-merged-pr` | One finding, one pull request. Also what the gate fan-out workflow runs. |
| `playbooks/work-item-to-change.md` | `work-item-to-change` | Brownfield delivery from `ado/work-items.csv`. |

Each playbook that changes code requires a verification pass against the running service
with a screen recording attached to the pull request. Keep that step: the argument this
repository is making is about merged, reviewable, non-regressing changes, and the recording
is what makes the "no regression" half of that claim checkable by someone who was not in
the session.

## Scan profile

`scan-profile.yml` asks Devin to read the code and report what is there, as opposed to
`security/`, which contains fabricated findings someone else's scanner would have reported.
Running both and comparing is the point. The two edges of the comparison are where the
value is:

- findings in `security/` that the scan does not reproduce, because they are not real;
- defects the scan reports that no export mentions, because missing controls do not show
  up in dataflow analysis. The absent authorization check on `/api/admin/*` is the example.

## DeepWiki

Index the repository before the first session. The question worth asking in front of an
audience is where authorization is enforced on the admin endpoints, and what happens to a
request with no `X-Internal-Admin` header. The answer is a real defect that no scanner in
`security/` reports, arrived at by reading the code.
