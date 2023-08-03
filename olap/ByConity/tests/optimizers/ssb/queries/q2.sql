-- q1.2
select sum(LO_EXTENDEDPRICE*LO_DISCOUNT) as revenue
from ssb.lineorder
where toYYYYMM(LO_ORDERDATE) = 199401
and LO_DISCOUNT between 4 and 6
and LO_QUANTITY between 26 and 35;
