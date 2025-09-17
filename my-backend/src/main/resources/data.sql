INSERT INTO app_user(id, name, phone, email, password, date_of_birth, address, role)
VALUES (1,'오너','010-0000-0000','owner@example.com','{noop}dev','1990-01-01','서울 강남구','BUSINESS')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO Hotel(id, user_id, business_id, name, address, star_rating, description, country)
VALUES (1,1,1001,'그랜드 머큐어 임페리얼 팰리스 서울 강남','서울 강남구 언주로 640',5,'강남 중심의 럭셔리 호텔…','KR')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO hotel_image(hotel_id, url, sort_no, is_cover) VALUES
(1,'https://picsum.photos/seed/hotelA/1024/600',0,TRUE)
ON DUPLICATE KEY UPDATE url=VALUES(url);

INSERT INTO Room(id, hotel_id, name, room_size, capacity_min, capacity_max, check_in_time, check_out_time) VALUES
(1,1,'디럭스 더블룸','26m²',1,2,'15:00:00','11:00:00')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO room_image(room_id, url, sort_no, is_cover) VALUES
(1,'https://picsum.photos/seed/roomA/800/500',0,TRUE)
ON DUPLICATE KEY UPDATE url=VALUES(url);
