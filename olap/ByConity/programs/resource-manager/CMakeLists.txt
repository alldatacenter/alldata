if(ENABLE_TESTS)
    add_subdirectory(tests)
endif()

set(CLICKHOUSE_RESOURCE_MANAGER_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/resource_manager.cpp)
set(CLICKHOUSE_RESOURCE_MANAGER_LINK PRIVATE resource_manager_lib clickhouse_functions clickhouse_table_functions clickhouse_aggregate_functions clickhouse_navigation_functions PUBLIC daemon)

cnch_program_add(resource_manager)
