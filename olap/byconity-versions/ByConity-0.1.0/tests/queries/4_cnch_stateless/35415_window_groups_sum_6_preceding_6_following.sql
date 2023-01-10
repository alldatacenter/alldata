DROP DATABASE IF EXISTS test;


DROP TABLE IF EXISTS groups_sum_6_preceding_6_following;

CREATE TABLE groups_sum_6_preceding_6_following(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree()
    PARTITION BY `a`
    PRIMARY KEY `a`
    ORDER BY `a`
    SETTINGS index_granularity = 8192;

INSERT INTO groups_sum_6_preceding_6_following values(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select a,sum(b) over (partition by a order by b groups BETWEEN 6 PRECEDING AND CURRENT ROW) as res
FROM groups_sum_6_preceding_6_following
order by a, res;

DROP TABLE groups_sum_6_preceding_6_following;
