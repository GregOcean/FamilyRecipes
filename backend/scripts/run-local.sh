#!/usr/bin/env bash
set -euo pipefail
backend_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$backend_dir"
if [[ ! -f .env ]]; then
  echo 'Missing backend/.env; configure the local database and JWT secret first.' >&2
  exit 1
fi
set -a
source .env
set +a
if [[ -x /usr/libexec/java_home ]]; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [[ ! -f target/backend-1.0.0.jar ]]; then
  echo 'Build the backend first: mvn -DskipTests package' >&2
  exit 1
fi
exec "$java_cmd" -jar target/backend-1.0.0.jar
