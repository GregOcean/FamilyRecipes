# 邮箱验证码注册与本地邮件验收

日期：2026-11-01。用户确认：暂不接入真实发信服务，先用本机邮件测试箱验收。

## 注册流程

1. App 输入邮箱，点击“获取验证码”。
2. 后端通过标准 SMTP 发送 6 位数字验证码。
3. 输入验证码、用户名和密码，提交注册；成功后自动登录。
4. 登录仍为邮箱和密码，不需要每次登录重新收验证码。已有本地账号继续可登录。

注册现在必须通过邮箱验证码。新增 `POST /api/auth/registration-code` 接口，接受 `email`；注册请求新增 `code`。邮箱按去除首尾空白、小写归一化处理。原先的短密码问题也已修复：客户端先校验至少 8 位、最多 72 个 UTF-8 字节，不再删除密码前后空格，HTTP 错误正文会显示具体原因。

验证码有效期 10 分钟，每个邮箱 60 秒内不能重复发送、每小时最多 5 封；每个直连来源地址每小时最多 20 次。连续输错 5 次后锁定该次验证码，重新获取后可再尝试。验证码按用途与邮箱绑定，数据库仅保存 HMAC 摘要；验证码和 SMTP 密码不写日志、不在发送接口响应中返回。

发送配额在 SMTP 调用前持久化，因此发信失败仍计入限额。发送成功后才激活验证码；注册与消费验证码在同一数据库事务中提交，防止重放。错误尝试数在返回错误前提交，避免因事务回滚而不计次数。超过一天的挑战与限流记录定期清理。不提供万能验证码，也不跳过本地验证码检查。

## 本地验收

- 邮件测试箱：<http://localhost:8025>
- SMTP：`127.0.0.1:1025`
- 通过 `backend/compose.local.yml` 启动 Mailpit，两个端口均只绑定本机。
- 邮件保存在专用 Docker 卷，最多保留 100 封。没有配置向外部邮箱转发或中继。

**在本机填写 QQ、Gmail 或测试邮箱，邮件都只进入 Mailpit，不会发送到真实邮箱。** 在浏览器打开邮件测试箱，找到收件人匹配的最新验证码邮件，然后返回模拟器填入即可。

当前本地数据库只新增了验证码和限流两张表，没有删除、重建已有账号或业务数据。全新数据库的 `schema.sql` 已包括这两张表。

## 后续接入真实邮箱

在未提交 Git 的 `backend/.env` 中配置 `SMTP_HOST`、`SMTP_PORT`、`SMTP_USERNAME`、`SMTP_PASSWORD`、`SMTP_FROM`，并按服务商要求设置 `SMTP_AUTH`、`SMTP_STARTTLS` 或 `SMTP_SSL`。常见 587 端口使用认证 + STARTTLS；465 端口使用认证 + SSL，具体遵循服务商文档。真实环境需使用服务商允许的发信地址或验证域名。

`EMAIL_CODE_SECRET` 可独立配置；未配置时使用已有 JWT 密钥做用途隔离的 HMAC。重启后端生效。该实现仅涉及注册验证，没有加入忘记密码/密码重置功能。

生产反向代理前暂不信任客户端提交的 `X-Forwarded-For`，以连接来源限流；上线时需要结合可信代理配置调整 IP 限流，避免所有请求共用代理地址。当前限流方案以单机为目标。

实现参考：[Spring Boot 邮件支持及超时配置](https://docs.spring.io/spring-boot/reference/io/email.html)、[Mailpit Docker 文档](https://mailpit.axllent.org/docs/install/docker/)。

## 验证

- 后端 32 项测试通过，包含 17 项真实 MySQL 集成测试。新增验证：缺少验证码、发送接口无需登录、摘要存储、验证码单次消费、错误次数、过期、发送间隔、邮箱配额、SMTP 失败不激活验证码。
- Android 调试包编译通过，4 项单元测试通过（提醒策略与密码规则）。
- 本地端到端验证通过：HTTP 申请验证码 → SMTP 投递 Mailpit → 从邮件读取验证码 → 验证注册 → 密码登录。真实服务商投递尚未测试。

## 本机调试签名

本机使用 Git 忽略的 `android/debug.keystore`，让容器和 Android Studio 使用同一调试签名，避免覆盖安装报签名不一致。文件不存在时回退到 Android 默认调试签名。新开发机如需跨构建环境共享签名，可在仓库根目录运行：

```sh
keytool -genkeypair -keystore android/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug,O=Android,C=US' -keyalg RSA -keysize 2048 -validity 10000
```

此密钥仅用于本地 debug 构建，不能用作发布签名。已有安装使用不同签名时需先备份应用数据再重装；之后保留此文件即可覆盖更新。
