# 调试指南：食材不存在错误

## 📋 我已添加详细的调试日志

为了找出"食材不存在"错误的真正原因，我在前后端都添加了详细的日志。

## 🔍 如何查看日志

### 后端日志（Spring Boot）

重启后端后，在终端可以看到日志输出：

```bash
cd backend
./mvnw spring-boot:run
```

当你保存"牛奶"时，应该会看到类似这样的日志：

```
--- 开始获取或创建食材: 牛奶 ---
食材不存在，创建新食材...
✅ 食材已创建，ID: 1, 名称: 牛奶, 分类: 其他

========== 开始添加食材到冰箱 ==========
用户ID: 1
食材ID: 1
数量: 一瓶
过期日期: 2026-02-18
✅ 找到食材: 牛奶 (ID: 1, 分类: 其他, 单位: null)
✅ 食材项已插入，ID: 1
========== 添加食材完成 ==========
```

**如果看到错误：**
```
❌ 食材不存在！食材ID: null
```
说明 `ingredient.id` 是 null，食材创建失败！

### Android 日志（Logcat）

在 Android Studio 中：
1. 打开 Logcat 面板
2. 过滤器输入：`AddIngredient`
3. 点击保存后查看日志

你应该看到：

```
========== 开始保存食材 ==========
食材名称: 牛奶
数量: 一瓶
保质期: 7天

--- 开始获取或创建食材: 牛奶 ---
搜索食材响应: true, code=200
找到 0 个相关食材
食材不存在，开始创建...
准备创建的食材: name=牛奶, category=其他, unit=null
创建食材响应: true, code=200
✅ 食材创建成功！ID=1, 名称=牛奶, 分类=其他

✅ 获取到食材: ID=1, 名称=牛奶
过期日期: 2026-02-18

准备添加到冰箱:
  - 用户ID: 1
  - 食材ID: 1
  - 数量: 一瓶
  - 过期日期: 2026-02-18

添加到冰箱响应: true, code=200
✅ 添加成功！
========== 保存流程结束 ==========
```

## 🐛 常见问题排查

### 问题 1: 食材ID为null

**日志特征：**
```
✅ 食材创建成功！ID=null, 名称=牛奶, 分类=其他
```

**原因：**
- MyBatis 的 `@Options(useGeneratedKeys = true, keyProperty = "id")` 没有生效
- 数据库表的 `id` 列不是 AUTO_INCREMENT

**解决方法：**
```sql
-- 检查表结构
DESC ingredient;

-- 确保 id 是 AUTO_INCREMENT
ALTER TABLE ingredient MODIFY COLUMN id BIGINT PRIMARY KEY AUTO_INCREMENT;
```

### 问题 2: 数据库连接问题

**日志特征：**
```
❌ 创建食材失败: Connection refused
```

**解决方法：**
- 检查 MySQL 是否运行：`mysql.server status`
- 检查 `application.yml` 中的数据库配置
- 确认数据库和表已创建

### 问题 3: 事务回滚

**日志特征：**
```
✅ 食材创建成功！ID=1
❌ 食材不存在！食材ID: 1
```

**原因：**
- 食材在一个事务中创建
- 查询在另一个未提交的事务中
- 事务隔离级别导致读不到

**解决方法：**
在 `FridgeController.createIngredient()` 中返回后刷新：

```java
@PostMapping("/ingredients")
public Result<Ingredient> createIngredient(@RequestBody Ingredient ingredient) {
    try {
        Ingredient created = fridgeService.getOrCreateIngredient(
            ingredient.getName(),
            ingredient.getCategory()
        );
        // 刷新以确保ID已设置
        if (created.getId() == null) {
            log.error("食材创建后ID仍为null！");
            return Result.error("食材创建失败：ID未生成");
        }
        return Result.success(created);
    } catch (Exception e) {
        log.error("创建食材异常", e);
        return Result.error(e.getMessage());
    }
}
```

### 问题 4: JSON序列化问题

**日志特征：**
```
创建食材响应: true, code=200
✅ 食材创建成功！ID=null
```

**原因：**
- 后端返回的 JSON 中 `id` 字段没有正确序列化
- Lombok 的 getter 方法有问题

**解决方法：**
在 `Ingredient.java` 中显式添加 getter/setter：

```java
@Data
public class Ingredient {
    private Long id;
    private String name;
    private String category;
    private String unit;
    
    // 显式 getter（如果 Lombok 有问题）
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
```

## 📝 下一步操作

1. **重启后端服务**
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. **重新编译 Android 应用**
   ```bash
   cd android
   ./gradlew clean assembleDebug
   ```

3. **执行测试并收集日志**
   - 输入"一瓶牛奶"
   - 点击保存
   - 查看后端控制台日志
   - 查看 Android Logcat 日志

4. **把日志发给我**
   - 复制后端的完整日志
   - 复制 Android 的 Logcat 日志（过滤 AddIngredient）
   - 我会根据日志精确定位问题

## 🎯 日志检查清单

查看后端日志，回答以下问题：

- [ ] 食材创建时，是否显示了 ID？
- [ ] 添加到冰箱时，传入的食材ID是多少？
- [ ] 查询食材时，是否找到了对应的食材？
- [ ] 是否有任何异常堆栈？

查看 Android 日志，回答以下问题：

- [ ] 创建食材的响应中，ID 是否为 null？
- [ ] 添加到冰箱时，传给后端的 ingredientId 是多少？
- [ ] 是否有网络请求失败的错误？

---

**重要：** 请执行上述步骤后，把日志发给我，我会立即帮你分析问题所在！

