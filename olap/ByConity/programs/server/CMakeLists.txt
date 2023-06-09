include(${ClickHouse_SOURCE_DIR}/cmake/embed_binary.cmake)

set(CLICKHOUSE_SERVER_SOURCES
    MetricsTransmitter.cpp
    Server.cpp
)

set (LINK_RESOURCE_LIB INTERFACE "-Wl,${WHOLE_ARCHIVE} $<TARGET_FILE:clickhouse_server_configs> -Wl,${NO_WHOLE_ARCHIVE}")

set (CLICKHOUSE_SERVER_LINK
    PRIVATE
        clickhouse_aggregate_functions
        clickhouse_common_config
        clickhouse_common_io
        clickhouse_common_zookeeper
        clickhouse_dictionaries
        clickhouse_functions
        clickhouse_parsers
        clickhouse_storages_system
        clickhouse_table_functions
        string_utils
        jemalloc

    ${LINK_RESOURCE_LIB}

    PUBLIC
        daemon
)

clickhouse_program_add(server)

install(FILES byconity-server.xml byconity-worker.xml byconity-worker-write.xml cnch_config.xml byconity-daemon-manager.xml byconity-tso.xml users.xml byconity-resource-manager.xml DESTINATION "${CLICKHOUSE_ETC_DIR}/byconity-server" COMPONENT clickhouse)

clickhouse_embed_binaries(
    TARGET clickhouse_server_configs
    RESOURCES byconity-server.xml byconity-worker.xml byconity-worker-write.xml cnch_config.xml byconity-daemon-manager.xml byconity-tso.xml users.xml embedded.xml play.html
)
add_dependencies(clickhouse-server-lib clickhouse_server_configs)
