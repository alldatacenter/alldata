CREATE DATABASE IF NOT EXISTS test;

-- constant folding
SELECT '--constant folding--case 1';
SELECT * FROM numbers(10) WHERE number > 10 - 2 * 2 ORDER BY number;
-- null simplify
SELECT '--null simplify--case 1';
SELECT * FROM numbers(10) WHERE number + NULL = 10 ORDER BY number;
-- simplify and
DROP TABLE IF EXISTS tbl;

CREATE TABLE tbl(`id` Int32, `i8` UInt8, `ni8` Nullable(UInt8))
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO tbl values (1, 0, 0) (2, 1, 1) (3, 2, 2) (4, 3, NULL);

SELECT '--simplify and--case 1';
select id from tbl where i8 and 1 order by id;
SELECT '--simplify and--case 2';
select id from tbl where ni8 and 1 order by id;
SELECT '--simplify and--case 3';
select id from tbl where i8 and 0 order by id;
SELECT '--simplify and--case 4';
select id from tbl where ni8 and 0 order by id;
SELECT '--simplify and--case 5';
select id from tbl where (ni8 and 0) = 0 order by id;
-- simplify or
SELECT '--simplify or--case 1';
select id from tbl where i8 or 1 order by id;
SELECT '--simplify or--case 2';
select id from tbl where ni8 or 1 order by id;
SELECT '--simplify or--case 3';
select id from tbl where i8 or 0 order by id;
SELECT '--simplify or--case 4';
select id from tbl where ni8 or 0 order by id;
-- simplify in
DROP TABLE IF EXISTS tbl;

CREATE TABLE tbl(`id` Int32, i32 Nullable(Int32))
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;

INSERT INTO tbl values (1, 0) (2, 1) (3, 2) (4, NULL);
SELECT '--simplify in--case 1';
select id from tbl where i32 in (1, 1, 1, 1) order by id;
SELECT '--simplify in--case 2';
select id from tbl where i32 in (select cast(i32 + 1, 'Nullable(Int32)') from tbl) order by id;
SELECT '--simplify in--case 3';
select id from tbl where i32 not in (1, 1, 1, 1) order by id;
SELECT '--simplify in--case 4';
select id from tbl where i32 not in (select cast(i32 + 1, 'Nullable(Int32)') from tbl) order by id;
