# Java 后端面试速查笔记

## 1. 架构概览
- **整体结构**：Spring Boot 单体服务，整合 MyBatis-Plus + MySQL 持久层、Redis 缓存、Kafka 事件流、MinIO 对象存储以及 WebSocket/STOMP 实时通讯。
- **分层设计**：Controller → Service → Repository → Domain，配套 DTO/VO 转换、统一异常处理与响应包装。
- **配置管理**：`application.yml` 按 profile（dev/test/prod）拆分，敏感配置走环境变量或配置中心；依赖可通过 Docker Compose 一键拉起。

## 2. 核心功能模块
- **认证授权**：JWT + Spring Security，登录成功写入 Redis 会话中心，并通过 `auth:fail/*` + `auth:lock/*` 实现 15 分钟滑动窗口限流。
- **Redis 宕机降级**：`JwtAuthenticationFilter` 在捕获 `SessionAccessException` 时会回退到纯 JWT 鉴权，保障登录流程不中断（Redis 恢复后自动切回会话校验）。
- **商品 / 订单**：订单状态机（待支付→待发货→待收货→完成/取消），Service 层校验身份、防止超卖，并触发 Kafka 事件。
- **聊天系统**：WebSocket 推送消息，Redis 记录未读数与消息指标，数据库持久化，确保离线可追溯。
- **秒杀 / 闪购**：独立 `flash_sale_item`、`flash_sale_order`，Redis 预扣库存 + 用户防重锁，Kafka 发送异步通知。
- **分布式 ID**：`RedisDistributedIdGenerator` 基于 Redis INCR + 自定义 epoch 生成 64 位 ID；`OrderEventPublisher` 生成 `eventId` 保障消息幂等，可扩展为订单号/支付流水号。

## 3. 性能与稳定性
- **数据库优化**：`docs/sql/mysql_optimizations.sql` 维护索引，`scripts/mysql_optimize_compare.sql` 输出基线 vs 优化对比；慢查询通过 `pt-query-digest` 分析。
- **缓存策略**：Cache-Aside，写库后删缓存；热点 key 加随机 TTL；计划补充本地二级缓存 + 逻辑过期。
- **限流与熔断**：登录失败滑动窗口 + 下单频控使用 Redis 原子计数；外围网关（Gateway/Sentinel）负责熔断降级；关键接口带幂等 token。
- **异步解耦**：Kafka 承载订单事件、通知，失败落入 DLQ；事务消息通过本地消息表补偿。
- **Redis 能力**：会话中心、热榜、秒杀锁、频控与分布式 ID 均依赖 Redis，详见 `docs/redis_full_notes.md`。

## 4. 数据一致性
- **无外键策略**：生产禁外键，由 Service 层显式校验；定期巡检孤儿数据并告警。
- **分布式锁**：Redis 锁/秒杀防重；规划引入 Redisson 公平锁、Lua 原子扣减。
- **事务管理**：`@Transactional` 保证单服务一致；跨服务使用本地消息表 + MQ 最终一致，后续计划接入 Seata。

## 5. 测试与质量保障
- **单元测试**：JUnit5 + Mockito 覆盖 Service 逻辑；Repository 使用 `@MybatisTest` 配合 H2/测试容器。
- **集成测试**：Spring Boot Test + Testcontainers 拉起 MySQL/Redis/Kafka，模拟下单、聊天等端到端流程。
- **性能测试**：`performance/` 提供 JMeter/Locust 脚本，关注 TPS、p95 延迟、错误率；配合 SkyWalking/Zipkin 定位瓶颈。

## 6. 运维与交付
- **CI/CD**：GitHub Actions + Maven 构建，执行静态扫描（SpotBugs/Checkstyle）、单测、Docker 镜像发布。
- **部署模式**：支持 Docker Compose 与 Kubernetes；配置健康检查、滚动更新、灰度发布；配置从 ConfigMap/Secret 注入。
- **监控告警**：Prometheus + Grafana 监控 JVM、线程池、数据库连接池、Kafka lag；ELK 采集日志，配置关键告警。
- **备份恢复**：`mysql_notes.md` 记录 MySQL 备份策略；Redis RDB/AOF 周期校验；MinIO 启用版本化。

## 7. 面试答题提示
- **强调成果**：准备 1~2 个真实案例（如订单列表从 500ms 优化至 30ms、Redis 分布式 ID 保障幂等）。
- **结构化表达**：按“场景 → 方案 → 关键点 → 成果 → 风险”叙述，辅以指标和日志截图。
- **扩展思路**：说明未来演进（微服务拆分、事件驱动、读写分离、Serverless）体现规划能力。
- **协作流程**：说明需求评审、代码评审、上线 checklist、事故复盘，展示团队意识。
- **代码质量**：提及统一日志、错误码、Swagger/OpenAPI 文档，以及技术债治理实践。

## 8. 演示材料 Checklist
- 在 `backend/` 执行 `mvn test`，保留 `target/surefire-reports` 摘要，重点展示 Kafka 事件 `eventId` 校验与登录滑动窗口用例。
- 通过 Redis CLI 演示 `auth:session/*`、`auth:fail/*`、`auth:lock/*` 的写入与清理流程，可录制命令行截图。
- 准备 Redis 会话中心、登录限流、秒杀锁、分布式 ID 的流程图或架构草图；说明异常/降级策略。
- 收集性能对比数据（如 `mysql_optimize_compare.log`、JMeter 报告）和监控截图（Redis 命中率、慢查询、Kafka lag）。
- 准备一次实际故障或压测复盘记录，展示“发现问题→定位→优化”全链路。

> 建议携带 `mysql_optimize_compare.log`、Redis ID 生成流程图、压测与监控截图，与上述清单组成完整的面试材料。
