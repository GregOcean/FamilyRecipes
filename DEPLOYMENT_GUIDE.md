# 家肴 App 生产环境部署指南

## 📋 部署清单

### 1. 云服务选择

**推荐：DigitalOcean（性价比最高）**

```bash
# 服务器配置
- Droplet: 2GB RAM, 1 vCPU, 50GB SSD ($12/月)
- 地区：Singapore 或 San Francisco（距离Google Play用户近）
- 操作系统：Ubuntu 22.04 LTS

# 数据库
- 选项A：同一服务器上自建MySQL（省钱）
- 选项B：Managed Database ($15/月，更稳定）

# 对象存储
- Cloudflare R2（免费10GB）或 DigitalOcean Spaces ($5/月)
```

### 2. 域名设置

```bash
# 购买域名（任选一）
- Namecheap.com
- Google Domains
- Cloudflare

# DNS解析
api.familyrecipes.app    → A记录 → 服务器IP
www.familyrecipes.app    → CNAME → api.familyrecipes.app
```

### 3. 后端配置修改

#### application-prod.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/family_recipes?useSSL=true&serverTimezone=UTC
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  # 文件上传配置
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

# 对象存储配置（使用环境变量）
storage:
  type: s3  # 或 r2, spaces
  endpoint: ${STORAGE_ENDPOINT}
  access-key: ${STORAGE_ACCESS_KEY}
  secret-key: ${STORAGE_SECRET_KEY}
  bucket: ${STORAGE_BUCKET}
  public-url: ${STORAGE_PUBLIC_URL}

# JWT配置
jwt:
  secret: ${JWT_SECRET}
  expiration: 604800000  # 7天
```

#### 环境变量 (.env)
```bash
DB_USERNAME=familyrecipes
DB_PASSWORD=your_secure_password_here

STORAGE_ENDPOINT=https://your-bucket.r2.cloudflarestorage.com
STORAGE_ACCESS_KEY=your_access_key
STORAGE_SECRET_KEY=your_secret_key
STORAGE_BUCKET=familyrecipes
STORAGE_PUBLIC_URL=https://cdn.familyrecipes.app

JWT_SECRET=your_very_long_random_secret_key_here
```

### 4. Android 配置修改

#### app/src/main/java/.../data/remote/ApiClient.kt
```kotlin
object ApiClient {
    private const val BASE_URL = "https://api.familyrecipes.app/"  // 生产环境
    // private const val BASE_URL = "http://10.0.2.2:8080/"  // 开发环境
    
    // ... rest of code
}
```

#### build.gradle (app)
```gradle
android {
    buildTypes {
        release {
            buildConfigField "String", "API_BASE_URL", '"https://api.familyrecipes.app/"'
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            buildConfigField "String", "API_BASE_URL", '"http://10.0.2.2:8080/"'
        }
    }
}
```

### 5. 服务器部署步骤

```bash
# 1. 连接服务器
ssh root@your_server_ip

# 2. 安装依赖
apt update && apt upgrade -y
apt install -y openjdk-17-jdk nginx mysql-server

# 3. 配置MySQL
mysql_secure_installation
mysql -u root -p
CREATE DATABASE family_recipes;
CREATE USER 'familyrecipes'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON family_recipes.* TO 'familyrecipes'@'localhost';
FLUSH PRIVILEGES;

# 4. 上传后端jar包
scp backend/target/familyrecipes-0.0.1-SNAPSHOT.jar root@your_server_ip:/opt/familyrecipes/

# 5. 创建systemd服务
cat > /etc/systemd/system/familyrecipes.service << EOF
[Unit]
Description=Family Recipes Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/familyrecipes
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod familyrecipes-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

Environment="DB_USERNAME=familyrecipes"
Environment="DB_PASSWORD=your_password"
Environment="JWT_SECRET=your_jwt_secret"
# ... 其他环境变量

[Install]
WantedBy=multi-user.target
EOF

# 6. 启动服务
systemctl daemon-reload
systemctl enable familyrecipes
systemctl start familyrecipes

# 7. 配置Nginx反向代理
cat > /etc/nginx/sites-available/familyrecipes << EOF
server {
    listen 80;
    server_name api.familyrecipes.app;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF

ln -s /etc/nginx/sites-available/familyrecipes /etc/nginx/sites-enabled/
nginx -t && systemctl restart nginx

# 8. 安装SSL证书（Let's Encrypt）
apt install -y certbot python3-certbot-nginx
certbot --nginx -d api.familyrecipes.app
```

### 6. 对象存储配置（Cloudflare R2）

```bash
# 1. 登录 Cloudflare Dashboard
# 2. 创建 R2 Bucket: familyrecipes
# 3. 生成 API Token
# 4. 配置 CORS（允许Android应用访问）

# CORS配置
[
  {
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3000
  }
]

# 5. 配置自定义域名（可选）
# cdn.familyrecipes.app → CNAME → your-bucket.r2.cloudflarestorage.com
```

### 7. Google Play Store 上架准备

#### 创建 keystore（签名密钥）
```bash
cd android/app
keytool -genkey -v -keystore familyrecipes.jks -keyalg RSA -keysize 2048 -validity 10000 -alias familyrecipes

# 保存好密钥信息：
# Keystore password: xxxxxx
# Key alias: familyrecipes
# Key password: xxxxxx
```

#### android/keystore.properties
```properties
storePassword=your_keystore_password
keyPassword=your_key_password
keyAlias=familyrecipes
storeFile=familyrecipes.jks
```

#### android/app/build.gradle
```gradle
android {
    signingConfigs {
        release {
            def keystorePropertiesFile = rootProject.file("keystore.properties")
            def keystoreProperties = new Properties()
            keystoreProperties.load(new FileInputStream(keystorePropertiesFile))

            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### 生成发布APK
```bash
cd android
./gradlew assembleRelease

# APK位置：app/build/outputs/apk/release/app-release.apk
```

### 8. 隐私政策页面

创建一个简单的HTML页面，托管在域名上：

```html
<!-- privacy.html -->
<!DOCTYPE html>
<html>
<head>
    <title>家肴 - 隐私政策</title>
    <meta charset="UTF-8">
</head>
<body>
    <h1>家肴 App 隐私政策</h1>
    
    <h2>信息收集</h2>
    <p>我们收集您提供的账号信息（邮箱、用户名）、上传的菜谱和图片。</p>
    
    <h2>信息使用</h2>
    <p>您的信息仅用于提供家肴服务，不会分享给第三方。</p>
    
    <h2>数据安全</h2>
    <p>我们使用加密技术保护您的数据安全。</p>
    
    <h2>联系我们</h2>
    <p>如有疑问，请联系：support@familyrecipes.app</p>
</body>
</html>
```

上传到：`https://www.familyrecipes.app/privacy.html`

## 💰 总成本估算

### 最小化方案（$20/月）
```
DigitalOcean Droplet: $12/月
Cloudflare R2: 免费（10GB内）
域名: $1/月 ($12/年)
Google Play 账号: $25（一次性）

总计：~$13/月 + $25一次性
```

### 推荐方案（$30/月）
```
DigitalOcean Droplet: $12/月
DO Managed Database: $15/月
Cloudflare R2: 免费
域名: $1/月
CDN: Cloudflare免费

总计：~$28/月 + $25一次性
```

## 🚀 部署后检查清单

- [ ] 后端API能通过HTTPS访问
- [ ] 数据库连接正常，数据迁移完成
- [ ] 图片上传和访问正常
- [ ] Android App连接生产环境正常
- [ ] SSL证书配置正确
- [ ] 服务器自动重启配置
- [ ] 数据库定期备份
- [ ] 监控和日志配置（可选）
- [ ] 防火墙规则配置
- [ ] 域名解析生效

## 📊 监控（可选但推荐）

### 免费监控方案
- UptimeRobot：API可用性监控
- Cloudflare Analytics：流量监控
- DigitalOcean Monitoring：服务器资源监控

### 日志管理
```bash
# 查看后端日志
journalctl -u familyrecipes -f

# 查看Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## 🔄 更新部署流程

```bash
# 1. 构建新版本
./mvnw clean package -DskipTests

# 2. 上传到服务器
scp target/familyrecipes-0.0.1-SNAPSHOT.jar root@your_server:/opt/familyrecipes/

# 3. 重启服务
ssh root@your_server "systemctl restart familyrecipes"

# 4. 检查状态
ssh root@your_server "systemctl status familyrecipes"
```

## 📱 Android发布流程

```bash
# 1. 更新版本号
# android/app/build.gradle
versionCode 2
versionName "1.1"

# 2. 构建发布版本
./gradlew assembleRelease

# 3. 上传到 Google Play Console
# https://play.google.com/console

# 4. 填写更新说明
# 5. 提交审核
```

## 🆘 常见问题

### API连接失败
- 检查服务器防火墙（开放80, 443端口）
- 检查域名DNS解析
- 检查SSL证书

### 图片上传失败
- 检查对象存储配置
- 检查CORS设置
- 检查网络连接

### 数据库连接失败
- 检查MySQL服务状态
- 检查数据库用户权限
- 检查防火墙规则

## 📚 相关文档

- [DigitalOcean文档](https://docs.digitalocean.com/)
- [Cloudflare R2文档](https://developers.cloudflare.com/r2/)
- [Google Play上架指南](https://play.google.com/console/about/guides/releasewithconfidence/)
- [Spring Boot部署指南](https://spring.io/guides/gs/spring-boot-docker/)

