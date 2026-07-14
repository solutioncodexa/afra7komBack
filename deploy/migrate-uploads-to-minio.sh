#!/bin/bash
# Migre les fichiers locaux ./uploads vers le bucket MinIO (même clés objects).
# Prérequis: mc (MinIO Client) installé, MinIO joignable.
#
# Usage:
#   export MINIO_ENDPOINT=http://minio:9000
#   export MINIO_ACCESS_KEY=...
#   export MINIO_SECRET_KEY=...
#   export MINIO_BUCKET=afra7kom
#   ./deploy/migrate-uploads-to-minio.sh /path/to/uploads

set -euo pipefail

UPLOADS_DIR="${1:-./uploads}"
ALIAS="afra7komlocal"
BUCKET="${MINIO_BUCKET:-afra7kom}"
ENDPOINT="${MINIO_ENDPOINT:-http://127.0.0.1:9000}"
ACCESS="${MINIO_ACCESS_KEY:-minioadmin}"
SECRET="${MINIO_SECRET_KEY:-minioadmin}"

if [ ! -d "$UPLOADS_DIR" ]; then
  echo "Dossier uploads introuvable: $UPLOADS_DIR"
  exit 1
fi

if ! command -v mc >/dev/null 2>&1; then
  echo "Installez MinIO Client (mc): https://min.io/docs/minio/linux/reference/minio-mc.html"
  exit 1
fi

mc alias set "$ALIAS" "$ENDPOINT" "$ACCESS" "$SECRET"
mc mb --ignore-existing "$ALIAS/$BUCKET"

echo "=== Sync $UPLOADS_DIR -> $ALIAS/$BUCKET ==="
# Structure locale: uploads/images/x.jpg → object images/x.jpg
mc mirror --overwrite "$UPLOADS_DIR" "$ALIAS/$BUCKET"

echo "=== Terminé. Les URLs legacy /uploads/... restent servies par le backend ;"
echo "    les nouveaux uploads utilisent https://api.afra7kom.ma/${BUCKET}/..."
echo "Optionnel SQL (à exécuter manuellement après vérif):"
echo "  UPDATE pack SET ... REPLACE('/uploads/', '/${BUCKET}/') ..."
