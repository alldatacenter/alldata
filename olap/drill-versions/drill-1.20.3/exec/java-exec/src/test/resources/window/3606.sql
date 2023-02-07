select
  col2,
  lead(col2) over(order by col0) as lead_col2
from
  dfs.`window/fewRowsAllData.parquet`