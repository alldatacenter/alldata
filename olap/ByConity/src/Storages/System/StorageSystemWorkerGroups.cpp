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

#include <Storages/System/StorageSystemWorkerGroups.h>

#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ResourceManagement/CommonData.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_ERROR;
}

NamesAndTypesList StorageSystemWorkerGroups::getNamesAndTypes()
{
    return {
        {"id", std::make_shared<DataTypeString>()},
        {"type", std::make_shared<DataTypeString>()},
        {"vw_uuid", std::make_shared<DataTypeUUID>()},
        {"vw_name", std::make_shared<DataTypeString>()},
        {"linked_id", std::make_shared<DataTypeString>()},
        {"active_workers", std::make_shared<DataTypeUInt32>()},
        {"min_cpu_usage", std::make_shared<DataTypeUInt32>()},
        {"max_cpu_usage", std::make_shared<DataTypeUInt32>()},
        {"avg_cpu_usage", std::make_shared<DataTypeUInt32>()},
        {"min_mem_usage", std::make_shared<DataTypeUInt32>()},
        {"max_mem_usage", std::make_shared<DataTypeUInt32>()},
        {"avg_mem_usage", std::make_shared<DataTypeUInt32>()},
        {"is_auto_linked", std::make_shared<DataTypeUInt32>()},
    };
}


void StorageSystemWorkerGroups::fillData(MutableColumns & res_columns, const ContextPtr context, const SelectQueryInfo &) const
{
    std::vector<WorkerGroupData> worker_group_data_list;
    try
    {
        auto client = context->getResourceManagerClient();
        if (client)
            worker_group_data_list = client->getAllWorkerGroups(true);
        else
            throw Exception("Resource Manager unavailable", ErrorCodes::RESOURCE_MANAGER_ERROR);
    }
    catch (const Exception & e)
    {
        throw Exception("Failed to get Worker Group data from Resource Manager: " + e.displayText(), ErrorCodes::RESOURCE_MANAGER_ERROR);
    }

    for (auto & data : worker_group_data_list)
    {
        size_t i = 0;
        res_columns[i++]->insert(data.id);
        res_columns[i++]->insert(RM::toString(data.type));
        res_columns[i++]->insert(data.vw_uuid);
        res_columns[i++]->insert(data.vw_name);
        res_columns[i++]->insert(data.linked_id);
        res_columns[i++]->insert(data.num_workers);
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.min_cpu_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.max_cpu_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.avg_cpu_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.min_mem_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.max_mem_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.metrics.avg_mem_usage));
        res_columns[i++]->insert(static_cast<uint32_t>(data.is_auto_linked));
    }
}

}
