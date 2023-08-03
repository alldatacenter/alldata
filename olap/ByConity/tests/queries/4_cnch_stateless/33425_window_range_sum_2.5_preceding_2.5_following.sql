


DROP TABLE IF EXISTS range_sum_2_5_preceding_2_5_following;

CREATE TABLE range_sum_2_5_preceding_2_5_following(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree() ORDER BY tuple();


INSERT INTO range_sum_2_5_preceding_2_5_following VALUES(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select a, sum(b) over (partition by a order by toFloat64(b) RANGE BETWEEN 2.5 PRECEDING AND 2.5 Following) as res
FROM range_sum_2_5_preceding_2_5_following
order by a, res;

DROP TABLE range_sum_2_5_preceding_2_5_following;
