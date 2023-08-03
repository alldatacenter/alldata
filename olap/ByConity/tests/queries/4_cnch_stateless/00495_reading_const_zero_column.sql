
drop table if exists one_table;
create table one_table (date Date, one UInt64) engine = CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY (date, one);
insert into one_table select today(), toUInt64(1) from system.numbers limit 100000;
SET preferred_block_size_bytes = 8192;
select isNull(one) from one_table where isNull(one);
