

DROP TABLE IF EXISTS test_tea_limit;

CREATE TABLE test_tea_limit(`id` Int32, `m` Int32)
    ENGINE = CnchMergeTree()
    PARTITION BY `id`
    PRIMARY KEY `id`
    ORDER BY `id`
    SETTINGS index_granularity = 8192;
insert into test_tea_limit values (1, 2);
insert into test_tea_limit values (1, 3);
insert into test_tea_limit values (2, 4);
insert into test_tea_limit values (2, 5);

SELECT
    id,
    sum(m) AS col_22
FROM
    test_tea_limit
GROUP BY id TEALIMIT 1 GROUP assumeNotNull(id) ORDER col_22 DESC