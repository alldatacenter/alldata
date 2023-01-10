select C_CITY, S_CITY, toYear(LO_ORDERDATE) AS d_year, sum(LO_REVENUE) as revenue
from customer, lineorder, supplier
where LO_CUSTKEY = C_CUSTKEY
and LO_SUPPKEY = S_SUPPKEY
and (C_CITY='UNITED KI1' or C_CITY='UNITED KI5')
and (S_CITY='UNITED KI1' or S_CITY='UNITED KI5')
and toYYYYMM(LO_ORDERDATE) = 199712
group by C_CITY, S_CITY, toYear(LO_ORDERDATE)
order by toYear(LO_ORDERDATE) asc, sum(LO_REVENUE) desc;
