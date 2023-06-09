
drop table if exists array_structure;
create table array_structure (date Date, `Struct.Key1` Array(UInt64), `Struct.Key2` Array(UInt64), padding FixedString(16)) engine = CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY date SETTINGS index_granularity = 16;
insert into array_structure select today() as date, [number], [number + 1], toFixedString('', 16) from system.numbers limit 100;
set preferred_max_column_in_block_size_bytes = 96;
select blockSize(), * from array_structure prewhere `Struct.Key1`[1] = 19 and `Struct.Key2`[1] >= 0 format Null;

drop table if exists array_structure;
create table array_structure (date Date, `Struct.Key1` Array(UInt64), `Struct.Key2` Array(UInt64), padding FixedString(16), x UInt64) engine = CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY date SETTINGS index_granularity = 8;
insert into array_structure select today() as date, [number], [number + 1], toFixedString('', 16), number from system.numbers limit 100;
set preferred_max_column_in_block_size_bytes = 112;
select blockSize(), * from array_structure prewhere x = 7 format Null;

