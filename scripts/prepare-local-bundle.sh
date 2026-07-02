#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DIST_DIR="${REPO_ROOT}/dist"
TEMPLATE_DIR="${REPO_ROOT}/docker/local-bundle"
BUNDLE_VERSION="${BUNDLE_VERSION:-dev}"
IMAGE_TAG="${IMAGE_TAG:-linkiving-local:${BUNDLE_VERSION}}"
BUNDLE_NAME="linkiving-core-local-${BUNDLE_VERSION}"
BUNDLE_DIR="${DIST_DIR}/${BUNDLE_NAME}"
ARCHIVE_PATH="${DIST_DIR}/${BUNDLE_NAME}.zip"
CHECKSUM_PATH="${ARCHIVE_PATH}.sha256"

APP_JAR="$(find "${REPO_ROOT}/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*plain.jar' | head -n 1)"

if [ -z "${APP_JAR}" ]; then
  echo "Built jar not found in build/libs" >&2
  exit 1
fi

rm -rf "${BUNDLE_DIR}" "${ARCHIVE_PATH}" "${CHECKSUM_PATH}"
mkdir -p "${BUNDLE_DIR}"

cp "${TEMPLATE_DIR}/README.md" "${BUNDLE_DIR}/README.md"
cp "${TEMPLATE_DIR}/docker-compose.yml" "${BUNDLE_DIR}/docker-compose.yml"
cp "${APP_JAR}" "${BUNDLE_DIR}/app.jar"
cp "${TEMPLATE_DIR}/Dockerfile" "${BUNDLE_DIR}/Dockerfile"

sed -i.bak "s|__IMAGE_TAG__|${IMAGE_TAG}|g" "${BUNDLE_DIR}/docker-compose.yml"
rm -f "${BUNDLE_DIR}/docker-compose.yml.bak"

docker build \
  --file "${BUNDLE_DIR}/Dockerfile" \
  --tag "${IMAGE_TAG}" \
  "${BUNDLE_DIR}"

docker save "${IMAGE_TAG}" | gzip > "${BUNDLE_DIR}/linkiving-core-local-image.tar.gz"

rm -f "${BUNDLE_DIR}/app.jar" "${BUNDLE_DIR}/Dockerfile"

(
  cd "${DIST_DIR}"
  zip -qr "${ARCHIVE_PATH}" "${BUNDLE_NAME}"
)

sha256sum "${ARCHIVE_PATH}" > "${CHECKSUM_PATH}"

echo "Created local bundle archive: ${ARCHIVE_PATH}"
