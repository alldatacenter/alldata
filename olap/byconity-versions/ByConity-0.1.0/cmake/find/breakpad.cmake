option (ENABLE_BREAKPAD "Enable BREAKPAD" OFF)

if (APPLE)
    set (ENABLE_BREAKPAD 0)
endif ()

if (ENABLE_BREAKPAD AND NOT EXISTS "${ClickHouse_SOURCE_DIR}/contrib/breakpad/src/client/linux/handler/exception_handler.h")
    message (FATAL_ERROR "submodule contrib/breakpad is missing. to fix try run: \n git submodule update --init --recursive")
endif()

if (ENABLE_BREAKPAD)
    set(USE_BREAKPAD 1)
endif()
