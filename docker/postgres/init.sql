-- =============================================================
-- PostgreSQL Init Script
-- spring-microservices-platform — Local Environment
-- Bu script PostgreSQL container ilk ayağa kalktığında çalışır.
-- Her mikroservis için ayrı veritabanı oluşturur.
-- =============================================================

-- Auth Server
CREATE DATABASE authdb;

-- User Service
CREATE DATABASE userdb;

-- Product Service
CREATE DATABASE productdb;

-- Order Service
CREATE DATABASE orderdb;

-- Payment Service
CREATE DATABASE paymentdb;

-- Notification Service
CREATE DATABASE notificationdb;

-- Cart Service
CREATE DATABASE cartdb;

-- ACS Service (3D Secure)
CREATE DATABASE acsdb;

-- Tüm veritabanlarına app kullanıcısı için yetkiler
GRANT ALL PRIVILEGES ON DATABASE authdb         TO app;
GRANT ALL PRIVILEGES ON DATABASE userdb         TO app;
GRANT ALL PRIVILEGES ON DATABASE productdb      TO app;
GRANT ALL PRIVILEGES ON DATABASE orderdb        TO app;
GRANT ALL PRIVILEGES ON DATABASE paymentdb      TO app;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO app;
GRANT ALL PRIVILEGES ON DATABASE cartdb         TO app;
GRANT ALL PRIVILEGES ON DATABASE acsdb          TO app;

