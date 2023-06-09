CREATE TABLE customer
(
    C_CUSTKEY     UInt32,
    C_NAME        String,
    C_ADDRESS     String,
    C_CITY        String,
    C_NATION      String,
    C_REGION      String,
    C_PHONE       FixedString(15),
    C_MKTSEGMENT  String
) ENGINE=CnchMergeTree() ORDER BY (C_CUSTKEY);

CREATE TABLE dwdate
(
    D_DATEKEY            UInt32,
    D_DATE               String,
    D_DAYOFWEEK          String,    -- defined in Section 2.6 as Size 8, but Wednesday is 9 letters
    D_MONTH              String,
    D_YEAR               UInt32,
    D_YEARMONTHNUM       UInt32,
    D_YEARMONTH          String,
    D_DAYNUMINWEEK       UInt32,
    D_DAYNUMINMONTH      UInt32,
    D_DAYNUMINYEAR       UInt32,
    D_MONTHNUMINYEAR     UInt32,
    D_WEEKNUMINYEAR      UInt32,
    D_SELLINGSEASON      String,
    D_LASTDAYINWEEKFL    UInt32,
    D_LASTDAYINMONTHFL   UInt32,
    D_HOLIDAYFL          UInt32,
    D_WEEKDAYFL          UInt32
) ENGINE=CnchMergeTree() ORDER BY (D_DATEKEY);

CREATE TABLE lineorder
(
    LO_ORDERKEY             UInt32,
    LO_LINENUMBER           UInt8,
    LO_CUSTKEY              UInt32,
    LO_PARTKEY              UInt32,
    LO_SUPPKEY              UInt32,
    LO_ORDERDATE            Date,
    LO_ORDERPRIORITY        String,
    LO_SHIPPRIORITY         UInt8,
    LO_QUANTITY             UInt8,
    LO_EXTENDEDPRICE        UInt32,
    LO_ORDTOTALPRICE        UInt32,
    LO_DISCOUNT             UInt8,
    LO_REVENUE              UInt32,
    LO_SUPPLYCOST           UInt32,
    LO_TAX                  UInt8,
    LO_COMMITDATE           Date,
    LO_SHIPMODE             String
) ENGINE=CnchMergeTree() ORDER BY (LO_ORDERDATE, LO_ORDERKEY);

CREATE TABLE part
(
    P_PARTKEY       UInt32,
    P_NAME          String,
    P_MFGR          String,
    P_CATEGORY      String,
    P_BRAND         String,
    P_COLOR         String,
    P_TYPE          String,
    P_SIZE          UInt8,
    P_CONTAINER     String
) ENGINE=CnchMergeTree() ORDER BY (P_PARTKEY);

CREATE TABLE supplier
(
    S_SUPPKEY       UInt32,
    S_NAME          String,
    S_ADDRESS       String,
    S_CITY          String,
    S_NATION        String,
    S_REGION        String,
    S_PHONE         String
) ENGINE=CnchMergeTree() ORDER BY (S_SUPPKEY);

CREATE TABLE lineorder_flat
(
    `LO_ORDERKEY` UInt32,
    `LO_LINENUMBER` UInt8,
    `LO_CUSTKEY` UInt32,
    `LO_PARTKEY` UInt32,
    `LO_SUPPKEY` UInt32,
    `LO_ORDERDATE` Date,
    `LO_ORDERPRIORITY` String,
    `LO_SHIPPRIORITY` UInt8,
    `LO_QUANTITY` UInt8,
    `LO_EXTENDEDPRICE` UInt32,
    `LO_ORDTOTALPRICE` UInt32,
    `LO_DISCOUNT` UInt8,
    `LO_REVENUE` UInt32,
    `LO_SUPPLYCOST` UInt32,
    `LO_TAX` UInt8,
    `LO_COMMITDATE` Date,
    `LO_SHIPMODE` String,
    `C_NAME` String,
    `C_ADDRESS` String,
    `C_CITY` String,
    `C_NATION` String,
    `C_REGION` String,
    `C_PHONE` FixedString(15),
    `C_MKTSEGMENT` String,
    `S_NAME` String,
    `S_ADDRESS` String,
    `S_CITY` String,
    `S_NATION` String,
    `S_REGION` String,
    `S_PHONE` String,
    `P_NAME` String,
    `P_MFGR` String,
    `P_CATEGORY` String,
    `P_BRAND` String,
    `P_COLOR` String,
    `P_TYPE` String,
    `P_SIZE` UInt8,
    `P_CONTAINER` String
) ENGINE = CnchMergeTree
PARTITION BY toYear(LO_ORDERDATE)
ORDER BY (LO_ORDERDATE, LO_ORDERKEY);