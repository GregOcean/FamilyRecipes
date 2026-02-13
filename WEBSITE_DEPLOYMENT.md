# 官网首页部署说明

## 📁 文件结构

```
backend/src/main/resources/static/
├── index.html          # 主页（下载页面、功能介绍）
├── privacy.html        # 隐私政策
├── changelog.md        # 更新日志
└── apk/               # APK文件存放目录
    └── familyrecipes-latest.apk  # 最新版APK（需要你上传）
```

## 🌐 访问地址

部署后可通过以下地址访问：

```
主页: https://familyrecipes.live/
      https://www.familyrecipes.live/
      https://api.familyrecipes.live/

隐私政策: https://familyrecipes.live/privacy.html

APK下载: https://familyrecipes.live/apk/familyrecipes-latest.apk
```

## 📦 如何上传APK

### 方法1：在部署时上传

```bash
# 在本地编译APK
cd android
./gradlew assembleRelease

# APK位置：
android/app/build/outputs/apk/release/app-release.apk

# 复制到static目录
cp android/app/build/outputs/apk/release/app-release.apk \
   backend/src/main/resources/static/apk/familyrecipes-latest.apk

# 重新打包后端
cd backend
./mvnw clean package -DskipTests

# 上传到服务器（会包含APK）
scp target/familyrecipes-0.0.1-SNAPSHOT.jar root@your_server:/opt/familyrecipes/
```

### 方法2：单独上传到服务器

```bash
# 编译APK
cd android
./gradlew assembleRelease

# 直接上传到服务器
scp android/app/build/outputs/apk/release/app-release.apk \
    root@your_server:/opt/familyrecipes/static/apk/familyrecipes-latest.apk

# 重启服务
ssh root@your_server "systemctl restart familyrecipes"
```

### 方法3：在服务器上创建软链接（推荐）

```bash
# 在服务器上创建专门的APK目录
ssh root@your_server
mkdir -p /var/www/apk

# 配置Nginx直接提供APK下载
nano /etc/nginx/sites-available/familyrecipes

# 添加：
location /apk/ {
    alias /var/www/apk/;
    autoindex on;
}

# 上传APK到服务器
scp android/app/build/outputs/apk/release/app-release.apk \
    root@your_server:/var/www/apk/familyrecipes-latest.apk

# 重启Nginx
ssh root@your_server "systemctl restart nginx"
```

## 🎨 自定义首页

### 修改颜色主题

在 `index.html` 中修改：

```css
/* 渐变背景 */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* 主色调 */
color: #667eea;

/* 下载按钮渐变 */
background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
```

### 添加Logo

```html
<!-- 在 header 中添加 -->
<img src="/images/logo.png" alt="家肴Logo" style="width: 100px;">
```

### 添加应用截图

```html
<!-- 在 features 后添加 -->
<div class="section">
    <h2>📱 应用截图</h2>
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;">
        <img src="/images/screenshot1.png" alt="截图1" style="width: 100%; border-radius: 10px;">
        <img src="/images/screenshot2.png" alt="截图2" style="width: 100%; border-radius: 10px;">
        <img src="/images/screenshot3.png" alt="截图3" style="width: 100%; border-radius: 10px;">
    </div>
</div>
```

## 📱 生成下载二维码

### 在线生成工具

1. **访问**: https://cli.im/ 或 https://www.the-qrcode-generator.com/
2. **输入**: https://familyrecipes.live/apk/familyrecipes-latest.apk
3. **下载二维码图片**
4. **保存到**: `backend/src/main/resources/static/images/qr-download.png`
5. **在index.html中使用**:

```html
<div class="qr-code">
    <p style="color: white; margin-bottom: 10px;">扫码下载</p>
    <img src="/images/qr-download.png" alt="下载二维码">
</div>
```

## 🔧 测试首页

### 本地测试

```bash
# 启动后端
cd backend
./mvnw spring-boot:run

# 浏览器访问
http://localhost:8080/

# 测试APK下载（需要先放APK文件）
http://localhost:8080/apk/familyrecipes-latest.apk
```

### 生产环境测试

```bash
# 部署后访问
https://familyrecipes.live/

# 测试下载
curl -I https://familyrecipes.live/apk/familyrecipes-latest.apk
# 应该返回 200 OK
```

## 📊 Google Play 上架时需要的隐私政策URL

```
隐私政策URL: https://familyrecipes.live/privacy.html
```

在Google Play Console填写应用信息时，这个URL是必填项。

## 🔄 更新流程

### 发布新版本时

1. **更新版本号**（在index.html中）:
```html
<strong>最新版本:</strong> 1.1.0 | <strong>大小:</strong> ~18MB | <strong>更新时间:</strong> 2026-03-01
```

2. **更新changelog.md**

3. **上传新APK**（覆盖旧的）

4. **重新部署**（如果修改了HTML）

## 🎯 SEO优化（可选）

### 添加meta标签

在 `index.html` 的 `<head>` 中添加：

```html
<meta name="description" content="家肴 - 专业的家庭菜谱管理应用，智能食材管理，菜谱收藏分享">
<meta name="keywords" content="菜谱,食谱,家常菜,食材管理,冰箱管理,家肴">
<meta name="author" content="FamilyRecipes">

<!-- Open Graph for社交分享 -->
<meta property="og:title" content="家肴 - 家庭菜谱管理助手">
<meta property="og:description" content="让每一道家常菜都有故事">
<meta property="og:image" content="https://familyrecipes.live/images/og-image.png">
<meta property="og:url" content="https://familyrecipes.live">
```

## 📈 添加统计（可选）

### Google Analytics

```html
<!-- 在 </head> 前添加 -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');
</script>
```

## ✅ 检查清单

部署前检查：

- [ ] index.html 已创建
- [ ] privacy.html 已创建  
- [ ] WebConfig.java 已配置
- [ ] APK文件已准备好
- [ ] 隐私政策邮箱已修改为真实邮箱
- [ ] Google Play链接已更新（上架后）
- [ ] 二维码已生成（可选）
- [ ] 应用截图已准备（可选）
- [ ] Logo已准备（可选）

部署后检查：

- [ ] 首页可以正常访问
- [ ] 隐私政策页面可以打开
- [ ] APK可以正常下载
- [ ] 在手机上测试下载和安装
- [ ] 所有链接都可以点击
- [ ] 移动端显示正常

---

**现在你已经有了一个完整的官网！** 🎉

