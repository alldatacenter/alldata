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

#include <Interpreters/CnchQueryMetrics/QueryMetricLogHelper.h>
#include <Interpreters/CnchSystemLog.h>
#include <Common/config_version.h>
#include <Common/ClickHouseRevision.h>
#include <Protos/cnch_common.pb.h>

namespace DB
{

struct QueryWorkerMetricElement
{
    String initial_query_id;
    String current_query_id;
    String query;
    QueryMetricLogState state;
    QueryMetricLogType type;
    String database;
    String table;
    String worker_id;
    time_t event_time{};
    UInt32 latency{};
    UInt32 runtime_latency{};
    UInt32 selected_parts{};
    UInt32 selected_ranges{};
    UInt32 selected_marks{};
    UInt32 vfs_time{};
    UInt64 peak_memory{};
    UInt32 read_rows{};
    UInt64 read_bytes{};
    UInt64 read_cached_bytes{};
    UInt32 write_rows{};
    UInt64 write_bytes{};
    UInt64 write_duration{};
    String operator_level;
    String exception;
    String stack_trace;

    UInt32 revision;
    String version_scm;

    QueryWorkerMetricElement(
        String initial_query_id_ = {},
        String current_query_id_ = {},
        String query_ = {},
        QueryMetricLogState state_ = QueryMetricLogState::QUERY_START,
        QueryMetricLogType type_ = QueryMetricLogType(),
        String database_ = {},
        String table_ = {},
        String worker_id_ = {},
        time_t event_time_ = {},
        UInt32 latency_ = 0,
        UInt32 runtime_latency_ = 0,
        UInt32 selected_parts_ = 0,
        UInt32 selected_ranges_ = 0,
        UInt32 selected_marks_ = 0,
        UInt32 vfs_time_ = 0,
        UInt64 peak_memory_ = 0,
        UInt32 read_rows_ = 0,
        UInt64 read_bytes_ = 0,
        UInt64 read_cached_bytes_ = 0,
        UInt32 write_rows_ = 0,
        UInt64 write_bytes_ = 0,
        UInt64 write_duration_ = 0,
        String operator_level_ = {},
        String exception_ = {},
        String stack_trace_ = {},
        UInt32 revision_ = ClickHouseRevision::getVersionRevision(),
        String version_scm_ = VERSION_SCM)
        : initial_query_id(initial_query_id_)
        , current_query_id(current_query_id_)
        , query(query_)
        , state(state_)
        , type(type_)
        , database(database_)
        , table(table_)
        , worker_id(worker_id_)
        , event_time(event_time_)
        , latency(latency_)
        , runtime_latency(runtime_latency_)
        , selected_parts(selected_parts_)
        , selected_ranges(selected_ranges_)
        , selected_marks(selected_marks_)
        , vfs_time(vfs_time_)
        , peak_memory(peak_memory_)
        , read_rows(read_rows_)
        , read_bytes(read_bytes_)
        , read_cached_bytes(read_cached_bytes_)
        , write_rows(write_rows_)
        , write_bytes(write_bytes_)
        , write_duration(write_duration_)
        , operator_level(operator_level_)
        , exception(exception_)
        , stack_trace(stack_trace_)
        , revision(revision_)
        , version_scm(version_scm_)
    {}
    static std::string name() {return "QueryWorkerMetric"; }
    static NamesAndTypesList getNamesAndTypes();
    static NamesAndAliases getNamesAndAliases() { return {}; }
    void appendToBlock(MutableColumns & columns) const;

    void read(ReadBuffer & in);
    void write(WriteBuffer & out) const;
};

using QueryWorkerMetricElementPtr = std::shared_ptr<QueryWorkerMetricElement>;

inline void fillQueryWorkerMetricElement(const QueryWorkerMetricElementPtr & element, Protos::QueryWorkerMetricElement & pb_element)
{
    pb_element.set_initial_query_id(element->initial_query_id);
    pb_element.set_current_query_id(element->current_query_id);
    pb_element.set_query(element->query);
    pb_element.set_state(element->state);
    pb_element.set_type(element->type.type);
    pb_element.set_database(element->database);
    pb_element.set_table(element->table);
    pb_element.set_worker_id(element->worker_id);
    pb_element.set_event_time(element->event_time);
    pb_element.set_latency(element->latency);
    pb_element.set_runtime_latency(element->runtime_latency);
    pb_element.set_selected_parts(element->selected_parts);
    pb_element.set_selected_ranges(element->selected_ranges);
    pb_element.set_selected_marks(element->selected_marks);
    pb_element.set_vfs_time(element->vfs_time);
    pb_element.set_peak_memory(element->peak_memory);
    pb_element.set_read_rows(element->read_rows);
    pb_element.set_read_bytes(element->read_bytes);
    pb_element.set_read_cached_bytes(element->read_cached_bytes);
    pb_element.set_write_rows(element->write_rows);
    pb_element.set_write_bytes(element->write_bytes);
    pb_element.set_write_duration(element->write_duration);
    pb_element.set_operator_level(element->operator_level);
    pb_element.set_exception(element->exception);
    pb_element.set_stack_trace(element->stack_trace);
    pb_element.set_revision(element->revision);
    pb_element.set_version_scm(element->version_scm);
}

inline QueryWorkerMetricElement createQueryWorkerMetricElement(const Protos::QueryWorkerMetricElement & pb_element)
{
    return QueryWorkerMetricElement(
        pb_element.initial_query_id(),
        pb_element.current_query_id(),
        pb_element.query(),
        QueryMetricLogState(pb_element.state()),
        QueryMetricLogType(pb_element.type()),
        pb_element.database(),
        pb_element.table(),
        pb_element.worker_id(),
        pb_element.event_time(),
        pb_element.latency(),
        pb_element.runtime_latency(),
        pb_element.selected_parts(),
        pb_element.selected_ranges(),
        pb_element.selected_marks(),
        pb_element.vfs_time(),
        pb_element.peak_memory(),
        pb_element.read_rows(),
        pb_element.read_bytes(),
        pb_element.read_cached_bytes(),
        pb_element.write_rows(),
        pb_element.write_bytes(),
        pb_element.write_duration(),
        pb_element.operator_level(),
        pb_element.exception(),
        pb_element.stack_trace(),
        pb_element.revision(),
        pb_element.version_scm()
    );
}

class QueryWorkerMetricLog : public CnchSystemLog<QueryWorkerMetricElement>
{
    using CnchSystemLog<QueryWorkerMetricElement>::CnchSystemLog;
};

}
