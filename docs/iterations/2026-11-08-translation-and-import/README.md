# 双语内容、聊天翻译与公开菜谱导入

## 实现范围

- 服务端集中译文表：原文 SHA-256、目标语言、翻译引擎版本唯一定位译文；原文不被覆盖。
- 中文与英文内容按 `Accept-Language: zh / en` 展示。覆盖菜谱名称、介绍、步骤、食材名称/单位/加工方式、用量、库存备注、分类、外链标题/备注、图片说明、点菜/备菜与实践记录。
- 枚举/筛选键不翻译；标签通过独立展示字段 `display_value`，筛选选项通过 `value` / `name` 分离，避免翻译后筛选失效。
- 编辑菜谱、库存、自己创建的点菜/备菜读取 `X-Content-Original: true`。他人只读点菜/备菜使用展示译文。用户名、URL、ID、时间等不翻译。
- 原文为中文时生成英文；原文为英文时生成中文。当前仅做中英，混合语句按是否包含汉字选择源语言。
- 新建/修改响应触发补翻译；后台每 30 秒小批扫描内容表，覆盖已有内容与非 HTTP 写入。重启后可重新扫描，已有哈希缓存复用。API key 未配置不会调用模型。
- 单机 MySQL 持久化队列；失败退避，最多 5 次；管理员可以重试失败任务。进程中断的运行中任务恢复。聊天优先于批量菜谱翻译。
- 聊天只接受已有消息 ID，检查当前群成员权限；不提供任意文本/任意缓存哈希查询接口。发送、离线 outbox 和翻译相互独立。
- Android 保留原文，下方小号灰色译文。接收游标推进后，仍会独立查询已收到消息的异步译文；缓存按账号隔离，展示时检查语言。
- API 未配置/尚未完成/失败时回退原文。内容页面刷新或重新打开后获得新译文；聊天随正常同步自动补齐。

## 翻译服务

按最新决定，聊天和内容统一调用外部 LLM API，不部署本地神经翻译模型，不需要 GPU。模型质量、延迟和服务商协议仍需真实 API 配置后验收。

## 三种业务任务、两种模型配置

| 业务任务 | 模型配置 | 调用方式 |
| --- | --- | --- |
| 聊天、菜谱、食材翻译 | `translation`，默认 `glm-5.3-flash` | 文本 → 译文 |
| 小票识别 | `vision`，默认 `glm-4.6v-flash` | 小票图片 → 食材、数量、单位等结构化结果，一次端到端请求 |
| 物品图片识别 | 同一个 `vision` | 物品/包装图片 → 物品与可见日期等结构化结果，一次端到端请求 |

小票和物品图片只区分业务提示词与输出结构，不拆成 OCR 和 NER 两个模型/两次请求。小票已接通 Android 选图/拍照、后端一次模型识别、可编辑草稿和批量幂等入库；物品图片识别仍仅预留配置与调用层。

```dotenv
# 翻译和视觉可共享同一个智谱 key，也可分别设置各自 key。
ZHIPU_API_KEY=YOUR-KEY
VISION_LLM_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
VISION_LLM_MODEL=glm-4.6v-flash
VISION_REASONING_EFFORT=
```

`VISION_LLM_MODEL` 默认 `glm-4.6v-flash`，小票与物品图片共用，不自动回退到翻译模型。未来识别结果中，包装明确读出的到期日、按生产日期与保质期限算出的日期，以及模型估算应分开标注；看不清的字段留空。入库前用户可核对修正。

智谱官方模型说明确认 GLM-5.3-Flash 支持图像输入，因此两类配置也可以暂时填同一模型，再分别调优。它不能关闭思考；翻译采用 `reasoning_effort=low`，并检查输出截断，避免保存不完整译文。

参考：https://docs.z.ai/guides/vlm/glm-5.3-flash 、https://docs.bigmodel.cn/api-reference/模型-api/对话补全 。

## 后端配置与数据库升级

先以 MySQL 管理员执行增量 SQL，不清空任何已有数据：

```bash
mysql family_recipes < backend/src/main/resources/migrations/2026-11-08-translations.sql
```

生产服务器上将下面变量加入现有服务的环境配置文件；密钥不要写进仓库、Android 或聊天。此处 URL 为完整接口地址，服务商必须支持 `model` / `messages` 输入和 `choices[0].message.content` 输出的 chat-completions 协议。

```dotenv
TRANSLATION_ENABLED=true
TRANSLATION_LLM_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
TRANSLATION_LLM_MODEL=glm-5.3-flash
TRANSLATION_LLM_KEY=YOUR-KEY
TRANSLATION_VERSION=v1
TRANSLATION_POLL_MS=2000
CATALOG_ADMIN_USER_ID=YOUR-DATA-ADMIN-USER-ID
```

- 不启用时 `TRANSLATION_ENABLED=false`，原有功能继续工作。
- 更换模型、术语规则时递增 `TRANSLATION_VERSION`，避免误用旧译文。
- 开启后后台会为已有内容补翻译，产生 API 调用费用；当前每个文本字段独立缓存和请求，默认串行。大批量导入的全量双语生成需要时间。
- `CATALOG_ADMIN_USER_ID` 必须是实际注册的数据管理员账号 ID，默认 0 禁止导入。其登录 token 用于上传与导入。普通用户不能编辑其菜谱，可以复制为自己的版本。
- 发布新 JAR 并重启现有 `familyrecipes` 服务，Android 也需更新才能显示聊天译文/来源页。此次没有自动发布线上。
- 管理员可调用 `GET /api/catalog/status` 查看队列状态；`POST /api/catalog/translations/retry` 重试失败项。均需 bearer token，不对普通用户开放。

## 数据整理和线上导入

原始数据、图片、整理结果位于 Git 忽略目录 `data/seed-candidates/`，脚本保存在仓库。

```bash
python3 backend/scripts/seed/collect_wikibooks.py --delay 20
python3 backend/scripts/seed/normalize_catalog.py
python3 backend/scripts/seed/import_catalog.py
```

最后一条默认仅预览数量，不写线上。确认新后端与 SQL 已部署后执行：

```bash
python3 backend/scripts/seed/import_catalog.py --url https://api.familyrecipes.live --apply --limit 5
# 检查前五条，再执行全量（会跳过已经成功导入的来源条目）
python3 backend/scripts/seed/import_catalog.py --url https://api.familyrecipes.live --apply
```

脚本隐藏输入数据管理员的登录 token，也可使用 `CATALOG_ADMIN_TOKEN` 环境变量。不会通过模型或脚本直接远程操作数据库。上传图片走原有鉴权媒体接口；同一来源 ID 幂等，响应丢失后重试不重复创建。**不做不同来源/同名菜谱的语义去重。**

图片上传缓存按服务器和管理员账号隔离；如果服务器上的已上传孤立图片被清理，删除对应 `normalized/uploads-*.json` 后重新上传。原始文件和来源记录不受影响。

当前整理原则：
- UniTools 使用原有结构化英文食材/步骤，完整数量与说明保存在正文。
- HowToCook/Wikibooks 的自由文本食材完整保存在介绍中，不进行不可靠的自动食材 ID 猜配；尚不能用于准确库存匹配。
- 无法解析的 wiki 模板、缺少食材/操作章节等记录留在 `normalized/report.json` 待处理，不当成完整菜谱导入。
- 缺封面/过程图会记录缺口。首张可用照片作为展示封面，其余进相册；没有把任意图片冒充过程图。未获得明确可复用许可的图片不导入。
- 没有“热门”排序证据；也尚未满足华人菜谱占一半及每条至少一张过程图的原始目标。
- 来源、作者、原始 revision/历史链接、各张图片许可、整理与机器翻译说明保存于 `recipe_provenance`。设置 → 内容来源与授权统一展示逐条索引，菜谱正文不塞许可说明。

## 验收重点

1. 中文原文在英文下显示译文，中文下仍是原文；更新原文后旧译文不能覆盖新内容。
2. 英文显示模式下进入编辑，读到原文；保存不污染其他语言。
3. 无权阅读群消息的人不能通过翻译接口获取内容。
4. 原文发送成功不依赖翻译成功；断网的群列表和 outbox 保持可用。
5. 重复运行导入脚本不重复同一来源记录；管理员创建的公开菜谱，普通用户只能复制改编。
6. 接入真实 API 后重点检查过敏、否定词、用量、冷藏/冷冻、烹饪术语。使用真实服务商模型验收这些语义。

## 2026-11-08 少量真实 API 验证

使用本机忽略的密钥文件完成 4 次直接 API 调用：中英互译各一次、模拟小票和包装标签各一次。总用量 3,195 tokens。翻译耗时 1.85/2.11 秒，视觉耗时 14.29/11.78 秒；均正常结束。小票未打印保质期的条目返回 null，包装标签正确读出 2026-11-20 到期及冷藏要求。详细原文、译文和结构化结果见 `live-api-smoke-results.json`。这是合成样例，不代表模糊实拍效果或完整 App 联调已通过。

全量翻译及线上导入等待用户查看小规模测试结果并明确同意后再开启。本轮没有启动全量任务，没有写线上业务数据。

## 建议发布顺序（2026-11-08）

1. 备份当前数据库，执行本次所需增量迁移（包括翻译表、推荐浏览记录表）；不重建数据库。
2. 部署新后端与新版 APK，同时更新下载元数据。官网暖色页面和电饭煲标识随 JAR 发布；APK 时间继续来自实际 release.json。
3. 初始保持 `TRANSLATION_ENABLED=false`，先检查注册、登录、聊天、菜谱、上传和下载功能。
4. 用管理员账号导入 5 条，验收图文、来源、公开只读权限和复制编辑；确认后再全量导入。
5. 最后开启翻译队列，观察成功数、失败数、耗时和费用。开启翻译会扫描既有内容，并非只翻译刚导入的 5 条；少量效果验证继续使用受控样例请求。

先导入再后台翻译可以直接复用数据库翻译队列，保留原文且允许失败重试。无需把本地所有数据翻译完成才发布。尚未翻译完的内容暂时显示原文，不能视作全部英文已就绪。

小票 App 操作链路已补齐并本地验收，详见 `receipt-fast-mode.md`。本记录不表示已经发布、全量翻译或导入线上。

完整发版操作见 [发版 SOP](../../releases/2026-11-08-release-sop.md)。
