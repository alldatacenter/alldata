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
#include <Core/Settings.h>
#include <Interpreters/Context.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <ResourceGroup/VWResourceGroup.h>
#include <ResourceGroup/VWResourceGroupManager.h>
#include <ServiceDiscovery/IServiceDiscovery.h>
#include <Interpreters/ProcessList.h>
#include <chrono>
#include <memory>

namespace DB
{

namespace ErrorCodes
{
    extern const int RESOURCE_NOT_ENOUGH;
    extern const int WAIT_FOR_RESOURCE_TIMEOUT;
    extern const int RESOURCE_GROUP_INTERNAL_ERROR;
    extern const int LOGICAL_ERROR;
}

VWResourceGroup::VWResourceGroup(ContextPtr context_)
    : WithContext(context_)
    , log(&Poco::Logger::get("VWResourceGroup")) {};

bool isVWQueueSyncOutdated(const VWResourceGroupManager & manager, UInt64 timeout)
{
    const auto last_sync_time = manager.getLastSyncTime();
    UInt64 time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    auto timeout_threshold = time_now - timeout * 1000;

    return last_sync_time < timeout_threshold;
}

Int32 VWResourceGroup::getNumServers() const
{
    // Set topology master once ready to prevent excessive Context lock obtaining
    if (unlikely(!topology_master))
    {
        LOG_DEBUG(log, "No topology master found. Start to create.");
        if (const auto & topology_master_ = getContext()->getCnchTopologyMaster())
        {
            LOG_DEBUG(log, "Set topology master.");
            topology_master = topology_master_;
        }
        else
            return 1;
    }

    auto current_topology = topology_master->getCurrentTopology();
    if (current_topology.size() == 0)
        return 1;

    return current_topology.back().getServerSize();
}

bool VWResourceGroup::canRunMore() const
{
    auto resource_group_mgr = getContext()->tryGetResourceGroupManager();
    if (!resource_group_mgr)
    {
        LOG_DEBUG(log, "Unable to get Resource Group Manager, server should be shutting down");
        return false;
    }
    const auto & manager = dynamic_cast<const VWResourceGroupManager &>(*resource_group_mgr);
    auto expiry = getSyncExpiry();

    QueryQueueInfo agg_queue_info;
    auto in_agg_map = manager.getAggQueryQueueInfo(name, agg_queue_info);

    if (isVWQueueSyncOutdated(manager, expiry) || !in_agg_map)
    {
        if (!logged)
        {
            if (manager.getLastSyncTime() == 0)
            {
                LOG_WARNING(log,"Resource Manager unavailable.\
                        Local query queue info will be used instead.");
            }
            else
            {
                LOG_WARNING(log,"Previous query queue info has expired/ is unavailable.\
                        Local query queue info will be used instead.");
            }
            logged = true;
        }
        Int64 current_running_queries = running_queries.size();
        return (max_concurrent_queries == 0 ||
                (current_running_queries < std::ceil(double (max_concurrent_queries) / getNumServers())))
        && (soft_max_memory_usage == 0 || cached_memory_usage_bytes < soft_max_memory_usage);
    }
    else
    {
        Int64 current_running_queries = running_queries.size();
        logged = false;

        auto last_sync_info = manager.getLastSyncQueueInfo(getName());

        // TODO: Remove after debugging
        if (max_concurrent_queries != 0
            && current_running_queries - last_sync_info.running_query_count >=
            std::ceil(double(max_concurrent_queries - agg_queue_info.running_query_count) / getNumServers()))
        {
            if (!running_limit_debug_logged)
            {
                running_limit_debug_logged = true;
                LOG_DEBUG(log, "Cannot run more queries. \
                        \nCurrent running: " + std::to_string(current_running_queries) +
                        "\nLast synced running " + std::to_string(last_sync_info.running_query_count) +
                        "\nMax concurrent: " + std::to_string(max_concurrent_queries) +
                        "\nTotal query count: " + std::to_string(agg_queue_info.running_query_count) +
                        "\nNum_servers: " + std::to_string(getNumServers()) +
                        "\nLimit: " + std::to_string(std::ceil((double(max_concurrent_queries) - agg_queue_info.running_query_count) / getNumServers()))
                );
            }
        }
        else
        {
            running_limit_debug_logged = false;
        }

        return (max_concurrent_queries == 0 ||
            current_running_queries - last_sync_info.running_query_count <
                std::ceil((double(max_concurrent_queries) - agg_queue_info.running_query_count) / getNumServers()))
            && (soft_max_memory_usage == 0 || cached_memory_usage_bytes < soft_max_memory_usage);
    }
}

bool VWResourceGroup::canQueueMore() const
{
    // Check sync time and use local server stats if sync is not yet performed/outdated
    auto resource_group_mgr = getContext()->tryGetResourceGroupManager();
    if (!resource_group_mgr)
    {
        LOG_DEBUG(log, "Unable to get Resource Group Manager, server should be shutting down");
        return false;
    }
    const auto & vw_resource_grp_mgr = dynamic_cast<const VWResourceGroupManager &>(*resource_group_mgr);
    auto expiry = getSyncExpiry();

    QueryQueueInfo agg_queue_info;
    auto in_agg_map = vw_resource_grp_mgr.getAggQueryQueueInfo(name, agg_queue_info);

    if (isVWQueueSyncOutdated(vw_resource_grp_mgr, expiry)
        || !in_agg_map)
    {
        if (!logged)
        {
            if (vw_resource_grp_mgr.getLastSyncTime() == 0)
            {
                LOG_WARNING(log,"Resource Manager unavailable.\
                        Local query queue info will be used instead.");
            }
            else
            {
                LOG_WARNING(log,"Previous query queue info has expired/ is unavailable.\
                        Local query queue info will be used instead.");
            }
            logged = true;
        }
        Int64 current_queued_queries = queued_queries.size();
        return current_queued_queries < std::ceil(double(max_queued) / getNumServers());
    }
    else
    {
        Int64 current_queued_queries = queued_queries.size();
        logged = false;

        auto last_sync_info = vw_resource_grp_mgr.getLastSyncQueueInfo(getName());

        // TODO: Remove after debugging
        if (current_queued_queries - last_sync_info.queued_query_count >=
            std::ceil((double(max_queued) - agg_queue_info.queued_query_count) / getNumServers()))
        {
            if (!queued_limit_debug_logged)
            {
                queued_limit_debug_logged = true;
                LOG_DEBUG(log, "Cannot queue more queries. \
                        \nCurrent queued: " + std::to_string(current_queued_queries) +
                        "\nLast synced queued " + std::to_string(last_sync_info.queued_query_count) +
                        "\nMax queued: " + std::to_string(max_queued) +
                        "\nTotal query count: " + std::to_string(agg_queue_info.queued_query_count) +
                        "\nNum_servers: " + std::to_string(getNumServers()) +
                        "\nLimit: " + std::to_string(std::ceil((double(max_queued) - agg_queue_info.queued_query_count) / getNumServers()))
                );
            }
        }
        else
        {
            queued_limit_debug_logged = false;
        }
        return current_queued_queries - last_sync_info.queued_query_count <
                std::ceil((double(max_queued) - agg_queue_info.queued_query_count) / getNumServers());
    }
}

}
