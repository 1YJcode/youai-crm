# 大量录音存储与持久转写队列

## 存储方式

新录音不再写入 MySQL BLOB。默认使用 `file`，录音写入独立目录；也支持私有 S3 兼容对象存储。
MySQL 保存录音位置、大小、SHA-256、版本、上传时间，以及转写、人工确认、任务状态。仍限制每段录音 200MB、两小时。

现有数据库录音保持可读，升级不会自动搬走或删除业务音频。Flyway V37–V39 增加可空 BLOB、外部文件字段、持久任务、直传凭证和历史版本表。

### 默认文件模式

Compose 将 `${RECORDING_HOST_DIRECTORY:-./data/recordings}` 绑定到后端 `/data/recordings`。重建容器不会删除该目录。
直接启动 Java 时，用 `RECORDING_STORAGE_DIRECTORY` 指定绝对路径；默认相对当前进程目录的 `./data/recordings`。
目录不可公开到 nginx 静态目录，已从 Git 和 Docker 构建上下文排除。

新上传按流写临时磁盘，校验后原子落盘，避免每个上传分配 200MB 内存。读取文件使用 Resource 流式响应。
文件模式浏览器仍通过鉴权接口读取 Blob；大规模并发播放优先选择对象存储模式。
每个实例最多同时校验一个录音，繁忙返回 429；临时目录也要预留磁盘容量。

### 对象存储模式

在服务器环境变量中配置，不将密钥填入浏览器或提交 Git：

```dotenv
RECORDING_STORAGE_BACKEND=s3
RECORDING_S3_BUCKET=your-private-bucket
RECORDING_S3_ENDPOINT=https://your-s3-endpoint
RECORDING_S3_REGION=your-region
RECORDING_S3_ACCESS_KEY=your-access-key
RECORDING_S3_SECRET_KEY=your-secret-key
RECORDING_S3_PATH_STYLE=false
RECORDING_QUEUE_CONCURRENCY=1
```

省略 endpoint 可使用 SDK 的标准 S3 endpoint。兼容存储是否需要 path-style 以服务商为准；MinIO 通常设为 true。
也可留空静态密钥，使用 SDK 默认凭证链。服务器需具备指定桶的读取、写入和 staging 删除权限，无需创建桶权限。
同一个配置的 endpoint 必须同时能被服务器和用户浏览器访问；生产环境使用 HTTPS。
云存储账号、私有桶、CORS 和备份须在所选服务中实际配置后才能启用，此代码不自动创建云资源。
录音 key 使用同一个已配置的桶；已有 S3 录音时不能直接改桶名或 endpoint，须先完整迁移正式录音和历史文件并校验，保留原 key 后再切换。

桶保持私有，禁止匿名读取。为实际网页来源配置 CORS，允许 PUT、GET、HEAD，允许 `Content-Type` 等签名所需请求头。
不要使用任意网站来源。对象存储直传接口使用 AWS SDK 官方预签名机制：
https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html

流程：

1. `POST /api/recording-uploads`，提交 `customerId` 或替换时的 `callId`、`size`、`note`。检查当前客户权限。
2. S3 模式返回 15 分钟有效的 PUT 地址，签名限定大小和 Content-Type；文件模式返回 `direct:false`，前端使用现有 multipart 接口。
3. 浏览器只向对象存储 PUT 原始文件，不附带 CRM 登录令牌或云账号密钥。
4. `POST /api/recording-uploads/{id}/complete` 再检查上传者、有效期、客户权限，检查大小、实际格式和时长。
5. 服务端以流复制并校验，再写入全新的录音 key；数据库事务成功后才作为业务录音可见。临时 PUT 地址不能覆盖已保存音频。

完成校验仍需要服务器读回音频，不是完全绕过业务服务器的带宽。后续超大规模可将校验独立为后台服务。
PUT 失败或完成请求失败不创建空通话。完成凭证不可重复使用，网络异常后可重新打开通话列表确认是否已成功。

`GET /api/calls/{id}/recording/playback` 保留兼容响应，返回 null，前端统一使用 CRM 鉴权接口读取。
根据客户资料隐私规则，S3 模式同样通过后端检查当前客户归属后流式返回音频，不再签发可绕过登录的播放 URL。
上传仍可直传 S3；播放带宽会经过业务服务器。

## 队列与重试

当前页面的本地/云端双转写使用数据库队列。用户提交时，录音版本、转写方式、请求账号、账号 ID 和任务一并持久保存。
云端仍必须显式提交 `cloudConsent:true`；不会从本地失败后自动切到云端。

- `QUEUED`：等待处理，页面持续轮询。
- `PROCESSING`：正在转写。
- `COMPLETED` / `FAILED`：完成或失败。
- 替换录音后尚未开始的旧任务取消，旧任务结果不能覆盖新录音。

默认并发 1，可用 `RECORDING_QUEUE_CONCURRENCY` 设为 1–4；转写适配器仍需载入一段音频，内存较小时保持 1。
每个工作进程通过数据库条件更新领取任务，每 20 秒续租，两分钟未续租后可被恢复。
失败最多尝试 3 次，等待 30 / 60 秒重试，最终失败可人工重试。账号停用、删除、身份变化或失去客户权限后拒绝继续处理。
外部转写调用和本地数据库无法形成同一事务，因此中断恢复可能重复调用供应商、产生重复计费；结果写入受任务令牌和录音版本保护。

旧 `/recording/analysis` 兼容接口仍保留原线程处理方式，只用于旧调用方；新的双转写入口已经改为上述持久队列。
转写完成后的知识库 AI 总结仍是附加步骤，尚未独立持久排队，进程在该步骤被终止可能留下待分析状态。

## 历史版本与迁移

通过上传接口替换录音时，将旧音频位置、旧转写、本地/云端结果、AI 总结和人工确认快照写入历史表。
录音弹窗展开“历史录音与转写”可分页查看并回听。历史访问仍使用客户当前权限。
快照保留替换时已经保存的内容；替换时尚在转写的旧任务不再更新历史快照。

管理员可调用：

```http
POST /api/recording-storage/migrate?limit=5
Authorization: Bearer <admin token>
```

每次最多 20 段，把数据库 BLOB 或其他当前存储方式的录音复制到配置的目标存储。
按大小和 SHA-256 校验成功后，以录音版本为条件切换位置，再清空 BLOB；保留录音版本、转写和审核内容。
重复执行会跳过已迁移录音。失败时不清空原数据库音频；不确定数据库提交结果时保留文件，避免误删正在使用的音频。
文件模式切到 S3 后，保留原目录以供尚未迁移的录音和历史版本读取。此批次迁移接口只迁移当前版本，历史版本仍按原位置读取。

## 保留与备份

正式录音、历史版本和文字默认长期保留，不设置自动删除期限。确定业务保留时间后再配置归档/删除规则。
staging `uploads/` 文件在凭证过期一天后由定时任务分批清理，每小时最多 100 个；大量上传可缩短 cleanup-ms 或在桶上为 `uploads/` 配置一天到期规则。
不要把 staging 到期规则应用到正式的 `recordings/` 前缀。

备份必须同时覆盖 MySQL 和音频目录/对象存储，二者缺一不可。建议开启桶版本控制和独立备份，并定期做恢复演练。
需要一致性快照时暂停上传、替换和迁移后备份数据库及文件；恢复到较早数据库版本时仍需保留其引用的旧文件。
不要在已有外部录音后直接回滚到只支持 BLOB 的旧镜像；需回迁音频或用匹配的旧数据库和文件快照恢复。
意外中断可能留下没有数据库引用的正式 key，当前不自动清理这些文件，避免误删；需要做引用核对后再处理。

## 验证

新增 `RecordingStorageQueueIntegrationTest`：独立文件存储、版本保留、鉴权、迁移、重复提交、过期租约恢复、失败重试、替换取消。
`RecordingS3IntegrationTest` 通过 `S3_TEST_ENDPOINT` 显式启用，在隔离 MinIO 上验证真实签名 PUT、私有读取、短时播放、临时文件复写隔离、损坏文件不创建通话。
MySQL 验证应使用独立临时库、Flyway 和 Hibernate validate，构建到新目录以免既有 target 中残留旧迁移。

2026-10-25 验证记录（上海时间）：

- 文件存储及相关后端回归：25 项通过；S3 专项在无测试 endpoint 时显式跳过。
- 隔离 MySQL 8.4 + MinIO：7 项通过，Flyway V37/V38/V39 成功，Hibernate validate 通过。报告位于 `.tmp_storage_verification_fb034cdab4e245c999ea674329d187d1/`。
- 无头 Edge + 隔离后端：文件模式和真实 MinIO 直传模式均通过登录、上传、播放、持久队列、重开读取、替换、历史回听、客户页上传流程。最后一轮浏览器与队列共 6 项通过，日志 `.tmp_storage_s3_browser_tests.log`、`.tmp_storage_browser.log`。
- 录音弹窗、客户上传的浏览器交互回归以及 API 传输检查通过，JavaScript 语法及 diff 空白检查通过。
- 转写供应商使用测试替身，未调用真实 AI。未迁移业务录音，未更新运行中的业务容器；临时数据库和对象存储测试容器已清理。

可重复运行 `scripts/test-recording-storage.ps1`；加 `-MySql -S3` 使用隔离 MySQL 和 MinIO。该脚本使用全新构建目录并清除继承的测试数据库目标，结束后恢复环境和删除临时容器。浏览器验证通过 `RUN_RECORDING_STORAGE_BROWSER=true` 单独启用 `RecordingStorageBrowserTest`，需 Node/Playwright/Edge；可选 `S3_BROWSER_TEST_ENDPOINT` 必须指向独立测试 MinIO。

## 业务部署（2026-10-25 09:17，上海时间）

- 已更新运行中的前后端，保持前端 4173、后端 8500、原 MySQL 容器、原数据卷、运行资源限制与 AI 配置。
- Flyway V36–V39 已执行，健康检查直连及前端代理均为 UP。启用 file 模式与持久转写队列，默认并发 1。
- 宿主机 `E:/Users/XY-Think/Desktop/youai_project/data/recordings` 绑定至后端 `/data/recordings`，新音频不写入 MySQL BLOB。
- 已迁移现有 2 段录音，共 45,857,056 字节；鉴权下载迁移前后 SHA-256 完全相同。录音版本、转写状态和全文哈希保持一致。
- 客户 57、通话 6、录音 2、账号 7，数量保持不变。当前任务队列为空，未触发实际 AI 调用。
- 已在真实浏览器完成登录、通话列表、既有录音播放、历史入口与双转写控件只读验收；无页面脚本错误，未登录接口返回 401。另以空文件确认上传拒绝逻辑，没有向业务记录写入测试录音。
- 部署前保留旧镜像 `before-recording-storage-20261025-091336`，短暂停止后端后保存一致性数据库备份。迁移后另存数据库及录音压缩备份，均在被 Git 忽略的 `.tmp_deploy_recording_storage_20261025_091336/`。部署报告为 `deployment-report.json`，浏览器报告为 `browser-report.json`。
- 已有录音切到外部文件，若回滚旧代码必须同时恢复匹配的数据库和文件快照；不可只切回旧镜像。备份和录音目录均含业务数据，不提交 Git。
