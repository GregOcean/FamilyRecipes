#!/usr/bin/env bash
# Disposable MySQL only. Never connects to a configured application database.
set -euo pipefail
backend_dir="$(cd "$(dirname "$0")/.." && pwd)"
docker_cmd="${DOCKER_BIN:-docker}"
container_id=''
cleanup() { if [[ -n "$container_id" ]]; then "$docker_cmd" rm -fv "$container_id" >/dev/null; fi; }
trap cleanup EXIT
container_id=$("$docker_cmd" run -d \
  -e MYSQL_ROOT_PASSWORD=foundation-local-test -e MYSQL_DATABASE=family_recipes \
  -v "$backend_dir/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro" \
  -v "$backend_dir/src/main/resources/data.sql:/docker-entrypoint-initdb.d/02-data.sql:ro" mysql:8.4)
ready=false
for attempt in {1..90}; do
  if "$docker_cmd" exec "$container_id" mysql -uroot -pfoundation-local-test -h127.0.0.1 family_recipes \
      -e 'SELECT COUNT(*) FROM inventory_categories' >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then "$docker_cmd" logs "$container_id"; exit 1; fi
"$docker_cmd" run --rm --network "container:$container_id" \
  -e FOUNDATION_DB_TEST=true \
  -e 'DB_URL=jdbc:mysql://127.0.0.1:3306/family_recipes?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC' \
  -e DB_USERNAME=root -e DB_PASSWORD=foundation-local-test \
  -e JWT_SECRET=foundation-local-test-secret-at-least-32-bytes \
  -v "${MAVEN_TEST_CACHE:-familyrecipes-test-maven}:/root/.m2" \
  -v "$backend_dir:/workspace" -w /workspace \
  maven:3.9.9-eclipse-temurin-17 mvn -B -ntp test
