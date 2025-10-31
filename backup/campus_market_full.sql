-- MySQL dump 10.13  Distrib 8.0.31, for Win64 (x86_64)
--
-- Host: localhost    Database: campus_market
-- ------------------------------------------------------
-- Server version	8.0.31

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `goods_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user_goods` (`user_id`,`goods_id`),
  KEY `fk_cart_goods` (`goods_id`),
  CONSTRAINT `fk_cart_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`id`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
INSERT INTO `cart_items` VALUES (4,2,6,1,'2025-10-20 22:49:14','2025-10-20 22:49:14');
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_message`
--

DROP TABLE IF EXISTS `chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `content` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` datetime DEFAULT NULL,
  `message_type` varchar(16) NOT NULL DEFAULT 'TEXT',
  PRIMARY KEY (`id`),
  KEY `fk_chat_message_receiver` (`receiver_id`),
  KEY `IDX_chat_message_pair_created` (`sender_id`,`receiver_id`,`created_at`),
  CONSTRAINT `fk_chat_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_message`
--

LOCK TABLES `chat_message` WRITE;
/*!40000 ALTER TABLE `chat_message` DISABLE KEYS */;
INSERT INTO `chat_message` VALUES (1,2,1,'十大','2025-10-18 10:40:15','2025-10-18 15:42:37','TEXT'),(2,2,1,'的撒','2025-10-18 10:42:06','2025-10-18 15:42:37','TEXT'),(3,2,3,'的撒的撒','2025-10-18 10:44:24','2025-10-18 10:54:32','TEXT'),(4,2,3,'十大撒旦','2025-10-18 10:44:43','2025-10-18 10:54:32','TEXT'),(5,2,3,'第三方','2025-10-18 10:54:28','2025-10-18 10:54:32','TEXT'),(6,3,2,'dfs','2025-10-18 10:54:38','2025-10-18 10:54:38','TEXT'),(7,3,2,'fds','2025-10-18 10:54:54','2025-10-18 10:55:16','TEXT'),(8,2,3,'https://localhost:9000/campus-market-chat/chat/2025/10/d60983e2-4b7a-48fe-a5eb-5609c9413852.png','2025-10-18 14:05:20','2025-10-18 14:07:46','IMAGE'),(9,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/82261040-d60e-4c97-8101-a4469548ef3f.png','2025-10-18 14:06:41','2025-10-18 14:07:46','IMAGE'),(10,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/52f5cdd0-fae0-4d03-9ce2-4cbc79e20a66.png','2025-10-18 14:06:48','2025-10-18 14:07:46','IMAGE'),(11,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/622f2b35-d8ca-46c6-9256-77e800ba4487.png','2025-10-18 14:07:06','2025-10-18 14:07:46','IMAGE'),(12,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/33a680e5-c6dc-47f9-b852-18d70dbe802f.png','2025-10-18 14:10:02','2025-10-18 15:26:16','IMAGE'),(13,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/d89d9080-af73-45a5-a8be-93382165675f.png','2025-10-18 14:14:22','2025-10-18 15:26:16','IMAGE'),(14,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/febae63b-26ff-4d1c-b36e-60163b56482d.png','2025-10-18 14:14:28','2025-10-18 15:26:16','IMAGE'),(15,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/fbef15f6-9137-480c-a51f-476daa066cea.png','2025-10-18 14:17:10','2025-10-18 15:26:16','IMAGE'),(16,2,3,'123','2025-10-18 14:17:11','2025-10-18 15:26:16','TEXT'),(17,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/651ee2ee-f98a-4202-b583-1323a6891db0.png','2025-10-18 14:22:09','2025-10-18 15:26:16','IMAGE'),(18,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/70200f5c-b854-46c9-a1b8-897cb189c8a8.png','2025-10-18 14:24:26','2025-10-18 15:26:16','IMAGE'),(19,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/d223a8db-0ce0-4f96-a2dd-2c0081110932.png','2025-10-18 14:58:17','2025-10-18 15:26:16','IMAGE'),(20,2,3,'http://localhost:9000/campus-market-chat/chat/2025/10/88fc0215-ae39-4d5a-a7e7-ba677dcfe76f.png','2025-10-18 15:00:38','2025-10-18 15:26:16','IMAGE');
/*!40000 ALTER TABLE `chat_message` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flash_sale_item`
--

DROP TABLE IF EXISTS `flash_sale_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flash_sale_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `description` text,
  `original_price` decimal(10,2) NOT NULL,
  `flash_price` decimal(10,2) NOT NULL,
  `total_stock` int NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'SCHEDULED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flash_sale_item`
--

LOCK TABLES `flash_sale_item` WRITE;
/*!40000 ALTER TABLE `flash_sale_item` DISABLE KEYS */;
INSERT INTO `flash_sale_item` VALUES (1,'AirPods 秒杀专场','限量 5 副，先到先得',1299.00,699.00,5,'2025-10-26 20:03:00','2025-10-31 00:00:00','SCHEDULED','2025-10-26 14:58:30','2025-10-26 14:58:30'),(2,'123','',12.00,1.00,3,'2025-10-26 15:01:00','2025-10-31 00:00:00','SCHEDULED','2025-10-26 15:00:10','2025-10-26 15:00:10');
/*!40000 ALTER TABLE `flash_sale_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flash_sale_order`
--

DROP TABLE IF EXISTS `flash_sale_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flash_sale_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `flash_sale_item_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PREPARING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flash_sale_user` (`flash_sale_item_id`,`user_id`),
  KEY `fk_flash_sale_user` (`user_id`),
  KEY `IDX_flash_sale_item_created` (`flash_sale_item_id`,`created_at`),
  CONSTRAINT `fk_flash_sale_item` FOREIGN KEY (`flash_sale_item_id`) REFERENCES `flash_sale_item` (`id`),
  CONSTRAINT `fk_flash_sale_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flash_sale_order`
--

LOCK TABLES `flash_sale_order` WRITE;
/*!40000 ALTER TABLE `flash_sale_order` DISABLE KEYS */;
INSERT INTO `flash_sale_order` VALUES (1,2,2,'PREPARING','2025-10-26 15:01:42','2025-10-26 15:01:42');
/*!40000 ALTER TABLE `flash_sale_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `goods`
--

DROP TABLE IF EXISTS `goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(128) NOT NULL,
  `description` text,
  `category` varchar(64) DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `cover_image_url` varchar(255) DEFAULT NULL,
  `seller_id` bigint NOT NULL,
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_sold` tinyint(1) NOT NULL DEFAULT '0',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  PRIMARY KEY (`id`),
  KEY `fk_goods_seller` (`seller_id`),
  KEY `IDX_goods_status_category_published` (`status`,`category`,`published_at`),
  CONSTRAINT `fk_goods_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `goods`
--

LOCK TABLES `goods` WRITE;
/*!40000 ALTER TABLE `goods` DISABLE KEYS */;
INSERT INTO `goods` VALUES (1,'Second-hand Laptop','95% new ultrabook, perfect for course work','Electronics',3200.00,1,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Laptop',1,'2025-10-12 19:41:40',0,1,'APPROVED'),(2,'Linear Algebra Textbook','No notes, no damage','Books',25.00,1,'https://dummyimage.com/600x360/34d399/ffffff.png&text=Book',1,'2025-10-12 19:41:39',0,1,'APPROVED'),(3,'Campus Bicycle','Well maintained, lock included','Daily',280.00,1,'https://dummyimage.com/600x360/f97316/ffffff.png&text=Bike',1,'2025-10-12 19:41:39',0,0,'APPROVED'),(4,'123','1235434324','Electronics',60.00,1,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics',2,'2025-10-18 09:43:49',0,1,'APPROVED'),(5,'44','335','Books',1.00,1,'https://dummyimage.com/600x360/34d399/ffffff.png&text=Books',2,'2025-10-18 10:18:31',0,0,'APPROVED'),(6,'124324','342','Books',1.00,3,'http://localhost:9000/campus-market-goods/goods/2025/10/e4825d35-3d1a-46f6-9f57-f80f7a4d3690.jpg',3,'2025-10-18 15:42:42',0,0,'APPROVED'),(7,'1','1','Electronics',1.00,0,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics',3,'2025-10-18 15:42:41',1,0,'APPROVED'),(8,'2','2','Electronics',1.00,0,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics',3,'2025-10-18 15:42:41',1,0,'APPROVED'),(9,'3','3','Electronics',1.00,1,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics',3,'2025-10-18 15:42:42',0,0,'APPROVED'),(10,'4','4','Electronics',1.00,1,'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics',3,'2025-10-18 15:42:40',0,0,'APPROVED');
/*!40000 ALTER TABLE `goods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `goods_id` bigint NOT NULL,
  `buyer_id` bigint NOT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `IDX_orders_buyer_created` (`buyer_id`,`created_at`),
  KEY `IDX_orders_goods_id` (`goods_id`),
  CONSTRAINT `fk_orders_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_orders_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (1,7,2,'COMPLETED','2025-10-20 22:37:21','2025-10-20 22:42:46'),(2,8,2,'COMPLETED','2025-10-20 22:40:16','2025-10-20 22:42:45'),(3,6,2,'PENDING_PAYMENT','2025-10-20 22:49:02','2025-10-20 22:49:02');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(32) NOT NULL DEFAULT 'STUDENT',
  `nickname` varchar(64) NOT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$7JNPZH08V2ioB8RFwEGYpOlzkFHobM7AWaPAvL8LB38iDeceEZo9y','ADMIN','管理员',NULL,'13800000000','2025-10-12 18:09:44'),(2,'123','$2a$10$MxUQJR3HGDNHLKVUNr5dhu8cdt.k0f5FetCgHB4Bul7UM4i85bzMu','STUDENT','123',NULL,'13333333333','2025-10-12 18:10:03'),(3,'1234','$2a$10$35pdT.yWVvBzIW/r0rNmIeS7AUBKpfxLjzvDg.eb18ud60csF6cp2','STUDENT','1234',NULL,'13333333333','2025-10-18 10:43:43'),(4,'student1','$2a$10$szFQz0DoTxLrh30f6PTkiuzwKitTCJIAqPmi5h0kXtj75u6EnpH7e','STUDENT','student1',NULL,'13333333333','2025-10-26 16:03:09'),(5,'student2','$2a$10$UOruTVRnwx2VBSXWmt9FEu9OdY6jIxrS0ISshxRK8GZ5U9xuJDGRC','STUDENT','student2',NULL,'13333333333','2025-10-26 16:03:29');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-29 14:39:27
