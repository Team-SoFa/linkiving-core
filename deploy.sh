#!/bin/bash

# deploy.sh - Blue-Green 무중단 배포 스크립트

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${SCRIPT_DIR}"
COMPOSE_FILE="${REPO_ROOT}/docker/docker-compose.yml"

PROJECT="linkiving-core"
DEPLOY_IMAGE_TAG="${IMAGE_TAG:-latest}"
DEPLOY_LOCK_FILE="${DEPLOY_LOCK_FILE:-/var/lock/linkiving-core-deploy.lock}"
NGINX_UPSTREAM_FILE="${NGINX_UPSTREAM_FILE:-/etc/nginx/conf.d/service-url.inc}"
TRAFFIC_CHECK_URL="${TRAFFIC_CHECK_URL:-http://127.0.0.1/health-check}"
TRAFFIC_CHECK_HOST="${TRAFFIC_CHECK_HOST:-}"
TRAFFIC_CHECK_EXPECTED_BODY="${TRAFFIC_CHECK_EXPECTED_BODY:-OK}"
TRAFFIC_CHECK_CONNECT_TIMEOUT="${TRAFFIC_CHECK_CONNECT_TIMEOUT:-5}"
TRAFFIC_CHECK_MAX_TIME="${TRAFFIC_CHECK_MAX_TIME:-10}"
TRAFFIC_SWITCH_DELAY_SECONDS="${TRAFFIC_SWITCH_DELAY_SECONDS:-5}"
PREVIOUS_STOP_DELAY_SECONDS="${PREVIOUS_STOP_DELAY_SECONDS:-30}"
HEALTH_CHECK_MAX_RETRY="${HEALTH_CHECK_MAX_RETRY:-30}"
HEALTH_CHECK_RETRY_INTERVAL_SECONDS="${HEALTH_CHECK_RETRY_INTERVAL_SECONDS:-10}"

NGINX_BACKUP_FILE=""
CANDIDATE_COLOR=""
ROLLBACK_REQUIRED=false
DEPLOYMENT_COMMITTED=false

compose() {
    sudo --preserve-env=GRAFANA_ADMIN_USER,GRAFANA_ADMIN_PASSWORD,APP_MEMBER_WITHDRAWAL_ENABLED,APP_MEMBER_WITHDRAWAL_INTERNAL_SECRET \
        IMAGE_TAG="${DEPLOY_IMAGE_TAG}" docker compose -p "${PROJECT}" -f "${COMPOSE_FILE}" "$@"
}

validate_member_withdrawal_configuration() {
    if [ "${APP_MEMBER_WITHDRAWAL_ENABLED:-}" != true ]; then
        echo "APP_MEMBER_WITHDRAWAL_ENABLED must be true for a production deployment." >&2
        return 1
    fi

    if [[ ! "${APP_MEMBER_WITHDRAWAL_INTERNAL_SECRET:-}" =~ ^[A-Za-z0-9_-]{32,}$ ]]; then
        echo "APP_MEMBER_WITHDRAWAL_INTERNAL_SECRET must be a URL-safe secret of at least 32 characters." >&2
        return 1
    fi
}

container_is_running() {
    local color="$1"
    [ -n "$(compose ps -q --status running "${color}")" ]
}

color_for_port() {
    case "$1" in
        8080) printf 'blue\n' ;;
        8081) printf 'green\n' ;;
        *) return 1 ;;
    esac
}

port_for_color() {
    case "$1" in
        blue) printf '8080\n' ;;
        green) printf '8081\n' ;;
        *) return 1 ;;
    esac
}

opposite_color() {
    case "$1" in
        blue) printf 'green\n' ;;
        green) printf 'blue\n' ;;
        *) return 1 ;;
    esac
}

read_nginx_upstream_port() {
    local file="$1"
    local ports

    ports=$(awk '
        /^[[:space:]]*#/ { next }
        /^[[:space:]]*(set[[:space:]]+\$[[:alnum:]_]+[[:space:]]+https?:\/\/|proxy_pass[[:space:]]+https?:\/\/|server[[:space:]]+)127\.0\.0\.1:808[01]([\/$;[:space:]]|$)/ {
            if (match($0, /127\.0\.0\.1:808[01]/)) {
                print substr($0, RSTART + 10, 4)
            }
        }
    ' "${file}")

    if [ "$(printf '%s\n' "${ports}" | awk 'NF { count++ } END { print count + 0 }')" -ne 1 ]; then
        return 1
    fi

    printf '%s\n' "${ports}"
}

http_check() {
    local url="$1"
    local expected_body="$2"
    local hostname="${3:-}"
    local request_url="${url}"
    local response trimmed scheme remainder authority connect_host connect_port path
    local curl_args=(
        --fail
        --silent
        --show-error
        --connect-timeout "${TRAFFIC_CHECK_CONNECT_TIMEOUT}"
        --max-time "${TRAFFIC_CHECK_MAX_TIME}"
    )

    if [ -n "${hostname}" ]; then
        if [[ ! "${url}" =~ ^https?:// ]] || [[ "${hostname}" == *:* ]] || [[ "${hostname}" == */* ]]; then
            echo "TRAFFIC_CHECK_HOST에는 포트나 경로가 없는 호스트명을 설정해야 합니다." >&2
            return 1
        fi

        scheme="${url%%://*}"
        remainder="${url#*://}"
        authority="${remainder%%/*}"
        connect_host="${authority%%:*}"
        if [[ "${authority}" == *:* ]]; then
            connect_port="${authority##*:}"
        elif [ "${scheme}" = "https" ]; then
            connect_port=443
        else
            connect_port=80
        fi

        if [[ "${remainder}" == */* ]]; then
            path="/${remainder#*/}"
        else
            path=""
        fi

        request_url="${scheme}://${hostname}:${connect_port}${path}"
        curl_args+=(--resolve "${hostname}:${connect_port}:${connect_host}")
    fi

    if ! response=$(curl "${curl_args[@]}" "${request_url}" 2>&1); then
        echo "  curl: ${response:-응답 없음}" >&2
        return 1
    fi

    trimmed=$(printf '%s' "${response}" | awk '{ sub(/^[[:space:]]+/, ""); sub(/[[:space:]]+$/, ""); printf "%s", $0 }')
    [[ "${trimmed}" == "${expected_body}" ]]
}

remove_candidate_container() {
    local color="$1"

    compose stop "${color}" >/dev/null 2>&1 || true
    compose rm -f "${color}" >/dev/null 2>&1 || true
}

create_nginx_backup() {
    local directory basename
    directory=$(dirname "${NGINX_UPSTREAM_FILE}")
    basename=$(basename "${NGINX_UPSTREAM_FILE}")
    NGINX_BACKUP_FILE=$(sudo mktemp "${directory}/.${basename}.backup.XXXXXX")
    sudo cp --preserve=all "${NGINX_UPSTREAM_FILE}" "${NGINX_BACKUP_FILE}"
}

replace_nginx_file_atomically() {
    local source="$1"
    local directory basename temporary
    directory=$(dirname "${NGINX_UPSTREAM_FILE}")
    basename=$(basename "${NGINX_UPSTREAM_FILE}")
    temporary=$(sudo mktemp "${directory}/.${basename}.new.XXXXXX")

    if ! sudo cp --preserve=all "${source}" "${temporary}"; then
        sudo rm -f "${temporary}" || true
        return 1
    fi

    if ! sudo mv -f "${temporary}" "${NGINX_UPSTREAM_FILE}"; then
        sudo rm -f "${temporary}" || true
        return 1
    fi
}

write_nginx_upstream_port() {
    local expected_port="$1"
    local target_port="$2"
    local current_port directory basename temporary

    current_port=$(read_nginx_upstream_port "${NGINX_UPSTREAM_FILE}") || return 1
    [ "${current_port}" = "${expected_port}" ] || return 1
    [ "${current_port}" != "${target_port}" ] || return 0

    directory=$(dirname "${NGINX_UPSTREAM_FILE}")
    basename=$(basename "${NGINX_UPSTREAM_FILE}")
    temporary=$(sudo mktemp "${directory}/.${basename}.new.XXXXXX")
    if ! sudo cp --preserve=all "${NGINX_UPSTREAM_FILE}" "${temporary}"; then
        sudo rm -f "${temporary}" || true
        return 1
    fi

    if ! sudo sed -i -E "/^[[:space:]]*#/! {/^[[:space:]]*(set[[:space:]]+\\\$[[:alnum:]_]+[[:space:]]+https?:\\/\\/|proxy_pass[[:space:]]+https?:\\/\\/|server[[:space:]]+)127\\.0\\.0\\.1:${expected_port}([\\/$;[:space:]]|$)/ s/127\\.0\\.0\\.1:${expected_port}/127.0.0.1:${target_port}/}" "${temporary}"; then
        sudo rm -f "${temporary}" || true
        return 1
    fi

    if [ "$(read_nginx_upstream_port "${temporary}")" != "${target_port}" ]; then
        sudo rm -f "${temporary}" || true
        return 1
    fi

    if ! sudo mv -f "${temporary}" "${NGINX_UPSTREAM_FILE}"; then
        sudo rm -f "${temporary}" || true
        return 1
    fi
}

restore_nginx_upstream() {
    if [ -z "${NGINX_BACKUP_FILE}" ] || ! sudo test -f "${NGINX_BACKUP_FILE}"; then
        echo "❌ 복원할 Nginx upstream 백업이 없습니다."
        return 1
    fi

    if ! replace_nginx_file_atomically "${NGINX_BACKUP_FILE}"; then
        echo "❌ Nginx upstream 백업 복원에 실패했습니다."
        return 1
    fi

    if ! sudo nginx -t || ! sudo nginx -s reload; then
        echo "❌ 복원된 Nginx 설정 적용에 실패했습니다."
        return 1
    fi

    echo "✅ Nginx upstream을 이전 설정으로 복원했습니다."
}

cleanup_nginx_backup() {
    if [ -z "${NGINX_BACKUP_FILE}" ]; then
        return 0
    fi

    if sudo rm -f "${NGINX_BACKUP_FILE}"; then
        NGINX_BACKUP_FILE=""
        return 0
    fi

    echo "⚠️ Nginx 백업 삭제에 실패했습니다: ${NGINX_BACKUP_FILE}" >&2
    return 1
}

handle_exit() {
    local status="$1"
    trap - EXIT INT TERM

    if [ "${status}" -ne 0 ] && [ "${ROLLBACK_REQUIRED}" = true ] && [ "${DEPLOYMENT_COMMITTED}" != true ]; then
        echo "❌ 완료 전 배포가 중단되어 Nginx upstream을 롤백합니다."
        if restore_nginx_upstream; then
            [ -z "${CANDIDATE_COLOR}" ] || remove_candidate_container "${CANDIDATE_COLOR}"
            cleanup_nginx_backup || true
        else
            echo "❌ 자동 롤백에 실패했습니다. 이전/후보 컨테이너와 백업 ${NGINX_BACKUP_FILE}을 유지합니다."
        fi
    elif [ "${status}" -ne 0 ] && [ -n "${CANDIDATE_COLOR}" ] && [ "${DEPLOYMENT_COMMITTED}" != true ]; then
        remove_candidate_container "${CANDIDATE_COLOR}"
    fi

    if [ "${DEPLOYMENT_COMMITTED}" = true ] && [ -n "${NGINX_BACKUP_FILE}" ]; then
        cleanup_nginx_backup || true
    elif [ "${status}" -ne 0 ] && [ -n "${NGINX_BACKUP_FILE}" ]; then
        echo "⚠️ 장애 확인을 위해 Nginx 백업을 보존합니다: ${NGINX_BACKUP_FILE}"
    fi

    exit "${status}"
}

acquire_deploy_lock() {
    sudo touch "${DEPLOY_LOCK_FILE}"
    sudo chown "$(id -u):$(id -g)" "${DEPLOY_LOCK_FILE}"
    exec 9>"${DEPLOY_LOCK_FILE}"
    if ! flock -n 9; then
        echo "❌ 다른 배포가 진행 중입니다."
        return 1
    fi
}

select_deployment_plan() {
    local upstream_port="$1"
    local active_running="$2"
    local standby_running="$3"
    local active_color standby_color standby_port
    active_color=$(color_for_port "${upstream_port}") || return 1
    standby_color=$(opposite_color "${active_color}")
    standby_port=$(port_for_color "${standby_color}")

    if [ "${active_running}" = true ]; then
        printf '%s %s %s %s %s\n' "${active_color}" "${upstream_port}" "${standby_color}" "${standby_port}" true
    elif [ "${standby_running}" = false ]; then
        printf '%s %s %s %s %s\n' none "${upstream_port}" "${active_color}" "${upstream_port}" false
    else
        return 1
    fi
}

main() {
    trap 'handle_exit $?' EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM

    validate_member_withdrawal_configuration
    acquire_deploy_lock
    echo "=== Blue-Green 배포 시작 ==="

    if [ ! -f "${COMPOSE_FILE}" ]; then
        echo "❌ Compose file not found: ${COMPOSE_FILE}"
        exit 1
    fi
    if [ ! -f "${NGINX_UPSTREAM_FILE}" ]; then
        echo "❌ Nginx 설정 파일을 찾을 수 없습니다: ${NGINX_UPSTREAM_FILE}"
        exit 1
    fi

    local upstream_port active_color standby_color active_running standby_running
    local before_color before_port after_color after_port switch_required server_healthy count health_url
    upstream_port=$(read_nginx_upstream_port "${NGINX_UPSTREAM_FILE}") || {
        echo "❌ Nginx upstream 대상은 지원되는 directive에 정확히 하나만 있어야 합니다."
        exit 1
    }
    active_color=$(color_for_port "${upstream_port}")
    standby_color=$(opposite_color "${active_color}")
    active_running=false
    standby_running=false
    container_is_running "${active_color}" && active_running=true
    container_is_running "${standby_color}" && standby_running=true

    if ! read -r before_color before_port after_color after_port switch_required \
        < <(select_deployment_plan "${upstream_port}" "${active_running}" "${standby_running}"); then
        echo "❌ Nginx upstream(${active_color})과 실행 중인 컨테이너(${standby_color})가 일치하지 않습니다."
        echo "❌ 자동 변경 없이 배포를 중단합니다."
        exit 1
    fi
    if [ "${before_color}" = none ]; then
        echo "실행 중인 컨테이너가 없어 현재 upstream 슬롯을 복구합니다."
        before_color=""
    fi

    CANDIDATE_COLOR="${after_color}"

    echo "Docker 이미지 pull..."
    compose pull
    sudo docker image prune -f

    echo "${after_color} 컨테이너 실행"
    compose up -d "${after_color}"
    echo "${after_color} server up (port:${after_port})"

    server_healthy=false
    count=0
    health_url="http://127.0.0.1:${after_port}/health-check"
    echo "서버 헬스체크 시작..."
    while [ "${count}" -lt "${HEALTH_CHECK_MAX_RETRY}" ]; do
        count=$((count + 1))
        echo "서버 응답 확인중 (${count}/${HEALTH_CHECK_MAX_RETRY})"
        if http_check "${health_url}" "OK"; then
            server_healthy=true
            break
        fi
        sleep "${HEALTH_CHECK_RETRY_INTERVAL_SECONDS}"
    done
    if [ "${server_healthy}" != true ]; then
        echo "❌ 후보 서버가 정상적으로 구동되지 않았습니다."
        exit 1
    fi

    create_nginx_backup
    ROLLBACK_REQUIRED=true

    if [ "${switch_required}" = true ]; then
        echo "Nginx upstream을 ${before_port}에서 ${after_port}로 전환합니다."
        write_nginx_upstream_port "${before_port}" "${after_port}"
        sudo nginx -t
        sudo nginx -s reload
    else
        echo "Nginx upstream은 이미 복구 슬롯(${after_port})을 가리키고 있습니다."
    fi

    sleep "${TRAFFIC_SWITCH_DELAY_SECONDS}"
    if ! http_check "${TRAFFIC_CHECK_URL}" "${TRAFFIC_CHECK_EXPECTED_BODY}" "${TRAFFIC_CHECK_HOST}"; then
        echo "❌ Nginx 경유 트래픽 확인에 실패했습니다."
        exit 1
    fi

    if [ -n "${before_color}" ]; then
        echo "이전 컨테이너 종료 전 ${PREVIOUS_STOP_DELAY_SECONDS}초 대기..."
        sleep "${PREVIOUS_STOP_DELAY_SECONDS}"
        DEPLOYMENT_COMMITTED=true
        ROLLBACK_REQUIRED=false
        compose stop "${before_color}" >/dev/null 2>&1 || true
        compose rm -f "${before_color}" >/dev/null 2>&1 || true
    else
        DEPLOYMENT_COMMITTED=true
        ROLLBACK_REQUIRED=false
    fi

    echo "✅ Active Container: ${after_color} (Port: ${after_port})"
    sudo docker ps --filter "name=blue" --filter "name=green" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    echo "=== 배포 완료 ==="
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    main "$@"
fi
