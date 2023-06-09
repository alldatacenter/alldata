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

#include <ResourceManagement/WorkerNode.h>

#include <ResourceManagement/CommonData.h>
#include <Protos/data_models.pb.h>
#include <Protos/RPCHelpers.h>
#include <common/logger_useful.h>

namespace DB::ResourceManagement
{

bool WorkerNode::available(UInt64 part_bytes) const
{
    // 10 seconds
    if (!last_update_time || time(nullptr) - last_update_time >= 10)
        return false;

    if (part_bytes && disk_space <= part_bytes * 1.5)
        return false;

    return true;
}

/// Whether the worker meets the resource requirement.
bool WorkerNode::available(const ResourceRequirement & requirement) const
{
    if (requirement.cpu_usage_max_threshold || requirement.request_cpu_cores)
    {
        double threshold = requirement.cpu_usage_max_threshold == 0 ? 100 : requirement.cpu_usage_max_threshold;
        auto reserve_percents = convertCoresToPercents(requirement.request_cpu_cores + reserved_cpu_cores.load(std::memory_order_relaxed));
        if (cpu_usage.load(std::memory_order_relaxed) + reserve_percents > threshold)
            return false;
    }

    if (requirement.request_mem_bytes)
    {
        auto expected_memory_available = requirement.request_mem_bytes + reserved_memory_bytes.load(std::memory_order_relaxed);
        if(memory_available.load(std::memory_order_relaxed) < expected_memory_available)
            return false;
    }

    if (requirement.request_disk_bytes && disk_space.load(std::memory_order_relaxed) < requirement.request_disk_bytes)
    {
        return false;
    }

    if (requirement.blocklist.contains(id))
        return false;

    return true;
}

void WorkerNode::reserveResourceQuotas(const ResourceRequirement & requirement, const uint32_t n)
{
    if (requirement.task_cold_startup_sec <= 0)
        return;

    auto mem = requirement.request_mem_bytes * n;
    auto cpu = requirement.request_cpu_cores * n;

    if (mem == 0 && cpu == 0)
        return;

    reserved_cpu_cores.fetch_add(cpu, std::memory_order_relaxed);
    reserved_memory_bytes.fetch_add(mem, std::memory_order_relaxed);

    time_t delete_time = time(nullptr) + requirement.task_cold_startup_sec;
    std::lock_guard lock(deduction_mutex);
    if (cpu)
        deductions.push_back(DeductionEntry(DeductionType::Cpu, cpu, delete_time));
    if (mem)
        deductions.push_back(DeductionEntry(DeductionType::Mem, mem, delete_time));
}

String WorkerNode::toDebugString() const
{
    std::stringstream ss;
    ss << "{id:" << id
       << " worker_group_id:" << worker_group_id
       << " vw:" << vw_name
       << " host:" << host.toDebugString()
       << " cpu_limit:" << cpu_limit
       << " cpu_usage:" << cpu_usage.load(std::memory_order_relaxed)
       << " reserved_cpu_cores:" << reserved_cpu_cores.load(std::memory_order_relaxed)
       << " memory_limit:" << memory_limit
       << " memory_usage:" << memory_usage
       << " memory_available:" << memory_available.load(std::memory_order_relaxed)
       << " reserved_memory_bytes:" << reserved_memory_bytes.load(std::memory_order_relaxed)
       << " disk_space:" << disk_space.load(std::memory_order_relaxed)
       << " query_num:" << query_num.load(std::memory_order_relaxed);
    return ss.str();
}

void WorkerNode::update(const WorkerNodeResourceData & data, const size_t register_granularity)
{
    cpu_usage.store(data.cpu_usage, std::memory_order_relaxed);
    memory_usage.store(data.memory_usage, std::memory_order_relaxed);
    memory_available.store(data.memory_available, std::memory_order_relaxed);
    disk_space.store(data.disk_space, std::memory_order_relaxed);
    query_num.store(data.query_num, std::memory_order_relaxed);
    auto now = time(nullptr);
    last_update_time = now;

    /// Remove outdated deduction entries.
    {
        std::lock_guard lock(deduction_mutex);
        for (auto it = deductions.begin(); it != deductions.end(); )
        {
            if (it->delete_time < now)
            {
                switch (it->type)
                {
                    case DeductionType::Mem:
                        reserved_memory_bytes.fetch_sub(it->value, std::memory_order_relaxed);
                        break;
                    case DeductionType::Cpu:
                        reserved_cpu_cores.fetch_sub(it->value, std::memory_order_relaxed);
                        break;
                }
                it = deductions.erase(it);
            }
            else
            {
                ++it;
            }
        }
    }

    /// Change all Registering workers to Running state every $register_granularity seconds.
    if (state.load(std::memory_order_relaxed) == WorkerState::Registering
        && last_update_time / register_granularity > register_time / register_granularity)
    {
        state.store(WorkerState::Running, std::memory_order_relaxed);
    }
}

WorkerNodeResourceData WorkerNode::getResourceData() const
{
    WorkerNodeResourceData res;
    res.host_ports = host;
    res.vw_name = vw_name;
    res.worker_group_id = worker_group_id;

    res.cpu_usage = cpu_usage.load(std::memory_order_relaxed);
    res.memory_usage = memory_usage.load(std::memory_order_relaxed);
    res.memory_available = memory_available.load(std::memory_order_relaxed);
    res.disk_space = disk_space.load(std::memory_order_relaxed);
    res.query_num = query_num.load(std::memory_order_relaxed);

    res.cpu_limit = cpu_limit;
    res.memory_limit = memory_limit;

    res.last_update_time = last_update_time;

    return res;
}

void WorkerNode::init(const WorkerNodeResourceData & data, const bool set_running)
{
    register_time = time(nullptr);
    state.store(set_running ? WorkerState::Running : WorkerState::Registering, std::memory_order_relaxed);

    update(data);

    host = data.host_ports;
    /// Init worker's cpu cores with a default value 60.
    cpu_limit = data.cpu_limit ? data.cpu_limit : 60;
    memory_limit = data.memory_limit;

    id = data.id;
    vw_name = data.vw_name;
    worker_group_id = data.worker_group_id;
}

void WorkerNode::fillProto(Protos::WorkerNodeData & entry) const
{
    entry.set_id(id);
    entry.set_worker_group_id(worker_group_id);
    RPCHelpers::fillHostWithPorts(host, *entry.mutable_host_ports());
}

void WorkerNode::fillProto(Protos::WorkerNodeResourceData & entry) const
{
    RPCHelpers::fillHostWithPorts(host, *entry.mutable_host_ports());

    entry.set_query_num(query_num.load(std::memory_order_relaxed));
    entry.set_cpu_usage(cpu_usage.load(std::memory_order_relaxed));
    entry.set_memory_usage(memory_usage.load(std::memory_order_relaxed));
    entry.set_disk_space(disk_space.load(std::memory_order_relaxed));
    entry.set_memory_available(memory_available.load(std::memory_order_relaxed));

    entry.set_id(id);
    entry.set_vw_name(vw_name);
    entry.set_worker_group_id(worker_group_id);
    entry.set_last_update_time(static_cast<UInt32>(last_update_time));

    entry.set_reserved_memory_bytes(reserved_memory_bytes.load(std::memory_order_relaxed));
    entry.set_reserved_cpu_cores(reserved_cpu_cores.load(std::memory_order_relaxed));
    entry.set_register_time(static_cast<UInt32>(register_time));
    entry.set_state(static_cast<UInt32>(state.load(std::memory_order_relaxed)));
}

}
