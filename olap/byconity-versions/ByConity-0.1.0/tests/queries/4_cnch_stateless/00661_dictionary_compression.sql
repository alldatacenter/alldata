drop table if exists compression_codec;

create table compression_codec (id UInt16 COMPRESSION, date Date) engine=CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY (id, date) SETTINGS index_granularity=8192;

insert into table compression_codec values (1, '2018-01-01'),(2, '2018-01-02'),(3, '2018-01-03');

select * from compression_codec order by id;

select id from compression_codec where id = 1 order by id;
select id, count() from compression_codec group by id order by id;
select id from compression_codec where id != 2 order by id;
select id from compression_codec where id in (1, 2) order by id;

drop table compression_codec;

create table compression_codec (id UInt16, date Date, info String COMPRESSION) engine=CnchMergeTree() PARTITION BY toYYYYMM(date) ORDER BY (id, date, info) SETTINGS index_granularity=8192;

insert into table compression_codec values (1, '2018-01-01', 'info1'),(2, '2018-01-03', 'info2'),(3, '2018-01-03', 'info3');

select * from compression_codec order by id;
select info, count() from compression_codec group by info order by info;
select info from compression_codec where info != 'info1' order by info;
select info from compression_codec where info = 'info1' order by info;
select info from compression_codec where info in ('info1', 'info2') order by info;

select * from compression_codec order by id;

drop table compression_codec;

