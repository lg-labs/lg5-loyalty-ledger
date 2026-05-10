#!/usr/bin/env bash
#
# publish-schemas.sh — Register the outbound Avro subjects with the
# local Confluent Schema Registry and set BACKWARD compatibility
# (TASK-014).
#
# Subjects:
#   - customer-balance-updated-value  (the record schema, value
#     subject for topic `customer-balance-updated` per Confluent's
#     TopicNameStrategy)
#   - BalanceUpdateCause              (the standalone enum schema,
#     governance / discovery subject per ADR-005)
#
# Idempotency: the SR API treats `POST /subjects/<S>/versions` as
# idempotent — if the supplied schema matches the latest version
# already registered under <S>, the existing schema id is returned
# (no new version is created). The compatibility-mode setting
# (`PUT /config/<S>`) is also idempotent.
#
# Usage:
#   SCHEMA_REGISTRY_URL=http://localhost:8081 ./publish-schemas.sh
#
# Defaults to http://localhost:8081 (matching docker/.env SCHEMA_PORT).
# Exits non-zero if any HTTP call fails.

set -euo pipefail

SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8081}"
COMPATIBILITY_MODE="${COMPATIBILITY_MODE:-BACKWARD}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
AVRO_DIR="${SCRIPT_DIR}/../../lg5-loyalty-ledger-message/lg5-loyalty-ledger-message-model/src/main/resources/avro"

RECORD_SCHEMA="${AVRO_DIR}/customer_balance_updated.avsc"
ENUM_SCHEMA="${AVRO_DIR}/balance_update_cause.avsc"

if [[ ! -f "$RECORD_SCHEMA" ]] || [[ ! -f "$ENUM_SCHEMA" ]]; then
    echo "ERROR: cannot find Avro schema files under: ${AVRO_DIR}" >&2
    exit 2
fi

# --- helpers ---------------------------------------------------------

# Wrap a raw .avsc body into the SR request envelope:
#   {"schema": "<escaped JSON>", "schemaType": "AVRO"}
to_register_payload() {
    local schema_file="$1"
    python3 -c '
import json, sys
with open(sys.argv[1]) as f:
    schema = f.read()
print(json.dumps({"schema": schema, "schemaType": "AVRO"}))
' "$schema_file"
}

post_subject_version() {
    local subject="$1" schema_file="$2"
    local payload http_status body
    payload="$(to_register_payload "$schema_file")"
    body="$(mktemp)"
    http_status="$(curl --silent --show-error --output "$body" --write-out '%{http_code}' \
        -X POST \
        -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data-binary "$payload" \
        "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions")"
    if [[ "$http_status" != "200" ]]; then
        echo "ERROR: register ${subject} failed (HTTP ${http_status}):" >&2
        cat "$body" >&2; echo >&2
        rm -f "$body"
        return 1
    fi
    echo "  registered ${subject} → $(cat "$body")"
    rm -f "$body"
}

put_compat_mode() {
    local subject="$1"
    local body http_status payload
    payload="$(python3 -c "import json; print(json.dumps({\"compatibility\": \"${COMPATIBILITY_MODE}\"}))")"
    body="$(mktemp)"
    http_status="$(curl --silent --show-error --output "$body" --write-out '%{http_code}' \
        -X PUT \
        -H 'Content-Type: application/vnd.schemaregistry.v1+json' \
        --data-binary "$payload" \
        "${SCHEMA_REGISTRY_URL}/config/${subject}")"
    if [[ "$http_status" != "200" ]]; then
        echo "ERROR: set compatibility on ${subject} failed (HTTP ${http_status}):" >&2
        cat "$body" >&2; echo >&2
        rm -f "$body"
        return 1
    fi
    echo "  ${subject} compatibility = ${COMPATIBILITY_MODE}"
    rm -f "$body"
}

# --- main ------------------------------------------------------------

echo "Schema Registry: ${SCHEMA_REGISTRY_URL}"
echo "Compatibility:   ${COMPATIBILITY_MODE}"
echo

echo "→ Registering customer-balance-updated-value"
post_subject_version "customer-balance-updated-value" "$RECORD_SCHEMA"
put_compat_mode      "customer-balance-updated-value"

echo
echo "→ Registering BalanceUpdateCause"
post_subject_version "BalanceUpdateCause" "$ENUM_SCHEMA"
put_compat_mode      "BalanceUpdateCause"

echo
echo "OK — all subjects registered with ${COMPATIBILITY_MODE} compatibility."
