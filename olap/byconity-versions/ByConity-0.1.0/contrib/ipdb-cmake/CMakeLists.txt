set(IPDB_SOURCE_DIR ${CMAKE_SOURCE_DIR}/contrib/ipdb)
add_library(ipdb
            ${IPDB_SOURCE_DIR}/ipdb.c
            ${IPDB_SOURCE_DIR}/ipdb.h)

target_include_directories (ipdb PUBLIC ${IPDB_SOURCE_DIR})
include_directories(${JSONC_INCLUDE_DIR})
target_link_libraries(ipdb ${JSONC_LIBRARY})