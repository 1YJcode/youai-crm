# 浏览器电话与短信通信网关

系统使用浏览器麦克风和耳机进行 WebRTC 通话。客户手机号由后端读取。电话网关负责 WebRTC 音频媒体、SIP/PSTN 路由、真实外显号码与呼入号码，并将服务商能力适配到下述 HTTP 协议。选择服务商后需要完成适配和真实号码联调。

## 配置

```dotenv
TELEPHONY_GATEWAY_ENABLED=true
TELEPHONY_GATEWAY_ENDPOINT=https://your-gateway.example/calls
TELEPHONY_GATEWAY_TOKEN=server-side-private-token
TELEPHONY_CALLER_ID=your-enabled-caller-number
COMMUNICATION_WEBHOOK_TOKEN=independent-long-random-event-secret
```

短信发送配置见 [短信网关说明](sms-gateway.md)。电话和短信默认关闭。修改 `.env` 后执行 `docker compose up -d --no-build --no-deps backend` 让已部署镜像加载配置。

远程浏览器访问需要 HTTPS，`localhost` 可用于本机测试。WebRTC 的音频媒体需要网关提供可达的 ICE/STUN/TURN 服务及相应网络端口。CRM 的 HTTP 代理只承载控制接口。

## 用户操作

客户详情“拨打”和右上角电话入口均可发起呼叫。右上角支持按客户手机号或客户编号查找，沿用客户权限。同一坐席同时允许一通正在建立或已接通的通话。

首次通话请求浏览器麦克风权限。通话面板支持静音、挂断及音频播放授权。后台每 3 秒检查分配给当前坐席的来电，提供接听和拒接。来电按号码唯一匹配客户，并检查坐席对客户的权限。无法匹配或无客户权限时显示“未匹配客户”，保留呼入记录和路由到该坐席的来电号码。

关窗、退出账号、切换账号和页面离开会释放麦克风并尝试挂断。活动通话每 2 秒查询状态并向后端证明浏览器仍在线。超过 90 秒无浏览器活动的未拨出通话会取消，已提交的通话会请求网关挂断。

## CRM 控制接口

以下接口使用现有登录凭证。

| 接口 | 用途 |
| --- | --- |
| `GET /api/telephony/browser-config` | 获取当前坐席的 WebRTC 配置 |
| `GET /api/telephony/customer?phone=...` 或 `?customerId=...` | 按权限查找呼叫目标 |
| `POST /api/telephony/calls` | 提交外呼队列，JSON 含 `customerId`、客户端 UUID `requestId` 和 `offerSdp` |
| `GET /api/telephony/calls/{id}` | 获取通话状态、SDP answer 和挂断请求状态 |
| `POST /api/telephony/calls/{id}/answer` | 接听分配给当前坐席的来电，JSON 含 `offerSdp` |
| `POST /api/telephony/calls/{id}/hangup` | 取消、挂断或拒接 |
| `GET /api/telephony/incoming` | 获取当前坐席的振铃来电 |

客户端外呼重复请求使用相同 UUID 和 SDP。后端返回同一通话，不会重复外呼。通话媒体接口限当前坐席和管理员访问，普通通话列表不返回 SDP。浏览器使用非 trickle ICE，在 ICE 收集完成后提交 SDP offer。结束通话后数据库清除 SDP。

## 网关 WebRTC 配置

后端请求 `GET <endpoint>?action=browser-config&agentUserId=<CRM用户ID>`，携带 `Authorization: Bearer <TELEPHONY_GATEWAY_TOKEN>`。

```json
{"mode":"WEBRTC","iceServers":[{"urls":"turn:turn.example.com:3478","username":"short-lived-user","credential":"short-lived-password"}]}
```

网关根据 CRM 用户 ID 区分坐席，返回短期 ICE 凭证。配置响应仅将 `mode`、`iceServers.urls/username/credential` 交给浏览器。服务商主密钥和网关 Bearer 凭证保留在服务端。

## 网关命令

后端向配置的 endpoint 发送 JSON `POST`，携带相同 Bearer 凭证。

```json
{"action":"DIAL","requestId":"call-123","callId":null,"agentUserId":7,"phone":"+8613800138000","callerId":"configured-number","offerSdp":"v=0..."}
```

`action` 支持 `DIAL`、`ANSWER`、`HANGUP`。`ANSWER` 携带已存在的服务商 `callId` 和浏览器 `offerSdp`。`HANGUP` 无 SDP，也用于拒接。网关必须持久化幂等结果，`DIAL/ANSWER` 的 `Idempotency-Key` 为 `call-123`，`HANGUP` 为 `hangup-123`。

```json
{"state":"ACCEPTED","callId":"provider-call-123","sequence":1,"durationSeconds":0,"answerSdp":"v=0..."}
```

状态支持 `ACCEPTED`、`RINGING`、`CONNECTED`、`ENDED`、`FAILED`、`UNKNOWN`。响应使用 HTTP 2xx。`answerSdp` 必须是非 trickle ICE 的 WebRTC answer，可在后续查询/事件中返回，但浏览器只有收到 answer 后才能建立音频。`CONNECTED` 表示真实通话已接通。

每通电话的 `sequence` 是严格递增的非负整数，同一事件重试保持相同序号。呼入来电事件与接听命令、后续状态查询共享同一序列。挂断命令受理后需持续报告通话最终状态。

## 网关状态查询

后端请求 `GET <endpoint>?requestId=call-123&callId=provider-call-123`。已有服务商编号时包含 `callId`，网关应优先用它查询呼入通话。响应与命令响应格式相同。查询不得重新拨号。电话网关应保留按请求编号和服务商编号查找通话的能力。

后台每 2 秒运行一轮队列，活动通话至少间隔 10 秒补查一次。HTTP 请求超时为 15 秒，响应上限为 64 KiB。连接超时或响应不确定时进入 `UNKNOWN`，只查询原通话。服务重启后超过 2 分钟未确认的提交进入 `UNKNOWN`。正常电话任务和短信任务使用独立于其他 CRM 定时任务的调度线程。

## 来电与通话事件

网关向 `POST /api/communications/events/calls` 推送 JSON，携带独立的 `X-Communication-Token: <COMMUNICATION_WEBHOOK_TOKEN>`。该接口不使用用户 JWT。

```json
{"callId":"provider-inbound-123","direction":"INBOUND","state":"RINGING","sequence":1,"from":"+8613800138000","agentUserId":7,"durationSeconds":0}
```

呼入必须提供已启用的 CRM 坐席 ID，号码可回拨与坐席路由需要在服务商侧开通。系统根据 `agentUserId` 向对应用户显示来电，不会向全员广播。

呼出事件使用 `direction=OUTBOUND` 并携带 `requestId=call-123`。事件可附带 `answerSdp`。成功返回 HTTP 204，重复或较旧事件不会回退状态。终态保留，已确认接通不会被振铃事件覆盖。数据库并发冲突返回 HTTP 409，网关应以原事件重试。

## 客户短信回复

网关向 `POST /api/communications/events/sms` 推送 JSON，使用相同独立事件凭证。

```json
{"messageId":"provider-inbound-sms-123","from":"+8613800138000","content":"客户回复内容"}
```

按号码唯一匹配客户后写入其会话，消息标记为 `INBOUND` 和未读。相同客户和网关消息编号重复推送不会增加消息数。未找到唯一客户时返回 HTTP 409，网关需要保留该来信待处理。短信通道本身需要具备接收客户回复的能力。

## 验证范围

`TelephonyGatewayClientTest` 验证 HTTP 鉴权、WebRTC 配置过滤、拨号/接听/挂断命令及响应大小限制。`TelephonyIntegrationTest` 覆盖并发去重、忙线保护、回调鉴权、状态顺序、呼入路由、短信回复、浏览器超时与客户权限。

运行 `node scripts/test-browser-telephony.cjs` 使用隐藏 Edge 浏览器和模拟麦克风，建立真实 WebRTC 本机音频回环，验证静音、挂断、来电接听与麦克风释放。可用 `BROWSER_EXE` 指定 Chromium 浏览器路径。测试网关不会发起真实手机通话或真实短信。
