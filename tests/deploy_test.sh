#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck source=../deploy.sh
source "${ROOT_DIR}/deploy.sh"

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

assert_equals() {
    local expected="$1"
    local actual="$2"
    local message="$3"
    [ "${expected}" = "${actual}" ] || fail "${message} (expected=${expected}, actual=${actual})"
}

test_http_check_requires_exact_trimmed_body() {
    curl() { printf ' \nOK\n '; }
    http_check "http://health.test/health-check" "OK" || fail "공백을 제거한 정확한 응답은 성공해야 합니다."

    curl() { printf 'NOT_OK'; }
    if http_check "http://health.test/health-check" "OK"; then
        fail "부분 문자열이 포함된 응답은 성공하면 안 됩니다."
    fi
}

test_http_check_uses_resolve_for_https_sni() {
    curl() {
        [[ " $* " == *" --resolve service.example.com:443:127.0.0.1 "* ]] || return 1
        [[ " $* " == *" https://service.example.com:443/health-check "* ]] || return 1
        printf 'OK'
    }

    http_check "https://127.0.0.1/health-check" "OK" "service.example.com" \
        || fail "HTTPS 검증은 실제 호스트명과 --resolve로 SNI를 맞춰야 합니다."
}

test_http_check_rejects_curl_failure() {
    local error_output
    curl() {
        echo "connection refused" >&2
        return 7
    }
    if error_output=$(http_check "http://health.test/health-check" "OK" 2>&1); then
        fail "curl HTTP 오류를 성공으로 처리하면 안 됩니다."
    fi
    [[ "${error_output}" == *"curl: connection refused"* ]] \
        || fail "curl 실패 원인이 배포 로그에 남아야 합니다."
}

test_read_nginx_upstream_port_requires_one_target() {
    local directory file
    directory=$(mktemp -d)
    file="${directory}/service-url.inc"

    printf 'set $service_url http://127.0.0.1:8080;\n' > "${file}"
    assert_equals 8080 "$(read_nginx_upstream_port "${file}")" "set directive의 포트를 읽어야 합니다."

    printf 'proxy_pass http://127.0.0.1:8081;\n' > "${file}"
    assert_equals 8081 "$(read_nginx_upstream_port "${file}")" "proxy_pass directive의 포트를 읽어야 합니다."

    printf 'proxy_pass http://127.0.0.1:8080/;\n' > "${file}"
    assert_equals 8080 "$(read_nginx_upstream_port "${file}")" \
        "trailing slash가 있는 proxy_pass의 포트를 읽어야 합니다."

    printf 'proxy_pass http://127.0.0.1:8081$request_uri;\n' > "${file}"
    assert_equals 8081 "$(read_nginx_upstream_port "${file}")" \
        "request_uri 변수가 있는 proxy_pass의 포트를 읽어야 합니다."

    printf '# proxy_pass http://127.0.0.1:8080;\nserver 127.0.0.1:8081;\n' > "${file}"
    assert_equals 8081 "$(read_nginx_upstream_port "${file}")" "주석은 upstream으로 세면 안 됩니다."

    printf 'server 127.0.0.1:8080;\nserver 127.0.0.1:8081;\n' > "${file}"
    if read_nginx_upstream_port "${file}" >/dev/null; then
        fail "여러 upstream 대상이 있으면 중단해야 합니다."
    fi

    rm -rf "${directory}"
}

test_deployment_plan_follows_nginx() {
    assert_equals "blue 8080 green 8081 true" \
        "$(select_deployment_plan 8080 true true)" \
        "양쪽 슬롯이 실행 중이어도 Nginx의 반대편을 후보로 선택해야 합니다."
    assert_equals "green 8081 blue 8080 true" \
        "$(select_deployment_plan 8081 true true)" \
        "green upstream이면 blue를 후보로 선택해야 합니다."
}

test_deployment_plan_recovers_current_upstream() {
    assert_equals "none 8081 green 8081 false" \
        "$(select_deployment_plan 8081 false false)" \
        "첫 배포/복구에서는 이미 설정된 upstream 슬롯을 기동해야 합니다."
}

test_deployment_plan_rejects_mismatch() {
    if select_deployment_plan 8080 false true >/dev/null; then
        fail "Nginx active는 멈추고 반대 슬롯만 실행 중인 불일치를 자동 전환하면 안 됩니다."
    fi
}

test_interrupted_switch_rolls_back() {
    local directory marker
    directory=$(mktemp -d)
    marker="${directory}/marker"
    if (
        ROLLBACK_REQUIRED=true
        DEPLOYMENT_COMMITTED=false
        NGINX_BACKUP_FILE="${directory}/backup"
        CANDIDATE_COLOR=green
        touch "${NGINX_BACKUP_FILE}"
        sudo() { "$@"; }
        restore_nginx_upstream() { printf 'restored\n' >> "${marker}"; }
        remove_candidate_container() { printf 'removed:%s\n' "$1" >> "${marker}"; }
        handle_exit 143
    ); then
        fail "중단된 배포는 실패 상태를 유지해야 합니다."
    fi
    assert_equals $'restored\nremoved:green' "$(<"${marker}")" \
        "트래픽 전환 후 중단되면 upstream 복원 후 후보를 제거해야 합니다."
    [ ! -e "${directory}/backup" ] || fail "롤백 성공 후 Nginx 백업을 삭제해야 합니다."
    rm -rf "${directory}"
}

test_interrupted_switch_preserves_backup_when_restore_fails() {
    local directory marker backup
    directory=$(mktemp -d)
    marker="${directory}/marker"
    backup="${directory}/backup"
    touch "${backup}"
    if (
        ROLLBACK_REQUIRED=true
        DEPLOYMENT_COMMITTED=false
        NGINX_BACKUP_FILE="${backup}"
        CANDIDATE_COLOR=green
        sudo() { "$@"; }
        restore_nginx_upstream() { return 1; }
        remove_candidate_container() { printf 'removed\n' >> "${marker}"; }
        handle_exit 1
    ); then
        fail "롤백 실패는 실패 상태를 유지해야 합니다."
    fi
    [ -e "${backup}" ] || fail "롤백 실패 시 복구용 Nginx 백업을 보존해야 합니다."
    [ ! -e "${marker}" ] || fail "롤백 실패 시 후보 컨테이너를 자동 제거하면 안 됩니다."
    rm -rf "${directory}"
}

test_committed_switch_does_not_roll_back() {
    local directory marker
    directory=$(mktemp -d)
    marker="${directory}/marker"
    if (
        ROLLBACK_REQUIRED=false
        DEPLOYMENT_COMMITTED=true
        NGINX_BACKUP_FILE=""
        CANDIDATE_COLOR=green
        restore_nginx_upstream() { printf 'restored\n' >> "${marker}"; }
        remove_candidate_container() { printf 'removed\n' >> "${marker}"; }
        handle_exit 143
    ); then
        fail "중단 상태는 유지해야 합니다."
    fi
    [ ! -e "${marker}" ] || fail "커밋된 전환은 이전 upstream으로 되돌리면 안 됩니다."
    rm -rf "${directory}"
}

test_http_check_requires_exact_trimmed_body
test_http_check_uses_resolve_for_https_sni
test_http_check_rejects_curl_failure
test_read_nginx_upstream_port_requires_one_target
test_deployment_plan_follows_nginx
test_deployment_plan_recovers_current_upstream
test_deployment_plan_rejects_mismatch
test_interrupted_switch_rolls_back
test_interrupted_switch_preserves_backup_when_restore_fails
test_committed_switch_does_not_roll_back

echo "deploy.sh tests passed"
