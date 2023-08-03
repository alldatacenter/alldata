set (SRCS
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/hive_metastore_constants.h
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/hive_metastore_constants.cpp
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/hive_metastore_types.h
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/hive_metastore_types.cpp
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/ThriftHiveMetastore.h
${ClickHouse_SOURCE_DIR}/contrib/hivemetastore/ThriftHiveMetastore.cpp
)

add_library(hivemetastore ${SRCS})
include(${ClickHouse_SOURCE_DIR}/contrib/thrift/build/cmake/ConfigureChecks.cmake)
configure_file("${ClickHouse_SOURCE_DIR}/contrib/thrift/build/cmake/config.h.in" "${ClickHouse_SOURCE_DIR}/contrib/thrift/lib/cpp/src/thrift/config.h")
target_include_directories(hivemetastore PUBLIC ../hivemetastore)
target_include_directories(hivemetastore PRIVATE ${ClickHouse_SOURCE_DIR}/contrib/thrift/lib/cpp/src)
target_link_libraries (hivemetastore PRIVATE boost::headers_only)
target_link_libraries(hivemetastore PUBLIC ${THRIFT_LIBRARY})
target_link_libraries(hivemetastore PUBLIC consistent-hashing)