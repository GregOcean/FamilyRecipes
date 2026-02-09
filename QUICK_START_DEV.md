# 快速开始（开发模式）

## 🚀 3 步启动应用（无需登录）

### 第 1 步：初始化数据库并创建默认用户

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库（如果还没创建）
CREATE DATABASE family_recipes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE family_recipes;

# 执行初始化脚本
SOURCE /Users/yang.wang/Documents/Personal_Project/FamilyRecipes/backend/src/main/resources/schema.sql;
SOURCE /Users/yang.wang/Documents/Personal_Project/FamilyRecipes/backend/src/main/resources/data.sql;

# 验证默认用户是否创建成功
SELECT * FROM user WHERE id = 1;
```

**预期输出：**
```
+----+----------------------------+----------+------------------+--------+---------------------+---------------------+
| id | email                      | password | username         | avatar | created_at          | updated_at          |
+----+----------------------------+----------+------------------+--------+---------------------+---------------------+
|  1 | default@familyrecipes.com  | $2a$...  | 家肴用户_DEV     | NULL   | 2026-02-09 12:00:00 | 2026-02-09 12:00:00 |
+----+----------------------------+----------+------------------+--------+---------------------+---------------------+
```

### 第 2 步：启动后端服务

```bash
cd /Users/yang.wang/Documents/Personal_Project/FamilyRecipes/backend

# 启动后端（会自动执行 schema.sql 和 data.sql）
./mvnw spring-boot:run
```

**预期输出：**
```
Started FamilyRecipesApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

**关键日志（说明已开启开发模式）：**
```
⚠️ 开发模式：未提供Token，使用默认用户ID: 1
```

### 第 3 步：测试后端 API

```bash
cd /Users/yang.wang/Documents/Personal_Project/FamilyRecipes

# 运行开发模式测试（无需登录）
./test-api-no-auth.sh
```

**预期输出：**
```
✓ 后端已启动且处于开发模式
✓ 查询成功（无需登录）
✓ 创建菜谱成功（无需登录）
✓ 创建者ID为1（默认用户），符合预期
✓ 开发模式测试通过！
```

---

## 📱 Android 客户端使用

### 方式 1：直接使用（推荐）

现在 Android 客户端**无需登录**即可使用所有功能：

1. 打开应用
2. 直接点击底部的"+"按钮
3. 填写菜谱信息，点击"发布"
4. ✅ 发布成功！所有操作会记录在"家肴用户_DEV"名下

### 方式 2：使用默认账号登录（可选）

如果想测试登录功能：

1. 进入"我的"Tab
2. 输入：
   - 邮箱：`default@familyrecipes.com`
   - 密码：`123456`
3. 点击"登录"

---

## ✅ 验证是否成功

### 检查后端日志

启动后端后，发送任意请求，应该看到：

```
DEBUG c.f.interceptor.AuthInterceptor - 拦截请求: POST /api/recipes
DEBUG c.f.interceptor.AuthInterceptor - Authorization Header: null
INFO  c.f.interceptor.AuthInterceptor - ⚠️ 开发模式：未提供Token，使用默认用户ID: 1
```

### 检查数据库

```sql
-- 查看默认用户
SELECT * FROM user WHERE id = 1;

-- 查看默认用户创建的菜谱
SELECT * FROM recipe WHERE creator_id = 1;
```

### 使用 curl 测试

```bash
# 不带 Token，直接创建菜谱
curl -X POST http://localhost:8080/api/recipes \
  -H "Content-Type: application/json" \
  -d '{
    "recipe": {
      "name": "测试菜谱",
      "description": "无需登录即可创建"
    },
    "tags": [{"tagType": "time", "tagValue": "午餐"}]
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "测试菜谱",
    "creatorId": 1,
    ...
  }
}
```

---

## 🔧 故障排查

### 问题 1：后端启动时报错"用户已存在"

**原因：**`data.sql` 多次执行，尝试插入重复的用户。

**解决方案：**
```sql
-- 删除默认用户后重新创建
DELETE FROM user WHERE id = 1;

-- 重新执行
SOURCE /path/to/data.sql;
```

或者，直接使用已存在的用户即可。

### 问题 2：创建菜谱时仍然返回 401

**原因：**后端没有开启开发模式。

**检查：**
```bash
# 查看 AuthInterceptor.java 第 21 行
grep "DEV_MODE" backend/src/main/java/com/familyrecipes/interceptor/AuthInterceptor.java
```

**预期输出：**
```java
private static final boolean DEV_MODE = true; // 开发模式开关
```

如果是 `false`，改为 `true` 并重启后端。

### 问题 3：Android 客户端仍然要求登录

**原因：**Android 客户端的代码可能有登录检查。

**临时解决方案：**
在"我的"Tab 使用默认账号登录：
- 邮箱：`default@familyrecipes.com`
- 密码：`123456`

### 问题 4：找不到默认用户（creator_id = 1）

**原因：**数据库中没有 ID=1 的用户。

**解决方案：**
```bash
mysql -u root -p family_recipes < backend/src/main/resources/data.sql
```

或手动插入：
```sql
INSERT INTO `user` (id, email, password, username) 
VALUES (1, 'default@familyrecipes.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye1VZjV4D8KQiwQNQON9gABU1Q2TaQZB6', '家肴用户_DEV');
```

---

## 📊 开发模式 vs 生产模式

| 功能 | 开发模式 | 生产模式 |
|------|---------|---------|
| 登录要求 | ❌ 可选 | ✅ 必须 |
| Token 验证 | 宽松（失败则用默认用户） | 严格（失败则拒绝） |
| 默认用户 | ✅ 启用 (ID=1) | ❌ 禁用 |
| 数据记录 | 默认用户或实际用户 | 实际用户 |
| 适用场景 | 开发、调试、演示 | 生产环境 |

---

## 🎯 下一步

### 开发阶段
✅ 当前配置已完成，可以直接开始开发和测试：
- 添加菜谱功能
- 搜索功能
- 收藏功能
- 冰箱管理功能

### 准备上线时
切换到生产模式，参考 `DEV_MODE.md` 的"关闭开发模式"章节。

---

## 📚 相关文档

- `DEV_MODE.md` - 开发模式详细说明
- `TROUBLESHOOTING.md` - 详细故障排查指南
- `DEPLOYMENT.md` - 生产环境部署指南

---

如有问题，请查看后端日志或运行测试脚本进行诊断。

