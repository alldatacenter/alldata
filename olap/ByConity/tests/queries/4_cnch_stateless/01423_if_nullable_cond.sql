SELECT CAST(null, 'Nullable(UInt8)') = 1 ? CAST(null, 'Nullable(UInt8)') : -1 AS x,
       toTypeName(CAST(null, 'Nullable(UInt8)') = 1 ? CAST(null, 'Nullable(UInt8)') : -1),
       dumpColumnStructure(CAST(null, 'Nullable(UInt8)') = 1 ? CAST(null, 'Nullable(UInt8)') : -1);
