#!/usr/bin/env bash

set -eu
set -o pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
MODE=${1:-dry-run}

usage() {
  cat <<'USAGE'
Usage:
  UPLOAD_KEYSTORE_PATH=/absolute/path/outside/repository/upload.jks \
  UPLOAD_KEY_ALIAS=gta-free-stem-upload \
    ./scripts/generate-upload-keystore.example.sh [--execute]

Default: dry run. No key or file is created.

--execute deliberately invokes keytool. Passwords and certificate identity are
requested interactively and are not accepted as command-line arguments.
USAGE
}

case "$MODE" in
  dry-run|--dry-run) ;;
  --execute) ;;
  -h|--help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac

if [ "$#" -gt 1 ]; then
  usage >&2
  exit 2
fi

: "${UPLOAD_KEYSTORE_PATH:?Set UPLOAD_KEYSTORE_PATH to an absolute path outside the repository}"
: "${UPLOAD_KEY_ALIAS:?Set UPLOAD_KEY_ALIAS to a non-secret key alias}"

case "$UPLOAD_KEYSTORE_PATH" in
  /*) ;;
  *)
    printf 'ERROR: UPLOAD_KEYSTORE_PATH must be absolute.\n' >&2
    exit 1
    ;;
esac

case "$UPLOAD_KEY_ALIAS" in
  *[!A-Za-z0-9._-]*|'')
    printf 'ERROR: UPLOAD_KEY_ALIAS may contain only letters, numbers, dot, underscore, and hyphen.\n' >&2
    exit 1
    ;;
esac

KEYSTORE_PARENT_INPUT=$(dirname -- "$UPLOAD_KEYSTORE_PATH")
if [ ! -d "$KEYSTORE_PARENT_INPUT" ]; then
  printf 'ERROR: destination directory does not exist: %s\n' "$KEYSTORE_PARENT_INPUT" >&2
  exit 1
fi
KEYSTORE_PARENT=$(CDPATH= cd -- "$KEYSTORE_PARENT_INPUT" && pwd -P)
KEYSTORE_NAME=$(basename -- "$UPLOAD_KEYSTORE_PATH")
RESOLVED_KEYSTORE_PATH="$KEYSTORE_PARENT/$KEYSTORE_NAME"

case "$RESOLVED_KEYSTORE_PATH" in
  "$PROJECT_ROOT"|"$PROJECT_ROOT"/*)
    printf 'ERROR: the upload keystore must be stored outside the repository.\n' >&2
    exit 1
    ;;
esac

if [ -e "$RESOLVED_KEYSTORE_PATH" ] || [ -L "$RESOLVED_KEYSTORE_PATH" ]; then
  printf 'ERROR: destination already exists; this helper never overwrites a key.\n' >&2
  exit 1
fi

if [ ! -w "$KEYSTORE_PARENT" ]; then
  printf 'ERROR: destination directory is not writable: %s\n' "$KEYSTORE_PARENT" >&2
  exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
  printf 'ERROR: keytool was not found. Install/select Java 17 first.\n' >&2
  exit 1
fi

if [ "$MODE" != "--execute" ]; then
  cat <<EOF
DRY RUN ONLY — no key was generated.

Destination: $RESOLVED_KEYSTORE_PATH
Alias:       $UPLOAD_KEY_ALIAS
Algorithm:   RSA 4096-bit
Validity:    10,000 days

The execute mode will:
  1. refuse to overwrite an existing destination;
  2. set a restrictive file-creation mask;
  3. let keytool prompt for passwords and certificate identity interactively;
  4. create no keystore.properties file and print no password.

After reviewing the release runbook, rerun the same command with --execute.
EOF
  exit 0
fi

if [ ! -t 0 ]; then
  printf 'ERROR: execute mode requires an interactive terminal for hidden password prompts.\n' >&2
  exit 1
fi

printf 'This will create a new private upload keystore at:\n  %s\n' "$RESOLVED_KEYSTORE_PATH"
printf 'It will not be recoverable unless you back it up. Type CREATE to continue: '
IFS= read -r confirmation
if [ "$confirmation" != "CREATE" ]; then
  printf 'Cancelled; no key was generated.\n'
  exit 1
fi

umask 077
keytool -genkeypair \
  -keystore "$RESOLVED_KEYSTORE_PATH" \
  -alias "$UPLOAD_KEY_ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000

chmod 600 "$RESOLVED_KEYSTORE_PATH"
printf 'Upload keystore created with permissions 600. Back it up securely and follow docs/ANDROID_RELEASE_RUNBOOK.md.\n'
