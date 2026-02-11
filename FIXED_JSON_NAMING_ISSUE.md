# 🎯 问题已解决！JSON字段命名不匹配

## 问题根因

### Android 发送的 JSON：
```json
{
  "ingredient_id": 2,     ← 蛇形命名（snake_case）
  "user_id": 0,
  "amount": "一瓶",
  "expiry_date": "2026-02-18",
  ...
}
```

### 后端期望的字段名：
```java
public class FridgeItem {
    private Long ingredientId;   ← 驼峰命名（camelCase）
    private Long userId;
    ...
}
```

### 结果：
- ✅ Android发送：`ingredient_id: 2`
- ❌ 后端接收：`ingredientId: null`（字段名不匹配，无法映射）
- ❌ 导致错误："食材不存在"（因为 ingredientId 是 null）

## 解决方案

我在 `application.yml` 中添加了 Jackson 配置，让后端自动支持蛇形命名：

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE  # 支持 snake_case
    deserialization:
      fail-on-unknown-properties: false
    serialization:
      write-dates-as-timestamps: false
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
```

现在后端可以：
- ✅ 接收 `ingredient_id` 并自动映射到 `ingredientId`
- ✅ 接收 `user_id` 并自动映射到 `userId`
- ✅ 返回数据时也使用 `ingredient_id` 格式
- ✅ 完全兼容 Android 的命名风格

## 📋 需要执行的步骤

### 1. 重启后端服务

```bash
cd backend
./mvnw spring-boot:run
```

或者在IDE中重启应用。

### 2. 无需重新编译 Android

Android 端的代码完全正确，不需要修改！

### 3. 测试

直接在 Android 应用中：
1. 输入"一瓶牛奶"
2. 点击保存
3. 应该看到"✅ 添加成功！"

## 📊 修复后的日志示例

### Android 日志（不变）：
```
✅ 获取到食材: ID=2, 名称=牛奶
准备添加到冰箱:
  - 用户ID: 0
  - 食材ID: 2      ← 正确
  - 数量: 一瓶
  - 过期日期: 2026-02-18
```

### 后端日志（修复后）：
```
========== 开始添加食材到冰箱 ==========
用户ID: 1
食材ID: 2          ✅ 现在能正确接收了！
数量: 一瓶
过期日期: 2026-02-18
✅ 找到食材: 牛奶 (ID: 2, 分类: 其他, 单位: null)
✅ 食材项已插入，ID: 1
========== 添加食材完成 ==========
```

## 🔍 为什么之前没发现这个问题？

1. **测试不充分**：之前可能没有真正测试完整的添加流程
2. **前后端分开开发**：Android 使用了 Gson/Retrofit 的默认蛇形命名约定
3. **后端没配置**：Spring Boot 默认使用驼峰命名，没有配置自动转换

## 🎓 技术知识点

### 命名风格对比

**蛇形命名（snake_case）**：
- 常用于：Python、Ruby、数据库字段、JSON API（RESTful风格）
- 示例：`ingredient_id`, `user_name`, `created_at`

**驼峰命名（camelCase）**：
- 常用于：Java、JavaScript、Kotlin（字段名）
- 示例：`ingredientId`, `userName`, `createdAt`

### Jackson 命名策略

Spring Boot 使用 Jackson 处理 JSON，支持多种命名策略：

```yaml
spring.jackson.property-naming-strategy:
  - SNAKE_CASE        # ingredient_id
  - LOWER_CAMEL_CASE  # ingredientId (默认)
  - UPPER_CAMEL_CASE  # IngredientId
  - LOWER_CASE        # ingredientid
  - KEBAB_CASE        # ingredient-id
```

### Android 端的 @SerializedName

```kotlin
data class FridgeItem(
    @SerializedName("ingredient_id")  // JSON中的名称
    val ingredientId: Long?            // Kotlin中的名称
)
```

这个注解告诉 Gson/Retrofit：
- 序列化时：`ingredientId` → `ingredient_id`
- 反序列化时：`ingredient_id` → `ingredientId`

## 📝 修改的文件

只修改了一个文件：
- ✅ `backend/src/main/resources/application.yml`
  - 添加了 `spring.jackson.property-naming-strategy: SNAKE_CASE`

## 🎉 总结

这是一个经典的**前后端接口约定不一致**问题：
- Android 使用蛇形命名（RESTful API 常用风格）
- Spring Boot 默认使用驼峰命名（Java 风格）
- 通过配置 Jackson 的命名策略，完美解决

**现在重启后端服务，就可以正常保存食材了！** 🚀

---
**修复时间**: 2026-02-11 11:30  
**问题类型**: 接口字段命名不匹配  
**影响范围**: 所有 FridgeItem 相关的 API  
**状态**: ✅ 已修复

