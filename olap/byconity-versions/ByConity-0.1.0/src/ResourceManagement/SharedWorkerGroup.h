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
#include <ResourceManagement/IWorkerGroup.h>

namespace DB::ResourceManagement
{

class VirtualWarehouse;

class SharedWorkerGroup : public IWorkerGroup
{
public:
    SharedWorkerGroup(String id_, UUID vw_uuid_, String linked_id_, bool is_auto_linked_ = false)
        : IWorkerGroup(WorkerGroupType::Shared, std::move(id_), vw_uuid_)
        , linked_id(std::move(linked_id_))
        , is_auto_linked(is_auto_linked_)
    {
    }

    size_t getNumWorkers() const override;
    std::map<String, WorkerNodePtr> getWorkers() const override;
    WorkerGroupData getData(bool with_metrics = false, bool only_running_state = true) const override;
    WorkerGroupMetrics getAggregatedMetrics() const override;

    void registerNode(const WorkerNodePtr &) override;
    void removeNode(const String &) override;

    bool empty() const override
    {
        if (auto linked_grp_shared_ptr = tryGetLinkedGroup())
            return linked_grp_shared_ptr->empty();
        else
            return true;
    }

    std::vector<WorkerNodePtr> randomWorkers(const size_t n, const std::unordered_set<String> & blocklist) const override
    {
        return getLinkedGroup()->randomWorkers(n, blocklist);
    }

    void setLinkedGroup(WorkerGroupPtr group);

    String tryGetLinkedGroupVWName() const;

    bool isAutoLinked() const { return is_auto_linked; }

    WorkerGroupPtr getLinkedGroup() const;
    WorkerGroupPtr tryGetLinkedGroup() const;

private:
    std::map<String, WorkerNodePtr> getWorkersImpl(std::lock_guard<std::mutex> & lock) const;

    const String linked_id;
    WorkerGroupWeakPtr linked_group;

    bool is_auto_linked;
};

using SharedWorkerGroupPtr = std::shared_ptr<SharedWorkerGroup>;
using VirtualWarehousePtr = std::shared_ptr<VirtualWarehouse>;

}
