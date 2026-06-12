# Knowledge Agent

基于 `Java + Spring Boot` 企业知识库 Agent 项目。

它覆盖了一条比较完整的 Agent 主链路：
- 文档上传与管理
- RAG 检索
- 短期记忆与长期记忆
- LLM 对话生成
- 本地工具调用
- 远程 MCP 工具接入
- 内置前端工作台

项目定位不是“做一个聊天框”，而是演示一个可扩展的企业知识问答原型。

## 项目亮点

- 混合检索而不只是纯向量检索。当前检索链路结合了标题匹配、关键词匹配、向量检索和少量启发式增强。
- 回答链路带证据。`/api/chat` 会返回知识片段、长期记忆命中和工具调用结果，便于演示“答案从哪里来”。
- 有降级策略。远程聊天模型或远程 embedding 不可用时，系统仍能继续工作。
- 工具链路是可展示的。除了本地工具外，还支持通过 MCP 暴露远程工具能力。
- 既能快速 demo，也能切到数据库存储。支持 `memory` 与 `postgres` 两种模式。

## 技术栈

- Java 17
- Spring Boot 3.3
- Spring Web
- Spring Validation
- Spring JDBC
- PostgreSQL
- pgvector
- DeepSeek-compatible chat API
- 自定义 MCP client（支持 JSON 与 SSE 响应解析）

## 核心链路

```text
用户问题
  -> 保存会话消息
  -> 查询短期上下文
  -> 检索知识库
  -> 检索长期记忆
  -> 触发记忆抽取
  -> 调用聊天模型
      -> 如有需要，调用本地工具 / MCP 工具
      -> 工具结果回填给模型
  -> 返回答案 + 证据 + 工具执行记录
```

## 当前能力

### 1. 文档侧

- 上传知识文档：`POST /api/docs/upload`
- 查看文档列表：`GET /api/docs`
- 查看文档详情：`GET /api/docs/{documentId}`
- 更新文档：`PUT /api/docs/{documentId}`
- 删除文档：`DELETE /api/docs/{documentId}`
- 首次启动时自动导入 `examples` 里的演示文档

### 2. 对话侧

- 发起聊天：`POST /api/chat`
- 查看会话：`GET /api/sessions/{sessionId}`
- 查看长期记忆：`GET /api/memories/{userId}`
- 查看运行状态：`GET /api/meta`

### 3. Agent 能力侧

- 短期记忆：基于最近若干轮会话上下文
- 长期记忆：保存用户偏好、业务事实、任务结果
- RAG：文档切片、embedding、召回、结果合并
- 工具调用：本地工具 + MCP 远程工具
- 回答降级：远程模型不可用时，回退到本地 fallback 回答

## 目录结构

```text
src/main/java/com/resume/agent
  agent      编排层，负责把上下文、知识、记忆、模型串起来
  chat       聊天接口与应用服务
  docs       文档上传、详情、更新、删除
  rag        检索与召回逻辑
  memory     短期记忆、长期记忆
  tool       本地工具与 MCP 工具接入
  llm        聊天模型、embedding、fallback
  shared     共享模型与存储抽象
  config     配置与运行状态接口

src/main/resources/static
  index.html 工作台页面
  app.css    页面样式
  app.js     页面交互

examples
  演示知识文档
```

## 快速开始

### 1. 环境要求

- Java 17
- Maven 3.9+ 或可用的 Maven 启动环境
- 可选：PostgreSQL + `pgvector`

### 2. 必要环境变量

如果要启用远程聊天模型和远程 embedding，请配置：

```powershell
$env:DEEPSEEK_API_KEY="your-deepseek-key"
$env:EMBEDDING_API_KEY="your-embedding-key"
```

如果使用 PostgreSQL，可以额外配置：

```powershell
$env:AGENT_DB_URL="jdbc:postgresql://localhost:5432/knowledge_agent"
$env:AGENT_DB_USERNAME="postgres"
$env:AGENT_DB_PASSWORD="your-password"
```

如果要启用远程 MCP：

```powershell
$env:MCP_SERVER_TOKEN="your-mcp-token"
```

### 3. 启动方式

通用方式：

```powershell
mvn test
mvn spring-boot:run
```

启动后访问：

- 工作台：[http://localhost:8080/](http://localhost:8080/)
- 运行状态：[http://localhost:8080/api/meta](http://localhost:8080/api/meta)

仓库里也提供了 [run-agent.ps1](K:\KnowledgeAgent\run-agent.ps1)，适合当前这台机器的固定 Java/Maven 路径环境；如果你的本机路径不同，建议优先使用上面的通用方式。

## 关键配置

配置文件在 [application.yml](K:\KnowledgeAgent\src\main\resources\application.yml)。

### 切换存储模式

内存模式：

```yaml
agent:
  storage:
    type: memory
```

PostgreSQL 模式：

```yaml
agent:
  storage:
    type: postgres
```

### 远程聊天模型

```yaml
agent:
  llm:
    base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY:replace-me}
    chat-model: deepseek-v4-pro
    remote-chat-enabled: true
```

### 远程 embedding

```yaml
agent:
  llm:
    embedding-base-url: ${EMBEDDING_BASE_URL:https://open.bigmodel.cn/api/paas/v4}
    embedding-api-key: ${EMBEDDING_API_KEY:replace-me}
    embedding-model: embedding-2
    remote-embedding-enabled: true
```

### MCP 配置

```yaml
agent:
  mcp:
    enabled: true
    server-name: github
    transport: streamable-http
    endpoint: https://api.githubcopilot.com/mcp/
    auth-token: ${MCP_SERVER_TOKEN:replace-me}
```

## 示例请求

### 上传文档

```json
{
  "title": "员工手册",
  "content": "公司规定：知识库回答必须优先基于已上传文档，不允许编造。"
}
```

### 发起聊天

```json
{
  "sessionId": "demo-session",
  "userId": "demo-user",
  "message": "这个项目用了什么技术栈？"
}
```

### `/api/chat` 返回重点

- `answer`：最终回答
- `knowledgeSnippets`：命中的知识片段
- `longTermMemories`：命中的长期记忆
- `toolCalls`：本轮工具调用详情

这几项很适合在面试演示时解释“为什么模型会这样回答”。

## 面试演示建议

推荐按下面顺序展示：

1. 打开首页，说明这是一个带工作台的企业知识库 Agent Demo。
2. 先看 `/api/meta`，快速说明当前运行模式、模型与 embedding 状态。
3. 上传一份文档或直接使用 `examples` 自动导入的数据。
4. 提问制度类问题，比如“什么时候下班？”“1000 元以上怎么报销审批？”
5. 展示返回里的 `knowledgeSnippets`，强调回答基于证据。
6. 发送一条偏好类消息，比如“我喜欢先给结论再解释原因”，再提问，展示长期记忆。
7. 提问项目类问题，展示工具调用或 MCP 能力。

可直接参考 [examples/demo-prompts.txt](K:\KnowledgeAgent\examples\demo-prompts.txt)。

## 测试

当前已补上的测试覆盖了几类核心场景：

- 文档上传后可参与问答
- 办公时间类问题可命中知识片段
- 长期记忆可写入并读取
- 文档详情、更新、删除链路可用
- fallback 回答行为可验证
- 配置文件不会再带真实密钥默认值

运行方式：

```powershell
mvn test
```

## 已知边界

- 长期记忆抽取目前还是规则触发，适合 demo，但还不是 LLM memory candidate 提取。
- MCP client 目前是自定义实现，便于展示工程能力，但要关注后续协议演进风险。
- 检索目前是“混合召回 + 启发式增强”，对面试展示很友好，但还不是完整的生产级检索方案。
- 远程接口的超时、重试、观测性还可以继续加强。

## 后续可优化方向

- 把记忆抽取升级为 LLM 驱动的 memory candidate 提取
- 增加 rerank / query rewrite / BM25
- 增加文档来源引用与回答出处展示
- 增加更多 MCP 工具与错误治理测试
- 补更完整的集成测试和异常场景测试

## 安全说明

- 仓库中的默认配置已经改为环境变量占位符，不再保留真实 key/token。
- 如果历史上曾提交过真实密钥，仍然应该去对应平台完成密钥旋转。

