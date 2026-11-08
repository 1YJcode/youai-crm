# 可配置短信网关

客户详情消息弹窗和消息中心发送短信时，使用现有会话接口并指定 `channel=SMS`。客户手机号由服务端从客户资料读取，调用者不能覆盖接收号码。历史消息和兼容调用的站内记录标记为 `LOCAL`。

## 部署配置

在项目 `.env` 中设置以下变量，然后执行 `docker compose up -d --no-build --no-deps backend`。

```dotenv
SMS_GATEWAY_ENABLED=true
SMS_GATEWAY_ENDPOINT=https://your-gateway.example/sms
SMS_GATEWAY_TOKEN=replace-with-gateway-secret
SMS_GATEWAY_SENDER=your-sender-number-or-device-id
```

默认关闭通道。关闭时短信请求返回 HTTP 503，消息弹窗显示“短信通道未配置”。密钥仅在后端使用。网关地址不带查询参数，远程通道使用 HTTPS。容器访问宿主机网关可以使用 `http://host.docker.internal:端口/sms`。

真实短信服务商或手机 SIM 设备需要提供符合以下协议的适配网关，将请求映射到其发送接口、签名或模板。`sender` 是可选的发送号码或设备标识，由网关解释。仅填写手机号还需要有对应的发送设备或服务账号。

## CRM 提交接口

`POST /api/conversations/{conversationId}/messages`，使用现有登录凭证。

```json
{"content":"您好，请问现在方便沟通吗？","channel":"SMS","requestId":"客户端生成的UUID"}
```

首次成功提交返回 HTTP 201 和 `deliveryStatus=QUEUED`。相同会话、相同请求编号与内容重复提交返回同一条记录，内容不同返回 HTTP 409。手机号支持大陆 11 位号码及带 `+` 国家区号的国际号码。大陆号码统一转换为 `+86` 格式。

`GET /api/conversations/sms-status` 返回 `{"configured":false}` 或 `true`。

`GET /api/conversations/{conversationId}/messages` 返回消息及 `deliveryStatus`、`deliveryError`，沿用原有客户访问权限。响应不包含真实接收号码与网关密钥。

## 网关发送协议

后端向配置的地址发送 `POST`。

```http
Authorization: Bearer <SMS_GATEWAY_TOKEN>
Idempotency-Key: sms-123
Content-Type: application/json
```

```json
{"requestId":"sms-123","phone":"+8613800138000","content":"您好，请问现在方便沟通吗？","sender":"device-1"}
```

网关必须持久化 `requestId` 的幂等结果，重复请求返回同一短信记录。请求编号来自数据库消息 ID，客户端编号不会直接传给网关。

响应使用 HTTP 2xx 和以下 JSON。

```json
{"status":"ACCEPTED","messageId":"provider-message-123"}
```

支持的状态为 `ACCEPTED`、`DELIVERED`、`FAILED`、`UNKNOWN`。`DELIVERED` 需要运营商或设备的送达回执。仅完成排队时返回 `ACCEPTED`。`FAILED` 表示明确的最终失败。HTTP 错误、连接异常、超时与格式错误会进入 `UNKNOWN`。

## 网关状态查询协议

后端向相同地址发送 `GET ?requestId=sms-123`，携带相同 Bearer 凭证。响应格式与发送接口相同。网关应根据请求编号查询原短信，查询操作不会再次发送短信。

已受理或结果不确定的记录，后端每隔至少 30 秒查询状态，每轮最多处理 20 条。请求超时为 15 秒。网关应快速返回查询结果。

## 队列和恢复

消息与队列状态在同一数据库事务内保存，后台任务每 5 秒领取最多 20 条待发送记录。领取使用数据库条件更新防止多实例重复发送。短信发送发生在事务提交后。

状态流程为 `QUEUED → SENDING → ACCEPTED → DELIVERED`。明确失败进入 `FAILED`。不确定的结果进入 `UNKNOWN`。进程中断后，超过 2 分钟的 `SENDING` 记录转入 `UNKNOWN` 并查询原请求。后端不会自动重发 `UNKNOWN` 或 `FAILED`。最终状态不会被过期查询覆盖。

通道关闭时队列暂停，重新启用后恢复处理 `QUEUED`，其余状态通过查询确认。真实送达后更新客户最近联系时间。

## 验证

`SmsGatewayClientTest` 使用本机模拟 HTTP 网关验证鉴权、幂等编号、号码规范化、发送及查询协议。`SmsDeliveryIntegrationTest` 覆盖未配置拒绝、幂等、并发、队列状态、超时与重启恢复。测试不会发送真实短信。
