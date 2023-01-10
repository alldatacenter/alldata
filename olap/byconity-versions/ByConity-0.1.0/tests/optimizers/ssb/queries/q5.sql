select sum(LO_REVENUE), toYear(LO_ORDERDATE) AS d_year, P_BRAND
from lineorder, part, supplier
where LO_PARTKEY = P_PARTKEY
and LO_SUPPKEY = S_SUPPKEY
and P_BRAND between 'MFGR#2221' and 'MFGR#2228'
and S_REGION = 'ASIA'
group by toYear(LO_ORDERDATE), P_BRAND
order by toYear(LO_ORDERDATE), P_BRAND;
