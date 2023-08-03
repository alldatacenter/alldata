set(CLICKHOUSE_TSO_HEADER
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOOperations.h
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOProxy.h
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOImpl.h
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOClient.h
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOServer.h
   )

set(CLICKHOUSE_TSO_SOURCE
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOOperations.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOProxy.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOImpl.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOClient.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/TSOServer.cpp
        )

add_library(tso_lib ${CLICKHOUSE_TSO_HEADER} ${CLICKHOUSE_TSO_SOURCE})
target_include_directories(tso_lib SYSTEM BEFORE PRIVATE ${BYTEKV_INCLUDE_DIR} PUBLIC ${ClickHouse_SOURCE_DIR}/libs/libdaemon/include)
target_link_libraries(tso_lib daemon dbms)

if (ENABLE_TESTS)
    add_subdirectory(tests)
endif()
