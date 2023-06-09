select
  lead(col3) over(partition by col2 order by col0) lead_col0
from
  dfs.`window/fewRowsAllData.parquet`