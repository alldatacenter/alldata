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

#pragma once

#include <Common/RWLock.h>
#include <Interpreters/Context.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceGroup/IResourceGroup.h>
#include <ResourceGroup/IResourceGroupManager.h>
#include <ResourceGroup/VWQueueSyncThread.h>
#include <ResourceManagement/CommonData.h>
#include <Parsers/IAST.h>
#include <Poco/Util/AbstractConfiguration.h>
#include <Poco/Util/Timer.h>

#include <atomic>
#include <regex>
#include <vector>
#include <unordered_set>

namespace DB
{

/** Manages VWResourceGroups, which track resource usage of each VW
  * Currently rely on Resource Manager for synchronisation of resource usages
  * across multiple servers.
  * Supports dynamic adding and deleting of resource groups
  */
class VWResourceGroupManager : public IResourceGroupManager, protected WithContext
{
public:
    VWResourceGroupManager(ContextPtr global_context_) : WithContext(global_context_) {}
    ~VWResourceGroupManager() override;

    void initialize(const Poco::Util::AbstractConfiguration & config) override;
    IResourceGroup * selectGroup(const Context & query_context, const IAST * ast) override;
    void shutdown() override;

    IResourceGroup * addGroup(const String & virtual_warehouse, const VirtualWarehouseData & vw_data) const;

    /* Deletes a resource group.
    ** Current implementation deletes the pointer entries in groups and rootGroups.
    ** As such, retrieval of a VWResourceGroup via VWResourceGroupManager must ensure
    ** that a valid pointer is received,
    ** i.e. either the group is retrieved via a copied shared_ptr, or a raw pointer that is
    ** deferenced only when the group still exists.
    */
    bool deleteGroup(const String & virtual_warehouse) const;

    // Retrieves the time where a successful sync to Resource Manager was made
    UInt64 getLastSyncTime() const
    {
        return vw_queue_sync_thread ? vw_queue_sync_thread->getLastSyncTime() : 0;
    }

    void updateAggQueryQueueMap(AggQueryQueueMap agg_query_queue_map_)
    {
        std::lock_guard lock(map_lock);
        agg_query_queue_map = std::move(agg_query_queue_map_);
    }

    QueryQueueInfo getLastSyncQueueInfo(const String & resource_grp_name) const;

    void updateLastSyncQueueMap(VWQueryQueueMap vw_query_queue_map_);

    void clearLastSyncQueueInfo(const std::vector<String> & deleted_vw_list);

    bool getAggQueryQueueInfo(const String & resource_grp_name, QueryQueueInfo & agg_queue_info) const;

private:
    // Task that facilitates executing of queued queries and updating of statistics
    class VWResourceTask : public Poco::Util::TimerTask
    {
        public:
            VWResourceTask(VWResourceGroupManager * manager_) : manager(manager_) {}

            virtual void run() override
            {
                auto lock = manager->getReadLock();
                for (auto [_, root] : manager->root_groups)
                {
                    root->processQueuedQueues();
                }
            }
        private:
            VWResourceGroupManager * manager;
    };

private:
    mutable std::unordered_map<String, std::vector<String>> vw_child_groups;
    mutable std::unordered_map<String, std::vector<String>> vw_select_case_map;
    // Thread that syncs resource usage with Resource Manager
    std::optional<VWQueueSyncThread> vw_queue_sync_thread;

    mutable std::mutex map_lock;
    ServerQueryQueueMap last_sync_queue_map;
    AggQueryQueueMap agg_query_queue_map;

    // Default values used for adding of resource groups
    Int32 default_soft_max_memory_usage = 0;
    Int32 default_min_query_memory_usage = 536870912; /// default 512MB
    Int32 default_priority = 0;
};

}
