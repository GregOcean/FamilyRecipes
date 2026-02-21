# 库存分类功能实现总结

## 🎯 需求
1. 将"食材"改名为"库存"，支持更广泛的物品类型（奶茶、剩菜、零食等）
2. 添加两级分类系统
3. 左侧展示分类树，支持展开/折叠
4. 支持智能分类匹配（基于关键词，未来可扩展为LLM）

## ✅ 已完成的工作

### 1. 后端实现

#### 数据库
- **新建表**: `inventory_categories` - 库存分类表
  - 支持两级分类结构（parent_id字段）
  - 包含图标、排序等字段
- **修改表**: `ingredients` 
  - 添加 `category_id` 字段（无外键约束）
  - 添加索引提升查询性能

#### 默认分类数据
**一级分类**（7个）:
- 🥕 原料
- 🍱 成品  
- 🥤 饮品
- 🥐 面点
- 🧂 调味料
- 🍪 零食
- 📦 其他

**二级分类示例**:
- 原料 → 蔬菜、肉类、海鲜、水果、蛋奶
- 饮品 → 酒精、无糖、茶饮、汽水、乳制品
- 面点 → 中式、西式
- 零食 → 膨化、糖果、坚果

#### Java代码
1. **Model**: `InventoryCategory.java` - 分类实体
2. **Mapper**: `InventoryCategoryMapper.java` - 数据访问层
3. **Service**: `InventoryCategoryService.java` - 业务逻辑
   - `getCategoryTree()` - 获取分类树
   - `smartMatchCategory()` - 智能匹配分类
4. **Controller**: `InventoryCategoryController.java` - API接口
   - `GET /api/inventory/categories/tree` - 获取分类树
   - `POST /api/inventory/categories/smart-match` - 智能匹配

#### 智能匹配规则
基于关键词匹配，示例：
```java
"奶茶" → 饮品/茶饮
"薯片" → 零食/膨化
"猪肉" → 原料/肉类
"剩菜" → 成品/剩菜
```

### 2. Android 实现

#### 数据模型
- **新增**: `InventoryCategory` - 分类数据类
- **修改**: `Ingredient` - 添加 `categoryId` 字段

#### API Service
```kotlin
@GET("/api/inventory/categories/tree")
suspend fun getCategoryTree(): Response<ApiResponse<List<InventoryCategory>>>

@POST("/api/inventory/categories/smart-match")
suspend fun smartMatchCategory(@Body itemName: String): Response<ApiResponse<Long>>
```

#### UI 文案更新
- ✅ 底部导航栏: "食材" → "库存"
- ✅ 添加菜单: "食材" → "库存"
- ✅ 提示文案: "添加食材" → "添加库存"

### 3. 数据库迁移
文件路径: `backend/src/main/resources/migrations/008_create_inventory_categories.sql`

## 📋 待实现功能

### Android UI - 分类筛选侧边栏
需要在 `FridgeFragment` 中实现：

```
┌─────┬───────────────────┐
│ 🥕  │  番茄 (2个)       │
│ 原料│  保质期：还剩3天   │
│ ├菜 │                  │
│ ├肉 │  半瓶奶茶         │
│ ├海 │  开封：今天        │
│ 🍱  │  建议2天内喝完    │
│ 成品│                  │
│ 🥤  │                  │
│ 饮品│                  │
│ ├酒 │                  │
│ ├茶▼│                  │
└─────┴───────────────────┘
```

#### 实现步骤
1. **修改布局** `fragment_fridge.xml`:
   ```xml
   <LinearLayout orientation="horizontal">
       <!-- 左侧分类列表 -->
       <RecyclerView 
           android:id="@+id/category_list"
           android:layout_width="80dp" />
       
       <!-- 右侧库存列表 -->
       <RecyclerView 
           android:id="@+id/items_list"
           android:layout_width="0dp"
           android:layout_weight="1" />
   </LinearLayout>
   ```

2. **创建适配器** `CategoryTreeAdapter.kt`:
   - 支持展开/折叠二级分类
   - 点击分类筛选库存列表
   - 高亮当前选中分类

3. **加载分类数据**:
   ```kotlin
   lifecycleScope.launch {
       val response = ApiClient.apiService.getCategoryTree()
       if (response.isSuccessful) {
           categoryAdapter.submitList(response.body()?.data)
       }
   }
   ```

4. **筛选逻辑**:
   ```kotlin
   fun filterByCategory(categoryId: Long?) {
       val filtered = if (categoryId == null) {
           allItems  // 显示全部
       } else {
           allItems.filter { it.categoryId == categoryId }
       }
       itemsAdapter.submitList(filtered)
   }
   ```

### 添加库存时的智能分类
在 `AddIngredientActivity` 中:
```kotlin
// 输入食材名称后，自动匹配分类
lifecycleScope.launch {
    val categoryId = ApiClient.apiService
        .smartMatchCategory(ingredientName)
        .body()?.data
    
    // 自动选中匹配的分类
    selectCategory(categoryId)
}
```

## 🔮 未来扩展

### LLM 智能分类
替换 `InventoryCategoryService.smartMatchCategory()` 的实现：

```java
public Long smartMatchCategory(String itemName) {
    // 调用 LLM API
    String prompt = "请根据物品名称判断分类: " + itemName;
    String categoryPath = callLLM(prompt);  // 例如: "饮品/茶饮"
    
    // 解析并返回分类ID
    return parseCategoryPath(categoryPath);
}
```

### 用户自定义分类
会员用户可以创建自己的分类：
```
POST /api/inventory/categories
{
  "name": "我的特殊分类",
  "parentId": 7,  // 挂在"其他"下
  "userId": 123
}
```

## 📝 注意事项

1. **无外键约束**: `ingredients.category_id` 没有外键，删除分类时不会影响已有库存
2. **默认分类**: 新增库存如果未指定分类，会自动匹配或使用"其他"
3. **图标显示**: 前端需要处理emoji图标的渲染
4. **性能优化**: 分类树数据量小，可以前端缓存

## 🚀 部署步骤

1. **执行数据库迁移**:
   ```sql
   source backend/src/main/resources/migrations/008_create_inventory_categories.sql
   ```

2. **重启后端服务**:
   ```bash
   cd backend && mvn spring-boot:run
   ```

3. **编译Android应用**:
   ```bash
   cd android && ./gradlew assembleDebug
   ```

4. **测试功能**:
   - 查看库存Tab是否显示为"库存"
   - 调用分类API是否返回正常
   - 添加库存时智能匹配是否生效

