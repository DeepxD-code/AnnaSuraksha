#!/usr/bin/env bash
# Simple demo helper: starts the app with H2 enabled and DEV bootstrap secret,
# waits for readiness, mints a dev token, and prints usage examples.

set -euo pipefail

SECRET=${1:-verysecret}
EMAIL=${2:-admin@local}
PORT=${3:-8081}

export SPRING_H2_CONSOLE_ENABLED=true
export DEV_BOOTSTRAP_SECRET="$SECRET"

echo "Starting app with SPRING_H2_CONSOLE_ENABLED=true and DEV_BOOTSTRAP_SECRET='$SECRET'"

nohup mvn -DskipTests spring-boot:run > dev-app.log 2>&1 &
APP_PID=$!
echo $APP_PID > .dev_pid
echo "App started (pid=$APP_PID), logs -> dev-app.log"

echo "Waiting for app to become healthy on http://localhost:${PORT}/actuator/health ..."
for i in $(seq 1 60); do
  if curl -sSf "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
    echo "App healthy."
    break
  fi
  sleep 1
done

if ! curl -sSf "http://localhost:${PORT}/actuator/health" >/dev/null 2>&1; then
  echo "Timed out waiting for app; check dev-app.log" >&2
  exit 1
fi

echo "Minting dev token for ${EMAIL} using bootstrap secret..."
RESPONSE=$(curl -sS -X POST "http://localhost:${PORT}/api/auth/dev-token" \
  -H "Content-Type: application/json" \
  -H "X-BOOTSTRAP-SECRET: ${SECRET}" \
  -d "{\"email\": \"${EMAIL}\"}")

TOKEN=""
if command -v jq >/dev/null 2>&1; then
  TOKEN=$(echo "$RESPONSE" | jq -r '.data.token // empty')
else
  TOKEN=$(python - <<PY
import sys,json
try:
    obj=json.load(sys.stdin)
    print(obj.get('data',{}).get('token',''))
except Exception:
    pass
PY
  <<<"$RESPONSE")
fi

if [ -z "$TOKEN" ]; then
  echo "Failed to mint token. Response:"
  echo "$RESPONSE"
  echo "Check dev-app.log for errors." >&2
  exit 1
fi

echo "Dev token minted (short-lived):"
echo
echo "$TOKEN"
echo
echo "You can now access the dev console in a browser by setting Authorization header:"
echo
echo "  Authorization: Bearer $TOKEN"
echo
echo "Or use curl to open the dev console (this will return HTML):"
echo
echo "  curl -H \"Authorization: Bearer $TOKEN\" http://localhost:${PORT}/dev/console"

echo
echo "To stop the app: kill \\$(cat .dev_pid)" 
