/* =======================================================================
   Egoda 호텔 프로젝트 — MariaDB 전체 스키마 & 최소 시드 (ONE FILE)
   -----------------------------------------------------------------------
   사용법(예):
     mysql -h 127.0.0.1 -P 3306 -u root -p
     -- 필요시 사용자/권한 생성 후:
     --   CREATE USER IF NOT EXISTS 'hotel'@'%' IDENTIFIED BY 'hotelpw';
     --   GRANT ALL PRIVILEGES ON hotel.* TO 'hotel'@'%';
     --   FLUSH PRIVILEGES;
     -- DB/테이블 생성:
     --   SOURCE C:/path/to/egoda_schema.sql;
   ======================================================================= */

-- 0) 데이터베이스
CREATE DATABASE IF NOT EXISTS `hotel`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `hotel`;

-- ======================================================================
-- 1) 사용자 (플랫폼 회원/호텔 소유자/관리자)
-- ======================================================================
CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `name` varchar(50)  NOT NULL,                              
  `phone` varchar(20)  NOT NULL,                             
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

-- ======================================================================
-- 2) 호텔 (호텔 기본 정보)
--  - status 추가: 공개/승인 흐름 관리
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `user_id` BIGINT NOT NULL,                                 
  `business_id` BIGINT NOT NULL,                             
  `name` varchar(100) NOT NULL,                              
  `address` varchar(255) NOT NULL,                           
  `star_rating` int NOT NULL,                                
  `description` text NULL,                                   
  `country` varchar(50) NOT NULL,                            
  `status` ENUM('PENDING','APPROVED','SUSPENDED') NOT NULL DEFAULT 'PENDING',
  PRIMARY KEY (`id`),
  KEY `idx_hotel_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Hotel_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- (마이그레이션: 과거 컬럼 정리)
ALTER TABLE `Hotel` DROP COLUMN IF EXISTS `image`;

-- ======================================================================
-- 3) 호텔 이미지 (1:N, 대표/정렬/캡션/alt 관리)
-- ======================================================================
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
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 커버+정렬 조회 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_himg_cover_sort
  ON `hotel_image` (`hotel_id`, `is_cover`, `sort_no`);

-- ======================================================================
-- 4) 편의시설 마스터 (공통 정의)
-- ======================================================================
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
  `category` ENUM('IN_ROOM','IN_HOTEL','LEISURE','FNB','BUSINESS','OTHER')
              NOT NULL DEFAULT 'OTHER',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 5) 호텔-편의시설 매핑 (N:M)
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Hotel_Amenity` (
  `hotel_id` BIGINT NOT NULL,                                
  `amenity_id` BIGINT NOT NULL,                              
  PRIMARY KEY (`hotel_id`,`amenity_id`),                     
  KEY `idx_ha_amenity` (`amenity_id`),
  CONSTRAINT `FK_Hotel_TO_Hotel_Amenity_1`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `FK_Amenity_TO_Hotel_Amenity_1`
    FOREIGN KEY (`amenity_id`) REFERENCES `Amenity` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 6) 객실(룸 타입)
-- ======================================================================
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
  CONSTRAINT `FK_Hotel_TO_Room_1`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- (마이그레이션: 과거 컬럼 정리)
ALTER TABLE `Room` DROP COLUMN IF EXISTS `image`;

-- ======================================================================
-- 7) 객실 이미지 (1:N)
-- ======================================================================
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
    FOREIGN KEY (`room_id`) REFERENCES `Room`(`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 커버+정렬 조회 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_rimg_cover_sort
  ON `room_image` (`room_id`, `is_cover`, `sort_no`);

-- ======================================================================
-- 8) 객실 일자별 재고
--    UNIQUE(room_id, date): 같은 날 특정 객실타입 재고는 1줄만 존재하도록 강제
-- ======================================================================
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
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 9) 객실 요금 정책 (성수기/요일별 구간 가격)
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Room_Price_Policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `room_id` BIGINT NOT NULL,                                 
  `season_type` ENUM('PEAK','OFF_PEAK','HOLIDAY') NOT NULL DEFAULT 'OFF_PEAK',
  `day_type`    ENUM('WEEKDAY','FRI','SAT','SUN') NOT NULL,
  `start_date` date NOT NULL,                                
  `end_date`   date NOT NULL,                                
  `price` int  NOT NULL,                                     
  PRIMARY KEY (`id`),
  KEY `idx_rpp_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Price_Policy_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 10) 예약
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `user_id` BIGINT NOT NULL,                                 
  `room_id` BIGINT NOT NULL,                                 
  `transaction_id` varchar(255) NULL,                        
  `num_adult` int NOT NULL DEFAULT 0,                        
  `num_kid`   int NOT NULL DEFAULT 0,                        
  `start_date` timestamp NOT NULL,                           
  `end_date`   timestamp NOT NULL,                           
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, 
  `status` ENUM('PENDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `expires_at` timestamp NULL,                               
  PRIMARY KEY (`id`),
  KEY `idx_res_user` (`user_id`),
  KEY `idx_res_room` (`room_id`),
  CONSTRAINT `FK_User_TO_Reservation_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `FK_Room_TO_Reservation_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 11) 결제
--    receipt_url: TEXT 대신 VARCHAR(512) UNIQUE를 권장 (인덱싱/UNIQUE 용이)
-- ======================================================================
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
  CONSTRAINT `FK_Reservation_TO_Payment_1`
    FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 12) 쿠폰
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `user_id` BIGINT NOT NULL,                                 
  `name` varchar(255) NOT NULL,                              
  `code` varchar(255) NOT NULL,                              
  `discount_type` ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` int NOT NULL,                             
  `min_spend` int NOT NULL DEFAULT 0,                        
  `valid_from` datetime NOT NULL,                            
  `valid_to`   datetime NULL,                                
  `is_active` boolean NOT NULL DEFAULT true,                 
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coupon_code` (`code`),
  KEY `idx_coupon_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Coupon_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- 13) 리뷰 (예약 1건당 1개 제한)
-- ======================================================================
CREATE TABLE IF NOT EXISTS `Review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       
  `reservation_id` BIGINT NOT NULL,                          
  `wrote_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,   
  `star_rating` int NOT NULL DEFAULT 5,                      
  `content` text NULL,                                       
  `image` text NULL,                                         
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_reservation` (`reservation_id`),     
  CONSTRAINT `FK_Reservation_TO_Review_1`
    FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ======================================================================
-- (선택) 최소 시드 데이터 — FE 상세 페이지 빠른 확인용
--   /api/hotels/1 호출에 대응하도록 기본값 넣기
-- ======================================================================

-- 유저(관리자/오너)
INSERT INTO app_user (id, name, phone, email, password, date_of_birth, address, role)
VALUES
  (1,'관리자','010-0000-0000','admin@egoda.local','{noop}pw','1990-01-01','Seoul','ADMIN')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO app_user (id, name, phone, email, password, date_of_birth, address, role)
VALUES
  (2,'오너','010-1111-1111','owner@egoda.local','{noop}pw','1990-01-01','Seoul','BUSINESS')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 호텔 (APPROVED)
INSERT INTO Hotel (id, user_id, business_id, name, address, star_rating, description, country, status)
VALUES
  (1, 2, 1001, '플로이 호스텔 (PLOY Hostel)', 'Samsen 4 Alley, 카오산, 방콕, 태국, 10200', 3,
   '플로이 호스텔은 여행자에게 적합한 공유공간과 옥상 테라스를 갖춘 합리적 숙소입니다.', 'TH', 'APPROVED')
ON DUPLICATE KEY UPDATE name=VALUES(name), status=VALUES(status);

-- 호텔 이미지(대표+서브)
INSERT INTO hotel_image (hotel_id, url, sort_no, is_cover)
VALUES
  (1,'https://picsum.photos/seed/hotel_hero/1200/720',0,1),
  (1,'https://picsum.photos/seed/hotel_sub1/600/360',1,0),
  (1,'https://picsum.photos/seed/hotel_sub2/600/360',2,0)
ON DUPLICATE KEY UPDATE url=VALUES(url), is_cover=VALUES(is_cover);

-- 객실
INSERT INTO Room (id, hotel_id, name, room_size, capacity_min, capacity_max, check_in_time, check_out_time)
VALUES
  (1,1,'14베드 혼성 도미토리룸 내 침대','48㎡',1,1,'15:00:00','11:00:00'),
  (2,1,'프라이빗 더블룸','26㎡',1,2,'15:00:00','11:00:00')
ON DUPLICATE KEY UPDATE name=VALUES(name), room_size=VALUES(room_size);

-- 객실 이미지
INSERT INTO room_image (room_id, url, sort_no, is_cover)
VALUES
  (1,'https://picsum.photos/seed/room1a/480/320',0,1),
  (1,'https://picsum.photos/seed/room1b/140/100',1,0),
  (1,'https://picsum.photos/seed/room1c/140/100',2,0),
  (1,'https://picsum.photos/seed/room1d/140/100',3,0),
  (2,'https://picsum.photos/seed/room2a/480/320',0,1),
  (2,'https://picsum.photos/seed/room2b/140/100',1,0),
  (2,'https://picsum.photos/seed/room2c/140/100',2,0)
ON DUPLICATE KEY UPDATE url=VALUES(url), is_cover=VALUES(is_cover);

-- 편의시설(샘플 4개) + 매핑
INSERT INTO Amenity (id, name, description, icon_url, fee_type, is_active, category)
VALUES
  (1,'무료 Wi-Fi',NULL,NULL,'FREE',true,'IN_HOTEL'),
  (2,'조식',NULL,NULL,'PAID',true,'FNB'),
  (3,'24시간 프런트 데스크',NULL,NULL,'FREE',true,'IN_HOTEL'),
  (4,'공항 이동 서비스',NULL,NULL,'PAID',true,'IN_HOTEL')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT IGNORE INTO Hotel_Amenity (hotel_id, amenity_id) VALUES
  (1,1),(1,2),(1,3),(1,4);

-- (옵션) 하루 재고 샘플
INSERT INTO Room_Inventory (room_id, `date`, total_quantity, available_quantity)
VALUES
  (1, CURRENT_DATE(), 5, 3),
  (2, CURRENT_DATE(), 3, 2)
ON DUPLICATE KEY UPDATE available_quantity=VALUES(available_quantity);

-- (옵션) 요금 샘플
INSERT INTO Room_Price_Policy (room_id, season_type, day_type, start_date, end_date, price)
VALUES
  (1,'OFF_PEAK','WEEKDAY', CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 90 DAY), 66170),
  (2,'OFF_PEAK','WEEKDAY', CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 90 DAY), 37190)
ON DUPLICATE KEY UPDATE price=VALUES(price);

/* ========================= END OF FILE ========================= */
