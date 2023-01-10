DROP TABLE IF EXISTS wfagg2;
CREATE TABLE wfagg2
(
    a UInt64,
    b Float64
)
ENGINE = CnchMergeTree() ORDER BY tuple();

set dialect_type = 'ANSI';

INSERT INTO wfagg2 (a, b)
VALUES (0, 0) (1, 4.5) (1, -2) (2, 5) (5, 9) (5, 99) (4, 1) (2, 1) (3, 2.1) (0, -5);
SELECT
  a,
  AVG(b) OVER (PARTITION BY a ORDER BY b ROWS BETWEEN 10 PRECEDING AND CURRENT ROW)
FROM
  wfagg2
ORDER BY a;
DROP TABLE wfagg2;
