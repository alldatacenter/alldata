-- q2.1
select sum(LO_REVENUE), toYear(LO_ORDERDATE) AS d_year, P_BRAND
from lineorder, part, supplier
where LO_PARTKEY = P_PARTKEY
and LO_SUPPKEY = S_SUPPKEY
and P_CATEGORY = 'MFGR#12'
and S_REGION = 'AMERICA'
group by toYear(LO_ORDERDATE), P_BRAND
order by toYear(LO_ORDERDATE), P_BRAND;
