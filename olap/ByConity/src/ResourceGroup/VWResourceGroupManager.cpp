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

#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Catalog/Catalog.h>
#include <Interpreters/Context.h>
#include <Interpreters/VirtualWarehouseHandle.h>
// #include <MergeTreeCommon/CnchWorkerClientPools.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTCreateQuery.h>
#include <Parsers/ASTDropQuery.h>
#include <Parsers/ASTRenameQuery.h>
// #include <Parsers/ASTDeleteQuery.h>
#include <Parsers/ASTInsertQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <ResourceGroup/VWResourceGroup.h>
#include <ResourceGroup/VWResourceGroupManager.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_GROUP_ILLEGAL_CONFIG;
    extern const int RESOURCE_GROUP_MISMATCH;
}

void VWResourceGroupManager::initialize([[maybe_unused]] const Poco::Util::AbstractConfiguration & config)
{
    bool old_val= false;
    if (started.compare_exchange_strong(old_val, true, std::memory_order_seq_cst, std::memory_order_relaxed))
    {
        VWResourceTask * resource_task = new VWResourceTask(this);
        timer.scheduleAtFixedRate(resource_task, 1, 1);

        UInt64 vw_queue_sync_interval = config.getUInt64("vw_queue_sync_interval", 5);
        vw_queue_sync_thread.emplace(vw_queue_sync_interval, getContext());
        vw_queue_sync_thread->start();
    }
}

IResourceGroup* VWResourceGroupManager::selectGroup(const Context & query_context, const IAST *ast)
{
    auto lock = getReadLock();
    const ClientInfo & client_info = query_context.getClientInfo();

    if (auto vw = query_context.tryGetCurrentVW(); vw)
    {
        const String & vw_name = vw->getName();

        //TODO: Optimize using UFDS when sub-groups are supported
        for (const auto & [key, select_case] : select_cases)
        {
            String parent_resource_group = select_case.group->getName();

            std::vector<IResourceGroup*> parent_groups {select_case.group};
            auto parent = select_case.group->getParent();
            while (parent)
            {
                parent_groups.push_back(parent);
                parent_resource_group = parent->getName();
                parent = parent->getParent();
            }

            if (parent_resource_group == vw_name
                && (select_case.user == nullptr || std::regex_match(client_info.initial_user, *(select_case.user)))
                && (select_case.query_id == nullptr || std::regex_match(client_info.initial_query_id, *(select_case.query_id)))
                && (select_case.query_type == nullptr || *select_case.query_type == ResourceSelectCase::getQueryType(ast)))
            {
                for (const auto & group : parent_groups)
                {
                    group->setInUse(true);
                }
                return select_case.group;
            }
        }
    }

    return nullptr;
}

IResourceGroup* VWResourceGroupManager::addGroup(const String & virtual_warehouse, const VirtualWarehouseData & vw_data) const
{
    auto pr = groups.emplace(std::piecewise_construct,
                   std::forward_as_tuple(virtual_warehouse),
                   std::forward_as_tuple(std::make_shared<VWResourceGroup>(getContext())));

    auto group = std::dynamic_pointer_cast<VWResourceGroup>(pr.first->second);
    group->setName(virtual_warehouse);
    group->setSoftMaxMemoryUsage(default_soft_max_memory_usage);
    group->setMinQueryMemoryUsage(default_min_query_memory_usage);
    group->setMaxConcurrentQueries(vw_data.settings.max_concurrent_queries);
    group->setMaxQueued(vw_data.settings.max_queued_queries);
    group->setMaxQueuedWaitingMs(vw_data.settings.max_queued_waiting_ms);
    group->setPriority(default_priority);
    group->setSyncExpiry(getContext()->getConfigRef().getUInt("vw_queue_sync_expiry_seconds", 15));
    UInt64 time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    group->setLastUsed(time_now);
    group->setInUse(false);
    // Set as root group
    group->setRoot();
    root_groups[virtual_warehouse] = pr.first->second.get();

    ResourceSelectCase select_case;
    select_case.name = "case_" + virtual_warehouse;
    select_case.group = pr.first->second.get();
    vw_select_case_map[virtual_warehouse].emplace_back(select_case.name);
    // TODO: add in the future
    // if (config.has(prefixWithKey + ".user"))
    //     select_case.user = std::make_shared<std::regex>(config.getString(prefixWithKey + ".user"));
    // if (config.has(prefixWithKey + ".query_id"))
    //     select_case.query_id = std::make_shared<std::regex>(config.getString(prefixWithKey + ".query_id"));
    // if (config.has(prefixWithKey + ".query_type"))
    // {
    //     String queryType = config.getString(prefixWithKey + ".query_type");
    //     select_case.query_type = ResourceSelectCase::translateQueryType(queryType);
    //     if (select_case.query_type == nullptr)
    //         throw Exception("Select case's query type is illegal: " + key + " -> " + queryType, ErrorCodes::RESOURCE_GROUP_ILLEGAL_CONFIG);
    // }
    select_cases[select_case.name] = std::move(select_case);
    LOG_DEBUG(&Poco::Logger::get("VWResourceGroupManager"), "Added group " + virtual_warehouse + " using default values");
    return pr.first->second.get();
}

bool VWResourceGroupManager::deleteGroup(const String & virtual_warehouse) const
{
    auto lock = getWriteLock();
    if (ResourceManagement::isSystemVW(virtual_warehouse))
    {
        LOG_DEBUG(&Poco::Logger::get("VWResourceGroupManager"), "Skipping deletion of read/write/task/default VWs.");
        return false;
    }

    auto root_group = root_groups.find(virtual_warehouse);
    if (root_group == root_groups.end())
    {
        LOG_DEBUG(&Poco::Logger::get("VWResourceGroupManager"), "Resource group does not exist locally");
        return false;
    }
    auto info = root_group->second->getInfo();

    // Used to let queries gracefully cancel, since corresponding VW has been deleted.
    auto vw_expiry = getContext()->getConfigRef().getUInt("vw_unused_expiry_seconds", 30);
    UInt64 time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now()
                                                                            .time_since_epoch()).count();
    auto vw_timeout_threshold = time_now - vw_expiry * 1000;
    if (info.queued_queries != 0 || info.running_queries != 0
    || info.in_use || info.last_used >= vw_timeout_threshold)
    {
        LOG_DEBUG(&Poco::Logger::get("VWResourceGroupManager"), "Resource group " + virtual_warehouse + " is still being used, and will not be deleted.");
        return false;
    }

    root_groups.erase(root_group);

    auto child_groups = vw_child_groups.find(virtual_warehouse);
    if (child_groups != vw_child_groups.end())
    {
        for (auto child_name : child_groups->second)
        {
            groups.erase(child_name);
        }
        vw_child_groups.erase(child_groups);
    }

    auto cases = vw_select_case_map.find(virtual_warehouse);
    if (cases != vw_select_case_map.end())
    {
        for (const auto & select_case : cases->second)
        {
            select_cases.erase(select_case);
        }
        vw_select_case_map.erase(cases);
    }
    groups.erase(virtual_warehouse);
    LOG_DEBUG(&Poco::Logger::get("VWResourceGroupManager"), "Deleted VWResourceGroup " + virtual_warehouse);
    return true;
}

QueryQueueInfo VWResourceGroupManager::getLastSyncQueueInfo(const String & resource_grp_name) const
{
    std::lock_guard lock(map_lock);
    auto it = last_sync_queue_map.find(resource_grp_name);

    if (it == last_sync_queue_map.end())
        return QueryQueueInfo{0, 0,{}};
    else
        return it->second;
}

void VWResourceGroupManager::updateLastSyncQueueMap(VWQueryQueueMap last_sync_queue_map_)
{
    std::lock_guard lock(map_lock);
    for (const auto & [vw_name, last_sync_queue_info] : last_sync_queue_map_)
    {
        last_sync_queue_map[vw_name] = last_sync_queue_info;
    }
}

void VWResourceGroupManager::clearLastSyncQueueInfo(const std::vector<String> & deleted_vw_list)
{
    std::lock_guard lock(map_lock);
    for (const auto & vw_name : deleted_vw_list)
        last_sync_queue_map.erase(vw_name);
}

bool VWResourceGroupManager::getAggQueryQueueInfo(const String & resource_grp_name, QueryQueueInfo & agg_query_queue_info) const
{
    std::lock_guard lock(map_lock);
    auto it = agg_query_queue_map.find(resource_grp_name);
    if (it == agg_query_queue_map.end())
        return false;
    else
    {
        agg_query_queue_info = it->second;
        return true;
    }
}

void VWResourceGroupManager::shutdown()
{
    timer.cancel(true);
    if (vw_queue_sync_thread)
        vw_queue_sync_thread->stop();
}

VWResourceGroupManager::~VWResourceGroupManager()
{
    shutdown();
};

}
