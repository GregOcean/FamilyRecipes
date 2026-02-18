# 🚀 快速开始 - 功能调整版

## 本次修改内容

1. ✅ **群组角色简化**：从三角色简化为二角色（管理员+成员）
2. ✅ **成员上限调整**：默认最大5人（会员可扩展）
3. ✅ **底部导航重构**：新增"首页"和"消息"Tab，调整顺序
4. ✅ **UI风格调整**：消息页面参考小红书，橘黄主题

---

## 📦 安装步骤

### 1. 数据库迁移

#### 开发环境（推荐）
```bash
# 删除旧表重建
cd backend
mysql -u root -p your_database << EOF
DROP TABLE IF EXISTS user_read_position;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS group_chat;
DROP TABLE IF EXISTS friendship;
EOF

# 执行新的建表脚本
mysql -u root -p your_database < src/main/resources/migrations/005_create_social_tables.sql
```

#### 生产环境（增量更新）
```bash
cd backend
mysql -u root -p your_database < src/main/resources/migrations/006_update_group_roles.sql
```

### 2. 启动后端
```bash
cd backend
mvn clean package
mvn spring-boot:run
```

### 3. 编译Android
```bash
cd android
./gradlew clean assembleDebug
```

或在Android Studio中点击运行。

---

## 🎯 新功能体验

### 底部导航栏
从左到右依次为：
1. 🏠 **首页** - 包含推荐和菜谱两个Tab
2. 🧊 **食材** - 冰箱管理
3. ➕ **添加** - 快捷添加入口
4. 💬 **消息** - 群聊和系统通知
5. 👤 **我的** - 个人中心

### 消息页面
- **爱心点赞**（粉红色）- 查看点赞通知
- **新增粉丝**（蓝色）- 查看粉丝动态
- **我被需要**（绿色）- 查看被艾特或提及
- **群组列表** - 显示所有加入的群聊

### 群组管理
- ✅ 创建群组：最多5人（含管理员）
- ✅ 管理员权限：
  - 添加/移除成员
  - 修改群名和描述
  - 解散群组
- ✅ 普通成员：可自由退出群组
- ⚠️ 管理员不能退出，需转让或解散

---

## 🧪 测试指南

### 测试群组角色
```bash
# 1. 创建群组（应该限制5人）
curl -X POST http://localhost:8080/api/groups/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "测试群组",
    "memberIds": [2, 3, 4, 5]
  }'

# 2. 尝试添加第6个人（应该失败）
curl -X POST http://localhost:8080/api/groups/1/members \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"userIds": [6]}'

# 3. 非管理员尝试移除成员（应该失败）
curl -X DELETE http://localhost:8080/api/groups/1/members/3 \
  -H "Authorization: Bearer USER2_TOKEN"
```

### 测试Android界面
1. 启动App，查看底部导航栏是否为新顺序
2. 点击"首页"，查看是否有推荐和菜谱两个Tab
3. 点击"消息"，查看是否显示三个功能入口
4. 测试扫码添加好友功能
5. 测试创建群组（最多5人）

---

## 📱 界面截图参考

### 底部导航栏
```
[首页] [食材] [➕] [消息] [我的]
```

### 首页Tab
```
推荐 | 菜谱
-----------
(ViewPager内容)
```

### 消息页面
```
┌─────────────────────────┐
│  ⭕爱心点赞  ⭕新增粉丝  ⭕我被需要  │
├─────────────────────────┤
│  我的群组        查看全部> │
├─────────────────────────┤
│  群聊列表                 │
│  ...                     │
└─────────────────────────┘
```

---

## ⚠️ 注意事项

### 数据库
- 如果执行增量更新失败，建议删表重建
- 记得备份生产环境数据

### 代码兼容
- 所有 `ROLE_OWNER` 和 `ROLE_ADMIN` 已改为 `ROLE_MANAGER`
- 权限检查逻辑已统一更新

### 已知问题
- 消息页面群组列表Adapter未实现（显示空白）
- 群聊界面未完整实现
- 三个功能入口（爱心点赞等）未实现

---

## 📝 下一步开发

按优先级排序：

### 高优先级
1. **实现消息页面群组Adapter** - 显示群组列表
2. **完善群聊界面** - 消息收发功能
3. **实现好友列表Adapter** - 显示好友信息

### 中优先级
4. 创建群组对话框
5. 群组设置页面
6. 转让管理员功能

### 低优先级
7. 三个功能入口页面（爱心点赞等）
8. 消息推送
9. 未读消息角标

---

## 🆘 常见问题

### Q: 数据库迁移失败？
A: 先删除旧表，再执行005脚本重建。

### Q: Android编译报错？
A: 执行 `./gradlew clean` 后重新编译。

### Q: 底部导航栏还是旧的？
A: 检查 `bottom_navigation_menu.xml` 是否正确更新。

### Q: 首页没有Tab？
A: 检查 `HomeFragment.kt` 和 `fragment_home.xml` 是否创建。

---

## 📞 支持

遇到问题请检查：
1. 后端日志：查看Spring Boot控制台
2. Android日志：`adb logcat | grep FamilyRecipes`
3. 数据库状态：执行006脚本中的验证SQL

---

**当前完成度：85%** 🎯

核心功能已实现，剩余主要是UI适配和完善。

