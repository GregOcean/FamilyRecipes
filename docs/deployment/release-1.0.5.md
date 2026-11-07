# 1.0.5 发布（2026-11-07）

状态：已构建并验证，本机无法通过 SSH 非交互认证，尚未发布服务器。

- Android versionCode 6，versionName 1.0.5；正式签名沿用现有密钥，线上 API 地址不变。
- 包含浅色/深色主题、多外链排序及备注、来源识别、输入栏下方的连续输入表情面板（五行，每行四个）。
- 尚未改变库存推荐算法。
- 官网版本日期及下载链接更新为版本化 APK，避免旧下载缓存。
- Release 构建和单元测试通过，后端打包通过；本轮前序隔离数据库测试 46 项通过。
- APK 签名验证通过，证书 SHA-256：3466d705e362f5ee5aa92bfc940cc49aebfb6b3c35e49ff331a32acf12c34846。
- 未进行新版 vivo 真机验收。

在 Mac 终端执行：
```sh
bash /Users/wangyang/Documents/personal_projects/FamilyRecipes/scripts/publish-1.0.5.sh
```

可能多次要求服务器 root SSH 密码，输入不会显示；不要输入数据库随机密码。
发布文件位于被 Git 忽略的 familyrecipes-release/1.0.5/，含 SHA256SUMS，不含签名私钥或配置密钥。

服务器脚本先核对 Nginx 的 /apk/ 映射、数据库、文件校验和，备份到 /opt/familyrecipes/backups/时间-before-1.0.5。
仅增加外链 notes/sort_order 以及缺失的聊天授权表，不执行 schema.sql/data.sql，不清空数据，不改邮件配置。
然后更新 JAR、重启、验证接口，再更新 APK 并通过本机 HTTPS 下载校验和首页检查。
后端更新后的步骤失败时回退旧 JAR/旧 latest APK；增量字段保留兼容，版本化新 APK 可能已存在。
脚本成功后手机访问 https://familyrecipes.live/ 下载覆盖安装。服务器需要支持现有 root 的 mysql socket 认证。

## 发布前追加修改
- Android 新建菜谱默认公开；后端创建接口未指定权限时同样默认公开。已有菜谱、已有草稿及显式私有/群组选择不改变。
- 官网重做为响应式湖蓝页面，仅保留真实功能介绍、APK 下载和安装说明；删除二维码占位、未上线商店入口及过时承诺。
- 页面从 /apk/release.json 获取真实版本、文件大小和发布成功时间（界面明确显示北京时间）。脚本通过 APK 下载校验与服务健康检查后生成元数据；失败回退时恢复旧元数据。
- 无元数据时不编造日期，显示暂不可用。预览使用测试元数据，不包含在发布包内。
- 新增默认公开与显式私有回归测试，隔离 MySQL 共 47 项通过；Android 正式构建及测试通过，签名复核通过。

## 已确认的实际下载配置
用户确认下载目录为 /var/www/familyrecipes-apk，旧 Nginx 仅有精确匹配 /apk/FamilyRecipes-latest.apk。
脚本现按此路径部署，并备份、修改实际启用的 familyrecipes 站点文件，将该单文件 location 替换为 ^~ /apk/ 前缀 alias。
保留站点其他路由及 TLS，禁用目录列表，下载信息不缓存。修改后 nginx -t 成功才 reload，后续失败同时回退 Nginx 配置。
配置变换已测试替换、重复运行及未知 alias 拒绝。尚未连接服务器执行。
