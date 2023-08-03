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

#include <ResourceManagement/ResourceManagerController.h>

#include <Catalog/Catalog.h>
#include <Common/Configurations.h>
#include <Core/UUID.h>
#include <Interpreters/Context.h>
#include <Poco/Util/Application.h>
#include <ResourceManagement/ElectionController.h>
#include <ResourceManagement/ResourceTracker.h>
#include <ResourceManagement/VirtualWarehouseManager.h>
#include <ResourceManagement/WorkerGroupManager.h>
#include <ResourceManagement/WorkerGroupResourceCoordinator.h>

#include <ResourceManagement/ElectionController.h>
#include <ResourceManagement/VirtualWarehouseType.h>

namespace DB::ErrorCodes
{
    extern const int KNOWN_WORKER_GROUP;
    extern const int RESOURCE_MANAGER_ILLEGAL_CONFIG;
    extern const int RESOURCE_MANAGER_REMOVE_WORKER_ERROR;
    extern const int VIRTUAL_WAREHOUSE_NOT_INITIALIZED;
    extern const int WORKER_GROUP_NOT_FOUND;
}

namespace DB::ResourceManagement
{
ResourceManagerController::ResourceManagerController(ContextPtr global_context_)
    : WithContext(global_context_), log(&Poco::Logger::get("ResourceManagerController"))
{
    resource_tracker = std::make_unique<ResourceTracker>(*this);
    vw_manager = std::make_unique<VirtualWarehouseManager>(*this);
    group_manager = std::make_unique<WorkerGroupManager>(*this);
    wg_resource_coordinator = std::make_unique<WorkerGroupResourceCoordinator>(*this);
    election_controller = std::make_unique<ElectionController>(*this);
}

ResourceManagerController::~ResourceManagerController()
{
}

Catalog::CatalogPtr ResourceManagerController::getCnchCatalog()
{
    return getContext()->getCnchCatalog();
}

void ResourceManagerController::createVWsFromConfig()
{
    const auto & config = getContext()->getConfigRef();
    Poco::Util::AbstractConfiguration::Keys config_keys;

    String prefix = "resource_manager.vws";
    config.keys(prefix, config_keys);
    String prefix_key;

    for (const String & key: config_keys)
    {
        prefix_key = prefix + "." + key;
        if (key.find("vw") == 0)
        {
            if (!config.has(prefix_key + ".name"))
            {
                LOG_WARNING(log, "Virtual Warehouse specified in config without name");
                continue;
            }
            String name = config.getString(prefix_key + ".name");
            if (!config.has(prefix_key + ".type"))
            {
                LOG_WARNING(log, "Virtual Warehouse " + name + " specified in config without type");
                continue;
            }
            if (!config.has(prefix_key + ".num_workers"))
            {
                LOG_WARNING(log, "Virtual Warehouse " + name + " specified in config without num_workers");
                continue;
            }
            LOG_DEBUG(log, "Found virtual warehouse " + name + " in config");
            if (vw_manager->tryGetVirtualWarehouse(name))
            {
                LOG_DEBUG(log, "Virtual warehouse " + name + " already exists, skipping creation");
                if (config.has(prefix_key + ".worker_groups"))
                    createWorkerGroupsFromConfig(prefix_key + ".worker_groups", name);
                continue;
            }
            VirtualWarehouseSettings vw_settings;
            auto type_str = config.getString(prefix_key + ".type", "Unknown");
            vw_settings.type = ResourceManagement::toVirtualWarehouseType(&type_str[0]);
            if (vw_settings.type == VirtualWarehouseType::Unknown)
            {
                LOG_WARNING(log, "Unknown virtual warehouse type " + type_str + ". Type should be of Read, Write, Task or Default");
                continue;
            }

            vw_settings.num_workers = config.getInt(prefix_key + ".num_workers", 0);
            vw_settings.min_worker_groups = config.getInt(prefix_key + ".min_worker_groups", 0);
            vw_settings.max_worker_groups = config.getInt(prefix_key + ".max_worker_groups", 0);
            vw_settings.max_concurrent_queries = config.getInt(prefix_key + ".max_concurrent_queries", 0);
            vw_settings.auto_suspend = config.getInt(prefix_key + ".auto_suspend", 0);
            vw_settings.auto_resume = config.getInt(prefix_key + ".auto_resume", 1);

            vw_manager->createVirtualWarehouse(name, vw_settings, false);
            if (config.has(prefix_key + ".worker_groups"))
                createWorkerGroupsFromConfig(prefix_key + ".worker_groups", name);
            LOG_DEBUG(log, "Created virtual warehouse " + name + " using config");
        }
    }

}

void ResourceManagerController::createWorkerGroupsFromConfig(const String & prefix, const String & vw_name)
{
    Poco::Util::AbstractConfiguration::Keys config_keys;
    const auto & config = getContext()->getConfigRef();

    config.keys(prefix, config_keys);
    String prefix_key;

    auto vw_lock  = vw_manager->getLock();
    auto wg_lock = group_manager->getLock();

    for (const String & key: config_keys)
    {
        prefix_key = prefix + "." + key;
        if (key.find("worker_group") == 0)
        {
            if (!config.has(prefix_key + ".name"))
            {
                LOG_WARNING(log, "Worker Group specified in config without name");
                continue;
            }
            String name = config.getString(prefix_key + ".name");
            if (group_manager->tryGetWorkerGroupImpl(name, &vw_lock, &wg_lock))
            {
                LOG_DEBUG(log, "Worker group " + name + " already exists, skipping creation");
                continue;
            }
            if (!config.has(prefix_key + ".type"))
            {
                LOG_WARNING(log, "Worker Group " + name + " specified in config without type");
                continue;
            }
            WorkerGroupData group_data;
            group_data.id = name;
            auto type_str = config.getString(prefix_key + ".type", "Unknown");
            group_data.type = ResourceManagement::toWorkerGroupType(&type_str[0]);
            if (group_data.type == WorkerGroupType::Unknown)
            {
                LOG_WARNING(log, "Unknown worker group type " + type_str + ". Type should be of Physical, Shared or Composite");
                continue;
            }
            if (config.has(prefix_key + ".psm"))
                group_data.psm = config.getString(prefix_key + ".psm");
            if (config.has(prefix_key + ".shared_group"))
                group_data.linked_id = config.getString(prefix_key + ".shared_group");

            createWorkerGroup(name, false, vw_name, group_data, &vw_lock, &wg_lock);
            LOG_DEBUG(log, "Created worker group " + name + " in Virtual Warehouse " + vw_name + " using config");
        }
    }
}

void ResourceManagerController::initialize()
{
    vw_manager->loadVirtualWarehouses();
    group_manager->loadWorkerGroups();
    createVWsFromConfig();

    auto vws = vw_manager->getAllVirtualWarehouses();
    auto groups = group_manager->getAllWorkerGroups();

    std::unordered_map<UUID, VirtualWarehousePtr> uuid_to_vw;
    for (auto & [_, vw] : vws)
        uuid_to_vw[vw->getUUID()] = vw;

    // Link worker groups to respective VWs
    for (auto & [_, group] : groups)
    {
        auto vw_uuid = group->getVWUUID();

        if (vw_uuid == UUIDHelpers::Nil)
        {
            LOG_DEBUG(log, "Worker group {} doesn't belong to any virtual warehouse", group->getID());
            continue;
        }

        auto vw_it = uuid_to_vw.find(vw_uuid);
        if (vw_it == uuid_to_vw.end())
        {
            LOG_WARNING(log, "Worker group {} belongs to a nonexistent virtual warehouse {}", group->getID(), toString(vw_uuid));
            continue;
        }

        auto & vw = vw_it->second;
        if (auto shared_group = dynamic_pointer_cast<SharedWorkerGroup>(group); shared_group && shared_group->isAutoLinked())
        {
            vw->addWorkerGroup(group, true);
        }
        else
        {
            vw->addWorkerGroup(group);
        }
        group->setVWName(vw->getName());
        LOG_DEBUG(log, "Add worker group {} to virtual warehouse {}", group->getID(), vw->getName());
    }

    // Link lent groups to their original VWs
    for (auto & [_, group] : groups)
    {
        if (auto shared_group = dynamic_pointer_cast<SharedWorkerGroup>(group); shared_group && shared_group->isAutoLinked())
        {
            auto vw_uuid = group->getVWUUID();
            auto vw_it = uuid_to_vw.find(vw_uuid);
            if (vw_it == uuid_to_vw.end())
            {
                LOG_WARNING(log, "Worker group {} belongs to a nonexistent virtual warehouse {}", group->getID(), toString(vw_uuid));
                continue;
            }
            auto & vw = vw_it->second;
            auto source_vw_name = shared_group->tryGetLinkedGroupVWName();
            if (source_vw_name.empty())
            {
                LOG_DEBUG(log, "Auto-borrowed worker group {}'s physical group does not have a virtual warehouse", group->getID());
                continue;
            }
            if (auto source_vw_it = vws.find(source_vw_name); source_vw_it == vws.end())
            {
                LOG_WARNING(log, "Auto-borrowed worker group's physical group's VW {} does not exist", source_vw_name);
                continue;
            }
            else
            {
                LOG_DEBUG(log, "Linking WG {} from VW {} to VW {}", group->getID(), source_vw_name, vw->getName());
                auto data = shared_group->getData();
                auto source_group = group_manager->getWorkerGroup(data.linked_id);
                auto source_physical_group = dynamic_pointer_cast<PhysicalWorkerGroup>(source_group);
                if (!source_physical_group)
                    throw Exception("Lent group is not of Physical type", ErrorCodes::LOGICAL_ERROR);
                auto source_vw = source_vw_it->second;
                source_vw->lendGroup(data.linked_id);
                source_physical_group->addLentGroupDestID(group->getID());
            }
        }
    }
}

void ResourceManagerController::registerWorkerNode(const WorkerNodeResourceData & data)
{
    if (data.worker_group_id.empty())
        throw Exception("The group_id of node must not be empty", ErrorCodes::KNOWN_WORKER_GROUP);

    auto [outdated, worker_node] = resource_tracker->registerNode(data);

    const auto & vw_name = worker_node->vw_name;
    const auto & worker_id = worker_node->getID();
    const auto & worker_group_id = worker_node->worker_group_id;

    WorkerGroupPtr group;
    {
        auto vw_lock = vw_manager->getLock();
        auto wg_lock = group_manager->getLock();
        group = group_manager->tryGetWorkerGroupImpl(worker_group_id, &vw_lock, &wg_lock);
        if (!group)
        {
            auto vw = vw_manager->tryGetVirtualWarehouseImpl(vw_name, &vw_lock);
            if (!vw)
                throw Exception("Worker node's Virtual Warehouse `" + vw_name + "` has not been created", ErrorCodes::VIRTUAL_WAREHOUSE_NOT_INITIALIZED);

            // Create group if not found
            WorkerGroupData worker_group_data;
            worker_group_data.id = worker_group_id;
            worker_group_data.vw_name = vw_name;
            group = createWorkerGroup(worker_group_id, false, vw_name, worker_group_data, &vw_lock, &wg_lock);
            // vw->addWorkerGroup(group);
            // group->setVWName(vw->getName());
        }
    }

    if (outdated)
    {
        /// remove node from worker group first.
        LOG_WARNING(log, "Worker {} is outdated, Will remove it from WorkerGroup and ResourceTracker", worker_id);
        group->removeNode(worker_id);
        resource_tracker->removeNode(worker_id);

        /// re-register new node after removing outdated node.
        return registerWorkerNode(data);
    }
    else
    {
        group->registerNode(worker_node);
        LOG_DEBUG(log, "Node {} registered successfully in worker group {}", worker_node->getID(), group->getID());
    }

    worker_node->assigned = true;
}

void ResourceManagerController::removeWorkerNode(const std::string & worker_id, const std::string & vw_name, const std::string & group_id)
{
    if (group_id.empty() || vw_name.empty())
        throw Exception("The vw_name and group_id must not be empty.", ErrorCodes::RESOURCE_MANAGER_REMOVE_WORKER_ERROR);

    auto vw_lock = vw_manager->getLock();
    auto wg_lock = group_manager->getLock();
    auto group = group_manager->tryGetWorkerGroupImpl(group_id, &vw_lock, &wg_lock);
    if (!group)
        throw Exception("The worker group " + group_id + " not exists!", ErrorCodes::RESOURCE_MANAGER_REMOVE_WORKER_ERROR);
    group->removeNode(worker_id);
}

WorkerGroupPtr ResourceManagerController::createWorkerGroup(
    const std::string & group_id, bool if_not_exists, const std::string & vw_name, WorkerGroupData data, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    std::unique_ptr<std::lock_guard<std::mutex>> vw_lock_guard;
    std::unique_ptr<std::lock_guard<std::mutex>> wg_lock_guard;

    if ((wg_lock != nullptr) ^ (vw_lock != nullptr))
        throw Exception("vw_lock and wg_lock must both be set or left empty", ErrorCodes::LOGICAL_ERROR);
    else if (wg_lock == nullptr && vw_lock == nullptr)
    {
        // VirtualWarehouseManager's lock should be obtained before WorkerGroupManager's
        vw_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(vw_manager->getMutex());
        wg_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(group_manager->getMutex());
        vw_lock = vw_lock_guard.get();
        wg_lock = wg_lock_guard.get();
    }

    VirtualWarehousePtr source_vw;
    WorkerGroupPtr source_group;
    auto is_auto_linked = data.is_auto_linked;

    if (is_auto_linked)
    {
        source_group = group_manager->getWorkerGroupImpl(data.linked_id, vw_lock, wg_lock);
        auto source_physical_group = dynamic_pointer_cast<PhysicalWorkerGroup>(source_group);
        if (!source_physical_group)
            throw Exception("Lent group is not of Physical type", ErrorCodes::LOGICAL_ERROR);
        source_vw = vw_manager->getVirtualWarehouseImpl(source_group->getVWName(), vw_lock);
        LOG_DEBUG(log, "Linking " + source_group->getID() + " from " + source_vw->getName() + " as " + group_id + " to " + vw_name);
        source_vw->lendGroup(data.linked_id);
        source_physical_group->addLentGroupDestID(group_id);
    }

    auto dest_group = group_manager->createWorkerGroupImpl(group_id, if_not_exists, vw_name, data, vw_lock, wg_lock);
    auto vw = vw_manager->getVirtualWarehouseImpl(vw_name, vw_lock);
    vw->addWorkerGroup(dest_group, is_auto_linked);
    dest_group->setVWName(vw->getName());

    return dest_group;
}

void ResourceManagerController::dropWorkerGroup(const std::string & group_id, bool if_exists, std::lock_guard<std::mutex> * vw_lock, std::lock_guard<std::mutex> * wg_lock)
{
    std::unique_ptr<std::lock_guard<std::mutex>> vw_lock_guard;
    std::unique_ptr<std::lock_guard<std::mutex>> wg_lock_guard;

    if ((wg_lock != nullptr) ^ (vw_lock != nullptr))
        throw Exception("vw_lock and wg_lock must both be set or left empty", ErrorCodes::LOGICAL_ERROR);
    else if (wg_lock == nullptr && vw_lock == nullptr)
    {
        // VirtualWarehouseManager's lock should be obtained before WorkerGroupManager's
        vw_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(vw_manager->getMutex());
        wg_lock_guard = std::make_unique<std::lock_guard<std::mutex>>(group_manager->getMutex());
        vw_lock = vw_lock_guard.get();
        wg_lock = wg_lock_guard.get();
    }

    auto group = group_manager->tryGetWorkerGroupImpl(group_id, vw_lock, wg_lock);
    if (!group)
    {
        if (if_exists)
            return;
        else
            throw Exception("Worker group `" + group_id + "` not found", ErrorCodes::WORKER_GROUP_NOT_FOUND);
    }

    auto vw_name = group->getVWName();

    if (!vw_name.empty())
    {
        auto vw = vw_manager->getVirtualWarehouseImpl(vw_name, vw_lock);
        if (auto shared_group = dynamic_pointer_cast<SharedWorkerGroup>(group); shared_group && shared_group->isAutoLinked())
        {
            // Unlend worker_group
            try
            {
                auto lender_vw_name = shared_group->tryGetLinkedGroupVWName();
                auto lender_vw = vw_manager->tryGetVirtualWarehouseImpl(lender_vw_name, vw_lock);
                if (lender_vw)
                {
                    auto time_now = time(nullptr);
                    lender_vw->unlendGroup(shared_group->getData().linked_id);
                    lender_vw->setLastLendTimestamp(time_now);
                }
                else
                {
                    LOG_DEBUG(log, "Auto linked WG's physical group's virtual warehouse no longer exists");
                }
                auto lender_group = shared_group->getLinkedGroup();
                auto lender_physical_group = dynamic_pointer_cast<PhysicalWorkerGroup>(lender_group);
                if (lender_physical_group)
                {
                    lender_physical_group->removeLentGroupDestID(group_id);
                }
                else
                {
                    LOG_DEBUG(log, "Auto linked WG's physical group no longer exists");
                }
            }
            catch (...)
            {
                tryLogCurrentException(log);
            }
        }
        else if (auto physical_group = dynamic_pointer_cast<PhysicalWorkerGroup>(group); physical_group)
        {
            // Drop auto linked groups by resource coordinator
            auto lent_groups_ids = physical_group->getLentGroupsDestIDs();
            for (const auto & lent_groups_id : lent_groups_ids)
            {
                dropWorkerGroup(lent_groups_id, true, vw_lock, wg_lock);
            }
        }

        vw->removeGroup(group_id);
    }
    group_manager->dropWorkerGroupImpl(group_id, wg_lock);
}

}
