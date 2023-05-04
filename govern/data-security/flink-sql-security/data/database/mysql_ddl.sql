DROP DATABASE IF EXISTS demo;
CREATE DATABASE demo;

USE demo;

DROP TABLE IF EXISTS products;
CREATE TABLE products (
  id          INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description VARCHAR(512)
) AUTO_INCREMENT = 101;

DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
  order_id      INTEGER        NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_date    DATETIME       NOT NULL,
  customer_name VARCHAR(255)   NOT NULL,
  price         DECIMAL(10, 5) NOT NULL,
  product_id    INTEGER        NOT NULL,
  order_status  BOOLEAN        NOT NULL,
  region        VARCHAR(255)   NOT NULL
) AUTO_INCREMENT = 10001;

DROP TABLE IF EXISTS shipments;
CREATE TABLE shipments (
  shipment_id INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id    INTEGER      NOT NULL,
  origin      VARCHAR(255) NOT NULL,
  destination VARCHAR(255) NOT NULL,
  is_arrived  BOOLEAN      NOT NULL
) AUTO_INCREMENT = 1001;