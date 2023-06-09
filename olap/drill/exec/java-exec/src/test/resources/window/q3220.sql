select
  count(1) over(partition by position_id order by sub)
from dfs.`window/b1.p1`