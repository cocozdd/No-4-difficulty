# MySQL 实战与面试要点

## 1. 项目内务必落实的实践

| 场景 | 当前措施 & 计划 | 说明 |
|------|-----------------|------|
| **表结构与约束** | 核心表：`goods`、`orders`、`chat_message`、`flash_sale_item`、`flash_sale_order` 等，均采用自增主键；`flash_sale_order` 已设置 `UNIQUE(flash_sale_item_id, user_id)` 防重复下单。 | 确保字段 `NOT NULL`、默认值、外键约束齐全，避免脏数据。 |
| **索引策略** | 在 `docs/sql/mysql_optimizations.sql` 中整理了推荐索引：<br>• `goods`：`IDX_goods_status_category_published (status, category, published_at)`<br>• `orders`：`IDX_orders_buyer_created (buyer_id, created_at)`、`IDX_orders_goods_id (goods_id)`<br>• `chat_message`：`IDX_chat_message_pair_created (sender_id, receiver_id, created_at)`<br>• `flash_sale_order`：`IDX_flash_sale_item_created (flash_sale_item_id, created_at)` | 运行脚本前后用 `SHOW INDEX FROM <table>` 对比，使用 `EXPLAIN` 验证查询是否命中索引。示例：`EXPLAIN SELECT * FROM orders WHERE buyer_id = ? ORDER BY created_at DESC LIMIT 20;` |
| **事务与锁控制** | 写操作由 Spring 事务包裹。在秒杀场景中 Redis 负责快速扣减，数据库层通过唯一索引保证幂等；若需要 DB 扣库存，可扩展 `UPDATE flash_sale_item SET total_stock = total_stock - 1 WHERE id = ? AND total_stock > 0` 实现乐观锁。 | 熟悉 `SELECT ... FOR UPDATE`、乐观锁版本号等模式，面试可结合秒杀举例说明如何避免超卖和重复下单。 |
| **缓存协同** | 缓存模式为“写库 → 删缓存”；空值缓存、防雪崩（随机 TTL）已落实；Redis 回滚逻辑在秒杀服务中处理。 | 可以补充延迟双删或 MQ 异步刷新作为扩展回答。 |
| **慢查询与监控** | 压测前通过 `EXPLAIN` 检查关键 SQL；建议上线前开启 slow query log + Prometheus/MySQL Exporter。 | 面试时可分享一次慢查询优化流程：开启日志 → `EXPLAIN` → 调整索引/SQL。 |
| **读写分离 / 高可用** | 目前单库运行。规划阶段可接入主从复制，应用层利用读写分离数据源或代理组件（ShardingSphere-Proxy）。 | 说明触发条件：并发激增、报表查询多等场景。 |
| **数据归档 / 分表** | 聊天记录、秒杀订单增长快，规划按照时间归档或历史库存表。 | 准备在面试中说明“避免单表过大”的思路。 |
| **备份恢复** | 建议执行全量 + binlog 增量备份策略，配合恢复演练。 | 可以准备一份运维脚本或手工命令（`mysqldump` + `mysqlbinlog`）。 |

> **操作提示**：创建/更新索引只需执行 `docs/sql/mysql_optimizations.sql`，示例命令：`mysql -u campus_user -p campus_market < docs/sql/mysql_optimizations.sql`

## 2. 面试高频知识点

### 2.1 事务与隔离级别
- ACID 特性；MySQL 默认 `REPEATABLE READ`。  
- 幻读、不可重复读的概念；MVCC（`undo log`、快照读）。  
- `redo log`、`undo log`、`binlog` 的作用与“两阶段提交”。

### 2.2 锁机制
- 行锁、表锁、意向锁、间隙锁含义及触发场景。  
- `SELECT ... FOR UPDATE`、`UPDATE` 如何加锁；死锁排查 (`SHOW ENGINE INNODB STATUS \G`)。  
- 乐观锁 vs 悲观锁：版本号字段、时间戳；结合秒杀库存举例。

### 2.3 索引
- B+ Tree、聚簇索引/二级索引、覆盖索引、回表。  
- 最左前缀原则、联合索引设计；`EXPLAIN` 输出字段（type、key、rows、Extra）。  
- 索引失效场景：函数、隐式转换、前导 `%`、数据倾斜。

### 2.4 SQL 优化与慢查询
- `EXPLAIN`、`SHOW PROFILE`、慢查询日志的使用方法。  
- 分页优化：延迟关联、覆盖索引；批量插入/更新。  
- 慢查询分析工具：`pt-query-digest`。  
- 准备一个真实案例：例如“订单列表慢 → 建索引 → 平均耗时从 500ms 降至 30ms”。

### 2.5 主从复制与读写分离
- 复制流程：主库写 binlog → 从库 IO 线程拉取 → SQL 线程重放。  
- 复制模式：异步、半同步、全同步；读写延迟影响。  
- 故障切换方案：手动提升、MHA/Orchestrator、云服务。  
- 读写分离一致性问题（写后读）与解决手段（强制走主库、延迟校验）。

### 2.6 分库分表与数据扩展
- 垂直拆分 vs 水平拆分；取模/范围/哈希三种分片。  
- 分布式事务的应对：柔性事务、消息最终一致、`seata`/`sharding-jdbc`。  
- 秒杀热点：Redis 预扣、库存队列、异步落库；结合项目说明。 

### 2.7 其它常见问题
- 数据一致性 & 缓存双写问题；事务失败后的补偿。  
- 连接池（HikariCP）核心参数：最大/最小连接、连接超时。  
- 字符集：UTF8MB4；排序规则带来的大小写/比较差异。  
- 安全性：SQL 注入、权限最小化、ddl 权限隔离。

## 3. 面试表述技巧
1. **结合项目**：从秒杀、缓存、压测入手，说明具体 SQL/索引如何优化。  
2. **突出实践**：展示`docs/sql/mysql_optimizations.sql` 的变更、`EXPLAIN` 验证截图、慢查询定位过程。  
3. **举出案例**：死锁排查、索引改造、缓存一致性处理。  
4. **展望计划**：描述后续引入读写分离、Nginx 负载均衡、备份恢复演练等路线，体现持续演进能力。
