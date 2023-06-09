/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once
#include <Interpreters/CnchQueryMetrics/QueryMetricLogHelper.h>
#include <Interpreters/CnchSystemLog.h>

namespace DB
{

struct QueryMetricElement
{
    String query_id;
    String query;
    QueryMetricLogState state;
    QueryMetricLogType type;
    String database;
    String table;
    UInt8 complex_query;
    String server_id;
    time_t event_time{};
    UInt32 latency{};
    UInt32 runtime_latency{};  /// ms, the wall clock time duration of the total query pipeline execution
    UInt32 init_time{};
    UInt32 catalog_time{};
    UInt32 total_partitions{};
    UInt32 pruned_partitions{};
    UInt32 selected_parts{};
    UInt64 peak_memory{};
    UInt32 read_rows{};
    UInt64 read_bytes{};
    UInt64 read_cached_bytes{};
    UInt32 write_rows{};
    UInt64 write_bytes{};
    UInt64 write_duration{};
    UInt32 result_rows{};
    String operator_level;
    String virtual_warehouse;
    String worker_group;
    String exception;
    String stack_trace;
    ClientInfo client_info;

    QueryMetricElement(
        String query_id_ = {},
        String query_ = {},
        QueryMetricLogState state_ = QueryMetricLogState::QUERY_START,
        QueryMetricLogType type_ = QueryMetricLogType(),
        String database_ = {},
        String table_ = {},
        UInt8 complex_query_ = 0,
        String server_id_ = {},
        time_t event_time_ = {},
        UInt32 latency_ = 0,
        UInt32 runtime_latency_ = 0,
        UInt32 init_time_ = 0,
        UInt32 catalog_time_ = 0,
        UInt32 total_partitions_ = 0,
        UInt32 pruned_partitions_ = 0,
        UInt32 selected_parts_ = 0,
        UInt64 peak_memory_ = 0,
        UInt32 read_rows_ = 0,
        UInt64 read_bytes_ = 0,
        UInt64 read_cached_bytes_ = 0,
        UInt32 write_rows_ = 0,
        UInt64 write_bytes_ = 0,
        UInt64 write_duration_ = 0,
        UInt32 result_rows_ = 0,
        String operator_level_ = {},
        String virtual_warehouse_ = {},
        String worker_group_ = {},
        String exception_ = {},
        String stack_trace_ = {},
        ClientInfo client_info_ = {})
        : query_id(query_id_)
        , query(query_)
        , state(state_)
        , type(type_)
        , database(database_)
        , table(table_)
        , complex_query(complex_query_)
        , server_id(server_id_)
        , event_time(event_time_)
        , latency(latency_)
        , runtime_latency(runtime_latency_)
        , init_time(init_time_)
        , catalog_time(catalog_time_)
        , total_partitions(total_partitions_)
        , pruned_partitions(pruned_partitions_)
        , selected_parts(selected_parts_)
        , peak_memory(peak_memory_)
        , read_rows(read_rows_)
        , read_bytes(read_bytes_)
        , read_cached_bytes(read_cached_bytes_)
        , write_rows(write_rows_)
        , write_bytes(write_bytes_)
        , write_duration(write_duration_)
        , result_rows(result_rows_)
        , operator_level(operator_level_)
        , virtual_warehouse(virtual_warehouse_)
        , worker_group(worker_group_)
        , exception(exception_)
        , stack_trace(stack_trace_)
        , client_info(client_info_)
    {}
    static std::string name() {return "QueryMetric"; }
    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }
    void appendToBlock(MutableColumns & columns) const;

    static void appendClientInfo(const ClientInfo & client_info, MutableColumns & columns, size_t & i);
};

class QueryMetricLog : public CnchSystemLog<QueryMetricElement>
{
public:
    using CnchSystemLog<QueryMetricElement>::CnchSystemLog;
};

}
