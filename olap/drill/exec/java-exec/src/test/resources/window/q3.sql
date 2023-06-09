SELECT 
  position_id,
  employee_id,
  LAST_VALUE(employee_id)
    OVER(PARTITION BY position_id
         ORDER by employee_id
         RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS `last_value`
FROM
  dfs.`window/b4.p4`
