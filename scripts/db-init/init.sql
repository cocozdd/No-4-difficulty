-- Base schema initialization (optional)
CREATE DATABASE IF NOT EXISTS campus_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'campus_user'@'%' IDENTIFIED BY 'campus_pass';
GRANT ALL PRIVILEGES ON campus_market.* TO 'campus_user'@'%';
FLUSH PRIVILEGES;
