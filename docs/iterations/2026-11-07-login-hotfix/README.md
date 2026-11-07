# 1.0.1 未登录闪屏修复

用户在 vivo X200 Pro mini 正式包首次使用发现头像空白、登录页面反复闪烁。

根因：ProfileFragment.checkLoginStatus 在未登录分支调用 Activity.recreate，onViewCreated/onResume 再调用该检查形成无限重建。改为 showLoginPage。onHiddenChanged 也改为检查认证状态，不直接展示已登录个人页。未登录用户名为空时 UserAvatar 新增人形图标占位。

版本提升至 versionCode 2 / versionName 1.0.1，沿用原正式签名。后端无需更新。

验证：assembleRelease、testReleaseUnitTest 成功；apksigner 验证通过。模拟器全新安装正式包（保留原 debug 数据），确认默认人形头像、登录页面稳定、输入可保留、无输入法遮挡时可切换到完整注册表单。输入法会转换 adb 输入文本，不将测试输入当成真实邮箱。未发送验证码或创建测试账号。vivo 真机需用户覆盖安装复验。

APK SHA-256：8228193e6a7c945052fd0e9ef5183dd66f9a635ce74859d31eac9dfb7ab72c29。

未上传服务器。上传当前 release APK 后替换 /var/www/familyrecipes-apk/FamilyRecipes-latest.apk，可用带 ?v=1.0.1 的链接区分旧下载缓存。
