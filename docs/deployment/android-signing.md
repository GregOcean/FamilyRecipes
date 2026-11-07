# Android 正式签名

2026-11-07，经用户同意生成新的专用 release 密钥；不要求兼容此前不同签名的安装包。正式 APK 构建和 apksigner 验证通过，包名 com.familyrecipes.android。未上传服务器。

本机文件（均权限 600，Git 忽略，勿提交）：

- android/familyrecipes-release.keystore：PKCS12，RSA 3072，别名 familyrecipes-release。
- android/keystore.properties：Gradle 签名路径、别名及密码。

公开证书 SHA-256：3466d705e362f5ee5aa92bfc940cc49aebfb6b3c35e49ff331a32acf12c34846。

后续沿用这套密钥；将两个文件一起存入密码管理器附件或加密离线备份。不要放入公开下载目录、Git 或聊天。换电脑时恢复这两个文件即可；Git clone 不包含它们。若遗失，不能直接用新签名覆盖已有安装。不同签名的旧包需要先卸载（会清除本机数据）。

构建：在仓库根目录执行 `ANDROID_HOME="$HOME/Library/Android/sdk" JAVA_HOME=$(/usr/libexec/java_home -v 17) android/gradlew -p android :app:assembleRelease`。产物 android/app/build/outputs/apk/release/app-release.apk；release API 为 https://api.familyrecipes.live。后续发布递增 android/app/build.gradle 中的 versionCode，并维护 versionName。

本次产物 SHA-256：179fdda8bccfe08ded04ed71a2bb14a9183fdc02324d99facb58d623de33f8df。重新构建后校验和可能变化，以实际文件为准。
