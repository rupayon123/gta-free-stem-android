#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
cd "$PROJECT_ROOT"

print_usage() {
  printf 'Usage: %s [--with-untracked] [commit-message]\n' "$(basename "$0")"
  printf 'Usage: %s --with-untracked "Refine nav tint contrast"\n' "$(basename "$0")"
  printf '\n'
  printf 'Tracked and untracked file changes are included by default.\n'
  printf 'If you do not pass a commit message, one is auto-generated from UTC time.\n'
  printf 'Use --with-untracked only for legacy callers; it is currently a no-op.\n'
}

COMMIT_MESSAGE=""
INCLUDE_UNTRACKED=${2:-}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --with-untracked)
      INCLUDE_UNTRACKED="--with-untracked"
      shift
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      if [ -n "$COMMIT_MESSAGE" ]; then
        print_usage
        echo "Too many arguments provided." >&2
        exit 2
      fi
      COMMIT_MESSAGE="$1"
      shift
      ;;
  esac
done

if [ -z "$COMMIT_MESSAGE" ]; then
  COMMIT_MESSAGE="chore: checkpoint $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
fi

if [ -n "$INCLUDE_UNTRACKED" ] && [ "$INCLUDE_UNTRACKED" != "--with-untracked" ]; then
  echo "Unsupported option: $INCLUDE_UNTRACKED" >&2
  print_usage
  exit 2
fi

has_changes() {
  [ -n "$(git status --short --untracked-files=normal)" ]
}

has_staged_changes() {
  [ -n "$(git diff --cached --name-only)" ]
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

UPSTREAM_REF=$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)
if [ -z "$UPSTREAM_REF" ]; then
  UPSTREAM_REF="origin/$CURRENT_BRANCH"
  echo "No upstream branch is configured for ${CURRENT_BRANCH}; defaulting to ${UPSTREAM_REF}."
fi

if [ -z "${UPSTREAM_REF#*/}" ]; then
  echo "Malformed upstream ref: ${UPSTREAM_REF}" >&2
  exit 1
fi

UPSTREAM_REMOTE=${UPSTREAM_REF%%/*}
UPSTREAM_BRANCH=${UPSTREAM_REF#*/}
UPSTREAM_PUSH_REF="${UPSTREAM_REF}"

if ! git remote get-url "$UPSTREAM_REMOTE" >/dev/null 2>&1; then
  echo "Configured remote '$UPSTREAM_REMOTE' is required for push-safe" >&2
  exit 1
fi

sync_with_upstream() {
  if ! git show-ref --verify --quiet "refs/remotes/$UPSTREAM_REF"; then
    return 0
  fi

  local_ahead=$(git rev-list --count "${UPSTREAM_REF}..${CURRENT_BRANCH}" || echo 0)
  remote_ahead=$(git rev-list --count "${CURRENT_BRANCH}..${UPSTREAM_REF}" || echo 0)

  if [ "$remote_ahead" -gt 0 ] && [ "$local_ahead" -gt 0 ]; then
    echo "Branch is diverged from ${UPSTREAM_REF}; rebasing to keep history linear."
    if ! git pull --rebase --autostash "$UPSTREAM_REMOTE" "$UPSTREAM_BRANCH"; then
      echo "Rebase failed while resolving divergence. Resolve conflicts and rerun." >&2
      return 1
    fi
  elif [ "$remote_ahead" -gt 0 ]; then
    echo "Branch is behind ${UPSTREAM_REF}; rebasing onto remote."
    if ! git pull --rebase --autostash "$UPSTREAM_REMOTE" "$UPSTREAM_BRANCH"; then
      echo "Rebase failed while syncing from ${UPSTREAM_REF}. Resolve conflicts and rerun." >&2
      return 1
    fi
  fi
}

echo "Syncing branch ${CURRENT_BRANCH} with ${UPSTREAM_REF}"
if ! git fetch --prune --quiet "$UPSTREAM_REMOTE"; then
  echo "Could not fetch from ${UPSTREAM_REMOTE} before syncing. Check network/auth first." >&2
  exit 1
fi

if ! sync_with_upstream; then
  exit 1
fi

if has_changes; then
  echo "Staging tracked + untracked changes for commit."
  git add -A
  if ! has_staged_changes; then
    echo "No changes were staged." >&2
    exit 1
  fi
  git commit -m "$COMMIT_MESSAGE"
else
  echo "No changes detected; nothing new to commit."
fi

attempt=0
max_attempts=3

while [ "$attempt" -lt "$max_attempts" ]; do
  attempt=$((attempt + 1))
  echo "Push attempt ${attempt}/${max_attempts}"

  if git push "$UPSTREAM_REMOTE" "${CURRENT_BRANCH}:${UPSTREAM_BRANCH}"; then
    echo "Push succeeded."

    if ! git fetch --prune --quiet "$UPSTREAM_REMOTE"; then
      echo "Push succeeded, but refresh failed; remote confirmation will be skipped." >&2
      exit 2
    fi

    LOCAL_AFTER=$(git rev-parse "$CURRENT_BRANCH")
    REMOTE_AFTER=$(git rev-parse "$UPSTREAM_PUSH_REF")
    if [ "$LOCAL_AFTER" = "$REMOTE_AFTER" ]; then
      echo "Verified remote includes local HEAD $(git rev-parse --short HEAD)"
      exit 0
    fi

    echo "Push returned success, but remote HEAD differs. Investigate immediately." >&2
    exit 2
  fi

  echo "Push attempt ${attempt}/${max_attempts} failed. Fetching and retrying."
  if ! git fetch --prune "$UPSTREAM_REMOTE"; then
    echo "Network/fetch failure after push attempt ${attempt}; retrying." >&2
    continue
  fi

  if ! sync_with_upstream; then
    exit 1
  fi
done

echo "Push failed after ${max_attempts} attempts. Resolve any local errors and try again." >&2
exit 1
