select
  first_value(col0) over w as col0,
  first_value(col1) over w as col1,
  first_value(col2) over w as col2,
  first_value(col3) over w as col3,
  first_value(col4) over w as col4,
  first_value(col5) over w as col5,
  first_value(col6) over w as col6,
  first_value(col7) over w as col7,
  first_value(col8) over w as col8
from
  dfs.`window/fewRowsAllData.parquet`
window w as ()
limit 1