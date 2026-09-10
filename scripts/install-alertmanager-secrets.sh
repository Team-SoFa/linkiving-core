#!/bin/bash

set -euo pipefail

# prom/alertmanager:v0.28.0 runs as the nobody user by default.
ALERTMANAGER_SECRET_UID="${ALERTMANAGER_SECRET_UID:-65534}"
ALERTMANAGER_SECRET_GID="${ALERTMANAGER_SECRET_GID:-65534}"
temporary_file=""

cleanup() {
    if [ -n "${temporary_file}" ]; then
        rm -f "${temporary_file}"
    fi
}

trap cleanup EXIT

install_secret() {
    local value="$1"
    local destination="$2"

    if [ -z "${value}" ]; then
        echo "Required Alertmanager secret is empty: $(basename "${destination}")" >&2
        return 1
    fi

    temporary_file=$(mktemp "${destination}.tmp.XXXXXX")
    chmod 600 "${temporary_file}"
    printf '%s' "${value}" > "${temporary_file}"

    sudo install \
        -o "${ALERTMANAGER_SECRET_UID}" \
        -g "${ALERTMANAGER_SECRET_GID}" \
        -m 600 \
        "${temporary_file}" \
        "${destination}"

    rm -f "${temporary_file}"
    temporary_file=""
}

main() {
    if [ "$#" -ne 2 ]; then
        echo "Usage: $0 <smtp-password-file> <discord-webhook-file>" >&2
        return 1
    fi

    install_secret "${SMTP_PASSWORD:-}" "$1"
    install_secret "${ALERTMANAGER_DISCORD_WEBHOOK_URL:-}" "$2"
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    main "$@"
fi
