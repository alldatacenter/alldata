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

#include <Interpreters/CnchQueryMetrics/QueryWorkerMetricLog.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeFixedString.h>
#include <Interpreters/CnchQueryMetrics/QueryMetricLogHelper.h>

namespace DB
{

/// Remember to update the QueryWorkerMetricElements message in cnch_common.proto when changing the schema.
NamesAndTypesList QueryWorkerMetricElement::getNamesAndTypes()
{
    auto query_status_datatype = std::make_shared<DataTypeEnum8>(
        DataTypeEnum8::Values
        {
            {"QueryStart",                  static_cast<Int8>(QUERY_START)},
            {"QueryFinish",                 static_cast<Int8>(QUERY_FINISH)},
            {"ExceptionBeforeStart",        static_cast<Int8>(EXCEPTION_BEFORE_START)},
            {"ExceptionWhileProcessing",    static_cast<Int8>(EXCEPTION_WHILE_PROCESSING)}
        });

    return {
        {"initial_query_id", std::make_shared<DataTypeString>()},
        {"current_query_id", std::make_shared<DataTypeString>()},
        {"query", std::make_shared<DataTypeString>()},
        {"state", std::move(query_status_datatype)},
        {"type", std::make_shared<DataTypeString>()},
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"worker_id", std::make_shared<DataTypeString>()},
        {"event_time", std::make_shared<DataTypeDateTime>()},
        {"latency", std::make_shared<DataTypeUInt32>()},
        {"runtime_latency", std::make_shared<DataTypeUInt32>()},
        {"selected_parts", std::make_shared<DataTypeUInt32>()},
        {"selected_ranges", std::make_shared<DataTypeUInt32>()},
        {"selected_marks", std::make_shared<DataTypeUInt32>()},
        {"vfs_time", std::make_shared<DataTypeUInt32>()},
        {"peak_memory", std::make_shared<DataTypeUInt64>()},
        {"read_rows", std::make_shared<DataTypeUInt32>()},
        {"read_bytes", std::make_shared<DataTypeUInt64>()},
        {"read_cached_bytes", std::make_shared<DataTypeUInt64>()},
        {"write_rows", std::make_shared<DataTypeUInt32>()},
        {"write_bytes", std::make_shared<DataTypeUInt64>()},
        {"write_duration", std::make_shared<DataTypeUInt64>()},
        {"operator_level", std::make_shared<DataTypeString>()},
        {"exception", std::make_shared<DataTypeString>()},
        {"stack_trace", std::make_shared<DataTypeString>()},

        {"revision", std::make_shared<DataTypeUInt32>()},
        {"version_scm", std::make_shared<DataTypeString>()},
    };
}

void QueryWorkerMetricElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;

    columns[i++]->insert(initial_query_id);
    columns[i++]->insert(current_query_id);
    columns[i++]->insert(query);
    columns[i++]->insert(state);
    columns[i++]->insert(type.toString());
    columns[i++]->insert(database);
    columns[i++]->insert(table);
    columns[i++]->insert(worker_id);
    columns[i++]->insert(event_time);
    columns[i++]->insert(latency);
    columns[i++]->insert(runtime_latency);
    columns[i++]->insert(selected_parts);
    columns[i++]->insert(selected_ranges);
    columns[i++]->insert(selected_marks);
    columns[i++]->insert(vfs_time);
    columns[i++]->insert(peak_memory);
    columns[i++]->insert(read_rows);
    columns[i++]->insert(read_bytes);
    columns[i++]->insert(read_cached_bytes);
    columns[i++]->insert(write_rows);
    columns[i++]->insert(write_bytes);
    columns[i++]->insert(write_duration);
    columns[i++]->insert(operator_level);
    columns[i++]->insert(exception);
    columns[i++]->insert(stack_trace);
    columns[i++]->insert(revision);
    columns[i++]->insert(version_scm);
}

void QueryWorkerMetricElement::read(ReadBuffer & in)
{
    readStringBinary(initial_query_id, in);
    readStringBinary(current_query_id, in);
    readStringBinary(query, in);
    UInt8 received_state;
    readVarUInt(received_state, in);
    state = QueryMetricLogState(received_state);
    UInt8 received_type;
    readVarUInt(received_type, in);
    type = QueryMetricLogType(received_type);
    readStringBinary(database, in);
    readStringBinary(table, in);
    readStringBinary(worker_id, in);

    readVarUInt(event_time, in);
    readVarUInt(latency, in);
    readVarUInt(runtime_latency, in);
    readVarUInt(vfs_time, in);
    readVarUInt(peak_memory, in);

    readVarUInt(read_rows, in);
    readVarUInt(read_bytes, in);
    readVarUInt(read_cached_bytes, in);

    readVarUInt(write_rows, in);
    readVarUInt(write_bytes, in);
    readVarUInt(write_duration, in);

    readStringBinary(operator_level, in);
    readStringBinary(exception, in);
    readStringBinary(stack_trace, in);

    readVarUInt(revision, in);
    readStringBinary(version_scm, in);
}

void QueryWorkerMetricElement::write(WriteBuffer & out) const
{
    writeStringBinary(initial_query_id, out);
    writeStringBinary(current_query_id, out);
    writeStringBinary(query, out);
    writeVarUInt(state, out);
    writeVarUInt(type.type, out);
    writeStringBinary(database, out);
    writeStringBinary(table, out);
    writeStringBinary(worker_id, out);

    writeVarUInt(event_time, out);
    writeVarUInt(latency, out);
    writeVarUInt(runtime_latency, out);
    writeVarUInt(vfs_time, out);
    writeVarUInt(peak_memory, out);

    writeVarUInt(read_rows, out);
    writeVarUInt(read_bytes, out);
    writeVarUInt(read_cached_bytes, out);

    writeVarUInt(write_rows, out);
    writeVarUInt(write_bytes, out);
    writeVarUInt(write_duration, out);

    writeStringBinary(operator_level, out);
    writeStringBinary(exception, out);
    writeStringBinary(stack_trace, out);

    writeVarUInt(revision, out);
    writeStringBinary(version_scm, out);
}

}
