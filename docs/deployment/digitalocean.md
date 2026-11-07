# DigitalOcean 现有后端部署核对与更新

2026-11-07。这里只整理操作说明，本轮没有修改线上机器。

## 已知事实与待确认项

### 2026-11-07 用户提供的主机信息

- 确认的主机：`152.42.242.92`，主机名 `familyrecipes-server`，Ubuntu 24.04.4 LTS x86_64。
- MySQL 为宿主机可直接访问的 8.0.46；root 登录执行 `mysql` 可进入，存在业务库 `family_recipes`。
- `/root/family_recipes/` 和 `/root/init.sql` 存在，但不能据此推断应用实际 JAR、启动方式或该旧 SQL 与当前代码兼容。重建使用本仓库最新 SQL，不使用旧 `/root/init.sql`。
- 用户确认网站为 `https://familyrecipes.live/`。本轮读取到的是旧下载介绍页（显示 2026-02-13）；不是 API 可用性的证据。仓库 release 仍配置 `https://api.familyrecipes.live`，两者可能分别承担官网与 API，需要核对 Nginx 后再确定。
- 本轮网页工具无法读取两个域名的 `/api/recipes/search`，不能据此判断服务宕机或接口是否存在。
- 登录提示系统需要重启；尚未执行更新或重启，不将其与应用发布混在一起。

用户目前在 `mysql>`，先用 `exit;` 返回服务器 shell，再执行下面的只读排查。先不清库：

```sh
systemctl list-units --type=service --all --no-pager | grep -Ei 'family|recipe|nginx|mysql|docker'
ss -ltnp | grep -E ':8080|:80 |:443 |:3306'
pgrep -af java
find /root/family_recipes /opt/familyrecipes -maxdepth 3 -type f \( -name '*.jar' -o -name 'compose*.yml' -o -name 'docker-compose*.yml' -o -name '*.service' \) 2>/dev/null
nginx -T 2>/dev/null | grep -E 'server_name|listen |location |proxy_pass|root |alias '
java -version
```

如果查到服务名，再 `systemctl cat 实际服务名` 在服务器核对 ExecStart、WorkingDirectory 和 EnvironmentFile。只分享这几个路径字段；不要分享 Environment 密钥行、环境文件、历史记录或带密码的启动参数。此后才能把重建指南里的服务名和路径替换成精确命令。


- 历史 `upload_apk.sh` 指向 `root@152.42.242.92`，APK 目录 `/opt/familyrecipes/apk/`，域名 `api.familyrecipes.live`。这些是仓库线索，不能证明机器当前配置。
- 仓库没有实际线上 systemd unit、Nginx 配置或后端发布脚本；根 README 的 Dockerfile 仅为历史建议。
- 本机严格校验 SSH 时提示没有该机器已知主机密钥，因此未连接成功，尚未查明旧服务如何启动。可从 DigitalOcean 控制台登录 Droplet 查明，不要通过禁用主机校验绕过。
- DigitalOcean 是之前提到的服务器平台；如果本次确实指 OceanBase 数据库，则需要另行确认兼容模式。当前项目使用 MySQL JDBC 驱动和 MySQL SQL，只验证过 MySQL 8 系列。

## 1. 找回原部署方式（只读）

先在 DigitalOcean 的 Droplets 页面确认 IP，再打开该机器 Console，或验证 SSH 主机指纹后登录。官方：[SSH 登录](https://docs.digitalocean.com/products/droplets/how-to/connect-with-ssh/)、[控制台登录](https://docs.digitalocean.com/products/droplets/how-to/connect-with-console/)。

在服务器执行：

```sh
systemctl list-units --type=service --all --no-pager | grep -Ei 'family|recipe|nginx|mysql|docker'
sudo ss -ltnp | grep -E ':8080|:80 |:443 |:3306'
ls -la /opt/familyrecipes
command -v docker >/dev/null && sudo docker ps --format '{{.Names}} {{.Image}} {{.Ports}}'
pgrep -af 'java|backend'
```

有 systemd 服务：用 `systemctl cat 实际服务名` 查 ExecStart、WorkingDirectory、EnvironmentFile；有容器：用 `docker inspect --format '{{json .Mounts}}' 实际容器名` 查挂载，再找原 Compose 文件。`nginx -T` 可以核对域名、证书路径及 proxy_pass。查看输出时不要向聊天粘贴密码、令牌或 Environment 中的密钥。

先记下实际 JAR/镜像路径、服务名、运行用户、数据库位置和媒体目录，不要假设它们与下文示例相同。

## 2. 当前版本发布前必须准备的配置

JDK 17；MySQL 8；环境变量参考 `backend/.env.example`：

- DB_URL、DB_USERNAME、DB_PASSWORD。
- JWT_SECRET：至少 32 字节；已有部署保持原值可避免无意使全部登录失效。
- MEDIA_DIR：独立持久目录，运行用户可写，例如 `/var/lib/familyrecipes/media`。
- PUBLIC_BASE_URL：`https://api.familyrecipes.live`，不带末尾斜杠。
- SMTP_HOST/PORT/USERNAME/PASSWORD/FROM 与 AUTH/STARTTLS/SSL：真实邮件服务配置。当前本地 Mailpit 不会向用户邮箱发信，不能原样当正式发信服务。若只是服务器内测，可继续使用隔离的 Mailpit 并通过 SSH 隧道看邮件，勿向公网开放测试箱。

本轮重构的表结构与半年以前版本可能不同，**只替换 JAR 并不能保证运行**。按“未上线、无需迁移”的约定，可以新建另一数据库，顺序执行当前 `schema.sql`、`data.sql`，再把新应用指向它；不要直接对旧库执行初始化脚本。先保留旧数据库、旧 JAR 和旧媒体目录。应用 `spring.sql.init.mode=never`，不会替你建表。

## 3. 打包与更新

Mac 仓库根目录：

```sh
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn clean verify
```

产物 `backend/target/backend-1.0.0.jar`。将它上传为服务器上的**新文件**，检查校验和后，按第 1 步查明的原方式替换与重启：

- systemd：先备份旧 JAR，在原 ExecStart 对应位置放置新 JAR，保持运行用户可读，再 `sudo systemctl restart 实际服务名`，随后 `sudo journalctl -u 实际服务名 -n 100 --no-pager`。
- Docker Compose：复用实际 Compose/镜像构建方式，更新后端服务；保留数据库与媒体卷，勿执行 `down -v`。本仓库 `compose.local.yml` 只有开发依赖，不是线上后端部署文件。
- nohup/手动 Java：记录原参数和环境，按原方式停止旧进程后启动新 JAR；建议另行整理为 systemd，但不要并行启动两个占用 8080 的进程。

若要新建一个标准 systemd 服务，建议结构是专用运行用户、`/opt/familyrecipes/backend.jar`、`/etc/familyrecipes/backend.env`、独立媒体目录。确认没有原服务后再创建；不要凭示例覆盖现有 unit。

## 4. 反向代理与验收

沿用已有域名和 TLS 证书。Nginx 的 API 与媒体请求都转到应用，媒体目录不能公开静态映射。已有 HTTPS server 内应包含等效配置（与现有配置合并，勿重复定义 location）：

```nginx
client_max_body_size 10m;
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
location /ws/chat {
    proxy_pass http://127.0.0.1:8080;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 300s;
}
```

WebSocket 必须转发 Upgrade/Connection，见 [Nginx 官方文档](https://nginx.org/en/docs/http/websocket.html)。改配置后先 `sudo nginx -t`，成功再 reload。数据库与 8080 不必直接暴露公网。

```sh
curl -fsS http://127.0.0.1:8080/api/recipes/search
curl -fsS https://api.familyrecipes.live/api/recipes/search
```

检查实际 JSON 成功码与结果，再验收验证码邮件、登录、厨房、菜谱图片上传/鉴权和聊天重连。失败时根据日志定位，必要时回退旧 JAR 和与其匹配的数据库配置；新旧数据库分开有助于回退。

## 5. 手机连接线上

当前 debug APK 默认连 `http://10.0.2.2:8080`，更新服务器不会让它自动切到线上。release 的 API 地址为 `https://api.familyrecipes.live`（见 `android/app/build.gradle`）；需要使用签名的 release 包或专门配置线上验收构建。

旧 `upload_apk.sh` 上传的是 debug 包，不能直接当作线上发布步骤使用。Namecheap DNS 若仍指向同一台机器，通常不需要改；仅后端版本更新不会改变域名解析。

## 2026-11-07 群聊版本附加步骤

这次新增菜谱聊天只读授权表。已有数据库在启动新版后端前，需要执行 `backend/scripts/sql/2026-11-07-chat-sharing.sql`；新建库直接使用更新后的 `schema.sql`。本机已应用，线上没有执行。详情见 [群聊迭代记录](../iterations/2026-11-07-chat/README.md)。

## 从当前工作区发布并重置测试数据

完整顺序见 [现有版本部署与重置数据库](reset-and-release.md)，包含构建上传、指定数据库重建、JWT 与媒体隔离、Mailpit 隧道及 Android 线上验收包。本轮仅保存文档，没有执行线上部署或清理。

## 2026-11-07 服务器排查结果（用户提供，第二次确认）

- 服务：`familyrecipes.service`，active/running；MySQL、Nginx 也是宿主机 systemd 服务。
- JAR：`/opt/familyrecipes/backend-1.0.0.jar`。发布时使用这个实际文件名，不使用指南中的 `backend.jar` 示例。
- 当前进程：`/usr/bin/java -jar -Dspring.profiles.active=prod backend-1.0.0.jar`。新版建议标准顺序 `/usr/bin/java -Dspring.profiles.active=prod -jar /opt/familyrecipes/backend-1.0.0.jar`；替换前先核对实际 unit，不据进程行推测完整配置。
- Java：OpenJDK 17.0.20.1，满足项目 Java 17 要求。
- MySQL 监听 `127.0.0.1:3306`；后端监听 `*:8080`。Nginx 同机，后续可配置 `SERVER_ADDRESS=127.0.0.1`，公网是否能直连 8080 仍取决于防火墙。
- Nginx `server_name familyrecipes.live www.familyrecipes.live api.familyrecipes.live`，`location /` 代理到 `http://localhost:8080`。域名当前共用后端，无需因官网域名改变 Android release API 地址。
- 旧 `location /files/` 静态映射 `/var/www/uploads/`。新版使用 `/api/upload/files/` 的应用鉴权，不应沿用公开媒体静态映射；更新时删除该旧 location 或显式返回 404，先核对完整 Nginx 文件再编辑。旧磁盘文件不会随清库自动删除。
- 还未确认：服务运行用户／组、WorkingDirectory、EnvironmentFile、外部 Spring prod 配置、Nginx 实际文件名与 WebSocket headers。

下一步只读命令（仅输出路径和运行身份，不输出环境密钥）：

```sh
systemctl show familyrecipes -p User -p Group -p WorkingDirectory -p FragmentPath -p DropInPaths -p EnvironmentFiles
find /opt/familyrecipes -maxdepth 2 -type f \( -name 'application*.yml' -o -name 'application*.yaml' -o -name 'application*.properties' -o -name '*.env' -o -name '.env' \) -print
nginx -T 2>/dev/null | grep -E '^# configuration file|server_name|proxy_http_version|proxy_set_header|proxy_read_timeout'
```

不要贴出 `.env`、application 配置正文或 `systemctl show -p Environment`，其中可能包含密码。新版本代码已不包含 `application-prod.yml`；若旧机器仍有外部配置，需核对其优先级，防止覆盖环境中的数据库、媒体路径与 JWT 配置。
