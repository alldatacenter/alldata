


DROP TABLE IF EXISTS groups_denserank_unbounded_preceding_current_row;

CREATE TABLE groups_denserank_unbounded_preceding_current_row(`a` Int64, `b` Int64)
    ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO groups_denserank_unbounded_preceding_current_row values(0,0)(0,0)(0,1)(0,5)(0,8)(0,14)(0,14)(0,14)(0,22)(1,3)(1,3)(1,4)(1,2)(1,6)(1,7)(1,9)(1,3)(1,6);

select dense_rank() over (partition by a order by b groups BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM  groups_denserank_unbounded_preceding_current_row;

DROP TABLE groups_denserank_unbounded_preceding_current_row;
