
DROP TABLE IF EXISTS wfnav1;
CREATE TABLE wfnav1 (`a` Int64, `b` Int64) ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfnav1 values (1,1)(1,2)(1,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,5)(3,3)(4,4)(4,8);

set dialect_type = 'CLICKHOUSE';

SELECT
  a,
  b,
  percent_rank() OVER (PARTITION BY a ORDER BY b ROWS UNBOUNDED PRECEDING),
  cume_dist() OVER (PARTITION by a ORDER BY b ROWS UNBOUNDED PRECEDING),
  ntile(3) OVER (PARTITION BY a ORDER BY b ROWS UNBOUNDED PRECEDING)
FROM wfnav1
ORDER BY a, b, 5;

set dialect_type = 'ANSI';

SELECT
  a,
  b,
  percent_rank() OVER (PARTITION BY a ORDER BY b ROWS UNBOUNDED PRECEDING),
  cume_dist() OVER (PARTITION by a ORDER BY b ROWS UNBOUNDED PRECEDING),
  ntile(3) OVER (PARTITION BY a ORDER BY b ROWS UNBOUNDED PRECEDING)
FROM wfnav1
ORDER BY a, b, 5;

set dialect_type = 'CLICKHOUSE';

DROP TABLE wfnav1;
