select
  max(cast(columns[2] as char(2)))
    over(partition by cast(columns[2] as char(2))
         order by cast(columns[0] as int))
from dfs.`window/allData.csv`