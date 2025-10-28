-- MySQL optional indexes for Campus Market

-- 首页/列表筛选：按状态 + 类目 + 发布时间
ALTER TABLE goods
  ADD INDEX IDX_goods_status_category_published (status, category, published_at);

-- 订单查询：买家视角分页
ALTER TABLE orders
  ADD INDEX IDX_orders_buyer_created (buyer_id, created_at);

-- 订单详情：根据商品查询
ALTER TABLE orders
  ADD INDEX IDX_orders_goods_id (goods_id);

-- 聊天窗口：基于双方 + 时间排序
ALTER TABLE chat_message
  ADD INDEX IDX_chat_message_pair_created (sender_id, receiver_id, created_at);

-- 秒杀订单：统计/回查
ALTER TABLE flash_sale_order
  ADD INDEX IDX_flash_sale_item_created (flash_sale_item_id, created_at);
