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

#include <Core/BackgroundSchedulePool.h>
#include <boost/noncopyable.hpp>

namespace Poco
{
    class Logger;
}

namespace DB::ResourceManagement
{

class ResourceManagerController;
class VirtualWarehouse;
class IWorkerGroup;
using VirtualWarehousePtr = std::shared_ptr<VirtualWarehouse>;
using WorkerGroupPtr = std::shared_ptr<IWorkerGroup>;


/** This class is used to enable sharing of worker groups across VW.
  * This feature is only enabled if cnch_auto_enable_resource_sharing is configured to true.
  *
  */
class WorkerGroupResourceCoordinator : private boost::noncopyable
{
public:
    WorkerGroupResourceCoordinator(ResourceManagerController & rm_controller_);
    ~WorkerGroupResourceCoordinator();

    void start();

    void stop();

private:
    ResourceManagerController & rm_controller;

    Poco::Logger * log;

    BackgroundSchedulePool::TaskHolder background_task;

    UInt64 task_interval_ms;


    // Unlink ineligible worker groups linked by WorkerGroupResourceCoordinator.
    // Also updates last lend time for each linked worker group's parent VW
    void unlinkBusyAndOverlentGroups(const VirtualWarehousePtr & vw,
                                     std::lock_guard<std::mutex> * vw_lock,
                                     std::lock_guard<std::mutex> * wg_lock);
    void unlinkOverborrowedGroups(const VirtualWarehousePtr & vw,
                                  std::lock_guard<std::mutex> * vw_lock,
                                  std::lock_guard<std::mutex> * wg_lock);
    void unlinkIneligibleGroups(const std::unordered_map<String, VirtualWarehousePtr> & vws,
                                std::lock_guard<std::mutex> * vw_lock,
                                std::lock_guard<std::mutex> * wg_lock);

    void getEligibleGroups(std::unordered_map<String, VirtualWarehousePtr> & vws,
                            std::vector<VirtualWarehousePtr> & eligible_vw_borrowers,
                            std::vector<WorkerGroupPtr> & eligible_wg_lenders,
                            size_t & borrow_slots,
                            size_t & lend_slots);
    void linkEligibleGroups(std::unordered_map<String, VirtualWarehousePtr> & vws,
                            std::vector<VirtualWarehousePtr> & eligible_vw_borrowers,
                            std::vector<WorkerGroupPtr> & eligible_wg_lenders,
                            size_t & borrow_slots,
                            size_t & lend_slots,
                            std::lock_guard<std::mutex> * vw_lock,
                            std::lock_guard<std::mutex> * wg_lock);

    void run();

    static constexpr auto linked_group_infix = "_borrow_from_";

};

}
