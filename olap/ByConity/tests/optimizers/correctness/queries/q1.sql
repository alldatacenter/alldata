select
    a,
    b
from q1_t1
order by a,b
    union all
select
    a,
    b
from q1_t1
order by a
    limit 2