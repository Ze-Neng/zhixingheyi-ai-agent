# 知行合一 — AI Agent 系统

> 一个具备自主决策、工具调用、知识库检索的 AI Agent 系统。

## 项目简介

**知行合一**是一个基于 Spring AI 框架的 AI Agent 系统，实现了 **Think-Execute 循环机制**——Agent 能够理解复杂任务、规划执行步骤、调用外部工具，并基于 RAG 技术从知识库中检索相关信息，完成多步骤的复杂任务。

它不是简单的 ChatBot，而是真正的 Agent：**能规划、能调用工具、能检索知识库、还能把执行过程实时推给前端**。

## 核心特性

- **Think-Execute 循环**：多轮自主决策，LLM 每一步自行判断是否需要调用工具
- **工具调用框架**：统一 Tool 接口，FIXED/OPTIONAL 两级分类，最小权限原则
- **RAG 知识库**：固定大小分块 + 滑动窗口重叠 + bge-m3 嵌入 + pgvector 向量检索
- **多模型支持**：注册表模式（ChatClientRegistry），支持 DeepSeek/GLM 动态切换
- **SSE 实时推送**：Agent 执行过程全链路可观测（PLANNING → THINKING → EXECUTING → DONE）
- **真实天气查询**：集成 Open-Meteo API，支持城市查询和 IP 自动定位

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Java 17, Spring Boot 3.5, Spring AI 1.1 |
| 数据库 | PostgreSQL + pgvector 扩展 |
| ORM | MyBatis 3 |
| LLM | DeepSeek (`deepseek-chat`), 智谱 AI (`glm-4.6`) |
| 嵌入模型 | Ollama + bge-m3 (1024 维) |
| 前端 | React 19, TypeScript, Vite, Ant Design 6, Tailwind CSS 4 |

## 系统架构

```
前端 (React) ←→ Controller (REST API) ←→ Service (业务逻辑)
                                              ↕
                                    Agent 核心 (Think-Execute 循环)
                                    ├─ ChatClientRegistry (多模型)
                                    ├─ Tool System (FIXED + OPTIONAL)
                                    │   ├─ TerminateTool (终止任务)
                                    │   ├─ KnowledgeTools (RAG 检索)
                                    │   ├─ DataBaseTools (数据库查询)
                                    │   ├─ EmailTools (邮件发送)
                                    │   └─ WeatherQueryTool (天气查询)
                                    └─ RAG Service (pgvector)
                                              ↕
                                    PostgreSQL (业务数据 + 向量数据)
```

## 快速开始

### 环境要求

- JDK 17+
- PostgreSQL 14+（需启用 pgvector 扩展）
- Ollama（用于 bge-m3 嵌入模型）
- Node.js 18+

### 后端启动

```bash
cd 知行合一
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
# 编辑 application.yaml，填入你的 API Key 和数据库连接信息
./mvnw spring-boot:run       # Linux/macOS
mvnw.cmd spring-boot:run     # Windows
```

数据库初始化：
```sql
CREATE DATABASE zhixingheyi;
CREATE EXTENSION vector;
```

### 前端启动

```bash
cd ui
npm install
npm run dev                   # http://localhost:5173
```

## 核心设计决策

1. **事件驱动解耦**：HTTP 请求立即返回，Agent 通过 @Async + @EventListener 异步执行
2. **手动控制工具执行**：关闭 Spring AI 自动工具执行，每一步可观测、可介入、可调试
3. **FIXED vs OPTIONAL 工具**：最小权限原则，Agent 按需配置工具能力（FIXED 工具始终可用，OPTIONAL 工具由前端勾选）
4. **注册表模式**：Map<String, ChatClient> Spring 自动注入，加新模型只加 @Bean
5. **pgvector 统一存储**：业务数据和向量数据在同一套 PostgreSQL，降低运维复杂度
6. **双重安全兜底**：MAX_STEPS=20 硬限制 + ChatMemory 滑动窗口
7. **固定大小分块**：Markdown 文档按 800 字符/chunk 切分，100 字符重叠窗口，对内容做 embedding

## 可用工具一览

### FIXED 工具（所有 Agent 默认拥有）

| 工具 | 功能 |
|------|------|
| `terminate` | 结束 Agent 循环，任务完成时调用 |
| `KnowledgeTool` | RAG 知识库语义检索 |
| `dateTool` | 获取当前日期 |

### OPTIONAL 工具（创建 Agent 时可勾选）

| 工具 | 功能 |
|------|------|
| `dataBaseTool` | PostgreSQL 只读查询（仅允许 SELECT） |
| `emailTool` | QQ 邮箱 SMTP 异步发送邮件 |
| `weatherQueryTool` | Open-Meteo 天气查询（支持城市名或 IP 自动定位） |

## 配置说明

首次使用请复制配置模板：

```bash
cp src/main/resources/application-example.yaml src/main/resources/application.yaml
```

然后编辑 `application.yaml` 填入你的配置：

- `spring.datasource.*` — PostgreSQL 连接信息
- `spring.ai.deepseek.api-key` — DeepSeek API Key（https://platform.deepseek.com）
- `spring.ai.zhipuai.api-key` — 智谱 AI API Key（https://open.bigmodel.cn）
- `spring.mail.*` — QQ 邮箱 SMTP（可选，仅 EmailTools 需要）
- `document.chunk.*` — RAG 分块参数（可选，默认 size=800, overlap=100）

**注意：勿将 application.yaml 提交到 Git 仓库，已在 .gitignore 中排除。**

## 功能演示

> 演示视频待补充，建议录制以下核心功能：

### 1. 创建智能体

展示如何创建一个 Agent：选择模型（DeepSeek/GLM）、勾选可选工具（数据库查询、邮件、天气）、设置系统 Prompt。

### 2. 天气查询

```
用户："今天天气怎么样？"
Agent 自动调用 weatherQuery → IP 定位 → Open-Meteo API → 返回当前温度、天气状况、风速、最高/最低温
```

### 3. 知识库 + 生成报告 + 发送邮件

```
用户上传一份 Markdown 文档到知识库
  → bge-m3 嵌入 → 固定大小分块 → 存入 pgvector
用户："帮我分析这份文档，生成一份分析报告，发到 xxx@qq.com"
Agent 依次调用：
  knowledgeQuery → 检索相关内容
  → LLM 根据检索结果生成分析报告
  → sendEmail → 异步发送邮件
```

## 许可证

MIT License
