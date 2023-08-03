-- q1.3
select sum(LO_EXTENDEDPRICE*LO_DISCOUNT) as revenue
from ssb.lineorder
where toISOWeek(LO_ORDERDATE) = 6
and toYear(LO_ORDERDATE)= 1994
and LO_DISCOUNT between 5 and 7
and LO_QUANTITY between 26 and 35;
