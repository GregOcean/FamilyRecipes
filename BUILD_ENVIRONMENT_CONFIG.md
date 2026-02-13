# 🔧 Android 编译环境配置说明

本项目使用 Gradle BuildConfig 自动区分开发环境和生产环境，无需手动修改代码。

## 📋 环境配置

### 1️⃣ Debug 构建（开发环境）

**用途：** 本地开发和调试

**配置：**
- API 地址：`http://10.0.2.2:8080`（Android 模拟器访问本机）
- 包名后缀：`.debug`
- 版本名后缀：`-debug`

**如何使用：**

```bash
# Android Studio 运行
点击 Run 按钮 (默认就是 debug 模式)

# 命令行编译
cd android
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 输出位置
android/app/build/outputs/apk/debug/app-debug.apk
```

**特点：**
- 应用包名：`com.familyrecipes.android.debug`
- 可与 Release 版本同时安装
- 连接本地后端服务器

---

### 2️⃣ Release 构建（生产环境）

**用途：** 正式发布和用户使用

**配置：**
- API 地址：`https://api.familyrecipes.live`
- 包名：`com.familyrecipes.android`
- 版本名：`1.0`

**如何使用：**

```bash
# Android Studio 编译
Build → Build Bundle(s) / APK(s) → Build APK(s)
或
Build → Generate Signed Bundle / APK → APK → 选择 release

# 命令行编译（未签名）
cd android
./gradlew assembleRelease

# 输出位置
android/app/build/outputs/apk/release/app-release-unsigned.apk
或（如果已签名）
android/app/build/outputs/apk/release/app-release.apk
```

**特点：**
- 应用包名：`com.familyrecipes.android`
- 连接线上生产服务器
- 可能需要签名才能安装

---

## 🎯 实际使用场景

### 场景1：本地开发调试

```bash
# 1. 启动本地后端
cd backend
./mvnw spring-boot:run

# 2. 在 Android Studio 中直接 Run
# 自动使用 debug 配置，连接 http://10.0.2.2:8080
```

### 场景2：生成测试 APK

```bash
# 生成 debug APK，发给测试人员
cd android
./gradlew assembleDebug

# 分享 APK
# android/app/build/outputs/apk/debug/app-debug.apk
```

### 场景3：生成生产 APK

```bash
# 生成 release APK，连接线上服务器
cd android
./gradlew assembleRelease

# 上传到服务器
scp android/app/build/outputs/apk/release/app-release.apk \
    root@137.184.73.116:/opt/familyrecipes/apk/FamilyRecipes-v1.0.0.apk
```

---

## 🔍 如何验证当前环境

可以在应用的 "我的" 页面或启动日志中查看：

```kotlin
// 在任何 Activity 中打印当前环境
Log.d("Environment", "当前环境: ${PreferenceManager.environment}")
Log.d("Environment", "API 地址: ${PreferenceManager.baseUrl}")
```

---

## 🛠️ 高级配置

### 添加更多环境变量

如需添加更多配置（如 API Key、功能开关等），在 `build.gradle` 中添加：

```groovy
buildTypes {
    debug {
        buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8080\""
        buildConfigField "String", "ENVIRONMENT", "\"development\""
        buildConfigField "boolean", "ENABLE_LOGS", "true"
        buildConfigField "String", "ANALYTICS_KEY", "\"debug-key\""
    }
    
    release {
        buildConfigField "String", "API_BASE_URL", "\"https://api.familyrecipes.live\""
        buildConfigField "String", "ENVIRONMENT", "\"production\""
        buildConfigField "boolean", "ENABLE_LOGS", "false"
        buildConfigField "String", "ANALYTICS_KEY", "\"prod-key\""
    }
}
```

使用方式：

```kotlin
if (BuildConfig.ENABLE_LOGS) {
    Log.d(TAG, "这是调试日志")
}

val analyticsKey = BuildConfig.ANALYTICS_KEY
```

### 添加 Staging 环境

```groovy
buildTypes {
    debug { /* ... */ }
    
    staging {
        initWith debug
        buildConfigField "String", "API_BASE_URL", "\"https://staging.familyrecipes.live\""
        buildConfigField "String", "ENVIRONMENT", "\"staging\""
        applicationIdSuffix ".staging"
    }
    
    release { /* ... */ }
}
```

---

## 📱 不同环境同时安装

由于 debug 和 release 版本的包名不同：
- Debug: `com.familyrecipes.android.debug`
- Release: `com.familyrecipes.android`

你可以在同一台设备上同时安装两个版本，方便对比测试。

---

## 🎓 总结

✅ **优点：**
- 自动切换环境，无需修改代码
- 支持多环境同时安装
- 编译时确定配置，更安全高效
- 符合 Android 开发最佳实践

❌ **无需再做：**
- 每次编译前修改 API 地址
- 担心发布时忘记改回生产地址
- 手动管理环境配置

---

**更新时间：** 2026-02-13  
**项目：** 家肴 - 家庭菜谱管理助手

