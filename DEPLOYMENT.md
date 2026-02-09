# 部署指南

## 云服务器部署（以阿里云为例）

### 1. 准备工作

#### 购买服务器
- **配置推荐**: 2核4G，带宽3M，系统盘40G
- **操作系统**: Ubuntu 20.04 LTS 或 CentOS 8
- **安全组规则**: 开放 22(SSH), 8080(应用), 3306(MySQL) 端口

#### 购买域名（可选）
- 在阿里云或其他平台购买域名
- 进行域名备案（中国大陆服务器需要）
- 添加 A 记录指向服务器 IP

### 2. 服务器环境配置

#### 连接服务器
```bash
ssh root@your_server_ip
```

#### 安装 JDK 17
```bash
# Ubuntu
sudo apt update
sudo apt install openjdk-17-jdk -y

# CentOS
sudo yum install java-17-openjdk-devel -y

# 验证安装
java -version
```

#### 安装 MySQL 8
```bash
# Ubuntu
sudo apt install mysql-server -y

# CentOS
sudo yum install mysql-server -y

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### 配置 MySQL
```bash
# 登录 MySQL
sudo mysql -u root -p

# 创建数据库
CREATE DATABASE family_recipes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户（建议不使用 root）
CREATE USER 'familyrecipes'@'localhost' IDENTIFIED BY 'your_strong_password';
GRANT ALL PRIVILEGES ON family_recipes.* TO 'familyrecipes'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 导入数据库表结构
```bash
mysql -u familyrecipes -p family_recipes < schema.sql
```

### 3. 部署后端应用

#### 方式一：直接运行 JAR

```bash
# 上传 JAR 文件到服务器
scp backend/target/backend-1.0.0.jar root@your_server_ip:/opt/familyrecipes/

# 创建配置文件
mkdir -p /opt/familyrecipes
cd /opt/familyrecipes

# 创建 application.yml（覆盖默认配置）
cat > application.yml << EOF
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/family_recipes?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: familyrecipes
    password: your_strong_password

jwt:
  secret: $(openssl rand -base64 32)

file:
  upload-dir: /opt/familyrecipes/uploads/
  base-url: http://your_domain.com/files/
EOF

# 创建上传目录
mkdir -p /opt/familyrecipes/uploads

# 运行应用
nohup java -jar backend-1.0.0.jar --spring.config.location=application.yml > app.log 2>&1 &

# 查看日志
tail -f app.log
```

#### 方式二：使用 systemd 服务（推荐）

```bash
# 创建服务文件
sudo cat > /etc/systemd/system/familyrecipes.service << EOF
[Unit]
Description=FamilyRecipes Backend Service
After=syslog.target network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/familyrecipes
ExecStart=/usr/bin/java -jar /opt/familyrecipes/backend-1.0.0.jar --spring.config.location=/opt/familyrecipes/application.yml
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
sudo systemctl daemon-reload
sudo systemctl start familyrecipes
sudo systemctl enable familyrecipes

# 查看状态
sudo systemctl status familyrecipes

# 查看日志
sudo journalctl -u familyrecipes -f
```

### 4. 配置 Nginx 反向代理（推荐）

```bash
# 安装 Nginx
sudo apt install nginx -y  # Ubuntu
sudo yum install nginx -y  # CentOS

# 创建配置文件
sudo cat > /etc/nginx/sites-available/familyrecipes << EOF
server {
    listen 80;
    server_name your_domain.com;  # 修改为你的域名或 IP

    # API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # 文件访问
    location /files/ {
        alias /opt/familyrecipes/uploads/;
        autoindex off;
    }

    # 文件上传大小限制
    client_max_body_size 10M;
}
EOF

# 启用配置
sudo ln -s /etc/nginx/sites-available/familyrecipes /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
sudo systemctl enable nginx
```

### 5. 配置 HTTPS（推荐）

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取 SSL 证书
sudo certbot --nginx -d your_domain.com

# 证书会自动续期
sudo systemctl status certbot.timer
```

### 6. 配置防火墙

```bash
# Ubuntu (UFW)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable

# CentOS (firewalld)
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 7. 性能优化

#### JVM 参数调优
修改 `/etc/systemd/system/familyrecipes.service`:
```ini
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -XX:+UseG1GC -jar /opt/familyrecipes/backend-1.0.0.jar
```

#### MySQL 优化
编辑 `/etc/mysql/mysql.conf.d/mysqld.cnf`:
```ini
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 512M
```

### 8. 监控和日志

#### 应用日志
```bash
# 查看实时日志
sudo journalctl -u familyrecipes -f

# 查看最近 100 行
sudo journalctl -u familyrecipes -n 100
```

#### 磁盘监控
```bash
# 查看磁盘使用
df -h

# 查看目录大小
du -sh /opt/familyrecipes/uploads/
```

### 9. 定期备份

#### 数据库备份脚本
```bash
#!/bin/bash
# /opt/backup/mysql_backup.sh

BACKUP_DIR="/opt/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
FILENAME="family_recipes_$DATE.sql"

mkdir -p $BACKUP_DIR

mysqldump -u familyrecipes -p'your_password' family_recipes > $BACKUP_DIR/$FILENAME

# 保留最近 7 天的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete

echo "Backup completed: $FILENAME"
```

#### 设置定时任务
```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点备份
0 2 * * * /opt/backup/mysql_backup.sh
```

### 10. 配置 Android 客户端

修改 `PreferenceManager.kt`:
```kotlin
var baseUrl: String
    get() = prefs.getString(KEY_BASE_URL, "https://your_domain.com") ?: "https://your_domain.com"
```

重新编译 APK:
```bash
cd android
./gradlew assembleRelease

# 签名 APK（生产环境必须）
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore your-keystore.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  your-alias
```

## Docker 部署（推荐）

### 1. 创建 Dockerfile

```dockerfile
# backend/Dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 创建 docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: familyrecipes_mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: family_recipes
      MYSQL_USER: familyrecipes
      MYSQL_PASSWORD: your_password
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    ports:
      - "3306:3306"
    networks:
      - familyrecipes_network

  backend:
    build: ./backend
    container_name: familyrecipes_backend
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/family_recipes?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: familyrecipes
      SPRING_DATASOURCE_PASSWORD: your_password
    volumes:
      - uploads:/opt/familyrecipes/uploads
    ports:
      - "8080:8080"
    networks:
      - familyrecipes_network
    restart: always

volumes:
  mysql_data:
  uploads:

networks:
  familyrecipes_network:
    driver: bridge
```

### 3. 启动服务

```bash
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f backend

# 停止服务
docker-compose down
```

## 常见问题排查

### 应用无法启动
```bash
# 检查端口占用
sudo netstat -tulpn | grep 8080

# 检查服务状态
sudo systemctl status familyrecipes

# 查看详细日志
sudo journalctl -xe -u familyrecipes
```

### 数据库连接失败
```bash
# 测试数据库连接
mysql -u familyrecipes -p -h localhost family_recipes

# 检查 MySQL 状态
sudo systemctl status mysql
```

### 图片上传失败
```bash
# 检查目录权限
ls -la /opt/familyrecipes/uploads/
chmod 755 /opt/familyrecipes/uploads/
```

### 内存不足
```bash
# 查看内存使用
free -h

# 添加 swap 分区
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

---

**部署完成后别忘记测试所有功能！** 🚀

