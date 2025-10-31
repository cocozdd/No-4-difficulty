# MySQL 实战落地要点

## 1. 项目落地实践清单

| 领域 | 现状 & 规划 | 说明 |
|------|-------------|------|
| **表结构与约束** | 核查 `goods`、`orders`、`chat_message`、`flash_sale_item`、`flash_sale_order` 等表的主键、唯一约束；`flash_sale_order` 增加 `UNIQUE(flash_sale_item_id, user_id)` 防止重复下单 | 确保字段 `NOT NULL`、默认值、数据约束与业务一致 |
| **索引体系** | 推荐集中维护脚本 `docs/sql/mysql_optimizations.sql`：`goods`(`IDX_goods_status_category_published`)、`orders`(`IDX_orders_buyer_created`,`IDX_orders_goods_id`)、`chat_message`(`IDX_chat_message_pair_created`)、`flash_sale_order`(`IDX_flash_sale_item_created`) | 执行前使用 `SHOW INDEX FROM <table>` 和 `EXPLAIN` 验证；通过 `mysql -u campus_user -p campus_market < docs/sql/mysql_optimizations.sql` 应用 |
| **性能对比** | `scripts/mysql_optimize_demo.sql` 自动跑基线查询与优化后查询并输出日志 | `mysql -u campus_user -p campus_market < scripts/mysql_optimize_demo.sql > scripts/mysql_optimize_demo.log`，对比 BEFORE/AFTER 的执行计划与耗时 |
| **并发一致性** | Spring 服务结合 Redis 原子累加 + 唯一约束控制热点库存；订单更新使用 `UPDATE ... WHERE stock > 0` 防止超卖 | 关注超卖、重复下单、订单撤销后的库存恢复 |
| **缓存协同** | 关键写操作已经实现“写库 + 删除缓存 + TTL”策略 | 可扩展为双删 + MQ 异步刷新的组合以降低缓存雪崩风险 |
| **查询治理** | 压测前逐条 `EXPLAIN` 核心 SQL；开启慢查询日志 + Prometheus/MySQL Exporter | 建立常用 SQL 优化清单，保留 `SHOW PROFILE`/slow log 结果 |
| **读写分离与高可用** | 当前单实例，计划阶段按模块切读写、预留读库 | 演练主从延迟、故障切换；评估 MGR 或 Proxy 层的接入 |
| **归档与分库分表** | 订单、聊天记录准备按时间归档 | 定义保留窗口、归档脚本、历史库容量与查询策略 |
| **备份与恢复** | 全量备份 + binlog 增量，定期恢复演练 | 准备 `mysqldump`、`mysqlbinlog` 组合脚本，记录恢复 SOP |

## 2. 理论知识复盘

### 2.1 事务与隔离
- ACID 特性；默认隔离级别 `REPEATABLE READ`
- MVCC 依赖 `undo log`，读写一致性通过当前读/快照读实现
- `redo log`、`undo log`、`binlog` 三者协调保证事务持久化

### 2.2 锁与并发控制
- 行锁/间隙锁/临键锁的触发场景
- `SELECT ... FOR UPDATE`、`SHOW ENGINE INNODB STATUS\G` 定位锁冲突
- 悲观锁与乐观锁策略、避免长事务

### 2.3 索引
- B+Tree 结构、聚簇索引与二级索引差异
- 最左前缀原则、覆盖索引与回表开销
- 索引失效的典型原因：函数转换、前置 `%` 模糊匹配、类型隐式转换

### 2.4 SQL 优化与查询诊断
- 主要工具：`EXPLAIN`、`SHOW PROFILE`、慢查询日志、`pt-query-digest`
- 调优思路：分页、范围查询、避免 `SELECT *`
- 目标：如“订单列表”查询从 500ms 压到 30ms

### 2.5 复制与高可用
- 主从架构下 binlog 写入流程，IO/SQL 线程拆分
- 异步、半同步、全同步复制模式取舍
- 容灾方案：MHA、MGR、延迟从库等；读写分离需考虑回放延迟

### 2.6 分库分表与扩展
- 垂直拆分、水平分片（取模/范围/哈希）
- 分布式事务组件：`seata`、`sharding-jdbc`
- 秒杀类业务：Redis 预扣、限流、异步落库

### 2.7 运维与安全
- 双写一致性、失败补偿
- 连接池参数（HikariCP）配置、超时控制
- 字符集 UTF8MB4、SQL 注入防护、最小权限

## 3. 交付物与验证
1. **性能手册**：梳理热点 SQL、压测场景、索引调整清单
2. **演示材料**：展示 `mysql_optimize_demo.log` 的 EXPLAIN/耗时对比
3. **排障指南**：准备一份常见故障定位手册 + SQL 优化案例
4. **拓展规划**：描述读写分离、缓存架构、Nginx 层限流等后续演进

## 4. 无 FK 优化演练脚本

### 4.1 测试环境准备
- 克隆一份测试库：`CREATE DATABASE campus_market_opt;`
- 导入正式库数据：`mysqldump campus_market | mysql campus_market_opt`
- 所有实验在 `campus_market_opt` 中执行，避免影响线上

### 4.2 FK → 索引 → 恢复 的 SQL 模板
执行前使用 `SHOW CREATE TABLE` 确认外键名称，如有差异请替换成当前实际值。

```sql
USE campus_market_opt;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 删除外键
ALTER TABLE goods DROP FOREIGN KEY fk_goods_seller;
ALTER TABLE orders DROP FOREIGN KEY fk_orders_goods;
ALTER TABLE orders DROP FOREIGN KEY fk_orders_buyer;
ALTER TABLE cart_items DROP FOREIGN KEY fk_cart_user;
ALTER TABLE cart_items DROP FOREIGN KEY fk_cart_goods;
ALTER TABLE chat_message DROP FOREIGN KEY fk_chat_message_sender;
ALTER TABLE chat_message DROP FOREIGN KEY fk_chat_message_receiver;

-- 2) 执行索引 / 结构调整
-- 示例：请换成实际要验证的 DDL
-- ALTER TABLE orders DROP INDEX IDX_orders_goods_id;
-- CREATE INDEX IDX_orders_buyer_created ON orders (buyer_id, created_at DESC);

-- 3) 补回外键
ALTER TABLE goods
  ADD CONSTRAINT fk_goods_seller FOREIGN KEY (seller_id) REFERENCES user(id);

ALTER TABLE orders
  ADD CONSTRAINT fk_orders_goods FOREIGN KEY (goods_id) REFERENCES goods(id),
  ADD CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES user(id);

ALTER TABLE cart_items
  ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user(id),
  ADD CONSTRAINT fk_cart_goods FOREIGN KEY (goods_id) REFERENCES goods(id);

ALTER TABLE chat_message
  ADD CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user(id),
  ADD CONSTRAINT fk_chat_message_receiver FOREIGN KEY (receiver_id) REFERENCES user(id);

SET FOREIGN_KEY_CHECKS = 1;
```

### 4.3 结果验证
- 再次运行 `scripts/mysql_optimize_demo.sql`，对比 `scripts/mysql_optimize_demo.log` 的 BEFORE/AFTER
- 若需更细粒度，增加 `EXPLAIN ANALYZE`、`SHOW PROFILE` 或慢查询日志分析
- 测试“无外键”效果时可只执行步骤 1 + 2；验证通过后执行步骤 3 恢复结构

### 4.4 回滚与上线建议
- 保留 `mysqldump` 或 `SHOW CREATE TABLE` 备份，保证可随时回滚
- 若线上长期取消外键，请同步更新建表脚本（如 `SETUP_GUIDE.txt`），避免环境不一致
- 去掉外键后务必确认应用层已有完整的数据校验与级联清理逻辑


## 5. 案例：订单列表查询优�?
> 目的：展示“定位慢 SQL �?设计索引 �?验证效果”的完整闭环，可直接用于面试讲解�?
### 5.1 场景 & 原始 SQL
- 接口：订单列�?`/api/orders`，按买家 ID 过滤并按创建时间倒序�?- SQL�?  ```sql
  SELECT id, goods_id, buyer_id, created_at
  FROM orders
  WHERE buyer_id = ?
  ORDER BY created_at DESC
  LIMIT 20;
  ```
- 问题：缺少合适索引时 MySQL 扫描整张表并 Filesort，订单量增大后进入慢查询�?
### 5.2 基线验证（无索引�?1. 在影子库执行 `scripts/mysql_optimize_compare.sql`，脚本会删除相关索引、运行基线查询，并输出日志到 `scripts/mysql_optimize_compare.log`�?   ```bash
   cd scripts
   mysql -u root -p campus_market_opt < mysql_optimize_compare.sql > mysql_optimize_compare.log
   ```
2. 日志节选（基线）：
   ```
   -> Sort: orders.created_at DESC ... -> Table scan on orders (cost=0.55 rows=3)
   ```
   - `type=ALL`、`Extra=Using filesort`，说明发生了全表扫描+排序�?
### 5.3 优化方案
- 创建复合索引 `(buyer_id, created_at)`、保留单列索�?`(goods_id)`�?  ```sql
  ALTER TABLE orders
    ADD INDEX IDX_orders_buyer_created (buyer_id, created_at DESC),
    ADD INDEX IDX_orders_goods_id (goods_id);
  ```
- 理由：最左前缀满足 `buyer_id` 查找，同时利用索引顺序避免额外排序，覆盖查询列减少回表�?
### 5.4 效果验证
- `mysql_optimize_compare.sql` 会再次执行查询并记录优化后的计划�?  ```
  -> Index lookup on orders using IDX_orders_buyer_created (buyer_id=1001) (reverse)
  ```
  - `type=range`、无 `Using filesort`�?  - `actual time` �?~0.024ms 降至 ~0.006ms（日志中可见），性能提升�?3~4 倍�?- 可选：在真实数据上执行 `EXPLAIN ANALYZE`、`SHOW PROFILE` 验证�?
### 5.5 演示材料建议
- **脚本**：保�?`mysql_optimize_compare.sql`、`mysql_optimize_compare.log`�?- **执行计划对比**：截取优化前后的 `EXPLAIN`，重点强�?`type`、`key`、`rows`、`Extra` 的变化�?- **总结�?*�?  | 指标 | 优化�?| 优化�?|
  |------|--------|--------|
  | type | ALL | range |
  | Extra | Using filesort | （空�?|
  | rows | 全表扫描 | 索引命中 |
  | 耗时 | ~0.024ms | ~0.006ms |

### 5.6 面试讲解要点
1. 如何发现问题（慢查询日志 + `EXPLAIN`）�?2. 索引设计思路（复合索引、最左前缀、覆盖索引）�?3. 验证手段（脚本、`EXPLAIN ANALYZE`、性能对比表）�?4. 风险与维护（数据倾斜、索引冗余、上线前影子库验证、持续监控）�?*** End Patch

