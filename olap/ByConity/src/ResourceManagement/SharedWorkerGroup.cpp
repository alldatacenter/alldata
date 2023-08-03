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

#include <ResourceManagement/SharedWorkerGroup.h>

#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int RESOURCE_MANAGER_NO_LINKED_GROUP;
}
}

namespace DB::ResourceManagement
{

size_t SharedWorkerGroup::getNumWorkers() const
{
    std::lock_guard lock(state_mutex);
    auto res = 0;
    if (auto linked_grp_shared_ptr = tryGetLinkedGroup())
        res = linked_grp_shared_ptr->getNumWorkers();
    return res;
}

std::map<String, WorkerNodePtr> SharedWorkerGroup::getWorkers() const
{
    std::lock_guard lock(state_mutex);
    return getWorkersImpl(lock);
}

std::map<String, WorkerNodePtr> SharedWorkerGroup::getWorkersImpl(std::lock_guard<std::mutex> & /*lock*/) const
{
    std::map<String, WorkerNodePtr> res;
    if (auto linked_grp_shared_ptr = tryGetLinkedGroup())
        res = linked_grp_shared_ptr->getWorkers();
    return res;
}

WorkerGroupData SharedWorkerGroup::getData(bool with_metrics, bool only_running_state) const
{
    WorkerGroupData data;
    data.id = getID();
    data.type = WorkerGroupType::Shared;
    data.vw_uuid = getVWUUID();
    data.vw_name = getVWName();

    {
        std::lock_guard lock(state_mutex);

        if (auto linked_group_ptr = tryGetLinkedGroup())
            data.linked_id = linked_group_ptr->getID();
        for (const auto & [_, worker] : getWorkersImpl(lock))
        {
            if(!only_running_state || worker->state.load(std::memory_order_relaxed) == WorkerState::Running)
                data.host_ports_vec.push_back(worker->host);
        }
    }

    data.num_workers = data.host_ports_vec.size();

    if (with_metrics)
        data.metrics = getAggregatedMetrics();

    data.is_auto_linked = isAutoLinked();
    data.linked_vw_name = tryGetLinkedGroupVWName();
    return data;
}

WorkerGroupMetrics SharedWorkerGroup::getAggregatedMetrics() const
{
    std::lock_guard lock(state_mutex);
    WorkerGroupMetrics res;
    if (auto linked_grp_shared_ptr = tryGetLinkedGroup())
        res = linked_grp_shared_ptr->getAggregatedMetrics();
    return res;
}

void SharedWorkerGroup::registerNode(const WorkerNodePtr &)
{
    throw Exception("Cannot register node to SharedWorkerGroup", ErrorCodes::LOGICAL_ERROR);
}

void SharedWorkerGroup::removeNode(const String &)
{
    throw Exception("Cannot remove node from SharedWorkerGroup", ErrorCodes::LOGICAL_ERROR);
}

void SharedWorkerGroup::setLinkedGroup(WorkerGroupPtr group)
{
    /// TODO: (zuochuang.zema) maybe in future we can remove this lock for SharedWorkerGroup.
    std::lock_guard lock(state_mutex);
    linked_group = std::move(group);
}

String SharedWorkerGroup::tryGetLinkedGroupVWName() const
{
    std::lock_guard lock(state_mutex);
    String res;
    if (auto linked_grp_shared_ptr = tryGetLinkedGroup())
        res = linked_grp_shared_ptr->getVWName();
    return res;
}

WorkerGroupPtr SharedWorkerGroup::getLinkedGroup() const
{
    auto linked_grp_shared_ptr = linked_group.lock();
    if (!linked_grp_shared_ptr)
        throw Exception("Linked group no longer exists for shared group " + id, ErrorCodes::RESOURCE_MANAGER_NO_LINKED_GROUP);

    return linked_grp_shared_ptr;
}

WorkerGroupPtr SharedWorkerGroup::tryGetLinkedGroup() const
{
    return linked_group.lock();
}

}
