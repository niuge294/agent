# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 最后更新: 2026-05-14

## Project overview

基于 Spring AI 的 AI 恋爱大师 + 自主 Agent 应用。教学项目 by 程序员鱼皮。

| 层 | 技术 |
|---|------|
| 框架 | Spring Boot 3.4.4 + Spring AI 1.0.0 |
| LLM | Alibaba DashScope (qwen-plus) |
| Embedding | DashScope text-embedding-v2 (1536维) |
| 向量库 | PGVector (PostgreSQL) |
| 缓存 | Redis |
| 存储 | MySQL |
| 前端 | Vue 3 + Vite |
| MCP 子项目 | Spring Boot 3.4.5 |

## Build & run

```bash
mvn compile
mvn spring-boot:run                           # 后端, 端口 8123, context-path /api
cd yu-ai-agent-frontend && npm install && npm run dev   # 前端
cd yu-image-search-mcp-server && mvn spring-boot:run    # MCP 服务端, 端口 8127
```

**Prerequisites:** Redis running, MySQL running (db `yupi`), PostgreSQL with `CREATE DATABASE yu_agent; CREATE EXTENSION vector;`, DashScope API key in `application.yml`.

## Architecture

### Chat memory (3-layer persistence)

```
MessageWindowChatMemory (窗口截断 20 条)
  → HistoryAwareChatMemory (装饰器: diff 新消息 → MySQL INSERT)
    → RedisChatMemory (Redis List, TTL 7d; cache miss → MySQL 回灌)
      → ChatHistoryRepository (MySQL chat_history, 只增不删)
```

| 操作 | Redis | MySQL |
|------|-------|-------|
| `saveAll()` | `RPUSH` + `EXPIRE` 7天 | `HistoryAwareChatMemory` 自动 diff `INSERT` |
| `findByConversationId()` | `LRANGE` 命中返回 | Redis miss → `findLastN(20)` 回灌 |

注意事项：
- `ChatMemoryRepository` 方法名与 `ChatMemory` 不同：`saveAll`/`findByConversationId`/`deleteByConversationId`
- 全局 `ObjectMapper` 被 `chatMemoryObjectMapper` Bean 替代，已配 `FAIL_ON_UNKNOWN_PROPERTIES=false`
- `MessageRecord.toMessage()` 对 `"tool"` 角色返回空 `ToolResponseMessage`，工具响应不持久化

Key files: `chatmemory/RedisChatMemory.java`, `chatmemory/HistoryAwareChatMemory.java`, `chatmemory/model/MessageRecord.java`, `repository/ChatHistoryRepository.java`, `config/RedisChatMemoryConfig.java`.

### Agent hierarchy

```
BaseAgent (AgentState 状态机, run/runStream 循环, 私有 messageList)
  → ReActAgent (step = think + act)
    → ToolCallAgent (ToolCallingManager, 禁用内置工具执行)
      → YuManus (@Component, maxSteps=20, 英文系统提示)
```

Controller 每次请求 **new 新 YuManus 实例**——Agent 状态不跨请求保持。

Key files: `agent/BaseAgent.java`, `agent/ReActAgent.java`, `agent/ToolCallAgent.java`, `agent/YuManus.java`, `agent/model/AgentState.java`.

### RAG pipeline

**ETL:**
```
3 个 *.md (classpath:document/)
  → LoveAppDocumentLoader 按 --- 切分
  → 标注 filename + status (单身/恋爱/已婚) 元数据
  → PgVectorStore.add() → Embedding → PG
```

**检索:**
```
用户问题 → QueryRewriter (LLM 改写) → Embedding
  → LoveAppRagCustomAdvisorFactory (topK=3, threshold=0.5)
    → status=null 全表检索 / 非 null 按 metadata 过滤
    → 无结果兜底 "我只回答恋爱问题"
  → 文档拼入 Prompt → LLM
```

- 纯向量语义检索（cosine），无关键词联合、无 rerank
- `MyTokenTextSplitter` (200/100) 和 `MyKeywordEnricher` 已编写但**未接入**
- `LoveAppVectorStoreConfig`（内存版）仍在创建 Bean 但 RAG 未使用，浪费 AI 调用
- RAG 端点未暴露到 Controller；前端实际调用 `doChatByStream()`

Key files: `rag/PgVectorVectorStoreConfig.java`, `rag/LoveAppRagCustomAdvisorFactory.java`, `rag/QueryRewriter.java`, `rag/LoveAppDocumentLoader.java`, `rag/LoveAppContextualQueryAugmenterFactory.java`, `rag/LoveAppRagCloudAdvisorConfig.java`.

### Data sources

| 数据源 | 配置前缀 | 用途 |
|--------|---------|------|
| MySQL (主) | `spring.datasource.*` | 对话历史 |
| PostgreSQL (次) | `app.pgvector.datasource.*` | PGVector 向量存储 |

PG DataSource 在方法内自建，**不注册为 Spring Bean**，避免冲突。启动时 `SELECT COUNT(*)` + `DELETE` + `add()` 重刷。

### Tools (7 个, 注册于 `tools/ToolRegistration.java`)

| 工具 | 类 | 功能 |
|------|-----|------|
| WebSearchTool | `WebSearchTool.java` | 百度搜索 (SearchAPI) |
| WebScrapingTool | `WebScrapingTool.java` | 网页抓取 (JSoup) |
| FileOperationTool | `FileOperationTool.java` | 文件读写 |
| ResourceDownloadTool | `ResourceDownloadTool.java` | URL 资源下载 |
| TerminalOperationTool | `TerminalOperationTool.java` | 命令执行 (cmd.exe /c) |
| PDFGenerationTool | `PDFGenerationTool.java` | PDF 生成 (iText) |
| TerminateTool | `TerminateTool.java` | Agent 终止标记 |

文件默认路径：`{user.dir}/tmp/{file,download,pdf}/`

### MCP integration

- **Client** (主项目): `spring-ai-starter-mcp-client`, SSE 连接 `localhost:8127`, `LoveApp.doChatWithMcp()` 使用
- **Server** (`yu-image-search-mcp-server/`): 独立 Spring Boot 3.4.5 应用, 暴露 `searchImage` 工具 (Pexels API), 支持 SSE/STDIO 传输

### Controllers & API

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/ai/love_app/chat/sync` | GET | 同步聊天 |
| `/api/ai/love_app/chat/sse` | GET | SSE 流式聊天 |
| `/api/ai/manus/chat` | GET | YuManus 流式执行 |
| `/api/health` | GET | 健康检查 |

### Frontend (Vue 3 SPA, `yu-ai-agent-frontend/`)

| 路由 | 视图 | SSE 策略 |
|------|------|---------|
| `/` | `Home.vue` | - (入口页) |
| `/love-master` | `LoveMaster.vue` | 逐字追加到同一气泡 |
| `/super-agent` | `SuperAgent.vue` | 按中文标点分句，800ms 间隔错开气泡 |

共享组件: `ChatRoom.vue` (聊天 UI), `AiAvatarFallback.vue` (头像), `AppFooter.vue` (页脚)。
API 层: `src/api/index.js` — axios 实例 + `connectSSE()` 通用 SSE 连接器。

## Key classes

| Class | Role |
|-------|------|
| `app/LoveApp.java` | 核心业务: chat / RAG / tools / MCP / 结构化报告 |
| `controller/AiController.java` | REST + SSE 端点 |
| `agent/YuManus.java` | 即用型自主 Agent |
| `tools/ToolRegistration.java` | `ToolCallback[]` Bean |
| `chatmemory/RedisChatMemory.java` | `ChatMemoryRepository` 的 Redis 实现 |
| `rag/PgVectorVectorStoreConfig.java` | PGVector 配置 (启动时重建) |

## LoveApp methods

| 方法 | 状态 | Controller 暴露 |
|------|------|----------------|
| `doChat()` | ✅ | `/love_app/chat/sync` |
| `doChatByStream()` | ✅ | `/love_app/chat/sse` |
| `doChatWithReport()` | ✅ 结构化输出 | 未暴露 |
| `doChatWithRag()` | ✅ | 未暴露 |
| `doChatWithTools()` | ❌ 注入 toolCallbacks 但未调用 | 未暴露 |
| `doChatWithMcp()` | ✅ | 未暴露 |

## Known issues

- `doChatWithTools()` is broken — 注入了 `ToolCallback[]` 但未调 `.toolCallbacks()`。
- PGVector 每次启动 DELETE + 重刷，仅适合开发。
- `LoveAppVectorStoreConfig` 内存版向量库浪费一次 AI 关键字调用，可注释掉。
- `MessageRecord.toMessage()` 的 `"tool"` 角色返回空 `ToolResponseMessage`。
- MCP 服务端 Pexels API key 硬编码在 `application.yml`。
- `ReReadingAdvisor` 在 LoveApp 构造函数中被注释掉。
- 未实现: 多查询扩展、rerank、关键词+向量联合检索。
- `demo/` 包下示例类未用于生产。
