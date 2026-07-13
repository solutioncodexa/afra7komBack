#!/bin/bash
set -euo pipefail

cd ~/afra7kom

echo "=== Deploy backend $(date -Iseconds) ==="

# Permissions volumes hôte (évite Permission denied sur logs/)
sudo mkdir -p logs uploads
sudo chown -R "$(id -u)":"$(id -g)" logs uploads 2>/dev/null || true
chmod -R u+rwX logs uploads 2>/dev/null || true

if [ -f docker-compose.minio.yml ]; then
  docker compose -f docker-compose.minio.yml --env-file env.prod up -d
fi

docker compose -f docker-compose.backend-only.yml --env-file env.prod up -d --build backend

echo "=== Health check ==="
sleep 10
curl -sf http://localhost:8080/actuator/health || echo "Backend pas encore prêt"
docker logs afra7kom-backend --tail 30
