-- 문자셋/타임존 (로컬 개발)
SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- 1) 사용자
CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `date_of_birth` date NOT NULL,
  `address` text NOT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `role` ENUM('USER','ADMIN','BUSINESS') NOT NULL DEFAULT 'USER',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_phone` (`phone`),
  UNIQUE KEY `uq_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) 호텔 (이미지 컬럼 제거)
CREATE TABLE IF NOT EXISTS `Hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `business_id` BIGINT NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `star_rating` int NOT NULL,
  `description` text NULL,
  `country` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_hotel_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Hotel_1` FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) 호텔 이미지 (1:N)
CREATE TABLE IF NOT EXISTS `hotel_image` (
  `id`       BIGINT NOT NULL AUTO_INCREMENT,
  `hotel_id` BIGINT NOT NULL,
  `url`      TEXT   NOT NULL,
  `sort_no`  INT    NOT NULL DEFAULT 0,
  `is_cover` BOOLEAN NOT NULL DEFAULT FALSE,
  `caption`  VARCHAR(255) NULL,
  `alt_text` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_himg_hotel` (`hotel_id`),
  UNIQUE KEY `uq_hotel_sort` (`hotel_id`,`sort_no`),
  CONSTRAINT `fk_himg_hotel`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) 편의시설 마스터
CREATE TABLE IF NOT EXISTS `Amenity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text NULL,
  `icon_url` varchar(255) NULL,
  `fee_type` ENUM('FREE','PAID','HOURLY') NOT NULL DEFAULT 'FREE',
  `fee_amount` int NULL,
  `fee_unit` VARCHAR(50) NULL,
  `operating_hours` varchar(255) NULL,
  `location` varchar(255) NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  `category` ENUM('IN_ROOM','IN_HOTEL','LEISURE','FNB','BUSINESS','OTHER') NOT NULL DEFAULT 'OTHER',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) 호텔-편의시설 (N:M)
CREATE TABLE IF NOT EXISTS `Hotel_Amenity` (
  `hotel_id` BIGINT NOT NULL,
  `amenity_id` BIGINT NOT NULL,
  PRIMARY KEY (`hotel_id`,`amenity_id`),
  KEY `idx_ha_amenity` (`amenity_id`),
  CONSTRAINT `FK_Hotel_TO_Hotel_Amenity_1`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_Amenity_TO_Hotel_Amenity_1`
    FOREIGN KEY (`amenity_id`) REFERENCES `Amenity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6) 객실(룸 타입)
CREATE TABLE IF NOT EXISTS `Room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hotel_id` BIGINT NOT NULL,
  `name` varchar(100) NOT NULL,
  `room_size` varchar(50) NOT NULL,
  `capacity_min` int NOT NULL,
  `capacity_max` int NOT NULL,
  `check_in_time` time NOT NULL,
  `check_out_time` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_room_hotel` (`hotel_id`),
  CONSTRAINT `FK_Hotel_TO_Room_1` FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7) 객실 이미지 (1:N)
CREATE TABLE IF NOT EXISTS `room_image` (
  `id`       BIGINT NOT NULL AUTO_INCREMENT,
  `room_id`  BIGINT NOT NULL,
  `url`      TEXT   NOT NULL,
  `sort_no`  INT    NOT NULL DEFAULT 0,
  `is_cover` BOOLEAN NOT NULL DEFAULT FALSE,
  `caption`  VARCHAR(255) NULL,
  `alt_text` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rimg_room` (`room_id`),
  UNIQUE KEY `uq_room_sort` (`room_id`,`sort_no`),
  CONSTRAINT `fk_rimg_room`
    FOREIGN KEY (`room_id`) REFERENCES `Room`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8) 객실 일자별 재고
CREATE TABLE IF NOT EXISTS `Room_Inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `date` date NOT NULL,
  `total_quantity` int NOT NULL,
  `available_quantity` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_room_day` (`room_id`,`date`),
  KEY `idx_inv_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Inventory_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9) 객실 요금 정책
CREATE TABLE IF NOT EXISTS `Room_Price_Policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `room_id` BIGINT NOT NULL,
  `season_type` ENUM('PEAK','OFF_PEAK','HOLIDAY') NOT NULL DEFAULT 'OFF_PEAK',
  `day_type` ENUM('WEEKDAY','FRI','SAT','SUN') NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `price` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rpp_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Price_Policy_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10) 예약
CREATE TABLE IF NOT EXISTS `Reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `room_id` BIGINT NOT NULL,
  `transaction_id` varchar(255) NULL,
  `num_adult` int NOT NULL DEFAULT 0,
  `num_kid` int NOT NULL DEFAULT 0,
  `start_date` timestamp NOT NULL,
  `end_date` timestamp NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` ENUM('PENDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `expires_at` timestamp NULL,
  PRIMARY KEY (`id`),
  KEY `idx_res_user` (`user_id`),
  KEY `idx_res_room` (`room_id`),
  CONSTRAINT `FK_User_TO_Reservation_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `FK_Room_TO_Reservation_1` FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11) 결제 (TEXT UNIQUE 대신 VARCHAR(512) UNIQUE)
CREATE TABLE IF NOT EXISTS `Payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reservation_id` BIGINT NOT NULL,
  `payment_method` varchar(50) NOT NULL,
  `base_price` int NOT NULL,
  `total_price` int NOT NULL,
  `tax` int NOT NULL DEFAULT 0,
  `discount` int NOT NULL DEFAULT 0,
  `status` ENUM('PAID','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PAID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `refunded_at` timestamp NULL,
  `receipt_url` VARCHAR(512) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_receipt_url` (`receipt_url`),
  KEY `idx_pay_res` (`reservation_id`),
  CONSTRAINT `FK_Reservation_TO_Payment_1` FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12) 쿠폰
CREATE TABLE IF NOT EXISTS `Coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` varchar(255) NOT NULL,
  `code` varchar(255) NOT NULL,
  `discount_type` ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` int NOT NULL,
  `min_spend` int NOT NULL DEFAULT 0,
  `valid_from` datetime NOT NULL,
  `valid_to` datetime NULL,
  `is_active` boolean NOT NULL DEFAULT true,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coupon_code` (`code`),
  KEY `idx_coupon_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Coupon_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13) 리뷰
CREATE TABLE IF NOT EXISTS `Review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reservation_id` BIGINT NOT NULL,
  `wrote_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `star_rating` int NOT NULL DEFAULT 5,
  `content` text NULL,
  `image` text NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_reservation` (`reservation_id`),
  CONSTRAINT `FK_Reservation_TO_Review_1` FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================== 샘플 데이터 ======================
-- 유저 1명(호텔 소유자)
INSERT INTO app_user (id, name, phone, email, password, date_of_birth, address, role)
VALUES (1,'오너','010-0000-0000','owner@example.com','$2a$10$dev','1990-01-01','서울 강남구','BUSINESS')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 호텔 1개
INSERT INTO Hotel (id, user_id, business_id, name, address, star_rating, description, country)
VALUES (1,1,1001,'그랜드 머큐어 임페리얼 팰리스 서울 강남','서울 강남구 언주로 640',5,'강남 중심의 럭셔리 호텔…','KR')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 호텔 이미지
INSERT INTO hotel_image (hotel_id, url, sort_no, is_cover) VALUES
(1,'https://picsum.photos/seed/hotelA/1024/600',0,TRUE),
(1,'https://picsum.photos/seed/hotelB/1024/600',1,FALSE)
ON DUPLICATE KEY UPDATE url=VALUES(url);

-- 객실 2개
INSERT INTO Room (id, hotel_id, name, room_size, capacity_min, capacity_max, check_in_time, check_out_time) VALUES
(1,1,'디럭스 더블룸','26m²',1,2,'15:00:00','11:00:00'),
(2,1,'트윈룸','24m²',1,2,'15:00:00','11:00:00')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 객실 이미지
INSERT INTO room_image (room_id, url, sort_no, is_cover) VALUES
(1,'https://picsum.photos/seed/roomA/800/500',0,TRUE),
(1,'https://picsum.photos/seed/roomA2/800/500',1,FALSE),
(2,'https://picsum.photos/seed/roomB/800/500',0,TRUE)
ON DUPLICATE KEY UPDATE url=VALUES(url);
