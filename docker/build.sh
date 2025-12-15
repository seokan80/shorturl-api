#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${1:-local}"
IMAGE_TAG="${2:-latest}"
TARGET="${3:-multi}"

export PROFILE
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-${PROFILE}}"
export IMAGE_TAG="${IMAGE_TAG}"
export SHORT_URL_REDIRECT_API_BASE_URL="${SHORT_URL_REDIRECT_API_BASE_URL:-http://redirect:8081}"
export SHORT_URL_ADMIN_API_BASE_URL="${SHORT_URL_ADMIN_API_BASE_URL:-http://admin-api:8080}"

COMPOSE_FILE="${ROOT_DIR}/docker/docker-compose.yml"
if [[ "${TARGET}" == "all-in-one" ]]; then
  COMPOSE_FILE="${ROOT_DIR}/docker/docker-compose.all-in-one.yml"
fi

echo "Building Docker images with profile='${PROFILE}', tag='${IMAGE_TAG}', compose='${COMPOSE_FILE}'..."
docker compose -f "${COMPOSE_FILE}" build
