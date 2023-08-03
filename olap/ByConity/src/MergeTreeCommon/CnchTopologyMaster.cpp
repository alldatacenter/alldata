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

#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Catalog/Catalog.h>
#include <common/logger_useful.h>
#include <Common/ConsistentHashUtils/Hash.h>
#include <CloudServices/CnchServerClient.h>
#include <Interpreters/Context.h>
#include <Storages/PartCacheManager.h>
#include <Storages/CnchStorageCache.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int CNCH_NO_AVAILABLE_TOPOLOGY;
}

CnchTopologyMaster::CnchTopologyMaster(ContextPtr context_)
    : WithContext(context_)
    , topology_fetcher(getContext()->getTopologySchedulePool().createTask("TopologyFetcher", [&]() { fetchTopologies(); }))
    , settings{context_->getSettings()}
{
    topology_fetcher->activateAndSchedule();
}

CnchTopologyMaster::~CnchTopologyMaster()
{
    try
    {
        shutDown();
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

void CnchTopologyMaster::fetchTopologies()
{
    try
    {
        auto fetched = getContext()->getCnchCatalog()->getTopologies();
        if (!fetched.empty())
        {
            /// copy current topology and update it with new fetched one.
            auto last_topology = topologies;
            {
                std::lock_guard lock(mutex);
                topologies = fetched;
            }

            /// need to adjust part cache if the server topology change.
            if (getContext()->getServerType() == ServerType::cnch_server && !last_topology.empty())
            {
                /// reset cache if the server fails to sync topology for a while, prevent from ABA problem
                if (topologies.front().getExpiration() > last_topology.front().getExpiration() + 2 * settings.topology_lease_life_ms.totalMilliseconds())
                {
                    LOG_WARNING(log, "Reset part and table cache because of topology change");
                    if (getContext()->getPartCacheManager())
                        getContext()->getPartCacheManager()->reset();
                    if (getContext()->getCnchStorageCache())
                        getContext()->getCnchStorageCache()->reset();
                }
                else if (!HostWithPorts::isExactlySameVec(topologies.front().getServerList(), last_topology.front().getServerList()))
                {
                    LOG_WARNING(log, "Invalid outdated part and table cache because of topology change");
                    if (getContext()->getPartCacheManager())
                        getContext()->getPartCacheManager()->invalidCacheWithNewTopology(topologies.front().getServerList());
                    /// TODO: invalid table cache with new topology.
                    if (getContext()->getCnchStorageCache())
                        getContext()->getCnchStorageCache()->reset();
                }
            }
        }
        else
        {
            /// needed for the 1st time write to kv..
            LOG_TRACE(log, "Cannot fetch topology from remote.");
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    topology_fetcher->scheduleAfter(settings.topology_refresh_interval_ms.totalMilliseconds());
}

std::list<CnchServerTopology> CnchTopologyMaster::getCurrentTopology()
{
    std::lock_guard lock(mutex);
    return topologies;
}

HostWithPorts CnchTopologyMaster::getTargetServerImpl(
        const String & table_uuid,
        std::list<CnchServerTopology> & current_topology,
        const UInt64 current_ts,
        bool allow_empty_result,
        bool allow_tso_unavailable)
{
    auto get_server_for_table = [](const String & uuid, const HostWithPortsVec & servers)
    {
        if (servers.empty())
            return HostWithPorts{};
        auto hashed_index = consistentHashForString(uuid, servers.size());
        return servers[hashed_index];
    };

    HostWithPorts target_server{};
    UInt64 lease_life_time = settings.topology_lease_life_ms.totalMilliseconds();
    bool tso_is_available = (current_ts != TxnTimestamp::fallbackTS());
    UInt64 commit_time_ms = current_ts >> 18;

    auto it = current_topology.begin();
    while(it != current_topology.end())
    {
        auto servers = it->getServerList();
        bool commit_within_lease_life_time = commit_time_ms >= it->getExpiration() - lease_life_time;

        if (!tso_is_available && allow_tso_unavailable && !servers.empty())
        {
            target_server = get_server_for_table(table_uuid, servers);
            LOG_DEBUG(log, "Fallback to first possible target server due to TSO unavailability. servers.size = {}", servers.size());
            break;
        }

        /// currently, topology_lease_life_ms is 12000ms by default. we suppose bytekv MultiWrite timeout is 6000ms.
        if (commit_within_lease_life_time && commit_time_ms < it->getExpiration() - 6000)
        {
            target_server = get_server_for_table(table_uuid, it->getServerList());
            break;
        }
        else if (commit_within_lease_life_time && commit_time_ms < it->getExpiration())
        {
            HostWithPorts server_in_old_topology = get_server_for_table(table_uuid, it->getServerList());
            it++;
            if (it != current_topology.end())
            {
                HostWithPorts server_in_new_topology = get_server_for_table(table_uuid, it->getServerList());
                if (server_in_new_topology.isExactlySame(server_in_old_topology))
                    target_server = server_in_new_topology;
            }
            break;
        }

        it++;
    }

    if (target_server.empty())
    {
        if (!allow_empty_result)
            throw Exception("No available topology for current commit time : " + std::to_string(commit_time_ms) + ". Available topology : " + dumpTopologies(current_topology), ErrorCodes::CNCH_NO_AVAILABLE_TOPOLOGY);
        else
            LOG_INFO(log, "No available topology for current commit time : {}. Available topology : {}", std::to_string(commit_time_ms), dumpTopologies(current_topology));
    }

    return target_server;
}


HostWithPorts CnchTopologyMaster::getTargetServer(const String & table_uuid, bool allow_empty_result, bool allow_tso_unavailable)
{
    /// Its important to get current topology before get current timestamp.
    std::list<CnchServerTopology> current_topology = getCurrentTopology();
    UInt64 ts = getContext()->tryGetTimestamp(__PRETTY_FUNCTION__);

    return getTargetServerImpl(table_uuid, current_topology, ts, allow_empty_result, allow_tso_unavailable);
}

HostWithPorts CnchTopologyMaster::getTargetServer(const String & table_uuid, const UInt64 ts,  bool allow_empty_result, bool allow_tso_unavailable)
{
    std::list<CnchServerTopology> current_topology = getCurrentTopology();
    return getTargetServerImpl(table_uuid, current_topology, ts, allow_empty_result, allow_tso_unavailable);
}


void CnchTopologyMaster::shutDown()
{
    if (topology_fetcher)
        topology_fetcher->deactivate();
}

}
