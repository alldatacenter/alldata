DROP TABLE IF EXISTS wfnestagg1;
CREATE TABLE wfnestagg1
(
    a UInt64,
    b String,
    c Float64
)
ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfnestagg1
VALUES (0, 'a', 4.2) (0, 'a', 4.1) (1, 'a', -2) (0, 'b', 0) (2, 'c', 9) (1, 'b', -55);
SELECT
  a,
  b,
  SUM(c) AS S,
  rank() OVER (PARTITION BY a ORDER BY b ROWS UNBOUNDED PRECEDING) AS X,
  SUM(SUM(c)) OVER (PARTITION by a ORDER BY b ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS Y
FROM wfnestagg1
GROUP BY a, b
ORDER BY a, b;
DROP TABLE wfnestagg1;
