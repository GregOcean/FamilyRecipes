# 发布失败排查指南

## 问题现象
Android 客户端发布菜谱时提示"发布失败，未知错误"，后端没有日志输出。

## 原因分析
后端所有 API 接口（除了 `/api/auth/**`）都需要 JWT 认证。如果用户没有登录，请求会被拦截器拦截并返回 401 错误。

---

## 排查步骤

### 步骤 1：检查后端是否正常启动

```bash
# 进入后端目录
cd /Users/yang.wang/Documents/Personal_Project/FamilyRecipes/backend

# 重新启动后端（会显示详细日志）
./mvnw spring-boot:run
```

**预期输出：**
```
Started FamilyRecipesApplication in X.XXX seconds
Tomcat started on port(s): 8080
```

如果启动失败，检查：
- MySQL 是否正常运行：`mysql -u root -p`
- 数据库 `family_recipes` 是否存在
- `application.yml` 中的数据库密码是否正确

---

### 步骤 2：检查 Android 客户端网络配置

#### 2.1 如果使用模拟器
默认配置 `http://10.0.2.2:8080` 是正确的（10.0.2.2 是模拟器访问宿主机的特殊地址）

#### 2.2 如果使用真机
需要修改 baseUrl 为电脑的局域网 IP：

```kotlin
// 在 PreferenceManager.kt 中修改
var baseUrl: String
    get() = prefs.getString(KEY_BASE_URL, "http://192.168.x.x:8080") ?: "http://192.168.x.x:8080"
```

**获取电脑 IP：**
```bash
# macOS/Linux
ifconfig | grep "inet "

# 找到类似 192.168.x.x 的地址（不是 127.0.0.1）
```

---

### 步骤 3：检查用户是否已登录

**Android 端检查：**

打开 Logcat，搜索以下关键词：
- `Authorization` - 查看是否发送了 Token
- `401` - 查看是否返回了未授权错误
- `OkHttp` - 查看完整的请求和响应

**关键日志示例：**
```
D/OkHttp: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

如果**没有**这行日志，说明用户未登录。

---

### 步骤 4：测试登录功能

#### 方法 1：使用 Android 客户端
1. 打开应用，进入"我的"Tab
2. 如果显示登录页面，点击"还没有账号？立即注册"
3. 输入邮箱、用户名、密码，点击"注册"
4. 注册成功后会自动登录

#### 方法 2：使用 curl 测试后端 API

**注册用户：**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "测试用户",
    "password": "123456"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "email": "test@example.com",
    "username": "测试用户"
  }
}
```

**登录获取 Token：**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "123456"
  }'
```

**预期响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "test@example.com",
      "username": "测试用户"
    }
  }
}
```

**使用 Token 创建菜谱：**
```bash
TOKEN="粘贴上面获取的token"

curl -X POST http://localhost:8080/api/recipes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "recipe": {
      "name": "测试菜谱",
      "description": "这是一个测试菜谱"
    },
    "tags": [
      {
        "tagType": "time",
        "tagValue": "午餐"
      }
    ]
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
    "description": "这是一个测试菜谱",
    ...
  }
}
```

---

### 步骤 5：查看后端详细日志

启动后端后，会输出详细日志：

```
2026-02-09 12:00:00.123 [http-nio-8080-exec-1] DEBUG c.f.interceptor.AuthInterceptor - 拦截请求: POST /api/recipes
2026-02-09 12:00:00.124 [http-nio-8080-exec-1] DEBUG c.f.interceptor.AuthInterceptor - Authorization Header: Bearer eyJhbGciOiJIUzI...
2026-02-09 12:00:00.125 [http-nio-8080-exec-1] DEBUG c.f.interceptor.AuthInterceptor - 提取到Token: eyJhbGciOiJIUzI1NiIs...
2026-02-09 12:00:00.130 [http-nio-8080-exec-1] DEBUG c.f.interceptor.AuthInterceptor - Token验证成功，用户ID: 1
```

**常见错误日志：**

1. **没有 Token：**
```
WARN  c.f.interceptor.AuthInterceptor - 请求 /api/recipes 没有提供Token
```
**解决方案：**用户需要先登录。

2. **Token 无效：**
```
WARN  c.f.interceptor.AuthInterceptor - 请求 /api/recipes 的Token无效或已过期
```
**解决方案：**Token 可能过期，需要重新登录。

---

## 快速解决方案

### 方案 1：在 Android 应用中先登录

1. 打开应用
2. 进入"我的"Tab
3. 注册/登录账号
4. 登录成功后再点击"+"按钮添加菜谱

### 方案 2：暂时禁用认证（仅用于调试）

**修改 `WebConfig.java`：**
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                    "/api/auth/**",
                    "/api/recipes/**",    // 临时放行菜谱接口
                    "/api/upload/**",     // 临时放行上传接口
                    "/files/**",
                    "/error"
            );
}
```

**⚠️ 注意：**生产环境必须启用认证！

---

## 检查清单

- [ ] 后端服务已启动，端口 8080 正常
- [ ] MySQL 数据库正常，`family_recipes` 库已创建并初始化
- [ ] Android 客户端能连接到后端（检查 baseUrl 配置）
- [ ] 用户已在 Android 客户端登录
- [ ] 发送请求时 Logcat 显示了 `Authorization: Bearer ...` 头
- [ ] 后端日志显示"Token验证成功"

---

## 常见问题

### Q1: Android 客户端能否直接发布，无需登录？
A: 不可以。所有菜谱操作都需要认证，因为需要记录创建者信息。

### Q2: 如何查看 Android 客户端是否已登录？
A: 进入"我的"Tab，如果显示用户信息（而非登录表单），说明已登录。

### Q3: 如何重置登录状态？
A: 在"我的"Tab 点击"退出登录"按钮，或清除应用数据。

### Q4: 模拟器无法连接后端？
A: 
- 确保后端运行在 `0.0.0.0:8080` 而非 `127.0.0.1:8080`
- 确保 baseUrl 使用 `http://10.0.2.2:8080`
- 检查防火墙是否阻止了 8080 端口

---

## 下一步

如果按照以上步骤排查后仍有问题，请提供：
1. 后端完整启动日志
2. Android Logcat 中的错误信息
3. 后端拦截器日志（搜索 "AuthInterceptor"）

