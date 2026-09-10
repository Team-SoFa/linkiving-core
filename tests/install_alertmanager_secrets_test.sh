#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_DIR=$(mktemp -d)

cleanup() {
    rm -rf "${TEST_DIR}"
}

trap cleanup EXIT

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

file_mode() {
    if stat -c '%a' "$1" >/dev/null 2>&1; then
        stat -c '%a' "$1"
    else
        stat -f '%Lp' "$1"
    fi
}

mkdir -p "${TEST_DIR}/bin" "${TEST_DIR}/secrets"
cat > "${TEST_DIR}/bin/sudo" <<'EOF'
#!/bin/sh
exec "$@"
EOF
chmod +x "${TEST_DIR}/bin/sudo"

smtp_value='smtp-test-value'
discord_value='https://discord.invalid/test-webhook'
output=$(
    PATH="${TEST_DIR}/bin:${PATH}" \
    ALERTMANAGER_SECRET_UID="$(id -u)" \
    ALERTMANAGER_SECRET_GID="$(id -g)" \
    SMTP_PASSWORD="${smtp_value}" \
    ALERTMANAGER_DISCORD_WEBHOOK_URL="${discord_value}" \
    "${ROOT_DIR}/scripts/install-alertmanager-secrets.sh" \
        "${TEST_DIR}/secrets/smtp_password" \
        "${TEST_DIR}/secrets/discord_webhook"
)

[ -z "${output}" ] || fail "Secret 설치 과정에서 표준 출력이 발생하면 안 됩니다."
[ "$(<"${TEST_DIR}/secrets/smtp_password")" = "${smtp_value}" ] \
    || fail "SMTP secret이 정확히 저장되어야 합니다."
[ "$(<"${TEST_DIR}/secrets/discord_webhook")" = "${discord_value}" ] \
    || fail "Discord secret이 정확히 저장되어야 합니다."
[ "$(file_mode "${TEST_DIR}/secrets/smtp_password")" = 600 ] \
    || fail "SMTP secret 권한은 600이어야 합니다."
[ "$(file_mode "${TEST_DIR}/secrets/discord_webhook")" = 600 ] \
    || fail "Discord secret 권한은 600이어야 합니다."

if (
    PATH="${TEST_DIR}/bin:${PATH}"
    ALERTMANAGER_SECRET_UID="$(id -u)"
    ALERTMANAGER_SECRET_GID="$(id -g)"
    SMTP_PASSWORD=''
    ALERTMANAGER_DISCORD_WEBHOOK_URL="${discord_value}"
    "${ROOT_DIR}/scripts/install-alertmanager-secrets.sh" \
        "${TEST_DIR}/secrets/empty_smtp" \
        "${TEST_DIR}/secrets/unused_webhook" >/dev/null 2>&1
); then
    fail "빈 Secret은 거부해야 합니다."
fi

[ ! -e "${TEST_DIR}/secrets/empty_smtp" ] \
    || fail "빈 Secret 파일을 생성하면 안 됩니다."

echo "Alertmanager secret installation tests passed"
