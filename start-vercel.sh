#!/bin/sh
set -eu

export JAVA_HOME="${JAVA_HOME:-/opt/java/openjdk}"
export PATH="$JAVA_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
export LD_LIBRARY_PATH="$JAVA_HOME/lib/server:$JAVA_HOME/lib"

APP_PORT="${PORT:-8080}"

printf '%s\n' "========================================"
printf '%s\n' "Starting Library Intelligence API"
printf '%s\n' "PORT=$APP_PORT"
printf '%s\n' "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-default}"
printf '%s\n' "JAVA_HOME=$JAVA_HOME"
if [ "${SPRING_PROFILES_ACTIVE:-default}" = "prod" ]; then
  [ -n "${DB_URL:-}" ] && printf '%s\n' "DB_URL configured: yes" || printf '%s\n' "DB_URL configured: NO"
  [ -n "${DB_USERNAME:-}" ] && printf '%s\n' "DB_USERNAME configured: yes" || printf '%s\n' "DB_USERNAME configured: NO"
  [ -n "${DB_PASSWORD:-}" ] && printf '%s\n' "DB_PASSWORD configured: yes" || printf '%s\n' "DB_PASSWORD configured: NO"
fi
printf '%s\n' "========================================"

exec /opt/java/openjdk/bin/java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=70.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar \
  --server.port="$APP_PORT" \
  --server.address=0.0.0.0
