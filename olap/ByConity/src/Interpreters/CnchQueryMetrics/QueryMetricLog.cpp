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

#include <Interpreters/CnchQueryMetrics/QueryMetricLog.h>
#include <Common/config_version.h>
#include <Common/ClickHouseRevision.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeFixedString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeEnum.h>
#include <array>

namespace DB
{

NamesAndTypesList QueryMetricElement::getNamesAndTypes()
{
    auto query_status_datatype = std::make_shared<DataTypeEnum8>(
        DataTypeEnum8::Values
        {
            {"QueryStart",                  static_cast<Int8>(QUERY_START)},
            {"QueryFinish",                 static_cast<Int8>(QUERY_FINISH)},
            {"ExceptionBeforeStart",        static_cast<Int8>(EXCEPTION_BEFORE_START)},
            {"ExceptionWhileProcessing",    static_cast<Int8>(EXCEPTION_WHILE_PROCESSING)}
        });

    return
    {
        {"query_id", std::make_shared<DataTypeString>()},
        {"query", std::make_shared<DataTypeString>()},
        {"state", std::move(query_status_datatype)},
        {"type", std::make_shared<DataTypeString>()},
        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"complex_query", std::make_shared<DataTypeUInt8>()},
        {"server_id", std::make_shared<DataTypeString>()},
        {"event_time", std::make_shared<DataTypeDateTime>()},
        {"latency", std::make_shared<DataTypeUInt32>()},
        {"runtime_latency", std::make_shared<DataTypeUInt32>()},
        {"init_time", std::make_shared<DataTypeUInt32>()},
        {"catalog_time", std::make_shared<DataTypeUInt32>()},
        {"total_partitions", std::make_shared<DataTypeUInt32>()},
        {"pruned_partitions", std::make_shared<DataTypeUInt32>()},
        {"selected_parts", std::make_shared<DataTypeUInt32>()},
        {"peak_memory", std::make_shared<DataTypeUInt64>()},
        {"read_rows", std::make_shared<DataTypeUInt32>()},
        {"read_bytes", std::make_shared<DataTypeUInt64>()},
        {"read_cached_bytes", std::make_shared<DataTypeUInt64>()},
        {"write_rows", std::make_shared<DataTypeUInt32>()},
        {"write_bytes", std::make_shared<DataTypeUInt64>()},
        {"write_duration", std::make_shared<DataTypeUInt64>()},
        {"result_rows", std::make_shared<DataTypeUInt32>()},
        {"operator_level", std::make_shared<DataTypeString>()},
        {"virtual_warehouse", std::make_shared<DataTypeString>()},
        {"worker_group", std::make_shared<DataTypeString>()},
        {"exception", std::make_shared<DataTypeString>()},
        {"stack_trace", std::make_shared<DataTypeString>()},

        {"user", std::make_shared<DataTypeString>()},
        {"address", std::make_shared<DataTypeString>()},
        {"port", std::make_shared<DataTypeUInt16>()},
        {"initial_user", std::make_shared<DataTypeString>()},
        {"initial_address", std::make_shared<DataTypeString>()},
        {"initial_port", std::make_shared<DataTypeUInt16>()},
        {"interface", std::make_shared<DataTypeUInt8>()},
        {"os_user", std::make_shared<DataTypeString>()},
        {"client_hostname", std::make_shared<DataTypeString>()},
        {"client_name", std::make_shared<DataTypeString>()},
        {"client_revision", std::make_shared<DataTypeUInt32>()},
        {"client_version_major", std::make_shared<DataTypeUInt32>()},
        {"client_version_minor", std::make_shared<DataTypeUInt32>()},
        {"client_version_patch", std::make_shared<DataTypeUInt32>()},
        {"http_method", std::make_shared<DataTypeUInt8>()},
        {"http_user_agent", std::make_shared<DataTypeString>()},
        {"quota_key", std::make_shared<DataTypeString>()},

        {"revision", std::make_shared<DataTypeUInt32>()},
        {"version_scm", std::make_shared<DataTypeString>()},
    };
}

void QueryMetricElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;

    columns[i++]->insert(query_id);
    columns[i++]->insert(query);
    columns[i++]->insert(state);
    columns[i++]->insert(type.toString());
    columns[i++]->insert(database);
    columns[i++]->insert(table);
    columns[i++]->insert(complex_query);
    columns[i++]->insert(server_id);
    columns[i++]->insert(event_time);
    columns[i++]->insert(latency);
    columns[i++]->insert(runtime_latency);
    columns[i++]->insert(init_time);
    columns[i++]->insert(catalog_time);
    columns[i++]->insert(total_partitions);
    columns[i++]->insert(pruned_partitions);
    columns[i++]->insert(selected_parts);
    columns[i++]->insert(peak_memory);
    columns[i++]->insert(read_rows);
    columns[i++]->insert(read_bytes);
    columns[i++]->insert(read_cached_bytes);
    columns[i++]->insert(write_rows);
    columns[i++]->insert(write_bytes);
    columns[i++]->insert(write_duration);
    columns[i++]->insert(result_rows);
    columns[i++]->insert(operator_level);
    columns[i++]->insert(virtual_warehouse);
    columns[i++]->insert(worker_group);
    columns[i++]->insert(exception);
    columns[i++]->insert(stack_trace);
    appendClientInfo(client_info, columns, i);
    columns[i++]->insert(ClickHouseRevision::getVersionRevision());
    columns[i++]->insert(VERSION_SCM);
}

void QueryMetricElement::appendClientInfo(const ClientInfo & client_info, MutableColumns & columns, size_t & i)
{
    columns[i++]->insert(client_info.current_user);
    columns[i++]->insert(client_info.current_address.host().toString());
    columns[i++]->insert(client_info.current_address.port());

    columns[i++]->insert(client_info.initial_user);
    columns[i++]->insert(client_info.initial_address.host().toString());
    columns[i++]->insert(client_info.initial_address.port());

    columns[i++]->insert(UInt64(client_info.interface));

    columns[i++]->insert(client_info.os_user);
    columns[i++]->insert(client_info.client_hostname);
    columns[i++]->insert(client_info.client_name);
    columns[i++]->insert(client_info.client_tcp_protocol_version);
    columns[i++]->insert(client_info.client_version_major);
    columns[i++]->insert(client_info.client_version_minor);
    columns[i++]->insert(client_info.client_version_patch);

    columns[i++]->insert(UInt64(client_info.http_method));
    columns[i++]->insert(client_info.http_user_agent);

    columns[i++]->insert(client_info.quota_key);
}

}
