#!/bin/bash
set -euo pipefail

cd ~/afra7kom

echo "=== Deploy backend $(date -Iseconds) ==="

if [ -f docker-compose.minio.yml ]; then
  docker compose -f docker-compose.minio.yml --env-file env.prod up -d
fi

docker compose -f docker-compose.backend-only.yml --env-file env.prod up -d --build backend

echo "=== Health check ==="
sleep 10
curl -sf http://localhost:8080/actuator/health || echo "Backend pas encore prêt"
docker logs afra7kom-backend --tail 30
