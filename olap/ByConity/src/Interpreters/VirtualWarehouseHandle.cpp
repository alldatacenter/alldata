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

#include <Catalog/Catalog.h>
#include <Common/Exception.h>
#include <Common/thread_local_rng.h>
#include <Interpreters/VirtualWarehouseHandle.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <ResourceManagement/ResourceManagerClient.h>
#include <ServiceDiscovery/IServiceDiscovery.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int VIRTUAL_WAREHOUSE_NOT_INITIALIZED;
    extern const int RESOURCE_MANAGER_LOCAL_NO_AVAILABLE_WORKER_GROUP;
    extern const int RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO;
}

VirtualWarehouseHandleImpl::VirtualWarehouseHandleImpl(
    VirtualWarehouseHandleSource source_, std::string name_, UUID uuid_, const ContextPtr global_context_, const VirtualWarehouseSettings & settings_)
    : WithContext(global_context_)
    , source(source_)
    , name(std::move(name_))
    , uuid(uuid_)
    , settings(settings_)
    , log(&Poco::Logger::get(name + " (VirtualWarehouseHandle)"))
{
    tryUpdateWorkerGroups(ForceUpdate);
}

VirtualWarehouseHandleImpl::VirtualWarehouseHandleImpl(
    VirtualWarehouseHandleSource source_, const VirtualWarehouseData & vw_data, const ContextPtr global_context_)
    : VirtualWarehouseHandleImpl(source_, vw_data.name, vw_data.uuid, global_context_, vw_data.settings)
{
}

size_t VirtualWarehouseHandleImpl::empty(UpdateMode update_mode)
{
    tryUpdateWorkerGroups(update_mode);

    std::lock_guard lock(state_mutex);
    return worker_groups.empty();
}

VirtualWarehouseHandleImpl::Container VirtualWarehouseHandleImpl::getAll(UpdateMode update_mode)
{
    tryUpdateWorkerGroups(update_mode);

    std::lock_guard lock(state_mutex);
    return worker_groups;
}


WorkerGroupHandle VirtualWarehouseHandleImpl::getWorkerGroup(const String & worker_group_id, UpdateMode update_mode)
{
    tryUpdateWorkerGroups(update_mode);

    std::lock_guard lock(state_mutex);

    auto it = worker_groups.find(worker_group_id);
    if (it == worker_groups.end())
        throw Exception("There is no worker group with id " + worker_group_id + " in VW " + name, ErrorCodes::LOGICAL_ERROR);
    return it->second;
}

WorkerGroupHandle VirtualWarehouseHandleImpl::pickWorkerGroup(VWScheduleAlgo algo, const Requirement & requirement, UpdateMode update_mode)
{
    tryUpdateWorkerGroups(update_mode);

    if (!algo)
        algo = settings.vw_schedule_algo;

    // All LOCAL_* algorithms use local scheduler.
    if (algo < VWScheduleAlgo::GlobalRoundRobin)
        return pickLocally(algo, requirement);

    // GLOBAL_* algorithms use RM service.
    auto rm_client = getContext()->getResourceManagerClient();
    if (!rm_client)
    {
        LOG_WARNING(log, "RM client is invalid, pick a worker group randomly for {}.", name);
        return randomWorkerGroup();
    }

    try
    {
        auto worker_group_data = rm_client->pickWorkerGroup(name, algo, {});

        {
            std::lock_guard lock(state_mutex);
            auto it = worker_groups.find(worker_group_data.id);
            if (it != worker_groups.end())
                return it->second;
        }
        LOG_WARNING(log, "Worker group returned from RM is not found locally, pick a worker group randomly.");
    }
    catch (const Exception & e)
    {
        LOG_WARNING(log, "Failed to connect to RM, pick a worker group randomly: {}", e.displayText());
    }

    return randomWorkerGroup();
}

void VirtualWarehouseHandleImpl::tryUpdateWorkerGroups(UpdateMode update_mode)
{
    if (update_mode == NoUpdate)
        return;

    UInt64 current_ns = clock_gettime_ns(CLOCK_MONOTONIC_COARSE);
    if (update_mode == ForceUpdate)
    {
        last_update_time_ns.store(current_ns);
    }
    else
    {
        constexpr UInt64 update_interval_ns = 500ULL * 1000 * 1000; // 500ms TODO: make it configurable
        UInt64 the_last_update_time_ns = last_update_time_ns.load();
        if (current_ns < the_last_update_time_ns + update_interval_ns)
            return;
        if (!last_update_time_ns.compare_exchange_strong(the_last_update_time_ns, current_ns))
            return;
    }

    // Try to retrieve worker group info from RM first
    auto success = updateWorkerGroupsFromRM();

    if (!success)
        success = updateWorkerGroupsFromPSM();

    if (!success && worker_groups.empty())
        LOG_WARNING(log, "Updating of worker groups failed");
}


bool VirtualWarehouseHandleImpl::addWorkerGroupImpl(const WorkerGroupHandle & worker_group, const std::lock_guard<std::mutex> & /*lock*/)
{
    auto [_, success] = worker_groups.try_emplace(worker_group->getID(), worker_group);
    if (!success)
        LOG_DEBUG(log, "Worker group {} already exists in {}.", worker_group->getQualifiedName(), name);
    return success;
}

bool VirtualWarehouseHandleImpl::addWorkerGroup(const WorkerGroupHandle & worker_group)
{
    std::lock_guard lock(state_mutex);
    return addWorkerGroupImpl(worker_group, lock);
}

bool VirtualWarehouseHandleImpl::updateWorkerGroupsFromRM()
{
    try
    {
        auto rm_client = getContext()->getResourceManagerClient();
        if (!rm_client)
        {
            LOG_TRACE(log, "The client of ResourceManagement is not initialized");
            return false;
        }

        std::vector<WorkerGroupData> groups_data;
        rm_client->getWorkerGroups(name, groups_data);
        if (groups_data.empty())
        {
            LOG_DEBUG(log, "No worker group found in VW {} from RM", name);
            return false;
        }

        std::lock_guard lock(state_mutex);

        Container old_groups;
        old_groups.swap(worker_groups);

        for (auto & group_data : groups_data)
        {
            if (auto it = old_groups.find(group_data.id); it == old_groups.end()) /// new worker group
                worker_groups.try_emplace(group_data.id, std::make_shared<WorkerGroupHandleImpl>(group_data, getContext()));
            else if (!it->second->isSame(group_data)) /// replace with the new one because of diff
                worker_groups.try_emplace(group_data.id, std::make_shared<WorkerGroupHandleImpl>(group_data, getContext()));
            else /// reuse the old worker group handle and update metrics.
            {
                it->second->setMetrics(group_data.metrics);
                worker_groups.try_emplace(group_data.id, it->second);
            }
        }

        return true;
    }
    catch (...)
    {
        tryLogDebugCurrentException(__PRETTY_FUNCTION__);
        return false;
    }
}

bool VirtualWarehouseHandleImpl::updateWorkerGroupsFromPSM()
{
    try
    {
        auto sd = getContext()->getServiceDiscoveryClient();
        auto psm = getContext()->getVirtualWarehousePSM();

        auto groups_map = sd->lookupWorkerGroupsInVW(psm, name);
        if (groups_map.empty())
        {
            LOG_DEBUG(log, "No worker group found in VW {} from PSM {}", name, psm);
            return false;
        }

        std::lock_guard lock(state_mutex);

        Container old_groups;
        old_groups.swap(worker_groups);

        for (auto & [group_tag, hosts] : groups_map)
        {
            auto group_id = group_tag + PSM_WORKER_GROUP_SUFFIX;
            if (auto it = old_groups.find(group_id); it == old_groups.end()) /// new worker group
                worker_groups.try_emplace(
                    group_id, std::make_shared<WorkerGroupHandleImpl>(group_id, WorkerGroupHandleSource::PSM, name, hosts, getContext()));
            else if (!HostWithPorts::isExactlySameVec(
                         it->second->getHostWithPortsVec(), hosts)) /// replace with the new one because of diff
                worker_groups.try_emplace(
                    group_id, std::make_shared<WorkerGroupHandleImpl>(group_id, WorkerGroupHandleSource::PSM, name, hosts, getContext()));
            else /// reuse the old one
                worker_groups.try_emplace(group_id, it->second);
        }

        return true;
    }
    catch (...)
    {
        tryLogDebugCurrentException(__PRETTY_FUNCTION__);
        return false;
    }
}

using WorkerGroupAndMetrics = std::pair<WorkerGroupHandle, WorkerGroupMetrics>;
static inline bool cmp_group_cpu(const WorkerGroupAndMetrics  & a, const WorkerGroupAndMetrics & b)
{
    return a.second.avg_cpu_usage < b.second.avg_cpu_usage;
}

static inline bool cmp_group_mem(const WorkerGroupAndMetrics & a, const WorkerGroupAndMetrics & b)
{
    return a.second.avg_mem_usage < b.second.avg_mem_usage;
}

/// pickLocally stage 1: filter worker groups by requirement.
void VirtualWarehouseHandleImpl::filterGroup(const Requirement & requirement, std::vector<WorkerGroupAndMetrics> & res)
{
    /// TODO: (zuochuang.zema) read-write lock.
    std::lock_guard lock(state_mutex);
    for (const auto & [_, group] : worker_groups)
    {
        if (!group->empty())
        {
            if (auto metrics = group->getMetrics(); metrics.available(requirement))
                res.emplace_back(group, metrics);
        }
    }
}

/// pickLocally stage 2: select one group by algo.
WorkerGroupHandle VirtualWarehouseHandleImpl::selectGroup(const VWScheduleAlgo & algo, std::vector<WorkerGroupAndMetrics> & available_groups)
{
    if (available_groups.size() == 1)
        return available_groups[0].first;

    auto comparator = cmp_group_cpu;
    switch (algo)
    {
        case VWScheduleAlgo::Unknown:
        case VWScheduleAlgo::Random:
        {
            std::uniform_int_distribution dist;
            auto index = dist(thread_local_rng) % available_groups.size();
            return available_groups[index].first;
        }

        case VWScheduleAlgo::LocalRoundRobin:
        {
            size_t index = pick_group_sequence.fetch_add(1, std::memory_order_relaxed) % available_groups.size();
            return available_groups[index].first;
        }
        case VWScheduleAlgo::LocalLowCpu:
            break;
        case VWScheduleAlgo::LocalLowMem:
            comparator = cmp_group_mem;
            break;
        default:
            const auto & s = std::string(ResourceManagement::toString(algo));
            throw Exception("Wrong vw_schedule_algo for local scheduler: " + s, ErrorCodes::RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO);
    }

    /// Choose from the two lowest CPU worker groups but not the lowest one as the metrics on server may be outdated.
    if (available_groups.size() >= 3)
    {
        std::partial_sort(available_groups.begin(), available_groups.begin() + 2, available_groups.end(), comparator);
        std::uniform_int_distribution dist;
        auto index = (dist(thread_local_rng) % 3) >> 1; /// prefer index 0 than 1.
        return available_groups[index].first;
    }
    else
    {
        auto it = std::min_element(available_groups.begin(), available_groups.end(), comparator);
        return it->first;
    }
}

/// Picking a worker group from local cache. Two stages:
/// - 1. filter worker groups by requirement.
/// - 2. select one group by algo.
WorkerGroupHandle VirtualWarehouseHandleImpl::pickLocally(const VWScheduleAlgo & algo, const Requirement & requirement)
{
    std::vector<WorkerGroupAndMetrics> available_groups;
    filterGroup(requirement, available_groups);
    if (available_groups.empty())
    {
        LOG_WARNING(log, "No available worker groups for requirement: {}, choose one randomly.", requirement.toDebugString());
        return randomWorkerGroup();
    }
    return selectGroup(algo, available_groups);
}

CnchWorkerClientPtr VirtualWarehouseHandleImpl::getWorkerByHash(const String & key)
{
    /// TODO: Should we expand the worker list first?
    UInt64 val = std::hash<String>{}(key);
    std::lock_guard lock(state_mutex);
    auto wg_index = val % worker_groups.size();
    auto & group = std::next(worker_groups.begin(), wg_index)->second;
    return group->getWorkerClientByHash(key);
}

/// Get a worker from the VW using a random strategy.
CnchWorkerClientPtr VirtualWarehouseHandleImpl::getWorker()
{
    auto wg_handle = randomWorkerGroup();
    return wg_handle->getWorkerClient();
}

std::vector<CnchWorkerClientPtr> VirtualWarehouseHandleImpl::getAllWorkers()
{
    std::vector<CnchWorkerClientPtr> res;
    std::lock_guard lock(state_mutex);
    for (const auto & [_, wg_handle] : worker_groups)
    {
        const auto & workers = wg_handle->getWorkerClients();
        res.insert(res.end(), workers.begin(), workers.end());
    }
    return res;
}

WorkerGroupHandle VirtualWarehouseHandleImpl::randomWorkerGroup() const
{
    std::uniform_int_distribution dist;

    {
        std::lock_guard lock(state_mutex);
        if (auto size = worker_groups.size())
        {
            auto begin = dist(thread_local_rng) % size;
            for (size_t i = 0; i < size; i++)
            {
                auto index = (begin + i) % size;
                auto & group = std::next(worker_groups.begin(), index)->second;
                if (!group->getHostWithPortsVec().empty())
                    return group;
            }
        }
    }

    throw Exception("No local available worker group for " + name, ErrorCodes::RESOURCE_MANAGER_LOCAL_NO_AVAILABLE_WORKER_GROUP);
}

std::optional<HostWithPorts> VirtualWarehouseHandleImpl::tryPickWorkerFromRM(VWScheduleAlgo algo, const Requirement & requirement)
{
    if (auto rm_client = getContext()->getResourceManagerClient())
    {
        try
        {
            return rm_client->pickWorker(name, algo, requirement);
        }
        catch (const Exception & e)
        {
            LOG_ERROR(log, "Failed to pick a worker in vw: {} from RM: {}", name, e.displayText());
        }
    }
    return {};
}

}
