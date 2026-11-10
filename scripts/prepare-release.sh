#!/usr/bin/env bash
# Build a local, reviewable bundle only. Does not SSH, publish or import content.
set -euo pipefail
fr_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$fr_root"
fr_version="${1:?用法: bash scripts/prepare-release.sh 1.0.6}"
[[ "$fr_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 1
python3 - "$fr_version" <<'PY'
import re,sys
from pathlib import Path
s=Path('android/app/build.gradle').read_text()
current=re.search(r'versionName\s+"([^"]+)"',s)[1]
if current!=sys.argv[1]:
    raise SystemExit(f'版本不匹配：build.gradle 当前为 {current}，请求打包 {sys.argv[1]}。请更新 versionName 并递增 versionCode 后重试。')
assert Path('android/keystore.properties').exists(), '缺少原 release 签名配置'
PY
fr_bundle="$fr_root/familyrecipes-release/$fr_version"
[[ ! -e "$fr_bundle" ]] || { echo "目录已存在，请先归档该发布包：$fr_bundle"; exit 1; }
export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17)}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/private/tmp/fr-gradle-cache}"
docker run --rm -v familyrecipes-test-maven:/root/.m2 -v "$fr_root/backend:/workspace" -w /workspace \
    maven:3.9.9-eclipse-temurin-17 mvn -B -ntp -DskipTests package
bash android/gradlew -p android :app:assembleRelease --console=plain
mkdir -p "$fr_bundle"
cp backend/target/backend-1.0.0.jar "$fr_bundle/"
cp android/app/build/outputs/apk/release/app-release.apk "$fr_bundle/FamilyRecipes-$fr_version.apk"
cp scripts/deploy-current-server.sh "$fr_bundle/"
for fr_migration in 2026-11-08-translations.sql 2026-11-08-recommendation-signals.sql 2026-11-08-receipt-import.sql 2026-11-08-public-recipes.sql 2026-11-09-recipe-classification.sql 2026-11-10-cuisine-refinement.sql; do
    cp "backend/src/main/resources/migrations/$fr_migration" "$fr_bundle/"
done
printf '%s\n' "$fr_version" > "$fr_bundle/VERSION"
(cd "$fr_bundle" && shasum -a 256 *.apk *.jar *.sql *.sh VERSION > SHA256SUMS)
echo "仅已打包：${fr_bundle}；未部署。"
