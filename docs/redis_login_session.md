# Redis 登录会话设计说明

## 1. 背景与目标

- 业务采用 JWT 无状态认证，但在分布式场景下仍需集中控制登录态（统一注销、单点登录、防止已登出的 Token 继续访问）。  
- 引入 Redis 作为会话中心：所有实例共享 Redis，便于快速校验、吊销和统计登录信息。

## 2. 关键组件

| 组件 | 作用 | 代码位置 |
|------|------|----------|
| `LoginSessionService` | 定义会话存储/刷新/注销接口 | `backend/src/main/java/com/campusmarket/service/LoginSessionService.java` |
| `LoginSessionServiceImpl` | 使用 `StringRedisTemplate` 和 `ObjectMapper` 将会话写入 Redis，Key TTL 与 JWT 相同 | `backend/src/main/java/com/campusmarket/service/impl/LoginSessionServiceImpl.java` |
| `AuthController` | 登录/注册成功后写入 Redis；`/logout` 删除会话 | `backend/src/main/java/com/campusmarket/controller/AuthController.java` |
| `JwtAuthenticationFilter` | 每次请求校验 Token 是否仍在 Redis 中，刷新 `lastSeen` 时间 | `backend/src/main/java/com/campusmarket/security/jwt/JwtAuthenticationFilter.java` |
| `SecurityConfig` | 仅开放 `/api/auth/login`、`/api/auth/register`；其余接口依赖 Redis 会话判定是否已登录 | `backend/src/main/java/com/campusmarket/config/SecurityConfig.java` |

## 3. Redis 数据模型

```
auth:session:{token}        -> JSON，包含 userId/username/role/ip/userAgent/issuedAt/expiresAt/lastSeenAt
auth:user-sessions:{userId} -> Set，存放该用户所有 Token，便于批量注销
```

- `auth:session:{token}` TTL = `jwt.expiration-ms`，到期后自动失效。  
- 刷新逻辑：请求命中会话后更新 `lastSeenAt` 并保持剩余 TTL，不会无限续期。  
- 注销流程：先解析出 `userId`，从 Set 删除 Token，再删除会话 Key。

## 4. 请求流程

1. **登录/注册成功** → 生成 JWT → 调用 `LoginSessionService.storeSession(...)`，写入 Redis。  
2. **后续请求** → `JwtAuthenticationFilter` 读取 Header：  
   - Token 签名校验通过后，从 Redis 取会话。  
   - Redis 未命中视为已失效，直接放行让 Spring Security 返回 401。  
   - Redis 命中则构建 `Authentication` 并写回 `SecurityContext`，同时调用 `refreshSession` 更新 `lastSeenAt`。  
3. **注销** → `/api/auth/logout` 读取当前 Token，调用 `revokeSession` 删除。  
4. **强制下线** → 运维或后台调用 `revokeAllSessions(userId)`，一次性清理所有设备的 Token。

## 5. 运行与排障

- 查看当前登录态：`HGETALL` / `GET`  
  ```powershell
  docker exec campus-redis redis-cli GET auth:session:<token>
  docker exec campus-redis redis-cli SMEMBERS auth:user-sessions:<userId>
  ```
- 强制某用户下线：
  ```java
  loginSessionService.revokeAllSessions(userId);
  ```
- 验证失效：删除会话后再访问任何受保护接口，应得到 401（`JwtAuthenticationFilter` 无法获取 session）。
- Redis 故障降级：服务端所有操作都做异常捕获；在 Redis 宕机时返回空 Optional，等同于会话不存在，系统会要求用户重新登录。重新恢复 Redis 后流程自动生效。

## 6. 面试与复盘要点

- **为什么仍要 Redis 会话**：在多实例下提供集中控制（注销、黑名单、风控），而不仅仅依赖客户端自持 Token。  
- **数据一致性**：JWT 仍做签名校验，Redis 中保存的是“是否仍允许使用该 Token”的额外约束；Token 删除即刻生效。  
- **扩展能力**：可以基于 `auth:user-sessions:*` 做异地登录提醒、并发设备限制、后台审计（IP/User-Agent/时间）。  
- **安全注意事项**：Redis 中只保存必要信息，不存密码；Key 命名前缀统一，便于设置命名空间和权限；日志记录失败原因，方便排障。

## 7. 流程拆解（逐步讲解）

| 步骤 | 触发方 | 关键方法/类 | Redis 行为 | 面试亮点 |
|------|--------|-------------|-------------|----------|
| 1. 提交登录/注册 | 客户端 → `/api/auth/login` or `/register` | `AuthController` 调用 `UserServiceImpl.login/register` | 暂无 | 强调密码校验、BCrypt、JWT 生成 |
| 2. 生成 JWT | `UserServiceImpl` | `JwtTokenProvider.generateToken` | 暂无 | 说明签名算法、过期时间来源于配置 |
| 3. 写入会话 | `AuthController.storeSession` | `LoginSessionService.storeSession` | `SET auth:session:{token}`（TTL=expiration） <br> `SADD auth:user-sessions:{userId}` | 说明 JSON 内容、为什么要记录 IP/User-Agent/lastSeen |
| 4. 返回响应 | `AuthResponse` | DTO 携带 `token/userId/role/expiresIn` | 客户端持有 JWT | 解释增加字段便于前端展示 & 调试 |
| 5. 访问受保护资源 | 客户端 → 任何需要认证的接口 | `JwtAuthenticationFilter` | `GET auth:session:{token}`，命中才放行 | 可以强调 “JWT 签名 + Redis 状态” 双因素 |
| 6. 刷新会话 | 同上 | `LoginSessionService.refreshSession` | 覆盖写 session JSON，更新 `lastSeenAt`，保持剩余 TTL | 体现会话活跃追踪、统计在线时长的可能性 |
| 7. 注销 | 客户端 → `/api/auth/logout` | `LoginSessionService.revokeSession` | `DEL auth:session:{token}` + `SREM auth:user-sessions:{userId}` | 说明删除 Set 的意义（多设备管理） |
| 8. 强制下线 | 后台/运维 | `LoginSessionService.revokeAllSessions` | 遍历 Set、批量删除 session | 面试可扩展：如何做“单点登录”或限制设备数 |

## 8. 常见面试问答及回答要点

1. **“JWT 本身无状态，为何还要 Redis？”**  
   - 客户端自持 Token 无法主动失效，Redis 让服务端掌握最终解释权，可随时吊销、统计、限流。  
   - 多实例场景下，Redis 提供统一的会话视图。  
   - 可以拓展：在此基础上实现风控、异地登录提醒、黑名单等。

2. **“如果 Redis 挂了会怎样？”**  
   - 代码捕获异常，默认视为未命中 → 需要重新登录，确保安全优先。  
   - 可配合 Redis Sentinel/Cluster 提升可用性。  
   - 可以额外提及监控指标（连接数、命中率、Key 过期情况）。

3. **“如何限制同账号多端登录？”**  
   - 利用 `auth:user-sessions:{userId}` 的 Set，将 token 数限制在固定数量。  
   - 登录时检查 Set 长度，超过阈值就删掉最早的 token（可根据 `LoginSession.lastSeenAt` 选择）。  
   - 面试时给出伪代码或策略说明即可。

4. **“如何与前端协同？”**  
   - `AuthResponse` 返回 `expiresInMs`，便于前端显示倒计时或提前刷新。  
  - 登出接口需带上 Bearer Token；未登录访问 401，由前端触发跳转登录。

5. **“如果想做刷新 Token / Remember Me 呢？”**  
   - 可以在 Redis 中再维护 Refresh Token（Key: `auth:refresh:{token}`），逻辑与会话类似。  
   - 简述刷新流程、过期策略、绑定设备场景。

## 9. 自测清单

- [ ] 登录后 `GET auth:session:{token}` 是否存在？字段值是否正确？  
- [ ] 注销接口调用后，Redis Key 是否立即删除？再次访问受保护接口是否 401？  
- [ ] 调用 `revokeAllSessions(userId)` 后，Set 与所有 session Key 是否都被清空？  
- [ ] 直接手动删除 Redis 会话，再请求接口是否被拦截？  
- [ ] Redis 宕机或模拟异常时，日志是否出现 WARN 并提示重新登录？  
- [ ] 结合 Postman/Rest Client 与 redis-cli 对每个步骤进行演练，确保答题时心中有实操路径。

## 10. MySQL + Redis 协同实战总结

| 场景 | 主要实践 | 核心要点 | 代码示例 |
|------|----------|----------|----------|
| **写多读多的数据** | 先写 MySQL，再删除相关 Redis 缓存 | 保证数据库为准；使用“先 DB 后删缓存”避免脏读 | `GoodsServiceImpl.evictCachesForGoods` 触发 `GoodsCacheServiceImpl` 清理 `goods:detail:*`、`goods:list:*` |
| **实时统计/排行榜** | Redis 作为主存储，MySQL 不同步；使用原子操作覆盖高并发 | `HINCRBY`、`ZINCRBY`、`INCR` 等天然支持多线程；异常时降级 | `GoodsMetricsServiceImpl`、`HotGoodsServiceImpl` 维护浏览/加购/订单、ZSet 排行 |
| **登录会话/黑名单** | Redis 保存 Token 状态，MySQL 仍存用户信息 | Redis 只掌控“是否有效”，失效立即生效，MySQL 无需跟进 | `LoginSessionServiceImpl` 维护 `auth:session:{token}` |
| **限流/防刷** | Redis `INCR` + `EXPIRE` 或 Lua 实现窗口计数 | 原子操作、TTL 自动过期，多线程安全 | `OrderServiceImpl.enforceOrderRateLimit` (`rate:order:{userId}`) |
| **分布式锁/一致性** | 若需要强一致更新，可选用 Redisson/SETNX | 本项目的计数类操作不需要锁；缓存失效+原子增保证一致性 | 可在热点库存扣减、互斥任务时扩展 |
| **错误处理策略** | Redis 读写失败时捕获异常，业务流程继续（必要时提示重试） | 避免 Redis 故障放大为服务不可用；日志记录失效 | 所有 Service 的 `try/catch`（如 `GoodsMetricsServiceImpl`, `LoginSessionServiceImpl`） |
| **多线程/多实例同步** | 统一通过 Redis 做状态共享；MySQL 事务保证持久化 | 读多写少场景通过缓存；写入后删缓存；统计、会话类数据靠 Redis | Spring Bean 单例，依赖注入的 Redis 客户端线程安全 |

- **高并发读写**：统计类使用 Redis 原子命令；缓存类在事务提交后统一失效，避免多个线程同时写缓存导致旧数据覆盖。  
- **一致性策略**：读一致性由“DB 写入→清缓存→下次读回填”实现；Redis 作为最终一致的附加信息（热点榜、会话、限流）。  
- **容灾与监控**：日志 WARN 提醒 Redis 失败；可结合 Sentinel/Cluster 保证高可用。MySQL 仍是权威来源，Redis 缺失时系统降级为“需要重新登录/失去缓存加速”。  
- **面试表达**：先描述数据库事务是基础，再解释 Redis 在不同子系统的角色，并列出失败兜底方案、常见并发模式（缓存失效、原子自增、限流、分布式锁）。
- **缓存穿透防护**：商品服务在缓存未命中且数据库查不到时，会写入短 TTL 的空值（GoodsServiceImpl.cacheGoodsNotFound），避免恶意请求持续穿透数据库。
- **缓存击穿防护**：对热点 Key 设置随机 TTL 避免集体过期；对于详情 Key 命中率高的场景，若缓存失效则立即回源并重建缓存，关键操作可扩展加 Redis 分布式锁（当前业务通过写库后删除缓存以及短 TTL 热点缓存，击穿窗口较小）。
- **缓存穿透防护**：商品服务在缓存未命中且数据库查不到时，会写入短 TTL 的空值（GoodsServiceImpl.cacheGoodsNotFound），避免因恶意或偶发的不存在 ID 不断穿透数据库。
- **缓存穿透防护**：商品服务在缓存未命中且数据库查不到时，会写入短 TTL 的空值（GoodsServiceImpl.cacheGoodsNotFound），避免因恶意或偶发的不存在 ID 不断穿透数据库。
- **缓存穿透防护**：商品服务在缓存未命中且数据库查不到时，会写入短 TTL 的空值（GoodsServiceImpl.cacheGoodsNotFound），避免因恶意或偶发的不存在 ID 不断穿透数据库。***

## 11. 设计初衷与架构考量

> 这部分整理项目在规划阶段就确定的准则，可作为面试时的“设计理念”回答。

- **持久层优先**  
  - MySQL 是唯一真相来源，所有写操作先落库，再触发 Redis 失效或补写。  
  - 事务提交成功后才操作缓存，避免多线程/多实例场景下出现旧数据覆盖。  
  - 若缓存刷新失败，只影响性能，不影响正确性。

- **Redis 聚焦高并发与实时性**  
  - 登录会话、未读消息、热门榜单、限流等都使用 Redis 原子命令来保证并发安全。  
  - Key 设计统一规范，TTL 明确，可支撑热点数据秒级更新。  
  - 所有 Redis 调用均包裹异常捕获，故障时自动降级为“重新登录/走数据库”。

- **面向分布式部署**  
  - JWT + Redis 会话中心、WebSocket 握手拦截器、Kafka 异步事件链路，确保多实例部署时状态共享、一致。  
  - `auth:user-sessions:*` 结构预留了单点登录、后台踢人、并发登录限制等扩展能力。

- **可观测与易排障**  
  - 日志记录关键操作与失败原因，方便快速定位问题。  
  - 文档提供 redis-cli 命令、自测清单，便于团队成员验证和回归。  
  - 所有 Key 前缀统一，便于监控、统计、清理。

掌握这些设计原则，有助于在面试中说明“我们不是写完后才想起 Redis，而是在架构阶段就围绕持久层可靠性、缓存一致性和分布式扩展做的规划”。***
