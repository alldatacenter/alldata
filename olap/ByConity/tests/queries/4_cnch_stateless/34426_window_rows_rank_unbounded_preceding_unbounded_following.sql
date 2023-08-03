


DROP TABLE IF EXISTS rows_rank_unbounded_preceding_unbounded_following;

CREATE TABLE rows_rank_unbounded_preceding_unbounded_following(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO rows_rank_unbounded_preceding_unbounded_following values(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select rank() over (partition by a order by b ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM rows_rank_unbounded_preceding_unbounded_following;

DROP TABLE rows_rank_unbounded_preceding_unbounded_following;