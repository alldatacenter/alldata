add_library (daemon
    BaseDaemon.cpp
    GraphiteWriter.cpp
    SentryWriter.cpp
)

target_include_directories (daemon PUBLIC ..)

if (OS_DARWIN AND NOT MAKE_STATIC_LIBRARIES)
    target_link_libraries (daemon PUBLIC -Wl,-undefined,dynamic_lookup)
endif()

target_link_libraries (daemon PUBLIC loggers PRIVATE clickhouse_common_io clickhouse_common_config common ${EXECINFO_LIBRARIES})

if (USE_SENTRY)
    target_link_libraries (daemon PRIVATE ${SENTRY_LIBRARY})
endif ()

if (USE_BREAKPAD)
    target_link_libraries(daemon PRIVATE breakpad_client)
    # Ignore warnings while compiling breakpad
    target_compile_options(daemon PRIVATE "-w")

    add_dependencies(daemon minidump_stackwalk minidump_dump minidump-2-core core2md dump_syms)

    install(TARGETS minidump_stackwalk minidump_dump minidump-2-core core2md dump_syms
        RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX}/usr/breakpad/bin
        COMPONENT breakpad
    )
endif (USE_BREAKPAD)
