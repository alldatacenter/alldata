select
  ntile(5)
    over(partition by col7 order by col0) as `ntile`
from
  dfs.`window/3648.parquet`