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
