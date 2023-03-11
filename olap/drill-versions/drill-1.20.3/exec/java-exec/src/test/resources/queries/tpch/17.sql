-- tpch17 using 1395599672 as a seed to the RNG
select
  sum(l.l_extendedprice) / 7.0 as avg_yearly
from
  cp.`tpch/lineitem.parquet` l,
  cp.`tpch/part.parquet` p
where
  p.p_partkey = l.l_partkey
  and p.p_brand = 'Brand#13'
  and p.p_container = 'JUMBO CAN'
  and l.l_quantity < (
    select
      0.2 * avg(l2.l_quantity)
    from
      cp.`tpch/lineitem.parquet` l2
    where
      l2.l_partkey = p.p_partkey
  );
