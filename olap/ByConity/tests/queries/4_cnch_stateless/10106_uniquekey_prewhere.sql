DROP TABLE IF EXISTS unique_prewhere;
DROP TABLE IF EXISTS unique_prewhere_large;
DROP TABLE IF EXISTS unique_prewhere_3;

CREATE TABLE unique_prewhere (id Int32, s String, m1 Int32) ENGINE = CnchMergeTree ORDER BY id UNIQUE KEY id;

INSERT INTO unique_prewhere VALUES (10001, 'BJ', 10), (10002, 'SH', 20), (10003, 'BJ', 30), (10004, 'SH',40), (10005, 'BJ', 50), (10006, 'BJ', 60);
INSERT INTO unique_prewhere VALUES (10004, 'SH', 400), (10005, 'BJ', 500);

SELECT 'prewhere-only', s FROM unique_prewhere prewhere s='BJ' ORDER BY s;
SELECT 'where-only', id, s, m1 FROM unique_prewhere where s='BJ' ORDER BY id settings optimize_move_to_prewhere = 0;
SELECT 'both', id, s, m1 FROM unique_prewhere prewhere s='BJ' where m1 >= 30 ORDER BY id settings optimize_move_to_prewhere = 0;
SELECT 'filter-optimized', id, s, m1 FROM unique_prewhere prewhere s='SH' where m1 >= 30 ORDER BY id settings optimize_move_to_prewhere = 0;
SELECT 'all-filtered', sum(m1) FROM unique_prewhere prewhere s='NYK';
SELECT 'all-filtered-where', sum(m1) FROM unique_prewhere where s='NYK' settings optimize_move_to_prewhere = 0;
SELECT 'none-filtered', sum(m1) FROM unique_prewhere prewhere m1 < 1000;
SELECT 'none-filtered-where', sum(m1) FROM unique_prewhere where m1 < 1000 settings optimize_move_to_prewhere = 0;
SELECT 'const-true', sum(m1) FROM unique_prewhere prewhere 1;
SELECT 'const-false', sum(m1) FROM unique_prewhere prewhere 0;

CREATE TABLE unique_prewhere_large (id Int32, val Int32, granule Int32) ENGINE = CnchMergeTree ORDER BY id UNIQUE KEY id SETTINGS index_granularity=8192;

INSERT INTO unique_prewhere_large SELECT number, number, number / 8192 FROM system.numbers limit 100000;
SELECT 'large', sum(val) FROM unique_prewhere_large;
SELECT 'large-90%', sum(val) FROM unique_prewhere_large prewhere (val % 10) > 0;

INSERT INTO unique_prewhere_large SELECT number, 0, number / 8192  FROM (SELECT number FROM system.numbers limit 100000) where number % 3 == 0;
SELECT 'large-60%', sum(val) FROM unique_prewhere_large prewhere (val % 10) > 0;

INSERT INTO unique_prewhere_large SELECT number, 0, number / 8192  FROM (SELECT number FROM system.numbers limit 100000) where number % 7 == 0;
SELECT 'large-50%', sum(val) FROM unique_prewhere_large prewhere (val % 10) > 0;

INSERT INTO unique_prewhere_large SELECT number, 0, number / 8192  FROM (SELECT number FROM system.numbers limit 100000) where number % 8 == 0;
SELECT 'large-40%', sum(val) FROM unique_prewhere_large prewhere (val % 10) > 0;

INSERT INTO unique_prewhere_large SELECT number, 0, granule FROM (SELECT number, toInt32(number / 8192) as granule FROM system.numbers limit 100000) where granule in (7, 8);
SELECT 'large-final', sum(val) FROM unique_prewhere_large prewhere (val % 10) > 0;

-- set index_granularity = 4 to test granule skipping using delete bitmap
-- set preferred_block_size_bytes = 1 to read one row at a time
select 'test unique_prewhere_3';
create table unique_prewhere_3 (c1 Int64, c2 Int64, c3 String) ENGINE=CnchMergeTree order by c1 unique key c1 SETTINGS index_granularity=4;
insert into unique_prewhere_3 select number, number, '0123456789' from system.numbers limit 10;
select count(1), sum(c1 = c2), sum(length(c3)) from unique_prewhere_3;
select 'update the first and last two rows';
insert into unique_prewhere_3 values (0, 1, '0123456789'), (1, 2, '0123456789'), (8, 9, '0123456789'), (9, 10, '0123456789');
select 'normal read', count(1), sum(c1 = c2), sum(length(c3)) from unique_prewhere_3 prewhere c2 < 100;
select 'small read', count(1), sum(c1 = c2), sum(length(c3)) from unique_prewhere_3 prewhere c2 < 100 settings preferred_block_size_bytes = 1;
select 'update the first granule';
insert into unique_prewhere_3 select number, number + 1, '0123456789' from system.numbers limit 5;
select 'normal read', count(1), sum(c1 = c2), sum(length(c3)) from unique_prewhere_3 prewhere c2 < 100;
select 'small read', count(1), sum(c1 = c2), sum(length(c3)) from unique_prewhere_3 prewhere c2 < 100 settings preferred_block_size_bytes = 1;
select 'normal read: bitmap filter union prewhere filter';
select * from unique_prewhere_3 prewhere c2 % 2 = 1 order by c1;
select 'small read: bitmap filter union prewhere filter';
select * from unique_prewhere_3 prewhere c2 % 2 = 1 order by c1 settings preferred_block_size_bytes = 1;

DROP TABLE IF EXISTS unique_prewhere;
DROP TABLE IF EXISTS unique_prewhere_large;
DROP TABLE IF EXISTS unique_prewhere_3;
