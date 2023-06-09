select
  last_value(col0) over w as col0,
  last_value(col1) over w as col1,
  last_value(col2) over w as col2,
  last_value(col3) over w as col3,
  last_value(col4) over w as col4,
  last_value(col5) over w as col5,
  last_value(col6) over w as col6,
  last_value(col7) over w as col7,
  last_value(col8) over w as col8
from
  dfs.`window/fewRowsAllData.parquet`
window w as ()
limit 1