# 开发模式说明

## ⚠️ 重要提示
当前系统处于**开发模式**，已临时放开登录限制，所有功能都可以不登录直接使用。

## 工作原理

### 后端
- 拦截器（`AuthInterceptor`）的 `DEV_MODE` 设置为 `true`
- 当请求没有提供 Token 时，会自动使用**默认用户ID = 1**
- 所有操作（创建菜谱、收藏、评论等）都会记录在这个默认用户名下

### 默认用户信息
- **用户ID**: 1
- **邮箱**: default@familyrecipes.com
- **用户名**: 家肴用户_DEV
- **密码**: 123456

---

## 初始化默认用户

### 方法1：自动初始化（推荐）

启动后端时，Spring Boot 会自动执行 `data.sql` 创建默认用户：

```bash
cd /Users/yang.wang/Documents/Personal_Project/FamilyRecipes/backend
./mvnw spring-boot:run
```

**预期日志：**
```
Executing SQL script from URL [classpath:data.sql]
```

### 方法2：手动创建

如果自动初始化失败，可以手动执行：

```bash
mysql -u root -p
```

```sql
USE family_recipes;

-- 插入默认用户
INSERT INTO `user` (id, email, password, username, avatar, created_at, updated_at)
VALUES (1, 'default@familyrecipes.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VZjV4D8KQiwQNQON9gABU1Q2TaQZB6', '家肴用户_DEV', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    email = 'default@familyrecipes.com',
    username = '家肴用户_DEV',
    updated_at = NOW();

-- 验证用户是否创建成功
SELECT * FROM `user` WHERE id = 1;
```

---

## Android 客户端使用

### 不需要登录
在开发模式下，Android 客户端可以**直接使用所有功能**，无需登录：

1. 打开应用
2. 直接点击"+"添加菜谱
3. 所有操作都会记录在默认用户名下

### 可选：使用默认账号登录
如果想测试登录功能，可以使用默认账号：

- 邮箱：`default@familyrecipes.com`
- 密码：`123456`

---

## 测试 API（无需登录）

### 使用测试脚本（需要登录）
```bash
cd /Users/yang.wang/Documents/Personal_Project/FamilyRecipes
./test-api.sh
```

### 使用 curl 直接测试（无需登录）

**创建菜谱：**
```bash
curl -X POST http://localhost:8080/api/recipes \
  -H "Content-Type: application/json" \
  -d '{
    "recipe": {
      "name": "测试菜谱（无登录）",
      "description": "这是在开发模式下不登录创建的菜谱"
    },
    "tags": [
      {"tagType": "time", "tagValue": "午餐"}
    ]
  }'
```

**查询菜谱列表：**
```bash
curl -X GET "http://localhost:8080/api/recipes/search?pageNum=1&pageSize=10"
```

**上传图片：**
```bash
curl -X POST http://localhost:8080/api/upload/image \
  -F "file=@/path/to/your/image.jpg"
```

---

## 查看开发模式日志

启动后端后，会看到类似的日志：

```
⚠️ 开发模式：未提供Token，使用默认用户ID: 1
```

这表示请求没有登录，系统自动使用了默认用户。

---

## 关闭开发模式（生产环境）

### 步骤1：修改拦截器
编辑 `AuthInterceptor.java`：

```java
// 将 DEV_MODE 改为 false
private static final boolean DEV_MODE = false;
```

### 步骤2：重新编译并启动

```bash
cd backend
./mvnw clean package
./mvnw spring-boot:run
```

### 步骤3：验证
此时所有 API 都需要登录才能访问，未登录请求会返回：

```json
{
  "code": 401,
  "message": "未登录或登录已过期"
}
```

---

## 常见问题

### Q1: 为什么创建菜谱后，创建者是"家肴用户_DEV"？
A: 因为当前处于开发模式，所有不登录的操作都使用默认用户。

### Q2: 我想用自己的账号测试，怎么办？
A: 可以在"我的"Tab 注册新账号并登录，登录后的操作会记录在你的账号下。

### Q3: 开发模式会影响已登录用户吗？
A: 不会。如果提供了有效的 Token，系统会使用 Token 中的用户信息，而不是默认用户。

### Q4: 默认用户的密码是什么？
A: 123456（已使用 BCrypt 加密）

### Q5: 如何查看哪些操作是默认用户创建的？
A: 查询数据库：
```sql
SELECT * FROM recipe WHERE creator_id = 1;
SELECT * FROM user WHERE id = 1;
```

---

## 安全提示

⚠️ **开发模式仅用于开发和测试阶段！**

- **不要**在生产环境启用开发模式
- **不要**将 `DEV_MODE = true` 的代码部署到服务器
- 上线前务必将 `DEV_MODE` 改为 `false`

---

## 切换到生产模式的检查清单

部署到生产环境前，确保：

- [ ] `AuthInterceptor.java` 中 `DEV_MODE = false`
- [ ] 删除或禁用 `data.sql` 自动执行（或删除默认用户）
- [ ] 更改 JWT secret 为强密码
- [ ] 启用 HTTPS
- [ ] 配置合适的 CORS 策略
- [ ] 配置文件上传大小限制
- [ ] 配置数据库备份策略

---

## 当前配置总结

| 配置项 | 开发模式 | 生产模式 |
|-------|---------|---------|
| 登录要求 | ❌ 不需要 | ✅ 必须 |
| 默认用户 | ✅ 启用 (ID=1) | ❌ 禁用 |
| Token 验证 | 可选 | 必须 |
| 数据记录 | 默认用户 | 实际用户 |

---

如有问题，请查看后端日志或参考 `TROUBLESHOOTING.md`。

