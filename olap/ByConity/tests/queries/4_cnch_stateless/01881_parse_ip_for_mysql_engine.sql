USE default;
CREATE TABLE mysql_test
(
    `id` UInt32,
    `name` String,
    `age` UInt32,
    `money` UInt32
)
ENGINE = MySQL('127.0.0.1:8090|[::1]:9981', 'clickhouse', '{}', 'root', 'clickhouse');

DROP TABLE mysql_test;
