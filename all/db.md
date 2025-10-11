/*M!999999- enable the sandbox mode */
-- MariaDB dump 10.19-11.8.3-MariaDB, for Win64 (AMD64)
--
-- Host: localhost      Database: hotel
-- ------------------------------------------------------
-- Server version 11.8.3-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `amenity`
--

DROP TABLE IF EXISTS `amenity`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `amenity` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` longtext DEFAULT NULL,
  `icon_url` varchar(255) DEFAULT NULL,
  `fee_type` enum('FREE','PAID','HOURLY') NOT NULL DEFAULT 'FREE',
  `fee_amount` int(11) DEFAULT NULL,
  `fee_unit` varchar(50) DEFAULT NULL,
  `operating_hours` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `category` enum('IN_ROOM','IN_HOTEL','LEISURE','FNB','BUSINESS','OTHER') NOT NULL DEFAULT 'OTHER',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `amenity`
--

LOCK TABLES `amenity` WRITE;
/*!40000 ALTER TABLE `amenity` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `amenity` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `app_user`
--

DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `date_of_birth` date NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `created_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `role` enum('USER','ADMIN','BUSINESS') NOT NULL DEFAULT 'USER',
  `is_active` bit(1) NOT NULL,
  `profile_image_url` varchar(255) DEFAULT NULL,
  `provider` enum('GOOGLE','KAKAO','LOCAL','NAVER') NOT NULL,
  `provider_id` varchar(255) DEFAULT NULL,
  `social_providers` text DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_email` (`email`),
  UNIQUE KEY `uq_user_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `app_user`
--

LOCK TABLES `app_user` WRITE;
/*!40000 ALTER TABLE `app_user` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `app_user` VALUES
(1,'김민수','01012345678','minsu.kim@example.com','$2a$10$example.hash.password','1990-05-15','서울특별시 강남구 역삼동','2025-09-26 04:44:24','USER',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(2,'이영희','01023456789','younghee.lee@example.com','$2a$10$example.hash.password','1985-08-22','서울특별시 서초구 서초동','2025-09-26 04:44:24','USER',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(3,'박철수','01034567890','chulsoo.park@example.com','$2a$10$example.hash.password','1992-03-10','부산광역시 해운대구','2025-09-26 04:44:24','USER',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(4,'최지현','01045678901','jihyun.choi@example.com','$2a$10$example.hash.password','1988-12-05','대구광역시 수성구','2025-09-26 04:44:24','USER',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(5,'홍길동','01056789012','gildong.hong@example.com','$2a$10$example.hash.password','1995-07-18','광주광역시 서구','2025-09-26 04:44:24','USER',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(6,'호텔왕','01098765432','hotelking@business.com','$2a$10$example.hash.password','1975-03-20','서울특별시 중구 명동','2025-09-26 04:44:24','BUSINESS',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(7,'리조트대표','01087654321','resort@business.com','$2a$10$example.hash.password','1980-09-15','제주특별자치도 제주시','2025-09-26 04:44:24','BUSINESS',0x01,NULL,'LOCAL',NULL,'{}',NULL),
(8,'Site Admin','010-0000-0001','hotel@hotel.com','$2a$10$nIVWz..lfmAOo9jNKIx8u.3L9gAeSqYXAtuI4dtW4e1Hc6Eu2VRxK','1985-01-01','Seoul','2025-09-26 04:44:47','ADMIN',0x01,NULL,'LOCAL',NULL,'{}','2025-10-01 10:59:00.034179'),
(9,'홍길동','010-2222-4455','1111@gmail.com','$2a$10$iTYJtpC8.ndLTA.FzSXi1uTk0.suJwUYo4cToLC8qUGtKdrANpKje','2008-02-27','서울','2025-09-26 08:53:47','BUSINESS',0x01,NULL,'LOCAL',NULL,'{}','2025-09-29 00:17:46.042476'),
(10,'호길동','010-3344-1133','spvkf9030@gmail.com','$2a$10$TR4DK8SIAuoFdIgPPzAyoe1wN0Fh8VsCIgdckyfWBIVglC9LjRjUa','2000-11-16','서울','2025-10-01 11:21:54','USER',0x01,NULL,'LOCAL',NULL,'{}','2025-10-01 20:21:54.537198');
/*!40000 ALTER TABLE `app_user` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `coupon`
--

DROP TABLE IF EXISTS `coupon`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `discount_type` enum('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` int(11) NOT NULL,
  `min_spend` int(11) NOT NULL DEFAULT 0,
  `valid_from` datetime NOT NULL,
  `valid_to` datetime DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coupon_code` (`code`),
  KEY `idx_coupon_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Coupon_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon`
--

LOCK TABLES `coupon` WRITE;
/*!40000 ALTER TABLE `coupon` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `coupon` VALUES
(2,2,'2','2','PERCENTAGE',22,0,'2025-09-11 05:10:00','2025-10-03 14:10:00',1),
(7,2,'22','22','PERCENTAGE',22,2222,'2025-09-03 15:50:00','2025-09-10 15:50:00',1);
/*!40000 ALTER TABLE `coupon` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `hotel`
--

DROP TABLE IF EXISTS `hotel`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `business_id` bigint(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `address` varchar(255) NOT NULL,
  `star_rating` int(11) NOT NULL,
  `description` longtext DEFAULT NULL,
  `country` varchar(50) NOT NULL,
  `approval_date` datetime(6) DEFAULT NULL,
  `approval_status` enum('APPROVED','PENDING','REJECTED','SUSPENDED') NOT NULL,
  `approved_by` bigint(20) DEFAULT NULL,
  `rejection_reason` tinytext DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_hotel_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Hotel_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotel`
--

LOCK TABLES `hotel` WRITE;
/*!40000 ALTER TABLE `hotel` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `hotel` VALUES
(1,6,1234567890,'그랜드 서울 호텔','서울특별시 중구 명동길 123',5,'서울 중심가에 위치한 5성급 럭셔리 호텔입니다. 최고의 서비스와 편의시설을 제공합니다.','한국',NULL,'APPROVED',NULL,NULL,'2025-09-26 13:44:24.000000'),
(2,7,9876543210,'제주 오션뷰 리조트','제주특별자치도 제주시 애월읍 해안로 456',4,'제주 바다를 한눈에 내려다보는 최고의 리조트입니다. 가족 단위 여행객에게 인기가 많습니다.','한국',NULL,'APPROVED',NULL,NULL,'2025-09-26 13:44:24.000000'),
(3,9,111112222,'사업자','경기도',0,'사업자 등록 신청','한국','2025-09-26 18:30:07.441023','APPROVED',NULL,NULL,'2025-09-26 18:29:51.526572');
/*!40000 ALTER TABLE `hotel` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `hotel_amenity`
--

DROP TABLE IF EXISTS `hotel_amenity`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotel_amenity` (
  `hotel_id` bigint(20) NOT NULL,
  `amenity_id` bigint(20) NOT NULL,
  PRIMARY KEY (`hotel_id`,`amenity_id`),
  KEY `idx_ha_amenity` (`amenity_id`),
  CONSTRAINT `FK_Amenity_TO_Hotel_Amenity_1` FOREIGN KEY (`amenity_id`) REFERENCES `amenity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_Hotel_TO_Hotel_Amenity_1` FOREIGN KEY (`hotel_id`) REFERENCES `hotel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotel_amenity`
--

LOCK TABLES `hotel_amenity` WRITE;
/*!40000 ALTER TABLE `hotel_amenity` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `hotel_amenity` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `hotel_image`
--

DROP TABLE IF EXISTS `hotel_image`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hotel_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hotel_id` bigint(20) NOT NULL,
  `url` tinytext NOT NULL,
  `sort_no` int(11) NOT NULL DEFAULT 0,
  `is_cover` tinyint(1) NOT NULL DEFAULT 0,
  `caption` varchar(255) DEFAULT NULL,
  `alt_text` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_hotel_sort` (`hotel_id`,`sort_no`),
  KEY `idx_himg_hotel` (`hotel_id`),
  CONSTRAINT `fk_himg_hotel` FOREIGN KEY (`hotel_id`) REFERENCES `hotel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hotel_image`
--

LOCK TABLES `hotel_image` WRITE;
/*!40000 ALTER TABLE `hotel_image` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `hotel_image` VALUES
(1,1,'https://images.example.com/hotel1-exterior-cover.jpg',0,1,'그랜드 서울 호텔 외관','그랜드 서울 호텔 정면 외관'),
(2,1,'https://images.example.com/hotel1-lobby.jpg',1,0,'그랜드 서울 호텔 로비','그랜드 서울 호텔 럭셔리 로비'),
(3,1,'https://images.example.com/hotel1-restaurant.jpg',2,0,'그랜드 서울 호텔 레스토랑','그랜드 서울 호텔 파인다이닝 레스토랑'),
(4,1,'https://images.example.com/hotel1-pool.jpg',3,0,'그랜드 서울 호텔 수영장','그랜드 서울 호텔 실내 수영장'),
(5,2,'https://images.example.com/hotel2-exterior-cover.jpg',0,1,'제주 오션뷰 리조트 외관','제주 오션뷰 리조트 바다 전망 외관'),
(6,2,'https://images.example.com/hotel2-beach.jpg',1,0,'제주 오션뷰 리조트 프라이빗 해변','제주 오션뷰 리조트 전용 해변'),
(7,2,'https://images.example.com/hotel2-infinity-pool.jpg',2,0,'제주 오션뷰 리조트 인피니티 풀','제주 오션뷰 리조트 인피니티 풀'),
(8,2,'https://images.example.com/hotel2-sunset.jpg',3,0,'제주 오션뷰 리조트 일몰','제주 오션뷰 리조트에서 바라본 아름다운 일몰');
/*!40000 ALTER TABLE `hotel_image` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `inquiry`
--

DROP TABLE IF EXISTS `inquiry`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `inquiry` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `replied_at` datetime(6) DEFAULT NULL,
  `reply` text DEFAULT NULL,
  `status` enum('ANSWERED','CLOSED','PENDING') NOT NULL,
  `subject` varchar(255) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inquiry_user` (`user_id`),
  CONSTRAINT `FK_Inquiry_User` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inquiry`
--

LOCK TABLES `inquiry` WRITE;
/*!40000 ALTER TABLE `inquiry` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `inquiry` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `content` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_pinned` bit(1) NOT NULL,
  `title` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
set autocommit=0;
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint(20) NOT NULL,
  `payment_method` varchar(50) NOT NULL,
  `base_price` int(11) NOT NULL,
  `total_price` int(11) NOT NULL,
  `tax` int(11) NOT NULL DEFAULT 0,
  `discount` int(11) NOT NULL DEFAULT 0,
  `status` enum('PENDING','COMPLETED','CANCELLED','FAILED') NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `refunded_at` timestamp NULL DEFAULT NULL,
  `receipt_url` varchar(512) DEFAULT NULL,
  `amount` int(11) DEFAULT NULL,
  `canceled_at` datetime(6) DEFAULT NULL,
  `order_id` varchar(255) DEFAULT NULL,
  `order_name` varchar(255) DEFAULT NULL,
  `payment_key` varchar(255) DEFAULT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_receipt_url` (`receipt_url`),
  KEY `idx_pay_res` (`reservation_id`),
  KEY `FKjs1fo45o9f0ld1sgn2023tgy7` (`user_id`),
  CONSTRAINT `FK_Reservation_TO_Payment_1` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`),
  CONSTRAINT `FKjs1fo45o9f0ld1sgn2023tgy7` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `payment` (`id`, `reservation_id`, `payment_method`, `base_price`, `total_price`, `tax`, `discount`, `status`, `created_at`, `refunded_at`, `receipt_url`, `amount`, `canceled_at`, `order_id`, `order_name`, `payment_key`, `user_id`) VALUES
(1,1,'CREDIT_CARD',360000,390000,30000,0,'COMPLETED','2024-01-10 01:35:00',NULL,'https://receipt.example.com/TXN-2024-0001',NULL,NULL,NULL,NULL,NULL,1),
(2,2,'KAKAO_PAY',840000,882000,42000,0,'COMPLETED','2024-02-15 05:25:00',NULL,'https://receipt.example.com/TXN-2024-0002',NULL,NULL,NULL,NULL,NULL,2),
(3,3,'NAVER_PAY',900000,945000,45000,0,'COMPLETED','2024-03-05 00:20:00',NULL,'https://receipt.example.com/TXN-2024-0003',NULL,NULL,NULL,NULL,NULL,1),
(4,4,'CREDIT_CARD',1520000,1596000,76000,0,'COMPLETED','2024-12-20 07:50:00',NULL,'https://receipt.example.com/TXN-2024-0004',NULL,NULL,NULL,NULL,NULL,3),
(5,5,'TOSS_PAY',390000,409500,19500,0,'COMPLETED','2024-12-27 02:35:00',NULL,'https://receipt.example.com/TXN-2024-0005',NULL,NULL,NULL,NULL,NULL,2),
(6,6,'CREDIT_CARD',1950000,2047500,97500,0,'FAILED','2024-03-27 23:25:00','2024-04-02 06:30:00','https://receipt.example.com/TXN-2024-0006',NULL,NULL,NULL,NULL,NULL,1),
(7,7,'BANK_TRANSFER',180000,189000,9000,0,'FAILED','2024-05-01 04:15:00','2024-05-10 00:45:00','https://receipt.example.com/TXN-2024-0007',NULL,NULL,NULL,NULL,NULL,2),
(8,8,'CREDIT_CARD',840000,882000,42000,0,'CANCELLED','2024-12-28 01:05:00',NULL,'https://receipt.example.com/TXN-2024-0008',NULL,NULL,NULL,NULL,NULL,3),
(9,9,'KAKAO_PAY',900000,945000,45000,0,'FAILED','2024-12-28 06:35:00','2025-09-26 09:05:49','https://receipt.example.com/TXN-2024-0009',NULL,NULL,NULL,NULL,NULL,4),
(10,10,'PAYPAL',1520000,1596000,76000,0,'FAILED','2024-12-28 11:20:00','2025-09-26 04:59:56','https://receipt.example.com/TXN-2024-0010',NULL,NULL,NULL,NULL,NULL,5),
(11,11,'TOSS_PAY',1000,1000,0,0,'PENDING','2025-10-01 11:26:31',NULL,NULL,1000,NULL,'2510012026H1R1','호텔이름 확인필요','',10),
(12,12,'TOSS_PAY',1000,1000,0,0,'CANCELLED','2025-10-01 11:30:45',NULL,'https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId=tgen_20251001203050zVmu2&ref=PX',1000,NULL,'2510012030H1R1','호텔이름 확인필요','tgen_20251001203050zVmu2',10);
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `reservation`
--

DROP TABLE IF EXISTS `reservation`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `reservation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `room_id` bigint(20) NOT NULL,
  `transaction_id` varchar(255) DEFAULT NULL,
  `num_adult` int(11) NOT NULL DEFAULT 0,
  `num_kid` int(11) NOT NULL DEFAULT 0,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PENDING','COMPLETED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
  `expires_at` timestamp NULL DEFAULT NULL,
  `num_rooms` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_res_user` (`user_id`),
  KEY `idx_res_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Reservation_1` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `FK_User_TO_Reservation_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservation`
--

LOCK TABLES `reservation` WRITE;
/*!40000 ALTER TABLE `reservation` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `reservation` VALUES
(1,1,1,'TXN-2024-0001',2,0,'2024-01-15 06:00:00','2024-01-17 02:00:00','2024-01-10 01:30:00','COMPLETED',NULL,1),
(2,2,4,'TXN-2024-0002',2,1,'2024-02-20 07:00:00','2024-02-23 02:00:00','2024-02-15 05:20:00','COMPLETED',NULL,1),
(3,3,2,'TXN-2024-0003',3,1,'2024-03-10 06:00:00','2024-03-12 02:00:00','2024-03-05 00:15:00','COMPLETED',NULL,1),
(4,4,5,'TXN-2024-0004',4,2,'2024-12-25 07:00:00','2024-12-27 02:00:00','2024-12-20 07:45:00','CANCELLED','2024-12-24 14:59:59',1),
(5,5,3,'TXN-2024-0005',1,0,'2024-12-30 06:00:00','2025-01-02 02:00:00','2024-12-27 02:30:00','CANCELLED','2024-12-29 14:59:59',1),
(6,1,6,'TXN-2024-0006',2,0,'2024-04-05 07:00:00','2024-04-08 02:00:00','2024-03-27 23:20:00','CANCELLED',NULL,1),
(7,2,1,'TXN-2024-0007',1,0,'2024-05-12 06:00:00','2024-05-14 02:00:00','2024-05-01 04:10:00','CANCELLED',NULL,1),
(8,3,4,'TXN-2024-0008',3,0,'2025-01-15 07:00:00','2025-01-18 02:00:00','2024-12-28 01:00:00','CANCELLED','2025-01-14 14:59:59',1),
(9,4,2,'TXN-2024-0009',2,0,'2025-02-10 06:00:00','2025-02-12 02:00:00','2024-12-28 06:30:00','CANCELLED','2025-02-09 14:59:59',1),
(10,5,5,'TXN-2024-0010',4,1,'2025-03-20 07:00:00','2025-03-24 02:00:00','2024-12-28 11:15:00','CANCELLED','2025-03-19 14:59:59',1),
(11,10,1,NULL,1,0,'2025-10-15 15:00:00','2025-10-22 15:00:00','2025-10-01 11:26:01','CANCELLED','2025-10-01 02:27:00',1),
(12,10,1,'tgen_20251001203050zVmu2',1,0,'2025-10-07 15:00:00','2025-10-14 15:00:00','2025-10-01 11:30:27','COMPLETED','2025-10-01 02:31:27',1);
/*!40000 ALTER TABLE `reservation` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint(20) NOT NULL,
  `wrote_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `star_rating` int(11) NOT NULL DEFAULT 5,
  `content` longtext DEFAULT NULL,
  `image` longtext DEFAULT NULL,
  `admin_reply` tinytext DEFAULT NULL,
  `is_hidden` bit(1) NOT NULL,
  `is_reported` bit(1) NOT NULL,
  `replied_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_reservation` (`reservation_id`),
  CONSTRAINT `FK_Reservation_TO_Review_1` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `review` VALUES
(1,1,'2024-01-18 05:30:00',5,'정말 만족스러운 숙박이었습니다! 직원분들이 친절하고 시설도 깨끗했어요. 한강뷰가 정말 아름다웠습니다. 다음에도 꼭 재방문하겠습니다.','https://images.example.com/review-001.jpg',NULL,0x00,0x00,NULL),
(2,2,'2024-02-24 01:15:00',4,'제주 바다 전망이 환상적이었어요! 아이들이 정말 좋아했습니다. 다만 조식 메뉴가 조금 아쉬웠어요. 그래도 전반적으로 만족합니다.','https://images.example.com/review-002.jpg',NULL,0x00,0x00,NULL);
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hotel_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `room_size` varchar(50) NOT NULL,
  `capacity_min` int(11) NOT NULL,
  `capacity_max` int(11) NOT NULL,
  `check_in_time` time NOT NULL,
  `check_out_time` time NOT NULL,
  `aircon` bit(1) DEFAULT NULL,
  `bath` int(11) DEFAULT NULL,
  `bed` varchar(50) DEFAULT NULL,
  `cancel_policy` varchar(100) DEFAULT NULL,
  `free_water` bit(1) DEFAULT NULL,
  `has_window` bit(1) DEFAULT NULL,
  `original_price` int(11) DEFAULT NULL,
  `payment` varchar(50) DEFAULT NULL,
  `price` int(11) DEFAULT NULL,
  `shared_bath` bit(1) DEFAULT NULL,
  `smoke` bit(1) DEFAULT NULL,
  `view_name` varchar(50) DEFAULT NULL,
  `wifi` bit(1) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `room_count` int(11) NOT NULL,
  `room_type` enum('디럭스룸','스위트룸','스탠다드룸','싱글룸','트윈룸') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_room_hotel` (`hotel_id`),
  CONSTRAINT `FK_Hotel_TO_Room_1` FOREIGN KEY (`hotel_id`) REFERENCES `hotel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room`
--

LOCK TABLES `room` WRITE;
/*!40000 ALTER TABLE `room` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room` VALUES
(1,1,'디럭스 더블룸','32㎡',1,2,'15:00:00','11:00:00',0x01,1,'더블베드','체크인 1일 전까지 무료 취소',0x01,0x01,200000,'CARD',180000,0x00,0x00,'시티뷰',0x01,'available',5,'디럭스룸'),
(2,1,'이그제큐티브 스위트','65㎡',2,4,'15:00:00','11:00:00',0x01,2,'킹베드 + 소파베드','체크인 2일 전까지 무료 취소',0x01,0x01,500000,'CARD',450000,0x00,0x00,'한강뷰',0x01,'available',3,'스위트룸'),
(3,1,'스탠다드 트윈룸','28㎡',1,2,'15:00:00','11:00:00',0x01,1,'트윈베드','체크인 1일 전까지 무료 취소',0x01,0x01,150000,'CARD',130000,0x00,0x00,'시티뷰',0x01,'available',8,'트윈룸'),
(4,2,'오션뷰 디럭스','38㎡',2,3,'16:00:00','11:00:00',0x01,1,'더블베드','체크인 3일 전까지 무료 취소',0x01,0x01,300000,'CARD',280000,0x00,0x00,'오션뷰',0x01,'available',6,'디럭스룸'),
(5,2,'패밀리 룸','52㎡',4,6,'16:00:00','11:00:00',0x01,2,'킹베드 + 번크베드','체크인 3일 전까지 무료 취소',0x01,0x01,400000,'CARD',380000,0x00,0x00,'오션뷰',0x01,'available',4,'스탠다드룸'),
(6,2,'프리미엄 스위트','75㎡',2,4,'16:00:00','11:00:00',0x01,2,'킹베드 + 거실','체크인 3일 전까지 무료 취소',0x01,0x01,700000,'CARD',650000,0x00,0x00,'오션뷰',0x01,'available',2,'스위트룸'),
(7,2,'스탠다드 싱글룸','22㎡',1,1,'15:00:00','11:00:00',0x01,1,'싱글베드','체크인 1일 전까지 무료 취소',0x01,0x01,110000,'CARD',100000,0x00,0x00,'시티뷰',0x01,'available',10,'싱글룸'),
(8,2,'비즈니스 더블룸','30㎡',1,2,'15:00:00','11:00:00',0x01,1,'더블베드','체크인 1일 전까지 무료 취소',0x01,0x01,200000,'CARD',180000,0x00,0x00,'시티뷰',0x01,'available',7,'디럭스룸');
/*!40000 ALTER TABLE `room` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room_image`
--

DROP TABLE IF EXISTS `room_image`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL,
  `url` text NOT NULL,
  `sort_no` int(11) NOT NULL DEFAULT 0,
  `is_cover` tinyint(1) NOT NULL DEFAULT 0,
  `caption` varchar(255) DEFAULT NULL,
  `alt_text` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rimg_room` (`room_id`),
  CONSTRAINT `fk_rimg_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_image`
--

LOCK TABLES `room_image` WRITE;
/*!40000 ALTER TABLE `room_image` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room_image` VALUES
(1,1,'http://localhost:8888/uploads/rooms/deluxe-double-room.jpg',0,1,'디럭스 더블룸 전경','그랜드 서울 호텔 디럭스 더블룸'),
(2,1,'http://localhost:8888/uploads/rooms/deluxe-double-bathroom.jpg',1,0,'디럭스 더블룸 욕실','그랜드 서울 호텔 디럭스 더블룸 욕실'),
(3,2,'http://localhost:8888/uploads/rooms/executive-suite-room.jpg',0,1,'이그제큐티브 스위트 전경','그랜드 서울 호텔 이그제큐티브 스위트'),
(4,2,'http://localhost:8888/uploads/rooms/executive-suite-livingroom.jpg',1,0,'이그제큐티브 스위트 거실','그랜드 서울 호텔 이그제큐티브 스위트 거실'),
(5,3,'http://localhost:8888/uploads/rooms/standard-twin-room.jpg',0,1,'스탠다드 트윈룸 전경','그랜드 서울 호텔 스탠다드 트윈룸'),
(6,4,'http://localhost:8888/uploads/rooms/ocean-view-deluxe.jpg',0,1,'오션뷰 디럭스 전경','제주 오션뷰 리조트 오션뷰 디럭스'),
(7,4,'http://localhost:8888/uploads/rooms/ocean-view-deluxe-view.jpg',1,0,'오션뷰 디럭스 바다 전망','제주 오션뷰 리조트 오션뷰 디럭스 바다 전망'),
(8,5,'http://localhost:8888/uploads/rooms/family-room.jpg',0,1,'패밀리 룸 전경','제주 오션뷰 리조트 패밀리 룸'),
(9,5,'http://localhost:8888/uploads/rooms/family-room-bunkbed.jpg',1,0,'패밀리 룸 이층침대','제주 오션뷰 리조트 패밀리 룸 이층침대'),
(10,6,'http://localhost:8888/uploads/rooms/premium-suite.jpg',0,1,'프리미엄 스위트 전경','제주 오션뷰 리조트 프리미엄 스위트'),
(11,6,'http://localhost:8888/uploads/rooms/premium-suite-terrace.jpg',1,0,'프리미엄 스위트 프라이빗 테라스','제주 오션뷰 리조트 프리미엄 스위트 테라스'),
(12,7,'http://localhost:8888/uploads/rooms/standard-single-room.jpg',0,1,'스탠다드 싱글룸','제주 오션뷰 리조트 스탠다드 싱글룸'),
(13,8,'http://localhost:8888/uploads/rooms/business-double-room.jpg',0,1,'비즈니스 더블룸','제주 오션뷰 리조트 비즈니스 더블룸');
/*!40000 ALTER TABLE `room_image` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room_inventory`
--

DROP TABLE IF EXISTS `room_inventory`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_inventory` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL,
  `date` date NOT NULL,
  `total_quantity` int(11) NOT NULL,
  `available_quantity` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_room_day` (`room_id`,`date`),
  KEY `idx_inv_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Inventory_1` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_inventory`
--

LOCK TABLES `room_inventory` WRITE;
/*!40000 ALTER TABLE `room_inventory` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room_inventory` VALUES
(1,1,'2025-10-16',5,5),
(2,1,'2025-10-17',5,5),
(3,1,'2025-10-18',5,5),
(4,1,'2025-10-19',5,5),
(5,1,'2025-10-20',5,5),
(6,1,'2025-10-21',5,5),
(7,1,'2025-10-22',5,5),
(8,1,'2025-10-08',5,4),
(9,1,'2025-10-09',5,4),
(10,1,'2025-10-10',5,4),
(11,1,'2025-10-11',5,4),
(12,1,'2025-10-12',5,4),
(13,1,'2025-10-13',5,4),
(14,1,'2025-10-14',5,4);
/*!40000 ALTER TABLE `room_inventory` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room_inventory_override`
--

DROP TABLE IF EXISTS `room_inventory_override`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_inventory_override` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `date` date NOT NULL,
  `note` varchar(200) DEFAULT NULL,
  `room_id` bigint(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_room_date` (`room_id`,`date`),
  CONSTRAINT `FK_Room_Inventory_Override_Room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_inventory_override`
--

LOCK TABLES `room_inventory_override` WRITE;
/*!40000 ALTER TABLE `room_inventory_override` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room_inventory_override` VALUES
(3,'2025-09-29 13:49:45.038932','2025-09-22','calendar-change',8,'closed'),
(8,'2025-09-29 16:47:02.432263','2025-09-26','calendar-change',7,'open'),
(12,'2025-09-29 16:55:42.963284','2025-09-11','calendar-change',6,'open'),
(14,'2025-09-29 18:49:37.294970','2025-09-11','calendar-change',7,'open'),
(15,'2025-09-29 18:51:35.226009','2025-09-12','calendar-change',6,'maintenance'),
(16,'2025-09-29 18:51:40.800738','2025-09-19','calendar-change',8,'closed'),
(18,'2025-09-29 19:08:55.998070','2025-09-25','calendar-change',7,'open'),
(19,'2025-10-01 11:23:39.425813','2025-10-07','calendar-change',6,'closed'),
(20,'2025-10-01 11:25:02.276456','2025-10-17','calendar-change',7,'closed'),
(21,'2025-10-01 11:25:14.422221','2025-10-23','calendar-change',5,'maintenance'),
(22,'2025-10-01 11:25:14.498242','2025-10-23','calendar-change',6,'closed'),
(23,'2025-10-01 11:25:14.556150','2025-10-23','calendar-change',7,'cleaning'),
(24,'2025-10-01 11:25:14.614872','2025-10-23','calendar-change',8,'closed'),
(25,'2025-10-01 13:35:19.143961','2025-10-02','calendar-change',7,'closed'),
(26,'2025-10-01 14:27:24.583721','2025-10-17','calendar-change',6,'maintenance'),
(27,'2025-10-01 14:42:43.809959','2025-10-23','calendar-change',4,'closed'),
(29,'2025-10-01 15:35:41.712412','2025-10-04','calendar-change',5,'maintenance'),
(30,'2025-10-01 18:36:28.345761','2025-10-10','calendar-change',6,'closed'),
(31,'2025-10-01 19:48:29.211263','2025-10-16','calendar-change',5,'closed'),
(32,'2025-10-01 19:49:58.830156','2025-10-09','calendar-change',6,'closed'),
(33,'2025-10-01 19:49:58.865433','2025-10-09','calendar-change',7,'cleaning');
/*!40000 ALTER TABLE `room_inventory_override` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room_price_policy`
--

DROP TABLE IF EXISTS `room_price_policy`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_price_policy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `room_id` bigint(20) NOT NULL,
  `season_type` enum('PEAK','OFF_PEAK','HOLIDAY') NOT NULL DEFAULT 'OFF_PEAK',
  `day_type` enum('WEEKDAY','FRI','SAT','SUN') NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `price` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rpp_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Price_Policy_1` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_price_policy`
--

LOCK TABLES `room_price_policy` WRITE;
/*!40000 ALTER TABLE `room_price_policy` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room_price_policy` (`id`, `room_id`, `season_type`, `day_type`, `start_date`, `end_date`, `price`) VALUES
(1, 1, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 200000),
(2, 1, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 250000),
(3, 2, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 500000),
(4, 2, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 600000),
(5, 3, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 150000),
(6, 3, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 180000),
(7, 4, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 300000),
(8, 4, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 350000),
(9, 5, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 400000),
(10, 5, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 480000),
(11, 6, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 700000),
(12, 6, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 850000),
(13, 7, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 110000),
(14, 7, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 140000),
(15, 8, 'OFF_PEAK', 'WEEKDAY', '2025-01-01', '2025-12-31', 200000),
(16, 8, 'PEAK', 'SAT', '2025-01-01', '2025-12-31', 250000);
/*!40000 ALTER TABLE `room_price_policy` ENABLE KEYS */;
UNLOCK TABLES;
commit;

--
-- Table structure for table `room_status_override`
--

DROP TABLE IF EXISTS `room_status_override`;
/*!40101 SET @saved_cs_client      = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `room_status_override` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date` date NOT NULL,
  `note` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `room_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_room_date` (`room_id`,`date`),
  CONSTRAINT `FKf9vusmkr02yq1x2pa9l7s3bmb` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room_status_override`
--

LOCK TABLES `room_status_override` WRITE;
/*!40000 ALTER TABLE `room_status_override` DISABLE KEYS */;
set autocommit=0;
INSERT INTO `room_status_override` VALUES
(1,'2025-09-18','calendar-change','cleaning',6),
(2,'2025-09-11','calendar-change','maintenance',6),
(3,'2025-09-19','calendar-change','open',7);
/*!40000 ALTER TABLE `room_status_override` ENABLE KEYS */;
UNLOCK TABLES;
commit;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2025-10-01 20:34:01