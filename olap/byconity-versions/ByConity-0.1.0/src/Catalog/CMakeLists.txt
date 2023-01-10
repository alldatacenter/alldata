include(${ClickHouse_SOURCE_DIR}/cmake/dbms_glob_sources.cmake)
add_headers_and_sources(catalog_lib .)

add_library(catalog_lib ${catalog_lib_sources})
target_include_directories(catalog_lib SYSTEM BEFORE
    PRIVATE ${BYTEKV_INCLUDE_DIR}
    PUBLIC ${ClickHouse_SOURCE_DIR}/libs/libdaemon/include
    PUBLIC ${ClickHouse_SOURCE_DIR}/contrib/foundationdb/include)

target_link_libraries(catalog_lib daemon dbms ${RDKAFKA_LIBRARY} ${CPPKAFKA_LIBRARY} foundationDB)

add_subdirectory(tests)
