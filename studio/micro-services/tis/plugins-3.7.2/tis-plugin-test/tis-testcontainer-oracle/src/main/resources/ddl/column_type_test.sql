-- ----------------------------------------------------------------------------------------------------------------
-- DATABASE:  column_type_test
-- ----------------------------------------------------------------------------------------------------------------
-- https://docs.oracle.com/database/121/SQLRF/sql_elements001.htm#SQLRF30020

CREATE TABLE "full_types" (
    "id" INTEGER NOT NULL,
    "bytea_c" BLOB,
    "small_c" SMALLINT,
    "int_c" INTEGER,
    "big_c" INTEGER,
    "real_c" REAL,
    "double_precision" DOUBLE PRECISION,
    "numeric_c" NUMERIC(10, 5),
    "decimal_c" DECIMAL(10, 5),
    "boolean_c" NUMBER(1,0),
    "text_c" CLOB,
    "char_c" CHAR(1),
    "character_c" CHARACTER(3),
    "character_varying_c" CHARACTER VARYING(20),
    "timestamp3_c" TIMESTAMP(3),
    "timestamp6_c" TIMESTAMP(6),
    "date_c" DATE ,
    "time_c" TIMESTAMP(0),
    "default_numeric_c" NUMERIC(10, 5),
    CONSTRAINT full_types_pk PRIMARY KEY ("id")
);

-- INSERT INTO full_types VALUES (
--     100, '2', 32767, 65535, 2147483647, 5.5, 6.6, 123.12345, 404.4443, 1,
--     'Hello World', 'a', 'abc', 'abcd..xyz',  '2020-07-17 18:00:22.123', '2020-07-17 18:00:22.123456',
--     '2020-07-17', '18:00:22', 500);
