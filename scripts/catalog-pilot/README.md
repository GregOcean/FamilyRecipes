# 菜谱媒体候选小批量任务

新增网页管理入口见 [后台使用说明](../../docs/iterations/2026-11-15-admin-and-password-reset/README.md)。下文为独立CLI试点；核心worker现位于 `backend/src/main/resources/catalog/worker.py`，随JAR打包，CLI入口兼容保留。

当前策略：rules-v2，2026-11-15。只读取已导入、公开的菜谱；不修改业务数据库，不下载视频，不调用聊天/翻译 LLM API。搜索 API 仍可能按次收费。

## 选菜

- 人工常见菜清单优先，然后参考已有中餐菜系、家常菜标签；不是网络热度榜。
- 蔬菜、蛋豆、肉类、鱼虾、汤羹、主食轮流分配名额，最多 30 道。不足时重新分配，不强行凑数。
- 分组是检索任务的启发式分类，不写回菜谱标签。
- 精确菜名/已有别名去除同批重复检索；不删除或合并原菜谱。
- 使用现有中英文菜名及常见菜双语词典。缺少对应语言的名称则记录缺口，不花 token 翻译。

## 检索与结果

先查 B站，规则未选中再查小红书；海外查 YouTube。每次最多 5 个候选，标题匹配优先于摘要，排名用于排序；排除吃播、探店、广告等。只选择具体内容链接，不校验视频本体或播放状态。结果为“相关做法”，不保证配方完全一致。

有对应地区教程即跳过检索；有封面或相册图即跳过补图。图片查询 Commons，白名单许可与尺寸筛选后保留作者、来源、许可；画面及菜谱匹配仍待检查，不自动上线。

搜索调用累计最多 120 次（含旧任务的搜索调用），命中本地缓存不计新调用。单并发、请求间隔 2 秒，单请求 120 秒超时。鉴权、HTTP 限流等 provider 错误停止整批；其他失败每道最多 3 次，人工重新启动可重试。启动不会创建定时任务。

## 启动与升级

在 Mac 项目目录：

```bash
bash scripts/start-catalog-pilot.sh
```

输入 SSH root 密码。安装脚本先停止旧任务，备份进度到服务器状态目录的 `backup-*`，替换脚本后启动。保留已完成结果和请求缓存；旧失败重置重试次数一次。新建 `recipes-balanced-v2.json` 均衡名单，旧名单不删除；已完成的菜谱不会重复处理。

服务器：

```bash
journalctl -u familyrecipes-catalog-pilot -f
systemctl stop familyrecipes-catalog-pilot
systemctl start familyrecipes-catalog-pilot
```

按 Ctrl+C 只退出日志跟踪，不会停止任务。

结果：`/var/lib/familyrecipes-catalog-pilot/results.json`。其中保留旧批次结果，当前批次名单以 `recipes-balanced-v2.json` 为准。`requests_by_kind` 含历史 LLM 次数，不代表规则版进行了新的 LLM 调用。

复用 `/etc/familyrecipes/ai.env` 的 `CATALOG_SEARCH_KEY`、`ZHIPU_API_KEY` 或 `TRANSLATION_LLM_KEY`；可在 `/etc/familyrecipes/catalog-pilot.env` 覆盖搜索配置。不要把密钥放入脚本或版本库。

## 本地验证

```bash
python3 -m unittest discover -s scripts/catalog-pilot -p 'test_*.py'
```

测试使用模拟 API，不产生真实搜索费用。线上需查看运行结果确认搜索权限及候选质量。
