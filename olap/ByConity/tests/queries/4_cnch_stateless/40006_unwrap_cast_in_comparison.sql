CREATE DATABASE IF NOT EXISTS test;

DROP TABLE IF EXISTS tbl;

CREATE TABLE tbl(`id` Int32, i8 Int8, ni8 Nullable(Int8))
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO tbl values (1, -128, NULL) (2, -128, -128) (3, -127, -127) (4, -1, -1) (5, 0, 0) (6, 1, 1) (7, 2, NULL) (8, 126, 126) (9, 127, 127) (10, 127, NULL);

SELECT '--case 1';
select id from tbl where cast(i8 as Int32) = -129 order by id;
SELECT '--case 2';
select id from tbl where cast(i8 as Int32) > -129 order by id;
SELECT '--case 3';
select id from tbl where cast(i8 as Int32) != 128 order by id;
SELECT '--case 4';
select id from tbl where cast(i8 as Int32) > 128 order by id;
SELECT '--case 5';
select id from tbl where cast(i8 as Int32) <= -128 order by id;
SELECT '--case 6';
select id from tbl where cast(i8 as Int32) > -128 order by id;
SELECT '--case 7';
select id from tbl where cast(i8 as Int32) >= -128 order by id;
SELECT '--case 8';
select id from tbl where cast(i8 as Int32) < -128 order by id;
SELECT '--case 9';
select id from tbl where cast(i8 as Int32) = 127 order by id;
SELECT '--case 10';
select id from tbl where cast(i8 as Int32) != 127 order by id;
SELECT '--case 11';
select id from tbl where cast(i8 as Int32) <= 127 order by id;
SELECT '--case 12';
select id from tbl where cast(i8 as Int32) > 127 order by id;
SELECT '--case 13';
select id from tbl where cast(i8 as Int32) >= 1 order by id;
SELECT '--case 14';
select id from tbl where cast(i8 as Int32) < -1 order by id;
SELECT '--case 15';
select id from tbl where cast(i8 as Int32) = 1.2 order by id;
SELECT '--case 16';
select id from tbl where cast(i8 as Int32) != 1.2 order by id;
SELECT '--case 17';
select id from tbl where cast(i8 as Int32) > 1.2 order by id;
SELECT '--case 18';
select id from tbl where cast(i8 as Int32) >= 1.2 order by id;
SELECT '--case 19';
select id from tbl where cast(i8 as Int32) < 1.2 order by id;
SELECT '--case 20';
select id from tbl where cast(i8 as Int32) <= 1.2 order by id;
SELECT '--case 21';
select id from tbl where cast(ni8 as Nullable(Int32)) > -129 order by id;
SELECT '--case 22';
select id from tbl where cast(ni8 as Nullable(Int32)) > 129 order by id;

