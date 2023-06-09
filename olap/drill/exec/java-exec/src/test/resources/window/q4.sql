SELECT
  position_id,
  employee_id,
  MAX(employee_id) OVER(PARTITION BY position_id) AS `last_value`
FROM (
  SELECT *
  FROM dfs.`window/b4.p4`
  ORDER BY position_id, employee_id
)
