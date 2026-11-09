# 佳肴发版 SOP：暖色 UI、双语、小票与公开菜谱

适用基线：线上 1.0.5，Ubuntu 24.04 + MySQL 8 + Java 17 + systemd + Nginx。目标版本建议 1.0.6（versionCode 7）。本文与脚本已保存，尚未执行线上发布、全量导入或全量翻译。

顺序：本地验收 → 新后端及 APK → 导入 5 条 → 验收 → 全量导入 → 后台翻译。保留现有用户和数据，不执行 schema.sql/data.sql，也不删除数据库或上传目录。

## 1. 本地验收和准备发布包（Mac）

```bash
cd /Users/wangyang/Documents/personal_projects/FamilyRecipes
bash backend/scripts/test-foundation.sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
export GRADLE_USER_HOME=/private/tmp/fr-gradle-cache
bash android/gradlew -p android :app:assembleDebug :app:testDebugUnitTest --console=plain
```

模拟器验收入口：登录 → 选择厨房 → `＋` → 食材 → 小票识别入库。选图/拍照后，修改名称、数量、储存方式、预计保存天数；移除非食品后确认入库。估计天数从实际入库当天起算。空值需要补填。未提交草稿可退出恢复；提交结果未知时，只能重试原批次，服务端防止重复添加。小票图片仅用于识别，不作为库存照片保存到后端；调用第三方模型会传输图片。

本地后端读取 backend/.env；小票需要 ZHIPU_API_KEY 或 VISION_LLM_KEY。不要把 key 写进 Android 或 Git。物品照片识别仍只是预留同一视觉模型路由，不是本次已实现的业务入口。

本次已将 `android/app/build.gradle` 的 `versionName` 更新为 `1.0.6`、`versionCode` 更新为 `7`，无需手动修改。后续发版时再更新版本并递增 versionCode。保留原 release keystore 和 alias，不重新生成签名。将 keystore 与密码另外安全备份。

```bash
bash scripts/prepare-release.sh 1.0.6
```

产物：`familyrecipes-release/1.0.6/`。此脚本仅构建，不上传。目录包含 APK、JAR、3 份增量 SQL、部署脚本、VERSION 和 SHA256SUMS；目录被 Git 忽略。构建使用已有签名配置。可以用 SDK build-tools 中的 `apksigner verify --print-certs` 对比新旧 APK 的签名证书 SHA-256，必须一致才能覆盖安装。

## 2. 配置服务器模型与数据管理员（服务器终端）

```bash
ssh root@152.42.242.92
install -d -m 700 /etc/familyrecipes
umask 077
vim /etc/familyrecipes/ai.env
```

在文件内填写真实 key 与管理员 ID（不要保留占位符；也不要把内容粘回聊天）：

```dotenv
ZHIPU_API_KEY=填写实际密钥
TRANSLATION_ENABLED=false
TRANSLATION_LLM_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
TRANSLATION_LLM_MODEL=glm-5.3-flash
TRANSLATION_VERSION=v1
VISION_LLM_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
VISION_LLM_MODEL=glm-4.6v-flash
VISION_THINKING_TYPE=disabled
VISION_TIMEOUT_SECONDS=45
VISION_MAX_TOKENS=2048
CATALOG_ADMIN_USER_ID=填写数据管理员用户ID
```

管理员需为已注册账号，可使用专用数据账号。服务器查询 ID：

```bash
mysql family_recipes -e 'SELECT id,username,email FROM user ORDER BY id;'
chmod 600 /etc/familyrecipes/ai.env
install -d /etc/systemd/system/familyrecipes.service.d
vim /etc/systemd/system/familyrecipes.service.d/90-ai.conf
```

新增 drop-in 内容，保留现有服务的数据库、JWT、邮件等配置：

```ini
[Service]
EnvironmentFile=/etc/familyrecipes/ai.env
```

```bash
systemctl daemon-reload
```

如果旧 `/opt/familyrecipes/application-prod.yml` 显式写了 `app.ai` / `app.translation` / `app.catalog` 的固定值，需要删除该项或改成相应 `${环境变量名:默认值}`，避免外部高优先级配置覆盖 JAR 内的占位符。不要在排查时打印整个配置文件或 systemctl 的 Environment 字段。

此时先不单独重启，随下一步部署重启。全量翻译保持关闭，小票可独立使用视觉模型。

## 3. 上传发布包（Mac 终端）

```bash
cd /Users/wangyang/Documents/personal_projects/FamilyRecipes
ssh root@152.42.242.92 'mkdir -p /root/familyrecipes-release/1.0.6'
scp familyrecipes-release/1.0.6/* root@152.42.242.92:/root/familyrecipes-release/1.0.6/
ssh -t root@152.42.242.92 'bash /root/familyrecipes-release/1.0.6/deploy-current-server.sh 1.0.6'
```

输入的是服务器 SSH 密码，不是数据库或模型密码。执行直到明确显示“发布成功”；仅看到文件传输完成不表示部署成功。

部署脚本会校验 SHA256、备份 JAR/数据库/APK/下载元数据/Nginx，执行三份增量迁移，更新 JAR、重启并检查健康，再发布 APK。支持当前 `/apk/` alias 或旧单文件 alias。保留 TLS 和 API 代理配置。实际部署成功时才写 `release.json.publishedAt`。官网暖色页面与电饭煲 SVG 随新 JAR 生效。

脚本对部署应用阶段的失败会恢复旧 JAR、下载元数据与 Nginx，保留兼容的新增表；迁移阶段失败则停止，不切换应用。保留终端打印的备份路径。

## 4. 发布后验收

服务器：

```bash
systemctl status familyrecipes --no-pager
journalctl -u familyrecipes -n 80 --no-pager
curl -fsS https://api.familyrecipes.live/api/recipes/search
curl -fsS https://familyrecipes.live/apk/release.json
```

手机访问 https://familyrecipes.live/ ：确认暖色、电饭煲、版本及本次实际更新时间；下载并覆盖安装。验证注册/登录、厨房切换、消息、离线草稿、菜谱、图片、中文/英文界面。小票识别选一张清晰照片，删除误识别项、补齐天数，确认库存只增加一次。

后台翻译仍关闭，因此已有内容显示原文是预期行为。未配置正式邮件服务时，注册验证码仍受原邮件测试箱/隧道限制；本次发布不会自动替你开通生产发信服务。

## 5. 整理和小批导入（Mac）

本轮最后重试完成后，不需要重新抓取就能导入已下载内容。

```bash
python3 backend/scripts/seed/normalize_catalog.py
python3 -m unittest discover -s backend/scripts/seed -p "test_*.py"
python3 backend/scripts/seed/import_catalog.py
python3 backend/scripts/seed/import_catalog.py --url https://api.familyrecipes.live --apply --limit 5 --login-email 数据管理员邮箱
```

把“数据管理员邮箱”替换为已注册账号的邮箱，脚本隐藏提示输入该账号密码，登录 token 只在进程内存中使用，不打印也不落盘。账号 ID 必须等于配置的 CATALOG_ADMIN_USER_ID。也可省略 --login-email 使用已有 token 的隐藏输入。所有导入走后端权限和媒体接口，不直连生产数据库。

检查 5 条：封面与正文/步骤是否正常，原始来源可在“内容来源与授权”查到；其他用户不能编辑原版，可以另存自己的做法。留意没有图片、菜系/口味待确认的记录；当前数据并未满足“全部热门、半数华人菜谱、每条有过程图”的理想目标。

确认后执行：

```bash
python3 backend/scripts/seed/import_catalog.py --url https://api.familyrecipes.live --apply --login-email 数据管理员邮箱
```

中断后可重跑，同来源 ID 会跳过；不会按名称做语义去重。上传前会自动将 WebP 等格式转换为 PNG，并按后端限制处理超大图片；原文件及原始哈希保留。Mac 使用自带 sips；其他平台需在运行脚本的 Python 环境安装 Pillow。转换缓存位于 data/seed-candidates/normalized/upload-images。图片上传缓存按服务器和管理员隔离，位于 data 下。不要在重新导入时删除已入库的 recipe_provenance 去重记录。

## 6. 开启后台翻译（服务器）

```bash
vim /etc/familyrecipes/ai.env
# 将 TRANSLATION_ENABLED=false 改成 true
systemctl restart familyrecipes
```

新旧内容均进入队列，保留原文，暂未翻译的字段回退原文。聊天任务优先于普通内容，已翻译相同原文会复用缓存。不是一次请求翻译整库；按字段调用模型，全部翻译完需要时间和额度。

```bash
mysql family_recipes -e 'SELECT status,COUNT(*) AS total FROM content_translations GROUP BY status;'
mysql family_recipes -e 'SELECT kind,status,COUNT(*) AS total FROM content_translations GROUP BY kind,status;'
```

也可用管理员 token 调用 `GET /api/catalog/status`；失败项修复配置后通过 `POST /api/catalog/translations/retry` 重试。遇到费用、额度或质量问题，先把 TRANSLATION_ENABLED 改回 false 并重启；缓存不删除，原业务继续可用。更换模型/术语后需要重新翻译时递增 TRANSLATION_VERSION。

## 7. 回滚

应用或下载问题先回滚应用，不自动恢复数据库。新增表可兼容旧版。以下在服务器执行，将路径替换为部署输出的真实备份目录：

```bash
fr_backup=/opt/familyrecipes/backups/替换为实际目录
# 暂停后台翻译，然后恢复旧程序
vim /etc/familyrecipes/ai.env
systemctl stop familyrecipes
cp -p "$fr_backup/backend-1.0.0.jar" /opt/familyrecipes/backend-1.0.0.jar
cp -p "$fr_backup/release.json" /var/www/familyrecipes-apk/release.json
cp -p "$fr_backup/FamilyRecipes-latest.apk" /var/www/familyrecipes-apk/FamilyRecipes-latest.apk
systemctl start familyrecipes
```

如果本次修改了 Nginx 且需要回滚，将备份 nginx-familyrecipes.conf 恢复到 `/etc/nginx/sites-enabled/familyrecipes` 指向的实际文件后，`nginx -t && systemctl reload nginx`。新版版本号 APK 可保留，但 release.json 指向旧版。

已经安装到手机的新 APK 不能靠修改网页自动降级；优先发布更高 versionCode 的修复版，保留原签名。不要为了降级让用户随便卸载丢失本地草稿。

数据库备份只用于确定发生数据损坏且接受丢失备份后的新增数据时，由管理员停服后定向恢复；不把整库回滚作为常规发版操作。媒体图片目录仍沿用现有路径，单独纳入服务器备份。
