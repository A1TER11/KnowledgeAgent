# Knowledge Agent

> 一个面向面试展示的 `LLM + RAG + Agent` 企业知识库智能问答项目。

![Java 17](https://img.shields.io/badge/Java-17-2f6fed)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6db33f)
![PostgreSQL + pgvector](https://img.shields.io/badge/PostgreSQL-pgvector-336791)
![RAG Hybrid Retrieval](https://img.shields.io/badge/RAG-Hybrid%20Retrieval-c97b21)
![Agent MCP Tools](https://img.shields.io/badge/Agent-MCP%20Tools-7a52cc)

## 项目简介

这个项目不是简单接一个聊天接口，而是围绕企业知识问答场景，完整实现了一条可运行、可演示、可扩展的 Agent 主链路：

- 文档上传与知识库管理
- 基于 RAG 的知识检索
- 短期上下文与长期记忆
- LLM 问答生成
- 本地工具调用与远程 MCP 工具扩展
- 前端工作台展示

项目定位是一个适合面试讲解的企业级 Agent 原型，用来展示我在 `Java 后端`、`LLM 应用开发`、`RAG 检索`、`向量存储`、`协议接入` 和 `工程化` 方面的综合能力。

## 面试官最值得关注的点

### 1. 不是普通 CRUD，而是 LLM 应用系统

项目围绕 `LLM + RAG + Agent` 架构设计，核心不是表单增删改查，而是如何让模型基于知识、记忆和工具稳定回答问题。

### 2. 做了完整的知识问答链路

从文档上传、切片、向量化、召回，到模型生成、工具调用、结果回填，形成了完整闭环，而不是只做了某一个孤立模块。

### 3. 有真实技术取舍

检索方案没有停留在“纯向量检索”，而是采用混合召回策略，结合标题匹配、关键词匹配、向量检索和启发式增强，提升制度类、知识类问题的召回效果。

### 4. 有可扩展的 Agent 能力

除了本地工具，还实现了基于 `MCP` 的远程工具扩展机制，支持后续接入更多外部能力。

### 5. 有工程化意识

项目包含 fallback 降级方案、测试补充、敏感配置收敛、示例文档和演示工作台，不只是“能跑”，而是“能讲、能演示、能维护”。

## 技术栈

### 后端

- `Java 17`
- `Spring Boot 3.3`
- `Spring Web`
- `Spring Validation`
- `Spring JDBC`

### 数据与存储

- `PostgreSQL`
- `pgvector`
- `memory / postgres` 双存储模式

### LLM / RAG / Agent

- `DeepSeek-compatible Chat API`
- `Embedding Service`
- `RAG`
- 混合检索：标题匹配 + 关键词匹配 + 向量检索 + 启发式增强
- 短期记忆 / 长期记忆
- Tool Calling
- `MCP` 协议接入
- Fallback 降级策略

### 前端

- `HTML`
- `CSS`
- `JavaScript`

## 我独立完成的核心工作

- 独立完成项目需求梳理、技术选型、整体架构设计与模块拆分
- 独立完成后端核心模块开发，包括文档管理、知识检索、会话管理、长期记忆、工具调用等
- 独立设计 `PostgreSQL + pgvector` 数据结构，完成知识文档、切片向量、长期记忆等存储建模
- 独立实现 `LLM + RAG` 知识问答链路，支持文档切片、向量化、召回、模型生成与工具结果回填
- 独立接入 `DeepSeek` 兼容模型、远程 `Embedding` 服务和 `MCP` 远程工具能力
- 独立补充单元测试、集成测试、配置安全治理与 README 演示文档

## 核心架构

```text
用户提问
  -> 保存会话消息
  -> 查询短期上下文
  -> 检索知识库
  -> 检索长期记忆
  -> 触发记忆抽取
  -> 调用 LLM
      -> 如有需要，触发本地工具 / MCP 工具
      -> 工具结果回填给模型
  -> 返回答案 + knowledgeSnippets + longTermMemories + toolCalls
```

## 核心能力

### 1. 文档与知识库管理

- 上传知识文档：`POST /api/docs/upload`
- 查看文档列表：`GET /api/docs`
- 查看文档详情：`GET /api/docs/{documentId}`
- 更新文档：`PUT /api/docs/{documentId}`
- 删除文档：`DELETE /api/docs/{documentId}`
- 启动时自动导入 `examples` 下的演示文档

### 2. 智能问答

- 发起问答：`POST /api/chat`
- 查看会话：`GET /api/sessions/{sessionId}`
- 查看长期记忆：`GET /api/memories/{userId}`
- 查看运行状态：`GET /api/meta`

### 3. 检索与记忆

- 文档切片
- 向量化
- 混合召回
- 短期上下文
- 长期记忆命中

### 4. 工具扩展

- 本地工具调用
- 远程 MCP 工具接入
- 工具结果回填给模型继续生成最终答案

## 为什么这个项目能体现技术能力

### LLM 应用能力

不仅是调用模型接口，而是把知识检索、上下文管理、工具调用和降级策略整合成完整系统。

### RAG 能力

不是只停留在“存向量、查相似度”，而是针对问题类型设计了混合召回与启发式增强策略。

### 数据库能力

结合 `PostgreSQL + pgvector` 完成关系数据与向量数据统一存储，既考虑业务结构，也考虑检索链路。

### 协议与系统扩展能力

实现了自定义 `MCP client`，支持 JSON / SSE 响应解析，为远程工具扩展预留能力。

### 工程能力

项目具备 fallback、测试、配置治理、演示工作台和文档说明，更接近真实项目而不是纯 demo 代码。

## 运行方式

### 环境要求

- `Java 17`
- `Maven 3.9+`
- 可选：`PostgreSQL + pgvector`

### 环境变量

如启用远程模型与远程 embedding：

```powershell
$env:DEEPSEEK_API_KEY="your-deepseek-key"
$env:EMBEDDING_API_KEY="your-embedding-key"
```

如启用 PostgreSQL：

```powershell
$env:AGENT_DB_URL="jdbc:postgresql://localhost:5432/knowledge_agent"
$env:AGENT_DB_USERNAME="postgres"
$env:AGENT_DB_PASSWORD="your-password"
```

如启用远程 MCP：

```powershell
$env:MCP_SERVER_TOKEN="your-mcp-token"
```

### 启动

```powershell
mvn test
mvn spring-boot:run
```

启动后访问：

- 工作台：[http://localhost:8080/](http://localhost:8080/)
- 运行状态：[http://localhost:8080/api/meta](http://localhost:8080/api/meta)

## 面试演示建议

建议按下面顺序讲，最容易让面试官快速理解项目价值：

1. 先介绍这是一个基于 `LLM + RAG + Agent` 的企业知识库问答系统
2. 打开 `/api/meta`，说明当前运行模式、模型状态和 embedding 状态
3. 展示知识文档上传或直接使用 `examples` 自动导入的数据
4. 提问制度类问题，例如“什么时候下班？”、“1000 元以上报销怎么审批？”
5. 展示返回里的 `knowledgeSnippets`，说明答案不是编的，而是基于知识库证据
6. 发送偏好类消息，例如“我喜欢先给结论再解释原因”，再提问，展示长期记忆能力
7. 继续提问项目类或查询类问题，展示工具调用或 MCP 扩展能力

可直接参考 [examples/demo-prompts.txt](K:\KnowledgeAgent\examples\demo-prompts.txt)。

## 测试与工程化

当前已覆盖的核心测试场景包括：

- 文档上传后参与问答
- 办公时间类问题命中知识片段
- 长期记忆写入与读取
- 文档详情、更新、删除链路
- fallback 回答逻辑
- 配置文件敏感信息占位校验

运行测试：

```powershell
mvn test
```

## 当前已知边界

- 长期记忆抽取目前仍以规则触发为主，后续可升级为 LLM memory candidate 提取
- 检索策略适合演示和原型验证，但仍有继续引入 rerank / query rewrite 的空间
- MCP client 为自定义实现，能够展示工程能力，但后续需要关注协议演进兼容性
- 远程模型调用的超时、重试、观测能力仍可继续补强

## 后续优化方向

- 升级长期记忆提取策略
- 增加 rerank / BM25 / query rewrite
- 增加答案出处展示
- 扩展更多 MCP 工具
- 增强观测性与异常治理

## 安全说明

- 仓库默认配置已改为环境变量占位，不保留真实 key / token
- 如果历史上使用过真实密钥，仍应在对应平台完成密钥旋转
