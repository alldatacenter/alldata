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

#include <Common/Exception.h>
#include <CloudServices/CnchWorkerClient.h>
#include <Interpreters/Context_fwd.h>
#include <ResourceManagement/CommonData.h>
#include <ResourceManagement/VWScheduleAlgo.h>

#include <atomic>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <vector>
#include <boost/noncopyable.hpp>

namespace Poco
{
class Logger;
}

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

namespace DB
{
class Context;
class WorkerGroupHandleImpl;
using WorkerGroupHandle = std::shared_ptr<WorkerGroupHandleImpl>;

enum class VirtualWarehouseHandleSource
{
    RM,
    PSM,
};

constexpr auto toString(VirtualWarehouseHandleSource s)
{
    if (s == VirtualWarehouseHandleSource::RM)
        return "RM";
    else if (s == VirtualWarehouseHandleSource::PSM)
        return "PSM";
    throw Exception("Unknown VW source", ErrorCodes::LOGICAL_ERROR);
}

/**
 * NOTE:
 * 1. VirtualWarehouseHandleImpl is a mutable class which is protected by mutex.
 * 2. There is only one VirtualWarehouseHandle object in server for a unique VW (identified by uuid).
 * 3. The content in handle might be outdated because the handle would be only updated periodically.
 * 4. After updating, the outdated WorkerGroupHandle(s) will be replaced by new ones.
 */
class VirtualWarehouseHandleImpl : protected WithContext, private boost::noncopyable
{
private:
    friend class VirtualWarehousePool; /// could only be created by VirtualWarehousePool

    VirtualWarehouseHandleImpl(
        VirtualWarehouseHandleSource source,
        String name,
        UUID uuid,
        const ContextPtr global_context_,
        const VirtualWarehouseSettings & settings = {});

    VirtualWarehouseHandleImpl(VirtualWarehouseHandleSource source, const VirtualWarehouseData & vw_data, const ContextPtr global_context_);

public:
    using Container = std::unordered_map<String, WorkerGroupHandle>;
    using VirtualWarehouseHandle = std::shared_ptr<VirtualWarehouseHandleImpl>;
    using CnchWorkerClientPtr = std::shared_ptr<CnchWorkerClient>;
    using VWScheduleAlgo = ResourceManagement::VWScheduleAlgo;
    using Requirement = ResourceManagement::ResourceRequirement;
    using WorkerGroupMetrics = ResourceManagement::WorkerGroupMetrics;

    enum UpdateMode
    {
        NoUpdate,
        TryUpdate,
        ForceUpdate,
    };

    auto getSource() const { return source; }
    auto & getName() const { return name; }
    auto getUUID() const { return uuid; }
    auto & getSettingsRef() const { return settings; }

    size_t empty(UpdateMode mode = NoUpdate);
    Container getAll(UpdateMode mode = NoUpdate);

    WorkerGroupHandle getWorkerGroup(const String & worker_group_id, UpdateMode mode = TryUpdate);
    WorkerGroupHandle pickWorkerGroup(VWScheduleAlgo query_algo, const Requirement & requirement = {}, UpdateMode mode = TryUpdate);
    WorkerGroupHandle pickLocally(const VWScheduleAlgo & algo, const Requirement & requirement = {});
    WorkerGroupHandle randomWorkerGroup() const;
    std::optional<HostWithPorts> tryPickWorkerFromRM(VWScheduleAlgo algo, const Requirement & requirement = {});

    bool addWorkerGroup(const WorkerGroupHandle & worker_group);

    CnchWorkerClientPtr getWorker();
    CnchWorkerClientPtr getWorkerByHash(const String & key);
    std::vector<CnchWorkerClientPtr> getAllWorkers();

private:
    bool addWorkerGroupImpl(const WorkerGroupHandle & worker_group, const std::lock_guard<std::mutex> & lock);

    void tryUpdateWorkerGroups(UpdateMode mode);
    bool updateWorkerGroupsFromRM();
    bool updateWorkerGroupsFromPSM();

    using WorkerGroupAndMetrics = std::pair<WorkerGroupHandle, WorkerGroupMetrics>;
    void filterGroup(const Requirement & requirement, std::vector<WorkerGroupAndMetrics> & out_available_groups);
    WorkerGroupHandle selectGroup(const VWScheduleAlgo & algo, std::vector<WorkerGroupAndMetrics> & available_groups);

    static constexpr auto PSM_WORKER_GROUP_SUFFIX = "_psm";

    const VirtualWarehouseHandleSource source;
    const String name;
    const UUID uuid;
    VirtualWarehouseSettings settings;
    Poco::Logger * log;

    std::atomic<UInt64> last_update_time_ns{0};

    mutable std::mutex state_mutex;
    Container worker_groups;
    std::atomic<size_t> pick_group_sequence = 0; /// round-robin index for pickWorkerGroup.
};

using VirtualWarehouseHandle = std::shared_ptr<VirtualWarehouseHandleImpl>;

}
