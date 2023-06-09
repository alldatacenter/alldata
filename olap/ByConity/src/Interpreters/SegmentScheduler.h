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

#include <unordered_set>
#include <Core/Block.h>
#include <Core/Types.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/CancellationCode.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/Context.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentExecutor.h>
#include <Parsers/IAST_fwd.h>
#include <Processors/Exchange/DataTrans/ConcurrentShardMap.h>
#include <Protos/plan_segment_manager.pb.h>
#include <brpc/controller.h>
#include <bthread/condition_variable.h>
#include <bthread/mutex.h>
#include <Common/Stopwatch.h>
#include <algorithm>
#include <random>

#ifndef NDEBUG
#    define TASK_ASSIGN_DEBUG
#endif

namespace DB
{

struct PlanSegmentsStatus
{
    //TODO dongyifeng add when PlanSegmentInfo is merged
    volatile bool is_final_stage_start = false;
    std::atomic<bool> is_cancel{false};
    String exception;
};

struct ExceptionWithCode
{
    ExceptionWithCode(const String & exception_, int code_) : exception(exception_), code(code_) { }
    String exception;
    int code;
};

using PlanSegmentsStatusPtr = std::shared_ptr<PlanSegmentsStatus>;
using RuntimeSegmentsStatusPtr = std::shared_ptr<RuntimeSegmentsStatus>;
using PlanSegmentsPtr = std::vector<PlanSegmentPtr>;
using Source = std::vector<size_t>;
// <query_id, <segment_id, status>>
using SegmentStatusMap = std::map<String, std::map<size_t, RuntimeSegmentsStatusPtr>>;

struct DAGGraph {
    DAGGraph(){}
    DAGGraph(const DAGGraph & other)
    {
        std::unique_lock lock(other.status_mutex);
        sources = other.sources;
        final = other.final;
        scheduler_segments = std::move(other.scheduler_segments);
        id_to_segment = std::move(other.id_to_segment);
        id_to_address = std::move(other.id_to_address);
        plan_segment_status_ptr = std::move(other.plan_segment_status_ptr);
        local_exchange_ids = other.local_exchange_ids;
    }
    Source sources;
    size_t final = std::numeric_limits<size_t>::max();
    std::set<size_t> scheduler_segments;
    std::unordered_map<size_t, PlanSegment *> id_to_segment;
    std::unordered_map<size_t, AddressInfos> id_to_address;
    std::set<AddressInfo> plan_send_addresses;
    PlanSegmentsStatusPtr plan_segment_status_ptr;
    bool has_set_local_exchange = false;
    size_t local_exchange_parallel_size=0;
    std::set<size_t> local_exchange_ids;
    AddressInfos first_local_exchange_address;
#if defined(TASK_ASSIGN_DEBUG)
    std::unordered_map<size_t, std::vector<std::pair<size_t, AddressInfo>>> exchange_data_assign_node_mappings;
#endif
    mutable bthread::Mutex status_mutex;
};

using DAGGraphPtr = std::shared_ptr<DAGGraph>;

class SegmentScheduler
{
public:
    SegmentScheduler(): log(&Poco::Logger::get("SegmentScheduler")) {}
    virtual ~SegmentScheduler() {}
    PlanSegmentsStatusPtr insertPlanSegments(const String & query_id,
                                             PlanSegmentTree * plan_segments_ptr,
                                             ContextPtr query_context);

    CancellationCode cancelPlanSegmentsFromCoordinator(const String query_id, const String & exception, ContextPtr query_context);
    CancellationCode cancelPlanSegments(
        const String & query_id,
        const String & exception,
        const String & origin_host_name,
        ContextPtr query_context,
        std::shared_ptr<DAGGraph> dag_graph_ptr = nullptr);

    void cancelWorkerPlanSegments(const String & query_id, const DAGGraphPtr dag_ptr, ContextPtr query_context);

    bool finishPlanSegments(const String & query_id);

    AddressInfos getWorkerAddress(const String & query_id, size_t segment_id);

    String getCurrentDispatchStatus(const String & query_id);
    void updateSegmentStatus(const RuntimeSegmentsStatus & segment_status);
    void updateException(const String & query_id, const String & exception, int code);
    ExceptionWithCode getException(const String & query_id, size_t timeout_ms);

private:
    std::unordered_map<String, std::shared_ptr<DAGGraph>> query_map;
    mutable bthread::Mutex mutex;
    mutable bthread::Mutex segment_status_mutex;
    mutable SegmentStatusMap segment_status_map;
    // record exception when exception occurred
    ConcurrentShardMap<String, ExceptionWithCode> query_to_exception_with_code;
    Poco::Logger * log;

    void buildDAGGraph(PlanSegmentTree * plan_segments_ptr, std::shared_ptr<DAGGraph> graph);
    bool scheduler(const String & query_id, ContextPtr query_context, std::shared_ptr<DAGGraph> dag_graph);

protected:
    virtual AddressInfos sendPlanSegment(PlanSegment * plan_segment_ptr, bool is_source, ContextPtr query_context, std::shared_ptr<DAGGraph> dag_graph, std::vector<size_t> random_worker_ids);
};

using SegmentSchedulerPtr = std::shared_ptr<SegmentScheduler>;

}
