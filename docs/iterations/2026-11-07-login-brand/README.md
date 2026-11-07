# 1.0.2 登录入口与品牌统一

- 库存、安排、消息在未登录时直接打开登录页；厨房选择与厨房/群组管理先检查登录。
- AuthUtil 在 MainActivity 内直接切换页面；从其他 Activity 返回时，MainActivity.onNewIntent 正确消费 navigate_to=profile。此前 SINGLE_TOP 请求未处理，导致跳转看似无效。
- 应用图标及开屏统一为湖蓝电饭煲＋矢量“佳肴”，覆盖旧版密度图标、adaptive icon 和开屏资源。应用名称和登录标题同步为用户指定的“佳肴”。
- versionCode 3 / versionName 1.0.2，沿用正式签名；后端不变。

验证：assembleRelease、testReleaseUnitTest 成功，apksigner 校验同证书。模拟器未登录正式包验证库存/安排/消息直达登录，新建菜谱触发登录，登录转注册显示验证码字段。实际冷启动截图检查新图标字形与开屏。最后补充管理入口后重新构建并安装，管理入口真机操作尚未验收。未执行验证码发送或注册账号操作。

此前登录闪屏未在开发验证发现的原因是主要沿用已有登录数据；命令行和 Android Studio 可控制同一个 Android Emulator，不是测试工具品牌差异。发布前需要分别验证未登录、已登录、退出登录、后台恢复及覆盖升级，不能只跑编译和单元测试。

本机可直接 adb install -r 正式 APK，无需上传服务器。真机需开启开发者选项/USB 调试、通过设备授权，adb devices 确认 serial 后使用 adb -s SERIAL install -r，不能同时连接模拟器和手机时省略设备选择。正式包连接线上 API，测试不应自动向真实群聊发送消息。

APK SHA-256：f5227aa293dcb2206e8f41467f4a1f86eceb267879eda6752f802df5b6276680。尚未上传官网；vivo X200 Pro mini 待用户复验。
