select C_CITY, S_CITY, toYear(LO_ORDERDATE) AS d_year, sum(LO_REVENUE) as revenue
from customer, lineorder, supplier
WHERE LO_CUSTKEY = C_CUSTKEY
AND LO_SUPPKEY = S_SUPPKEY
and C_NATION = 'UNITED STATES'
and S_NATION = 'UNITED STATES'
and toYear(LO_ORDERDATE) >= 1992 and toYear(LO_ORDERDATE) <= 1997
group by C_CITY, S_CITY, toYear(LO_ORDERDATE)
order by toYear(LO_ORDERDATE) asc, sum(LO_REVENUE) desc;
