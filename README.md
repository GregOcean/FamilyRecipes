# 家庭菜谱管理系统 (FamilyRecipes)

一个功能完整的家庭菜谱管理系统，包含 Spring Boot 后端和 Android 客户端。

## 功能特性

### 🍳 菜谱管理
- 创建、编辑、删除菜谱
- 菜谱分类（时段、类型、主食材、特殊需求等标签）
- 记录食材、烹饪步骤、图片
- 外链收藏（方便收藏下厨房等网站的菜谱）
- 记录谁会做这道菜
- 收藏喜欢的菜谱（❤）
- 记录最近常做的菜

### 🔍 搜索功能
- 按菜名、tag、食材、用户名搜索
- 不同页面智能排序（菜谱页优先菜名，冰箱页优先食材）
- 多条件组合搜索

### 🎲 推荐功能
- 随机推荐新菜
- 结合已登记菜谱和互联网资源

### 🧊 冰箱管理
- 记录食材的保质期
- 到期前自动提醒
- 一键标记食材已消耗
- 按保质期排序显示

### 👥 多用户支持
- 邮箱登录注册
- 数据云端同步
- 记录每个菜谱的创建者
- 查看自己登记和收藏的菜

### 📸 拍照功能
- 拍摄烹饪过程和成品
- 图片上传和管理

## 技术栈

### 后端 (Spring Boot)
- **框架**: Spring Boot 3.2.2
- **数据库**: MySQL 8.0
- **ORM**: MyBatis 3.0.3
- **认证**: JWT (io.jsonwebtoken)
- **构建工具**: Maven

### Android 客户端
- **语言**: Kotlin
- **最低SDK**: 24 (Android 7.0)
- **目标SDK**: 34 (Android 14)
- **UI**: Material Design 3
- **网络**: Retrofit 2.9.0 + OkHttp 4.12.0
- **图片**: Glide 4.16.0
- **相机**: CameraX 1.3.1
- **后台任务**: WorkManager 2.9.0

## 项目结构

```
FamilyRecipes/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/familyrecipes/
│   │   ├── controller/        # REST API 控制器
│   │   ├── service/           # 业务逻辑层
│   │   ├── mapper/            # MyBatis Mapper
│   │   ├── entity/            # 数据库实体类
│   │   ├── common/            # 通用类（Result, PageResult）
│   │   ├── config/            # 配置类
│   │   ├── interceptor/       # 拦截器（认证）
│   │   └── util/              # 工具类（JWT）
│   ├── src/main/resources/
│   │   ├── mapper/            # MyBatis XML 映射文件
│   │   ├── application.yml    # 应用配置
│   │   └── schema.sql         # 数据库表结构
│   └── pom.xml
│
└── android/                    # Android 客户端
    ├── app/src/main/java/com/familyrecipes/android/
    │   ├── ui/                # UI 层
    │   │   ├── auth/          # 登录注册
    │   │   ├── recipe/        # 菜谱模块
    │   │   ├── fridge/        # 冰箱模块
    │   │   ├── recommend/     # 推荐模块
    │   │   ├── profile/       # 个人中心
    │   │   ├── adapter/       # RecyclerView适配器
    │   │   └── MainActivity.kt
    │   ├── data/              # 数据层
    │   │   ├── model/         # 数据模型
    │   │   ├── remote/        # 网络请求（Retrofit）
    │   │   └── local/         # 本地存储（SharedPreferences）
    │   ├── worker/            # WorkManager任务
    │   └── FamilyRecipesApp.kt
    ├── app/src/main/res/      # 资源文件
    └── app/build.gradle       # Gradle配置
```

## 快速开始

### 后端部署

#### 1. 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

#### 2. 数据库配置

创建数据库：
```sql
CREATE DATABASE family_recipes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行 `backend/src/main/resources/schema.sql` 创建表结构：
```bash
mysql -u root -p family_recipes < backend/src/main/resources/schema.sql
```

#### 3. 配置文件

编辑 `backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/family_recipes?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的密码

jwt:
  secret: your-secret-key-change-this-in-production-at-least-256-bits  # 修改JWT密钥
```

#### 4. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### Android 客户端

#### 1. 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- Gradle 8.2+

#### 2. 配置服务器地址

在 `android/app/src/main/java/com/familyrecipes/android/data/local/PreferenceManager.kt` 中修改：
```kotlin
var baseUrl: String
    get() = prefs.getString(KEY_BASE_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"
```

注意：
- `10.0.2.2` 是 Android 模拟器访问主机的地址
- 真机调试请改为电脑的局域网 IP，如 `http://192.168.1.100:8080`
- 生产环境改为服务器域名

#### 3. 构建运行

使用 Android Studio 打开 `android` 目录，然后：
1. 点击 "Sync Project with Gradle Files"
2. 点击 "Run" 按钮运行到模拟器或真机

或使用命令行：
```bash
cd android
./gradlew assembleDebug
# 生成的APK在 app/build/outputs/apk/debug/
```

## API 文档

### 认证相关

#### 用户注册
```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "username": "张三"
}
```

#### 用户登录
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "张三"
    }
  }
}
```

### 菜谱相关

#### 搜索菜谱
```
GET /api/recipes/search?keyword=红烧肉&pageNum=1&pageSize=20
Authorization: Bearer {token}
```

#### 创建菜谱
```
POST /api/recipes
Authorization: Bearer {token}
Content-Type: application/json

{
  "recipe": {
    "name": "红烧肉",
    "description": "经典家常菜",
    "cookingTime": 60,
    "difficulty": 3,
    "servings": 4
  },
  "tags": [
    {"tagType": "meal_time", "tagValue": "午餐"},
    {"tagType": "dish_type", "tagValue": "炒菜"}
  ],
  "ingredients": [
    {"ingredientId": 1, "amount": "500g", "isMain": true}
  ],
  "steps": [
    {"stepNumber": 1, "description": "五花肉切块"}
  ],
  "cookUserIds": [1, 2]
}
```

### 冰箱相关

#### 获取冰箱食材
```
GET /api/fridge/items
Authorization: Bearer {token}
```

#### 添加食材
```
POST /api/fridge/items
Authorization: Bearer {token}
Content-Type: application/json

{
  "ingredientId": 1,
  "amount": "500g",
  "expiryDate": "2024-12-31",
  "storageLocation": "冷藏"
}
```

## 数据库设计

主要表结构：

- **user**: 用户表
- **recipe**: 菜谱表
- **recipe_tag**: 菜谱标签表
- **ingredient**: 食材表
- **recipe_ingredient**: 菜谱食材关联表
- **cooking_step**: 烹饪步骤表
- **external_recipe**: 外链食谱表
- **recipe_cook**: 菜谱厨师关联表
- **user_favorite**: 用户收藏表
- **fridge_item**: 冰箱食材表
- **reminder_setting**: 提醒设置表
- **cooking_history**: 烹饪记录表

详见 `backend/src/main/resources/schema.sql`

## 云端部署建议

### 后端部署

#### 使用 Docker
```dockerfile
FROM openjdk:17-slim
COPY target/backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

#### 云服务器
推荐使用：
- 阿里云 ECS
- 腾讯云 CVM
- AWS EC2

最低配置：1核2G，10G硬盘

### 数据库
- 使用云数据库服务（RDS）更稳定
- 或自建 MySQL（记得定期备份）

### 文件存储
- 本地存储：适合小规模使用
- 对象存储：推荐使用阿里云 OSS、腾讯云 COS、AWS S3

## 开发说明

### 后端开发
- 所有接口返回统一的 `Result<T>` 格式
- 使用 JWT 进行认证，token 有效期 7 天
- 密码使用 SHA-256 哈希（生产环境建议改用 BCrypt）
- 使用 MyBatis 注解 + XML 混合方式

### Android 开发
- 使用 Kotlin 协程处理异步任务
- ViewBinding 替代 findViewById
- Material Design 3 设计规范
- 所有图片使用 Glide 加载

## 待完善功能

- [ ] 推荐算法完善（接入第三方 API）
- [ ] 社交功能（评论、分享）
- [ ] 营养分析
- [ ] 购物清单生成
- [ ] 导出菜谱为PDF
- [ ] 深色模式
- [ ] 多语言支持

## 常见问题

**Q: Android 无法连接后端？**
A: 检查服务器地址配置，模拟器用 `10.0.2.2`，真机用局域网 IP

**Q: 数据库连接失败？**
A: 检查 MySQL 是否启动，用户名密码是否正确，防火墙是否开放 3306 端口

**Q: 图片上传失败？**
A: 检查 `application.yml` 中的 `file.upload-dir` 路径是否有写权限

**Q: 提醒功能不工作？**
A: Android 8.0+ 需要用户授予通知权限，WorkManager 依赖网络连接

## 许可证

MIT License

## 作者

Yang Wang

---

**祝您使用愉快！如有问题欢迎提 Issue** 🎉

