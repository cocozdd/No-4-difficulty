-- scripts/mysql_optimize_demo.sql (safe version)
-- 用法：mysql -u root -p campus_market < scripts/mysql_optimize_demo.sql > scripts/mysql_optimize_demo.log

USE campus_market;
SET max_execution_time = 0; -- allow full execution

DROP PROCEDURE IF EXISTS drop_idx_safe;
DELIMITER $$
CREATE PROCEDURE drop_idx_safe(IN tbl VARCHAR(64), IN idx VARCHAR(64))
BEGIN
    DECLARE idx_exist INT DEFAULT 0;

    SELECT COUNT(*) INTO idx_exist
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = tbl
       AND index_name = idx;

    IF idx_exist > 0 THEN
        SET @sql := CONCAT('ALTER TABLE ', tbl, ' DROP INDEX ', idx);
        BEGIN
            DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN
                SELECT CONCAT('[WARN] skip drop ', idx, ' on ', tbl, ' (maybe referenced by FK)') AS warning_message;
            END;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END;
    END IF;
END$$
DELIMITER ;

CALL drop_idx_safe('orders', 'IDX_orders_buyer_created');
CALL drop_idx_safe('orders', 'IDX_orders_goods_id');
CALL drop_idx_safe('chat_message', 'IDX_chat_message_pair_created');
CALL drop_idx_safe('goods', 'IDX_goods_status_category_published');
CALL drop_idx_safe('flash_sale_order', 'IDX_flash_sale_item_created');
DROP PROCEDURE drop_idx_safe;

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
SOURCE docs/sql/mysql_optimizations.sql;

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
