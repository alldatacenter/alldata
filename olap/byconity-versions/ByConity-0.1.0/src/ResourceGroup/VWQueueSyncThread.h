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

#include <thread>
#include <Core/BackgroundSchedulePool.h>
#include <Core/Types.h>
#include <common/logger_useful.h>


namespace DB
{

namespace ResourceManagement
{
    struct VirtualWarehouseData;
}

class IResourceGroup;
class VWResourceGroupManager;

using ResourceGroupPtr = std::shared_ptr<IResourceGroup>;
using VirtualWarehouseData = ResourceManagement::VirtualWarehouseData;


/** Server-side thread that synchronises server query queue info with Resource Manager
  * Retrieves total query queue info snapshot, and performs resource group deletion for unused groups
  */
class VWQueueSyncThread : protected WithContext
{
public:
    VWQueueSyncThread(UInt64 interval_, ContextPtr global_context_);
    ~VWQueueSyncThread();

    void start()
    {
        task->activateAndSchedule();
    }

    void stop()
    {
        task->deactivate();
    }

    UInt64 getLastSyncTime() const
    {
        return last_sync_time.load();
    }

private:
    using ResourceGroupDataPair = std::pair<VirtualWarehouseData, ResourceGroupPtr>;
    bool syncQueueDetails(VWResourceGroupManager * vw_resource_group_manager);
    bool syncResourceGroups(VWResourceGroupManager * vw_resource_group_manager);
    void run();

    UInt64 interval; /// in seconds;
    std::atomic<UInt64> last_sync_time{0};

    BackgroundSchedulePool::TaskHolder task;

    Poco::Logger * log;
};

}
