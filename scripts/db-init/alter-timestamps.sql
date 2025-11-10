-- Run this script inside the Postgres container to convert TIMESTAMPTZ columns
-- to TIMESTAMP while preserving existing data.
-- Execute with:
-- docker compose exec -T postgres psql -U campus_user -d campus_market -f /docker-entrypoint-initdb.d/alter-timestamps.sql

ALTER TABLE users
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';

ALTER TABLE goods
  ALTER COLUMN published_at TYPE TIMESTAMP USING published_at AT TIME ZONE 'UTC';

ALTER TABLE cart_items
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE chat_message
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN read_at TYPE TIMESTAMP USING read_at AT TIME ZONE 'UTC';

ALTER TABLE flash_sale_item
  ALTER COLUMN start_time TYPE TIMESTAMP USING start_time AT TIME ZONE 'UTC',
  ALTER COLUMN end_time   TYPE TIMESTAMP USING end_time   AT TIME ZONE 'UTC',
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE flash_sale_order
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE orders
  ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';
