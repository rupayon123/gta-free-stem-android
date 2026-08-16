#!/usr/bin/env bash

set -eu
set -o pipefail
set +x

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)

KEYCHAIN_SERVICE="com.rupayonhaldar.gtafreestem.upload-keystore"
KEYCHAIN_STORE_PASSWORD_ACCOUNT="store-password"
KEYCHAIN_KEY_PASSWORD_ACCOUNT="key-password"
DEFAULT_KEYSTORE_PATH="${HOME:?HOME must be set}/Library/Application Support/GTAFreeSTEM/signing/gta-free-stem-upload.jks"

usage() {
  cat <<EOF
Usage: ./scripts/build-signed-release-from-keychain.sh [Gradle task ...]

Builds a signed release without putting passwords in source files or shell
arguments. With no task arguments, it runs:
  clean testDebugUnitTest lintRelease bundleRelease

Keychain service: $KEYCHAIN_SERVICE
Store-password account: $KEYCHAIN_STORE_PASSWORD_ACCOUNT
Key-password account: $KEYCHAIN_KEY_PASSWORD_ACCOUNT

Default keystore: $DEFAULT_KEYSTORE_PATH
Override the non-secret defaults with GTA_UPLOAD_KEYSTORE_PATH and
GTA_UPLOAD_KEY_ALIAS. The two GTA_UPLOAD_*_PASSWORD values are always read from
the named Keychain items and are never accepted from the caller.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

if [ "$(uname -s)" != "Darwin" ] || [ ! -x /usr/bin/security ]; then
  printf 'ERROR: this wrapper requires macOS and /usr/bin/security.\n' >&2
  exit 1
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  printf 'ERROR: set JAVA_HOME to a working Java 17 installation before running this wrapper.\n' >&2
  exit 1
fi

PATH="$JAVA_HOME/bin:$PATH"
export PATH

GTA_UPLOAD_KEYSTORE_PATH=${GTA_UPLOAD_KEYSTORE_PATH:-$DEFAULT_KEYSTORE_PATH}
GTA_UPLOAD_KEY_ALIAS=${GTA_UPLOAD_KEY_ALIAS:-gta-free-stem-upload}

case "$GTA_UPLOAD_KEYSTORE_PATH" in
  /*) ;;
  *)
    printf 'ERROR: GTA_UPLOAD_KEYSTORE_PATH must be absolute.\n' >&2
    exit 1
    ;;
esac

case "$GTA_UPLOAD_KEY_ALIAS" in
  *[!A-Za-z0-9._-]*|'')
    printf 'ERROR: GTA_UPLOAD_KEY_ALIAS may contain only letters, numbers, dot, underscore, and hyphen.\n' >&2
    exit 1
    ;;
esac

if [ -L "$GTA_UPLOAD_KEYSTORE_PATH" ]; then
  printf 'ERROR: the upload keystore must not be a symbolic link.\n' >&2
  exit 1
elif [ ! -f "$GTA_UPLOAD_KEYSTORE_PATH" ]; then
  printf 'ERROR: the upload keystore does not exist at the configured path.\n' >&2
  exit 1
fi

if ! GTA_UPLOAD_STORE_PASSWORD=$(
  /usr/bin/security find-generic-password \
    -s "$KEYCHAIN_SERVICE" \
    -a "$KEYCHAIN_STORE_PASSWORD_ACCOUNT" \
    -w 2>/dev/null
); then
  printf 'ERROR: the store-password Keychain item is missing or inaccessible.\n' >&2
  exit 1
fi

if ! GTA_UPLOAD_KEY_PASSWORD=$(
  /usr/bin/security find-generic-password \
    -s "$KEYCHAIN_SERVICE" \
    -a "$KEYCHAIN_KEY_PASSWORD_ACCOUNT" \
    -w 2>/dev/null
); then
  unset GTA_UPLOAD_STORE_PASSWORD
  printf 'ERROR: the key-password Keychain item is missing or inaccessible.\n' >&2
  exit 1
fi

if [ -z "$GTA_UPLOAD_STORE_PASSWORD" ] || [ -z "$GTA_UPLOAD_KEY_PASSWORD" ]; then
  unset GTA_UPLOAD_STORE_PASSWORD GTA_UPLOAD_KEY_PASSWORD
  printf 'ERROR: a required Keychain password is empty.\n' >&2
  exit 1
fi

cleanup() {
  unset GTA_UPLOAD_KEYSTORE_PATH GTA_UPLOAD_STORE_PASSWORD GTA_UPLOAD_KEY_ALIAS GTA_UPLOAD_KEY_PASSWORD
}
trap cleanup EXIT

export GTA_UPLOAD_KEYSTORE_PATH
export GTA_UPLOAD_STORE_PASSWORD
export GTA_UPLOAD_KEY_ALIAS
export GTA_UPLOAD_KEY_PASSWORD

cd "$PROJECT_ROOT"
REQUIRE_SIGNING=1 ./scripts/verify-release-config.sh

if [ "$#" -eq 0 ]; then
  set -- clean testDebugUnitTest lintRelease bundleRelease
fi

./gradlew --no-daemon --stacktrace "$@"
