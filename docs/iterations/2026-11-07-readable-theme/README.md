# 1.0.3 可读性修复

用户真机截图显示菜谱详情和食材录入混用深色背景、固定深蓝文字，提示卡白字不可读，输入框标题与多行示例叠加。

根因与处理：
- 原 Material3 DayNight 主题跟随系统夜间模式，但自定义色未形成完整深色配色。改为 Material3 Light，Application 设置 MODE_NIGHT_NO，主题 forceDarkAllowed=false；当前版本仅支持浅色主题。
- 明确背景、surface、正文、次要文字、输入提示与主色按钮白字。
- XML 输入控件统一深蓝文字、深色提示及 medium 字重；动态 Ui.field 同步加粗。
- 食材提示卡改深蓝字；浮动标签固定收起，多行示例移到输入框下方 helperText，不再用子输入框 hint 与父标签叠加。
- versionCode 4、versionName 1.0.3，沿用正式签名，仅更新 Android，不需要后端变更。

构建与单元测试通过。真机 vivo 强制深色兼容性仍需复验；不能由模拟器结果保证所有厂商强制变暗行为。
