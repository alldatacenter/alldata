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

#include <ResourceManagement/WorkerGroupManager.h>

#include <Catalog/Catalog.h>
#include <ResourceManagement/PhysicalWorkerGroup.h>
#include <ResourceManagement/ResourceManagerController.h>
#include <ResourceManagement/SharedWorkerGroup.h>
#include <ResourceManagement/VirtualWarehouseManager.h>
#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    const extern int LOGICAL_ERROR;
    const extern int BRPC_TIMEOUT;
    const extern int WORKER_GROUP_NOT_FOUND;
    const extern int WORKER_GROUP_ALREADY_EXISTS;
}
}

namespace DB::ResourceManagement
{
WorkerGroupManager::WorkerGroupManager(ResourceManagerController & rm_controller_)
    : rm_controller(rm_controller_), log(&Poco::Logger::get("WorkerGroupManager"))
{
}

void WorkerGroupManager::loadWorkerGroups()
{
    auto wg_lock = getLock();
    loadWorkerGroupsImpl(&wg_lock);
}

void WorkerGroupManager::loadWorkerGroupsImpl(std::lock_guard<std::mutex> * /*wg_lock*/)
{
    auto catalog = rm_controller.getCnchCatalog();

    LOG_DEBUG(log, "Loading worker groups...");

    auto data_vec = catalog->scanWorkerGroups();

    std::lock_guard cells_lock(cells_mutex);

    for (auto & data : data_vec)
    {
        if (data.type != WorkerGroupType::Physical)
            continue;

        try
        {
            cells.try_emplace(data.id, createWorkerGroupObject(data, &cells_lock));
            LOG_DEBUG(log, "Loaded physical worker group {}", data.id);
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }
    }

    for (auto & data : data_vec)
    {
        if (data.type != WorkerGroupType::Shared)
            continue;

        try
        {
            cells.try_emplace(data.id, createWorkerGroupObject(data, &cells_lock));
            LOG_DEBUG(log, "Loaded shared worker group {}", data.id);
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }
    }

    LOG_INFO(log, "Loaded {} worker groups.", cells.size());
}

void WorkerGroupManager::clearWorkerGroups()
{
    auto wg_lock = getLock();
    clearWorkerGroupsImpl(&wg_lock);
}

void WorkerGroupManager::clearWorkerGroupsImpl(std::lock_guard<std::mutex> * /*wg_lock*/)
{
    std::lock_guard cells_lock(cells_mutex);
    cells.clear();
}

WorkerGroupPtr WorkerGroupManager::createWorkerGroupObject(const WorkerGroupData & data, std::lock_guard<std::mutex> * wg_lock)
{
    switch (data.type)
    {
        case WorkerGroupType::Physical:
            return std::make_shared<PhysicalWorkerGroup>(rm_controller.getContext(), data.id, data.vw_uuid, data.psm);

        case WorkerGroupType::Shared: {
            std::optional<std::lock_guard<std::mutex>> maybe_lock;
            if (!wg_lock)
            {
                maybe_lock.emplace(cells_mutex);
                wg_lock = &maybe_lock.value();
            }

            auto res = std::make_shared<SharedWorkerGroup>(data.id, data.vw_uuid, data.linked_id, data.is_auto_linked);

            if (auto linked_group = tryGetImpl(data.linked_id, *wg_lock))
                res->setLinkedGroup(linked_group);

            return res;
        }

        default:
            return nullptr;
    }
}

WorkerGroupPtr WorkerGroupManager::tryGetWorkerGroup(const std::string & id, std::lock_guard<std::mutex> * vw_lock)
{
    std::unique_ptr<std::lock_guard<std::mutex>> vw_lock_guard;
    if (!vw_lock)
    {
        vw_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(rm_controller.getVirtualWarehouseManager().getMutex());
        vw_lock = vw_lock_guard.get();
    }
    auto wg_lock = getLock();
    return tryGetWorkerGroupImpl(id, vw_lock, &wg_lock);
}

WorkerGroupPtr WorkerGroupManager::tryGetWorkerGroupImpl(const std::string & id, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    auto res = tryGet(id);
    if (!res && need_sync_with_catalog.load(std::memory_order_relaxed))
    {
        auto catalog = rm_controller.getCnchCatalog();

        WorkerGroupData data;
        if (!catalog->tryGetWorkerGroup(id, data))
            return nullptr;

        res = rm_controller.createWorkerGroup(id, false, data.vw_name, data, vw_lock, wg_lock);
    }
    return res;
}

std::unordered_map<String, WorkerGroupPtr> WorkerGroupManager::getAllWorkerGroups()
{
    auto wg_lock = getLock();
    return getAllWorkerGroupsImpl(&wg_lock);
}

std::unordered_map<String, WorkerGroupPtr> WorkerGroupManager::getAllWorkerGroupsImpl(std::lock_guard<std::mutex> * /*wg_lock*/)
{
    return getAll();
}


WorkerGroupPtr WorkerGroupManager::getWorkerGroup(const std::string & id, std::lock_guard<std::mutex> * vw_lock)
{
    std::unique_ptr<std::lock_guard<std::mutex>> vw_lock_guard;
    if (!vw_lock)
    {
        vw_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(rm_controller.getVirtualWarehouseManager().getMutex());
        vw_lock = vw_lock_guard.get();
    }
    auto wg_lock = getLock();
    return getWorkerGroupImpl(id, vw_lock, &wg_lock);
}

WorkerGroupPtr WorkerGroupManager::getWorkerGroupImpl(const std::string & id, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    auto res = tryGetWorkerGroupImpl(id, vw_lock, wg_lock);
    if (!res)
        throw Exception("Worker group `" + id + "` not found", ErrorCodes::WORKER_GROUP_NOT_FOUND);
    return res;
}

WorkerGroupPtr WorkerGroupManager::createWorkerGroup(
    const std::string & id, bool if_not_exists, const std::string & vw_name, WorkerGroupData data, std::lock_guard<std::mutex> * vw_lock)
{
    std::unique_ptr<std::lock_guard<std::mutex>> vw_lock_guard;
    if (!vw_lock)
    {
        vw_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(rm_controller.getVirtualWarehouseManager().getMutex());
        vw_lock = vw_lock_guard.get();
    }
    auto wg_lock = getLock();
    return createWorkerGroupImpl(id, if_not_exists, vw_name, data, vw_lock, &wg_lock);
}

WorkerGroupPtr WorkerGroupManager::createWorkerGroupImpl(
    const std::string & id, bool if_not_exists, const std::string & vw_name, WorkerGroupData data, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * /*wg_lock*/)
{
    if (!vw_lock)
        throw Exception("Virtual warehouse lock should have been obtained", ErrorCodes::LOGICAL_ERROR);

    auto catalog = rm_controller.getCnchCatalog();

    if (!vw_name.empty())
    {
        auto vw = rm_controller.getVirtualWarehouseManager().getVirtualWarehouseImpl(vw_name, vw_lock);
        data.vw_uuid = vw->getUUID();
    }

    auto creator = [&] {
        try
        {
            if (data.type == WorkerGroupType::Shared)
            {
                if (const auto & linked = tryGet(data.linked_id);
                    !linked || linked->getType() == WorkerGroupType::Shared)
                    throw Exception("Create shared worker group with error! linked_id: " + data.linked_id, ErrorCodes::LOGICAL_ERROR);
            }
            catalog->createWorkerGroup(id, data);
            LOG_DEBUG(log, "Created worker group {} in catalog", id);
        }
        catch (const Exception & e)
        {
            if (e.code() == ErrorCodes::BRPC_TIMEOUT)
                need_sync_with_catalog.store(true, std::memory_order_relaxed);
            throw;
        }

        return createWorkerGroupObject(data);
    };

    auto [group, created] = getOrCreate(id, std::move(creator));
    if (!if_not_exists && !created)
        throw Exception("Worker group " + id + " already exists.", ErrorCodes::WORKER_GROUP_ALREADY_EXISTS);

    return group;
}

void WorkerGroupManager::dropWorkerGroup(const std::string & id)
{
    auto wg_lock = getLock();
    dropWorkerGroupImpl(id, &wg_lock);
}

void WorkerGroupManager::dropWorkerGroupImpl(const std::string & id, std::lock_guard<std::mutex> * /*lock*/)
{
    auto catalog = rm_controller.getCnchCatalog();
    try
    {
        catalog->dropWorkerGroup(id);
    }
    catch (const Exception & e)
    {
        if (e.code() == ErrorCodes::BRPC_TIMEOUT)
            need_sync_with_catalog.store(true, std::memory_order_relaxed);

        throw;
    }

    erase(id);
}
}
