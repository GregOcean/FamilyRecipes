#!/bin/bash

# 脚本：将文本推送到Android模拟器剪贴板
# 使用方法: ./push_text_to_emulator.sh

# 确保adb在PATH中
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"

# 测试链接
TEXT="这种包子是泡打粉发的面吗？ 自家做的包子，面皮永远... http://xhslink.com/o/42iB8crbRgI 
复制后打开【小红书】查看笔记！"

echo "正在推送文本到模拟器剪贴板..."

# 方法1: 使用am broadcast（适用于大多数Android版本）
adb shell "am broadcast -a clipper.set -e text '$TEXT'" 2>/dev/null

# 方法2: 使用service call（更通用）
adb shell "service call clipboard 1 i32 0 s16 'text' s16 '$TEXT'" 2>/dev/null

echo "完成！现在在模拟器中长按输入框应该能看到粘贴选项了。"
echo ""
echo "如果还是不行，可以手动在模拟器的Extended Controls > Clipboard中粘贴文本。"

