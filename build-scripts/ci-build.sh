#!/bin/bash
# build-scripts/ci-build.sh - For CI/CD with full testing and deployment

set -e

APP_VERSION=${1:-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)}
REGISTRY=${2:-"registry.stegienko.com:8443"}
IMAGE_NAME="${REGISTRY}/campaign_controller_api:${APP_VERSION}"

echo "🔨 Building Docker image for CI/CD with tests and deployment"

# Ensure maven-settings.xml exists for CI
if [ ! -f "maven-settings.xml" ]; then
    echo "⚠️  Warning: maven-settings.xml not found. Using default Maven settings."
fi

# Build with tests
echo "🧪 Running tests..."
docker build \
  --build-arg APP_VERSION="${APP_VERSION}" \
  --build-arg SKIP_TESTS=false \
  --target tester \
  -t "${IMAGE_NAME}-test" .

# Build runtime image
echo "📦 Building runtime image..."
docker build \
  --build-arg APP_VERSION="${APP_VERSION}" \
  --build-arg SKIP_TESTS=true \
  --target runtime \
  -t "${IMAGE_NAME}" .

# Deploy artifacts (optional)
if [ "${DEPLOY_ARTIFACTS:-false}" = "true" ]; then
    echo "🚀 Deploying artifacts to repository..."
    docker build \
      --build-arg APP_VERSION="${APP_VERSION}" \
      --target deployer \
      -t "${IMAGE_NAME}-deployer" .
fi

echo "✅ CI/CD build completed: ${IMAGE_NAME}"
echo "📤 Push with: docker push ${IMAGE_NAME}"