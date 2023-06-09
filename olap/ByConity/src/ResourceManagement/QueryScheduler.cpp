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

#include <ResourceManagement/QueryScheduler.h>
#include <ResourceManagement/VirtualWarehouse.h>

#include <Common/Exception.h>
#include <Common/thread_local_rng.h>

namespace DB::ErrorCodes
{
    extern const int RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO;
    extern const int RESOURCE_MANAGER_NO_AVAILABLE_WORKER;
}

namespace DB::ResourceManagement
{

static inline bool cmp_group_cpu(const WorkerGroupAndMetrics & a, const WorkerGroupAndMetrics & b)
{
    return a.second.avg_cpu_usage < b.second.avg_cpu_usage;
}

static inline bool cmp_group_mem(const WorkerGroupAndMetrics & a, const WorkerGroupAndMetrics & b)
{
    return a.second.avg_mem_usage < b.second.avg_mem_usage;
}

static inline bool cmp_worker_cpu(const WorkerNodePtr & a, const WorkerNodePtr & b)
{
    return a->convertCoresToPercents() + a->cpu_usage.load(std::memory_order_relaxed)
        < b->convertCoresToPercents() + b->cpu_usage.load(std::memory_order_relaxed);
}

static inline bool cmp_worker_mem(const WorkerNodePtr & a, const WorkerNodePtr & b)
{
    /// Use add but not sub to avoid uint overflow.
    return a->memory_available.load(std::memory_order_relaxed) + b->reserved_memory_bytes.load(std::memory_order_relaxed)
        < b->memory_available.load(std::memory_order_relaxed) + a->reserved_memory_bytes.load(std::memory_order_relaxed);
}

static inline bool cmp_worker_disk(const WorkerNodePtr & a, const WorkerNodePtr & b)
{
    return a->disk_space.load(std::memory_order_relaxed) > b->disk_space.load(std::memory_order_relaxed);
}

QueryScheduler::QueryScheduler(VirtualWarehouse & vw_) : vw(vw_)
{
    log = &Poco::Logger::get(vw.getName() + " (QueryScheduler)");
}

/// pickWorkerGroups stage 1: filter groups by requirement.
void QueryScheduler::filterGroup(const Requirement & requirement, std::vector<WorkerGroupAndMetrics> & res) const
{
    auto rlock = vw.getReadLock();
    for (const auto & [_, group] : vw.groups)
    {
        if (auto metrics = group->getAggregatedMetrics(); metrics.available(requirement))
            res.emplace_back(group, metrics);
    }
}

/// pickWorkerGroups stage 2: select a group order by algo.
WorkerGroupPtr QueryScheduler::selectGroup(const VWScheduleAlgo & algo, const std::vector<WorkerGroupAndMetrics> & available_groups)
{
    if (available_groups.size() == 1)
        return available_groups[0].first;

    auto comparator = cmp_group_cpu;
    switch (algo)
    {
        case VWScheduleAlgo::Random:
        {
            std::uniform_int_distribution dist;
            auto index = dist(thread_local_rng) % available_groups.size();
            return available_groups[index].first;
        }

        case VWScheduleAlgo::GlobalRoundRobin:
        {
            size_t index = pick_group_sequence.fetch_add(1, std::memory_order_relaxed) % available_groups.size();
            return available_groups[index].first;
        }

        case VWScheduleAlgo::GlobalLowCpu:
            break;

        case VWScheduleAlgo::GlobalLowMem:
            comparator = cmp_group_mem;
            break;

        default:
            throw Exception("Wrong vw_schedule_algo for query scheduler: " + std::string(toString(algo)),
                            ErrorCodes::RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO);
    }

    auto it = std::min_element(available_groups.begin(), available_groups.end(), comparator);
    return it->first;
}

/// Picking a worker group from the virtual warehouse. Two stages:
/// - 1. filter worker groups by resource requirement. @see filterGroup
/// - 2. select one from filtered groups by algo. @see selectGroup
WorkerGroupPtr QueryScheduler::pickWorkerGroup(const VWScheduleAlgo & algo, const Requirement & requirement)
{
    std::vector<WorkerGroupAndMetrics> available_groups;
    filterGroup(requirement, available_groups);
    if (available_groups.empty())
    {
        LOG_WARNING(log, "No available worker group for requirement: {}, choose one randomly.", requirement.toDebugString());
        return vw.randomWorkerGroup();
    }
    return selectGroup(algo, available_groups);
}


/// pickWorker stage 1: filter workers by requirement.
void QueryScheduler::filterWorker(const Requirement & requirement, std::vector<WorkerNodePtr> & res)
{
    auto rlock = vw.getReadLock();

    /// Scan specified worker group's workers
    if (!requirement.worker_group.empty())
    {
        auto required_group = vw.getWorkerGroup(requirement.worker_group);
        for (const auto & [_, worker] : required_group->getWorkers())
        {
            if (worker->available(requirement))
            {
                res.emplace_back(worker);
            }
        }
        return;
    }

    /// Otherwise, scan all.
    for (const auto & [_, group] : vw.groups)
    {
        for (const auto & [_, worker] : group->getWorkers())
        {
            if (worker->available(requirement))
            {
                res.emplace_back(worker);
            }
        }
    }
}

/// pickWorker stage 2: select n workers order by algo and reserve quotas.
std::vector<WorkerNodePtr> QueryScheduler::selectWorkers(const VWScheduleAlgo & algo, const Requirement & requirement, std::vector<WorkerNodePtr> & available_workers)
{
    auto n = requirement.expected_workers;
    std::vector<WorkerNodePtr> res;
    res.reserve(n);
    auto comparator = cmp_worker_mem;
    switch (algo)
    {
        case VWScheduleAlgo::GlobalRoundRobin:
        {
            for (size_t i = 0; i < n; ++i)
            {
                size_t index = pick_worker_sequence.fetch_add(1, std::memory_order_relaxed) % available_workers.size();
                available_workers[index]->reserveResourceQuotas(requirement);
                res.push_back(available_workers[index]);
            }
            return res;
        }

        case VWScheduleAlgo::GlobalLowMem:
            break;

        case VWScheduleAlgo::GlobalLowCpu:
            comparator = cmp_worker_cpu;
            break;

        case VWScheduleAlgo::GlobalLowDisk:
            comparator = cmp_worker_disk;
            break;

        default:
            throw Exception("Wrong vw_schedule_algo for query scheduler: " + std::string(toString(algo)), ErrorCodes::RESOURCE_MANAGER_WRONG_VW_SCHEDULE_ALGO);
    }

    for (size_t i = 0; i < n; ++i)
    {
        auto it = std::min_element(available_workers.begin(), available_workers.end(), comparator);
        (*it)->reserveResourceQuotas(requirement);
        res.push_back(*it);
        if (i == n - 1)
            break;
        if (requirement.no_repeat)
            available_workers.erase(it);
    }
    return res;
}

HostWithPorts QueryScheduler::pickWorker(const VWScheduleAlgo & algo, const Requirement & requirement)
{
    return pickWorkers(algo, requirement)[0];
}

/// Picking n workers from the virtual warehouse. Two stages:
/// - 1. filter workers by resource requirement. @see filterWorker
/// - 2. select at most n workers from filtered workers by algo. @see selectWorkers
std::vector<HostWithPorts> QueryScheduler::pickWorkers(const VWScheduleAlgo & algo, const Requirement & requirement)
{
    std::vector<WorkerNodePtr> workers{};
    bool need_reserve_quotas = true;

    std::vector<WorkerNodePtr> available_workers;
    filterWorker(requirement, available_workers);

    /// If there is no worker meets the requirement,
    /// - If random result is allowed, then do a sampling among all workers.
    /// - If random result is not allowed, then just return an empty result set.
    if (available_workers.empty())
    {
        LOG_WARNING(log, "No available worker for requirement: {}", requirement.toDebugString());
        if (!requirement.forbid_random_result)
        {
            auto group = requirement.worker_group.empty() ? vw.randomWorkerGroup() : vw.getWorkerGroup(requirement.worker_group);
            workers = group->randomWorkers(requirement.expected_workers, requirement.blocklist);
        }
    }
    /// If have not enough available workers, just return all of them.
    else if (available_workers.size() <= requirement.expected_workers && requirement.no_repeat)
    {
        workers = std::move(available_workers);
    }
    /// We have enough available workers, so select some of them according to vw_schedule_algo.
    else
    {
        workers = selectWorkers(algo, requirement, available_workers);
        need_reserve_quotas = false;
    }

    std::vector<HostWithPorts> res;
    res.reserve(workers.size());
    for (const auto & worker : workers)
    {
        if (need_reserve_quotas)
            worker->reserveResourceQuotas(requirement);
        res.push_back(worker->host);
    }
    return res;
}

}
