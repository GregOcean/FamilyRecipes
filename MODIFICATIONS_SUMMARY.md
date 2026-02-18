# 功能调整总结

## 修改内容

### 1. 群组角色调整 ✅

#### 数据库变更
- 修改 `group_chat` 表
  - 添加 `manager_id` 字段：群管理员ID（有且只有1个）
  - 修改 `max_members` 默认值：从100改为5
  
- 修改 `group_member` 表
  - 简化 `role` 枚举：从 `owner/admin/member` 改为 `manager/member`

#### 后端代码调整
- `GroupChat.java`: 添加 `managerId` 字段，默认最大成员数为5
- `GroupMember.java`: 角色枚举改为 `manager` 和 `member`
- `GroupChatService.java`: 
  - 创建群组时设置创建者为管理员
  - 检查成员数量上限（5人）
  - 权限检查：只有管理员可以添加/移除成员、修改信息、解散群组
  - 管理员不能退出群组，需要转让或解散

#### Android代码调整
- `Models.kt`: `GroupMember` 角色常量改为 `ROLE_MANAGER` 和 `ROLE_MEMBER`

---

### 2. 底部导航栏重构 ✅

#### 新增"首页"Tab
**位置**：最左侧（替代原"推荐"tab）
**图标**：沿用菜谱的图标（`ic_recipe`）
**结构**：
- 二级Tab导航
  - Tab1: 推荐（展示原推荐页内容）
  - Tab2: 菜谱（展示原菜谱页内容）

**新建文件**：
- `HomeFragment.kt` - 首页Fragment，包含ViewPager2
- `fragment_home.xml` - 首页布局，含TabLayout和ViewPager2

#### 调整Tab顺序
新的底部导航顺序（从左到右）：
1. 🏠 **首页** (`nav_home`) - 新增，包含推荐和菜谱二级Tab
2. 🧊 **食材** (`nav_fridge`) - 从第4位移到第2位
3. ➕ **添加** (`nav_add`) - 位置不变（中间）
4. 💬 **消息** (`nav_message`) - 新增，替代原"推荐"位置
5. 👤 **我的** (`nav_profile`) - 位置不变

---

### 3. 新增"消息"Tab ✅

**位置**：第4位（加号右侧）
**风格**：参考小红书消息页面，橘黄主题色

#### 顶部三个功能入口
- **爱心点赞** - 粉红色圆形背景 (`#FFEBE6`)
- **新增粉丝** - 蓝色圆形背景 (`#E6F3FF`)
- **我被需要** - 绿色圆形背景 (`#E6FFE6`)

#### 群组列表区域
- 标题：我的群组
- 右侧：查看全部按钮 → 跳转到 `GroupsListActivity`
- 下拉刷新支持
- RecyclerView展示群组列表（待完善adapter）

**新建文件**：
- `MessageFragment.kt` - 消息Fragment
- `fragment_message.xml` - 消息页面布局
- `bg_message_icon_red.xml` - 爱心点赞背景
- `bg_message_icon_blue.xml` - 新增粉丝背景
- `bg_message_icon_green.xml` - 我被需要背景
- `ic_message.xml` - 消息图标

---

## 文件清单

### 后端修改文件
- ✅ `005_create_social_tables.sql` - 数据库表结构
- ✅ `GroupChat.java` - 添加managerId字段
- ✅ `GroupMember.java` - 简化角色枚举
- ✅ `GroupChatMapper.java` - 更新insert语句
- ✅ `GroupChatService.java` - 更新所有权限检查逻辑

### Android新建文件
- ✅ `HomeFragment.kt` - 首页Fragment
- ✅ `fragment_home.xml` - 首页布局
- ✅ `MessageFragment.kt` - 消息Fragment
- ✅ `fragment_message.xml` - 消息布局
- ✅ `ic_message.xml` - 消息图标
- ✅ `bg_message_icon_red.xml`
- ✅ `bg_message_icon_blue.xml`
- ✅ `bg_message_icon_green.xml`

### Android修改文件
- ✅ `Models.kt` - 更新GroupMember角色常量
- ✅ `MainActivity.kt` - 更新导航逻辑
- ✅ `bottom_navigation_menu.xml` - 更新menu配置

---

## 数据库迁移

需要执行以下操作：

### 方案1：删除旧表重新创建（推荐用于开发环境）
```sql
-- 删除旧的社交功能表
DROP TABLE IF EXISTS user_read_position;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS group_chat;
DROP TABLE IF EXISTS friendship;

-- 执行新的迁移脚本
SOURCE backend/src/main/resources/migrations/005_create_social_tables.sql;
```

### 方案2：增量修改（用于生产环境）
```sql
-- 修改group_chat表
ALTER TABLE group_chat 
ADD COLUMN manager_id BIGINT NOT NULL AFTER creator_id,
ADD FOREIGN KEY (manager_id) REFERENCES user(id),
ADD INDEX idx_manager (manager_id),
MODIFY COLUMN max_members INT DEFAULT 5 COMMENT '最大成员数（默认5人，会员可扩展）';

-- 更新已有群组的manager_id（设置为creator_id）
UPDATE group_chat SET manager_id = creator_id WHERE manager_id IS NULL;

-- 修改group_member表的role枚举
ALTER TABLE group_member 
MODIFY COLUMN role ENUM('manager', 'member') DEFAULT 'member' COMMENT '角色：manager-群管理员，member-普通成员';

-- 更新已有成员的角色
UPDATE group_member SET role = 'manager' WHERE role = 'owner' OR role = 'admin';
```

---

## 启动说明

### 后端
```bash
cd backend
# 执行数据库迁移
mysql -u your_user -p your_db < src/main/resources/migrations/005_create_social_tables.sql

# 启动服务
mvn spring-boot:run
```

### Android
```bash
cd android
./gradlew assembleDebug
# 或在Android Studio中运行
```

---

## 功能验证清单

### 后端API
- [ ] 创建群组时检查5人上限
- [ ] 只有管理员能添加/移除成员
- [ ] 只有管理员能修改群组信息
- [ ] 只有管理员能解散群组
- [ ] 管理员不能退出群组

### Android界面
- [ ] 底部导航栏显示：首页、食材、➕、消息、我的
- [ ] 首页Tab包含推荐和菜谱两个子Tab
- [ ] 消息页面显示三个功能入口和群组列表
- [ ] 点击消息页面"查看全部"跳转到群组列表
- [ ] 搜索功能在首页正常工作

---

## 已知待完成

1. **消息页面Adapter** - 群组列表的RecyclerView adapter
2. **好友列表Adapter** - FriendsListActivity的adapter
3. **群聊界面** - GroupChatActivity完整实现
4. **三个功能入口实现** - 爱心点赞、新增粉丝、我被需要的详细页面

---

## 注意事项

⚠️ **数据库迁移**
- 开发环境建议删除旧表重建
- 生产环境需要使用增量修改SQL
- 迁移后需要重启后端服务

⚠️ **代码兼容性**
- 旧的角色检查代码（`ROLE_OWNER`、`ROLE_ADMIN`）已全部更新
- 如有其他地方引用需要同步修改

⚠️ **UI测试**
- 首页ViewPager滑动流畅性
- TabLayout指示器显示正确
- 消息页面下拉刷新正常

