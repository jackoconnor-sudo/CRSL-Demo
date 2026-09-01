# ado/

Azure DevOps seeds. Nothing here talks to a live organisation.

## work-items.csv

Eight work items: six user stories and two bugs, written the way a maintenance backlog on
a service like this actually reads. Import with **Boards → Work Items → Import Work
Items**, or with the CLI:

    az boards work-item create --org https://dev.azure.com/<org> --project <project> \
      --type "User Story" --title "..." --description "..."

The first two stories are the pair used in the walkthrough. They are ordered: the history
endpoint depends on the table created by the story above it. Their descriptions carry the
constraints that make the work brownfield rather than greenfield — a frozen response shape
with a positional client behind it, schema changes by hand through `schema.sql`, and no
migration tool. Those constraints are the point. A model that ignores them produces a
change that passes its own tests and breaks a desktop client nobody in the room maintains.

## Pipeline

`azure-pipelines.yml` at the repository root is the Azure DevOps expression of the same
build, gate and fan-out flow as `.github/workflows/`. The `remediate` stage runs only when
the gate stage fails, and prints payloads until `DEVIN_DISPATCH_LIVE` is set to `true`.

Variables it expects:

| Variable | Purpose |
| -------- | ------- |
| `DEVIN_API_KEY` | Secret. Devin API key. Without it the stage stays a dry run. |
| `DEVIN_DISPATCH_LIVE` | `true` to create sessions. Defaults to `false`. |
| `DEVIN_PLAYBOOK_ID` | Playbook the created sessions run. |
