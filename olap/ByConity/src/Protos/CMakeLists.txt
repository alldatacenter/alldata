protobuf_generate_cpp(dbms_proto_srcs dbms_proto_headers
    cnch_common.proto
    cnch_server_rpc.proto
    cnch_worker_rpc.proto
    data_models.proto
    data_part.proto
    plan_segment_manager.proto
    registry.proto
    resource_manager_rpc.proto
    tso.proto
    optimizer_statistics.proto
    runtime_filter.proto
    daemon_manager_rpc.proto)

# Disable clang-tidy for protobuf-generated *.pb.h and *.pb.cpp files.
set (CMAKE_CXX_CLANG_TIDY "")

add_library(clickhouse_protos_lib ${dbms_proto_srcs} ${dbms_proto_headers})

# Ignore warnings while compiling protobuf-generated *.pb.h and *.pb.cpp files.
target_compile_options(clickhouse_protos_lib PRIVATE "-w")

target_link_libraries (clickhouse_protos_lib PRIVATE ${Protobuf_LIBRARY})
target_include_directories(clickhouse_protos_lib SYSTEM PUBLIC "${CMAKE_CURRENT_BINARY_DIR}/..")
target_include_directories (clickhouse_protos_lib SYSTEM BEFORE PRIVATE ${Protobuf_INCLUDE_DIR})
