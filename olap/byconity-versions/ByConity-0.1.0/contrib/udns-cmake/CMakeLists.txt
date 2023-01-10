set(LIBRARY_DIR ${ClickHouse_SOURCE_DIR}/contrib/udns)
set(UDNS_INCLUDE_DIR ${LIBRARY_DIR})
add_definitions(-DHAVE_CONFIG_H)

add_custom_target(
    udns_config
    COMMAND ${LIBRARY_DIR}/configure
    WORKING_DIRECTORY ${LIBRARY_DIR})

file(GLOB UDNS_DIR_SRC "${LIBRARY_DIR}/*.c")

add_library(udns STATIC ${UDNS_DIR_SRC})

add_dependencies(udns udns_config)

target_include_directories(udns PRIVATE ${LIBRARY_DIR})
