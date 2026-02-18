# 社交功能开发文档

## 项目概述

为家肴App添加面对面扫码加好友、群组管理和群聊功能。

---

## ✅ 已完成功能

### 1. 后端开发（100%完成）

#### 数据库设计
- ✅ `friendship` - 好友关系表（双向关系）
- ✅ `group_chat` - 群组表
- ✅ `group_member` - 群组成员表
- ✅ `message` - 消息表
- ✅ `user_read_position` - 用户读取位置表（未读消息）

#### 后端Model类
- ✅ `Friendship.java` - 好友关系实体
- ✅ `GroupChat.java` - 群组实体
- ✅ `GroupMember.java` - 群组成员实体
- ✅ `Message.java` - 消息实体
- ✅ `UserReadPosition.java` - 用户读取位置实体

#### Mapper层
- ✅ `FriendshipMapper.java` - 好友关系数据访问
- ✅ `GroupChatMapper.java` - 群组数据访问
- ✅ `MessageMapper.java` - 消息数据访问

#### Service层
- ✅ `FriendshipService.java` - 好友业务逻辑（添加/删除/列表）
- ✅ `GroupChatService.java` - 群组业务逻辑（创建/管理/解散）
- ✅ `MessageService.java` - 消息业务逻辑（发送/接收/已读）

#### Controller层
- ✅ `FriendshipController.java` - 好友API接口
- ✅ `GroupChatController.java` - 群组API接口
- ✅ `MessageController.java` - 消息API接口

### 2. Android前端开发（80%完成）

#### 核心功能
- ✅ **二维码功能**
  - `QRCodeUtil.kt` - 二维码生成和解析工具
  - `MyQRCodeActivity` - 我的名片码展示
  - `ScanQRActivity` - 扫码添加好友

- ✅ **UI界面**
  - 更新`ProfileFragment` - 添加社交功能入口
  - `FriendsListActivity` - 好友列表（基础框架）
  - `GroupsListActivity` - 群组列表（基础框架）

- ✅ **数据模型**
  - 添加社交相关Model类到`Models.kt`
  - 添加API接口到`ApiService.kt`

- ✅ **权限管理**
  - 相机权限（扫码）
  - 存储权限（保存二维码）

#### 依赖库
- ✅ ZXing - 二维码扫描和生成
  - `com.google.zxing:core:3.5.2`
  - `com.journeyapps:zxing-android-embedded:4.3.0`

---

## ⚠️ 待完成功能

### 1. 群聊界面（优先级：高）

需要实现：
- `GroupChatActivity` - 群聊主界面
  - 消息列表RecyclerView
  - 消息输入框
  - 发送按钮
  - 消息adapter和ViewHolder

- `activity_group_chat.xml` - 群聊布局
- `item_message.xml` - 消息item布局（左右两种）

### 2. Adapter实现（优先级：高）

需要完善：
- `FriendsAdapter` - 好友列表适配器
- `GroupsAdapter` - 群组列表适配器
- `MessagesAdapter` - 消息列表适配器

### 3. 群组管理对话框（优先级：中）

需要实现：
- 创建群组对话框
- 选择好友对话框
- 群组设置对话框（编辑群名、成员管理）

### 4. 优化功能（优先级：低）

- 实时消息推送（WebSocket）
- 消息分页加载
- 图片消息支持
- 群组头像上传
- 好友搜索
- 群组搜索

---

## 🚀 下一步开发建议

### 立即开始（必需）

1. **实现群聊Activity**
   ```kotlin
   // 创建 GroupChatActivity.kt
   // 创建 MessagesAdapter.kt
   // 创建消息布局文件
   ```

2. **完善Adapter**
   ```kotlin
   // 实现 FriendsAdapter
   // 实现 GroupsAdapter
   ```

### 短期目标（本周）

3. **测试基础流程**
   - 添加好友 → 创建群组 → 发送消息
   - 修复UI和交互问题

4. **群组管理对话框**
   - 创建群组选择好友
   - 群组设置页面

### 中期目标（下周）

5. **优化用户体验**
   - 加载状态提示
   - 错误处理
   - 空状态页面

6. **消息功能增强**
   - 消息时间显示
   - 消息状态（发送中/已发送/已读）
   - 长按消息菜单（复制/删除）

---

## 📝 API接口清单

### 好友相关
- `POST /api/friends/add` - 添加好友
- `GET /api/friends/list` - 获取好友列表
- `DELETE /api/friends/{friendId}` - 删除好友
- `PUT /api/friends/{friendId}/nickname` - 更新好友备注

### 群组相关
- `POST /api/groups/create` - 创建群组
- `GET /api/groups/list` - 获取群组列表
- `GET /api/groups/{groupId}` - 获取群组详情
- `POST /api/groups/{groupId}/members` - 添加群成员
- `DELETE /api/groups/{groupId}/members/{memberId}` - 移除群成员
- `POST /api/groups/{groupId}/leave` - 退出群组
- `PUT /api/groups/{groupId}` - 更新群组信息
- `DELETE /api/groups/{groupId}` - 解散群组

### 消息相关
- `POST /api/messages/send` - 发送消息
- `GET /api/messages/list` - 获取消息列表
- `POST /api/messages/read` - 标记已读
- `GET /api/messages/unread` - 获取未读数

---

## 🗂️ 文件结构

```
backend/
├── src/main/resources/migrations/
│   └── 005_create_social_tables.sql         ✅ 数据库表
├── src/main/java/com/familyrecipes/
│   ├── model/
│   │   ├── Friendship.java                  ✅
│   │   ├── GroupChat.java                   ✅
│   │   ├── GroupMember.java                 ✅
│   │   ├── Message.java                     ✅
│   │   └── UserReadPosition.java            ✅
│   ├── mapper/
│   │   ├── FriendshipMapper.java            ✅
│   │   ├── GroupChatMapper.java             ✅
│   │   └── MessageMapper.java               ✅
│   ├── service/
│   │   ├── FriendshipService.java           ✅
│   │   ├── GroupChatService.java            ✅
│   │   └── MessageService.java              ✅
│   └── controller/
│       ├── FriendshipController.java        ✅
│       ├── GroupChatController.java         ✅
│       └── MessageController.java           ✅

android/
├── app/src/main/java/com/familyrecipes/android/
│   ├── data/model/
│   │   └── Models.kt                        ✅ 添加社交Model
│   ├── data/remote/
│   │   └── ApiService.kt                    ✅ 添加社交API
│   ├── util/
│   │   └── QRCodeUtil.kt                    ✅ 二维码工具
│   └── ui/
│       ├── profile/
│       │   └── ProfileFragment.kt           ✅ 更新社交入口
│       └── social/
│           ├── MyQRCodeActivity.kt          ✅ 名片码
│           ├── ScanQRActivity.kt            ✅ 扫码
│           ├── FriendsListActivity.kt       ✅ 好友列表（待完善）
│           ├── GroupsListActivity.kt        ✅ 群组列表（待完善）
│           └── GroupChatActivity.kt         ⚠️ 待实现
└── app/src/main/res/
    ├── layout/
    │   ├── activity_my_qr_code.xml          ✅
    │   ├── activity_friends_list.xml        ✅
    │   ├── activity_groups_list.xml         ✅
    │   └── activity_group_chat.xml          ⚠️ 待创建
    └── drawable/
        ├── ic_qr_code.xml                   ✅
        ├── ic_scan.xml                      ✅
        ├── ic_friends.xml                   ✅
        └── ic_groups.xml                    ✅
```

---

## 💡 技术要点

### 二维码格式
```
familyrecipes://add_friend?userId=123
```

### 好友关系
- 双向关系（自动创建两条记录）
- 支持备注名

### 群组权限
- `owner` - 群主（唯一，可解散群）
- `admin` - 管理员（可添加/移除成员）
- `member` - 普通成员

### 消息类型
- `text` - 文本消息
- `image` - 图片消息
- `system` - 系统消息

---

## ⚙️ 数据库迁移

需要执行迁移脚本：
```sql
-- 位于 backend/src/main/resources/migrations/005_create_social_tables.sql
```

在MySQL中执行创建表结构。

---

## 🎯 当前状态总结

✅ **已完成（80%）**
- 后端API完整实现
- Android基础框架搭建
- 二维码功能完成
- 好友/群组列表基础实现

⚠️ **待完成（20%）**
- 群聊界面及消息功能
- RecyclerView Adapter实现
- 群组管理对话框

💪 **建议下一步**
立即实现`GroupChatActivity`和消息相关的Adapter，这样就能完成完整的用户流程测试。

