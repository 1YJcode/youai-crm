# 数据中心知识库

入口：顶部“数据中心”菜单 → “知识库”。

- 管理员：新增、编辑和删除知识文章。每篇包含标题、分类和正文；删除需再次点击确认。
- 已登录员工：搜索和阅读，无维护权限。知识库是企业共享资料，不应用于保存某个客户的私密信息。
- 支持标题及正文关键词搜索、分类筛选、每页 12 篇及分页。分类根据已有文章生成。
- MySQL 持久化，迁移 V31。文章保留创建/更新账号和时间；版本校验拒绝旧窗口覆盖或删除新内容。
- 正文为纯文本，HTML 和脚本不会执行。目前为文章知识库，尚无附件上传和 AI 问答。

验证：`KnowledgeControllerTest` 覆盖新增/搜索/编辑/删除、版本冲突、员工只读及未登录拒绝，在 H2 和独立 MySQL 8.4 上均通过。完整应用浏览器测试 `KnowledgeBrowserE2ETest` 覆盖菜单进入、创建、正文搜索、编辑、重置及确认删除；Playwright 使用测试后端，未写入业务库。

浏览器测试需 `RUN_KNOWLEDGE_BROWSER=true` 和包含 Playwright 的 `NODE_PATH`，使用本机 Edge。MySQL 回归入口：`scripts/test-recording-mysql.ps1 -TestClasses KnowledgeControllerTest`。

2026-10-17 已部署至现有 Docker 服务，前端 `http://localhost:4173/`。健康返回 UP，V31 迁移成功，原客户仍为 57 条，知识库初始为空。脚本返回 200，未登录访问接口返回 401。部署前数据库、旧前端文件和配置备份位于 Git 忽略目录 `.tmp_deploy_knowledge_20261017/`；后端旧镜像标签为 `before-knowledge-20261017`。未向业务知识库写入测试文章。
