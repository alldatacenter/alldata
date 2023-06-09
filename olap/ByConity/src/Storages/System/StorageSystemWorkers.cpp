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

#include <Storages/System/StorageSystemWorkers.h>

#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeDateTime.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ResourceManagement/WorkerNode.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_ERROR;
}

NamesAndTypesList StorageSystemWorkers::getNamesAndTypes()
{
    return {
        {"worker_id", std::make_shared<DataTypeString>()},
        {"host", std::make_shared<DataTypeString>()},
        {"tcp_port", std::make_shared<DataTypeUInt16>()},
        {"rpc_port", std::make_shared<DataTypeUInt16>()},
        {"http_port", std::make_shared<DataTypeUInt16>()},
        {"exchange_port", std::make_shared<DataTypeUInt16>()},
        {"exchange_status_port", std::make_shared<DataTypeUInt16>()},
        {"vw_name", std::make_shared<DataTypeString>()},
        {"worker_group_id", std::make_shared<DataTypeString>()},
        {"query_num", std::make_shared<DataTypeUInt32>()},
        {"cpu_usage", std::make_shared<DataTypeFloat64>()},
        {"reserved_cpu_cores", std::make_shared<DataTypeUInt32>()},
        {"memory_usage", std::make_shared<DataTypeFloat64>()},
        {"disk_space", std::make_shared<DataTypeUInt64>()},
        {"memory_available", std::make_shared<DataTypeUInt64>()},
        {"reserved_memory_bytes", std::make_shared<DataTypeUInt64>()},
        {"register_time", std::make_shared<DataTypeDateTime>()},
        {"last_update_time", std::make_shared<DataTypeDateTime>()},
        {"state", std::make_shared<DataTypeUInt8>()},
    };
}

void StorageSystemWorkers::fillData(MutableColumns & res_columns, const ContextPtr context, const SelectQueryInfo & /*query_info*/) const
{
    // TODO: support filter by vw_name.
    std::vector<WorkerNodeResourceData> res;

    try
    {
        auto client = context->getResourceManagerClient();
        if (client)
            client->getAllWorkers(res);
        else
            throw Exception("Resource Manager unavailable", ErrorCodes::RESOURCE_MANAGER_ERROR);
    }
    catch (const Exception & e)
    {
        throw Exception("Unable to get worker data through Resource Manager at this moment due to: " + e.displayText(), ErrorCodes::RESOURCE_MANAGER_ERROR);
    }

    for (auto & node: res)
    {
        size_t i = 0;
        res_columns[i++]->insert(node.id);
        res_columns[i++]->insert(node.host_ports.getHost());
        res_columns[i++]->insert(node.host_ports.tcp_port);
        res_columns[i++]->insert(node.host_ports.rpc_port);
        res_columns[i++]->insert(node.host_ports.http_port);
        res_columns[i++]->insert(node.host_ports.exchange_port);
        res_columns[i++]->insert(node.host_ports.exchange_status_port);
        res_columns[i++]->insert(node.vw_name);
        res_columns[i++]->insert(node.worker_group_id);
        res_columns[i++]->insert(node.query_num);
        res_columns[i++]->insert(node.cpu_usage);
        res_columns[i++]->insert(node.reserved_cpu_cores);
        res_columns[i++]->insert(node.memory_usage);
        res_columns[i++]->insert(node.disk_space);
        res_columns[i++]->insert(node.memory_available);
        res_columns[i++]->insert(node.reserved_memory_bytes);
        res_columns[i++]->insert(node.register_time);
        res_columns[i++]->insert(node.last_update_time);
        res_columns[i++]->insert(static_cast<UInt8>(node.state));
    }
}

}
