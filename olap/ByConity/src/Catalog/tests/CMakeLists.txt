# add_executable(fdb_test fdb_test.cpp)
# target_link_libraries(fdb_test catalog_lib)

add_executable(metastore_benchmark metastore_benchmark.cpp)
target_link_libraries(metastore_benchmark catalog_lib benchmark::benchmark)