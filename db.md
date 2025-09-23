-- =====================================================================
-- 1) 사용자 (플랫폼 회원/호텔 소유자/관리자)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `name` varchar(50)  NOT NULL,                              -- 실명/닉네임
  `phone` varchar(20)  NOT NULL,                             -- 전화번호(Unique)
  `email` varchar(100) NOT NULL,                             -- 이메일(Unique)
  `password` varchar(255) NOT NULL,                          -- 해시된 비밀번호
  `date_of_birth` date NOT NULL,                             -- 생년월일
  `address` text NOT NULL,                                   -- 주소(자유기입)
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 가입일
  `role` ENUM('USER','ADMIN','BUSINESS') NOT NULL DEFAULT 'USER', -- 권한
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_phone` (`phone`),
  UNIQUE KEY `uq_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 2) 호텔 (호텔 기본 정보)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Hotel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `user_id` BIGINT NOT NULL,                                 -- 소유자(app_user.id)
  `business_id` BIGINT NOT NULL,                             -- 사업자 식별(외부/내부 ID)
  `name` varchar(100) NOT NULL,                              -- 호텔명
  `address` varchar(255) NOT NULL,                           -- 주소
  `star_rating` int NOT NULL,                                -- 성급(숫자)
  `description` text NULL,                                   -- 소개문
  `country` varchar(50) NOT NULL,                            -- 국가코드/국가명
  PRIMARY KEY (`id`),
  KEY `idx_hotel_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Hotel_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 3) 호텔 이미지 (1:N, 대표/정렬/캡션/alt 관리)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `hotel_image` (
  `id`       BIGINT NOT NULL AUTO_INCREMENT,                 -- PK
  `hotel_id` BIGINT NOT NULL,                                -- FK → Hotel.id
  `url`      TEXT   NOT NULL,                                -- 이미지 URL(CDN 등)
  `sort_no`  INT    NOT NULL DEFAULT 0,                      -- 노출 순서(0부터)
  `is_cover` BOOLEAN NOT NULL DEFAULT FALSE,                 -- 대표 이미지 여부
  `caption`  VARCHAR(255) NULL,                              -- (선택) 캡션
  `alt_text` VARCHAR(255) NULL,                              -- (선택) 접근성 텍스트
  PRIMARY KEY (`id`),
  KEY `idx_himg_hotel` (`hotel_id`),
  UNIQUE KEY `uq_hotel_sort` (`hotel_id`,`sort_no`),         -- 호텔 내 정렬번호 중복 방지
  CONSTRAINT `fk_himg_hotel`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel`(`id`)
    ON DELETE CASCADE                                        -- 호텔 삭제 시 이미지 자동 삭제
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 4) 편의시설 마스터 (호텔/객실과 무관한 공통 정의)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Amenity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `name` varchar(255) NOT NULL,                              -- 이름
  `description` text NULL,                                   -- 설명
  `icon_url` varchar(255) NULL,                              -- 아이콘 URL
  `fee_type` ENUM('FREE','PAID','HOURLY') NOT NULL DEFAULT 'FREE', -- 요금 타입
  `fee_amount` int NULL,                                     -- 요금(옵션)
  `fee_unit` VARCHAR(50) NULL,                               -- 'per_hour','per_day' 등
  `operating_hours` varchar(255) NULL,                       -- 운영 시간
  `location` varchar(255) NULL,                              -- 위치(예: B1, 2F)
  `is_active` boolean NOT NULL DEFAULT true,                 -- 사용 가능 여부
  `category` ENUM('IN_ROOM','IN_HOTEL','LEISURE','FNB','BUSINESS','OTHER')
              NOT NULL DEFAULT 'OTHER',                      -- 카테고리
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 5) 호텔-편의시설 매핑 (N:M)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Hotel_Amenity` (
  `hotel_id` BIGINT NOT NULL,                                -- FK → Hotel.id
  `amenity_id` BIGINT NOT NULL,                              -- FK → Amenity.id
  PRIMARY KEY (`hotel_id`,`amenity_id`),                     -- 중복 방지
  KEY `idx_ha_amenity` (`amenity_id`),
  CONSTRAINT `FK_Hotel_TO_Hotel_Amenity_1`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`)
    ON DELETE CASCADE,                                       -- 호텔 삭제 시 매핑 삭제
  CONSTRAINT `FK_Amenity_TO_Hotel_Amenity_1`
    FOREIGN KEY (`amenity_id`) REFERENCES `Amenity` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 6) 객실(룸 타입)  
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `hotel_id` BIGINT NOT NULL,                                -- FK → Hotel.id
  `name` varchar(100) NOT NULL,                              -- 객실명(자유 텍스트) << 고민해보기
  `room_size` varchar(50) NOT NULL,                          -- 예: '26m²'
  `capacity_min` int NOT NULL,                               -- 최소 인원
  `capacity_max` int NOT NULL,                               -- 최대 인원
  `check_in_time` time NOT NULL,                             -- 체크인 시각
  `check_out_time` time NOT NULL,                            -- 체크아웃 시각
  PRIMARY KEY (`id`),
  KEY `idx_room_hotel` (`hotel_id`),
  CONSTRAINT `FK_Hotel_TO_Room_1`
    FOREIGN KEY (`hotel_id`) REFERENCES `Hotel` (`id`)
    ON DELETE CASCADE                                        -- 호텔 삭제 시 객실 삭제
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 7) 객실 이미지 (1:N)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `room_image` (
  `id`       BIGINT NOT NULL AUTO_INCREMENT,                 -- PK
  `room_id`  BIGINT NOT NULL,                                -- FK → Room.id
  `url`      TEXT   NOT NULL,                                -- 이미지 URL
  `sort_no`  INT    NOT NULL DEFAULT 0,                      -- 노출 순서
  `is_cover` BOOLEAN NOT NULL DEFAULT FALSE,                 -- 대표 여부
  `caption`  VARCHAR(255) NULL,                              -- (선택) 캡션
  `alt_text` VARCHAR(255) NULL,                              -- (선택) 접근성 텍스트
  PRIMARY KEY (`id`),
  KEY `idx_rimg_room` (`room_id`),
  UNIQUE KEY `uq_room_sort` (`room_id`,`sort_no`),           -- 객실 내 정렬번호 중복 방지
  CONSTRAINT `fk_rimg_room`
    FOREIGN KEY (`room_id`) REFERENCES `Room`(`id`)
    ON DELETE CASCADE                                        -- 객실 삭제 시 이미지 삭제
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 8) 객실 일자별 재고
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Room_Inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `room_id` BIGINT NOT NULL,                                 -- FK → Room.id
  `date` date NOT NULL,                                      -- 기준 일자
  `total_quantity` int NOT NULL,                             -- 총 보유 객실 수
  `available_quantity` int NOT NULL,                         -- 판매 가능 수량
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_room_day` (`room_id`,`date`), // (room_id=101, date='2025-09-14')가 두 줄 생기면, “그날 재고가 몇 개인지”가 모호해지고 합산/차감 시 이중 계산
  KEY `idx_inv_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Inventory_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
    ON DELETE CASCADE                                        -- 객실 삭제 시 재고 삭제
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 9) 객실 요금 정책 (성수기/요일별 구간 가격)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Room_Price_Policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `room_id` BIGINT NOT NULL,                                 -- FK → Room.id
  `season_type` ENUM('PEAK','OFF_PEAK','HOLIDAY') NOT NULL DEFAULT 'OFF_PEAK',
  `day_type`    ENUM('WEEKDAY','FRI','SAT','SUN') NOT NULL,
  `start_date` date NOT NULL,                                -- 적용 시작일
  `end_date`   date NOT NULL,                                -- 적용 종료일
  `price` int  NOT NULL,                                     -- 1박 요금(세금 전)
  PRIMARY KEY (`id`),
  KEY `idx_rpp_room` (`room_id`),
  CONSTRAINT `FK_Room_TO_Room_Price_Policy_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 10) 예약 (기본 흐름: PENDING → COMPLETED/ CANCELLED)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `user_id` BIGINT NOT NULL,                                 -- FK → app_user.id
  `room_id` BIGINT NOT NULL,                                 -- FK → Room.id
  `transaction_id` varchar(255) NULL,                        -- 외부 결제 트랜잭션 ID
  `num_adult` int NOT NULL DEFAULT 0,                        -- 성인 수
  `num_kid`   int NOT NULL DEFAULT 0,                        -- 어린이 수
  `start_date` timestamp NOT NULL,                           -- 체크인(체크인 시각)
  `end_date`   timestamp NOT NULL,                           -- 체크아웃(체크아웃 시각)
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
  `status` ENUM('PENDING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `expires_at` timestamp NULL,                               -- 임시홀드 만료 등
  PRIMARY KEY (`id`),
  KEY `idx_res_user` (`user_id`),
  KEY `idx_res_room` (`room_id`),
  CONSTRAINT `FK_User_TO_Reservation_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `FK_Room_TO_Reservation_1`
    FOREIGN KEY (`room_id`) REFERENCES `Room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 11) 결제 (영수증/금액 합계/상태)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `reservation_id` BIGINT NOT NULL,                          -- FK → Reservation.id
  `payment_method` varchar(50) NOT NULL,                     -- 'TOSS','KAKAOPAY' 등
  `base_price` int NOT NULL,                                 -- 기본 금액
  `total_price` int NOT NULL,                                -- 총 금액(앱 계산 결과)
  `tax` int NOT NULL DEFAULT 0,                              -- 세금
  `discount` int NOT NULL DEFAULT 0,                         -- 할인
  `status` ENUM('PAID','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PAID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, -- 결제 시각
  `refunded_at` timestamp NULL,                              -- 환불 시각
  `receipt_url` VARCHAR(512) NULL,                           -- 영수증 URL(UNIQUE)
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_receipt_url` (`receipt_url`),
  KEY `idx_pay_res` (`reservation_id`),
  CONSTRAINT `FK_Reservation_TO_Payment_1`
    FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 12) 쿠폰 (코드 단일성/유효구간/최소금액)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `user_id` BIGINT NOT NULL,                                 -- 소유자(발급 대상)
  `name` varchar(255) NOT NULL,                              -- 쿠폰명
  `code` varchar(255) NOT NULL,                              -- 쿠폰 코드(Unique)
  `discount_type` ENUM('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` int NOT NULL,                             -- 할인 값(%) 또는 금액
  `min_spend` int NOT NULL DEFAULT 0,                        -- 최소 결제 금액
  `valid_from` datetime NOT NULL,                            -- 시작일시
  `valid_to`   datetime NULL,                                -- 종료일시(null=상시)
  `is_active` boolean NOT NULL DEFAULT true,                 -- 사용 가능 여부
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_coupon_code` (`code`),
  KEY `idx_coupon_user` (`user_id`),
  CONSTRAINT `FK_User_TO_Coupon_1`
    FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 13) 리뷰 (예약 1건당 1개 제한)
-- =====================================================================
CREATE TABLE IF NOT EXISTS `Review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,                       -- PK
  `reservation_id` BIGINT NOT NULL,                          -- FK → Reservation.id
  `wrote_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- 작성일시
  `star_rating` int NOT NULL DEFAULT 5,                      -- 별점(1~5)
  `content` text NULL,                                       -- 내용
  `image` text NULL,                                         -- 리뷰 이미지(단일, 다장은 별도화 가능)
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_review_reservation` (`reservation_id`),     -- 예약별 1개 제한
  CONSTRAINT `FK_Reservation_TO_Review_1`
    FOREIGN KEY (`reservation_id`) REFERENCES `Reservation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
