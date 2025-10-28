# 秒杀（Flash Sale）功能说明

## 数据表

```sql
CREATE TABLE IF NOT EXISTS flash_sale_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL,
  description TEXT,
  original_price DECIMAL(10,2) NOT NULL,
  flash_price DECIMAL(10,2) NOT NULL,
  total_stock INT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS flash_sale_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  flash_sale_item_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PREPARING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_flash_sale_user (flash_sale_item_id, user_id),
  CONSTRAINT fk_flash_sale_item FOREIGN KEY (flash_sale_item_id) REFERENCES flash_sale_item(id),
  CONSTRAINT fk_flash_sale_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> 建议优先执行以上 DDL，随后重启后端服务。

## Redis Key 约定

| Key | 说明 | TTL |
|-----|------|-----|
| `flash:item:{id}` | 活动基础信息 JSON | 到活动结束后再多保留 6 小时 |
| `flash:stock:{id}` | 剩余库存，使用 `DECR`/`INCR` 原子操作 | 到活动结束后多保留 1 小时 |
| `flash:user:{eventId}:{userId}` | 防止同一用户重复抢购 | 活动截止时间 + 5 分钟 |

## API

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| `POST` | `/api/flash-sale/items` | 管理员（`ROLE_ADMIN`） | 新建秒杀活动 |
| `GET` | `/api/flash-sale/items` | 公开 | 查看进行中/即将开始的活动 |
| `POST` | `/api/flash-sale/purchase` | 登录用户 | 发起秒杀请求，成功返回抢购订单 ID |

## 流程概述

1. 管理员调用 `POST /api/flash-sale/items` 新建活动，服务会持久化数据并预热 Redis 库存与活动信息。  
2. 用户访问 `GET /api/flash-sale/items` 拉取活动列表（接口已开放匿名访问，便于提前查看）。  
3. 秒杀开始后，用户调用 `POST /api/flash-sale/purchase`：  
   - 先在 Redis 中抢占用户标记，防止重复请求；  
   - `DECR` 库存，库存不足则立刻回滚并提示售罄；  
   - 创建 `flash_sale_order` 记录（状态 `PREPARING`），供后续扩展异步下单等逻辑。  

后续可以继续完善：异步写正式订单、补偿任务、失败回滚、热点商品互斥锁、压测与限流等。***
