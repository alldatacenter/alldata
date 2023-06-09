select
  count(*) over pos_win `count`,
  sum(salary) over pos_win `sum`
from
  dfs.`window/%s`
window pos_win as %s