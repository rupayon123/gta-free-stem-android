#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
cd "$PROJECT_ROOT"

print_usage() {
  printf 'Usage: %s <commit-message> [--with-untracked]\n' "$(basename "$0")"
  printf 'Example: %s "Refine nav tint contrast"\n' "$(basename "$0")"
  printf '\n' 
  printf 'If --with-untracked is passed, new files are included in the commit.\n' 
}

if [ "$#" -lt 1 ]; then
  print_usage
  echo "Missing commit message" >&2
  exit 2
fi

COMMIT_MESSAGE=$1
INCLUDE_UNTRACKED=${2:-}

if [ ! -d .git ]; then
  echo "Not a git repository: $PROJECT_ROOT" >&2
  exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ -z "$CURRENT_BRANCH" ] || [ "$CURRENT_BRANCH" = "HEAD" ]; then
  echo "Cannot determine branch" >&2
  exit 1
fi

if ! git remote | grep -qx origin; then
  echo "origin remote is required for push-safe" >&2
  exit 1
fi

UPSTREAM="origin/$CURRENT_BRANCH"

echo "Syncing branch ${CURRENT_BRANCH} with ${UPSTREAM}"
git fetch origin

if git show-ref --verify --quiet "refs/remotes/$UPSTREAM"; then
  LOCAL_SHA=$(git rev-parse "$CURRENT_BRANCH")
  REMOTE_SHA=$(git rev-parse "$UPSTREAM")
  if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
    echo "Rebasing on top of ${UPSTREAM}"
    git pull --rebase origin "$CURRENT_BRANCH"
  fi
fi

if [ -z "$(git status --short)" ]; then
  echo "No changes detected; nothing new to commit."
else
  if [ "$INCLUDE_UNTRACKED" = "--with-untracked" ]; then
    git add -A
  else
    git add -u
  fi

  if [ -z "$(git diff --cached --name-only)" ]; then
    echo "No staged tracked changes. Use --with-untracked to include new files." >&2
    exit 1
  fi

  git commit -m "$COMMIT_MESSAGE"
fi

attempt=0
max_attempts=3

while [ "$attempt" -lt "$max_attempts" ]; do
  attempt=$((attempt + 1))
  echo "Push attempt ${attempt}/${max_attempts}"

  if git push origin "$CURRENT_BRANCH"; then
    echo "Push succeeded."

    git fetch origin
    LOCAL_AFTER=$(git rev-parse "$CURRENT_BRANCH")
    REMOTE_AFTER=$(git rev-parse "$UPSTREAM")

    if [ "$LOCAL_AFTER" = "$REMOTE_AFTER" ]; then
      echo "Verified remote includes local HEAD $(git rev-parse --short HEAD)"
      exit 0
    fi

    echo "Push returned success, but remote HEAD differs. Investigate immediately." >&2
    exit 2
  fi

done

echo "Push failed after ${max_attempts} attempts. Resolve any local errors and try again." >&2
exit 1
