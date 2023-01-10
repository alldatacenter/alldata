-- q3.1
select C_NATION, S_NATION, toYear(LO_ORDERDATE) AS d_year, sum(LO_REVENUE) as revenue
from customer, lineorder, supplier
where LO_CUSTKEY = C_CUSTKEY
and LO_SUPPKEY = S_SUPPKEY
and C_REGION = 'ASIA' and S_REGION = 'ASIA'
and toYear(LO_ORDERDATE) >= 1992 and toYear(LO_ORDERDATE) <= 1997
group by C_NATION, S_NATION, toYear(LO_ORDERDATE)
order by toYear(LO_ORDERDATE) asc, sum(LO_REVENUE) desc;
