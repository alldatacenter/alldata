set(LIBRARY_DIR ${ClickHouse_SOURCE_DIR}/contrib/udns)
set(UDNS_INCLUDE_DIR ${LIBRARY_DIR})

file(GLOB UDNS_DIR_SRC "${LIBRARY_DIR}/*.c")

add_library(udns STATIC ${UDNS_DIR_SRC})

target_include_directories(udns PRIVATE ${LIBRARY_DIR})
