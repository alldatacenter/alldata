drop table if exists test.tab0;

CREATE TABLE test.tab0  (`col0` Int64, `col1` Int64, `col2` Int64) ENGINE = CnchMergeTree() PARTITION BY `col0` PRIMARY KEY `col1` ORDER BY `col1` SETTINGS index_granularity = 8192;

insert into test.tab0 values (1, 1, 1);
insert into test.tab0 values (1, 2, 2);
insert into test.tab0 values (1, 2, 3);

SELECT cor0.col1 FROM test.tab0 cross join test.tab0 AS cor0 order by cor0.col1;
SELECT cor0.col1 FROM test.tab0 as a join test.tab0 AS cor0 on a.col0 = cor0.col0 order by cor0.col1;
SELECT cor0.col1 FROM test.tab0 as a join test.tab0 AS cor0 on a.col0 = cor0.col1 order by cor0.col1;
SELECT cor0.col1 FROM test.tab0 as a join test.tab0 AS cor0 on a.col1 = cor0.col0 order by cor0.col1;
SELECT cor0.col1 as result FROM test.tab0 as a join test.tab0 AS cor0 on a.col1 = cor0.col0 order by cor0.col1;

select cor0.col1 as key, count() 
FROM 
    test.tab0 as a 
        join 
    test.tab0 AS cor0 
    on a.col1 = cor0.col0 
    group by key order by cor0.col1;

SELECT key, sum(key) from
(
    select cor0.col1 as key, count() 
    FROM 
        test.tab0 as a 
            join 
        test.tab0 AS cor0 
        on a.col1 = cor0.col0 
        group by key order by cor0.col1
) 
group by key order by key;

SELECT cor0.col1, sum(cor0.col1) from
(
    select cor0.col1, count() 
    FROM 
        test.tab0 as a 
            join 
        test.tab0 AS cor0 
        on a.col1 = cor0.col0 
        group by cor0.col1 order by cor0.col1
) 
group by cor0.col1 order by cor0.col1;

drop table if exists test.tab0;