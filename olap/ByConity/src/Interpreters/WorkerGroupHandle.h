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

#include <Interpreters/Cluster.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/IAST_fwd.h>
#include <ResourceManagement/WorkerGroupType.h>
#include <ResourceManagement/VWScheduleAlgo.h>
#include <Common/ConsistentHashUtils/ConsistentHashRing.h>

#include <ResourceManagement/CommonData.h>
#include <boost/noncopyable.hpp>

namespace DB
{

class CnchWorkerClient;
using CnchWorkerClientPtr = std::shared_ptr<CnchWorkerClient>;

using WorkerGroupData = ResourceManagement::WorkerGroupData;
using WorkerGroupMetrics = ResourceManagement::WorkerGroupMetrics;
using VWScheduleAlgo = ResourceManagement::VWScheduleAlgo;

class Context;

enum class WorkerGroupHandleSource
{
    RM,
    PSM,
    TEMP,
};

constexpr auto toString(WorkerGroupHandleSource s)
{
    if (s == WorkerGroupHandleSource::RM)
        return "RM";
    else if (s == WorkerGroupHandleSource::PSM)
        return "PSM";
    else if (s == WorkerGroupHandleSource::TEMP)
        return "TEMP";
    return "Unknown";
}


class WorkerGroupHandleImpl;
using WorkerGroupHandle = std::shared_ptr<WorkerGroupHandleImpl>;

/**
 *  WorkerGroupHandleImpl is an immutable class
 *
 */

class WorkerGroupHandleImpl : private boost::noncopyable, public WithContext
{

public:
    using Address = Cluster::Address;
    using Addresses = Cluster::Addresses;
    using ShardInfo = Cluster::ShardInfo;
    using ShardsInfo = Cluster::ShardsInfo;

    using WorkerGroupType = ResourceManagement::WorkerGroupType;

    WorkerGroupHandleImpl(String id_, WorkerGroupHandleSource source_, String vw_name_, HostWithPortsVec hosts_, const ContextPtr & context_, const WorkerGroupMetrics & metrics_ = {});
    WorkerGroupHandleImpl(const WorkerGroupData & worker_group_data, const ContextPtr & context_);
    WorkerGroupHandleImpl(const WorkerGroupHandleImpl & from, const std::vector<size_t> & indices);

    auto & getID() const { return id; }
    auto getType() const { return type; }
    auto getSource() const { return source; }
    auto & getVWName() const { return vw_name; }
    /// Logging friendly
    auto getQualifiedName() const { return backQuoteIfNeed(vw_name) + '.' + backQuoteIfNeed(id); }
    auto & getHostWithPortsVec() const { return hosts; }
    auto empty() const { return hosts.empty(); }

    /// TODO: (zuochuang.zema) consider reduce the size of WorkerGroupMetrics.
    auto getMetrics() const
    {
        std::lock_guard lock(state_mutex);
        return metrics;
    }

    void setMetrics(const WorkerGroupMetrics & metrics_)
    {
        std::lock_guard lock(state_mutex);
        metrics = metrics_;
    }

    const auto & getWorkerClients() const { return worker_clients; }
    const ShardsInfo & getShardsInfo() const { return shards_info; }

    CnchWorkerClientPtr getWorkerClient() const;
    CnchWorkerClientPtr getWorkerClientByHash(const String & key) const;
    CnchWorkerClientPtr getWorkerClient(const HostWithPorts & host_ports) const;

    std::optional<size_t> indexOf(const HostWithPorts & host_ports) const
    {
        for (size_t i = 0, size = hosts.size(); i < size; i++)
        {
            if (host_ports.isSameEndpoint(hosts[i]))
                return i;
        }
        return {};
    }

    bool isSame(const WorkerGroupData & worker_group_data) const;

    // Clones a worker group with workers at specified indices removed
    WorkerGroupHandle cloneAndRemoveShards(const std::vector<uint64_t> & remove_marks) const;

    Strings getWorkerTCPAddresses(const Settings & settings) const;
    Strings getWorkerIDVec() const;
    std::vector<std::pair<String, UInt16>> getReadWorkers() const;

    bool hasRing() const { return ring != nullptr; }
    const DB::ConsistentHashRing & getRing() const { return *ring; }

private:
    /// Note: updating mutable fields (like `metrics`) should be guarded with lock.
    mutable std::mutex state_mutex;

    String id;
    WorkerGroupType type{WorkerGroupType::Unknown};
    WorkerGroupHandleSource source{};
    String vw_name;
    HostWithPortsVec hosts;
    WorkerGroupMetrics metrics;

    std::vector<CnchWorkerClientPtr> worker_clients;

    /// Description of the cluster shards.
    ShardsInfo shards_info;

    /// Hash Map for part allocation
    std::unique_ptr<DB::ConsistentHashRing> ring;

    static std::unique_ptr<DB::ConsistentHashRing> buildRing(const ShardsInfo & shards_info, const ContextPtr global_context);
};

}
