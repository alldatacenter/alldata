


DROP TABLE IF EXISTS range_denserank_unbouned_preceding_unbounded_following;

CREATE TABLE range_denserank_unbouned_preceding_unbounded_following(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO range_denserank_unbouned_preceding_unbounded_following values(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select dense_rank() over (partition by a order by b RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM range_denserank_unbouned_preceding_unbounded_following;

DROP TABLE range_denserank_unbouned_preceding_unbounded_following;
