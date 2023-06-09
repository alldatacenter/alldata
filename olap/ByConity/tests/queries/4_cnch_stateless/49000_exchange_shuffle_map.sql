DROP TABLE IF EXISTS map_tbl;
DROP TABLE IF EXISTS map_array_tbl;
create table map_tbl(c1 UInt64, c2 UInt64, c3 Map(String, UInt8)) ENGINE=CnchMergeTree() order by c1;
create table map_array_tbl(c1 UInt64, c2 UInt64, c3 Map(String, Array(String))) ENGINE = CnchMergeTree() order by c1;
------------------------------------------------------------------------------------
insert into map_tbl values(1, 1, {'abc':1, 'bcd':2});
insert into map_tbl values(2, 2, {'abc':1, 'bcd':2});

insert into map_tbl values(1, 1, {'abc':1, 'bcd':3});
insert into map_tbl values(3, 1, {'bcd':3, 'abc':1});

insert into map_tbl values(0, 1, {'abcd':1, 'bcd':3});
insert into map_tbl values(1, 1, {'abc':2, 'bcd':3});

insert into map_tbl values(1, 1, {});
insert into map_tbl values(1, 1, {});
------------------------------------------------------------------------------------
insert into map_array_tbl values(1, 1, {'abc':['1','2'], 'bcd':['1','2']});
insert into map_array_tbl values(1, 1, {'abc':['1','2'], 'bcd':['1','2']});
insert into map_array_tbl values(1, 1, {'bcd':['1','2'], 'abc':['1','2']});

insert into map_array_tbl values(1, 1, {'abc':['1','3'], 'bcd':['1','2']});

------------------------------------------------------------------------------------
SELECT sum(c1) sum_c1, c3 FROM map_tbl GROUP BY c3 ORDER BY sum_c1;
SELECT sum(c1) sum_c1, c3 FROM map_tbl GROUP BY c3 ORDER BY sum_c1 settings enable_optimizer =1, exchange_parallel_size=5;

SELECT sum(c1) sum_c1, c3 FROM map_array_tbl GROUP BY c3 ORDER BY sum_c1;
SELECT sum(c1) sum_c1, c3 FROM map_array_tbl GROUP BY c3 ORDER BY sum_c1 settings enable_optimizer =1, exchange_parallel_size=5;

DROP TABLE IF EXISTS map_tbl;
DROP TABLE IF EXISTS map_array_tbl;