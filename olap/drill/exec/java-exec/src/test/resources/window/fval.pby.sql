select
  first_value(employee_id) over(partition by position_id order by line_no) as `first_value`
from
  dfs.`window/b4.p4`