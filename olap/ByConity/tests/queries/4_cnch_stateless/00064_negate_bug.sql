SELECT x, toTypeName(x) AS t FROM (SELECT -toUInt32(1) AS x)
