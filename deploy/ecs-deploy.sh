#!/usr/bin/env bash
# ============================================================
# One-click deploy on Alibaba Cloud ECS
# Usage: bash deploy/ecs-deploy.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Check .env
if [ ! -f .env ]; then
    echo "ERROR: .env not found. Copy the template and fill in values:"
    echo "  cp deploy/.env.example .env"
    exit 1
fi

set -a; source .env; set +a

# Determine compose files
if [ "${ACR_REGISTRY:-}" ]; then
    echo ">>> ACR registry detected — using pre-built images"
    COMPOSE_FILES="-f docker-compose.yml -f deploy/docker-compose.ecs.yml"
    echo ">>> Pulling latest images ..."
    docker compose $COMPOSE_FILES pull
else
    echo ">>> No ACR registry set — building locally"
    COMPOSE_FILES="-f docker-compose.yml"
fi

echo ">>> Starting services ..."
docker compose $COMPOSE_FILES up -d

echo ""
echo ">>> Waiting for services to be healthy ..."
timeout 180 bash -c '
    while docker compose ps | grep -q "health:\|starting\|unhealthy"; do
        sleep 5
        echo "   ... checking service health ..."
    done
' 2>/dev/null || true

echo ""
echo "=========================================="
docker compose ps
echo "=========================================="
echo ""
echo "Deploy complete!"
echo "  Frontend:  http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo '<ECS_IP>'):${FRONTEND_PORT:-80}"
echo "  Backend:   http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo '<ECS_IP>'):8080"
echo "  Python AI: http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo '<ECS_IP>'):8000"
