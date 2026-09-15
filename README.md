# 优爱 CRM

一个根据参考界面实现的客户管理系统，包含静态前端和 Java/Spring Boot 后端。客户、任务、订单、通话记录和客户消息均通过 API 持久化到 MySQL。

## 本地运行

1. 在小皮面板中启动 MySQL 5.7。
2. 初始化数据库：`powershell -ExecutionPolicy Bypass -File .\scripts\setup-mysql.ps1`
3. 启动后端：`powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1`
4. 启动前端：`powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend.ps1`
5. 打开 `http://127.0.0.1:4173/`。

`run-frontend.ps1` 会优先使用 Python；如果系统没有 Python，会自动使用项目内置的 PowerShell 静态服务器。

首次进入会显示登录页。演示账号：

- 管理员：`admin` / `Admin@123`
- 销售账号：`linxi` / `Linxi@123`

管理员登录后，点击右上角账号菜单中的“切换账号”即可切换到其他启用账号；销售账号可通过“退出登录”返回登录页，再登录其他账号。

数据权限按角色隔离：管理员可以查看和维护全部业务数据，销售账号只能查看和维护负责人为本人的客户、任务、订单及工作台统计；销售提交其他负责人时，后端会自动归属到当前账号。

默认开发数据库为 `youke_crm`，连接 `127.0.0.1:3306`。数据库账号可通过环境变量 `DB_USERNAME` 和 `DB_PASSWORD` 覆盖。

后端默认运行在 `http://127.0.0.1:8080`。首次启动会自动执行 Flyway 数据库迁移并写入少量演示数据；生产环境请通过环境变量覆盖默认数据库密码。

运行后端测试不依赖系统全局安装 Maven，项目会使用 `.tools` 中自带的 Java 和 Maven：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-backend.ps1
```

如果使用 Docker 中的 MySQL（本项目示例映射到宿主机 `3307`），后端启动命令改为：

`powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1 -DbPort 3307 -DbPassword root`

## Docker 一键运行

项目提供了完整的 Docker Compose 配置，会启动 MySQL、Spring Boot 后端和 Nginx 前端。它使用独立的数据卷，默认不会占用现有 `youke-mysql` 的 3307 端口：

```powershell
docker compose up -d --build
docker compose ps
```

启动后访问 `http://127.0.0.1:4173/`，后端地址为 `http://127.0.0.1:8080`，Compose 内置 MySQL 映射到宿主机 `3308`。停止服务：

```powershell
docker compose down
```

### 内网访问

本机作为服务器时，先让电脑和其他设备连接同一个局域网，然后在服务器电脑执行：

```powershell
ipconfig
docker compose up -d --build
docker compose ps
```

找到无线网卡或以太网卡的 IPv4 地址，例如 `192.168.119.228`，其他电脑或手机访问：

`http://192.168.119.228:4173/`

Docker 前端已经通过 Nginx 将 `/api` 代理到后端，内网设备不需要直接访问 `8080` 或 `3308`。如果 Windows 防火墙拦截访问，请使用管理员 PowerShell 放行前端端口：

```powershell
New-NetFirewallRule -DisplayName "YouAI CRM LAN Frontend" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 4173 -Profile Private
```

只在可信的家庭或办公网络使用 `Private` 配置文件，不要把 `4173`、`8080` 或 `3308` 端口暴露到公网。若使用 `scripts\\run-frontend.ps1` 启动非 Docker 前端，脚本也会监听 `0.0.0.0:4173`；此时后端 `8080` 端口也必须允许局域网访问。

如果要复用你已经创建的 `youke-mysql`（宿主机 3307）和其中的数据，先停止上面的 Compose 全栈，再执行：

```powershell
docker compose down
docker compose -f docker-compose.yml -f docker-compose.host-mysql.yml up -d backend frontend
```

该模式不会启动 Compose 内置 MySQL，后端通过 `host.docker.internal:3307` 连接现有数据库。

如需修改密码或端口，可在执行命令前设置 `MYSQL_ROOT_PASSWORD`、`MYSQL_HOST_PORT`、`BACKEND_HOST_PORT` 和 `FRONTEND_HOST_PORT` 环境变量。生产环境必须修改 `JWT_SECRET`。

### 生产环境配置

复制 `.env.example` 为 `.env`，填写强密码和随机 JWT 密钥。生产部署时启用 `prod` Profile；此 Profile 不提供数据库账号、密码或 JWT 密钥默认值，缺少任一变量都会拒绝启动：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:mysql://db-host:3306/youke_crm?useSSL=true'
$env:DB_USERNAME = 'youke_app'
$env:DB_PASSWORD = '<strong-database-password>'
$env:JWT_SECRET = '<at-least-32-random-characters>'
```

不要将 `.env` 提交到 Git，也不要直接向公网开放 `3308` 或 `8080`。

## 后端接口

- `GET /api/health`：服务与数据库健康检查
- `POST /api/auth/login`：账号登录并返回 JWT
- 登录保护默认按 IP 限制为每分钟 30 次尝试；同一账号连续失败 5 次后锁定 15 分钟。可通过 `LOGIN_IP_MAX_ATTEMPTS`、`LOGIN_IP_WINDOW_SECONDS`、`LOGIN_ACCOUNT_MAX_FAILURES`、`LOGIN_ACCOUNT_LOCK_SECONDS` 调整。
- `GET /api/auth/me`：读取当前登录用户
- `GET /api/auth/users`：管理员读取启用账号列表
- `POST /api/auth/switch?username=linxi`：管理员切换账号并返回新 JWT
- `POST /api/auth/logout`：服务端注销当前账号的既有 JWT，客户端同时清除 Token
- `GET/POST /api/customers`：客户查询和新增
- `GET/PUT /api/customers/{customerNo}`：客户详情和编辑
- `POST /api/customers/import`：导入客户（管理员导入后进入白板）
- `PATCH /api/customers/{customerNo}/assignment`：管理员将白板/客户分配给销售员工并记录分配时间
- `GET /api/customers/{customerNo}/assignment-events`：读取客户历次分配记录
- `GET /api/customers/pool`、`PATCH /api/customers/{customerNo}/pool`：查看公海、放入公海或领取公海客户
- `PATCH /api/customers/{customerNo}/stage`：更新跟进阶段
- `GET /api/tasks`：跟进任务列表
- `GET /api/tasks/{id}`：任务详情
- `POST /api/tasks`：新建跟进任务
- `PUT /api/tasks/{id}`：修改跟进任务
- `PATCH /api/tasks/{id}/completion`：更新完成状态
- `DELETE /api/tasks/{id}`：删除跟进任务
- `GET /api/orders`：订单列表
- `GET /api/orders/{orderNo}`：订单详情
- `POST /api/orders`：新建订单
- `PUT /api/orders/{orderNo}`：修改订单
- `PATCH /api/orders/{orderNo}/payment`：更新回款金额
- `PATCH /api/orders/{orderNo}/service`：更新服务状态
- `PATCH /api/orders/{orderNo}/confirmation`：确认或撤销订单业绩
- `GET/POST /api/calls`：查询和新增通话记录
- `GET/POST /api/conversations`：查询和创建客户会话
- `GET/POST /api/conversations/{id}/messages`：查询和发送会话消息
- `PATCH /api/conversations/read`：将当前账号可见消息全部标记为已读
- `GET/POST /api/message-templates`：查询和创建消息模板
- `PUT/DELETE /api/message-templates/{id}`：修改和删除消息模板
- `GET/PATCH /api/notifications/read`：读取或更新当前账号的系统通知已读状态
- `GET /api/call-reviews`：读取当前账号可见通话的质检状态
- `PATCH /api/call-reviews/{callId}`：更新通话质检状态
- `GET /api/dashboard`：工作台汇总数据

任务列表支持 `owner`、`status`、`completed` 查询参数；订单列表支持 `keyword`、`paymentStatus`、`serviceStatus` 查询参数；通话列表支持 `status` 查询参数。所有业务接口都需要在请求头携带 `Authorization: Bearer <accessToken>`。
