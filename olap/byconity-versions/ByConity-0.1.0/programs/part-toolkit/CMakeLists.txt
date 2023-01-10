set(CLICKHOUSE_PART_TOOLKIT_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/PartToolkit.cpp
)
set(CLICKHOUSE_PART_TOOLKIT_LINK
    PRIVATE
        Poco::Foundation
        clickhouse_parsers
        clickhouse_common_config
        clickhouse_common_io
)

clickhouse_program_add(part-toolkit)