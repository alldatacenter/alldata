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

#include <Storages/System/StorageSystemVirtualWarehouses.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_ERROR;
}

NamesAndTypesList StorageSystemVirtualWarehouses::getNamesAndTypes()
{
    return {
        {"name", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeString>()},
        {"type", std::make_shared<DataTypeString>()},
        {"active_worker_groups", std::make_shared<DataTypeUInt32>()},
        {"active_borrowed_worker_groups", std::make_shared<DataTypeUInt32>()},
        {"active_lent_worker_groups", std::make_shared<DataTypeUInt32>()},
        {"last_borrow_timestamp", std::make_shared<DataTypeDateTime>()},
        {"last_lend_timestamp", std::make_shared<DataTypeDateTime>()},
        {"auto_suspend", std::make_shared<DataTypeUInt32>()},
        {"auto_resume", std::make_shared<DataTypeUInt32>()},
        {"num_workers", std::make_shared<DataTypeUInt32>()},
        {"min_worker_groups", std::make_shared<DataTypeUInt32>()},
        {"max_worker_groups", std::make_shared<DataTypeUInt32>()},
        {"max_concurrent_queries", std::make_shared<DataTypeUInt32>()},
        {"max_queued_queries", std::make_shared<DataTypeUInt32>()},
        {"max_queued_waiting_ms", std::make_shared<DataTypeUInt32>()},
        {"vw_schedule_algo", std::make_shared<DataTypeString>()},
        {"max_auto_borrow_links", std::make_shared<DataTypeUInt32>()},
        {"max_auto_lend_links", std::make_shared<DataTypeUInt32>()},
        {"cpu_threshold_for_borrow", std::make_shared<DataTypeUInt32>()},
        {"mem_threshold_for_borrow", std::make_shared<DataTypeUInt32>()},
        {"cpu_threshold_for_lend", std::make_shared<DataTypeUInt32>()},
        {"mem_threshold_for_lend", std::make_shared<DataTypeUInt32>()},
        {"cpu_threshold_for_recall", std::make_shared<DataTypeUInt32>()},
        {"mem_threshold_for_recall", std::make_shared<DataTypeUInt32>()},
        {"cooldown_seconds_after_auto_link", std::make_shared<DataTypeUInt32>()},
        {"cooldown_seconds_after_auto_unlink", std::make_shared<DataTypeUInt32>()},
    };
}

void StorageSystemVirtualWarehouses::fillData(MutableColumns & res_columns, const ContextPtr context, const SelectQueryInfo &) const
{
    std::vector<VirtualWarehouseData> vws_data;

    try
    {
        auto client = context->getResourceManagerClient();
        if (client)
            client->getAllVirtualWarehouses(vws_data);
        else
            throw Exception("Resource Manager unavailable", ErrorCodes::RESOURCE_MANAGER_ERROR);
    }
    catch (const Exception & e)
    {
        throw Exception(
            "Failed to get Virtual Warehouse data from Resource Manager: " + e.displayText(), ErrorCodes::RESOURCE_MANAGER_ERROR);
    }

    for (const auto & vw_data : vws_data)
    {
        size_t i = 0;
        auto & vw_settings = vw_data.settings;

        res_columns[i++]->insert(vw_data.name);
        res_columns[i++]->insert(UUIDHelpers::UUIDToString(vw_data.uuid));
        res_columns[i++]->insert(ResourceManagement::toString(vw_settings.type));
        res_columns[i++]->insert(vw_data.num_worker_groups);
        res_columns[i++]->insert(vw_data.num_borrowed_worker_groups);
        res_columns[i++]->insert(vw_data.num_lent_worker_groups);
        res_columns[i++]->insert(vw_data.last_borrow_timestamp);
        res_columns[i++]->insert(vw_data.last_lend_timestamp);
        res_columns[i++]->insert(vw_settings.auto_suspend);
        res_columns[i++]->insert(vw_settings.auto_resume);
        res_columns[i++]->insert(vw_settings.num_workers);
        res_columns[i++]->insert(vw_settings.min_worker_groups);
        res_columns[i++]->insert(vw_settings.max_worker_groups);
        res_columns[i++]->insert(vw_settings.max_concurrent_queries);
        res_columns[i++]->insert(vw_settings.max_queued_queries);
        res_columns[i++]->insert(vw_settings.max_queued_waiting_ms);
        res_columns[i++]->insert(ResourceManagement::toString(vw_settings.vw_schedule_algo));
        res_columns[i++]->insert(vw_settings.max_auto_borrow_links);
        res_columns[i++]->insert(vw_settings.max_auto_lend_links);
        res_columns[i++]->insert(vw_settings.cpu_threshold_for_borrow);
        res_columns[i++]->insert(vw_settings.mem_threshold_for_borrow);
        res_columns[i++]->insert(vw_settings.cpu_threshold_for_lend);
        res_columns[i++]->insert(vw_settings.mem_threshold_for_lend);
        res_columns[i++]->insert(vw_settings.cpu_threshold_for_recall);
        res_columns[i++]->insert(vw_settings.mem_threshold_for_recall);
        res_columns[i++]->insert(vw_settings.cooldown_seconds_after_auto_link);
        res_columns[i++]->insert(vw_settings.cooldown_seconds_after_auto_unlink);
    }
}

}
