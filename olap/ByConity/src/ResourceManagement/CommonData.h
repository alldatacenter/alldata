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

#include <memory>
#include <sstream>
#include <unordered_map>
#include <unordered_set>

#include <Common/HostWithPorts.h>
#include <Core/Types.h>
#include <Core/UUID.h>
#include <ResourceManagement/VirtualWarehouseType.h>
#include <ResourceManagement/VWScheduleAlgo.h>
#include <ResourceManagement/WorkerGroupType.h>

namespace DB
{
    class Context;
}

namespace DB::Protos
{
    class ResourceRequirement;
    class QueryQueueInfo;
    class VirtualWarehouseSettings;
    class VirtualWarehouseAlterSettings;
    class VirtualWarehouseData;
    class WorkerNodeData;
    class WorkerNodeResourceData;
    class WorkerGroupData;
    class WorkerGroupMetrics;
}

namespace DB::ResourceManagement
{

struct VirtualWarehouseSettings
{
    VirtualWarehouseType type{VirtualWarehouseType::Unknown};
    size_t num_workers{0}; /// per group
    size_t min_worker_groups{0};
    size_t max_worker_groups{0};
    size_t max_concurrent_queries{0};
    size_t max_queued_queries{0};
    size_t max_queued_waiting_ms{5000};
    size_t auto_suspend{0};
    size_t auto_resume{1};
    VWScheduleAlgo vw_schedule_algo{VWScheduleAlgo::Random};
    size_t max_auto_borrow_links{0};
    size_t max_auto_lend_links{0};
    size_t cpu_threshold_for_borrow{100};
    size_t mem_threshold_for_borrow{100};
    size_t cpu_threshold_for_lend{0};
    size_t mem_threshold_for_lend{0};
    size_t cpu_threshold_for_recall{100};
    size_t mem_threshold_for_recall{100};
    size_t cooldown_seconds_after_auto_link{300};
    size_t cooldown_seconds_after_auto_unlink{300};


    void fillProto(Protos::VirtualWarehouseSettings & pb_settings) const;
    void parseFromProto(const Protos::VirtualWarehouseSettings & pb_settings);

    static inline auto createFromProto(const Protos::VirtualWarehouseSettings & pb_settings)
    {
        VirtualWarehouseSettings vw_settings;
        vw_settings.parseFromProto(pb_settings);
        return vw_settings;
    }
};

struct VirtualWarehouseAlterSettings
{
    std::optional<VirtualWarehouseType> type;
    std::optional<size_t> num_workers; /// per group
    std::optional<size_t> min_worker_groups;
    std::optional<size_t> max_worker_groups;
    std::optional<size_t> max_concurrent_queries;
    std::optional<size_t> max_queued_queries;
    std::optional<size_t> max_queued_waiting_ms;
    std::optional<size_t> auto_suspend;
    std::optional<size_t> auto_resume;
    std::optional<VWScheduleAlgo> vw_schedule_algo;
    std::optional<size_t> max_auto_borrow_links;
    std::optional<size_t> max_auto_lend_links;
    std::optional<size_t> cpu_threshold_for_borrow;
    std::optional<size_t> mem_threshold_for_borrow;
    std::optional<size_t> cpu_threshold_for_lend;
    std::optional<size_t> mem_threshold_for_lend;
    std::optional<size_t> cpu_threshold_for_recall;
    std::optional<size_t> mem_threshold_for_recall;
    std::optional<size_t> cooldown_seconds_after_auto_link;
    std::optional<size_t> cooldown_seconds_after_auto_unlink;

    void fillProto(Protos::VirtualWarehouseAlterSettings & pb_settings) const;
    void parseFromProto(const Protos::VirtualWarehouseAlterSettings & pb_settings);

    static inline auto createFromProto(const Protos::VirtualWarehouseAlterSettings & pb_settings)
    {
        VirtualWarehouseAlterSettings vw_settings;
        vw_settings.parseFromProto(pb_settings);
        return vw_settings;
    }
};

struct VirtualWarehouseData
{
    /// constants
    std::string name;
    UUID uuid;

    VirtualWarehouseSettings settings;

    ///  runtime information
    size_t num_worker_groups{0};
    size_t num_workers{0};
    size_t running_query_count;
    size_t queued_query_count;
    size_t num_borrowed_worker_groups{0};
    size_t num_lent_worker_groups{0};
    uint64_t last_borrow_timestamp{0};
    uint64_t last_lend_timestamp{0};

    void fillProto(Protos::VirtualWarehouseData & pb_data) const;
    void parseFromProto(const Protos::VirtualWarehouseData & pb_data);
    static inline auto createFromProto(const Protos::VirtualWarehouseData & pb_data)
    {
        VirtualWarehouseData res;
        res.parseFromProto(pb_data);
        return res;
    }

    std::string serializeAsString() const;
    void parseFromString(const std::string & s);

    static inline auto createFromString(const std::string & s)
    {
        VirtualWarehouseData res;
        res.parseFromString(s);
        return res;
    }
};

struct WorkerNodeCatalogData
{
    std::string id;
    std::string worker_group_id;
    HostWithPorts host_ports;

    std::string serializeAsString() const;
    void parseFromString(const std::string & s);

    static inline auto createFromString(const std::string & s)
    {
        WorkerNodeCatalogData res;
        res.parseFromString(s);
        return res;
    }

    static WorkerNodeCatalogData createFromProto(const Protos::WorkerNodeData & worker_data);
};

enum class WorkerState
{
    Registering,
    Running,
    Stopped, /// We don't have a Stopping state as we regard the worker as stopped immediately when RM receive a unregister request.
};

struct WorkerNodeResourceData
{
    HostWithPorts host_ports;
    std::string id;
    std::string vw_name;
    std::string worker_group_id;

    double cpu_usage;
    double memory_usage;
    UInt64 memory_available;
    UInt64 disk_space;
    UInt32 query_num;

    UInt64 cpu_limit;
    UInt64 memory_limit;

    UInt32 register_time;
    UInt32 last_update_time;

    UInt64 reserved_memory_bytes;
    UInt32 reserved_cpu_cores;
    WorkerState state;

    std::string serializeAsString() const;
    void parseFromString(const std::string & s);

    static inline auto createFromString(const std::string & s)
    {
        WorkerNodeCatalogData res;
        res.parseFromString(s);
        return res;
    }

    void fillProto(Protos::WorkerNodeResourceData & resource_info) const;
    static WorkerNodeResourceData createFromProto(const Protos::WorkerNodeResourceData & resource_info);
    inline String toDebugString() const
    {
        std::stringstream ss;
        ss << "{ vw:" << vw_name
        << ", worker_group:" << worker_group_id
        << ", id:" << id
        << ", cpu_usage:" << cpu_usage
        << ", memory_usage:" << memory_usage
        << ", memory_available:" << memory_available
        << ", query_num:" << query_num
        << " }";
        return ss.str();
    }
};

/// Based on VWScheduleAlgo, we can pass some additional requirements.
/// We can queue the current query/task if there is no worker/wg satisfy the requirements.
/// Examples:
///  1. Require at least 20G disk space for a merge task.
///  2. Require about 5G memory for a insert query.
///  3. Require the avg. cpu of the worker/wg not higher than 80%.
struct ResourceRequirement
{
    size_t request_mem_bytes{0};
    size_t request_disk_bytes{0};
    uint32_t expected_workers{0};
    String worker_group{};
    uint32_t request_cpu_cores{0};
    double cpu_usage_max_threshold{0};
    uint32_t task_cold_startup_sec{0};
    std::unordered_set<String> blocklist;
    bool forbid_random_result{false};
    bool no_repeat{false};

    void fillProto(Protos::ResourceRequirement & proto) const;
    void parseFromProto(const Protos::ResourceRequirement & proto);

    static inline auto createFromProto(const Protos::ResourceRequirement & pb_data)
    {
        ResourceRequirement requirement{};
        requirement.parseFromProto(pb_data);
        return requirement;
    }

    inline String toDebugString() const
    {
        std::stringstream ss;
        ss << "{request_cpu_cores:" << request_cpu_cores
           << ", cpu_usage_max_threshold:" << cpu_usage_max_threshold
           << ", request_mem_bytes:" << request_mem_bytes
           << ", request_disk_bytes:" << request_disk_bytes
           << ", expected_workers:" << expected_workers
           << ", worker_group:" << worker_group
           << ", task_cold_startup_sec:" << task_cold_startup_sec
           << ", blocklist size: " << blocklist.size()
           << "}";
        return ss.str();
    }

};

/// Worker Group's aggregated metrics.
struct WorkerGroupMetrics
{
    String id;
    uint32_t num_workers; /// 0 means metrics (and the worker group) is unavailable.

    /// CPU state.
    double min_cpu_usage;
    double max_cpu_usage;
    double avg_cpu_usage;

    /// MEM state.
    double min_mem_usage;
    double max_mem_usage;
    double avg_mem_usage;
    uint64_t min_mem_available;

    /// Query State.
    uint32_t total_queries;

    WorkerGroupMetrics(const String & _id = "") : id(_id)
    {
        min_cpu_usage = std::numeric_limits<double>::max();
        min_mem_usage = std::numeric_limits<double>::max();
        min_mem_available = std::numeric_limits<uint64_t>::max();
    }

    void reset();
    void fillProto(Protos::WorkerGroupMetrics & proto) const;
    void parseFromProto(const Protos::WorkerGroupMetrics & proto);
    static inline auto createFromProto(const Protos::WorkerGroupMetrics & proto)
    {
        WorkerGroupMetrics metrics;
        metrics.parseFromProto(proto);
        return metrics;
    }

    bool available(const ResourceRequirement & requirement) const
    {
        /// Worker group is unavailable if there are not enough active workers.
        if (num_workers < requirement.expected_workers)
            return false;

        /// Worker group is unavailable if any worker's cpu usage is higher than the threshold.
        if (requirement.cpu_usage_max_threshold > 0.01 && max_cpu_usage > requirement.cpu_usage_max_threshold)
            return false;

        /// Worker group is unavailable if any worker's available memory space is less than request.
        if (requirement.request_mem_bytes && min_mem_available < requirement.request_mem_bytes)
            return false;

        if (requirement.blocklist.contains(id))
            return false;

        return true;
    }

    inline String toDebugString() const
    {
        std::stringstream ss;
        ss << id << ":"
            << max_cpu_usage << "|" << min_cpu_usage << "|" << avg_cpu_usage << "|"
            << max_mem_usage << "|" << min_mem_usage << "|" << avg_mem_usage << "|" << min_mem_available
            << "|" << total_queries;
        return ss.str();
    }
};

struct WorkerGroupData
{
    std::string id;
    WorkerGroupType type {WorkerGroupType::Physical};
    UUID vw_uuid {};
    std::string vw_name;

    std::string psm; /// For physical group, XXX: remove me
    std::string linked_id; /// For shared group

    std::vector<HostWithPorts> host_ports_vec;
    size_t num_workers{};

    /// For query scheduler
    WorkerGroupMetrics metrics;

    bool is_auto_linked {false};
    std::string linked_vw_name;

    std::string serializeAsString() const;
    void parseFromString(const std::string & s);

    void fillProto(Protos::WorkerGroupData & data, const bool with_host_ports, const bool with_metrics) const;
    void parseFromProto(const Protos::WorkerGroupData & data);

    static inline auto createFromProto(const Protos::WorkerGroupData & pb_data)
    {
        WorkerGroupData group_data;
        group_data.parseFromProto(pb_data);
        return group_data;
    }
};

// Struct used for synchronising Query Queue information with Resource Manager
struct QueryQueueInfo
{
    UInt32 queued_query_count {0};
    UInt32 running_query_count {0};
    UInt64 last_sync {0};

    void fillProto(Protos::QueryQueueInfo & data) const;
    void parseFromProto(const Protos::QueryQueueInfo & data);

    static inline auto createFromProto(const Protos::QueryQueueInfo & pb_data)
    {
        QueryQueueInfo group_data;
        group_data.parseFromProto(pb_data);
        return group_data;
    }
};

}

namespace DB
{
namespace RM = ResourceManagement;
using ServerQueryQueueMap = std::unordered_map<String, RM::QueryQueueInfo>;
using VWQueryQueueMap = std::unordered_map<String, RM::QueryQueueInfo>;
using AggQueryQueueMap = std::unordered_map<String, RM::QueryQueueInfo>;
using QueryQueueInfo = RM::QueryQueueInfo;
using VirtualWarehouseSettings = RM::VirtualWarehouseSettings;
using VirtualWarehouseAlterSettings = RM::VirtualWarehouseAlterSettings;
using VirtualWarehouseType = RM::VirtualWarehouseType;
using VirtualWarehouseData = RM::VirtualWarehouseData;
using WorkerNodeData = RM::WorkerNodeCatalogData;
using WorkerNodeResourceData = RM::WorkerNodeResourceData;
using WorkerGroupData = RM::WorkerGroupData;
using WorkerGroupType = RM::WorkerGroupType;
using ResourceRequirement = RM::ResourceRequirement;
using VWScheduleAlgo = RM::VWScheduleAlgo;
}
