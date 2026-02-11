# 配置管理系统重构文档

## 概述

将应用中的硬编码配置（常见食材、存储位置、系统文本等）迁移到数据库存储，实现配置的集中管理和动态更新。

## 架构设计

### 1. 数据库设计

创建了4个配置表（统一使用 `config_` 前缀）：

#### `config_ingredients` - 常见食材配置表
- 存储所有常见食材信息
- 支持分类、排序、启用/禁用
- 已预置180+常见食材数据

#### `config_storage_locations` - 存储位置配置表
- 存储食材存储位置选项
- 支持设置默认位置
- 已预置10个常用位置

#### `config_categories` - 食材分类配置表
- 管理食材分类（肉类、海鲜、蔬菜等）
- 已预置9个分类

#### `config_texts` - 系统文本配置表
- 存储界面显示文本（按钮文本、状态文本等）
- 使用 key-value 格式
- 支持按分类管理

### 2. 后端实现

**Entity 层**（`entity/config/`）:
- `ConfigIngredient.java`
- `ConfigStorageLocation.java`
- `ConfigCategory.java`
- `ConfigText.java`

**Mapper 层**（`mapper/config/ConfigMapper.java`）:
```java
@Select("SELECT * FROM config_ingredients WHERE is_enabled = 1 ORDER BY sort_order")
List<ConfigIngredient> findAllIngredients();

@Select("SELECT * FROM config_storage_locations WHERE is_default = 1 LIMIT 1")
ConfigStorageLocation findDefaultStorageLocation();
```

**Service 层**（`ConfigService.java`）:
- 使用 `@Cacheable` 注解实现缓存
- 提供 `getAllConfigs()` 一次性获取所有配置

**Controller 层**（`ConfigController.java`）:
```
GET /api/config/all                  - 获取所有配置
GET /api/config/ingredients          - 获取食材列表
GET /api/config/storage-locations    - 获取存储位置
GET /api/config/categories           - 获取分类列表
GET /api/config/texts                - 获取文本配置
```

### 3. Android 实现

**数据模型**（`ConfigModels.kt`）:
```kotlin
data class ConfigIngredient(...)
data class ConfigStorageLocation(...)
data class ConfigCategory(...)
data class ConfigText(...)
data class AppConfig(...)  // 完整配置
```

**配置管理器**（`ConfigManager.kt`）:
```kotlin
object ConfigManager {
    suspend fun initialize(context: Context, forceRefresh: Boolean = false)
    fun getIngredients(): List<String>
    fun getStorageLocations(): List<String>
    fun getDefaultStorageLocation(): String
    fun getText(key: String, defaultValue: String = ""): String
}
```

**特性**:
- ✅ 应用启动时自动加载配置
- ✅ 内存缓存，快速访问
- ✅ 24小时自动更新
- ✅ 服务器不可用时使用默认配置（fallback）
- ✅ 支持强制刷新

**初始化**（`FamilyRecipesApp.kt`）:
```kotlin
override fun onCreate() {
    super.onCreate()
    // ...
    initializeConfig()  // 异步加载配置
}
```

### 4. 代码适配

**`CommonIngredients.kt`**:
```kotlin
// 改为从 ConfigManager 读取
val INGREDIENTS: List<String>
    get() = ConfigManager.getIngredients()

val STORAGE_LOCATIONS: List<String>
    get() = ConfigManager.getStorageLocations()
```

**`IngredientParser.kt`**:
```kotlin
data class ParseResult(
    ...
    // 从硬编码改为动态获取
    val storageLocation: String = ConfigManager.getDefaultStorageLocation()
)
```

## 使用方式

### 后端管理配置

1. **修改食材列表**:
```sql
INSERT INTO config_ingredients (name, category, sort_order) VALUES ('新食材', '蔬菜', 999);
UPDATE config_ingredients SET is_enabled = 0 WHERE name = '旧食材';
```

2. **修改存储位置**:
```sql
UPDATE config_storage_locations SET is_default = 1 WHERE name = '冰箱冷藏';
```

3. **修改文本配置**:
```sql
UPDATE config_texts SET config_value = '标记为已用完' WHERE config_key = 'button.consume';
```

### Android 使用配置

```kotlin
// 获取食材列表
val ingredients = ConfigManager.getIngredients()

// 获取存储位置
val locations = ConfigManager.getStorageLocations()

// 获取默认存储位置
val defaultLocation = ConfigManager.getDefaultStorageLocation()

// 获取文本配置
val buttonText = ConfigManager.getText("button.save", "保存")

// 强制刷新配置
lifecycleScope.launch {
    ConfigManager.initialize(context, forceRefresh = true)
}
```

## 迁移步骤

1. ✅ 执行数据库迁移脚本 `002_create_config_tables.sql`
2. ✅ 重启后端服务（自动创建表并插入初始数据）
3. ✅ 重新编译Android应用
4. ✅ 首次启动时会自动从服务器加载配置

## 优势

### 集中管理
- 所有配置集中在数据库，便于维护
- 可通过管理后台修改配置（未来可扩展）

### 动态更新
- 修改配置无需发版
- 客户端自动定期同步
- 支持 A/B 测试

### 性能优化
- 后端使用缓存（Spring Cache）
- 前端内存缓存
- 减少硬编码带来的维护成本

### 可扩展性
- 易于添加新配置类型
- 支持多语言（未来可扩展）
- 支持个性化配置（用户级配置）

## 注意事项

1. **首次启动**: 需要网络连接才能加载配置
2. **Fallback 机制**: 网络不可用时使用内置默认配置
3. **缓存更新**: 默认24小时更新一次，可手动刷新
4. **向后兼容**: `CommonIngredients` 保留作为兼容层

## 后续优化

- [ ] 添加配置管理后台界面
- [ ] 支持多语言配置
- [ ] 支持用户级个性化配置
- [ ] 添加配置版本管理
- [ ] 添加配置审核流程

## 文件清单

### 后端
- `migrations/002_create_config_tables.sql`
- `entity/config/ConfigIngredient.java`
- `entity/config/ConfigStorageLocation.java`
- `entity/config/ConfigCategory.java`
- `entity/config/ConfigText.java`
- `mapper/config/ConfigMapper.java`
- `service/ConfigService.java`
- `controller/ConfigController.java`

### Android
- `data/model/ConfigModels.kt`
- `data/local/ConfigManager.kt`
- `data/remote/ApiService.kt` (添加配置API)
- `FamilyRecipesApp.kt` (添加配置初始化)
- `util/CommonIngredients.kt` (适配为兼容层)
- `util/IngredientParser.kt` (使用动态配置)

---

**创建日期**: 2026-02-11
**版本**: 1.0
**状态**: ✅ 已完成

