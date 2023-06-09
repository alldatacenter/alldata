select
    n1,
    fake
from
    (
        select
            number n1,
            array(1, number) as fake
        from
            system.numbers
        limit
            100
    )
    inner join (
        select
            number as n2
        from
            system.numbers
        limit
            100
    ) on n1 = n2
where
    arrayExists((`x`) -> `x` in (2, 3), fake) = 1