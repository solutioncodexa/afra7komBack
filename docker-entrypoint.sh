#!/bin/sh
set -e

# Corriger les permissions des volumes montés depuis l'hôte
if [ -d /app/logs ]; then
  chown -R afra7kom:afra7kom /app/logs 2>/dev/null || true
  chmod -R u+rwX /app/logs 2>/dev/null || true
fi

if [ -d /app/uploads ]; then
  chown -R afra7kom:afra7kom /app/uploads 2>/dev/null || true
  chmod -R u+rwX /app/uploads 2>/dev/null || true
fi

if [ -d /app/config ]; then
  chown -R afra7kom:afra7kom /app/config 2>/dev/null || true
fi

exec su-exec afra7kom sh -c "java ${JAVA_OPTS:--Xms512m -Xmx1536m} -jar app.jar"
