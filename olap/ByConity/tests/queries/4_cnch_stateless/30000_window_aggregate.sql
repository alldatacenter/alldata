DROP TABLE IF EXISTS wfagg1;
CREATE TABLE wfagg1
(
    a UInt64,
    b Float64
)
ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfagg1 (a, b)
VALUES (0, 0) (1, 4.5) (1, -2) (2, 5) (5, 9) (5, 99) (4, 1) (2, 1) (3, 2.1) (0, -5);
SELECT
  a,
  AVG(b) OVER (PARTITION BY a)
FROM
  wfagg1
ORDER BY a;
DROP TABLE wfagg1;
