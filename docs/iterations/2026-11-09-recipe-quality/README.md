# 菜谱标签、家肴名称和外链打开

## 原因与修复

原来的 RecipeClassification.complete 仅补「待确认」，本地 classify_catalog.py 不回写已经导入的菜谱，因此重复导入不会补齐线上标签。

新增单机后台 RecipeTagBackfill：每次处理一个缺少有效口味或菜系的菜谱，使用现有 translation 文字模型，根据菜名、描述、食材和步骤生成受白名单约束的分类。保留来源/用户明确标签；不能推断时记为 needs_review。模型返回与证据、模型名、源内容摘要保存在 recipe_classification。每个任务最多尝试三次，失败间隔十分钟。保存编辑会重置任务，调用期间发生内容修改时丢弃旧结果。不会重建菜谱或影响收藏关系。

详情页隐藏无意义的「待确认」占位标签。名称统一为「家肴」，包括启动器名称、软件说明、官网、隐私说明以及电饭煲图标文字。

外链使用保留完整 URI 的 ACTION_VIEW 交由系统选择处理应用，不再按包名强行选择浏览器。外链标题和平台文字也可点击。YouTube 的视频 ID、时间点、播放列表参数原样保留。尚需在用户 vivo 手机上确认 YouTube App 的实际行为。

## 发布与存量数据补全

prepare-release.sh 和 deploy-current-server.sh 已加入可重复执行的 2026-11-09-recipe-classification.sql；先随新版后端执行迁移，勿手工清库或重新导入。

上线后，在现有 /etc/familyrecipes/ai.env 中添加：

```dotenv
RECIPE_CLASSIFICATION_ENABLED=true
RECIPE_CLASSIFICATION_POLL_MS=10000
```

然后 `systemctl restart familyrecipes`。沿用现有文字模型 URL/key/model，无需新的模型或 key。这会产生模型调用费用；默认关闭，本次开发未发起全量付费请求，也未修改线上数据。

查看进度（服务器 MySQL）：

```sql
SELECT status, COUNT(*) FROM recipe_classification GROUP BY status;
SELECT COUNT(*) AS missing_categories FROM recipe r
WHERE NOT EXISTS (SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='taste' AND TRIM(t.tag_value) NOT IN ('','待确认'))
   OR NOT EXISTS (SELECT 1 FROM recipe_tag t WHERE t.recipe_id=r.id AND t.tag_type='cuisine' AND TRIM(t.tag_value) NOT IN ('','待确认'));
```

管理员 GET /api/catalog/status 也返回 classification 状态。管理员 POST /api/catalog/classification/retry 可重新排队 failed/needs_review；先检查模型配置或人工补充信息，避免反复付费重试。关闭开关并重启即可暂停；已有标签保留。
