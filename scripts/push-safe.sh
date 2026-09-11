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
COMMIT_MESSAGE_ARGS=()
INCLUDE_UNTRACKED="false"

while [ "$#" -gt 0 ]; do
  case "$1" in
    --with-untracked)
      INCLUDE_UNTRACKED="true"
      shift
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    --*)
      print_usage
      echo "Unsupported option: $1" >&2
      exit 2
      ;;
    *)
      COMMIT_MESSAGE_ARGS+=("$1")
      shift
      while [ "$#" -gt 0 ]; do
        COMMIT_MESSAGE_ARGS+=("$1")
        shift
      done
      COMMIT_MESSAGE="${COMMIT_MESSAGE_ARGS[*]}"
      ;;
  esac
done

if [ "${#COMMIT_MESSAGE_ARGS[@]}" -eq 0 ]; then
  COMMIT_MESSAGE=""
fi

if [ -z "$COMMIT_MESSAGE" ]; then
  COMMIT_MESSAGE="chore: checkpoint $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
fi

has_changes() {
  [ -n "$(git status --short --untracked-files=normal)" ]
}

has_staged_changes() {
  [ -n "$(git diff --cached --name-only)" ]
}

ensure_git_identity() {
  local git_name
  local git_email

  git_name="$(git config --get user.name || true)"
  git_email="$(git config --get user.email || true)"

  if [ -z "$git_name" ] || [ -z "$git_email" ]; then
    echo "Cannot create commits without git user identity." >&2
    echo "Set it once with:
      git config user.name "Your Name"
      git config user.email "you@example.com"" >&2
    exit 1
  fi
}

ensure_no_in_progress_git_op() {
  if [ -d "$PROJECT_ROOT/.git/rebase-apply" ] || [ -d "$PROJECT_ROOT/.git/rebase-merge" ] ||
     [ -f "$PROJECT_ROOT/.git/MERGE_HEAD" ] ||
     [ -f "$PROJECT_ROOT/.git/CHERRY_PICK_HEAD" ] || [ -f "$PROJECT_ROOT/.git/BISECT_LOG" ]; then
    echo "Repository is mid-operation (rebase/merge/cherry-pick/revert/bisect)." >&2
    echo "Finish or abort that operation before running push-safe." >&2
    exit 1
  fi
}

cleanup_stale_rebase_marker() {
  if [ -f "$PROJECT_ROOT/.git/REBASE_HEAD" ] &&
     ! [ -d "$PROJECT_ROOT/.git/rebase-apply" ] &&
     ! [ -d "$PROJECT_ROOT/.git/rebase-merge" ]; then
    echo "Removing stale .git/REBASE_HEAD marker before push." 
    rm -f "$PROJECT_ROOT/.git/REBASE_HEAD"
  fi
}

has_upstream_config() {
  [ -n "$(git config --get "branch.${CURRENT_BRANCH}.remote")" ] && \
    [ -n "$(git config --get "branch.${CURRENT_BRANCH}.merge")" ]
}

remote_head_sha() {
  git ls-remote --heads "$UPSTREAM_REMOTE" "$TARGET_PUSH_BRANCH" 2>/dev/null | awk 'NF==2 {print $1; exit}'
}

cleanup_stale_locks() {
  local lock_file
  local lock_files=(
    "$PROJECT_ROOT/.git/index.lock"
    "$PROJECT_ROOT/.git/FETCH_HEAD.lock"
    "$PROJECT_ROOT/.git/HEAD.lock"
    "$PROJECT_ROOT/.git/config.lock"
    "$PROJECT_ROOT/.git/ORIG_HEAD.lock"
    "$PROJECT_ROOT/.git/packed-refs.lock"
  )

  for lock_file in "${lock_files[@]}"; do
    if [ -f "$lock_file" ]; then
      echo "Removing stale lock file: $lock_file"
      rm -f "$lock_file"
    fi
  done
}

if [ ! -d .git ]; then
  echo "Not a git repository: $PROJECT_ROOT" >&2
  exit 1
fi

cleanup_stale_locks

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
TARGET_PUSH_BRANCH="$CURRENT_BRANCH"

if [ "$CURRENT_BRANCH" != "$UPSTREAM_BRANCH" ]; then
  echo "Notice: ${CURRENT_BRANCH} is configured to track ${UPSTREAM_REF}."
  if git ls-remote --heads "$UPSTREAM_REMOTE" "$CURRENT_BRANCH" | grep -q "refs/heads/$CURRENT_BRANCH$"; then
    echo "Pushing will target existing remote branch ${UPSTREAM_REMOTE}/${CURRENT_BRANCH}."
  else
    echo "Remote branch ${UPSTREAM_REMOTE}/${CURRENT_BRANCH} does not exist; it will be created on push."
  fi
fi

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

is_network_or_repo_busy_failure() {
  echo "$1" | grep -qiE "could not resolve host|Failed to connect to|network is unreachable|Connection timed out|RPC failed|remote hung up|The requested URL returned error|Unable to access|Connection refused|timeout|unable to access" 
}

is_auth_failure() {
  echo "$1" | grep -qiE "authentication|Authentication failed|Permission denied|could not read Username|403|access denied|remote: Permission to .*denied"
}

is_protection_or_permission_block() {
  echo "$1" | grep -qiE "GH00[0-9]|protected branch|not allowed to update refs|Update was rejected|pre-receive hook declined|remote: error: GH"
}

fetch_with_retry() {
  local remote="$1"
  local attempt=0
  local max_attempts=5
  local delay=1

  while [ "$attempt" -lt "$max_attempts" ]; do
    attempt=$((attempt + 1))

    local fetch_output
    local fetch_status=0

    fetch_output="$(git fetch --prune --quiet "$remote" 2>&1)"
    fetch_status=$?
    if [ "$fetch_status" -eq 0 ]; then
      return 0
    fi

    echo "Fetch attempt ${attempt}/${max_attempts} failed."
    echo "$fetch_output"

    if is_auth_failure "$fetch_output"; then
      echo "Detected authentication/permission failure while fetching. This requires manual fix." >&2
      return 1
    fi

    if [ "$attempt" -ge "$max_attempts" ]; then
      echo "Fetch failed after ${max_attempts} attempts." >&2
      return 1
    fi

    if is_network_or_repo_busy_failure "$fetch_output"; then
      echo "Detected transient fetch/network issue. Retrying in ${delay}s."
      sleep "$delay"
      delay=$((delay * 2))
      if [ "$delay" -gt 16 ]; then
        delay=16
      fi
      continue
    fi

    # Unknown failure: do not retry blindly.
    echo "Aborting fetch retries due unrecognized fetch failure." >&2
    return 1
  done
}

verify_remote_contains_head() {
  local local_sha=$1
  local remote_sha
  remote_sha=$(remote_head_sha || true)

  if [ -z "$remote_sha" ]; then
    return 1
  fi

  if [ "$local_sha" = "$remote_sha" ]; then
    return 0
  fi

  if git fetch --dry-run "$UPSTREAM_REMOTE" "$UPSTREAM_BRANCH" >/dev/null 2>&1; then
    if git merge-base --is-ancestor "$local_sha" "$UPSTREAM_REMOTE/$TARGET_PUSH_BRANCH"; then
      return 0
    fi
  fi

  return 1
}

echo "Syncing branch ${CURRENT_BRANCH} with ${UPSTREAM_REF}"
cleanup_stale_rebase_marker
ensure_git_identity
ensure_no_in_progress_git_op
  if ! fetch_with_retry "$UPSTREAM_REMOTE"; then
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
max_attempts=5
retry_delay=1

while [ "$attempt" -lt "$max_attempts" ]; do
  attempt=$((attempt + 1))
  echo "Push attempt ${attempt}/${max_attempts}"

  if has_upstream_config; then
    push_output="$(git push "$UPSTREAM_REMOTE" "${CURRENT_BRANCH}:${TARGET_PUSH_BRANCH}" 2>&1 || true)"
  else
    echo "No upstream branch configured for ${CURRENT_BRANCH}; creating tracked upstream on push."
    push_output="$(git push -u "$UPSTREAM_REMOTE" "$CURRENT_BRANCH" 2>&1 || true)"
  fi
  push_exit_code=$?

  if [ "$push_exit_code" -eq 0 ]; then
    echo "$push_output"
    echo "Push succeeded."

    LOCAL_AFTER=$(git rev-parse "$CURRENT_BRANCH")
    if fetch_with_retry "$UPSTREAM_REMOTE"; then
      if verify_remote_contains_head "$LOCAL_AFTER"; then
        echo "Verified remote includes local HEAD $(git rev-parse --short "$CURRENT_BRANCH")"
        exit 0
      fi
      echo "Push output reported success, but remote verification could not confirm local HEAD."
      echo "Please verify manually: git fetch && git log origin/${CURRENT_BRANCH} --oneline -n 3"
      exit 1
    fi

    echo "Push reported success, but fetch for verification failed."
    echo "Please retry once network is back: ./scripts/push-safe.sh \"$COMMIT_MESSAGE\""
    exit 1
  fi

  echo "Push attempt ${attempt}/${max_attempts} failed with exit code ${push_exit_code}."
  echo "$push_output"

  if echo "$push_output" | grep -qiE "fatal: Not possible to fast-forward|non-fast-forward|Updates were rejected|failed to push|rejected"; then
    echo "Detected push rejection. Re-syncing before retry."
  elif is_auth_failure "$push_output"; then
    echo "Detected authentication/permission failure. This requires manual fix before retry."
    exit 1
  elif is_protection_or_permission_block "$push_output"; then
    echo "Detected repository protection or server-side permission block. Resolve branch protections/hooks first."
    exit 1
  elif is_network_or_repo_busy_failure "$push_output"; then
    echo "Detected transient network/repository access issue. Retrying after sync and backoff."
  else
    echo "Unknown push failure; retrying after sync."
  fi

  if [ "$attempt" -ge "$max_attempts" ]; then
    break
  fi

  echo "Waiting ${retry_delay}s before retry."
  sleep "$retry_delay"
  retry_delay=$((retry_delay * 2))
  if [ "$retry_delay" -gt 16 ]; then
    retry_delay=16
  fi

  if ! fetch_with_retry "$UPSTREAM_REMOTE"; then
    echo "Network/fetch failure after push attempt ${attempt}; retrying." >&2
    continue
  fi

  if ! sync_with_upstream; then
    exit 1
  fi
done

echo "Push failed after ${max_attempts} attempts. Resolve any local errors and try again." >&2
exit 1
