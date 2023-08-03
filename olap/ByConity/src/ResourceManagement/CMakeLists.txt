include(${ClickHouse_SOURCE_DIR}/cmake/dbms_glob_sources.cmake)
add_headers_and_sources(resource_manager_lib .)

add_library(resource_manager_lib ${resource_manager_lib_sources})
target_include_directories(resource_manager_lib SYSTEM BEFORE PUBLIC ${ClickHouse_SOURCE_DIR}/libs/libdaemon/include)
target_link_libraries(resource_manager_lib daemon dbms)