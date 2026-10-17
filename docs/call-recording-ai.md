# 通话录音与 AI 整理

## 当前状态

按用户 2026-10-17 的选择，先完成本地功能，服务商暂未确定。AI 配置默认留空，不自动发送录音。电话平台尚未开通，自动获取电话录音仍未实现；不能宣称完整目标已经完成。

## 已实现

- 通话列表的“录音管理”弹窗：上传、鉴权播放、分析状态、原始转写、AI 草稿及人工编辑确认。
- WAV、MP3、FLAC、OGG，单文件上限 20MB；WAV 解析和数据长度检查，其他格式目前只检查文件头。浏览器能否播放其他编码取决于浏览器支持。
- 音频保存在 MySQL，跟随数据库持久化与备份。迁移为 V28（录音）、V29（分析）、V30（人工确认）。重新上传替换该通话录音并清空原分析与审核内容。
- 上传、读取、分析和审核均检查客户当前负责人权限；管理员可访问。客户转移后旧负责人失去访问权限。
- 两个后台分析线程，无等待队列；繁忙时返回 429。状态持久化，超过十分钟的中断任务允许手动重试。外部请求不占用数据库事务。
- 录音版本和分析启动时间防止旧任务覆盖新录音；审核版本防止旧编辑窗口覆盖新确认内容。重新分析清除之前的审核状态。
- 原始转写、AI 草稿和人工确认结果分别保存，确认记录包含账号和时间。不会自动创建跟进任务或发送客户消息。
- 结果按纯文本展示；音频与分析响应禁止缓存，关闭弹窗或退出/切换账号释放播放器资源。

## 本地使用

1. Docker 前后端已于 2026-10-17 更新，数据库已执行至 V30。访问 `http://127.0.0.1:4173/`。
2. 进入通话记录，点击“录音管理”，选择已获授权、与当前客户对应的录音并上传。
3. 可以播放核对；未配置 AI 时显示“管理员尚未配置AI服务”，不会伪造结果。
4. 后续服务配置完成后，勾选该次外部处理确认并开始分析。完成后核对原文，编辑总结并“确认保存”。

## 可选 AI 配置

当前适配器只支持 OpenAI 官方接口，不代表用户已选定该服务。服务端环境变量（`.env.example` 默认留空）：

- `RECORDING_AI_API_KEY`
- `RECORDING_AI_TRANSCRIPTION_MODEL`：需支持 JSON 转写的模型。
- `RECORDING_AI_SUMMARY_MODEL`：需支持 Responses 文本输出的模型。

流程为 `/v1/audio/transcriptions` 转写，再用 `/v1/responses` 整理。Responses 设置 `store=false`；不将此参数解释为供应商零保留承诺。请求有超时和响应大小限制，禁止跟随重定向。密钥仅在后端读取。

摘要要求区分客户需求、异议、销售承诺、约定事项与建议；事实附原话依据，未提及明确标注，不猜测说话人身份。当前未实现说话人分离和时间戳分段。

接口依据：
- https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create
- https://developers.openai.com/api/reference/python/resources/responses/methods/create

## 验证证据

- 2026-10-17：14 项后端相关测试通过，覆盖上传、损坏 WAV、越权及客户转移、协议适配、分析完成/失败重试、重复任务、录音替换隔离、人工确认并发保护。使用 H2 和模拟 AI。
- 2026-10-16：独立 MySQL 8.4 容器中 6 项集成测试通过，Flyway 至 V30、Hibernate validate。报告 `.tmp_recording_mysql_2b6328682e334bf08751699f3bef8f74/`；临时容器清理，未连接业务库。
- `scripts/test-recording-browser.cjs`：真实无头 Edge，模拟 API，验证上传交互、WAV 实际播放、处理确认、轮询、防注入、保存重开、账号清理。截图 `.tmp_recording_browser/review.png` 已查看。
- `RecordingBrowserE2ETest` + `scripts/test-recording-e2e.cjs`：真实浏览器、JWT 登录、随机端口 Spring 后端、独立 H2、真实上传/读取/分析调度/审核接口全部通过；仅 AI 客户端 mock。现已使用完整 index.html 和 app.js，从登录页经“呼叫中心 → 通话记录”进入录音管理；完成上传、播放、分析、审核保存与重开。测试不覆盖其他无关页面。日志 `.tmp_recording_e2e.log`。
- `scripts/test-recording-transport.cjs`：multipart 边界、鉴权、Blob 及原有 JSON 请求通过；JavaScript 语法和 diff 空白检查通过。

浏览器端到端测试需提供 Playwright 包位置 `NODE_PATH`，安装 Edge，并设置 `RUN_RECORDING_BROWSER=true` 后运行 Maven 的 `RecordingBrowserE2ETest`。常规测试默认不启动浏览器。Windows 如 Mockito 动态 attach 失败，可通过 `argLine` 显式加载本机 Byte Buddy agent。

## 尚待接入与验收

- 用户确定服务商并配置账号后，用授权测试录音验证真实转写、总结质量及成本；目前没有真实 AI 调用。
- 电话平台选定后，按其协议实现认证/验签、外部通话 ID 去重、客户关联及录音导入。目前没有自动录音同步或真实拨号能力。
- MP3、FLAC、OGG 完整解码校验及目标部署环境验收。
- 若需要自动生成跟进记录或任务，应单独提供员工确认操作，目前仅保存录音审核结果。

2026-10-17 补充：移除无效的 AI 预览开关，录音质检视图复用录音管理入口，导出不再将所有通话标成暂无录音。完整主应用端到端测试通过；AI 服务仍为测试 mock，生产配置保持未填写。

## Docker 部署记录（2026-10-17）

已更新现有 `youai_project-backend-1`、`youai_project-frontend-1`，保留原 MySQL 容器、数据卷和 dev 运行配置。前端 4173、后端 8500。健康接口返回 UP，V28/V29/V30 迁移成功，部署前后客户均为 57 条。录音脚本返回 200，未登录读取分析接口返回 401，AI 三个配置项仍为空。

数据库备份及部署报告位于 `.tmp_deploy_recording_20261017/`（包含业务数据，已被 Git 忽略）；切换前备份为 `database-pre-switch.sql`。旧镜像保留 `before-recording-20261017` 标签。此次仅执行部署冒烟验证，没有向业务客户上传测试录音或触发外部AI。真实AI、自动电话录音接入仍待服务商确定。
