with x as(
    select
    a,
    b
    from q2_t1
    order by a
    )
select
    a
from x
order by a,b;