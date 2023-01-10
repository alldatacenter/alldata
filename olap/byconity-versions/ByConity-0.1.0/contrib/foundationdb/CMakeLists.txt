set(FDB_ROOT ${ClickHouse_SOURCE_DIR}/contrib/foundationdb)
set(FDB_INCLUDE ${FDB_ROOT}/include)

add_library(foundationDB SHARED IMPORTED GLOBAL)
set_property(TARGET foundationDB PROPERTY IMPORTED_LOCATION ${FDB_ROOT}/lib/libfdb_c.so)
set_property(TARGET foundationDB PROPERTY INTERFACE_INCLUDE_DIRECTORIES ${FDB_INCLUDE})
install (FILES ${FDB_ROOT}/lib/libfdb_c.so DESTINATION ${CMAKE_INSTALL_LIBDIR} COMPONENT clickhouse)