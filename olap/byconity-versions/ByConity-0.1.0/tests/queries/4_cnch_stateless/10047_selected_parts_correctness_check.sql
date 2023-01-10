-- TO check if can get parts correctly from catalog;

DROP TABLE IF EXISTS parts_checker;

CREATE TABLE parts_checker (id UInt64, name String) ENGINE = CnchMergeTree PARTITION BY id ORDER BY id SETTINGS index_granularity = 8192;
insert into table parts_checker values (1, 'test1'), (11, 'test11'), (111, 'test111');
select * from parts_checker order by id;
