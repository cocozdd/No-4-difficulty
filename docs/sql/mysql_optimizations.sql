-- docs/sql/mysql_optimizations.sql
-- 作用：在创建索引前先检查是否已存在，避免重复执行报错。

USE campus_market;

SET @msg := 'Applying optional indexes…';
SELECT @msg AS info;

-- goods: (status, category, published_at)
SET @sql := (
  SELECT IF(COUNT(*) = 0,
             'ALTER TABLE goods ADD INDEX IDX_goods_status_category_published (status, category, published_at)',
             'SELECT "INDEX IDX_goods_status_category_published already exists" AS note')
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'goods'
     AND index_name = 'IDX_goods_status_category_published');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- orders: (buyer_id, created_at)
SET @sql := (
  SELECT IF(COUNT(*) = 0,
             'ALTER TABLE orders ADD INDEX IDX_orders_buyer_created (buyer_id, created_at)',
             'SELECT "INDEX IDX_orders_buyer_created already exists" AS note')
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'orders'
     AND index_name = 'IDX_orders_buyer_created');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- orders: goods_id
SET @sql := (
  SELECT IF(COUNT(*) = 0,
             'ALTER TABLE orders ADD INDEX IDX_orders_goods_id (goods_id)',
             'SELECT "INDEX IDX_orders_goods_id already exists" AS note')
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'orders'
     AND index_name = 'IDX_orders_goods_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- chat_message: (sender_id, receiver_id, created_at)
SET @sql := (
  SELECT IF(COUNT(*) = 0,
             'ALTER TABLE chat_message ADD INDEX IDX_chat_message_pair_created (sender_id, receiver_id, created_at)',
             'SELECT "INDEX IDX_chat_message_pair_created already exists" AS note')
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'chat_message'
     AND index_name = 'IDX_chat_message_pair_created');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- flash_sale_order: (flash_sale_item_id, created_at)
SET @sql := (
  SELECT IF(COUNT(*) = 0,
             'ALTER TABLE flash_sale_order ADD INDEX IDX_flash_sale_item_created (flash_sale_item_id, created_at)',
             'SELECT "INDEX IDX_flash_sale_item_created already exists" AS note')
    FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'flash_sale_order'
     AND index_name = 'IDX_flash_sale_item_created');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'done' AS info;
