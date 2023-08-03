DROP TABLE IF EXISTS wfagg3;
CREATE TABLE wfagg3
(
    a UInt64,
    b Float64
)
ENGINE = CnchMergeTree() ORDER BY tuple();

INSERT INTO wfagg3 (a, b)
VALUES (0, 0) (1, 4.5) (1, -2) (2, 5) (5, 9) (5, 99) (4, 1) (2, 1) (3, 2.1) (0, -5);
SELECT
  a,
  AVG(b) OVER (PARTITION BY a ROWS UNBOUNDED PRECEDING)
FROM
  wfagg3
ORDER BY a;
DROP TABLE wfagg3;
