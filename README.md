############################db####################################################
INSERT INTO USERS (USERNAME, PASSWORD, ROLE) VALUES ('user1', '1234', 'USER');
INSERT INTO USERS (USERNAME, PASSWORD, ROLE) VALUES ('admin', '1234', 'ADMIN');

INSERT INTO hotel (name, image_url, price, rating, amenities, capacity)
VALUES
('서울 신라 호텔', 'hotel1.png', 500000, '5성급(*****)', '무료 Wi-Fi,수영장,피트니스 센터', 2),
('롯데 호텔 월드', 'hotel2.png', 450000, '4성급(****)', '무료 Wi-Fi,주차 가능,피트니스 센터', 4),
('노보텔 앰배서더 강남', 'hotel3.png', 350000, '3성급(***)', '무료 Wi-Fi,수영장,피트니스 센터', 3),
('파크 하얏트 서울', 'hotel4.png', 250000, '2성급(**)', '무료 Wi-Fi,수영장,피트니스 센터', 1),
('웨스틴 서울 파르나스', 'hotel5.png', 200000, '1성급(*)', '무료 Wi-Fi,수영장,피트니스 센터', 5); 

INSERT INTO WISHLIST (USER_ID, HOTEL_ID) VALUES (1, 1);
INSERT INTO WISHLIST (USER_ID, HOTEL_ID) VALUES (1, 2);
INSERT INTO WISHLIST (USER_ID, HOTEL_ID) VALUES (2, 3);

SELECT * FROM HOTEL;
SELECT * FROM USERS;
SELECT * FROM WISHLIST;
SELECT * FROM PAYMENT;

#########################mariadb#######################################
C:\Program Files\MariaDB 12.0\bin>mysql -u root -p
Enter password: ****
Welcome to the MariaDB monitor.  Commands end with ; or \g.
Your MariaDB connection id is 3
Server version: 12.0.2-MariaDB mariadb.org binary distribution

Copyright (c) 2000, 2018, Oracle, MariaDB Corporation Ab and others.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

MariaDB [(none)]> CREATE DATABASE mydb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
Query OK, 1 row affected (0.002 sec)

MariaDB [(none)]> CREATE USER 'myuser'@'%' IDENTIFIED BY 'mypassword';
Query OK, 0 rows affected (0.007 sec)

MariaDB [(none)]> GRANT ALL PRIVILEGES ON mydb.* TO 'myuser'@'%';
Query OK, 0 rows affected (0.006 sec)

MariaDB [(none)]> FLUSH PRIVILEGES;
Query OK, 0 rows affected (0.001 sec)

MariaDB [(none)]> Ctrl-C -- exit!
Bye


C:\Program Files\MariaDB 12.0\bin>mysql -u myuser -p -P 3307 mydb
Enter password: **********
Welcome to the MariaDB monitor.  Commands end with ; or \g.
Your MariaDB connection id is 4
Server version: 12.0.2-MariaDB mariadb.org binary distribution

Copyright (c) 2000, 2018, Oracle, MariaDB Corporation Ab and others.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

MariaDB [mydb]> INSERT INTO HOTEL (NAME) VALUES ('신라호텔');
Query OK, 1 row affected (0.008 sec)

MariaDB [mydb]> UPDATE hotel SET image_url = 'hotel1.png' WHERE id = 1;
Query OK, 1 row affected (0.006 sec)
Rows matched: 1  Changed: 1  Warnings: 0
