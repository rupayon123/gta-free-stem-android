#!/usr/bin/env bash

set -u
set -o pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
BUILD_FILE="$PROJECT_ROOT/app/build.gradle.kts"
MANIFEST_FILE="$PROJECT_ROOT/app/src/main/AndroidManifest.xml"
IGNORE_FILE="$PROJECT_ROOT/.gitignore"
BACKUP_RULES_FILE="$PROJECT_ROOT/app/src/main/res/xml/backup_rules.xml"
DATA_EXTRACTION_RULES_FILE="$PROJECT_ROOT/app/src/main/res/xml/data_extraction_rules.xml"
KEYCHAIN_BUILD_WRAPPER="$PROJECT_ROOT/scripts/build-signed-release-from-keychain.sh"

EXPECTED_APPLICATION_ID=${EXPECTED_APPLICATION_ID:-com.rupayonhaldar.gtafreestem}
EXPECTED_COMPILE_SDK=${EXPECTED_COMPILE_SDK:-36}
EXPECTED_TARGET_SDK=${EXPECTED_TARGET_SDK:-36}
EXPECTED_MIN_SDK=${EXPECTED_MIN_SDK:-26}
EXPECTED_VERSION_CODE=${EXPECTED_VERSION_CODE:-2}
EXPECTED_VERSION_NAME=${EXPECTED_VERSION_NAME:-1.0.1}
REQUIRE_SIGNING=${REQUIRE_SIGNING:-0}

failures=0
warnings=0

pass() {
  printf 'PASS: %s\n' "$1"
}

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  failures=$((failures + 1))
}

warn() {
  printf 'WARN: %s\n' "$1" >&2
  warnings=$((warnings + 1))
}

extract_value() {
  key=$1
  file=$2
  awk -v wanted="$key" '
    $1 == wanted && $2 == "=" {
      value = $3
      gsub(/^"|"$/, "", value)
      print value
      exit
    }
  ' "$file"
}

check_value() {
  label=$1
  key=$2
  expected=$3
  actual=$(extract_value "$key" "$BUILD_FILE")

  if [ -z "$actual" ]; then
    fail "$label is missing from app/build.gradle.kts"
  elif [ "$actual" = "$expected" ]; then
    pass "$label is $expected"
  else
    fail "$label is $actual; expected $expected"
  fi
}

has_ignore_rule() {
  rule=$1
  grep -Fqx "$rule" "$IGNORE_FILE"
}

property_is_present() {
  property=$1
  file=$2
  awk -F= -v wanted="$property" '
    /^[[:space:]]*#/ { next }
    {
      key = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key == wanted && NF > 1) {
        value = substr($0, index($0, "=") + 1)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        if (value != "" && value !~ /^REPLACE_/) {
          found = 1
        }
      }
    }
    END { exit(found ? 0 : 1) }
  ' "$file"
}

read_property() {
  property=$1
  file=$2
  awk -F= -v wanted="$property" '
    /^[[:space:]]*#/ { next }
    {
      key = $1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", key)
      if (key == wanted) {
        value = substr($0, index($0, "=") + 1)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        print value
        exit
      }
    }
  ' "$file"
}

file_mode() {
  file=$1
  if stat -f '%Lp' "$file" >/dev/null 2>&1; then
    stat -f '%Lp' "$file"
  elif stat -c '%a' "$file" >/dev/null 2>&1; then
    stat -c '%a' "$file"
  else
    printf 'unknown\n'
  fi
}

printf 'GTA FREE STEM Android release configuration check\n'
printf 'Project: %s\n\n' "$PROJECT_ROOT"

for required_file in \
  "$BUILD_FILE" \
  "$MANIFEST_FILE" \
  "$IGNORE_FILE" \
  "$KEYCHAIN_BUILD_WRAPPER" \
  "$PROJECT_ROOT/settings.gradle.kts" \
  "$PROJECT_ROOT/gradlew" \
  "$PROJECT_ROOT/gradle/wrapper/gradle-wrapper.properties"
do
  if [ -f "$required_file" ]; then
    pass "found ${required_file#"$PROJECT_ROOT"/}"
  else
    fail "missing ${required_file#"$PROJECT_ROOT"/}"
  fi
done

if [ ! -f "$BUILD_FILE" ]; then
  printf '\nFAILED: cannot inspect a missing app/build.gradle.kts\n' >&2
  exit 1
fi

check_value "applicationId" "applicationId" "$EXPECTED_APPLICATION_ID"
check_value "compileSdk" "compileSdk" "$EXPECTED_COMPILE_SDK"
check_value "targetSdk" "targetSdk" "$EXPECTED_TARGET_SDK"
check_value "minSdk" "minSdk" "$EXPECTED_MIN_SDK"
check_value "versionCode" "versionCode" "$EXPECTED_VERSION_CODE"
check_value "versionName" "versionName" "$EXPECTED_VERSION_NAME"

if grep -Fq 'JavaVersion.VERSION_17' "$BUILD_FILE" && \
   grep -Eq 'jvmToolchain\([[:space:]]*17[[:space:]]*\)' "$BUILD_FILE"; then
  pass "Java and Kotlin toolchains target 17"
else
  fail "Java and Kotlin toolchains must both target 17"
fi

if grep -Eq 'keystorePropertiesFile.*keystore\.properties' "$BUILD_FILE" && \
   grep -Fq 'keystorePropertiesFile.isFile' "$BUILD_FILE" && \
   grep -Eq 'signingConfig[[:space:]]*=[[:space:]]*releaseSigning' "$BUILD_FILE"; then
  pass "release signing is conditionally loaded from local keystore.properties"
else
  fail "release signing must be local and conditionally gated by keystore.properties"
fi

environment_signing_source_ok=1
for environment_key in \
  GTA_UPLOAD_KEYSTORE_PATH \
  GTA_UPLOAD_STORE_PASSWORD \
  GTA_UPLOAD_KEY_ALIAS \
  GTA_UPLOAD_KEY_PASSWORD
do
  if ! grep -Fq "\"$environment_key\"" "$BUILD_FILE"; then
    environment_signing_source_ok=0
  fi
done
if [ "$environment_signing_source_ok" -eq 1 ] && \
   grep -Fq 'signingEnvironmentConfigured' "$BUILD_FILE"; then
  pass "release signing accepts the complete GTA_UPLOAD_* environment contract"
else
  fail "release signing is missing the complete GTA_UPLOAD_* environment contract"
fi

if grep -Fq 'com.rupayonhaldar.gtafreestem.upload-keystore' "$KEYCHAIN_BUILD_WRAPPER" && \
   grep -Fq 'KEYCHAIN_STORE_PASSWORD_ACCOUNT="store-password"' "$KEYCHAIN_BUILD_WRAPPER" && \
   grep -Fq 'KEYCHAIN_KEY_PASSWORD_ACCOUNT="key-password"' "$KEYCHAIN_BUILD_WRAPPER" && \
   grep -Fq 'security find-generic-password' "$KEYCHAIN_BUILD_WRAPPER"; then
  pass "Keychain wrapper uses the documented service and password accounts"
else
  fail "Keychain wrapper does not match the documented service/account contract"
fi

if grep -Eq 'android:usesCleartextTraffic[[:space:]]*=[[:space:]]*"false"' "$MANIFEST_FILE"; then
  pass "source manifest explicitly disables cleartext traffic"
else
  fail "source manifest must explicitly set android:usesCleartextTraffic to false"
fi

backup_rules_ok=1
for domain in root file database sharedpref external; do
  if ! grep -Fq "<exclude domain=\"$domain\" path=\".\" />" "$BACKUP_RULES_FILE" || \
     [ "$(grep -Fc "<exclude domain=\"$domain\" path=\".\" />" "$DATA_EXTRACTION_RULES_FILE")" -lt 2 ]; then
    backup_rules_ok=0
  fi
done

if grep -Eq 'android:allowBackup[[:space:]]*=[[:space:]]*"false"' "$MANIFEST_FILE" && \
   grep -Eq 'android:fullBackupContent[[:space:]]*=[[:space:]]*"@xml/backup_rules"' "$MANIFEST_FILE" && \
   grep -Eq 'android:dataExtractionRules[[:space:]]*=[[:space:]]*"@xml/data_extraction_rules"' "$MANIFEST_FILE" && \
   [ "$backup_rules_ok" -eq 1 ]; then
  pass "source manifest and extraction rules disable Android backup and device transfer"
else
  fail "source manifest and extraction rules must exclude local-only app data from backup and transfer"
fi

if grep -Eq '<uses-permission[[:space:]][^>]*android:name="android\.permission\.INTERNET"' "$MANIFEST_FILE"; then
  pass "source manifest declares the INTERNET permission required for the HTTPS feed"
else
  fail "source manifest is missing android.permission.INTERNET"
fi

unexpected_permissions=$(sed -nE 's/.*<uses-permission[^>]*android:name="([^"]+)".*/\1/p' "$MANIFEST_FILE" | \
  grep -Fvx 'android.permission.INTERNET' || true)
if [ -n "$unexpected_permissions" ]; then
  fail "source manifest declares a permission beyond the expected INTERNET permission"
else
  pass "source manifest declares no unexpected permissions"
fi

if find "$PROJECT_ROOT/app/src/main" -type f -name '*.kt' -exec grep -Il 'http://' {} + 2>/dev/null | grep -q .; then
  fail "Kotlin source contains at least one cleartext http:// URL"
else
  pass "Kotlin source contains no cleartext http:// URL"
fi

for ignore_rule in 'keystore.properties' '*.jks' '*.keystore' '*.p12' '*.pfx' '*.key' '*-private.pem' 'private*.pem'; do
  if has_ignore_rule "$ignore_rule"; then
    pass ".gitignore protects $ignore_rule"
  else
    fail ".gitignore is missing the $ignore_rule rule"
  fi
done

found_private_key=$(find "$PROJECT_ROOT" \
  -path "$PROJECT_ROOT/.git" -prune -o \
  -path "$PROJECT_ROOT/.gradle" -prune -o \
  -path "$PROJECT_ROOT/app/build" -prune -o \
  -type f \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pfx' -o -name '*.key' \) \
  -print -quit)
if [ -n "$found_private_key" ]; then
  fail "a private signing/key file exists inside the project tree; move it outside the repository"
else
  pass "no keystore, PKCS#12, PFX, or .key file exists inside the project tree"
fi

found_private_pem=$(find "$PROJECT_ROOT" \
  -path "$PROJECT_ROOT/.git" -prune -o \
  -path "$PROJECT_ROOT/.gradle" -prune -o \
  -path "$PROJECT_ROOT/app/build" -prune -o \
  -type f -name '*.pem' -exec grep -IlE -- '-----BEGIN (ENCRYPTED |RSA |EC )?PRIVATE KEY-----' {} + 2>/dev/null | head -n 1)
if [ -n "$found_private_pem" ]; then
  fail "a private PEM key exists inside the project tree; move it outside the repository"
else
  pass "no private PEM key exists inside the project tree"
fi

if git -C "$PROJECT_ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  tracked_secrets=$(git -C "$PROJECT_ROOT" ls-files | grep -E '(^|/)keystore\.properties$|\.(jks|keystore|p12|pfx|key)$' || true)
  if [ -n "$tracked_secrets" ]; then
    fail "Git tracks a keystore properties/key file"
  else
    pass "Git tracks no keystore properties/key files"
  fi
  tracked_private_pem=$(git -C "$PROJECT_ROOT" grep -IlE -- '-----BEGIN (ENCRYPTED |RSA |EC )?PRIVATE KEY-----' -- '*.pem' 2>/dev/null | head -n 1 || true)
  if [ -n "$tracked_private_pem" ]; then
    fail "Git tracks a private PEM key"
  else
    pass "Git tracks no private PEM key"
  fi
else
  warn "project is not currently inside a Git worktree; tracked-secret verification was skipped"
fi

case "$REQUIRE_SIGNING" in
  0|1) ;;
  *)
    fail "REQUIRE_SIGNING must be 0 or 1"
    REQUIRE_SIGNING=0
    ;;
esac

KEYSTORE_PROPERTIES="$PROJECT_ROOT/keystore.properties"
environment_signing_count=0
[ -n "${GTA_UPLOAD_KEYSTORE_PATH:-}" ] && environment_signing_count=$((environment_signing_count + 1))
[ -n "${GTA_UPLOAD_STORE_PASSWORD:-}" ] && environment_signing_count=$((environment_signing_count + 1))
[ -n "${GTA_UPLOAD_KEY_ALIAS:-}" ] && environment_signing_count=$((environment_signing_count + 1))
[ -n "${GTA_UPLOAD_KEY_PASSWORD:-}" ] && environment_signing_count=$((environment_signing_count + 1))

if [ "$environment_signing_count" -gt 0 ]; then
  if [ "$environment_signing_count" -ne 4 ]; then
    fail "environment-backed signing is incomplete; set all four GTA_UPLOAD_* variables"
  else
    environment_ok=1
    case "$GTA_UPLOAD_KEYSTORE_PATH" in
      /*) ;;
      *)
        fail "GTA_UPLOAD_KEYSTORE_PATH must be absolute and outside the repository"
        environment_ok=0
        ;;
    esac
    case "$GTA_UPLOAD_KEY_ALIAS" in
      *[!A-Za-z0-9._-]*|'')
        fail "GTA_UPLOAD_KEY_ALIAS contains unsupported characters"
        environment_ok=0
        ;;
    esac

    if [ "$environment_ok" -eq 1 ]; then
      if [ -L "$GTA_UPLOAD_KEYSTORE_PATH" ]; then
        fail "environment-configured upload keystore must not be a symbolic link"
        environment_ok=0
      elif [ ! -f "$GTA_UPLOAD_KEYSTORE_PATH" ]; then
        fail "the environment-configured upload keystore does not exist"
        environment_ok=0
      else
        environment_store_parent=$(CDPATH= cd -- "$(dirname -- "$GTA_UPLOAD_KEYSTORE_PATH")" && pwd -P)
        environment_store_file="$environment_store_parent/$(basename -- "$GTA_UPLOAD_KEYSTORE_PATH")"
        case "$environment_store_file" in
          "$PROJECT_ROOT"|"$PROJECT_ROOT"/*)
            fail "environment-configured upload keystore resolves inside the repository"
            environment_ok=0
            ;;
        esac

        environment_store_mode=$(file_mode "$environment_store_file")
        if [ "$environment_store_mode" = "600" ]; then
          pass "environment-configured upload keystore permissions are 600"
        elif [ "$environment_store_mode" = "unknown" ]; then
          warn "could not determine environment-configured upload-keystore permissions"
        else
          fail "environment-configured upload keystore permissions are $environment_store_mode; expected 600"
          environment_ok=0
        fi
      fi
    fi

    if [ "$environment_ok" -eq 1 ]; then
      pass "environment-backed signing values and upload-keystore path passed structural checks"
      warn "this script does not print or cryptographically verify the private key; confirm its certificate fingerprint separately"
    fi
  fi
elif [ -e "$KEYSTORE_PROPERTIES" ] || [ -L "$KEYSTORE_PROPERTIES" ]; then
  if [ -L "$KEYSTORE_PROPERTIES" ]; then
    fail "keystore.properties must not be a symbolic link"
  elif [ ! -f "$KEYSTORE_PROPERTIES" ]; then
    fail "keystore.properties is not a regular file"
  else
    properties_ok=1
    for property in storeFile storePassword keyAlias keyPassword; do
      if ! property_is_present "$property" "$KEYSTORE_PROPERTIES"; then
        fail "keystore.properties is missing a non-placeholder $property value"
        properties_ok=0
      fi
    done

    mode=$(file_mode "$KEYSTORE_PROPERTIES")
    if [ "$mode" = "600" ]; then
      pass "keystore.properties permissions are 600"
    elif [ "$mode" = "unknown" ]; then
      warn "could not determine keystore.properties permissions"
    else
      fail "keystore.properties permissions are $mode; expected 600"
      properties_ok=0
    fi

    if [ "$properties_ok" -eq 1 ]; then
      store_file=$(read_property storeFile "$KEYSTORE_PROPERTIES")
      case "$store_file" in
        /*) ;;
        *)
          fail "storeFile must be an absolute path outside the repository"
          properties_ok=0
          ;;
      esac

      if [ -L "$store_file" ]; then
        fail "upload keystore must not be a symbolic link"
        properties_ok=0
      elif [ ! -f "$store_file" ]; then
        fail "the configured upload keystore does not exist"
        properties_ok=0
      else
        store_parent=$(CDPATH= cd -- "$(dirname -- "$store_file")" && pwd -P)
        resolved_store_file="$store_parent/$(basename -- "$store_file")"
        case "$resolved_store_file" in
          "$PROJECT_ROOT"|"$PROJECT_ROOT"/*)
            fail "storeFile resolves inside the repository"
            properties_ok=0
            ;;
        esac

        store_mode=$(file_mode "$resolved_store_file")
        if [ "$store_mode" = "600" ]; then
          pass "upload keystore permissions are 600"
        elif [ "$store_mode" = "unknown" ]; then
          warn "could not determine upload-keystore permissions"
        else
          fail "upload keystore permissions are $store_mode; expected 600"
          properties_ok=0
        fi
      fi
    fi

    if [ "$properties_ok" -eq 1 ]; then
      pass "local signing properties and upload-keystore path passed structural checks"
      warn "this script does not print or cryptographically verify the private key; confirm its certificate fingerprint separately"
    fi
  fi
elif [ "$REQUIRE_SIGNING" -eq 1 ]; then
  fail "REQUIRE_SIGNING=1 but neither complete GTA_UPLOAD_* values nor local keystore.properties are present"
else
  warn "local signing secrets are absent, as expected for CI; rerun with REQUIRE_SIGNING=1 on the secured release machine"
fi

printf '\nSummary: %s failure(s), %s warning(s)\n' "$failures" "$warnings"
if [ "$failures" -ne 0 ]; then
  printf 'NOT RELEASE-READY: resolve every failure and complete the release runbook.\n' >&2
  exit 1
fi

printf 'STATIC CONFIGURATION CHECK PASSED. This is not Play readiness, upload approval, or publication evidence.\n'
