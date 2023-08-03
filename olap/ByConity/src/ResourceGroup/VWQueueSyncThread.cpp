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

#include <ResourceGroup/VWQueueSyncThread.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceGroup/IResourceGroupManager.h>
#include <ResourceGroup/VWResourceGroup.h>
#include <ResourceGroup/VWResourceGroupManager.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <Common/Exception.h>

#include <memory>

namespace DB
{

VWQueueSyncThread::VWQueueSyncThread(UInt64 interval_, ContextPtr global_context_)
    : WithContext(global_context_)
    , interval(interval_)
    , log(&Poco::Logger::get("VWQueueSyncThread"))
{
    LOG_DEBUG(log, "Starting VW Queue Sync");
    task = getContext()->getSchedulePool().createTask("VWQueueSyncThread", [this]{ run(); });
}

bool VWQueueSyncThread::syncQueueDetails(VWResourceGroupManager * vw_resource_group_manager)
{
    auto tries = 3;
    bool done = false;

    while (--tries >= 0 && !done)
    {
        try
        {
            // Aggregates no. of queued and running queries on server

            auto infos = vw_resource_group_manager->getInfoVec();

            UInt64 time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();

            VWQueryQueueMap vw_query_queue_map;

            if (infos.size() > 0)
            {
                // Update resource group last_used variables
                auto groups = vw_resource_group_manager->getGroups();
                for (const auto & info : infos)
                {
                    // Only sync parent resource groups for now
                    if (info.parent_resource_group.empty())
                    {
                        auto group_it = groups.find(info.name);

                        if (group_it == groups.end())
                            throw Exception("Group not found for group info", ErrorCodes::LOGICAL_ERROR);

                        auto group = group_it->second;
                        if (info.in_use)
                        {
                            group->setInUse(false);
                            group->setLastUsed(time_now);
                        }

                        QueryQueueInfo server_query_queue_info {info.queued_queries, info.running_queries};

                        vw_query_queue_map[info.name] = server_query_queue_info;
                    }
                }
            }

            // Retrieve and store VW resource usage snapshot
            auto rm_client = getContext()->getResourceManagerClient();
            if (rm_client)
            {
                std::vector<String> deleted_vw_list;
                auto agg_query_queue_map = rm_client->syncQueueDetails(vw_query_queue_map, &deleted_vw_list);

                vw_resource_group_manager->updateAggQueryQueueMap(agg_query_queue_map);
                vw_resource_group_manager->updateLastSyncQueueMap(vw_query_queue_map);

                vw_resource_group_manager->clearLastSyncQueueInfo(deleted_vw_list);
                for (const auto & vw_name : deleted_vw_list)
                {
                    vw_resource_group_manager->deleteGroup(vw_name);
                }

                time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
                last_sync_time = std::max(last_sync_time.load(), time_now);
                done = true;
            }
        }
        catch (...)
        {
            tryLogDebugCurrentException(log, __PRETTY_FUNCTION__);
        }
    }
    if (tries < 0)
    {
        LOG_DEBUG(log, "Failed to sync VW queue info after 3 tries");
    }

    return done;

}

bool VWQueueSyncThread::syncResourceGroups(VWResourceGroupManager * vw_resource_group_manager)
{
    auto tries = 3;
    auto done = false;
    while (--tries >= 0 && !done)
    {
        try
        {
            // 1. Get all VWs from RM
            // 2. Create ResourceGroups if not present locally
            // 3. Alter ResourceGroups if limits have changed
            std::vector<VirtualWarehouseData> vws;
            getContext()->getResourceManagerClient()->getAllVirtualWarehouses(vws);

            auto groups = vw_resource_group_manager->getGroups();
            std::vector<ResourceGroupDataPair> resource_group_data_pairs;
            std::vector<VirtualWarehouseData> new_vws;

            for (const auto & vw : vws)
            {
                auto it = groups.find(vw.name);
                if (it == groups.end())
                {
                    new_vws.push_back(vw);
                }
                else
                {
                    resource_group_data_pairs.push_back(ResourceGroupDataPair(vw, it->second));
                }
            }

            // Filter out groups that have no changes
            auto has_no_changes = [] (ResourceGroupDataPair entry)
            {
                auto vw_data = entry.first;
                auto resource_group = entry.second;

                bool max_concurrent_updated = vw_data.settings.max_concurrent_queries != UInt64(resource_group->getMaxConcurrentQueries());
                bool max_queue_updated = vw_data.settings.max_queued_queries != UInt64(resource_group->getMaxQueued());
                bool max_queue_waiting_ms_updated = vw_data.settings.max_queued_waiting_ms != UInt64(resource_group->getMaxQueuedWaitingMs());

                return !(max_concurrent_updated || max_queue_updated || max_queue_waiting_ms_updated);
            };

            std::erase_if(resource_group_data_pairs, has_no_changes);

            for (const auto & [vw_data, resource_group] : resource_group_data_pairs)
            {
                // Obtain resource group's root mutex to ensure that no concurrent reads are peformed
                auto lock = resource_group->getLock();
                resource_group->setMaxConcurrentQueries(vw_data.settings.max_concurrent_queries);
                resource_group->setMaxQueued(vw_data.settings.max_queued_queries);
                resource_group->setMaxQueuedWaitingMs(vw_data.settings.max_queued_waiting_ms);
            }

            for (const auto & vw_data : new_vws)
            {
                auto resource_group = vw_resource_group_manager->addGroup(vw_data.name, vw_data);
                auto lock = resource_group->getLock();
                resource_group->setMaxConcurrentQueries(vw_data.settings.max_concurrent_queries);
                resource_group->setMaxQueued(vw_data.settings.max_queued_queries);
                resource_group->setMaxQueuedWaitingMs(vw_data.settings.max_queued_waiting_ms);
            }

            done = true;
        }
        catch (...)
        {
            tryLogDebugCurrentException((log), __PRETTY_FUNCTION__);
        }
    }

    if (tries < 0)
        LOG_DEBUG(log, "Failed to sync resource group info with RM");

    return done;
}

void VWQueueSyncThread::run()
{
    // Resource Groups can be tied to VWs, but not necessarily.
    // 1. Get VWs from ResourceManager. Add and delete ResourceGroups per VW
    // 2. Sync VWs with their respective ResourceGroups
    auto resource_group_manager = getContext()->tryGetResourceGroupManager();
    auto vw_resource_group_manager = dynamic_cast<VWResourceGroupManager *>(resource_group_manager);
    if (!vw_resource_group_manager)
    {
        LOG_DEBUG(&Poco::Logger::get("VWQueueSyncThread"), "Stopping VW Queue Sync Thread because Resource Group Manager is not of type VWResourceGroupManager");
        return;
    }

    syncQueueDetails(vw_resource_group_manager);
    syncResourceGroups(vw_resource_group_manager);

    task->scheduleAfter(interval * 1000);
}

VWQueueSyncThread::~VWQueueSyncThread()
{
    try
    {
        stop();
    }
    catch (...)
    {
        tryLogCurrentException(log);
    }
}

}
