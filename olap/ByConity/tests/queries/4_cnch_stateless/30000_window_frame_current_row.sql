DROP TABLE IF EXISTS window_frame_current_row;
CREATE TABLE window_frame_current_row
(
    a UInt64,
    b String,
    c Float64
)
ENGINE = CnchMergeTree() ORDER BY tuple();


INSERT INTO window_frame_current_row
VALUES (0, 'a', 4.2) (0, 'a', 4.1) (1, 'a', -2) (0, 'b', 0) (0, 'b', 1)  (2, 'c', 9) (1, 'b', -55);
SELECT
  a,
  b,
  SUM(c) AS S,
  rank() OVER (PARTITION BY a ORDER BY b ROWS CURRENT ROW) AS X,
  SUM(SUM(c)) OVER (PARTITION by a ORDER BY b ROWS CURRENT ROW) AS Y
FROM window_frame_current_row
GROUP BY a, b
ORDER BY a, b;

DROP TABLE window_frame_current_row;
