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

#include <memory>
#include <set>
#include <time.h>
#include <Client/Connection.h>
#include <CloudServices/CnchServerResource.h>
#include <IO/ConnectionTimeoutsContext.h>
#include <IO/MemoryReadWriteBuffer.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentManagerRpcService.h>
#include <Interpreters/DistributedStages/executePlanSegment.h>
#include <Interpreters/SegmentScheduler.h>
#include <Parsers/queryToString.h>
#include <Processors/Exchange/DataTrans/RpcClient.h>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>
#include <Protos/plan_segment_manager.pb.h>
#include <Storages/StorageReplicatedMergeTree.h>
#include <Common/HostWithPorts.h>
#include <Common/Macros.h>
#include <Common/ProfileEvents.h>
#include <common/getFQDNOrHostName.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int UNEXPECTED_PACKET_FROM_SERVER;
    extern const int UNKNOWN_PACKET_FROM_SERVER;
    extern const int QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING;
    extern const int UNKNOWN_EXCEPTION;
}

AddressInfo getLocalAddress(ContextPtr & query_context)
{
    const auto & host = getIPOrFQDNOrHostName();
    auto port = query_context->getTCPPort();
    const ClientInfo & info = query_context->getClientInfo();
    return AddressInfo(
        host, port, info.current_user, info.current_password, query_context->getExchangePort(), query_context->getExchangeStatusPort());
}

AddressInfo getRemoteAddress(HostWithPorts host_with_ports, ContextPtr & query_context)
{
    const ClientInfo & info = query_context->getClientInfo();
    return AddressInfo(
        host_with_ports.getHost(), host_with_ports.tcp_port, info.current_user, info.current_password, host_with_ports.exchange_port, host_with_ports.exchange_status_port);
}

PlanSegmentsStatusPtr
SegmentScheduler::insertPlanSegments(const String & query_id, PlanSegmentTree * plan_segments_ptr, ContextPtr query_context)
{
    std::shared_ptr<DAGGraph> dag_ptr = std::make_shared<DAGGraph>();
    buildDAGGraph(plan_segments_ptr, dag_ptr);
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        if (query_map.find(query_id) != query_map.end())
        {
            // cancel running query
            if (query_context->getSettingsRef().replace_running_query)
            {
                //TODO support replace running query
                throw Exception("Query with id = " + query_id + " is already running and replace_running_query is not supported now.", ErrorCodes::QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);
            }
            else
                throw Exception("Query with id = " + query_id + " is already running.", ErrorCodes::QUERY_WITH_SAME_ID_IS_ALREADY_RUNNING);
        }
        query_map.emplace(std::make_pair(query_id, dag_ptr));
    }
    /// send resource to worker before scheduler

    auto server_resource = query_context->tryGetCnchServerResource();
    if (server_resource)
    {
        /// TODO: we can skip some worker
        server_resource->sendResource(query_context);
    }

    scheduler(query_id, query_context, dag_ptr);
#if defined(TASK_ASSIGN_DEBUG)
    String res;
    res += "dump statics:" + std::to_string(dag_ptr->exchange_data_assign_node_mappings.size()) + "\n";
    for (auto it = dag_ptr->exchange_data_assign_node_mappings.begin(); it != dag_ptr->exchange_data_assign_node_mappings.end(); it++)
    {
        res += "segment id: " + std::to_string(it->first);
        for (size_t j = 0; j < it->second.size(); j++)
        {
            res += "\n  index:" + std::to_string(it->second[j].first) + " address:" + it->second[j].second.getHostName() + "_"
                + std::to_string(it->second[j].second.getPort());
        }
        res += "\n";
    }
    LOG_DEBUG(log, res);

#endif
    return dag_ptr->plan_segment_status_ptr;
}


CancellationCode
SegmentScheduler::cancelPlanSegmentsFromCoordinator(const String query_id, const String & exception, ContextPtr query_context)
{
    const String & coordinator_host = getIPOrFQDNOrHostName();
    return cancelPlanSegments(query_id, exception, coordinator_host, query_context);
}

CancellationCode SegmentScheduler::cancelPlanSegments(
    const String & query_id, const String & exception, const String & origin_host_name, ContextPtr query_context, std::shared_ptr<DAGGraph> dag_graph_ptr)
{
    std::shared_ptr<DAGGraph> dag_ptr;

    if (dag_graph_ptr == nullptr) // try to get the dag_graph_ptr
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        auto query_map_ite = query_map.find(query_id);
        if (query_map_ite == query_map.end())
            return CancellationCode::NotFound;
        dag_ptr = query_map_ite->second;
    }
    else
    {
        dag_ptr = dag_graph_ptr;
    }

    {
        {
            std::unique_lock<bthread::Mutex> lock(dag_ptr->status_mutex);
            LOG_ERROR(
                log,
                "query(" + query_id + ") receive error from host:" + origin_host_name + " with exception:" + exception
                    + " and plan_send_addresses size:" + std::to_string(dag_ptr->plan_send_addresses.size()));

            if (dag_ptr->plan_segment_status_ptr->is_cancel.load(std::memory_order_relaxed))
                return CancellationCode::CancelSent;
            dag_ptr->plan_segment_status_ptr->is_cancel.store(true, std::memory_order_relaxed);
            dag_ptr->plan_segment_status_ptr->exception
                = "query(" + query_id + ") receive exception from host-" + origin_host_name + " with exception:" + exception;
        }

        /// send cancel query rpc request to all executor except exception original executor
        cancelWorkerPlanSegments(query_id, dag_ptr, query_context);
    }
    return CancellationCode::CancelSent;
}

void SegmentScheduler::cancelWorkerPlanSegments(const String & query_id, const DAGGraphPtr dag_ptr, ContextPtr query_context)
{
    String coordinator_addr = getIPOrFQDNOrHostName() + ":" + std::to_string(query_context->getExchangeStatusPort());
    //TODO: cancel worker in parallel
    for (const auto & addr : dag_ptr->plan_send_addresses)
    {
        auto address = extractExchangeStatusHostPort(addr);
        std::shared_ptr<RpcClient> rpc_client = RpcChannelPool::getInstance().getClient(address, BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY, true);
        Protos::PlanSegmentManagerService_Stub manager(&rpc_client->getChannel());
        brpc::Controller cntl;
        Protos::CancelQueryRequest request;
        Protos::CancelQueryResponse response;
        request.set_query_id(query_id);
        request.set_coordinator_address(coordinator_addr);
        manager.cancelQuery(&cntl, &request, &response, nullptr);
        rpc_client->assertController(cntl);
        LOG_INFO(log, "Cancel plan segment query_id-{} on host-{}, ret_code-{}", query_id, extractExchangeHostPort(addr), response.ret_code());
    }
}

bool SegmentScheduler::finishPlanSegments(const String & query_id)
{
    std::unique_lock<bthread::Mutex> lock(segment_status_mutex);
    auto query_map_ite = query_map.find(query_id);
    if (query_map_ite != query_map.end())
    {
        query_map.erase(query_map_ite);
    }

    auto seg_status_map_ite = segment_status_map.find(query_id);
    if (seg_status_map_ite != segment_status_map.end())
        segment_status_map.erase(seg_status_map_ite);

    query_to_exception_with_code.remove(query_id);
    return true;
}

AddressInfos SegmentScheduler::getWorkerAddress(const String & query_id, size_t segment_id)
{
    std::unique_lock<bthread::Mutex> lock(mutex);
    auto query_map_ite = query_map.find(query_id);
    if (query_map_ite == query_map.end())
        return {};
    std::shared_ptr<DAGGraph> dag_ptr = query_map_ite->second;
    if (dag_ptr->id_to_address.count(segment_id))
        return dag_ptr->id_to_address[segment_id];
    else
        return {};
}

String SegmentScheduler::getCurrentDispatchStatus(const String & query_id)
{
    std::unique_lock<bthread::Mutex> lock(mutex);
    auto query_map_ite = query_map.find(query_id);
    if (query_map_ite == query_map.end())
        return "query_id-" + query_id + " is not exist in scheduler query map";

    std::shared_ptr<DAGGraph> dag_ptr = query_map_ite->second;
    String status("(segment_id, worker_size): ");
    for (const auto & ip_address : dag_ptr->id_to_address)
        status += "(" + std::to_string(ip_address.first) + "," + std::to_string(ip_address.second.size()) + "), ";
    return status;
}

void SegmentScheduler::updateSegmentStatus(const RuntimeSegmentsStatus & segment_status)
{
    std::unique_lock<bthread::Mutex> lock(segment_status_mutex);
    auto query_iter = segment_status_map.find(segment_status.query_id);
    if (query_iter == segment_status_map.end())
        segment_status_map[segment_status.query_id] = {};

    auto segment_iter = segment_status_map[segment_status.query_id].find(segment_status.segment_id);
    if (segment_iter == segment_status_map[segment_status.query_id].end())
        segment_status_map[segment_status.query_id][segment_status.segment_id] = std::make_shared<RuntimeSegmentsStatus>();

    RuntimeSegmentsStatusPtr status = segment_status_map[segment_status.query_id][segment_status.segment_id];
    status->query_id = segment_status.query_id;
    status->segment_id = segment_status.segment_id;
    status->is_succeed = segment_status.is_succeed;
    status->is_canceled = segment_status.is_canceled;
    status->message = segment_status.message;
    status->code = segment_status.code;
}

void SegmentScheduler::updateException(const String & query_id, const String & exception, int code)
{
    std::unique_lock<bthread::Mutex> lock(mutex);
    // if query map can not find query_id means query has already finished
    if (query_map.count(query_id))
    {
        if (query_to_exception_with_code.exist(query_id))
        {
            const auto ptr = query_to_exception_with_code.get(query_id, 10);
            if (ptr)
            {
                const auto & new_exception = ptr->exception + ":" + exception;
                query_to_exception_with_code.put(query_id, std::make_shared<ExceptionWithCode>(new_exception, code));
            }
            else
                query_to_exception_with_code.put(query_id, std::make_shared<ExceptionWithCode>(exception, code));

        }
        else
        {
            query_to_exception_with_code.put(query_id, std::make_shared<ExceptionWithCode>(exception, code));
        }
    }
}

ExceptionWithCode SegmentScheduler::getException(const String & query_id, size_t timeout_ms) {
    const auto ptr = query_to_exception_with_code.get(query_id, timeout_ms);
    if (ptr)
        return *ptr;
    else
        return {"Unknown", ErrorCodes::UNKNOWN_EXCEPTION};
}

void SegmentScheduler::buildDAGGraph(PlanSegmentTree * plan_segments_ptr, std::shared_ptr<DAGGraph> graph_ptr)
{
    graph_ptr->plan_segment_status_ptr = std::make_shared<PlanSegmentsStatus>();
    PlanSegmentTree::Nodes & nodes = plan_segments_ptr->getNodes();

    // use to traversal the tree
    std::stack<PlanSegmentTree::Node *> plan_segment_stack;
    std::vector<PlanSegment *> plan_segment_vector;
    std::set<size_t> plan_segment_vector_id_list;
    for (PlanSegmentTree::Node & node : nodes)
    {
        plan_segment_stack.emplace(&node);
        if (plan_segment_vector_id_list.find(node.getPlanSegment()->getPlanSegmentId()) == plan_segment_vector_id_list.end())
        {
            plan_segment_vector.emplace_back(node.getPlanSegment());
            plan_segment_vector_id_list.emplace(node.getPlanSegment()->getPlanSegmentId());
        }
    }
    while (!plan_segment_stack.empty())
    {
        PlanSegmentTree::Node * node_ptr = plan_segment_stack.top();
        plan_segment_stack.pop();
        for (PlanSegmentTree::Node * node : node_ptr->children)
        {
            plan_segment_stack.emplace(node);
            if (plan_segment_vector_id_list.find(node->getPlanSegment()->getPlanSegmentId()) == plan_segment_vector_id_list.end())
            {
                plan_segment_vector.emplace_back(node->getPlanSegment());
                plan_segment_vector_id_list.emplace(node->getPlanSegment()->getPlanSegmentId());
            }
        }
    }

    for (PlanSegment * plan_segment_ptr : plan_segment_vector)
    {
        graph_ptr->id_to_segment.emplace(std::make_pair(plan_segment_ptr->getPlanSegmentId(), plan_segment_ptr));
        // value, readnothing, system table
        if (plan_segment_ptr->getPlanSegmentInputs().empty())
        {
            graph_ptr->sources.emplace_back(plan_segment_ptr->getPlanSegmentId());
        }
        // source
        if (plan_segment_ptr->getPlanSegmentInputs().size() >= 1)
        {
            bool all_tables = true;
            for (const auto & input : plan_segment_ptr->getPlanSegmentInputs())
            {
                if (input->getPlanSegmentType() != PlanSegmentType::SOURCE)
                {
                    all_tables = false;
                    break;
                }
            }
            if (all_tables)
                graph_ptr->sources.emplace_back(plan_segment_ptr->getPlanSegmentId());
        }
        // final stage
        if (plan_segment_ptr->getPlanSegmentOutput()->getPlanSegmentType() == PlanSegmentType::OUTPUT)
        {
            if (graph_ptr->final != std::numeric_limits<size_t>::max())
            {
                throw Exception("Logical error: PlanSegments should be only one final stage", ErrorCodes::LOGICAL_ERROR);
            }
            else
            {
                graph_ptr->final = plan_segment_ptr->getPlanSegmentId();
            }
        }
    }
    // set exchangeParallelSize for plan inputs
    for (PlanSegment * plan_segment_ptr : plan_segment_vector)
    {
        for (const auto & input : plan_segment_ptr->getPlanSegmentInputs())
        {
            if (input->getPlanSegmentType() == PlanSegmentType::EXCHANGE)
            {
                if (graph_ptr->id_to_segment.find(input->getPlanSegmentId()) == graph_ptr->id_to_segment.end())
                    throw Exception(
                        "Logical error: can't find the segment which id is " + std::to_string(input->getPlanSegmentId()),
                        ErrorCodes::LOGICAL_ERROR);
                PlanSegment * input_plan_segment_ptr = graph_ptr->id_to_segment.find(input->getPlanSegmentId())->second;
                input->setExchangeParallelSize(input_plan_segment_ptr->getExchangeParallelSize());
            }
        }
    }
    // do some check
    // 1. check source or final is empty
    if (graph_ptr->sources.empty())
        throw Exception("Logical error: source is empty", ErrorCodes::LOGICAL_ERROR);
    if (graph_ptr->final == std::numeric_limits<size_t>::max())
        throw Exception("Logical error: final is empty", ErrorCodes::LOGICAL_ERROR);

    // 2. check the parallel size
    for (auto it = graph_ptr->id_to_segment.begin(); it != graph_ptr->id_to_segment.end(); it++)
    {
        if (!it->second->getPlanSegmentInputs().empty())
        {
            for (auto plan_segment_input_ptr : it->second->getPlanSegmentInputs())
            {
                // only check when input is from an another exchange
                if (plan_segment_input_ptr->getPlanSegmentType() != PlanSegmentType::EXCHANGE)
                    continue;
                size_t input_plan_segment_id = plan_segment_input_ptr->getPlanSegmentId();
                if (graph_ptr->id_to_segment.find(input_plan_segment_id) == graph_ptr->id_to_segment.end())
                    throw Exception(
                        "Logical error: can't find the segment which id is " + std::to_string(input_plan_segment_id),
                        ErrorCodes::LOGICAL_ERROR);
                auto & input_plan_segment_ptr = graph_ptr->id_to_segment.find(input_plan_segment_id)->second;
                auto plan_segment_output = input_plan_segment_ptr->getPlanSegmentOutput();
                {
                    // if stage out is write to local:
                    // 1.the left table for broadcast join
                    // 2.the left table or right table for local join
                    // the next stage parallel size must be the same
                    if (plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_NO_NEED_REPARTITION
                        || plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_MAY_NEED_REPARTITION)
                    {
                        if (input_plan_segment_ptr->getParallelSize() != it->second->getParallelSize()
                            || (graph_ptr->local_exchange_parallel_size != 0
                                && (graph_ptr->local_exchange_parallel_size != input_plan_segment_ptr->getParallelSize())))
                            throw Exception(
                                "Logical error: the parallel size between local stage is different, input id:"
                                    + std::to_string(input_plan_segment_id)
                                    + " current id:" + std::to_string(it->second->getPlanSegmentId()),
                                ErrorCodes::LOGICAL_ERROR);
                        // set output parallel size to 1, no need to shuffle
                        if (graph_ptr->local_exchange_parallel_size == 0)
                            graph_ptr->local_exchange_parallel_size = input_plan_segment_ptr->getParallelSize();
                        plan_segment_output->setParallelSize(1);
                        graph_ptr->local_exchange_ids.emplace(input_plan_segment_id);
                        graph_ptr->local_exchange_ids.emplace(it->second->getPlanSegmentId());
                    }
                    else
                    {
                        // if stage out is shuffle, the output parallel size must be equal to next stage parallel size
                        if (plan_segment_output->getParallelSize() != it->second->getParallelSize())
                        {
                            throw Exception(
                                "Logical error: the parallel size between stage is different, input id:"
                                    + std::to_string(input_plan_segment_id)
                                    + " current id:" + std::to_string(it->second->getPlanSegmentId()),
                                ErrorCodes::LOGICAL_ERROR);
                        }
                    }
                }
            }
        }
    }
}

bool SegmentScheduler::scheduler(const String & query_id, ContextPtr query_context, std::shared_ptr<DAGGraph> dag_graph_ptr)
{
    try
    {
        UInt64 total_send_time_ms = 0;
        Stopwatch watch;
        /// random pick workers
        const auto & worker_group = query_context->tryGetCurrentWorkerGroup();
        std::vector<size_t> random_worker_ids;

        if (worker_group)
        {
            const auto & worker_hosts = worker_group->getHostWithPortsVec();
            random_worker_ids.resize(worker_hosts.size(), 0);
            std::iota(random_worker_ids.begin(), random_worker_ids.end(), 0);
            thread_local std::random_device rd;
            std::shuffle(random_worker_ids.begin(), random_worker_ids.end(), rd);
        }

        // scheduler source
        for (auto segment_id : dag_graph_ptr->sources)
        {
            if (segment_id == dag_graph_ptr->final)
                continue;
            std::unordered_map<size_t, PlanSegment *>::iterator it;
            it = dag_graph_ptr->id_to_segment.find(segment_id);
            if (it == dag_graph_ptr->id_to_segment.end())
                throw Exception("Logical error: source segment can not be found", ErrorCodes::LOGICAL_ERROR);
            AddressInfos address_infos;
            // TODO dongyifeng support send plansegment parallel
            address_infos = sendPlanSegment(it->second, true, query_context, dag_graph_ptr, random_worker_ids);
            if (dag_graph_ptr->local_exchange_ids.find(segment_id) != dag_graph_ptr->local_exchange_ids.end()
                && !dag_graph_ptr->has_set_local_exchange)
            {
                dag_graph_ptr->has_set_local_exchange = true;
                dag_graph_ptr->first_local_exchange_address = address_infos;
            }
            dag_graph_ptr->id_to_address.emplace(std::make_pair(segment_id, std::move(address_infos)));
            dag_graph_ptr->scheduler_segments.emplace(segment_id);
        }
        total_send_time_ms += watch.elapsedMilliseconds();

        std::unordered_map<size_t, PlanSegment *>::iterator it;
        while (dag_graph_ptr->id_to_address.size() < (dag_graph_ptr->id_to_segment.size() - 1))
        {
            for (it = dag_graph_ptr->id_to_segment.begin(); it != dag_graph_ptr->id_to_segment.end(); it++)
            {
                // final stage should not scheduler
                if (it->first == dag_graph_ptr->final)
                    continue;
                // already scheduled
                if (dag_graph_ptr->scheduler_segments.find(it->first) != dag_graph_ptr->scheduler_segments.end())
                    continue;
                // source
                if (it->second->getPlanSegmentInputs().size() == 1
                    && it->second->getPlanSegmentInputs()[0]->getPlanSegmentType() == PlanSegmentType::SOURCE)
                    throw Exception("Logical error: source segment should be schedule", ErrorCodes::LOGICAL_ERROR);

                bool is_inputs_ready = true;
                for (auto & segment_input : it->second->getPlanSegmentInputs())
                {
                    if (segment_input->getPlanSegmentType() == PlanSegmentType::SOURCE)
                    {
                        // segment has more than one input which one is table
                        continue;
                    }
                    if (dag_graph_ptr->scheduler_segments.find(segment_input->getPlanSegmentId())
                        != dag_graph_ptr->scheduler_segments.end())
                    {
                        auto address_it = dag_graph_ptr->id_to_address.find(segment_input->getPlanSegmentId());
                        if (address_it == dag_graph_ptr->id_to_address.end())
                            throw Exception(
                                "Logical error: address of segment " + std::to_string(segment_input->getPlanSegmentId())
                                    + " can not be found",
                                ErrorCodes::LOGICAL_ERROR);
                        if (segment_input->getSourceAddresses().empty())
                            segment_input->insertSourceAddress(address_it->second);
                    }
                    else
                    {
                        is_inputs_ready = false;
                        break;
                    }
                }
                if (is_inputs_ready)
                {
                    AddressInfos address_infos;
                    watch.restart();
                    address_infos = sendPlanSegment(it->second, false, query_context, dag_graph_ptr, random_worker_ids);
                    total_send_time_ms += watch.elapsedMilliseconds();
                    // local join/global join is not between two source stages, for example, group by subquery global join source table
                    if (dag_graph_ptr->local_exchange_ids.find(it->first) != dag_graph_ptr->local_exchange_ids.end()
                        && !dag_graph_ptr->has_set_local_exchange)
                    {
                        dag_graph_ptr->has_set_local_exchange = true;
                        dag_graph_ptr->first_local_exchange_address = address_infos;
                    }
                    dag_graph_ptr->id_to_address.emplace(std::make_pair(it->first, std::move(address_infos)));
                    dag_graph_ptr->scheduler_segments.emplace(it->first);
                }
            }
        }
        LOG_DEBUG(log, "SegmentScheduler send plansegments takes:{}", total_send_time_ms);

        auto final_it = dag_graph_ptr->id_to_segment.find(dag_graph_ptr->final);
        if (final_it == dag_graph_ptr->id_to_segment.end())
            throw Exception("Logical error: final stage is not found", ErrorCodes::LOGICAL_ERROR);

        const auto & final_address_info = getLocalAddress(query_context);
        LOG_TRACE(log, "SegmentScheduler set final plansegment with AddressInfo: {}", final_address_info.toString());
        final_it->second->setCurrentAddress(final_address_info);
        final_it->second->setCoordinatorAddress(final_address_info);

        for (const auto & plan_segment_input : final_it->second->getPlanSegmentInputs())
        {
            // segment has more than one input which one is table
            if (plan_segment_input->getPlanSegmentType() != PlanSegmentType::EXCHANGE)
                continue;
            plan_segment_input->setParallelIndex(1);
            if (dag_graph_ptr->scheduler_segments.find(plan_segment_input->getPlanSegmentId()) != dag_graph_ptr->scheduler_segments.end())
            {
                auto address_it = dag_graph_ptr->id_to_address.find(plan_segment_input->getPlanSegmentId());
                if (address_it == dag_graph_ptr->id_to_address.end())
                    throw Exception(
                        "Logical error: address of segment " + std::to_string(plan_segment_input->getPlanSegmentId()) + " can not be found",
                        ErrorCodes::LOGICAL_ERROR);
                if (plan_segment_input->getSourceAddresses().empty())
                    plan_segment_input->insertSourceAddress(address_it->second);
            }
            else
            {
                throw Exception(
                    "Logical error: source of final stage is not ready, id=" + std::to_string(plan_segment_input->getPlanSegmentId()),
                    ErrorCodes::LOGICAL_ERROR);
            }
        }
        dag_graph_ptr->plan_segment_status_ptr->is_final_stage_start = true;
    }
    catch (const Exception & e)
    {
        this->cancelPlanSegments(query_id, "receive exception during scheduler:" + e.message(), "coordinator", query_context, dag_graph_ptr);
        e.rethrow();
    }
    catch (...)
    {
        this->cancelPlanSegments(query_id, "receive unknown exception during scheduler", "coordinator", query_context, dag_graph_ptr);
        throw;
    }
    return true;
}

void sendPlanSegmentToLocal(PlanSegment * plan_segment_ptr, ContextPtr query_context, std::shared_ptr<DAGGraph> dag_graph_ptr)
{
    const auto local_address = getLocalAddress(query_context);
    plan_segment_ptr->setCurrentAddress(local_address);

    /// FIXME: deserializePlanSegment is heavy task, using executePlanSegmentRemotely can deserialize plansegment asynchronous
    // executePlanSegmentLocally(*plan_segment_ptr, query_context);
    executePlanSegmentRemotely(*plan_segment_ptr, query_context, true);
    if (dag_graph_ptr)
    {
        std::unique_lock<bthread::Mutex> lock(dag_graph_ptr->status_mutex);
        dag_graph_ptr->plan_send_addresses.emplace(std::move(local_address));
    }
}

void sendPlanSegmentToRemote(
    AddressInfo & addressinfo,
    ContextPtr query_context,
    PlanSegment * plan_segment_ptr,
    std::shared_ptr<DAGGraph> dag_graph_ptr)
{
    plan_segment_ptr->setCurrentAddress(addressinfo);

    executePlanSegmentRemotely(*plan_segment_ptr, query_context, true);
    if (dag_graph_ptr)
    {
        std::unique_lock<bthread::Mutex> lock(dag_graph_ptr->status_mutex);
        dag_graph_ptr->plan_send_addresses.emplace(addressinfo);
    }
}

AddressInfos SegmentScheduler::sendPlanSegment(
    PlanSegment * plan_segment_ptr,
    bool  /*is_source*/,
    ContextPtr query_context,
    std::shared_ptr<DAGGraph> dag_graph_ptr,
    std::vector<size_t> random_worker_ids)
{
    LOG_TRACE(
        &Poco::Logger::get("SegmentScheduler::sendPlanSegment"),
        "begin sendPlanSegment: " + std::to_string(plan_segment_ptr->getPlanSegmentId()));
    auto local_address = getLocalAddress(query_context);
    plan_segment_ptr->setCoordinatorAddress(local_address);
    // if stage is relation with local stage
    if (dag_graph_ptr->local_exchange_ids.find(plan_segment_ptr->getPlanSegmentId()) != dag_graph_ptr->local_exchange_ids.end()
        && dag_graph_ptr->has_set_local_exchange)
    {
        size_t parallel_index_id_index = 0;

        for (auto & address : dag_graph_ptr->first_local_exchange_address)
        {
            parallel_index_id_index++;
            for (auto & plan_segment_input : plan_segment_ptr->getPlanSegmentInputs())
            {
                {
                    plan_segment_input->setParallelIndex(parallel_index_id_index);

                    // if input mode is local, set parallel index to 1
                    if (auto it = dag_graph_ptr->id_to_segment.find(plan_segment_input->getPlanSegmentId()); it != dag_graph_ptr->id_to_segment.end())
                    {
                        auto plan_segment_output = it->second->getPlanSegmentOutput();
                        {
                            // if data is write to local, so no need to shuffle data
                            if (plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_NO_NEED_REPARTITION
                                || plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_MAY_NEED_REPARTITION)
                            {
                                plan_segment_input->setParallelIndex(1);
                                plan_segment_input->clearSourceAddresses();
                                plan_segment_input->insertSourceAddress(AddressInfo("localhost", 0, "", ""));
                            }
                        }
                    }

                    // collect status, useful for debug
#if defined(TASK_ASSIGN_DEBUG)
                    if (dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                        == dag_graph_ptr->exchange_data_assign_node_mappings.end())
                    {
                        dag_graph_ptr->exchange_data_assign_node_mappings.emplace(
                            std::make_pair(plan_segment_input->getPlanSegmentId(), std::vector<std::pair<size_t, AddressInfo>>{}));
                    }
                    dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                        ->second.emplace_back(std::make_pair(plan_segment_input->getParallelIndex(), address));
#endif
                }
            }
            sendPlanSegmentToRemote(address, query_context, plan_segment_ptr, dag_graph_ptr);
        }
        return dag_graph_ptr->first_local_exchange_address;
    }

    AddressInfos addresses;
    // getParallelSize equals to 0, then is just to send to local
    if (plan_segment_ptr->getParallelSize() == 0 || plan_segment_ptr->getClusterName().empty())
    {
        // send to local
        addresses.emplace_back(local_address);
        for (auto & plan_segment_input : plan_segment_ptr->getPlanSegmentInputs())
        {
            plan_segment_input->setParallelIndex(1);
#if defined(TASK_ASSIGN_DEBUG)
            if (dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                == dag_graph_ptr->exchange_data_assign_node_mappings.end())
            {
                dag_graph_ptr->exchange_data_assign_node_mappings.emplace(
                    std::make_pair(plan_segment_input->getPlanSegmentId(), std::vector<std::pair<size_t, AddressInfo>>{}));
            }
            dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                ->second.emplace_back(std::make_pair(plan_segment_input->getParallelIndex(), local_address));
#endif
        }
        sendPlanSegmentToLocal(plan_segment_ptr, query_context, dag_graph_ptr);
    }
    else
    {
        if (plan_segment_ptr->getClusterName().empty())
        {
            throw Exception(
                "Logical error: can't find workgroup in context which named " + plan_segment_ptr->getClusterName(),
                ErrorCodes::LOGICAL_ERROR);
        }


        const auto & worker_group = query_context->getCurrentWorkerGroup();
        const auto & worker_endpoints = worker_group->getHostWithPortsVec();
        size_t parallel_index_id_index = 0;
        // set ParallelIndexId and source address
        for (auto i : random_worker_ids)
        {
            parallel_index_id_index++;
            if (parallel_index_id_index > plan_segment_ptr->getParallelSize())
                break;
            const auto & worker_endpoint = worker_endpoints[i];
            for (const auto& plan_segment_input : plan_segment_ptr->getPlanSegmentInputs())
            {
                if (plan_segment_input->getPlanSegmentType() != PlanSegmentType::EXCHANGE)
                    continue;
                plan_segment_input->setParallelIndex(parallel_index_id_index);

                // if input mode is local, set parallel index to 1
                auto it = dag_graph_ptr->id_to_segment.find(plan_segment_input->getPlanSegmentId());
                auto plan_segment_output = it->second->getPlanSegmentOutput();
                {
                    // if data is write to local, so no need to shuffle data
                    if (plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_NO_NEED_REPARTITION
                        || plan_segment_output->getExchangeMode() == ExchangeMode::LOCAL_MAY_NEED_REPARTITION)
                    {
                        plan_segment_input->setParallelIndex(1);
                        plan_segment_input->clearSourceAddresses();
                        plan_segment_input->insertSourceAddress(AddressInfo("localhost", 0, "", ""));
                    }
                }
            }
            auto worker_address = getRemoteAddress(worker_endpoint, query_context);
            sendPlanSegmentToRemote(worker_address, query_context, plan_segment_ptr, dag_graph_ptr);
            addresses.emplace_back(std::move(worker_address));

#if defined(TASK_ASSIGN_DEBUG)
            for (auto & plan_segment_input : plan_segment_ptr->getPlanSegmentInputs())
            {
                {
                    if (dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                        == dag_graph_ptr->exchange_data_assign_node_mappings.end())
                    {
                        dag_graph_ptr->exchange_data_assign_node_mappings.emplace(
                            std::make_pair(plan_segment_input->getPlanSegmentId(), std::vector<std::pair<size_t, AddressInfo>>{}));
                    }
                    dag_graph_ptr->exchange_data_assign_node_mappings.find(plan_segment_input->getPlanSegmentId())
                        ->second.emplace_back(std::make_pair(plan_segment_input->getParallelIndex(), addresses[addresses.size() - 1]));
                }
            }
#endif
        }
    }

#if defined(TASK_ASSIGN_DEBUG)
    String res_log = "segment id:" + std::to_string(plan_segment_ptr->getPlanSegmentId()) + " send planSegment address information:\n";
    for (const auto& address_inf : addresses)
    {
        res_log += "  " + address_inf.toString() + "\n";
    }
    LOG_DEBUG(log, res_log);
#endif

    LOG_TRACE(&Poco::Logger::get("SegmentScheduler::sendPlanSegment"), "end sendPlanSegment: {}", plan_segment_ptr->getPlanSegmentId());
    return addresses;
}

}
