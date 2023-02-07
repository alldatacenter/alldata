select  count(fv) as cnt
from (
        select  first_value(c2) over(partition by c2 order by c1) as fv
        from    dfs.`window/3668.parquet`
)
where   fv = 'e'