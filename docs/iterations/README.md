# 迭代记录

这里保存项目评审、设计决策、实施计划和验收记录。文档中的“计划”和“设计”不代表代码已经实现。

## 记录索引

| 日期 | 迭代 | 状态 | 内容 |
| --- | --- | --- | --- |
| 2026-11-01 | [单机基础可靠性](./2026-11-01-foundation-review/04-implementation.md) | 已实施，自动化验证通过；真机待验收 | 权限、数据完整性、本地媒体、聊天与构建；不迁移、不改版 |

当前实现请先读 [04 实施记录](./2026-11-01-foundation-review/04-implementation.md)。

历史设计阅读顺序：

1. [原始评审](./2026-11-01-foundation-review/01-review.md)：为什么要改，以及现有代码证据。
2. [详细设计](./2026-11-01-foundation-review/02-design.md)：改成什么、边界规则与兼容方案。
3. [实施与验收计划](./2026-11-01-foundation-review/03-plan.md)：依赖顺序、工作包和完成标准。

## 维护约定

- 每轮使用 `YYYY-MM-DD-topic/` 目录；评审保留当时基线，后续发现写入设计或实施记录。
- 状态使用：设计草案、待实施、实施中、已验收、已搁置；不以估算百分比代替验收。
- 每个实施项记录关联提交、验证命令／场景、结果和遗留问题；未执行的检查明确标为未执行。
- 业务语义变化先更新设计决策，再更新代码和测试；不要让历史评审充当最新接口文档。
- 文档使用仓库相对链接；评审中的行号对应记录的基线版本。
- 不记录密码、token、真实用户数据或生产数据库备份。


- [家庭厨房与图文收藏实现、验证及验收步骤](2026-11-01-foundation-review/05-family-kitchens.md)

- [邮箱验证码注册与本地邮件验收](2026-11-01-foundation-review/06-email-registration.md)

- [单人使用与默认厨房](2026-11-01-foundation-review/07-solo-kitchen.md)

- [库存分类与查找](2026-11-01-foundation-review/08-inventory-classification.md)

- [09 · 湖蓝界面与点菜、备菜流程](2026-11-01-foundation-review/09-ui-and-meal-workflows.md)

- [一级页面与设计稿对齐检查](2026-11-01-foundation-review/10-ui-design-alignment.md)
