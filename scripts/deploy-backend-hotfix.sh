#!/usr/bin/env bash
# Existing 1.0.16 server only: replace the JAR, preserving APK, data and settings.
set -Eeuo pipefail
umask 077
cd "$(dirname "$0")"
[[ $(id -u) == 0 ]] || { echo '请使用 root 运行'; exit 1; }
sha256sum -c SHA256SUMS
for fr_command in mysql systemctl curl python3; do command -v "$fr_command" >/dev/null; done
fr_jar=/opt/familyrecipes/backend-1.0.0.jar
[[ -f "$fr_jar" ]]
# This bundle does not migrate the database. Stop before changing the service if
# the inventory migration from 1.0.16 has not been installed yet.
[[ $(mysql -NBe "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='family_recipes' AND table_name='fridge_item' AND column_name='client_mutation_id'") == 1 ]] || {
    echo '请先按完整发布流程部署 1.0.16；本补丁不执行数据库迁移。'; exit 1;
}
mkdir -p /opt/familyrecipes/backups
fr_backup=$(mktemp -d "/opt/familyrecipes/backups/$(date +%Y%m%d-%H%M%S)-backend-hotfix-XXXXXX")
cp -p "$fr_jar" "$fr_backup/backend-1.0.0.jar"
rollback() {
    fr_status=$?
    trap - ERR
    set +e
    echo "补丁部署失败，恢复旧后端。备份：$fr_backup"
    cp -p "$fr_backup/backend-1.0.0.jar" "$fr_jar.rollback"
    mv "$fr_jar.rollback" "$fr_jar"
    systemctl restart familyrecipes
    exit "$fr_status"
}
trap rollback ERR
cp -p "$fr_jar" "$fr_jar.next"
cat backend-1.0.0.jar > "$fr_jar.next"
mv "$fr_jar.next" "$fr_jar"
systemctl restart familyrecipes
fr_ready=false
for fr_attempt in {1..45}; do
    if curl -fsS --connect-timeout 2 --max-time 5 http://127.0.0.1:8080/api/config/catalog-summary -o health.json 2>/dev/null &&
       python3 -c 'import json; r=json.load(open("health.json")); assert r["code"] == 200 and isinstance(r["data"]["publicRecipes"], int)' 2>/dev/null; then
        fr_ready=true; break
    fi
    sleep 2
done
[[ "$fr_ready" == true ]]
systemctl is-active --quiet familyrecipes
trap - ERR
echo "后端补丁发布成功。APK、数据库和配置未改动。备份：$fr_backup"
echo '请强制刷新 /admin/ 页面；重启前正在运行的补充任务可在后台检查并恢复。'
