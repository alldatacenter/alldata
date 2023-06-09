option(USE_INTERNAL_JSONC_LIBRARY "Set to FALSE to use system json-c library instead of bundled" ${NOT_UNBUNDLED})

if(NOT EXISTS "${ClickHouse_SOURCE_DIR}/contrib/json-c/json_object.h")
    if(USE_INTERNAL_JSONC_LIBRARY)
        message(FATAL_ERROR "submodule contrib/json-c is missing. to fix try run: \n git submodule update --init --recursive")
        set(USE_INTERNAL_JSONC_LIBRARY 0)
    endif()
    set(USE_INTERNAL_JSONC_LIBRARY 1)
endif()

if(NOT USE_INTERNAL_JSONC_LIBRARY)
    find_library(JSONC_LIBRARY json-c)
    find_path(JSONC_INCLUDE_DIR NAMES json.h PATHS ${ROCKSDB_INCLUDE_PATHS})
endif()

if(JSONC_LIBRARY AND JSONC_INCLUDE_DIR)
elseif(NOT MISSING_INTERNAL_JSONC_LIBRARY)
    set(JSONC_INCLUDE_DIR "${ClickHouse_SOURCE_DIR}/contrib/json-c" "${ClickHouse_BINARY_DIR}/contrib/json-c" CACHE INTERNAL "")
    set(USE_INTERNAL_JSONC_LIBRARY 1)
    set(JSONC_LIBRARY json-c)
endif()

if(JSONC_LIBRARY AND JSONC_INCLUDE_DIR)
    set(USE_JSONC 1)
endif()

message(STATUS "Using json-c=${USE_JSONC}: ${JSONC_INCLUDE_DIR} : ${JSONC_LIBRARY}")
