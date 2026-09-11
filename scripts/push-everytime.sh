#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

BRANCH="$(git rev-parse --abbrev-ref HEAD)"
MESSAGE="${1:-chore: update Android app UI polish}"

git add -A
if git diff --cached --quiet; then
  echo "No staged changes. Pulling latest before push only."
  git pull --rebase --autostash origin "$BRANCH"
  git push origin "$BRANCH"
  exit 0
fi

git commit -m "$MESSAGE"
git pull --rebase --autostash origin "$BRANCH"
git push origin "$BRANCH"
