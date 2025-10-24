# Kafka 事件参考手册

## 1. 文档范围

本手册梳理 Campus Market 项目中 Kafka 的使用情况，涵盖：

- 已使用的 Topic、事件模型与职责划分；
- 生产者、消费者的实现要点；
- 手动/自动化验证步骤；
- 监控、排障及后续规划。

文档为 UTF-8 编码，可在 VS Code / Typora / Obsidian 等编辑器中查看。

## 2. Topic 清单

| Topic            | 分区 | 保留策略 | 生产者（当前实现）                                     | 消费者（当前实现）                                   | 主要用途                               |
|------------------|------|----------|--------------------------------------------------------|------------------------------------------------------|----------------------------------------|
| `order-events`   | 3    | 默认     | `OrderService`、`KafkaDiagnosticsService`              | `OrderEventListener`                                 | 订单生命周期变更、诊断冒烟验证         |
| `goods-events`   | 3    | 默认     | `GoodsServiceImpl`（创建/更新/删除/审核/浏览等操作）   | `GoodsEventListener` + `GoodsEventProcessor`         | 商品行为采集、缓存刷新、热榜更新       |
| `chat-events`    | 3    | 默认     | `ChatMessageServiceImpl`（消息入库后触发）             | `ChatEventListener` + `ChatMetricsService`           | 聊天消息推送、审计、消息计数           |

> 所有 Topic 在 `KafkaTopicConfig`（`backend/src/main/java/com/campusmarket/config/KafkaTopicConfig.java`）中自动声明，可视需要调整分区数、保留策略等配置。

## 3. 事件结构

### 3.1 `OrderEvent`

```json
{
  "eventType": "ORDER_CREATED | ORDER_STATUS_CHANGED | TEST_EVENT",
  "orderId": 12345,
  "goodsId": 67890,
  "buyerId": 24680,
  "currentStatus": "CREATED | PAID | SHIPPED | COMPLETED | TEST",
  "previousStatus": "CREATED | PAID | ... | null",
  "note": "可选的业务或诊断说明",
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **消息 key**：使用 `orderId`（诊断事件无订单号时退化为 `diag-<UUID>`）。
- **Schema 演进**：新增字段保持可选，避免删除/重命名已存在字段。
- **校验**：生产者在发送前完成领域校验；消费者对可空字段做好容错。

### 3.2 `GoodsEvent`

```json
{
  "eventType": "GOODS_CREATED | GOODS_UPDATED | GOODS_DELETED | GOODS_REVIEWED | GOODS_MARKED_SOLD | GOODS_VIEWED",
  "goodsId": 1001,
  "sellerId": 2001,
  "actorId": 3001,
  "currentStatus": "APPROVED | PENDING_REVIEW | ...",
  "quantity": 5,
  "note": "Goods updated by seller",
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **消息 key**：使用 `goodsId`，保证同一商品事件落在同一分区。
- **actorId**：标识触发操作的用户（卖家/管理员/浏览者），浏览事件可能为空。
- **currentStatus / quantity**：提供热度榜、搜索、运营统计所需上下文。
- **note**：用于日志与 BI 解读。

### 3.3 `ChatEvent`

```json
{
  "eventType": "CHAT_MESSAGE_CREATED",
  "messageId": 555,
  "senderId": 101,
  "receiverId": 202,
  "messageType": "TEXT | IMAGE",
  "contentPreview": "Hello ...",
  "eventTime": "2024-01-30T12:34:56.789"
}
```

- **消息 key**：使用 `messageId` 确保幂等处理。
- **contentPreview**：提供消息摘要，方便 Push、审计或 BI 引用。

## 4. 生产者注意事项

1. **统一封装**：订单相关事件通过 `OrderEventPublisher`、`KafkaDiagnosticsService` 发送，保持一致的发布模板。
2. **商品事件全面覆盖**：`GoodsServiceImpl` 在创建、更新、删除、审核、库存变动、浏览等节点调用 `GoodsEventPublisher`，保证事件与数据库写操作同事务提交。
3. **聊天事件链路**：`ChatMessageServiceImpl` 保存消息后立即调用 `ChatEventPublisher`，触发 `CHAT_MESSAGE_CREATED` 事件，供推送和审计使用。
4. **避免阻塞**：生产者禁止直接 `CompletableFuture#join()`；若需同步确认，需设置超时（诊断接口示例：5 秒）。
5. **日志追踪**：发布结果以 `INFO`/`WARN` 记录，并附带业务标识，便于跨服务排查。

### 诊断接口

- `POST /api/diagnostics/kafka/order-events`：发送 `TEST_EVENT`，内置 5 秒超时。
- 前端开发模式（`npm run dev`）首页提供“Kafka 快速测试”卡片，可直接调该接口。

## 5. 消费者注意事项

### 5.1 通用配置

- `KafkaListenerConfig` 注册了 `DefaultErrorHandler(FixedBackOff(1s, 2))`，失败时重试两次并记录 WARN。
- 消费者需保持幂等性（可通过 Redis Set、数据库唯一键、业务主键等实现）。
- 如需进一步容灾，可接入 Retry Topic、死信队列等机制。

### 5.2 订单事件监听器

- `OrderEventListener` 打印 `type/orderId/status/note`，便于通知、缓存刷新、统计等下游消费。

### 5.3 商品事件监听器

- `GoodsEventListener` 结合 `GoodsEventProcessor` 异步处理：
  - `GOODS_VIEWED`：交由 `GoodsEventProcessor` 调用 `GoodsMetricsService.recordView`；
  - `GOODS_DELETED`：移除对应热度/计数数据；
  - 其他变更（创建、更新、审核、售罄等）：触发 `HotGoodsService.evictHotCache()`。
- 监听器使用独立 groupId（`${spring.kafka.consumer.group-id}-goods`），便于水平扩展。

### 5.4 聊天事件监听器

- `ChatEventListener` 搭配 `ChatMetricsService`，在消费 `CHAT_MESSAGE_CREATED` 时将消息计数写入 Redis（发送数/接收数）。
- 监听器使用 `${spring.kafka.consumer.group-id}-chat`，便于和订单、商品监听独立扩容。

## 6. 验证手册

按重要性顺序执行以下验证：

1. **CLI 冒烟**
   ```powershell
   "smoke-test" | docker exec -i campus-kafka /opt/kafka/bin/kafka-console-producer.sh `
     --bootstrap-server localhost:9092 --topic order-events

   docker exec campus-kafka /opt/kafka/bin/kafka-console-consumer.sh `
     --bootstrap-server localhost:9092 --topic order-events --from-beginning --timeout-ms 5000
   ```
2. **后端诊断接口**
   ```powershell
   Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/diagnostics/kafka/order-events `
     -Body '{"message":"Hello Kafka","orderId":9001}' -ContentType 'application/json'
   ```
   → 期望 HTTP 200，日志输出 `Kafka diagnostics event published`。
3. **前端测试面板**
   - 运行 `npm run dev`，访问 `http://localhost:5173`，使用“Kafka 快速测试”卡片提交消息，确认界面回显及后台日志。
4. **消费组 Lag**
   ```powershell
   docker exec campus-kafka /opt/kafka/bin/kafka-consumer-groups.sh `
     --bootstrap-server localhost:9092 --describe --group quick-check
   ```
5. **异常消息回放（订单）**
   ```powershell
   docker exec -i campus-kafka /opt/kafka/bin/kafka-console-producer.sh `
     --bootstrap-server localhost:9092 --topic order-events <<< "invalid"
   ```
   → 检查 `DefaultErrorHandler` 是否打印重试告警。
6. **商品事件验证**
   - 浏览：`POST /api/goods/{id}/view`（确保已审核通过），确认 `GoodsEventListener` 日志出现 `GOODS_VIEWED` 且 Redis 计数增加。
   - 创建/更新/审核：操作商品后，确认日志包含对应事件，热榜缓存被刷新。
7. **聊天事件验证**
   ```powershell
   Invoke-RestMethod -Method Post `
     -Uri http://localhost:8080/api/chat/messages `
     -Headers @{ Authorization = 'Bearer <token>' } `
     -Body '{"receiverId":2,"content":"ping","messageType":"TEXT"}' `
     -ContentType 'application/json'
   ```
   → 检查 `ChatEventListener` 日志及 Redis 计数（`chat:metrics:sent` / `chat:metrics:received`）。
8. **Topic 列表确认**
   ```powershell
   docker exec campus-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
   ```
   → 应列出 `order-events`、`goods-events`、`chat-events`。


### 自动化测试

- 依赖 Docker 环境（Testcontainers 会自动拉起容器）。
- 运行 `./mvnw test` 可执行 order/goods/chat 事件的集成测试。

## 7. 监控与告警

- **Lag / 吞吐**：结合 Prometheus + Kafka JMX Exporter 采集指标，设置 Lag 阈值告警。
- **应用指标**：开启 Spring Boot Actuator（`/actuator/prometheus`），关注发布成功/失败次数、消费耗时。
- **日志中心**：对接 ELK/EFK，重点关注 `GoodsEventProcessor`、`ChatEventListener`、`KafkaDiagnosticsService` 的 WARN/ERROR。
- **可用性检查**：可通过定时任务自动调用诊断接口或推送测试消息，提前发现 Broker/监听器异常。

## 8. 排障速查

| 现象                           | 可能原因                     | 处理建议                                                                 |
|--------------------------------|------------------------------|--------------------------------------------------------------------------|
| `UNKNOWN_TOPIC_OR_PARTITION`   | Topic 未创建或 Broker 重启   | 参照 `SETUP_GUIDE` 手动创建；必要时开启自动建 Topic 或调整启动顺序        |
| 诊断接口超时（5 秒）           | Kafka Broker 不可达          | 检查 Docker 容器状态、网络连通及 9092 端口映射                           |
| 消费端无日志/阻塞              | 反序列化异常或监听器异常退出 | 查看日志，调整 Schema/Trusted Packages，必要时引入死信队列               |
| Lag 持续增长                   | 消费者异常或处理过慢         | 增加实例、优化消费逻辑、检查 Offset 提交策略                             |
| `goods-events` 未触发          | 服务未调用发布器或事务回滚   | 对照 `GoodsServiceImpl` 对应代码与数据库操作记录，确保事务成功提交       |
| `chat-events` 未触发           | 消息保存失败或发布器异常     | 查看 `ChatMessageServiceImpl` 日志，确认消息写入及事件发布成功           |
| Redis 计数无变化               | Redis 不可用或权限不足       | 检查 `StringRedisTemplate` 依赖，确认 Redis 服务、认证、网络配置         |
| 401/403                        | JWT 过期/缺失                | 重新登录获取 Token，并确认请求头 `Authorization` 设置正确                |

## 9. 后续规划

0. ���� GitHub Actions ����（`.github/workflows/ci.yml`）��push/PR �Զ������ backend `./mvnw test` �� frontend `npm run build`����������������Ҫʱ���ڼ� �Լ�����ƽ����顣
1. 为 `goods-events`、`chat-events` 增加更多实用消费者：热度榜、搜索索引、推送服务、运营审计等。
2. 使用 Testcontainers/Embedded Kafka 编写发布-消费集成测试，并纳入 CI。
3. 引入 Schema Registry，统一管理事件结构与版本兼容策略。
4. 将 Kafka 指标纳入 Grafana，并设置 Lag、失败率、Broker 健康等告警。
5. 规划事件级 SLA 与 Topic 保留策略，对高价值事件设置更长保留、更多副本。

---

如 Topic、事件字段、消费策略发生调整，请第一时间更新本手册，并同步检查相关源码引用是否一致。***
