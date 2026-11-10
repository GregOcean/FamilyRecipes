#!/usr/bin/env bash
set -Eeuo pipefail
trap 'fr_status=$?; printf "发布失败：脚本第 %s 行，退出码 %s。\n" "$LINENO" "$fr_status" >&2; exit "$fr_status"' ERR
umask 077
cd "$(dirname "$0")"
[[ $(id -u) == 0 ]] || { echo "请使用 root 运行"; exit 1; }
fr_version="${1:?用法: deploy-current-server.sh 1.0.6}"
[[ "$fr_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo '版本格式错误'; exit 1; }
[[ "$(cat VERSION)" == "$fr_version" ]] || { echo '发布包版本不一致'; exit 1; }
sha256sum -c SHA256SUMS
echo "检查服务器依赖与部署路径…"
for command in mysql mysqldump systemctl nginx curl python3; do
    command -v "$command" >/dev/null || { echo "发布停止：找不到命令 $command" >&2; exit 1; }
done
[[ -f /opt/familyrecipes/backend-1.0.0.jar ]] || { echo "发布停止：不存在 /opt/familyrecipes/backend-1.0.0.jar，请核对当前服务路径。" >&2; exit 1; }
[[ -d /var/www/familyrecipes-apk ]] || { echo "发布停止：不存在 /var/www/familyrecipes-apk，请核对 Nginx 下载目录。" >&2; exit 1; }
nginx -t
nginx -T > nginx-current.txt 2>&1
# Resolve the enabled symlink, preserving the existing site file and TLS settings.
fr_nginx=$(readlink -f /etc/nginx/sites-enabled/familyrecipes)
python3 - "$fr_nginx" <<'PY'
from pathlib import Path
import re, sys
source=Path(sys.argv[1]).read_text()
old=r'location\s*=\s*/apk/FamilyRecipes-latest\.apk\s*\{[^{}]*\}'
matches=list(re.finditer(old,source))
prefix=r'location\s+(?:\^~\s+)?/apk/\s*\{[^{}]*alias\s+/var/www/familyrecipes-apk/\s*;[^{}]*\}'
if len(matches)==1 and not re.search(prefix,source):
    block=matches[0]
    if not re.search(r'alias\s+/var/www/familyrecipes-apk/FamilyRecipes-latest\.apk\s*;',block.group()):
        raise SystemExit("APK alias 与已确认路径不符，未修改配置")
    replacement='''location ^~ /apk/ {
        alias /var/www/familyrecipes-apk/;
        autoindex off;
        add_header Cache-Control "no-store" always;
    }'''
    source=source[:block.start()]+replacement+source[block.end():]
elif len(matches)==0 and len(re.findall(prefix,source))==1:
    pass
else:
    raise SystemExit("下载配置与预期不符，未修改配置；请核对 /apk/ location")
Path('nginx-next.txt').write_text(source)
PY
mysql -NBe "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='family_recipes' AND table_name='external_recipe'" | grep -qx 1
fr_backup="/opt/familyrecipes/backups/$(date +%Y%m%d-%H%M%S)-before-${fr_version}"
mkdir -p "$fr_backup"
cp -p "$fr_nginx" "$fr_backup/nginx-familyrecipes.conf"
cp -p /opt/familyrecipes/backend-1.0.0.jar "$fr_backup/backend-1.0.0.jar"
mysqldump --single-transaction --no-tablespaces family_recipes | gzip > "$fr_backup/family_recipes.sql.gz"
if [[ -f /var/www/familyrecipes-apk/FamilyRecipes-latest.apk ]]; then
    cp -p /var/www/familyrecipes-apk/FamilyRecipes-latest.apk "$fr_backup/FamilyRecipes-latest.apk"
fi
# Rerunnable migrations. The public-recipes migration deliberately publishes legacy private recipes.
# Never run schema.sql or data.sql against this database.
for fr_column in notes sort_order; do
    fr_exists=$(mysql -NBe "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='family_recipes' AND table_name='external_recipe' AND column_name='$fr_column'")
    if [[ "$fr_exists" == 0 ]]; then
        if [[ "$fr_column" == notes ]]; then
            mysql family_recipes -e 'ALTER TABLE external_recipe ADD COLUMN notes VARCHAR(2000) NULL'
        else
            mysql family_recipes -e 'ALTER TABLE external_recipe ADD COLUMN sort_order INT NOT NULL DEFAULT 0'
        fi
    fi
done
echo "迁移菜谱可见性：旧私有菜谱将改为公开，群组可见保持不变。数据库备份：$fr_backup/family_recipes.sql.gz"
for fr_migration in 2026-11-08-translations.sql 2026-11-08-recommendation-signals.sql 2026-11-08-receipt-import.sql 2026-11-08-public-recipes.sql 2026-11-09-recipe-classification.sql 2026-11-10-cuisine-refinement.sql; do
    mysql family_recipes < "$fr_migration"
done
if [[ -f /var/www/familyrecipes-apk/release.json ]]; then
    cp -p /var/www/familyrecipes-apk/release.json "$fr_backup/release.json"
fi
rollback() {
    fr_status=$?
    trap - ERR
    set +e
    echo "失败位置：第 ${BASH_LINENO[0]} 行，退出码 $fr_status"
    cat "$fr_backup/nginx-familyrecipes.conf" > "$fr_nginx"
    nginx -t && systemctl reload nginx
    echo "发布未完成，恢复旧 JAR；新增数据库字段保留（兼容旧版本）。备份：$fr_backup"
    cp -p "$fr_backup/backend-1.0.0.jar" /opt/familyrecipes/backend-1.0.0.jar.rollback
    mv /opt/familyrecipes/backend-1.0.0.jar.rollback /opt/familyrecipes/backend-1.0.0.jar
    if [[ -f "$fr_backup/FamilyRecipes-latest.apk" ]]; then
        cp -p "$fr_backup/FamilyRecipes-latest.apk" /var/www/familyrecipes-apk/FamilyRecipes-latest.apk
    fi
    if [[ -f "$fr_backup/release.json" ]]; then
        cp -p "$fr_backup/release.json" /var/www/familyrecipes-apk/release.json
    else
        rm -f /var/www/familyrecipes-apk/release.json
    fi
    systemctl restart familyrecipes || true
    exit "$fr_status"
}
trap rollback ERR
echo "更新官网下载路由…"
cat nginx-next.txt > "$fr_nginx"
nginx -t
systemctl reload nginx
echo "更新后端并等待健康检查…"
# Preserve the existing owner/mode used by the service.
cp -p /opt/familyrecipes/backend-1.0.0.jar /opt/familyrecipes/backend-1.0.0.jar.next
cat backend-1.0.0.jar > /opt/familyrecipes/backend-1.0.0.jar.next
mv /opt/familyrecipes/backend-1.0.0.jar.next /opt/familyrecipes/backend-1.0.0.jar
systemctl restart familyrecipes
fr_ready=false
for attempt in {1..45}; do
    if curl -fsS --max-time 3 http://127.0.0.1:8080/api/recipes/search -o health.json &&
       python3 -c 'import json; assert json.load(open("health.json"))["code"] == 200' 2>/dev/null; then
        fr_ready=true; break
    fi
    sleep 2
done
[[ "$fr_ready" == true ]]
install -m 0644 FamilyRecipes-${fr_version}.apk /var/www/familyrecipes-apk/FamilyRecipes-${fr_version}.apk.next
mv /var/www/familyrecipes-apk/FamilyRecipes-${fr_version}.apk.next /var/www/familyrecipes-apk/FamilyRecipes-${fr_version}.apk
install -m 0644 FamilyRecipes-${fr_version}.apk /var/www/familyrecipes-apk/FamilyRecipes-latest.apk.next
mv /var/www/familyrecipes-apk/FamilyRecipes-latest.apk.next /var/www/familyrecipes-apk/FamilyRecipes-latest.apk
curl -fsS --max-time 60 --resolve familyrecipes.live:443:127.0.0.1 https://familyrecipes.live/apk/FamilyRecipes-${fr_version}.apk -o downloaded.apk
cmp FamilyRecipes-${fr_version}.apk downloaded.apk
curl -fsS --max-time 15 --resolve familyrecipes.live:443:127.0.0.1 https://familyrecipes.live/ -o homepage.html
grep -q '/apk/release.json' homepage.html
systemctl is-active --quiet familyrecipes
python3 - "$fr_version" <<'PYMETA'
import datetime, json, os, sys
version=sys.argv[1]
from pathlib import Path
root=Path('/var/www/familyrecipes-apk')
data=dict(version=version,url=f'/apk/FamilyRecipes-{version}.apk',
          bytes=(root/f'FamilyRecipes-{version}.apk').stat().st_size,
          publishedAt=datetime.datetime.now(datetime.timezone.utc).isoformat())
tmp=root/'release.json.next'
tmp.write_text(json.dumps(data))
os.chmod(tmp,0o644)
tmp.replace(root/'release.json')
PYMETA
curl -fsS --max-time 15 --resolve familyrecipes.live:443:127.0.0.1 https://familyrecipes.live/apk/release.json -o published.json
cmp /var/www/familyrecipes-apk/release.json published.json
trap - ERR
echo "发布成功：${fr_version}，https://familyrecipes.live/ 。备份：$fr_backup"
