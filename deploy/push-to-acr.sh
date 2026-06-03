#!/usr/bin/env bash
# ============================================================
# Build & Push to Alibaba Cloud Container Registry (ACR)
# Usage: bash deploy/push-to-acr.sh
# Prerequisites:
#   1. docker login your-registry.cn-xxxxx.cr.aliyuncs.com
#   2. .env file exists with ACR_REGISTRY, ACR_NAMESPACE set
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load env
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a; source "$PROJECT_ROOT/.env"; set +a
fi

ACR_REGISTRY="${ACR_REGISTRY:?ERROR: ACR_REGISTRY not set in .env}"
ACR_NAMESPACE="${ACR_NAMESPACE:?ERROR: ACR_NAMESPACE not set in .env}"
ACR_TAG="${ACR_IMAGE_TAG:-latest}"

REGISTRY_URL="$ACR_REGISTRY/$ACR_NAMESPACE"

SERVICES=("java-backend:backend" "python-service:python-services" "vue-frontend:frontend")

echo "=========================================="
echo " Pushing to: $REGISTRY_URL"
echo " Tag:        $ACR_TAG"
echo "=========================================="

for entry in "${SERVICES[@]}"; do
    IFS=':' read -r service_name context_dir <<< "$entry"
    image_name="multimodal-$service_name"
    full_tag="$REGISTRY_URL/$image_name:$ACR_TAG"

    echo ""
    echo ">>> Building $image_name ..."
    docker build -t "$full_tag" "$PROJECT_ROOT/$context_dir"

    echo ">>> Pushing $full_tag ..."
    docker push "$full_tag"

    # Also tag as 'latest' if ACR_TAG is not latest
    if [ "$ACR_TAG" != "latest" ]; then
        docker tag "$full_tag" "$REGISTRY_URL/$image_name:latest"
        docker push "$REGISTRY_URL/$image_name:latest"
    fi
done

echo ""
echo "=========================================="
echo " All images pushed successfully!"
echo "=========================================="
