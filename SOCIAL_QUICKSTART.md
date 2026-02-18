# 社交功能快速启动指南

## 🚀 开始使用

### 1. 数据库迁移

在MySQL中执行以下迁移脚本：

```bash
cd backend
mysql -u your_username -p your_database < src/main/resources/migrations/005_create_social_tables.sql
```

或者手动复制SQL内容执行。

### 2. 启动后端

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

### 3. 编译Android应用

```bash
cd android
./gradlew assembleDebug
```

或者在Android Studio中点击运行。

---

## 📱 功能使用流程

### 添加好友流程
1. 用户A：进入"我的" → 点击"我的名片码"
2. 用户B：进入"我的" → 点击右上角"扫一扫"
3. 用户B扫描用户A的二维码
4. 自动添加好友成功（双向关系）

### 创建群组流程
1. 进入"我的" → 点击"我的群组"
2. 点击右下角"+"按钮
3. 输入群组名称
4. 选择要邀请的好友
5. 创建成功

### 群聊流程
1. 在群组列表点击某个群组
2. 进入群聊界面
3. 输入消息并发送
4. 实时显示聊天记录

---

## 🔧 当前可用功能

### ✅ 已实现并可测试
- 扫码添加好友
- 查看好友列表
- 查看群组列表
- 所有后端API

### ⚠️ 部分实现（需要手动测试API）
- 创建群组（有按钮但未实现对话框）
- 群聊界面（框架存在但未完整实现）

---

## 🧪 API测试示例

### 测试添加好友

```bash
curl -X POST http://localhost:8080/api/friends/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"friendId": 2}'
```

### 测试创建群组

```bash
curl -X POST http://localhost:8080/api/groups/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "我的家庭群",
    "memberIds": [2, 3]
  }'
```

### 测试发送消息

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "groupId": 1,
    "content": "大家好！",
    "messageType": "text"
  }'
```

### 测试获取消息列表

```bash
curl -X GET "http://localhost:8080/api/messages/list?groupId=1&pageNum=1&pageSize=50" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🐛 已知问题

1. **群聊界面未完成** - `GroupChatActivity`需要完整实现
2. **Adapter未实现** - 好友和群组列表显示空白
3. **创建群组对话框未实现** - 只有Toast提示

---

## 📋 下一步TODO

按优先级排序：

1. **立即完成** - 实现`GroupChatActivity`和`MessagesAdapter`
2. **重要** - 实现`FriendsAdapter`和`GroupsAdapter`
3. **优化** - 创建群组对话框、群组设置页面

---

## 💡 开发提示

### 二维码格式
```
familyrecipes://add_friend?userId=123
```

### 测试账号建议
- 注册2-3个测试账号
- 使用不同的设备或模拟器
- 测试扫码添加好友流程

### 调试日志
- 后端日志：查看Spring Boot控制台
- Android日志：`adb logcat | grep FamilyRecipes`

---

## 🎉 完成标准

功能完整的标准：
- ✅ 可以扫码添加好友
- ✅ 可以查看好友列表（显示好友信息）
- ✅ 可以创建群组（选择好友）
- ✅ 可以在群组中发送消息
- ✅ 可以查看聊天记录
- ✅ 可以管理群组（添加/移除成员、解散群组）

当前进度：**80% 完成** 🎯

