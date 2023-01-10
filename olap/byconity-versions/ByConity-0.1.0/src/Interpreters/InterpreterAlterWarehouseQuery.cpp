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
#include <Interpreters/Context_fwd.h>
#include <Interpreters/InterpreterAlterWarehouseQuery.h>
#include <Parsers/ASTAlterWarehouseQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ResourceManagement/VirtualWarehouseType.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_MANAGER_ERROR;
    extern const int LOGICAL_ERROR;
    extern const int RESOURCE_MANAGER_UNKNOWN_SETTING;
    extern const int RESOURCE_MANAGER_INCOMPATIBLE_SETTINGS;
    extern const int RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO;

}

InterpreterAlterWarehouseQuery::InterpreterAlterWarehouseQuery(const ASTPtr & query_ptr_, ContextPtr context_)
    : WithContext(context_), query_ptr(query_ptr_) {}


BlockIO InterpreterAlterWarehouseQuery::execute()
{
    auto & alter = query_ptr->as<ASTAlterWarehouseQuery &>();
    auto & vw_name = alter.name;

    if (!alter.rename_to.empty())
        throw Exception("Renaming VirtualWarehouse is currently unsupported.", ErrorCodes::LOGICAL_ERROR);

    ResourceManagement::VirtualWarehouseAlterSettings vw_alter_settings;

    if (alter.settings)
    {
        for (const auto & change : alter.settings->changes)
        {
            if (change.name == "type")
            {
                using RMType = ResourceManagement::VirtualWarehouseType;
                auto value = change.value.safeGet<std::string>();
                auto type = RMType(ResourceManagement::toVirtualWarehouseType(&value[0]));
                if (type == RMType::Unknown)
                    throw Exception("Unknown Virtual Warehouse type: " + value, ErrorCodes::RESOURCE_MANAGER_UNKNOWN_SETTING);
                vw_alter_settings.type = type;
            }
            else if (change.name == "auto_suspend")
            {
                vw_alter_settings.auto_suspend = change.value.safeGet<size_t>();
            }
            else if (change.name == "auto_resume")
            {
                vw_alter_settings.auto_resume = change.value.safeGet<size_t>();
            }
            else if (change.name == "num_workers")
            {
                vw_alter_settings.num_workers = change.value.safeGet<size_t>();
            }
            else if (change.name == "min_worker_groups")
            {
                vw_alter_settings.min_worker_groups = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_worker_groups")
            {
                vw_alter_settings.max_worker_groups = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_concurrent_queries")
            {
                vw_alter_settings.max_concurrent_queries = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_queued_queries")
            {
                vw_alter_settings.max_queued_queries = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_queued_waiting_ms")
            {
                vw_alter_settings.max_queued_waiting_ms = change.value.safeGet<size_t>();
            }
            else if (change.name == "vw_schedule_algo")
            {
                auto value = change.value.safeGet<std::string>();
                auto algo = ResourceManagement::toVWScheduleAlgo(&value[0]);
                if (algo == ResourceManagement::VWScheduleAlgo::Unknown)
                    throw Exception("Wrong vw_schedule_algo: " + value, ErrorCodes::RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO);
                vw_alter_settings.vw_schedule_algo = algo;
            }
            else if (change.name == "max_auto_borrow_links")
            {
                vw_alter_settings.max_auto_borrow_links = change.value.safeGet<size_t>();
            }
            else if (change.name == "max_auto_lend_links")
            {
                vw_alter_settings.max_auto_lend_links = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_borrow")
            {
                vw_alter_settings.cpu_threshold_for_borrow = change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_borrow")
            {
                vw_alter_settings.mem_threshold_for_borrow = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_lend")
            {
                vw_alter_settings.cpu_threshold_for_lend = change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_lend")
            {
                vw_alter_settings.mem_threshold_for_lend = change.value.safeGet<size_t>();
            }
            else if (change.name == "cpu_threshold_for_recall")
            {
                vw_alter_settings.cpu_threshold_for_recall = change.value.safeGet<size_t>();
            }
            else if (change.name == "mem_threshold_for_recall")
            {
                vw_alter_settings.mem_threshold_for_recall = change.value.safeGet<size_t>();
            }
            else if (change.name == "cooldown_seconds_after_auto_link")
            {
                vw_alter_settings.cooldown_seconds_after_auto_link = change.value.safeGet<size_t>();
            }
            else if (change.name == "cooldown_seconds_after_auto_unlink")
            {
                vw_alter_settings.cooldown_seconds_after_auto_unlink = change.value.safeGet<size_t>();
            }
            else
            {
                throw Exception("Unknown setting " + change.name, ErrorCodes::RESOURCE_MANAGER_UNKNOWN_SETTING);
            }
        }
    }

    if (vw_alter_settings.min_worker_groups && vw_alter_settings.max_worker_groups
        && vw_alter_settings.min_worker_groups > vw_alter_settings.max_worker_groups)
        throw Exception("min_worker_groups should be less than or equal to max_worker_groups", ErrorCodes::RESOURCE_MANAGER_INCOMPATIBLE_SETTINGS);

    auto client = getContext()->getResourceManagerClient();
    client->updateVirtualWarehouse(vw_name, vw_alter_settings);

    return {};
}

}
