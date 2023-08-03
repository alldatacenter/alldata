
DROP TABLE IF EXISTS wfnav3;
CREATE TABLE wfnav3 (`a` Int64, `b` Int64) ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfnav3 values (1,1)(1,2)(1,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,4)(2,5)(3,3)(4,4)(4,8);

set max_threads=1;
SELECT
  a,
  b,
  percent_rank() OVER (PARTITION BY a ORDER BY a),
  cume_dist() OVER (PARTITION by b ORDER BY b),
  ntile(2 * 1 + 1) OVER (PARTITION BY a ORDER BY b),
  ntile(4) OVER (PARTITION BY a ORDER BY b)
FROM wfnav3
ORDER BY a, b, 5, 6;

DROP TABLE wfnav3;
