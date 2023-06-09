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

#include <ResourceManagement/PhysicalWorkerGroup.h>
#include <ResourceManagement/CommonData.h>

#include <iterator>
#include <algorithm>
#include <random>

namespace DB::ErrorCodes
{
    extern const int RESOURCE_MANAGER_NO_AVAILABLE_WORKER;
}

namespace DB::ResourceManagement
{
size_t PhysicalWorkerGroup::getNumWorkers() const
{
    std::lock_guard lock(state_mutex);
    return workers.size();
}

std::map<String, WorkerNodePtr> PhysicalWorkerGroup::getWorkers() const
{
    std::lock_guard lock(state_mutex);
    return getWorkersImpl(lock);
}

std::map<String, WorkerNodePtr> PhysicalWorkerGroup::getWorkersImpl(std::lock_guard<std::mutex> & /*lock*/) const
{
    return workers;
}

WorkerGroupData PhysicalWorkerGroup::getData(bool with_metrics, bool only_running_state) const
{
    WorkerGroupData data;
    data.id = getID();
    data.type = WorkerGroupType::Physical;
    data.vw_uuid = getVWUUID();
    data.vw_name = getVWName();
    data.psm = psm;
    for (const auto & [_, worker] : getWorkers())
    {
        if(!only_running_state || worker->state.load(std::memory_order_relaxed) == WorkerState::Running)
            data.host_ports_vec.push_back(worker->host);
    }
    data.num_workers = data.host_ports_vec.size();

    if (with_metrics)
        data.metrics = getAggregatedMetrics();
    return data;
}

/// A background task refreshing the worker group's aggregated metrics.
void PhysicalWorkerGroup::refreshAggregatedMetrics()
{
    {
        std::lock_guard lock(state_mutex);
        aggregated_metrics.reset();

        auto workers_ = getWorkersImpl(lock);
        auto count = workers_.size();
        for (const auto & [_, worker] : workers_)
        {
            auto worker_cpu_usage = worker->cpu_usage.load(std::memory_order_relaxed);
            if (worker_cpu_usage < aggregated_metrics.min_cpu_usage)
                aggregated_metrics.min_cpu_usage = worker_cpu_usage;
            if (worker_cpu_usage > aggregated_metrics.max_cpu_usage)
                aggregated_metrics.max_cpu_usage = worker_cpu_usage;
            aggregated_metrics.avg_cpu_usage += worker_cpu_usage;

            auto worker_mem_usage = worker->memory_usage.load(std::memory_order_relaxed);
            if (worker_mem_usage < aggregated_metrics.min_mem_usage)
                aggregated_metrics.min_mem_usage = worker_mem_usage;
            if (worker_mem_usage > aggregated_metrics.max_mem_usage)
                aggregated_metrics.max_mem_usage = worker_mem_usage;
            aggregated_metrics.avg_mem_usage += worker_mem_usage;

            auto worker_mem_available = worker->memory_available.load(std::memory_order::relaxed);
            if (worker_mem_available < aggregated_metrics.min_mem_available)
                aggregated_metrics.min_mem_available = worker_mem_available;

            /// For now, we do not count in a worker's disk space.

            aggregated_metrics.total_queries += worker->query_num.load(std::memory_order_relaxed);
        }
        aggregated_metrics.avg_cpu_usage /= count;
        aggregated_metrics.avg_mem_usage /= count;
        aggregated_metrics.num_workers = count;
    }

    refresh_metrics_task->scheduleAfter(1 * 1000);
}

WorkerGroupMetrics PhysicalWorkerGroup::getAggregatedMetrics() const
{
    std::lock_guard lock(state_mutex);
    return aggregated_metrics;
}

void PhysicalWorkerGroup::registerNode(const WorkerNodePtr & node)
{
    std::lock_guard lock(state_mutex);
    /// replace the old Node if exists.
    workers[node->getID()] = node;
}

void PhysicalWorkerGroup::removeNode(const String & worker_id)
{
    std::lock_guard lock(state_mutex);
    workers.erase(worker_id);
}

void PhysicalWorkerGroup::addLentGroupDestID(const String & group_id)
{
    std::lock_guard lock(state_mutex);
    lent_groups_dest_ids.insert(group_id);
}

void PhysicalWorkerGroup::removeLentGroupDestID(const String & group_id)
{
    std::lock_guard lock(state_mutex);
    auto it = lent_groups_dest_ids.find(group_id);
    if (it == lent_groups_dest_ids.end())
        throw Exception("Auto linked group id not found", ErrorCodes::LOGICAL_ERROR);

    lent_groups_dest_ids.erase(it);
}

void PhysicalWorkerGroup::clearLentGroups()
{
    std::lock_guard lock(state_mutex);
    lent_groups_dest_ids.clear();
}

std::unordered_set<String> PhysicalWorkerGroup::getLentGroupsDestIDs() const
{
    std::lock_guard lock(state_mutex);
    return lent_groups_dest_ids;
}

std::vector<WorkerNodePtr> PhysicalWorkerGroup::randomWorkers(const size_t n, const std::unordered_set<String> & blocklist) const
{
    std::vector<WorkerNodePtr> candidates;

    {
        std::lock_guard lock(state_mutex);
        if (!workers.empty())
        {
            candidates.reserve(workers.size() - blocklist.size());
            for (const auto & [_, worker] : workers)
            {
                if (!blocklist.contains(worker->id))
                    candidates.push_back(worker);
            }
        }
    }

    if (candidates.empty())
        throw Exception("No available worker for " + id, ErrorCodes::RESOURCE_MANAGER_NO_AVAILABLE_WORKER);

    std::vector<WorkerNodePtr> res;
    res.reserve(n);
    std::sample(candidates.begin(), candidates.end(), std::back_inserter(res), n, std::mt19937{std::random_device{}()});
    return res;
}

}
