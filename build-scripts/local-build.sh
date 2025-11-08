#!/bin/bash
# build-scripts/local-build.sh - For local development

set -e

APP_VERSION=${1:-"dev-local"}
IMAGE_NAME="campaign_controller_api:${APP_VERSION}"

echo "🔨 Building Docker image locally (no tests, no settings.xml)"
docker build \
  --build-arg APP_VERSION="${APP_VERSION}" \
  --build-arg SKIP_TESTS=true \
  --target runtime \
  -t "${IMAGE_NAME}" .

echo "✅ Local build completed: ${IMAGE_NAME}"
echo "🚀 Run with: docker run -p 8080:8080 ${IMAGE_NAME}"