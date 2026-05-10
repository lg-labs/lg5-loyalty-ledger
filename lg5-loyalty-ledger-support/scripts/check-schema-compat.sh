#!/usr/bin/env bash
#
# check-schema-compat.sh — Verify that the local outbound Avro
# schemas are BACKWARD-compatible with the latest version already
# registered in the Schema Registry, WITHOUT registering them
# (TASK-014, CI gate).
#
# Behavior:
#   - If a subject does not yet exist in the registry, the check is
#     considered a PASS for that subject (first-publish path).
#   - If the subject exists, POST the local schema to
#     /compatibility/subjects/<S>/versions/latest. If the response
#     is `{"is_compatible": false}` the script exits non-zero
#     WITHOUT touching the registry (no side effect).
#   - All other HTTP failures (network, 5xx) also exit non-zero.
#
# Usage:
#   SCHEMA_REGISTRY_URL=http://localhost:8081 ./check-schema-compat.sh

set -euo pipefail

SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
AVRO_DIR="${SCRIPT_DIR}/../../lg5-loyalty-ledger-message/lg5-loyalty-ledger-message-model/src/main/resources/avro"

RECORD_SCHEMA="${AVRO_DIR}/customer_balance_updated.avsc"
ENUM_SCHEMA="${AVRO_DIR}/balance_update_cause.avsc"

if [[ ! -f "$RECORD_SCHEMA" ]] || [[ ! -f "$ENUM_SCHEMA" ]]; then
    echo "ERROR: cannot find Avro schema files under: ${AVRO_DIR}" >&2
    exit 2
fi

to_register_payload() {
    local schema_file="$1"
    python3 -c '
import json, sys
with open(sys.argv[1]) as f:
    schema = f.read()
print(json.dumps({"schema": schema, "schemaType": "AVRO"}))
' "$schema_file"
}

# Returns 0 if subject does not exist (404), 1 if it exists, 2 on any
# other error.
subject_exists() {
    local subject="$1"
    local http_status
    http_status="$(curl --silent --output /dev/null --write-out '%{http_code}' \
        "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions/latest")"
    case "$http_status" in
        200) return 1 ;;
        404) return 0 ;;
        *)
            echo "ERROR: probing subject ${subject} → HTTP ${http_status}" >&2
            return 2
            ;;
    esac
}

# Returns 0 if compatible, 1 if incompatible, 2 on transport failure.
check_one() {
    local subject="$1" schema_file="$2"
    local payload body http_status compat
    if subject_exists "$subject"; then
        echo "  ${subject} — first-publish (no registered version yet) → PASS"
        return 0
    fi
    payload="$(to_register_payload "$schema_file")"
    body="$(mktemp)"
    http_status="$(curl --silent --show-error --output "$body" --write-out '%{http_code}' \
        -X POST \
        -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data-binary "$payload" \
        "${SCHEMA_REGISTRY_URL}/compatibility/subjects/${subject}/versions/latest")"
    if [[ "$http_status" != "200" ]]; then
        echo "ERROR: compatibility check for ${subject} failed (HTTP ${http_status}):" >&2
        cat "$body" >&2; echo >&2
        rm -f "$body"
        return 2
    fi
    compat="$(python3 -c '
import json, sys
print(json.load(open(sys.argv[1])).get("is_compatible", False))
' "$body")"
    rm -f "$body"
    if [[ "$compat" == "True" ]]; then
        echo "  ${subject} — BACKWARD compatible with registered latest → PASS"
        return 0
    fi
    echo "  ${subject} — INCOMPATIBLE with registered latest → FAIL"
    return 1
}

echo "Schema Registry: ${SCHEMA_REGISTRY_URL}"
echo "(read-only compatibility check — no registration is performed)"
echo

rc=0
check_one "customer-balance-updated-value" "$RECORD_SCHEMA" || rc=$?
check_one "BalanceUpdateCause" "$ENUM_SCHEMA" || rc=$(( rc == 0 ? $? : rc ))

if [[ "$rc" != "0" ]]; then
    echo
    echo "Compatibility check FAILED (rc=${rc})." >&2
    exit "$rc"
fi
echo
echo "OK — all schemas are BACKWARD-compatible with the registered latest versions."
