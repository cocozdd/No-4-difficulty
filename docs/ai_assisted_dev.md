# AI 辅助开发备忘

> 目标：在 Java/Spring Boot 实习或落地项目中，用 1–2 个主流 AI 工具提升效率，同时保持安全、可验证的交付质量。

## 1. 推荐工具组合

| 类型 | 代表工具 | 建议用途 |
| --- | --- | --- |
| AI 结对编程/补全 | GitHub Copilot、Codeium、Cursor（三选一即可） | 生成 Spring Boot 脚手架、Controller/Service/Repository 样板、JPA/MapStruct 代码、异常处理、日志骨架 |
| 通用 Chat 助理 | ChatGPT、Claude（二选一） | 代码评审（线程安全、事务、N+1）、需求澄清、架构权衡、日志/堆栈分析、性能排查、文档辅助 |

工具少而精：熟练掌握“一补全 + 一 Chat”即可支撑大多数后端场景。

## 2. 常见 AI 用例

1. **需求 → 接口设计**：让模型根据 PRD 输出 RESTful API + OpenAPI 草案，人工再校验。
2. **代码 → 测试**：生成 JUnit5 + Mockito 单测骨架、REST Assured/API 测试样例、Testcontainers（Postgres/Redis/Kafka）模板。
3. **SQL 与建模**：请模型给出数据表、范式、索引建议，或润色复杂 SQL；自己再跑 `EXPLAIN` 验证。
4. **DevOps 自动化**：让模型起草 Dockerfile、docker-compose、GitHub Actions、Prometheus/Grafana 指标排障清单。
5. **日志/性能诊断**：提供栈追踪/Actuator 指标，请模型列出可能原因和验证步骤，再逐项验证。

## 3. “最低配” AI 工作流示例

| 环节 | 提示示例 |
| --- | --- |
| 需求 → 接口 | “根据以下需求给我 REST API 设计（资源/路径/状态码），并生成 OpenAPI 3.0 草案。” |
| 代码 → 测试 | “针对这段 Controller/Service，生成 JUnit5 + Mockito 测试骨架，覆盖异常路径，并给一个 Testcontainers 集成测试示例。” |
| 性能/SQL | “这条查询在 100 万行数据下很慢，结合表结构给索引建议，并给我可运行的基准脚本（JMH 或 wrk）。” |
| 可靠性 | “检查这段 @Transactional 代码是否存在懒加载异常或 N+1 风险，并给重构建议。” |

## 4. 两周练习路线

- **第 1 周**：Spring Boot + JPA + PostgreSQL CRUD 服务 → 用 AI 生成接口/测试 → 接入 Swagger → Testcontainers 集成测试 → Docker 化 + CI 草稿。
- **第 2 周**：加入 Redis 缓存、Kafka/RabbitMQ、Spring Security + JWT、Micrometer → 让 AI 协助写压测脚本与日志分析 → 生成 README、Mermaid 架构图。

## 5. 面试与投递提效

- **简历定制**：用模型依据 JD 生成针对性的项目要点，但要确保经历真实、能复盘。
- **行为面试**：让模型按 STAR 模板润色故事、列出可能追问做演练。
- **算法/系统设计**：请模型给思路与复杂度，再自行实现/手撕，验证掌握程度。

## 6. 风险与边界

1. 不上传公司/课程的敏感代码；必要时脱敏或使用企业/本地模型。
2. AI 输出必须可运行、可测试，并经人工审查；把它当“聪明的自动补全”，不是权威答案。
3. 保留作者意识：所有 PR / 文档都要写清楚何时使用了 AI，以及人工验证方式。

> Checklist：提示词 + 模型输出 → 人工验证（运行/测试/审阅）→ 纳入版本库。如此循环，AI 才能真正提升交付质量。
