create table nation (
    n_nationkey  Int32,
    n_name  Nullable(String),
    n_regionkey  Nullable(Int32),
    n_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY n_nationkey;
create table region (
    r_regionkey  Int32,
    r_name  Nullable(String),
    r_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY r_regionkey;
create table part (
    p_partkey  Int32,
    p_name  Nullable(String),
    p_mfgr  Nullable(String),
    p_brand  Nullable(String),
    p_type  Nullable(String),
    p_size  Nullable(Int32),
    p_container  Nullable(String),
    p_retailprice  Nullable(Float64),
    p_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY p_partkey;
create table supplier (
    s_suppkey  Int32,
    s_name  Nullable(String),
    s_address  Nullable(String),
    s_nationkey  Nullable(Int32),
    s_phone  Nullable(String),
    s_acctbal  Nullable(Float64),
    s_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY s_suppkey;
create table partsupp (
    ps_partkey  Int32,
    ps_suppkey  Int32,
    ps_availqty  Nullable(Int32),
    ps_supplycost  Nullable(Float64),
    ps_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY ps_partkey;
create table customer (
    c_custkey  Int32,
    c_name  Nullable(String),
    c_address  Nullable(String),
    c_nationkey  Nullable(Int32),
    c_phone  Nullable(String),
    c_acctbal  Nullable(Float64),
    c_mktsegment  Nullable(String),
    c_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY c_custkey;
create table orders (
    o_orderkey  Int32,
    o_custkey  Nullable(Int32),
    o_orderstatus  Nullable(String),
    o_totalprice  Nullable(Float64),
    o_orderdate Nullable(date),
    o_orderpriority  Nullable(String),
    o_clerk  Nullable(String),
    o_shippriority  Nullable(Int32),
    o_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY o_orderkey;
create table lineitem (
    l_orderkey  Int32,
    l_partkey  Nullable(Int32),
    l_suppkey  Nullable(Int32),
    l_linenumber  Nullable(Int32),
    l_quantity  Nullable(Float64),
    l_extendedprice  Nullable(Float64),
    l_discount  Nullable(Float64),
    l_tax  Nullable(Float64),
    l_returnflag  Nullable(String),
    l_linestatus  Nullable(String),
    l_shipdate Nullable(date),
    l_commitdate Nullable(date),
    l_receiptdate Nullable(date),
    l_shipinstruct  Nullable(String),
    l_shipmode  Nullable(String),
    l_comment  Nullable(String)
) ENGINE = CnchMergeTree()
ORDER BY l_orderkey;