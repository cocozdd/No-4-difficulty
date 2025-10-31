-- scripts/mysql_optimize_compare.sql
-- Usage (PowerShell):
--   cmd /c "mysql -u root -p campus_market_opt < scripts\mysql_optimize_compare.sql > scripts\mysql_optimize_compare.log"
-- 脚本流程：
--   1. 关闭外键检查并删除目标外键
--   2. 删除需要对比的索引
--   3. 采集“无索引”基线
--   4. 重新创建索引
--   5. 恢复外键
--   6. 采集“重建索引后”结果

USE campus_market_opt;
SET NAMES utf8mb4;

SET @current_db := DATABASE();

SELECT '--- PREPARE: DROP FOREIGN KEYS ---' AS info;
SET @original_fk_checks := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS drop_fk_if_exists;
DELIMITER $$
CREATE PROCEDURE drop_fk_if_exists(IN tbl VARCHAR(64), IN fk VARCHAR(64))
BEGIN
    DECLARE fk_count INT DEFAULT 0;
    SELECT COUNT(*) INTO fk_count
      FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = tbl
       AND constraint_name = fk
       AND constraint_type = 'FOREIGN KEY';

    IF fk_count > 0 THEN
        SET @sql := CONCAT('ALTER TABLE ', tbl, ' DROP FOREIGN KEY ', fk);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[INFO] dropped foreign key ', fk, ' on ', tbl) AS info;
    ELSE
        SELECT CONCAT('[INFO] skip foreign key ', fk, ' on ', tbl, ' (not present)') AS info;
    END IF;
END$$
DELIMITER ;

CALL drop_fk_if_exists('orders', 'fk_orders_goods');
CALL drop_fk_if_exists('orders', 'fk_orders_buyer');
CALL drop_fk_if_exists('cart_items', 'fk_cart_user');
CALL drop_fk_if_exists('cart_items', 'fk_cart_goods');
CALL drop_fk_if_exists('chat_message', 'fk_chat_message_sender');
CALL drop_fk_if_exists('chat_message', 'fk_chat_message_receiver');
CALL drop_fk_if_exists('goods', 'fk_goods_seller');
CALL drop_fk_if_exists('flash_sale_order', 'fk_flash_sale_user');

DROP PROCEDURE drop_fk_if_exists;
SET FOREIGN_KEY_CHECKS = @original_fk_checks;

SELECT '--- PREPARE: DROP INDEXES ---' AS info;

DROP PROCEDURE IF EXISTS drop_idx_if_exists;
DELIMITER $$
CREATE PROCEDURE drop_idx_if_exists(IN tbl VARCHAR(64), IN idx VARCHAR(64))
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = tbl
       AND index_name = idx;

    IF idx_count > 0 THEN
        SET @sql := CONCAT('ALTER TABLE ', tbl, ' DROP INDEX ', idx);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[INFO] dropped index ', idx, ' on ', tbl) AS info;
    ELSE
        SELECT CONCAT('[INFO] skip index ', idx, ' on ', tbl, ' (not present)') AS info;
    END IF;
END$$
DELIMITER ;

CALL drop_idx_if_exists('orders', 'IDX_orders_buyer_created');
CALL drop_idx_if_exists('orders', 'IDX_orders_goods_id');
CALL drop_idx_if_exists('chat_message', 'IDX_chat_message_pair_created');
CALL drop_idx_if_exists('goods', 'IDX_goods_status_category_published');
CALL drop_idx_if_exists('flash_sale_order', 'IDX_flash_sale_item_created');

DROP PROCEDURE drop_idx_if_exists;

SELECT '--- BASELINE (NO INDEX) ---' AS info;

SELECT 'orders_by_buyer_before' AS query, NOW() AS executed_at;
EXPLAIN ANALYZE
SELECT id, goods_id, buyer_id, created_at
FROM orders
WHERE buyer_id = 1001
ORDER BY created_at DESC
LIMIT 20;

SELECT 'chat_messages_before' AS query, NOW() AS executed_at;
EXPLAIN ANALYZE
SELECT sender_id, receiver_id, content, created_at
FROM chat_message
WHERE sender_id = 1001 AND receiver_id = 2002
ORDER BY created_at DESC
LIMIT 20;

SELECT '--- APPLY INDEXES ---' AS info;

DROP PROCEDURE IF EXISTS add_idx_if_missing;
DELIMITER $$
CREATE PROCEDURE add_idx_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl_sql TEXT)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = tbl
       AND index_name = idx;

    IF idx_count = 0 THEN
        SET @sql := ddl_sql;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[INFO] added index ', idx, ' on ', tbl) AS info;
    ELSE
        SELECT CONCAT('[INFO] skip add index ', idx, ' on ', tbl, ' (already exists)') AS info;
    END IF;
END$$
DELIMITER ;

CALL add_idx_if_missing('goods', 'IDX_goods_status_category_published',
    'ALTER TABLE goods ADD INDEX IDX_goods_status_category_published (status, category, published_at)');

CALL add_idx_if_missing('orders', 'IDX_orders_buyer_created',
    'ALTER TABLE orders ADD INDEX IDX_orders_buyer_created (buyer_id, created_at)');

CALL add_idx_if_missing('orders', 'IDX_orders_goods_id',
    'ALTER TABLE orders ADD INDEX IDX_orders_goods_id (goods_id)');

CALL add_idx_if_missing('chat_message', 'IDX_chat_message_pair_created',
    'ALTER TABLE chat_message ADD INDEX IDX_chat_message_pair_created (sender_id, receiver_id, created_at)');

CALL add_idx_if_missing('flash_sale_order', 'IDX_flash_sale_item_created',
    'ALTER TABLE flash_sale_order ADD INDEX IDX_flash_sale_item_created (flash_sale_item_id, created_at)');

DROP PROCEDURE add_idx_if_missing;

SELECT '--- RESTORE FOREIGN KEYS ---' AS info;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS add_fk_if_missing;
DELIMITER $$
CREATE PROCEDURE add_fk_if_missing(IN tbl VARCHAR(64), IN fk VARCHAR(64), IN ddl_sql TEXT)
BEGIN
    DECLARE fk_count INT DEFAULT 0;
    SELECT COUNT(*) INTO fk_count
      FROM information_schema.table_constraints
     WHERE table_schema = DATABASE()
       AND table_name = tbl
       AND constraint_name = fk
       AND constraint_type = 'FOREIGN KEY';

    IF fk_count = 0 THEN
        SET @sql := ddl_sql;
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[INFO] added foreign key ', fk, ' on ', tbl) AS info;
    ELSE
        SELECT CONCAT('[INFO] skip add foreign key ', fk, ' on ', tbl, ' (already exists)') AS info;
    END IF;
END$$
DELIMITER ;

CALL add_fk_if_missing('goods', 'fk_goods_seller',
    'ALTER TABLE goods ADD CONSTRAINT fk_goods_seller FOREIGN KEY (seller_id) REFERENCES user(id)');

CALL add_fk_if_missing('orders', 'fk_orders_goods',
    'ALTER TABLE orders ADD CONSTRAINT fk_orders_goods FOREIGN KEY (goods_id) REFERENCES goods(id)');

CALL add_fk_if_missing('orders', 'fk_orders_buyer',
    'ALTER TABLE orders ADD CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES user(id)');

CALL add_fk_if_missing('cart_items', 'fk_cart_user',
    'ALTER TABLE cart_items ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user(id)');

CALL add_fk_if_missing('cart_items', 'fk_cart_goods',
    'ALTER TABLE cart_items ADD CONSTRAINT fk_cart_goods FOREIGN KEY (goods_id) REFERENCES goods(id)');

CALL add_fk_if_missing('chat_message', 'fk_chat_message_sender',
    'ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES user(id)');

CALL add_fk_if_missing('chat_message', 'fk_chat_message_receiver',
    'ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_receiver FOREIGN KEY (receiver_id) REFERENCES user(id)');

CALL add_fk_if_missing('flash_sale_order', 'fk_flash_sale_user',
    'ALTER TABLE flash_sale_order ADD CONSTRAINT fk_flash_sale_user FOREIGN KEY (user_id) REFERENCES user(id)');

DROP PROCEDURE add_fk_if_missing;
SET FOREIGN_KEY_CHECKS = @original_fk_checks;

SELECT '--- AFTER INDEX ---' AS info;

SELECT 'orders_by_buyer_after' AS query, NOW() AS executed_at;
EXPLAIN ANALYZE
SELECT id, goods_id, buyer_id, created_at
FROM orders
WHERE buyer_id = 1001
ORDER BY created_at DESC
LIMIT 20;

SELECT 'chat_messages_after' AS query, NOW() AS executed_at;
EXPLAIN ANALYZE
SELECT sender_id, receiver_id, content, created_at
FROM chat_message
WHERE sender_id = 1001 AND receiver_id = 2002
ORDER BY created_at DESC
LIMIT 20;

SELECT '--- CURRENT INDEXES ---' AS info;
SHOW INDEX FROM orders;
SHOW INDEX FROM chat_message;
SHOW INDEX FROM goods;
SHOW INDEX FROM flash_sale_order;
