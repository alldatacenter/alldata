option (ENABLE_BRPC "Enable brpc" ON)

if (ENABLE_BRPC)

    option (USE_INTERNAL_BRPC_LIBRARY "Set to FALSE to use system brpc library instead of bundled" ${NOT_UNBUNDLED})
    if (NOT EXISTS "${ClickHouse_SOURCE_DIR}/contrib/incubator-brpc/src/brpc/amf.h")
        if (USE_INTERNAL_BRPC_LIBRARY)
            message (WARNING "submodule contrib/brpc is missing. to fix try run: \n git submodule update --init --recursive")
            set (USE_INTERNAL_BRPC_LIBRARY 0)
        endif ()
        set (MISSING_INTERNAL_BRPC_LIBRARY 1)
    endif ()

    if(NOT USE_INTERNAL_BRPC_LIBRARY)
        find_library(BRPC_LIBRARY brpc)
        find_path(BRPC_INCLUDE_DIR NAMES brpc/amf.h PATHS ${BRPC_INCLUDE_PATHS})
    endif()

    if (BRPC_LIBRARY AND BRPC_INCLUDE_DIR)
        set (USE_BRPC 1)
    elseif (NOT MISSING_INTERNAL_BRPC_LIBRARY)
        set (USE_INTERNAL_BRPC_LIBRARY 1)
        set (USE_BRPC 1)
    endif ()

endif()

message (STATUS "Using brpc=${USE_BRPC}")
