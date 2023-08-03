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

#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/PhysicalWorkerGroup.h>
#include <ResourceManagement/QueryScheduler.h>
#include <ResourceManagement/SharedWorkerGroup.h>
#include <Common/Config/ConfigProcessor.h>

#include <vector>
#include <map>
#include <shared_mutex>

namespace DB
{

namespace Catalog
{
class Catalog;
using CatalogPtr = std::shared_ptr<Catalog>;
}

namespace ResourceManagement
{
using VirtualWarehousePtr = std::shared_ptr<VirtualWarehouse>;

/// VirtualWarehouse consists of WorkerGroups
class VirtualWarehouse : private boost::noncopyable, public std::enable_shared_from_this<VirtualWarehouse>
{
    using ReadLock = std::shared_lock<std::shared_mutex>;
    using WriteLock = std::unique_lock<std::shared_mutex>;
    friend class QueryScheduler;
    friend class VirtualWarehouseFactory;

    /// NOTE: Using VirtualWarehouseFactory::create(...).
    VirtualWarehouse(String n, UUID u, const VirtualWarehouseSettings & s = {});

public:
    auto & getName() const { return name; }
    auto getUUID() const { return uuid; }
    auto & getQueryScheduler() { return *query_scheduler; }

    auto getReadLock() const { return ReadLock(state_mutex); }
    auto getWriteLock() const { return WriteLock(state_mutex); }

    VirtualWarehouseSettings getSettings() const
    {
        auto rlock = getReadLock();
        return settings;
    }

    auto getExpectedNumWorkers() const
    {
        auto rlock = getReadLock();
        return settings.num_workers;
    }

    void applySettings(const VirtualWarehouseAlterSettings & new_settings, const Catalog::CatalogPtr & catalog);

    VirtualWarehouseData getData() const;
    std::vector<WorkerGroupPtr> getAllWorkerGroups() const;
    std::vector<WorkerGroupPtr> getNonborrowedGroups() const;
    std::vector<WorkerGroupPtr> getBorrowedGroups() const;
    std::vector<WorkerGroupPtr> getLentGroups() const;

    /// Worker group operations
    size_t getNumGroups() const;

    void addWorkerGroup(const WorkerGroupPtr & group, const bool is_auto_linked = false);
    void loadGroup(const WorkerGroupPtr & group);
    void removeGroup(const String & id);

    // Lends a group via the auto-sharing feature
    void lendGroup(const String & group_id);
    // Unlends a group via the auto-sharing feature
    void unlendGroup(const String & group_id);

    size_t getNumBorrowedGroups() const;
    size_t getNumLentGroups() const;

    WorkerGroupPtr getWorkerGroup(const String & id);
    WorkerGroupPtr getWorkerGroup(const size_t & index);

    /// Worker Node operations
    void registerNode(const WorkerNodePtr & node);
    void registerNodes(const std::vector<WorkerNodePtr> & node);
    void removeNode(const String & worker_group_id, const String & worker_id);

    size_t getNumWorkers() const;

    void updateQueueInfo(const String & server_id, const QueryQueueInfo & server_query_queue_info);
    QueryQueueInfo getAggQueueInfo();

    size_t getLastBorrowTimestamp() const { return last_borrow_timestamp; }
    size_t getLastLendTimestamp() const { return last_lend_timestamp; }

    void setLastBorrowTimestamp(UInt64 last_borrow_timestamp_) { last_borrow_timestamp = last_borrow_timestamp_; }
    void setLastLendTimestamp(UInt64 last_lend_timestamp_) { last_lend_timestamp = last_lend_timestamp_; }

private:
    const WorkerGroupPtr & getWorkerGroupImpl(const String & id, ReadLock & rlock);
    const WorkerGroupPtr & getWorkerGroupExclusiveImpl(const String & id, WriteLock & wlock);

    void registerNodeImpl(const WorkerNodePtr & node, WriteLock & wlock);

    size_t getNumWorkersImpl(ReadLock & lock) const;

    size_t getNumBorrowedGroupsImpl(ReadLock & rlock) const;
    size_t getNumLentGroupsImpl(ReadLock & rlock) const;

    const WorkerGroupPtr & randomWorkerGroup() const;

    const String name;
    const UUID uuid;

    mutable std::shared_mutex state_mutex;

    mutable std::mutex queue_map_mutex;
    ServerQueryQueueMap server_query_queue_map;

    VirtualWarehouseSettings settings;

    std::map<String, WorkerGroupPtr> groups;
    std::unordered_set<String> borrowed_groups; // Set of group_ids
    std::unordered_map<String, size_t> lent_groups; // Map of group_id to lend count
    UInt64 last_borrow_timestamp{0};
    UInt64 last_lend_timestamp{0};

    std::unique_ptr<QueryScheduler> query_scheduler;

    void cleanupQueryQueueMap();
};

class VirtualWarehouseFactory
{
public:
    /// Creating VW by this factory method to ensure the query scheduler is prepared.
    static VirtualWarehousePtr create(String n, UUID u, const VirtualWarehouseSettings & s)
    {
        return VirtualWarehousePtr(new VirtualWarehouse(std::move(n), u, s));
    }
};

}
}
