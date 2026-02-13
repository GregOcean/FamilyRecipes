#!/bin/bash

# 上传 APK 到服务器脚本

APK_PATH="android/app/build/outputs/apk/debug/app-debug.apk"
VERSION="v1.0.0-debug"
SERVER="root@152.42.242.92"
REMOTE_PATH="/opt/familyrecipes/apk/"

echo "📱 准备上传 APK..."

# 检查 APK 是否存在
if [ ! -f "$APK_PATH" ]; then
    echo "❌ 错误: APK 文件不存在: $APK_PATH"
    echo "请先在 Android Studio 中生成 APK："
    echo "  Build → Build Bundle(s) / APK(s) → Build APK(s)"
    exit 1
fi

# 获取 APK 文件大小
SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "📦 APK 大小: $SIZE"

# 重命名为带版本号的文件名
VERSIONED_APK="FamilyRecipes-${VERSION}.apk"

echo "🚀 上传到服务器..."

# 创建远程目录
ssh $SERVER "mkdir -p $REMOTE_PATH"

# 上传 APK
scp "$APK_PATH" "$SERVER:$REMOTE_PATH$VERSIONED_APK"

if [ $? -eq 0 ]; then
    echo "✅ APK 上传成功!"
    echo "📍 服务器路径: $REMOTE_PATH$VERSIONED_APK"
    echo "🌐 下载地址: https://api.familyrecipes.live/apk/$VERSIONED_APK"
    
    # 创建 latest 符号链接
    ssh $SERVER "cd $REMOTE_PATH && ln -sf $VERSIONED_APK FamilyRecipes-latest.apk"
    echo "🔗 最新版链接: https://api.familyrecipes.live/apk/FamilyRecipes-latest.apk"
else
    echo "❌ 上传失败"
    exit 1
fi

