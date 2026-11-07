# 现有版本部署与重置数据库

2026-11-07。适用：DigitalOcean 上现有 Linux 服务器、MySQL 8、Java 17。本次仅编写说明，没有连接服务器、清库或发布。若实际是 OceanBase，先确认数据库类型，本说明不能直接套用。

## 部署范围

服务器需要后端 JAR、MySQL、可写的本地媒体目录、邮件 SMTP 和现有 HTTPS/Nginx。Android UI 在 APK 内，不需要部署 Web 前端，但测试者必须安装当前 Android 版本并连接线上地址。AI 功能仍为计划，不包含在本次部署。

当前 debug API 默认 http://10.0.2.2:8080，release 默认 https://api.familyrecipes.live；旧 APK 上传脚本传的是 debug 包，不可直接用作线上验收。当前工作区存在未提交改动，仅在服务器 git pull 不能取得这些修改；从当前本机工作区构建并上传 JAR 和 SQL 可确保对应。

## 已核实的这台机器

用户提供：`root@152.42.242.92`，Ubuntu 24.04.4 LTS，MySQL 8.0.46，业务库 `family_recipes`；root 在 shell 直接执行 `mysql` 可以管理。网站是 `https://familyrecipes.live/`，API 域名仍待 Nginx 核对；不要因官网域名而直接改掉 release 的 API 子域名。已进一步确认服务名 `familyrecipes.service`、JAR `/opt/familyrecipes/backend-1.0.0.jar`，三个域名同站代理至 8080；运行用户、环境文件和新版媒体路径待确认。[下一步只读排查](digitalocean.md#2026-11-07-用户提供的主机信息)。

## 1. 查明旧服务（服务器，只读）

先按 [现有部署核对](digitalocean.md#1-找回原部署方式只读) 查 systemd、Docker、Nginx、JAR 路径、数据库主机/端口/库名、运行用户、MEDIA_DIR。历史 IP 只是线索，先在控制台确认。不要贴出密钥或完整环境配置。

下文主流程假设：后端使用 systemd、MySQL 在本机且可用 `sudo mysql` 管理。若实际为 Docker／远程数据库，使用末尾分支。示例服务名 `familyrecipes`、目录 `/opt/familyrecipes`、数据库 `family_recipes` 都必须替换为查明的实际值。

## 2. 构建与上传（Mac 仓库根目录）

有 Maven 时使用 Java 17：

```sh
cd /Users/wangyang/Documents/personal_projects/FamilyRecipes
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -f backend/pom.xml -DskipTests package
```

本机没有 Maven 可用 Docker（此步骤打包，不等于完成集成测试）：

```sh
docker run --rm -v familyrecipes-test-maven:/root/.m2 \
  -v "$PWD/backend:/workspace" -w /workspace \
  maven:3.9.9-eclipse-temurin-17 mvn -B -ntp -DskipTests package
```

不要在本机后端运行时覆盖它正在使用的同路径 JAR；先停止该本地 Java 进程再打包。需重跑后端集成测试时用 `bash backend/scripts/test-foundation.sh`（需要 Docker）。

下面的 `实际SSH目标` 换成已确认的 `用户名@IP`，先创建仅自己可读的上传目录：

```sh
ssh 实际SSH目标 'mkdir -p ~/familyrecipes-release && chmod 700 ~/familyrecipes-release'
scp backend/target/backend-1.0.0.jar backend/src/main/resources/schema.sql backend/src/main/resources/data.sql 实际SSH目标:~/familyrecipes-release/
shasum -a 256 backend/target/backend-1.0.0.jar
```

服务器用 `sha256sum ~/familyrecipes-release/backend-1.0.0.jar` 对照。

## 3. 停服务并按需留备份（服务器）

```sh
sudo systemctl stop familyrecipes
sudo ss -ltnp | grep ':8080'
```

确认没有旧应用继续写库。虽然不做数据迁移，仍可在清理前留一个可回退副本；不需要保留的测试数据可以省略备份步骤：

```sh
umask 077
backup_dir="$HOME/familyrecipes-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"
sudo mysqldump --single-transaction --routines --events --triggers --databases family_recipes > "$backup_dir/database.sql"
```

检查命令退出码为 0 且备份不是空文件；备份旧 JAR、环境文件和实际媒体目录时也使用受限目录，不上传 Git。回退需恢复成套的旧 JAR、数据库、配置和媒体，不能只退 JAR。

## 4. 清空并重建数据库（破坏性，仅执行已确认库名）

这会删除全部账号、库存、群组、聊天、收藏、点菜、备菜等旧记录。不要删除整个 MySQL 数据目录或其他应用的库。先核对连接实例和库名：

```sh
sudo mysql -e 'SELECT @@hostname, @@port, VERSION(); SHOW DATABASES;'
```

确认目标确实为 `family_recipes` 后执行：

```sh
sudo mysql <<'SQL'
DROP DATABASE IF EXISTS family_recipes;
CREATE DATABASE family_recipes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL
sudo mysql --default-character-set=utf8mb4 family_recipes < "$HOME/familyrecipes-release/schema.sql"
# 上一步成功后才继续；导入失败不要启动后端。
sudo mysql --default-character-set=utf8mb4 family_recipes < "$HOME/familyrecipes-release/data.sql"
sudo mysql family_recipes -e 'SHOW TABLES; SELECT COUNT(*) AS ingredients FROM ingredient; SELECT COUNT(*) AS accounts FROM user;'
```

`schema.sql` 已包含新的聊天授权表，不需要额外运行旧迁移脚本。`data.sql` 包含公共配置与种子数据，不提供默认登录账号。两个 SQL 只对新空库执行一次；不能重复导入。Spring 配置是 `spring.sql.init.mode=never`，重启不会建表。

删除数据库不会自动删除数据库用户及其授权。沿用旧应用数据库用户时核对授权与连接是否仍正确；新用户只授予目标应用库权限，不使用 root 跑应用。参见 [MySQL DROP DATABASE](https://dev.mysql.com/doc/refman/8.4/en/drop-database.html)、[备份说明](https://dev.mysql.com/doc/refman/8.4/en/mysqldump.html)。

## 5. 隔离旧媒体、失效旧登录

清库不会清理磁盘图片。最简单的是使用一个新的空目录，不把旧文件混进新库。例如（运行用户按实际替换）：

```sh
sudo install -d -o familyrecipes -g familyrecipes -m 0750 /var/lib/familyrecipes/media-clean-20261107
```

将 `MEDIA_DIR` 指向新目录。旧目录留作回退，确认无需要后再删除那个已核对的目录，不要通配删除 `/var/lib`、Docker 卷或 `/opt`。

**重置数据库后更换 JWT_SECRET**，避免旧 token 在用户 ID 重新分配后仍有效。用 `openssl rand -hex 32` 生成并填入服务器环境文件；若单独设置了 EMAIL_CODE_SECRET，也更换。客户端清除应用数据后重新注册，避免旧离线队列、草稿、厨房 ID 和缓存进入新账号。

## 6. 配置、替换 JAR、启动

编辑旧服务实际使用的 EnvironmentFile（不是仅在当前终端 export）。按 `backend/.env.example` 核对：

- DB_URL：实际 MySQL 主机、端口、`family_recipes` 库；DB_USERNAME/DB_PASSWORD。
- JWT_SECRET：上一步新生成的值。
- MEDIA_DIR：新的空媒体目录。
- PUBLIC_BASE_URL：`https://api.familyrecipes.live`，无末尾斜杠。
- SMTP_*：有效的邮件服务或下节的内测 Mailpit。

环境文件由 root 管理并限制读取，例如权限 600；systemd 的 EnvironmentFile 使用 `KEY=value`，不要加 `export` 或写 shell 命令替换。参见 [systemd 环境文件](https://www.freedesktop.org/software/systemd/man/260/systemd.exec.html)。

在旧服务已停止后，将新 JAR 放到实际 ExecStart 指定路径。以下路径只是主流程示例：

```sh
sudo install -m 0644 "$HOME/familyrecipes-release/backend-1.0.0.jar" /opt/familyrecipes/backend-1.0.0.jar
sudo systemctl start familyrecipes
sudo systemctl status familyrecipes --no-pager
sudo journalctl -u familyrecipes -n 100 --no-pager
```

若修改 unit 文件则先 `sudo systemctl daemon-reload`；只改环境文件，重启服务即可。保留现有 HTTPS 配置，按 [反向代理说明](digitalocean.md#4-反向代理与验收) 核对 `/api/`、受保护媒体和 `/ws/chat`，不要把媒体目录直接作为公开静态目录。

## 7. 暂无 SMTP：继续用 Mailpit 做内测

服务器已经安装 Docker、1025/8025 没被占用时，可单独启动邮件测试箱，不必为此启动仓库内的整套本地数据库：

```sh
docker run -d --name familyrecipes-mailpit --restart unless-stopped \
  -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 \
  -e MP_DATABASE=/data/mailpit.db -v familyrecipes-mailpit:/data \
  axllent/mailpit:latest
```

已有同名容器时先检查并复用，不重复创建。设置 SMTP_HOST=127.0.0.1、SMTP_PORT=1025、SMTP_AUTH=false、SMTP_STARTTLS=false、SMTP_SSL=false、SMTP_FROM=noreply@familyrecipes.test；USERNAME/PASSWORD 留空。此配置适用于后端跑在宿主机的情况。

在 Mac 运行隧道（本机 8025 已被本地测试箱占用，因此改用 18025）：

```sh
ssh -N -L 18025:127.0.0.1:8025 实际SSH目标
```

打开 `http://localhost:18025` 看服务器收集的验证码邮件。**这些邮件不会真正发到 QQ/Gmail 等邮箱**；只适合你自己内测，邀请普通用户前需配置真实 SMTP。不要把测试箱端口公开到公网。[Mailpit Docker 文档](https://mailpit.axllent.org/docs/install/docker/)

## 8. Android 包与验收

已配置 release keystore 的本机执行（不要提交签名文件或密码）：

```sh
ANDROID_HOME="$HOME/Library/Android/sdk" JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  android/gradlew -p android :app:assembleRelease
```

APK 位于 `android/app/build/outputs/apk/release/app-release.apk`。release 与 debug 包名不同，可能同时安装；线上验收使用 release，避免误开旧 debug。签名配置缺失时构建会失败，需要先配置 `android/keystore.properties`；不要临时覆盖正式签名或修改现有 debug 包地址来冒充 release。

清除准备验收的应用数据（会删除本机草稿和待发送消息），重新注册。模拟器可使用设置→应用→存储→清除存储；真机同理。仅安装更新 APK 不会自动清除数据。

服务器检查：

```sh
curl -fsS http://127.0.0.1:8080/api/recipes/search
curl -fsS https://api.familyrecipes.live/api/recipes/search
```

除了 HTTP 成功还需检查 JSON 业务成功码。按顺序验收：验证码与注册 → 默认厨房 → 库存分类与添加 → 菜谱及图片 → 两账号群聊／@／表情 → 点菜备菜分享及只读权限 → 断线重连。旧数据库账号已消失，必须重新注册。

## 原部署不是宿主机 systemd 时

- Docker 后端：先停止对应后端容器，用原 Compose 文件更新镜像／JAR 挂载，保留精确路径和端口。容器内的 localhost 是容器自己；数据库和 Mailpit 同网络时用服务名，不能照抄宿主机的 127.0.0.1。MEDIA_DIR 指向有持久挂载的容器内路径。不要 `docker compose down -v`。
- Docker MySQL：可用 `docker exec -it 实际数据库容器 mysql -uroot -p` 登录核对并重建库。将 SQL 用 `docker cp` 复制到该容器的 `/tmp`，在 MySQL 会话先 `USE family_recipes;` 再依次 `SOURCE /tmp/schema.sql;`、`SOURCE /tmp/data.sql;`。仅在前一步成功后继续。不要把数据库密码写到命令行。
- 远程 MySQL：使用实际 host/port/user 和交互密码连接同一目标实例，执行同样的核对、备份和 SQL；不要操作服务器上另一个本地 MySQL。
- nohup/手动 Java：按旧命令与环境停止、替换、重启，先确认没有占用 8080 的旧进程；建议后续单独统一成 systemd。

这些分支需要实际部署信息才能给出准确替换命令；不能凭历史 IP 推定当前部署方式。

## 启动故障修正：日志目录权限

2026-11-07 用户部署日志显示 Logback 无法创建 `/opt/familyrecipes/logs/family-recipes.log`。部署目录由 root 管理，新服务以 familyrecipes 运行，因此需提前单独创建可写日志目录（此前步骤遗漏）。无需重建数据库或重新打包：

```sh
systemctl stop familyrecipes
install -d -o familyrecipes -g familyrecipes -m 0750 /opt/familyrecipes/logs
chown -R familyrecipes:familyrecipes /opt/familyrecipes/logs
systemctl reset-failed familyrecipes
systemctl start familyrecipes
journalctl -u familyrecipes --since '2 minutes ago' --no-pager
curl -fsS http://127.0.0.1:8080/api/recipes/search
```

等待应用启动完成后检查接口。刚启动时 systemd 的 active(running) 仅证明进程已创建，不代表 Spring Boot 完成启动。不要将整个 `/opt/familyrecipes` 改为全员可写。
