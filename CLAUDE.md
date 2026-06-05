# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 最后更新: 2026-05-28

## Project overview

基于 Spring AI 的 AI 恋爱大师 + 自主 Agent 应用。教学项目 by 程序员鱼皮。

| 层 | 技术 |
|---|------|
| 框架 | Spring Boot 3.4.4 + Spring AI 1.0.0 |
| LLM | Alibaba DashScope (qwen-plus) |
| Embedding | DashScope text-embedding-v2 (1536维) |
| 向量库 | PGVector (PostgreSQL) |
| ORM | MyBatis-Plus 3.5.10.1 |
| 缓存 | Redis |
| 存储 | MySQL |
| 前端 | Vue 3 + Vite |
| MCP 子项目 | Spring Boot 3.4.5 |

## Build & run

```bash
mvn compile
mvn spring-boot:run                           # 后端, 端口 8123, context-path /api
cd yu-ai-agent-frontend && npm install && npm run dev   # 前端, 端口 3000（Vite 代理 /api → 8123）
cd yu-image-search-mcp-server && mvn spring-boot:run    # MCP 服务端, 端口 8127（可选）
```

**Prerequisites:** Redis running, MySQL running (db `yupi`), PostgreSQL with `CREATE DATABASE yu_agent; CREATE EXTENSION vector;`, DashScope API key in `application.yml`.

**MCP 配置需注释掉才能在不启动 MCP 服务端时运行后端**（`application.yml` 中 `spring.ai.mcp.*` 已默认注释）。

## Architecture

### Auth (JWT httpOnly Cookie)

```
注册/登录 → JwtUtil.createToken() → Cookie(token=JWT; HttpOnly; Path=/)
    → UserContextInterceptor.preHandle() 读 Cookie → UserContext.setUserId()
      → ChatHistoryRepository 从 conversationId 解析 userId（非 ThreadLocal，避免异步线程丢失）
```

| 端点 | 方法 |
|------|------|
| `POST /api/user/register` | 注册 |
| `POST /api/user/login` | 登录 |
| `GET /api/user/me` | 当前用户信息 |
| `POST /api/user/logout` | 退出（清 Cookie） |

JWT 配置在 `application.yml`: `jwt.secret` / `jwt.ttl` / `jwt.token-name` → `JwtConfig`（`@ConfigurationProperties`）→ `JwtUtil`（静态方法）。

Key files: `util/JwtUtil.java`, `config/JwtConfig.java`, `interceptor/UserContextInterceptor.java`, `controller/UserController.java`, `context/UserContext.java`.

### Chat memory (3-layer persistence)

```
MessageWindowChatMemory (窗口截断 20 条)
  → HistoryAwareChatMemory (装饰器: diff 新消息 → MySQL INSERT)
    → RedisChatMemory (Redis List, TTL 7d; cache miss → MySQL 回灌)
      → ChatHistoryRepository (MySQL chat_history, 只增不删)
```

- userId 编码进 conversationId（`4:love_xxx`），`ChatHistoryRepository.parseConversationId()` 拆分
- `ChatHistoryRepository` 用 JdbcTemplate，不接入 MyBatis-Plus（稳定不变）

Key files: `chatmemory/RedisChatMemory.java`, `chatmemory/HistoryAwareChatMemory.java`, `repository/ChatHistoryRepository.java`, `config/RedisChatMemoryConfig.java`.

### Agent hierarchy

```
BaseAgent (AgentState 状态机, run/runStream 循环, 私有 messageList)
  → ReActAgent (step = think + act)
    → ToolCallAgent (ToolCallingManager, 禁用内置工具执行)
      → YuManus (@Component, maxSteps=20, 英文系统提示)
```

Controller 每次请求 **new 新 YuManus 实例**——Agent 状态不跨请求保持。

### Conversation (会话管理)

| 端点 | 方法 | 描述 |
|------|------|------|
| `GET /api/conversation/list` | 列表 | 按更新时间倒序 |
| `POST /api/conversation/create` | 创建 | title="新对话" |
| `DELETE /api/conversation/{id}` | 删除 | 级联删除 chat_history |
| `GET /api/conversation/{chatId}/history` | 历史消息 | 按时间升序 |

- 前端点"新对话"立刻调 create → 侧边栏即时出现
- 第一条消息发送后，后端把 title 从"新对话"更新为消息前 20 字
- 点击历史会话加载 chat_history 消息
- 删除会话弹 ConfirmModal 确认 → 一起删 conversation + chat_history

Key files: `controller/ConversationController.java`, `model/entity/Conversation.java`, `model/entity/ChatHistory.java`, `service/ConversationService.java`, `service/ChatHistoryService.java`.

### RAG pipeline

```
用户问题 → QueryRewriter (LLM 改写) → Embedding
  → LoveAppRagCustomAdvisorFactory (topK=3, threshold=0.5)
    → status=null 全表检索 / 非 null 按 metadata 过滤
    → 无结果兜底 "我只回答恋爱问题"
  → 文档拼入 Prompt → LLM
```

- 纯向量语义检索（cosine），无关键词联合、无 rerank
- `MyTokenTextSplitter` 和 `MyKeywordEnricher` 已编写但未接入
- PGVector 每次启动 DELETE + 重刷，仅适合开发

### LoveApp — 唯一活跃方法

`doChatByStream(message, chatId)` — 流式对话，合并了记忆 + RAG + 工具 + MCP：

```java
queryRewriter.rewrite(msg) → advisors(memory, RAG) → toolCallbacks(allTools, MCP) → .stream().content()
```

旧方法 `doChat` `doChatWithRag` `doChatWithTools` `doChatWithMcp` `doChatWithReport` 均已注释。

### Frontend (Vue 3 SPA)

| 路由 | 视图 | 描述 |
|------|------|------|
| `/` | `Home.vue` | 入口页，右上用户下拉 |
| `/login` | `Login.vue` | 登录注册页 |
| `/love-master` | `LoveMaster.vue` | AI 恋爱大师，左侧会话栏 |
| `/super-agent` | `SuperAgent.vue` | AI 超级智能体，左侧会话栏 |

共享组件: `ChatRoom.vue` (聊天 UI)，`ConversationSidebar.vue` (会话列表)，`ConfirmModal.vue` (确认弹窗)，`AppFooter.vue` (页脚)。

路由守卫检查 `localStorage.user`，未登录自动跳 `/login`。Vite 代理 `/api → localhost:8123` 解决跨域。

### MyBatis-Plus 注意

`ServiceImpl.lambdaQuery()` 有 bug（3.5.10.1），必须用 `new LambdaQueryWrapper<>()` 替代。Service 层继承 `ServiceImpl<M, T>`，CRUD 方法自带。

### Key classes

| Class | Role |
|-------|------|
| `app/LoveApp.java` | 核心对话：记忆 + RAG + 工具 + MCP |
| `controller/AiController.java` | 聊天 SSE 端点 |
| `controller/UserController.java` | 登录注册 |
| `controller/ConversationController.java` | 会话管理 |
| `agent/YuManus.java` | 自主 Agent |
| `util/JwtUtil.java` | JWT 静态工具 |
| `config/JwtConfig.java` | JWT 配置 |
| `config/ChatClientConfig.java` | ChatClient Bean 装配 |
| `interceptor/UserContextInterceptor.java` | Cookie → UserContext |
| `exception/GlobalExceptionHandler.java` | 全局异常处理 |

## Development conventions

### 分层调用链

Controller 不写业务逻辑，只做参数接收和调度。超过一行的逻辑必须下沉到 ServiceImpl。

```
Controller (@RestController)
  → Service (interface extends IService<T>)
    → ServiceImpl (@Service extends ServiceImpl<M, T> implements Service)
      → Mapper (@Mapper extends BaseMapper<T>)
```

规则：
- **Controller** — 只做两件事：接收参数、调用 Service。禁止出现 SQL、业务判断、拼接逻辑
- **Service 接口** — 定义自定义方法签名（MyBatis-Plus 通用的 CRUD 由 IService 提供，不重复声明）
- **ServiceImpl** — 所有业务逻辑、SQL 条件拼装、事务处理都在这里。MyBatis-Plus 3.5.10.1 的 `lambdaQuery()` 有 bug，统一使用 `new LambdaQueryWrapper<>()`
- **Mapper** — 只继承 `BaseMapper<T>`，不加自定义方法。后续需要复杂 SQL 时写在 `resources/mapper/*.xml`

### 方法注释

每个新方法必须在开头写一行注释表明作用，中文，一行写完：

```java
/**
 * 根据手机号查询用户
 */
public User findByPhone(String phone) { ... }

/**
 * 删除会话及其所有聊天记录
 */
public void deleteByIdWithHistory(Long id, Long userId) { ... }
```

## Known issues

- PGVector 每次启动 DELETE + 重刷，仅适合开发。
- `MessageRecord.toMessage()` 对 `"tool"` 角色返回空 `ToolResponseMessage`。
- MP 3.5.10.1 `lambdaQuery()` 链式调用 bug，需用 `new LambdaQueryWrapper<>()`。
- 未实现: 多查询扩展、rerank、关键词+向量联合检索。
