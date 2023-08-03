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
#include <ResourceManagement/VirtualWarehouse.h>

#include <chrono>

namespace DB::ErrorCodes
{
    extern const int RESOURCE_MANAGER_ERROR;
    extern const int RESOURCE_MANAGER_INCORRECT_SETTING;
    extern const int RESOURCE_MANAGER_NO_AVAILABLE_WORKER_GROUP;
}

namespace DB::ResourceManagement
{

VirtualWarehouse::VirtualWarehouse(String n, UUID u, const VirtualWarehouseSettings & s)
    : name(std::move(n)), uuid(u), settings(s)
{
    query_scheduler = std::make_unique<QueryScheduler>(*this);
}

void VirtualWarehouse::applySettings(const VirtualWarehouseAlterSettings & setting_changes, const Catalog::CatalogPtr & catalog)
{
    auto data = getData();
    VirtualWarehouseSettings & new_settings = data.settings;

    if (setting_changes.type)
        new_settings.type = *setting_changes.type;
    if (setting_changes.max_worker_groups)
    {
        if ((setting_changes.min_worker_groups
                && setting_changes.min_worker_groups.value() > setting_changes.max_worker_groups.value())
            || (!setting_changes.min_worker_groups && settings.min_worker_groups > setting_changes.max_worker_groups.value()))
            throw Exception("min_worker_groups cannot be less than max_worker_groups", ErrorCodes::RESOURCE_MANAGER_INCORRECT_SETTING);
        new_settings.max_worker_groups = *setting_changes.max_worker_groups;
    }
    if (setting_changes.min_worker_groups)
    {
        new_settings.min_worker_groups = *setting_changes.min_worker_groups;
    }
    if (setting_changes.num_workers)
    {
        new_settings.num_workers = *setting_changes.num_workers;
    }
    if (setting_changes.auto_suspend)
        new_settings.auto_suspend = *setting_changes.auto_suspend;
    if (setting_changes.auto_resume)
        new_settings.auto_resume = *setting_changes.auto_resume;
    if (setting_changes.max_concurrent_queries)
        new_settings.max_concurrent_queries = *setting_changes.max_concurrent_queries;
    if (setting_changes.max_queued_queries)
        new_settings.max_queued_queries = *setting_changes.max_queued_queries;
    if (setting_changes.max_queued_waiting_ms)
        new_settings.max_queued_waiting_ms = *setting_changes.max_queued_waiting_ms;
    if (setting_changes.vw_schedule_algo)
        new_settings.vw_schedule_algo = *setting_changes.vw_schedule_algo;
    if (setting_changes.max_auto_borrow_links)
        new_settings.max_auto_borrow_links = *setting_changes.max_auto_borrow_links;
    if (setting_changes.max_auto_lend_links)
        new_settings.max_auto_lend_links = *setting_changes.max_auto_lend_links;
    if (setting_changes.cpu_threshold_for_borrow)
        new_settings.cpu_threshold_for_borrow = *setting_changes.cpu_threshold_for_borrow;
    if (setting_changes.mem_threshold_for_borrow)
        new_settings.mem_threshold_for_borrow = *setting_changes.mem_threshold_for_borrow;
    if (setting_changes.cpu_threshold_for_lend)
        new_settings.cpu_threshold_for_lend = *setting_changes.cpu_threshold_for_lend;
    if (setting_changes.mem_threshold_for_lend)
        new_settings.mem_threshold_for_lend = *setting_changes.mem_threshold_for_lend;
    if (setting_changes.cpu_threshold_for_recall)
        new_settings.cpu_threshold_for_recall = *setting_changes.cpu_threshold_for_recall;
    if (setting_changes.mem_threshold_for_recall)
        new_settings.mem_threshold_for_recall = *setting_changes.mem_threshold_for_recall;
    if (setting_changes.cooldown_seconds_after_auto_link)
        new_settings.cooldown_seconds_after_auto_link = *setting_changes.cooldown_seconds_after_auto_link;
    if (setting_changes.cooldown_seconds_after_auto_unlink)
        new_settings.cooldown_seconds_after_auto_unlink = *setting_changes.cooldown_seconds_after_auto_unlink;

    catalog->alterVirtualWarehouse(name, data);
    {
        auto wlock = getWriteLock();
        settings = new_settings;
    }
}

VirtualWarehouseData VirtualWarehouse::getData() const
{
    VirtualWarehouseData data;
    data.name = name;
    data.uuid = uuid;

    auto rlock = getReadLock();
    data.settings = settings;
    data.num_worker_groups = groups.size();
    data.num_workers = getNumWorkersImpl(rlock);
    data.num_borrowed_worker_groups = getNumBorrowedGroupsImpl(rlock);
    data.num_lent_worker_groups = getNumLentGroupsImpl(rlock);
    data.last_borrow_timestamp = getLastBorrowTimestamp();
    data.last_lend_timestamp = getLastLendTimestamp();

    return data;
}

std::vector<WorkerGroupPtr> VirtualWarehouse::getAllWorkerGroups() const
{
    std::vector<WorkerGroupPtr> res;

    auto rlock = getReadLock();

    for (const auto & [_, group]: groups)
        res.emplace_back(group);

    return res;
}

std::vector<WorkerGroupPtr> VirtualWarehouse::getNonborrowedGroups() const
{
    std::vector<WorkerGroupPtr> res;

    auto rlock = getReadLock();

    for (const auto & [_, group]: groups)
    {
        if (borrowed_groups.find(group->getID()) == borrowed_groups.end())
            res.emplace_back(group);
    }

    return res;
}

std::vector<WorkerGroupPtr> VirtualWarehouse::getBorrowedGroups() const
{
    std::vector<WorkerGroupPtr> res;

    auto rlock = getReadLock();

    for (const auto & id : borrowed_groups)
    {
        if (auto it = groups.find(id); it == groups.end())
            throw Exception("Borrowed worker group " + id + "cannot be found in VW " + getName(), ErrorCodes::LOGICAL_ERROR);
        else
            res.emplace_back(it->second);
    }

    return res;
}

std::vector<WorkerGroupPtr> VirtualWarehouse::getLentGroups() const
{
    std::vector<WorkerGroupPtr> res;

    auto rlock = getReadLock();

    for (const auto & [id, _] : lent_groups)
    {
        if (auto it = groups.find(id); it == groups.end())
            throw Exception("Lent worker group " + id + "cannot be found in VW " + getName(), ErrorCodes::LOGICAL_ERROR);
        else
            res.emplace_back(it->second);
    }

    return res;
}

size_t VirtualWarehouse::getNumGroups() const
{
    auto rlock = getReadLock();
    return groups.size();
}

void VirtualWarehouse::addWorkerGroup(const WorkerGroupPtr & group, const bool is_auto_linked /* = false */)
{
    auto wlock = getWriteLock();
    auto res = groups.try_emplace(group->getID(), group).second;
    if (!res)
        throw Exception("Worker group " + group->getID() + " already exists in VW " + getName(), ErrorCodes::LOGICAL_ERROR);

    if (is_auto_linked)
    {
        auto id = group->getID();
        borrowed_groups.insert(id);
    }
}

void VirtualWarehouse::loadGroup(const WorkerGroupPtr & group)
{
    addWorkerGroup(group);
}

void VirtualWarehouse::lendGroup(const String & id)
{
   auto wlock = getWriteLock();

   // Update lend count of group
   auto it = lent_groups.find(id);

   if (it != lent_groups.end())
   {
       it->second += 1;
   }
   else
   {
       lent_groups[id] = 1;
   }
}

void VirtualWarehouse::unlendGroup(const String & id)
{
   auto wlock = getWriteLock();
   auto it = lent_groups.find(id);
   if (it == lent_groups.end())
   {
       throw Exception("Lent group " + id + " does not exist in VW " + name, ErrorCodes::LOGICAL_ERROR);
   }
   else
   {
       if (it->second == 1)
           lent_groups.erase(it); // Erase group from lent groups if this is the last link
       else
           it->second -= 1;
   }
}

size_t VirtualWarehouse::getNumBorrowedGroups() const
{
    auto rlock = getReadLock();
    return getNumBorrowedGroupsImpl(rlock);
}

size_t VirtualWarehouse::getNumBorrowedGroupsImpl(ReadLock & /*rlock*/) const
{
    return borrowed_groups.size();
}

size_t VirtualWarehouse::getNumLentGroups() const
{
    auto rlock = getReadLock();
    return getNumLentGroupsImpl(rlock);
}

size_t VirtualWarehouse::getNumLentGroupsImpl(ReadLock & /*rlock*/) const
{
    auto res = 0;
    for (const auto & [_, count] : lent_groups)
    {
        res += count;
    }

    return res;
}

void VirtualWarehouse::removeGroup(const String & id)
{
    auto wlock = getWriteLock();

    borrowed_groups.erase(id);

    size_t num = groups.erase(id);
    if (num == 0)
        throw Exception("Cannot remove a nonexistent worker group " + id + " in VW " + getName(), ErrorCodes::LOGICAL_ERROR);
}

WorkerGroupPtr VirtualWarehouse::getWorkerGroup(const String & id)
{
    auto rlock = getReadLock();
    return getWorkerGroupImpl(id, rlock);
}

WorkerGroupPtr VirtualWarehouse::getWorkerGroup(const size_t & index)
{
    auto rlock = getReadLock();
    return std::next(groups.begin(), index)->second;
}

const WorkerGroupPtr & VirtualWarehouse::getWorkerGroupImpl(const String & id, ReadLock & /*rlock*/)
{
    auto it = groups.find(id);
    if (it == groups.end())
        throw Exception("Worker group " + id + " not found in VW " + getName(), ErrorCodes::LOGICAL_ERROR);
    return it->second;
}

const WorkerGroupPtr & VirtualWarehouse::getWorkerGroupExclusiveImpl(const String & id, WriteLock & /*wlock*/)
{
    auto it = groups.find(id);
    if (it == groups.end())
        throw Exception("Worker group " + id + " not found in VW " + getName(), ErrorCodes::LOGICAL_ERROR);
    return it->second;
}

void VirtualWarehouse::registerNodeImpl(const WorkerNodePtr & node, WriteLock & wlock)
{
    if (node->worker_group_id.empty())
        throw Exception("Group ID cannot be empty", ErrorCodes::RESOURCE_MANAGER_ERROR);
    auto & group = getWorkerGroupExclusiveImpl(node->worker_group_id, wlock);
    group->registerNode(node);
}

void VirtualWarehouse::registerNode(const WorkerNodePtr & node)
{
    auto wlock = getWriteLock();
    registerNodeImpl(node, wlock);
}

void VirtualWarehouse::registerNodes(const std::vector<WorkerNodePtr> & nodes)
{
    auto wlock = getWriteLock();
    for (const auto & node : nodes)
        registerNodeImpl(node, wlock);
}

void VirtualWarehouse::removeNode(const String & worker_group_id, const String & worker_id)
{
    auto wlock = getWriteLock();
    auto & group = getWorkerGroupExclusiveImpl(worker_group_id, wlock);
    group->removeNode(worker_id);
}

/// The final fallback strategy for picking a worker group from this vw.
const WorkerGroupPtr & VirtualWarehouse::randomWorkerGroup() const
{
    std::uniform_int_distribution dist;

    {
        auto rlock = getReadLock();
        if (auto size = groups.size())
        {
            auto begin = dist(thread_local_rng) % size;
            for (size_t i = 0; i < size; i++)
            {
                auto index = (begin + i) % size;
                auto & group = std::next(groups.begin(), index)->second;
                if (!group->empty())
                    return group;
            }
        }
    }

    throw Exception("No available worker group for " + name, ErrorCodes::RESOURCE_MANAGER_NO_AVAILABLE_WORKER_GROUP);
}

size_t VirtualWarehouse::getNumWorkersImpl(ReadLock & /*rlock*/) const
{
    size_t res{0};
    for (auto & [_, group] : groups)
        res += group->getNumWorkers();
    return res;
}

size_t VirtualWarehouse::getNumWorkers() const
{
    auto rlock = getReadLock();
    return getNumWorkersImpl(rlock);
}

void VirtualWarehouse::updateQueueInfo(const String & server_id, const QueryQueueInfo & server_query_queue_info)
{
    std::lock_guard lock(queue_map_mutex);

    server_query_queue_map[server_id] = server_query_queue_info;
}

QueryQueueInfo VirtualWarehouse::getAggQueueInfo()
{
    std::lock_guard lock(queue_map_mutex);

    UInt64 time_now = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now()
                                                                            .time_since_epoch()).count();
    // TODO: Use setting vw_queue_server_sync_expiry_seconds
    auto timeout = 15;
    auto timeout_threshold = time_now - timeout * 1000;

    QueryQueueInfo res;

    auto it = server_query_queue_map.begin();

    while (it != server_query_queue_map.end())
    {
        //Remove outdated entries
        if (it->second.last_sync < timeout_threshold)
        {
            LOG_DEBUG(&Poco::Logger::get("VirtualWarehouse"),
                        "Removing outdated server sync from {}, last synced {}",
                        it->first, std::to_string(it->second.last_sync));
            it = server_query_queue_map.erase(it);
        }
        else
        {
            res.queued_query_count += it->second.queued_query_count;
            res.running_query_count += it->second.running_query_count;
            ++it;
        }
    }

    return res;
}

}
