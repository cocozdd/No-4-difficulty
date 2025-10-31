# Redis 技术全景与项目落地指南

## 0. 为什么选择 Redis
- **业务痛点**：商品浏览、订单协同、即时聊天、秒杀抢购都要求毫秒级响应、跨实例共享状态，单靠 MySQL 容易出现锁争用和慢查询。
- **替代方案评估**：本地缓存无法跨实例共享；Memcached 缺乏持久化和高级结构；数据库触发器/物化视图在高并发下开销大且难维护。
- **Redis 优势**：内存级吞吐（单实例 10w+ QPS）、丰富数据结构、原子命令/Lua 支持，以及成熟的主从、哨兵、集群方案——既可用作“读写加速层”，又能充当“分布式协调中心”。

## 1. 在项目中的职责与落地情况
| 场景 | 说明 | 状态 & 代码位置 |
|------|------|----------------|
| 登录会话中心 | 统一 Token 校验、批量注销，多实例共享会话 | ✅ `backend/src/main/java/com/campusmarket/service/impl/LoginSessionServiceImpl.java` |
| 登录失败滑动窗口 | 15 分钟内失败 ≥5 次自动锁定账号 | ✅ `backend/src/main/java/com/campusmarket/service/impl/UserServiceImpl.java` |
| 聊天未读 & 指标 | 未读数累计、消息发送统计 | ✅ `ChatCacheServiceImpl`、`ChatMetricsServiceImpl` |
| 热门榜单 & 计数 | 浏览/加购/下单指标 + 热度榜缓存 5 分钟 | ✅ `GoodsMetricsServiceImpl`、`HotGoodsServiceImpl` |
| 秒杀防重 & 库存 | `SETNX` 用户锁 + `DECR` 库存，防超卖 | ✅ `FlashSaleServiceImpl` |
| 下单限流 | 60 秒最多 5 次请求，保护后端 | ✅ `OrderServiceImpl#enforceOrderRateLimit` |
| 分布式 ID | Redis INCR 生成 64 位 Snowflake 风格 ID | ✅ `RedisDistributedIdGenerator`、`OrderEventPublisher` |
| Redis 宕机降级 | 会话/限流自动降级到纯 JWT 模式 | ✅ `JwtAuthenticationFilter`、`LoginSessionServiceImpl` |
| 规划中的能力 | 商品详情二级缓存、验证码计数、Stream 延迟队列、UV 统计 | ⏳ 未来迭代 |

## 2. 关键实现与设计思路

### 2.1 会话中心与降级
- 正常情况下：`LoginSessionServiceImpl.storeSession` 将 `LoginSession` 序列化后写入 `auth:session:{token}` 并维护 `auth:user-sessions:{userId}`；请求进入 `JwtAuthenticationFilter` 时先从 Redis 读取会话再刷新 TTL。
- Redis 不可用时：`LoginSessionServiceImpl` 抛出 `SessionAccessException`，`JwtAuthenticationFilter` 捕获后回退到“纯 JWT”鉴权（根据 Token Claims 获取用户信息并注入权限），保证请求不被 401。Redis 恢复后会话校验自动恢复。
- 日志会输出 WARN，提醒需要尽快恢复 Redis。

### 2.2 登录失败滑动窗口
- 使用 `auth:fail:{username}` 统计 15 分钟内错误次数，首次失败设置 TTL；达到阈值写入 `auth:lock:{username}` 锁定 15 分钟。
- 登录成功或 Redis 不可用时会清理计数/跳过锁定逻辑，确保安全与可用性的平衡。

### 2.3 聊天 & 热榜
- 未读计数：`chat:unread:processed:{userId}` 用 Set 去重消息 ID，`chat:unread:{userId}` Hash 存储未读数，TTL 7 天。
- 热榜指标：`GoodsMetricsServiceImpl` 分别用 Hash 和 ZSet 统计浏览/加购/下单次数，`HotGoodsServiceImpl` 综合计算热度并缓存 5 分钟，商品删除时调用 `removeMetrics` 清理。

### 2.4 秒杀与限流
- 秒杀流程：用户维度的 `SETNX` 锁 + `DECR` 库存，失败时自动回滚并删除锁。
- 下单限流：`OrderServiceImpl` 通过 `INCR` + `EXPIRE` 实现 60 秒 5 次限制；Redis 异常时忽略限流逻辑，保障主流程可用。

### 2.5 分布式 ID
- 结构：`| sign(1) | timestamp(41) | shard(10) | sequence(12) |`，基于 Redis `INCR` 获取 12 位序列，首次自增设置 2 秒 TTL，序列溢出时休眠 1ms。
- Redis 故障降级为“时间戳 + 随机数”，Kafka 事件、订单号等可使用 `nextIdAsString` 生成 Base36 ID。

示例（登录失败窗口）：
```java
Long failures = redisTemplate.opsForValue().increment("auth:fail:" + username);
if (failures == 1L) {
    redisTemplate.expire("auth:fail:" + username, 15, TimeUnit.MINUTES);
}
if (failures >= 5) {
    redisTemplate.opsForValue().set("auth:lock:" + username, "1", 15, TimeUnit.MINUTES);
}
```

## 3. 数据结构与典型 Key
| 数据结构 | 典型 Key | 用途 |
|----------|----------|------|
| String | `auth:session:{token}`、`auth:lock:{username}` | 会话存储、登录锁定 |
| Hash | `chat:unread:{userId}` | 会话未读计数 |
| Set | `chat:unread:processed:{userId}` | 去重消息 ID |
| ZSet | `goods:ranking:{metric}` | 热度排行 |
| List/Stream | 规划中 | 延迟任务、消息流水 |
| Bitmap / HyperLogLog | 规划中 | UV、活跃用户统计 |

## 4. 缓存策略与一致性
- Cache Aside：写库后删缓存，读 miss 时回源并填充。
- TTL：热门榜缓存 5 分钟、聊天未读 7 天，TTL 加随机偏移防止雪崩。
- 穿透防护：空值缓存；后续可加布隆过滤器。
- 热点 Key：规划二级缓存 + 逻辑过期，降低 Redis 压力。

## 5. 原子性与并发控制
- `SETNX` + TTL：防止重复请求、控制抢购并发。
- `INCR` / `DECR`：用于限流、库存扣减、计数；计划将“扣库存 + 入队”封装成 Lua 保证原子性。
- Redis 客户端设置合理的超时/重试，避免放大故障范围。

## 6. 分布式 ID 方案
- Redis Key：`idgen:{businessTag}:{epochMillis}`，首次自增设置 TTL。
- 序列溢出时进入下一毫秒、Redis 不可用时降级随机数。
- 落地：Kafka 事件 `eventId`、订单号、支付流水号等。
- 面试亮点：说明位拆分、单线程原子性、TTL 清理与降级策略。

## 7. 持久化与高可用
- RDB + AOF：`appendfsync everysec`，定期 AOF 重写并保存到对象存储。
- 主从 + Sentinel：至少 1 主 2 从 + 3 Sentinel 仲裁，客户端通过虚拟域名接入。
- Cluster：数据量/QPS 增长时迁移至 Cluster 并演练槽位迁移。

## 8. 监控与运维
- 指标：内存、命中率、慢查询、连接数、复制延迟、Key 总数。
- 工具：Prometheus Redis Exporter、Grafana、RedisInsight、`redis-cli --latency`。
- 热 key / 大 key 巡检：定期执行 `--bigkeys`、`MEMORY USAGE` 并结合业务日志分析。
- 演练：定期进行主从切换、备份恢复、Redis 宕机降级演练。

## 9. 安全与规范
- 启用 ACL，限制危险命令；区分应用、运维、排障账号。
- 网络层仅开放内网，跨机房使用 VPN/TLS；敏感数据不写入 Redis。
- Key 命名/TTL/序列化格式在代码和文档中统一管理，方便审计。

## 10. 研发流程要求
- 统一封装 `StringRedisTemplate`、`RedisDistributedIdGenerator`，避免散落调用。
- 新增 Key/TTL 必须在 PR 和文档中登记；压测覆盖命中率、缓存失效、热点重建、锁竞争。
- 单测/集成测试覆盖 Redis 主要场景（包括故障降级），CI 中执行 `mvn test`。

## 11. 面试答题要点
1. 说明场景痛点：高并发、实时性、跨实例共享。
2. 落地案例：会话中心、登录限流、热榜、秒杀、分布式 ID、宕机降级。
3. 细节：缓存一致性策略、滑动窗口算法、ID 位拆分、降级处理。
4. 故障处理：Redis 宕机、热 key、慢查询的排查与应急方案。
5. 演进规划：单节点 → 主从 → 哨兵 → 集群的演进路线、成本与监控策略。
6. 可观测性：监控指标、告警、演练和日志记录。

## 12. 演示与验证清单
- **代码走查**：`LoginSessionServiceImpl`、`UserServiceImpl`、`FlashSaleServiceImpl`、`RedisDistributedIdGenerator` 等关键类。
- **自动化测试**：`mvn test`，重点展示 `UserServiceImplTest`（滑动窗口）和 Kafka 事件测试中 `eventId` 校验。
- **命令演示**：在 Redis CLI 演示 `INCR/EXPIRE`、`SETNX`、`GET auth:fail:demo` 等命令；模拟 Redis 宕机后依旧可登录。
- **场景剧本**：1) 正常登录 → 会话写入；2) 错误登录触发锁定；3) 秒杀抢购；4) Redis 宕机 → 纯 JWT 鉴权继续生效；5) Kafka 事件幂等演示。
- **资料整理**：准备流程图、监控仪表盘截图（命中率、延迟、慢查询）、压测对比表、故障复盘记录。

> 搭配源码片段、测试报告和监控截图，可快速对面试官展示 Redis 设计思路、工程落地与故障处理能力。

## 13. 面试重点速览
1. **会话中心与降级**：`LoginSessionServiceImpl` + `JwtAuthenticationFilter` 如何在 Redis 宕机时退回纯 JWT，保障可用性。  
2. **登录滑动窗口**：`UserServiceImpl` 中 `auth:fail/*` & `auth:lock/*` 的设计、阈值与安全性。  
3. **秒杀锁与热榜指标**：`FlashSaleServiceImpl` 的 `SETNX` + `DECR`、防超卖逻辑，以及 `GoodsMetricsServiceImpl` / `HotGoodsServiceImpl` 的 Hash/ZSet 用法。  
4. **分布式 ID**：`RedisDistributedIdGenerator` 的位拆分、序列控制、降级策略及 Kafka 幂等应用。  
5. **自动化验证**：`mvn test` 如何覆盖上述核心场景，以及在 Redis 宕机场景下的降级测试思路。

掌握这五点，再配合前述演示材料，就能满足研究生级 Java 面试中对 Redis 应用与工程实践的深度考察。
