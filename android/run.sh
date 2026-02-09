#!/bin/bash
# 快速启动 Android 应用的脚本

echo "=== 家庭菜谱 Android 应用启动脚本 ==="

# 检查是否在 android 目录
if [ ! -f "settings.gradle" ]; then
    echo "错误：请在 android 目录下运行此脚本"
    echo "cd android && ./run.sh"
    exit 1
fi

# 构建项目
echo "正在构建项目..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo ""
    echo "📱 接下来请："
    echo "1. 连接 Android 设备或启动模拟器"
    echo "2. 运行以下命令安装："
    echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "或者使用 Android Studio 点击 Run 按钮"
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi

