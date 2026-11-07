# 单机运行与验证

本轮以全新数据库为前提。运行一个 Spring Boot 进程、一个 MySQL，媒体保存在独立本地目录。详细决策见 [迭代记录](../docs/iterations/2026-11-01-foundation-review/05-family-kitchens.md)。

## 初始化

### 本机使用 Docker（无需安装 mysql 命令）

仓库根目录执行：

```sh
docker compose --env-file backend/.env -f backend/compose.local.yml up -d --wait
```

连接地址为 `127.0.0.1:3308`，数据库 `family_recipes`，用户名和密码见本机 `backend/.env`。该文件已被 Git 忽略；本次已生成随机数据库密码和 JWT 密钥。其他开发机需自行创建 `.env`，并额外配置 `LOCAL_DB_ROOT_PASSWORD`；将 `DB_URL` 的端口设为 3308。

首次启动会自动执行建表和种子脚本；后续启动保留数据，不重复初始化。数据保存在 Docker 卷 `familyrecipes-dev_mysql-data`。Android 模拟器配置使用 `http://10.0.2.2:8080`；真机测试时把 `PUBLIC_BASE_URL` 改为电脑局域网 API 地址。

```sh
# 查看状态
docker compose --env-file backend/.env -f backend/compose.local.yml ps

# 进入数据库终端，提示密码时输入 .env 中的 DB_PASSWORD
docker compose --env-file backend/.env -f backend/compose.local.yml exec mysql mysql -u familyrecipes -p family_recipes

# 停止，保留数据；再次运行 up -d --wait 即可恢复
docker compose --env-file backend/.env -f backend/compose.local.yml stop
```

不要在需要保留本地数据时使用 `down -v`，它会删除数据库卷。

### 已有 MySQL 时手动初始化

需要 JDK 17、Maven、MySQL 8；本轮数据库测试使用 MySQL 8.4。

先创建空数据库及专用应用账号，再按顺序执行（以下在仓库根目录）：

```sh
mysql -u root -p family_recipes < backend/src/main/resources/schema.sql
mysql -u root -p family_recipes < backend/src/main/resources/data.sql
```

应用账号需要该库的 SELECT/INSERT/UPDATE/DELETE 权限。不要再执行旧 migrations 或 schema_no_fk.sql。脚本不是重复执行的迁移工具；应用启动不会自动建表或插入种子。

## 配置与启动

本机 `.env` 与 JAR 已准备好时，在仓库根目录执行：

```sh
bash backend/scripts/run-local.sh
```

该脚本加载本机环境配置，在 macOS 自动选择 JDK 17，并以前台方式启动服务。保持终端打开，按 Ctrl+C 停止。后端地址为 `http://localhost:8080`，可访问 `/api/recipes/search` 检查数据库连接。修改后端代码后须重新打包再启动，脚本不会自动编译。

在 backend 中复制 `.env.example` 为 `.env` 并填入真实值：

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`：数据库连接。
- `JWT_SECRET`：用 `openssl rand -hex 32` 生成；没有内置兜底值。
- `MEDIA_DIR`：例如 `/var/lib/familyrecipes/media`，事先创建并赋予运行用户写权限；不要放在每次发布会替换的目录里。
- `PUBLIC_BASE_URL`：手机能访问的 API 地址，不带末尾斜杠。例如线上 HTTPS 域名，或局域网服务器地址。不要把 localhost 返回给真机。

```sh
cd backend
set -a
source .env
set +a
mvn test
mvn package
java -jar target/backend-1.0.0.jar
```

图片通过 `/api/upload/files/` 鉴权读取；代理将该路径与其他 API 一起转给应用，不能为媒体目录配置公开的静态映射。代理 `/ws/chat` 时需要支持 WebSocket Upgrade。

数据库与媒体目录共同备份。当前未使用 S3，也不需要 Redis 或 MinIO。公开图片被改为私有后，服务器会拒绝后续无权限读取；已经被他人下载的内容无法撤回。

## 验证

```sh
# 仅单元测试；数据库集成测试会明确显示 skipped
cd backend
mvn test

# 在仓库根目录运行，自动启动和清理独立的测试 MySQL
backend/scripts/test-foundation.sh

# Android：JDK 17，SDK 34 / build-tools 34.0.0
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Docker 测试脚本可设置 `DOCKER_BIN` 指定 Docker 路径；`MAVEN_TEST_CACHE` 可指向 Maven 缓存目录或卷。测试不连接应用数据库。

Android 真机测试：登录两个账号，验证私有/公开菜谱、库存越权、编辑保留步骤、图片权限、群聊发送与历史合并、退出页面与切换账号。编译成功不能代替这些设备测试。

## 在 Android 模拟器验收 UI

1. 保持 Docker 中的数据库和上述后端服务运行。
2. Android Studio 打开仓库中的 `android` 目录，在 Device Manager 启动模拟器，选择 `app` 的 debug 版本后点击 Run。debug 默认连接 `http://10.0.2.2:8080`，这是模拟器访问 Mac 的地址。
3. 也可以安装已编译的 `android/app/build/outputs/apk/debug/app-debug.apk`，无需重新构建。
4. 进入 App 的“我的”，点击注册和“获取验证码”；在电脑打开 `http://localhost:8025` 查看本地邮件，填入验证码注册（没有预设账号），然后在消息页创建家庭群组，再通过顶部厨房入口选择厨房。公共菜谱可以通过首页/搜索查看。
5. 按 [本轮验收清单](../docs/iterations/2026-11-01-foundation-review/05-family-kitchens.md#本机验收顺序) 检查库存、图文收藏、草稿、群聊、提醒等。

`http://localhost:8080` 提供后端与项目静态介绍页面；需要在 Android App 中查看实际移动端 UI。

## 邮箱验证码

本地 Docker Compose 同时启动 Mailpit（SMTP `127.0.0.1:1025`，网页 `http://localhost:8025`）。本机验证码邮件仅进入测试箱，不会投递到真实邮箱。当前注册必须填写验证码；登录仍使用邮箱和密码。真实发信配置、限流规则见 [邮箱验证码迭代记录](../docs/iterations/2026-11-01-foundation-review/06-email-registration.md)。

## 更新现有云服务器

旧服务器线索、如何找回启动方式、当前版本配置及数据库注意事项见 [DigitalOcean 现有部署核对与更新](../docs/deployment/digitalocean.md)。不要直接沿用旧 APK 上传脚本：当前 debug 包连接本机后端。
