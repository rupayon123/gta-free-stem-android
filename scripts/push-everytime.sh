#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)

cd "$PROJECT_ROOT"

# Keep backward-compatibility with earlier usage; delegate all heavy lifting to push-safe.
"$SCRIPT_DIR/push-safe.sh" "$@"
