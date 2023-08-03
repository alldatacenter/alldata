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


#include <Interpreters/Context.h>
#include <Interpreters/InterpreterCreateWarehouseQuery.h>
#include <Parsers/ASTCreateWarehouseQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ResourceManagement/VirtualWarehouseType.h>



namespace DB
{

namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
    extern const int RESOURCE_MANAGER_ERROR;
    extern const int RESOURCE_MANAGER_INCOMPATIBLE_SETTINGS;
    extern const int RESOURCE_MANAGER_ILLEGAL_CONFIG;
    extern const int RESOURCE_MANAGER_UNKNOWN_SETTING;
    extern const int RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO;
}

InterpreterCreateWarehouseQuery::InterpreterCreateWarehouseQuery(const ASTPtr & query_ptr_, ContextPtr context_)
    : WithContext(context_), query_ptr(query_ptr_) {}


BlockIO InterpreterCreateWarehouseQuery::execute()
{
    auto & create = query_ptr->as<ASTCreateWarehouseQuery &>();
    auto & vw_name = create.name;

    bool num_workers_in_settings(false);

    VirtualWarehouseSettings vw_settings;

    if (create.settings)
    {
        for (const auto & change : create.settings->changes)
        {
            if (change.name == "type")
            {
                auto value = change.value.safeGet<std::string>();
                vw_settings.type = ResourceManagement::toVirtualWarehouseType(&value[0]);
            }
            else if (change.name == "auto_suspend")
            {
                vw_settings.auto_suspend = change.value.safeGet<size_t>();
            }
            else if (change.name == "auto_resume")
            {
                vw_settings.auto_resume = change.value.safeGet<size_t>();
            }
            else if (change.name == "num_workers")
            {
                num_workers_in_settings = true;
                vw_settings.num_workers = change.value.safeGet<size_t>();
            }

            else if (change.name == "max_worker_groups")
            {
                vw_settings.max_worker_groups = change.value.safeGet<size_t>();
            }
            else if (change.name == "min_worker_groups")
            {
                vw_settings.min_worker_groups = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_concurrent_queries")
            {
                vw_settings.max_concurrent_queries = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_queued_queries")
            {
                vw_settings.max_queued_queries = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_queued_waiting_ms")
            {
                vw_settings.max_queued_waiting_ms = change.value.safeGet<size_t>();
            }
            else if (change.name == "vw_schedule_algo")
            {
                auto value = change.value.safeGet<std::string>();
                auto algo = ResourceManagement::toVWScheduleAlgo(&value[0]);
                if (algo == ResourceManagement::VWScheduleAlgo::Unknown)
                    throw Exception("Wrong vw_schedule_algo: " + value, ErrorCodes::RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO);
                vw_settings.vw_schedule_algo = algo;
            }
            else if (change.name == "max_auto_borrow_links")
            {
                vw_settings.max_auto_borrow_links = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_auto_lend_links")
            {
                vw_settings.max_auto_lend_links = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_borrow")
            {
                vw_settings.cpu_threshold_for_borrow = change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_borrow")
            {
                vw_settings.mem_threshold_for_borrow = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_lend")
            {
                vw_settings.cpu_threshold_for_lend= change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_lend")
            {
                vw_settings.mem_threshold_for_lend = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_recall")
            {
                vw_settings.cpu_threshold_for_recall= change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_recall")
            {
                vw_settings.mem_threshold_for_recall = change.value.safeGet<size_t>();
            }
            else if (change.name == "cooldown_seconds_after_auto_link")
            {
                vw_settings.cooldown_seconds_after_auto_link = change.value.safeGet<size_t>();
            }
            else if (change.name == "cooldown_seconds_after_auto_unlink")
            {
                vw_settings.cooldown_seconds_after_auto_unlink = change.value.safeGet<size_t>();
            }
            else
            {
                throw Exception("Unknown setting " + change.name, ErrorCodes::RESOURCE_MANAGER_UNKNOWN_SETTING);
            }
        }
    }

    if (!num_workers_in_settings)
    {
        throw Exception("Expected num_workers setting to be filled", ErrorCodes::SYNTAX_ERROR);
    }

    if (vw_settings.type == ResourceManagement::VirtualWarehouseType::Unknown)
        throw Exception("Unknown Virtual Warehouse type, expected type to be one of 'Read', 'Write', 'Task' or 'Default filled under SETTINGS'", ErrorCodes::RESOURCE_MANAGER_UNKNOWN_SETTING);

    if (vw_settings.min_worker_groups > vw_settings.max_worker_groups)
        throw Exception("min_worker_groups should be less than or equal to max_worker_groups", ErrorCodes::RESOURCE_MANAGER_INCOMPATIBLE_SETTINGS);

    if (auto client = getContext()->getResourceManagerClient())
        client->createVirtualWarehouse(vw_name, vw_settings, create.if_not_exists);
    else
        throw Exception("Can't apply DDL of warehouse as RM is not enabled.", ErrorCodes::RESOURCE_MANAGER_ILLEGAL_CONFIG);

    return {};
}

}
