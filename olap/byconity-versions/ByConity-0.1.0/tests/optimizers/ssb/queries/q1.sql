-- q1.1
select sum(LO_EXTENDEDPRICE*LO_DISCOUNT) as revenue
from ssb.lineorder
where toYear(LO_ORDERDATE) = 1993
and LO_DISCOUNT between 1 and 3
and LO_QUANTITY < 25;
