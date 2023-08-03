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

#include <ResourceManagement/VirtualWarehouse.h>
#include <Common/ConcurrentMapForCreating.h>

#include <common/logger_useful.h>
#include <ResourceManagement/CommonData.h>

namespace DB::ResourceManagement
{
class ResourceManagerController;

class VirtualWarehouseManager : protected ConcurrentMapForCreating<std::string, VirtualWarehouse>, private boost::noncopyable
{
public:
    VirtualWarehouseManager(ResourceManagerController & rm_controller_);

    void loadVirtualWarehouses();

    VirtualWarehousePtr createVirtualWarehouse(const std::string & name, const VirtualWarehouseSettings & settings, const bool if_not_exists);
    VirtualWarehousePtr tryGetVirtualWarehouse(const std::string & name);
    VirtualWarehousePtr getVirtualWarehouse(const std::string & name);
    void alterVirtualWarehouse(const std::string & name, const VirtualWarehouseAlterSettings & settings);
    void dropVirtualWarehouse(const std::string & name, const bool if_exists);

    std::unordered_map<String, VirtualWarehousePtr> getAllVirtualWarehouses();

    void clearVirtualWarehouses();

    void updateQueryQueueMap(const String & server_id, const VWQueryQueueMap & vw_query_queue_map, std::vector<String> & deleted_vw_list);

    AggQueryQueueMap getAggQueryQueueMap() const;

private:
    ResourceManagerController & rm_controller;
    Poco::Logger * log{nullptr};
    mutable std::mutex vw_mgr_mutex;

    auto & getMutex() const
    {
        return vw_mgr_mutex;
    }

    auto getLock() const
    {
        return std::lock_guard<std::mutex>(vw_mgr_mutex);
    }

    void loadVirtualWarehousesImpl(std::lock_guard<std::mutex> * vw_lock);

    VirtualWarehousePtr createVirtualWarehouseImpl(const std::string & name, const VirtualWarehouseSettings & settings, const bool if_not_exists, std::lock_guard<std::mutex> * vw_lock);
    VirtualWarehousePtr tryGetVirtualWarehouseImpl(const std::string & name, std::lock_guard<std::mutex> * vw_lock);
    VirtualWarehousePtr getVirtualWarehouseImpl(const std::string & name, std::lock_guard<std::mutex> * vw_lock);
    void alterVirtualWarehouseImpl(const std::string & name, const VirtualWarehouseAlterSettings & settings, std::lock_guard<std::mutex> * vw_lock);
    void dropVirtualWarehouseImpl(const std::string & name, const bool if_exists, std::lock_guard<std::mutex> * vw_lock);

    std::unordered_map<String, VirtualWarehousePtr> getAllVirtualWarehousesImpl(std::lock_guard<std::mutex> * vw_lock);

    void clearVirtualWarehousesImpl(std::lock_guard<std::mutex> * vw_lock);

    std::atomic_bool need_sync_with_catalog{false};

    friend class ResourceManagerController;
    friend class WorkerGroupManager;
    friend class WorkerGroupResourceCoordinator;
};
}
