#!/usr/bin/env bash
# Restore the repository to the pristine demo baseline: main at the demo-baseline
# tag, no open demo pull requests, no leftover devin/* branches.
#
#   ./demo/reset.sh            show what would change
#   ./demo/reset.sh --apply    actually reset
#
# Requires the gh CLI, authenticated against the demo repository.
set -euo pipefail

BASELINE_TAG="${BASELINE_TAG:-demo-baseline}"
REMOTE="${REMOTE:-origin}"
APPLY=0
[[ "${1:-}" == "--apply" ]] && APPLY=1

run() {
  if [[ $APPLY -eq 1 ]]; then
    echo "+ $*"
    "$@"
  else
    echo "would run: $*"
  fi
}

cd "$(dirname "$0")/.."
git fetch --prune "$REMOTE" --tags >/dev/null

if ! git rev-parse -q --verify "refs/tags/$BASELINE_TAG" >/dev/null; then
  echo "tag $BASELINE_TAG does not exist; create it on the pristine commit first:"
  echo "  git tag $BASELINE_TAG <sha> && git push $REMOTE $BASELINE_TAG"
  exit 1
fi

baseline="$(git rev-parse "$BASELINE_TAG^{commit}")"
current="$(git rev-parse "$REMOTE/main")"

echo "baseline $BASELINE_TAG -> $baseline"
echo "$REMOTE/main         -> $current"

for pr in $(gh pr list --state open --json number --jq '.[].number'); do
  run gh pr close "$pr" --delete-branch --comment "Closing after the demo; the fixture is reset to $BASELINE_TAG."
done

for branch in $(git branch -r --list "$REMOTE/devin/*" --format '%(refname:lstrip=3)'); do
  run git push "$REMOTE" --delete "$branch"
done

if [[ "$baseline" != "$current" ]]; then
  run git checkout main
  run git reset --hard "$baseline"
  run git push --force-with-lease "$REMOTE" main
else
  echo "main already at the baseline"
fi

if [[ $APPLY -eq 1 ]]; then
  python3 security/generate_findings.py
  python3 security/gate_check.py || true
fi
