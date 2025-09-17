-- 1. 데이터베이스 선택
CREATE DATABASE IF NOT EXISTS hotel;
USE hotel;

-- 2. 기존 테이블 삭제 (순서 주의: 외래키 참조 고려)
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS hotel_amenity;
DROP TABLE IF EXISTS hotels;

-- 3. 테이블 생성
CREATE TABLE hotels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(20)
);

CREATE TABLE room (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    room_number VARCHAR(10) NOT NULL,
    capacity INT,
    price DECIMAL(10,2),
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

CREATE TABLE hotel_amenity (
    id INT AUTO_INCREMENT PRIMARY KEY,
    hotel_id INT NOT NULL,
    amenity_name VARCHAR(50),
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

-- 4. 샘플 데이터 삽입
INSERT INTO hotels (name, address, phone)
VALUES 
    ('Hotel A', 'Seoul', '010-1111-1111'),
    ('Hotel B', 'Busan', '010-2222-2222');

INSERT INTO room (hotel_id, room_number, capacity, price)
VALUES 
    (1, '101', 2, 100.00),
    (1, '102', 4, 150.00),
    (2, '201', 2, 120.00);

INSERT INTO hotel_amenity (hotel_id, amenity_name)
VALUES
    (1, 'WiFi'),
    (1, 'Pool'),
    (2, 'Parking');

-- 5. 완료
SELECT 'post.sql 실행 완료' AS message;
