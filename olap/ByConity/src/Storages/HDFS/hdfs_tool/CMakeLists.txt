
add_executable (min_bench min_bench.cpp)
# add_custom_command(TARGET min_bench POST_BUILD COMMAND ${CMAKE_STRIP} min_bench)
target_link_libraries(min_bench clickhouse_common_io)


add_executable(hdfs_command hdfs_command.cpp)
# add_custom_command(TARGET hdfs_command POST_BUILD COMMAND ${CMAKE_STRIP} hdfs_command)
target_link_libraries(hdfs_command clickhouse_common_io)
