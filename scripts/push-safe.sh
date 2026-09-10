#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
cd "$PROJECT_ROOT"

print_usage() {
  printf 'Usage: %s <commit-message> [--with-untracked]\n' "$(basename "$0")"
  printf 'Example: %s "Refine nav tint contrast"\n' "$(basename "$0")"
  printf '\n' 
  printf 'All tracked and untracked file changes are included by default.\n'
  printf 'If --with-untracked is passed, the behavior is explicit and unchanged (legacy compatibility).\n'
}

if [ "$#" -lt 1 ]; then
  print_usage
  echo "Missing commit message" >&2
  exit 2
fi

COMMIT_MESSAGE=$1
INCLUDE_UNTRACKED=${2:-}

if [ -n "$INCLUDE_UNTRACKED" ] && [ "$INCLUDE_UNTRACKED" != "--with-untracked" ]; then
  echo "Unsupported option: $INCLUDE_UNTRACKED" >&2
  print_usage
  exit 2
fi

check_sync_state() {
  LOCAL_AHEAD=$(git rev-list --count "${UPSTREAM}..${CURRENT_BRANCH}" || echo 0)
  REMOTE_AHEAD=$(git rev-list --count "${CURRENT_BRANCH}..${UPSTREAM}" || echo 0)
}

has_staged_changes() {
  [ -n "$(git diff --cached --name-only)" ]
}

has_untracked_changes() {
  [ -n "$(git status --short --untracked-files=normal | sed -n '/^??/p' | head -n 1)" ]
}

has_tracked_changes() {
  [ -n "$(git status --short --untracked-files=no)" ]
}

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
  check_sync_state

  if [ "$REMOTE_AHEAD" -gt 0 ]; then
    if [ "$LOCAL_AHEAD" -gt 0 ]; then
      echo "Branch is diverged from ${UPSTREAM}; rebasing to keep history linear."
    else
      echo "Branch is behind ${UPSTREAM}; rebasing to pick up remote commits."
    fi
    git pull --rebase origin "$CURRENT_BRANCH"
  fi
fi

if [ -z "$(git status --short)" ]; then
  echo "No changes detected; nothing new to commit."
else
  echo "Including tracked + untracked changes for commit."
  git add -A

  if ! has_staged_changes; then
    if has_untracked_changes && [ "$INCLUDE_UNTRACKED" != "--with-untracked" ]; then
      echo "No tracked file changes were staged for commit. Set --with-untracked to include new files."
      exit 1
    fi
    echo "No changed tracked files to commit."
  else
    git commit -m "$COMMIT_MESSAGE"
  fi

  if [ "$INCLUDE_UNTRACKED" = "--with-untracked" ] && [ -z "$(git diff --cached --name-only)" ]; then
    echo "No tracked/untracked changes were available to commit." >&2
    exit 1
  fi
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

  echo "Push attempt ${attempt}/${max_attempts} failed, reconciling remote state and retrying..."
  git fetch origin
  check_sync_state

  if [ "$REMOTE_AHEAD" -gt 0 ]; then
    if [ "$LOCAL_AHEAD" -eq 0 ]; then
      echo "Remote branch moved forward while pushing. Rebasing and retrying."
    else
      echo "Branch diverged from remote; rebasing local commits and retrying."
    fi
    git pull --rebase origin "$CURRENT_BRANCH"
  fi

done

echo "Push failed after ${max_attempts} attempts. Resolve any local errors and try again." >&2
exit 1
