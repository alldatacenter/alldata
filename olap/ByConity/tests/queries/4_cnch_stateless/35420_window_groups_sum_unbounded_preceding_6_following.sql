


DROP TABLE IF EXISTS groups_sum_unbounded_preceding_6_following;

CREATE TABLE groups_sum_unbounded_preceding_6_following(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO groups_sum_unbounded_preceding_6_following values(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select a, sum(b) over (partition by a order by b groups BETWEEN UNBOUNDED PRECEDING AND 6 FOLLOwING) as res
FROM groups_sum_unbounded_preceding_6_following
order by a, res;

DROP TABLE groups_sum_unbounded_preceding_6_following;
