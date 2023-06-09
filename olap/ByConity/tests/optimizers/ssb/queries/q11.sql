-- q4.1
select toYear(LO_ORDERDATE) AS d_year, C_NATION, sum(LO_REVENUE - LO_SUPPLYCOST) as profit
from customer, supplier, part, lineorder
WHERE LO_CUSTKEY = C_CUSTKEY
 AND LO_SUPPKEY = S_SUPPKEY
 AND LO_PARTKEY = P_PARTKEY
 and C_REGION = 'AMERICA'
 and S_REGION = 'AMERICA'
 and (P_MFGR = 'MFGR#1' or P_MFGR = 'MFGR#2')
group by toYear(LO_ORDERDATE), C_NATION
order by toYear(LO_ORDERDATE), C_NATION;
