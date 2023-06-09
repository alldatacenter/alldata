select
  count(*) over pos_win `count`,
  sum(salary) over pos_win `sum`,
  row_number() over pos_win `row_number`,
  rank() over pos_win `rank`,
  dense_rank() over pos_win `dense_rank`,
  cume_dist() over pos_win `cume_dist`,
  percent_rank() over pos_win `percent_rank`
from dfs.`window/%s`
window pos_win as %s