if (USE_INCLUDE_WHAT_YOU_USE)
    set (CMAKE_CXX_INCLUDE_WHAT_YOU_USE ${IWYU_PATH})
endif ()

if (USE_CLANG_TIDY)
    set (CMAKE_CXX_CLANG_TIDY "${CLANG_TIDY_PATH}")
endif ()

if(COMPILER_PIPE)
    set(MAX_COMPILER_MEMORY 2500)
else()
    set(MAX_COMPILER_MEMORY 1500)
endif()
if(MAKE_STATIC_LIBRARIES)
    set(MAX_LINKER_MEMORY 3500)
else()
    set(MAX_LINKER_MEMORY 2500)
endif()
include(../cmake/limit_jobs.cmake)

set (CONFIG_VERSION "${CMAKE_CURRENT_BINARY_DIR}/Common/config_version.h")
set (CONFIG_COMMON "${CMAKE_CURRENT_BINARY_DIR}/Common/config.h")

include (../cmake/version.cmake)
message (STATUS "Will build ${VERSION_FULL} revision ${VERSION_REVISION} ${VERSION_OFFICIAL}")
configure_file (Common/config.h.in ${CONFIG_COMMON})
configure_file (Common/config_version.h.in ${CONFIG_VERSION})
configure_file (Core/config_core.h.in "${CMAKE_CURRENT_BINARY_DIR}/Core/include/config_core.h")

if (USE_DEBUG_HELPERS)
    set (INCLUDE_DEBUG_HELPERS "-I\"${ClickHouse_SOURCE_DIR}/base\" -include \"${ClickHouse_SOURCE_DIR}/src/Core/iostream_debug_helpers.h\"")
    set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${INCLUDE_DEBUG_HELPERS}")
endif ()

if (COMPILER_GCC)
    # If we leave this optimization enabled, gcc-7 replaces a pair of SSE intrinsics (16 byte load, store) with a call to memcpy.
    # It leads to slow code. This is compiler bug. It looks like this:
    #
    # (gdb) bt
    #0  memcpy (destination=0x7faa6e9f1638, source=0x7faa81d9e9a8, size=16) at ../libs/libmemcpy/memcpy.h:11
    #1  0x0000000005341c5f in _mm_storeu_si128 (__B=..., __P=<optimized out>) at /usr/lib/gcc/x86_64-linux-gnu/7/include/emmintrin.h:720
    #2  memcpySmallAllowReadWriteOverflow15Impl (n=<optimized out>, src=<optimized out>, dst=<optimized out>) at ../src/Common/memcpySmall.h:37

    add_definitions ("-fno-tree-loop-distribute-patterns")
endif ()

add_subdirectory (Access)
add_subdirectory (Columns)
add_subdirectory (Common)
add_subdirectory (Core)
add_subdirectory (DataStreams)
add_subdirectory (DataTypes)
add_subdirectory (Dictionaries)
add_subdirectory (Disks)
add_subdirectory (Storages)
add_subdirectory (Parsers)
add_subdirectory (IO)
add_subdirectory (Functions)
add_subdirectory (Interpreters)
add_subdirectory (AggregateFunctions)
add_subdirectory (Client)
add_subdirectory (TableFunctions)
add_subdirectory (Processors)
add_subdirectory (Formats)
add_subdirectory (Compression)
add_subdirectory (Server)
add_subdirectory (Coordination)
add_subdirectory (Protos)
add_subdirectory (FormaterTool)
add_subdirectory (ServiceDiscovery)
add_subdirectory (ResourceManagement)
add_subdirectory (Transaction)
add_subdirectory (MergeTreeCommon)
add_subdirectory (Catalog)
add_subdirectory (TSO)
add_subdirectory (DaemonManager)
add_subdirectory (Optimizer)


set(dbms_headers)
set(dbms_sources)

add_headers_and_sources(clickhouse_common_io Common)
add_headers_and_sources(clickhouse_common_io Common/HashTable)
add_headers_and_sources(clickhouse_common_io Common/CGroup)
add_headers_and_sources(clickhouse_common_io Common/Brpc)
add_headers_and_sources(clickhouse_common_io IO)
add_headers_and_sources(clickhouse_common_io IO/S3)
add_headers_and_sources(clickhouse_common_io ServiceDiscovery)
list (REMOVE_ITEM clickhouse_common_io_sources Common/malloc.cpp Common/new_delete.cpp)
add_headers_and_sources(dbms Storages/IndexFile)
add_headers_and_sources(dbms Storages/IngestColumnCnch)
add_headers_and_sources(dbms FormaterTool)
add_headers_and_sources(dbms Transaction)
add_headers_and_sources(dbms Transaction/Actions)
add_headers_and_sources(dbms MergeTreeCommon)
add_headers_and_sources(dbms Catalog)
add_headers_and_sources(dbms CloudServices)
add_headers_and_sources(dbms WorkerTasks)
add_headers_and_sources(dbms ResourceGroup)
add_headers_and_sources(dbms ResourceManagement)
add_headers_and_sources(dbms TSO)

if(USE_RDKAFKA)
    add_headers_and_sources(dbms Storages/Kafka)
endif()

if (USE_AMQPCPP)
    add_headers_and_sources(dbms Storages/RabbitMQ)
endif()

if (USE_LIBPQXX)
    add_headers_and_sources(dbms Core/PostgreSQL)
    add_headers_and_sources(dbms Databases/PostgreSQL)
    add_headers_and_sources(dbms Storages/PostgreSQL)
endif()

if (USE_ROCKSDB)
    add_headers_and_sources(dbms Storages/RocksDB)
endif()

if (USE_AWS_S3)
    add_headers_and_sources(dbms Common/S3)
    add_headers_and_sources(dbms Disks/S3)
    add_headers_and_sources(dbms Storages/S3)
endif()

if (USE_HDFS)
    add_headers_and_sources(dbms Storages/HDFS)
    add_headers_and_sources(dbms Disks/HDFS)
endif()

add_headers_and_sources(dbms Protos)

list (APPEND clickhouse_common_io_sources ${CONFIG_BUILD})
list (APPEND clickhouse_common_io_headers ${CONFIG_VERSION} ${CONFIG_COMMON})

list (APPEND dbms_sources Functions/IFunction.cpp Functions/FunctionFactory.cpp Functions/FunctionHelpers.cpp Functions/extractTimeZoneFromFunctionArguments.cpp Functions/replicate.cpp Functions/FunctionsLogical.cpp)
list (APPEND dbms_headers Functions/IFunction.h Functions/FunctionFactory.h Functions/FunctionHelpers.h Functions/extractTimeZoneFromFunctionArguments.h Functions/replicate.h Functions/FunctionsLogical.h)

list (APPEND dbms_sources
    AggregateFunctions/IAggregateFunction.cpp
    AggregateFunctions/AggregateFunctionFactory.cpp
    AggregateFunctions/AggregateFunctionCombinatorFactory.cpp
    AggregateFunctions/AggregateFunctionState.cpp
    AggregateFunctions/AggregateFunctionCount.cpp
    AggregateFunctions/parseAggregateFunctionParameters.cpp)

list (APPEND dbms_headers
    AggregateFunctions/IAggregateFunction.h
    AggregateFunctions/IAggregateFunctionCombinator.h
    AggregateFunctions/AggregateFunctionFactory.h
    AggregateFunctions/AggregateFunctionCombinatorFactory.h
    AggregateFunctions/AggregateFunctionState.h
    AggregateFunctions/AggregateFunctionCount.cpp
    AggregateFunctions/FactoryHelpers.h
    AggregateFunctions/parseAggregateFunctionParameters.h)

list (APPEND dbms_sources TableFunctions/ITableFunction.cpp TableFunctions/TableFunctionFactory.cpp)
list (APPEND dbms_headers TableFunctions/ITableFunction.h   TableFunctions/TableFunctionFactory.h)
list (APPEND dbms_sources Dictionaries/DictionaryFactory.cpp Dictionaries/DictionarySourceFactory.cpp Dictionaries/DictionaryStructure.cpp  Dictionaries/getDictionaryConfigurationFromAST.cpp)
list (APPEND dbms_headers Dictionaries/DictionaryFactory.h   Dictionaries/DictionarySourceFactory.h   Dictionaries/DictionaryStructure.h Dictionaries/getDictionaryConfigurationFromAST.h)

if (NOT ENABLE_SSL)
    list (REMOVE_ITEM clickhouse_common_io_sources Common/OpenSSLHelpers.cpp)
    list (REMOVE_ITEM clickhouse_common_io_headers Common/OpenSSLHelpers.h)
endif ()

add_library(clickhouse_common_io ${clickhouse_common_io_headers} ${clickhouse_common_io_sources})

if (SPLIT_SHARED_LIBRARIES)
    target_compile_definitions(clickhouse_common_io PRIVATE SPLIT_SHARED_LIBRARIES)
endif ()

add_library (clickhouse_malloc OBJECT Common/malloc.cpp)
set_source_files_properties(Common/malloc.cpp PROPERTIES COMPILE_FLAGS "-fno-builtin")

if (((SANITIZE STREQUAL "thread") OR (SANITIZE STREQUAL "address")) AND COMPILER_GCC)
    message(WARNING "Memory tracking is disabled, due to gcc sanitizers")
else()
    add_library (clickhouse_new_delete STATIC Common/new_delete.cpp)
    target_link_libraries (clickhouse_new_delete PRIVATE clickhouse_common_io jemalloc)
endif()

add_subdirectory(Common/ZooKeeper)
add_subdirectory(Common/Config)

set (all_modules)
macro(add_object_library name common_path)
    if (MAKE_STATIC_LIBRARIES OR NOT SPLIT_SHARED_LIBRARIES)
        add_headers_and_sources(dbms ${common_path})
    else ()
        list (APPEND all_modules ${name})
        add_headers_and_sources(${name} ${common_path})
        add_library(${name} SHARED ${${name}_sources} ${${name}_headers})
        if (OS_DARWIN)
            target_link_libraries (${name} PRIVATE -Wl,-undefined,dynamic_lookup)
        else()
            target_link_libraries (${name} PRIVATE -Wl,--unresolved-symbols=ignore-all)
        endif()
    endif ()
endmacro()

add_object_library(clickhouse_access Access)
add_object_library(clickhouse_core Core)
add_object_library(clickhouse_core_mysql Core/MySQL)
add_object_library(clickhouse_compression Compression)
add_object_library(clickhouse_datastreams DataStreams)
add_object_library(clickhouse_datatypes DataTypes)
add_object_library(clickhouse_datatypes_serializations DataTypes/Serializations)
add_object_library(clickhouse_databases Databases)
add_object_library(clickhouse_databases_mysql Databases/MySQL)
add_object_library(clickhouse_disks Disks)
add_object_library(clickhouse_interpreters Interpreters)
add_object_library(clickhouse_interpreters_mysql Interpreters/MySQL)
add_object_library(clickhouse_interpreters_clusterproxy Interpreters/ClusterProxy)
add_object_library(clickhouse_interpreters_jit Interpreters/JIT)
add_object_library(clickhouse_interpreters_distributedstages Interpreters/DistributedStages)
add_object_library(clickhouse_interpreters_runtimefilters Interpreters/RuntimeFilter)
add_object_library(clickhouse_interpreters_cnch_query_metrics Interpreters/CnchQueryMetrics)
add_object_library(clickhouse_columns Columns)
add_object_library(clickhouse_storages Storages)
add_object_library(clickhouse_disk_cache Storages/DiskCache)
add_object_library(clickhouse_storages_hive Storages/Hive)
add_object_library(clickhouse_storages_mysql Storages/MySQL)
add_object_library(clickhouse_storages_distributed Storages/Distributed)
add_object_library(clickhouse_storages_mergetree Storages/MergeTree)
add_object_library(clickhouse_storages_liveview Storages/LiveView)
add_object_library(clickhouse_client Client)
add_object_library(clickhouse_bridge Bridge)
add_object_library(clickhouse_server Server)
add_object_library(clickhouse_server_http Server/HTTP)
add_object_library(clickhouse_formats Formats)
add_object_library(clickhouse_processors Processors)
add_object_library(clickhouse_processors_executors Processors/Executors)
add_object_library(clickhouse_processors_formats Processors/Formats)
add_object_library(clickhouse_processors_formats_impl Processors/Formats/Impl)
add_object_library(clickhouse_processors_transforms Processors/Transforms)
add_object_library(clickhouse_processors_sources Processors/Sources)
add_object_library(clickhouse_processors_merges Processors/Merges)
add_object_library(clickhouse_processors_merges_algorithms Processors/Merges/Algorithms)
add_object_library(clickhouse_processors_exchange Processors/Exchange)
add_object_library(clickhouse_processors_exchange_datatrans Processors/Exchange/DataTrans)
add_object_library(clickhouse_processors_exchange_datatrans_brpc Processors/Exchange/DataTrans/Brpc)
add_object_library(clickhouse_processors_exchange_datatrans_local Processors/Exchange/DataTrans/Local)
add_object_library(clickhouse_analyzers Analyzers)
add_object_library(clickhouse_queryplan QueryPlan)
add_object_library(clickhouse_queryplan_optimizations QueryPlan/Optimizations)
add_object_library(clickhouse_optimizer Optimizer)
add_object_library(clickhouse_optimizer_iterative Optimizer/Iterative)
add_object_library(clickhouse_optimizer_costmodel Optimizer/CostModel)
add_object_library(clickhouse_optimizer_cardinality Optimizer/CardinalityEstimate)
add_object_library(clickhouse_optimizer_cascades Optimizer/Cascades)
add_object_library(clickhouse_optimizer_materialized_view Optimizer/MaterializedView)
add_object_library(clickhouse_optimizer_rule Optimizer/Rule)
add_object_library(clickhouse_optimizer_rule_transformation Optimizer/Rule/Transformation)
add_object_library(clickhouse_optimizer_rule_implementation Optimizer/Rule/Implementation)
add_object_library(clickhouse_optimizer_rewrite Optimizer/Rule/Rewrite)
add_object_library(clickhouse_optimizer_property Optimizer/Property)
add_object_library(clickhouse_optimizer_rewriter Optimizer/Rewriter)
add_object_library(clickhouse_optimizer_dump Optimizer/Dump)
add_object_library(clickhouse_statistics Statistics)


if (USE_NURAFT)
    add_object_library(clickhouse_coordination Coordination)
endif()

set (DBMS_COMMON_LIBRARIES)
# libgcc_s does not provide an implementation of an atomics library. Instead,
# GCC’s libatomic library can be used to supply these when using libgcc_s.
if ((NOT USE_LIBCXX) AND COMPILER_CLANG AND OS_LINUX)
    list (APPEND DBMS_COMMON_LIBRARIES atomic)
endif()


# hash functions
option(ENABLE_MULTITARGET_CODE "Enable platform-dependent code" ON)

if (ENABLE_MULTITARGET_CODE)
    add_definitions(-DENABLE_MULTITARGET_CODE=1)
else()
    add_definitions(-DENABLE_MULTITARGET_CODE=0)
endif()
list (APPEND DBMS_COMMON_LIBRARIES ${CITYHASH_LIBRARIES})
list (APPEND DBMS_COMMON_LIBRARIES ${FARMHASH_LIBRARIES})
list (APPEND DBMS_COMMON_LIBRARIES metrohash)
list (APPEND DBMS_COMMON_LIBRARIES murmurhash)

if (MAKE_STATIC_LIBRARIES OR NOT SPLIT_SHARED_LIBRARIES)
    add_library (dbms STATIC ${dbms_headers} ${dbms_sources})
    target_link_libraries (dbms PRIVATE jemalloc libdivide ${DBMS_COMMON_LIBRARIES})
    set (all_modules dbms)
else()
    add_library (dbms SHARED ${dbms_headers} ${dbms_sources})
    target_link_libraries (dbms PUBLIC ${all_modules} ${DBMS_COMMON_LIBRARIES})
    target_link_libraries (clickhouse_interpreters PRIVATE jemalloc libdivide)
    list (APPEND all_modules dbms)
    # force all split libs to be linked
    if (OS_DARWIN)
        set (CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-undefined,error")
    else()
        set (CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--no-as-needed")
    endif()
endif ()

macro (dbms_target_include_directories)
    foreach (module ${all_modules})
        target_include_directories (${module} ${ARGN})
    endforeach ()
endmacro ()

macro (dbms_target_link_libraries)
    foreach (module ${all_modules})
        target_link_libraries (${module} ${ARGN})
    endforeach ()
endmacro ()

dbms_target_include_directories (PUBLIC "${ClickHouse_SOURCE_DIR}/src" "${ClickHouse_BINARY_DIR}/src")
target_include_directories (clickhouse_common_io PUBLIC "${ClickHouse_SOURCE_DIR}/src" "${ClickHouse_BINARY_DIR}/src")

# FIXME (UNIQUE KEY): Find a better place to locate this
target_include_directories(clickhouse_common_io PUBLIC "${SNAPPY_INCLUDE_DIR}") # uses some includes from core

target_include_directories(clickhouse_common_io PUBLIC "${LIBNUMA_INCLUDE_DIR}") # libnuma

if (USE_EMBEDDED_COMPILER)
    dbms_target_link_libraries (PRIVATE ${REQUIRED_LLVM_LIBRARIES})
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${LLVM_INCLUDE_DIRS})
endif ()

# Otherwise it will slow down stack traces printing too much.
set_source_files_properties(
        Common/Elf.cpp
        Common/Dwarf.cpp
        Common/SymbolIndex.cpp
        PROPERTIES COMPILE_FLAGS "-O3 ${WITHOUT_COVERAGE}")

target_link_libraries (clickhouse_common_io
        PRIVATE
            string_utils
            widechar_width
            ${LINK_LIBRARIES_ONLY_ON_X86_64}
        PUBLIC
            common
            dbms
            ${DOUBLE_CONVERSION_LIBRARIES}
            dragonbox_to_chars
)

# Use X86 AVX2/AVX512 instructions to accelerate filter operations
set_source_files_properties(
        Columns/ColumnFixedString.cpp
        Columns/ColumnsCommon.cpp
        Columns/ColumnVector.cpp
        Columns/ColumnDecimal.cpp
        Columns/ColumnString.cpp
        PROPERTIES COMPILE_FLAGS "${X86_INTRINSICS_FLAGS}")

if(RE2_LIBRARY)
    target_link_libraries(clickhouse_common_io PUBLIC ${RE2_LIBRARY})
endif()
if(RE2_ST_LIBRARY)
    target_link_libraries(clickhouse_common_io PUBLIC ${RE2_ST_LIBRARY})
endif()

target_link_libraries(clickhouse_common_io
        PRIVATE
            ${EXECINFO_LIBRARIES}
            cpuid
        PUBLIC
            boost::program_options
            boost::system
            ${CITYHASH_LIBRARIES}
            ${ZLIB_LIBRARIES}
            pcg_random
            Poco::Foundation
            Poco::ZIP
            roaring
            ${LIBNUMA_LIBRARY}
            ipdb
            maxminddb
        PRIVATE
            ${UDNS_LIBRARIES}

)

target_include_directories(clickhouse_common_io SYSTEM PUBLIC ${UDNS_INCLUDE_DIR})

# dbms_target_link_libraries(PUBLIC ch_contrib::abseil_swiss_tables)

if (USE_RDKAFKA)
    dbms_target_link_libraries(PRIVATE ${CPPKAFKA_LIBRARY} ${RDKAFKA_LIBRARY} )
    dbms_target_include_directories(SYSTEM BEFORE PRIVATE  ${RAPIDJSON_INCLUDE_DIR})
    if(NOT USE_INTERNAL_RDKAFKA_LIBRARY)
        dbms_target_include_directories(SYSTEM BEFORE PRIVATE ${RDKAFKA_INCLUDE_DIR})
    endif()
endif()

if (USE_CYRUS_SASL)
    dbms_target_link_libraries(PRIVATE ${CYRUS_SASL_LIBRARY})
endif()

if (USE_KRB5)
    dbms_target_include_directories(SYSTEM BEFORE PRIVATE ${KRB5_INCLUDE_DIR})
    dbms_target_link_libraries(PRIVATE ${KRB5_LIBRARY})
endif()

if (USE_NURAFT)
    dbms_target_link_libraries(PUBLIC ${NURAFT_LIBRARY})
endif()

if(RE2_INCLUDE_DIR)
    target_include_directories(clickhouse_common_io SYSTEM BEFORE PUBLIC ${RE2_INCLUDE_DIR})
endif()

dbms_target_link_libraries (
    PRIVATE
        boost::filesystem
        boost::program_options
        clickhouse_common_config
        clickhouse_common_zookeeper
        clickhouse_dictionaries_embedded
        clickhouse_parsers
        lz4
        Poco::JSON
        Poco::MongoDB
        Poco::ZIP
        string_utils
    PUBLIC
        ${MYSQLXX_LIBRARY}
        boost::system
        clickhouse_common_io
        clickhouse_protos_lib
        catalog_lib
        resource_manager_lib
        tso_lib
        daemon_manager_lib
)

target_include_directories(clickhouse_common_io PUBLIC "${CMAKE_CURRENT_BINARY_DIR}/Core/include") # uses some includes from core
dbms_target_include_directories(PUBLIC "${CMAKE_CURRENT_BINARY_DIR}/Core/include")

dbms_target_include_directories(SYSTEM BEFORE PUBLIC ${PDQSORT_INCLUDE_DIR})
dbms_target_include_directories(SYSTEM BEFORE PUBLIC ${MINISELECT_INCLUDE_DIR})

if (ZSTD_LIBRARY)
    dbms_target_link_libraries(PRIVATE ${ZSTD_LIBRARY})
    target_link_libraries (clickhouse_common_io PUBLIC ${ZSTD_LIBRARY})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${ZSTD_INCLUDE_DIR})
    if (NOT USE_INTERNAL_ZSTD_LIBRARY AND ZSTD_INCLUDE_DIR)
        dbms_target_include_directories(SYSTEM BEFORE PRIVATE ${ZSTD_INCLUDE_DIR})
    endif ()
endif()

if (XZ_LIBRARY)
    target_link_libraries (clickhouse_common_io PUBLIC ${XZ_LIBRARY})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${XZ_INCLUDE_DIR})
endif()

if (USE_ICU)
    dbms_target_link_libraries (PRIVATE ${ICU_LIBRARIES})
    dbms_target_include_directories (SYSTEM PRIVATE ${ICU_INCLUDE_DIRS})
endif ()

if (USE_CAPNP)
    dbms_target_link_libraries (PRIVATE ${CAPNP_LIBRARIES})
endif ()

if (USE_PARQUET)
    dbms_target_link_libraries(PRIVATE ${PARQUET_LIBRARY})
    if (NOT USE_INTERNAL_PARQUET_LIBRARY)
        dbms_target_include_directories (SYSTEM BEFORE PRIVATE ${PARQUET_INCLUDE_DIR} ${ARROW_INCLUDE_DIR})
        if (USE_STATIC_LIBRARIES)
            dbms_target_link_libraries(PRIVATE ${ARROW_LIBRARY})
        endif()
    endif ()
endif ()

if (USE_AVRO)
    dbms_target_link_libraries(PRIVATE ${AVROCPP_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PRIVATE ${AVROCPP_INCLUDE_DIR})
endif ()

if (OPENSSL_CRYPTO_LIBRARY)
    dbms_target_link_libraries (PRIVATE ${OPENSSL_CRYPTO_LIBRARY})
    target_link_libraries (clickhouse_common_io PRIVATE ${OPENSSL_CRYPTO_LIBRARY})
endif ()

if (USE_LDAP)
    dbms_target_include_directories (SYSTEM BEFORE PRIVATE ${OPENLDAP_INCLUDE_DIRS})
    dbms_target_link_libraries (PRIVATE ${OPENLDAP_LIBRARIES})
endif ()
dbms_target_include_directories (SYSTEM BEFORE PRIVATE ${SPARSEHASH_INCLUDE_DIR})

if (USE_PROTOBUF)
    dbms_target_link_libraries (PRIVATE ${Protobuf_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PRIVATE ${Protobuf_INCLUDE_DIR})
    target_link_libraries(clickhouse_common_io PUBLIC brpc-static ${DYNAMIC_LIB})
    target_link_libraries(clickhouse_common_io PUBLIC clickhouse_protos_lib)
    target_link_libraries(clickhouse_common_io PUBLIC ${Protobuf_LIBRARY})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${Protobuf_INCLUDE_DIR})
endif ()

if (USE_BYTEKV)
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${BYTEKV_INCLUDE_DIR})
endif()

if (USE_GRPC)
    dbms_target_link_libraries (PUBLIC clickhouse_grpc_protos)
endif()

if (USE_HDFS)
    dbms_target_link_libraries(PRIVATE ${HDFS3_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${HDFS3_INCLUDE_DIR})
endif()

if (USE_AWS_S3)
    target_link_libraries (clickhouse_common_io PUBLIC ${AWS_S3_LIBRARY})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${AWS_S3_CORE_INCLUDE_DIR})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${AWS_S3_INCLUDE_DIR})
endif()

if (USE_BROTLI)
    target_link_libraries (clickhouse_common_io PRIVATE ${BROTLI_LIBRARY})
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PRIVATE ${BROTLI_INCLUDE_DIR})
endif()

if (USE_AMQPCPP)
    dbms_target_link_libraries(PUBLIC ${AMQPCPP_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${AMQPCPP_INCLUDE_DIR})
endif()

if (USE_CASSANDRA)
    dbms_target_link_libraries(PUBLIC ${CASSANDRA_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${CASS_INCLUDE_DIR})
endif()

target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${DOUBLE_CONVERSION_INCLUDE_DIR})

if (USE_MSGPACK)
    target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${MSGPACK_INCLUDE_DIR})
endif()

target_link_libraries (clickhouse_common_io PUBLIC ${FAST_FLOAT_LIBRARY})
target_include_directories (clickhouse_common_io SYSTEM BEFORE PUBLIC ${FAST_FLOAT_INCLUDE_DIR})

if (USE_ORC)
    dbms_target_link_libraries(PUBLIC ${ORC_LIBRARIES})
    dbms_target_include_directories(SYSTEM BEFORE PUBLIC ${ORC_INCLUDE_DIR} "${CMAKE_BINARY_DIR}/contrib/orc/c++/include")
endif ()

if (USE_ROCKSDB)
    dbms_target_link_libraries(PUBLIC ${ROCKSDB_LIBRARY})
    dbms_target_include_directories(SYSTEM BEFORE PUBLIC ${ROCKSDB_INCLUDE_DIR})
endif()

if (USE_HIVE)
    dbms_target_link_libraries(PRIVATE ${HIVE_METASTORE_LIBRARY})
    dbms_target_include_directories (SYSTEM BEFORE PUBLIC ${HIVE_METASTORE_INCLUDE_DIR})
endif()

if (USE_LIBPQXX)
    dbms_target_link_libraries(PUBLIC ${LIBPQXX_LIBRARY})
    dbms_target_include_directories(SYSTEM BEFORE PUBLIC ${LIBPQXX_INCLUDE_DIR})
endif()

target_link_libraries(clickhouse_aggregate_functions PRIVATE datasketches_lib)
dbms_target_link_libraries(PUBLIC datasketches_lib)

dbms_target_link_libraries(PRIVATE _boost_context)

include ("${ClickHouse_SOURCE_DIR}/cmake/add_check.cmake")

if (ENABLE_TESTS AND USE_GTEST)
    macro (grep_gtest_sources BASE_DIR DST_VAR)
        # Cold match files that are not in tests/ directories
        file(GLOB_RECURSE "${DST_VAR}" RELATIVE "${BASE_DIR}" "gtest*.cpp")
    endmacro()

    macro (grep_gtest_headers BASE_DIR DST_VAR)
    # Cold match files that are not in tests/ directories
    file(GLOB_RECURSE "${DST_VAR}" RELATIVE "${BASE_DIR}" "gtest*.h")
    endmacro()

    # attach all dbms gtest sources
    grep_gtest_sources("${ClickHouse_SOURCE_DIR}/src" dbms_gtest_sources)
    grep_gtest_headers("${ClickHouse_SOURCE_DIR}/src" dbms_gtest_headers)
    add_executable(unit_tests_dbms ${dbms_gtest_sources})

    target_include_directories(unit_tests_dbms PRIVATE ${dbms_gtest_headers})

    # gtest framework has substandard code
    target_compile_options(unit_tests_dbms PRIVATE
        -Wno-zero-as-null-pointer-constant
        -Wno-covered-switch-default
        -Wno-undef
        -Wno-sign-compare
        -Wno-used-but-marked-unused
        -Wno-missing-noreturn
        -Wno-old-style-cast
        -Wno-suggest-override
        -Wno-suggest-destructor-override
        -Wno-gnu-zero-variadic-macro-arguments
        -Wno-unreachable-code
    )

    target_link_libraries(unit_tests_dbms PRIVATE
        ${GTEST_BOTH_LIBRARIES}
        clickhouse_functions
        clickhouse_aggregate_functions
        clickhouse_parsers
        clickhouse_storages_system
        dbms
        clickhouse_common_zookeeper
        string_utils
        brpc-static
        )

    # For __udivmodti4 referenced in Core/tests/gtest_DecimalFunctions.cpp
    if (OS_DARWIN AND COMPILER_GCC)
        target_link_libraries(unit_tests_dbms PRIVATE gcc)
    endif ()

    add_check(unit_tests_dbms)
endif ()
