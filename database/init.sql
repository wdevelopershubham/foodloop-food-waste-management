CREATE DATABASE IF NOT EXISTS foodloop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'foodloop_user'@'localhost' IDENTIFIED BY 'ChangeMe123!';
GRANT ALL PRIVILEGES ON foodloop_db.* TO 'foodloop_user'@'localhost';
FLUSH PRIVILEGES;
