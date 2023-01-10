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

#include <exception>
#include <memory>
#include <vector>
#include <DataStreams/BlockIO.h>
#include <Interpreters/Context.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentExecutor.h>
#include <Interpreters/DistributedStages/PlanSegmentProcessList.h>
#include <Interpreters/RuntimeFilter/RuntimeFilterManager.h>
#include <Interpreters/ProcessList.h>
#include <Processors/Exchange/BroadcastExchangeSink.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxy.h>
#include <Processors/Exchange/DataTrans/BroadcastSenderProxyRegistry.h>
#include <Processors/Exchange/DataTrans/Brpc/AsyncRegisterResult.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastReceiver.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/DataTrans/Local/LocalBroadcastChannel.h>
#include <Processors/Exchange/DataTrans/Local/LocalChannelOptions.h>
#include <Processors/Exchange/DataTrans/RpcClient.h>
#include <Processors/Exchange/DataTrans/RpcChannelPool.h>
#include <Processors/Exchange/ExchangeDataKey.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Exchange/ExchangeSource.h>
#include <Processors/Exchange/ExchangeUtils.h>
#include <Processors/Exchange/LoadBalancedExchangeSink.h>
#include <Processors/Exchange/MultiPartitionExchangeSink.h>
#include <Processors/Exchange/RepartitionTransform.h>
#include <Processors/Exchange/SinglePartitionExchangeSink.h>
#include <Processors/Executors/PipelineExecutor.h>
#include <Processors/Transforms/BufferedCopyTransform.h>
#include <Protos/plan_segment_manager.pb.h>
#include <QueryPlan/BuildQueryPipelineSettings.h>
#include <QueryPlan/GraphvizPrinter.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <QueryPlan/QueryPlan.h>
#include <Common/Brpc/BrpcChannelPoolOptions.h>
#include <Common/CurrentThread.h>
#include <Common/Exception.h>
#include <Common/ThreadStatus.h>
#include <common/defines.h>
#include <common/logger_useful.h>
#include <common/types.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int LOGICAL_ERROR;
    extern const int QUERY_WAS_CANCELLED;
}

PlanSegmentExecutor::PlanSegmentExecutor(PlanSegmentPtr plan_segment_, ContextMutablePtr context_)
    : context(std::move(context_))
    , plan_segment(std::move(plan_segment_))
    , plan_segment_output(plan_segment->getPlanSegmentOutput())
    , logger(&Poco::Logger::get("PlanSegmentExecutor"))
{
    options = ExchangeUtils::getExchangeOptions(context);
}

PlanSegmentExecutor::PlanSegmentExecutor(PlanSegmentPtr plan_segment_, ContextMutablePtr context_, ExchangeOptions options_)
    : context(std::move(context_))
    , plan_segment(std::move(plan_segment_))
    , plan_segment_output(plan_segment->getPlanSegmentOutput())
    , options(std::move(options_))
    , logger(&Poco::Logger::get("PlanSegmentExecutor"))
{
}

RuntimeSegmentsStatus PlanSegmentExecutor::execute(ThreadGroupStatusPtr thread_group)
{
    LOG_DEBUG(logger, "execute PlanSegment:\n" + plan_segment->toString());
    try
    {
        doExecute(std::move(thread_group));
        RuntimeSegmentsStatus status(plan_segment->getQueryId(), plan_segment->getPlanSegmentId(), true, false, "execute success", 0);
        sendSegmentStatus(status);
        return status;
    }
    catch (...)
    {
        int exception_code = getCurrentExceptionCode();
        RuntimeSegmentsStatus status(
            plan_segment->getQueryId(), plan_segment->getPlanSegmentId(), false, false, getCurrentExceptionMessage(false), exception_code);
        tryLogCurrentException(logger, __PRETTY_FUNCTION__);
        if (exception_code == ErrorCodes::QUERY_WAS_CANCELLED)
            status.is_canceled = true;
        sendSegmentStatus(status);
        return status;
    }

    //TODO notify segment scheduler with finished or exception status.
}

BlockIO PlanSegmentExecutor::lazyExecute(bool /*add_output_processors*/)
{
    BlockIO res;
    // Will run as master query and already initialized
    if (!CurrentThread::get().getQueryContext() || CurrentThread::get().getQueryContext().get() != context.get())
        throw Exception("context not match", ErrorCodes::LOGICAL_ERROR);

    res.plan_segment_process_entry = context->getPlanSegmentProcessList().insert(*plan_segment, context);

    res.pipeline = std::move(*buildPipeline());

    return res;
}

void PlanSegmentExecutor::doExecute(ThreadGroupStatusPtr thread_group)
{
    std::optional<CurrentThread::QueryScope> query_scope;

    if (!thread_group)
    {
        if (!CurrentThread::getGroup())
        {
            query_scope.emplace(context); // Running as master query and not initialized
        }
        else
        {
            // Running as master query and already initialized
            if (!CurrentThread::get().getQueryContext() || CurrentThread::get().getQueryContext().get() != context.get())
                throw Exception("context not match", ErrorCodes::LOGICAL_ERROR);
        }
    }
    else
    {
        // Running as slave query in a thread different from master query
        if (CurrentThread::getGroup())
            throw Exception("There is a query attacted to context", ErrorCodes::LOGICAL_ERROR);

        if (CurrentThread::getQueryId() != plan_segment->getQueryId())
            throw Exception("Not the same distributed query", ErrorCodes::LOGICAL_ERROR);

        CurrentThread::attachTo(thread_group);
    }

    PlanSegmentProcessList::EntryPtr process_plan_segment_entry = context->getPlanSegmentProcessList().insert(*plan_segment, context);

    QueryPipelinePtr pipeline;
    BroadcastSenderPtrs senders;
    buildPipeline(pipeline, senders);

    QueryStatus * query_status = &process_plan_segment_entry->get();
    context->setProcessListElement(query_status);
    pipeline->setProcessListElement(query_status);

    auto pipeline_executor = pipeline->execute();

    size_t max_threads = context->getSettingsRef().max_threads;
    if (max_threads)
        pipeline->setMaxThreads(max_threads);
    size_t num_threads = pipeline->getNumThreads();

    LOG_DEBUG(
        logger,
        "Runing plansegment id {}, segment: {} pipeline with {} threads",
        plan_segment->getQueryId(),
        plan_segment->getPlanSegmentId(),
        num_threads);
    pipeline_executor->execute(num_threads);
    GraphvizPrinter::printPipeline(pipeline_executor->getProcessors(), pipeline_executor->getExecutingGraph(), context, plan_segment->getPlanSegmentId(), extractExchangeStatusHostPort(plan_segment->getCurrentAddress()));
    for (const auto & sender : senders)
        sender->finish(BroadcastStatusCode::ALL_SENDERS_DONE, "Upstream pipeline finished");
}

QueryPipelinePtr PlanSegmentExecutor::buildPipeline()
{
    QueryPipelinePtr pipeline = plan_segment->getQueryPlan().buildQueryPipeline(
        QueryPlanOptimizationSettings::fromContext(context), BuildQueryPipelineSettings::fromPlanSegment(plan_segment.get(), context));
    registerAllExchangeReceivers(*pipeline, options.exhcange_timeout_ms / 3);
    return pipeline;
}

void PlanSegmentExecutor::buildPipeline(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders)
{
    if (!plan_segment->getPlanSegmentOutput())
        throw Exception("PlanSegment has no output", ErrorCodes::LOGICAL_ERROR);

    size_t exchange_parallel_size = plan_segment_output->getExchangeParallelSize();
    size_t parallel_size = plan_segment_output->getParallelSize();
    ExchangeMode exchange_mode = plan_segment_output->getExchangeMode();
    size_t write_plan_segment_id = plan_segment->getPlanSegmentId();
    size_t read_plan_segment_id = plan_segment_output->getPlanSegmentId();
    String coordinator_address = extractExchangeStatusHostPort(plan_segment->getCoordinatorAddress());

    bool keep_order = plan_segment_output->needKeepOrder() || context->getSettingsRef().exchange_enable_force_keep_order;

    if (exchange_mode == ExchangeMode::BROADCAST)
        exchange_parallel_size = 1;

    /// output partitions num = num of plan_segment * exchange size
    /// for example, if downstream plansegment size is 2 (parallel_id is 0 and 1) and exchange_parallel_size is 4
    /// Exchange Sink will repartition data into 8 partition(2*4), partition id is range from 1 to 8.
    /// downstream plansegment and consumed partitions table:
    /// plansegment parallel_id :  partition id
    /// -----------------------------------------------
    /// 0                       : 1,2,3,4
    /// 1                       : 5,6,7,8
    size_t total_partition_num = exchange_parallel_size == 0 ? parallel_size : parallel_size * exchange_parallel_size;

    if (total_partition_num == 0)
        throw Exception("Total partition number should not be zero", ErrorCodes::LOGICAL_ERROR);

    const Block & header = plan_segment_output->getHeader();

    for (size_t i = 0; i < total_partition_num; i++)
    {
        size_t partition_id = i + 1;
        auto data_key = std::make_shared<ExchangeDataKey>(
            plan_segment->getQueryId(), write_plan_segment_id, read_plan_segment_id, partition_id, coordinator_address);
        BroadcastSenderProxyPtr sender = BroadcastSenderProxyRegistry::instance().getOrCreate(data_key);
        sender->accept(context, header);
        senders.emplace_back(std::move(sender));
    }

    pipeline = plan_segment->getQueryPlan().buildQueryPipeline(
        QueryPlanOptimizationSettings::fromContext(context), BuildQueryPipelineSettings::fromPlanSegment(plan_segment.get(), context));

    registerAllExchangeReceivers(*pipeline, options.exhcange_timeout_ms / 3);

    pipeline->setMaxThreads(pipeline->getNumThreads());
    auto output_size = context->getSettingsRef().exchange_unordered_output_parallel_size;
    if (!keep_order && output_size)
        pipeline->resize(output_size, false, false);

    LOG_DEBUG(logger, "plan segment {} add {} senders", plan_segment->getPlanSegmentId(), senders.size());

    if (senders.empty())
        throw Exception("Plan segment has no exchange sender!", ErrorCodes::LOGICAL_ERROR);

    if (exchange_mode == ExchangeMode::REPARTITION || exchange_mode == ExchangeMode::LOCAL_MAY_NEED_REPARTITION
        || exchange_mode == ExchangeMode::GATHER)
        addRepartitionExchangeSink(pipeline, senders, keep_order);
    else if (exchange_mode == ExchangeMode::LOCAL_NO_NEED_REPARTITION)
        addLoadBalancedExchangeSink(pipeline, senders);
    else if (exchange_mode == ExchangeMode::BROADCAST)
        addBroadcastExchangeSink(pipeline, senders);
    else
        throw Exception("Cannot find expected ExchangeMode " + std::to_string(UInt8(exchange_mode)), ErrorCodes::LOGICAL_ERROR);
}

void PlanSegmentExecutor::registerAllExchangeReceivers(const QueryPipeline & pipeline, UInt32 register_timeout_ms)
{
    const Processors & procesors = pipeline.getProcessors();
    std::vector<AsyncRegisterResult> async_results;
    std::vector<LocalBroadcastChannel *> local_receivers;
    std::exception_ptr exception;

    try
    {
        for (const auto & processor : procesors)
        {
            auto exchange_source_ptr = std::dynamic_pointer_cast<ExchangeSource>(processor);
            if (!exchange_source_ptr)
                continue;
            auto * receiver_ptr = exchange_source_ptr->getReceiver().get();
            auto * local_receiver = dynamic_cast<LocalBroadcastChannel *>(receiver_ptr);
            if (local_receiver)
                local_receivers.push_back(local_receiver);
            else
            {
                auto * brpc_receiver = dynamic_cast<BrpcRemoteBroadcastReceiver *>(receiver_ptr);
                if (unlikely(!brpc_receiver))
                    throw Exception("Unexpected SubReceiver Type: " + std::string(typeid(receiver_ptr).name()), ErrorCodes::LOGICAL_ERROR);
                async_results.emplace_back(brpc_receiver->registerToSendersAsync(register_timeout_ms));
            }
        }

        for (auto * local_receiver : local_receivers)
            local_receiver->registerToSenders(register_timeout_ms);
    }
    catch (...)
    {
        exception = std::current_exception();
    }



    /// Wait all brpc register rpc done
    for (auto & res : async_results)
        brpc::Join(res.cntl->call_id());

    if (exception)
        std::rethrow_exception(std::move(exception));

    /// get result
    for (auto & res : async_results)
    {
        // if exchange_enable_force_remote_mode = 1, sender and receiver in same process and sender stream may close before rpc end
        if (res.cntl->ErrorCode() == brpc::EREQUEST && boost::algorithm::ends_with(res.cntl->ErrorText(), "was closed before responded"))
        {
            LOG_INFO(
                &Poco::Logger::get("PlanSegmentExecutor"),
                "Receiver register sender successfully but sender already finished, host-{} , data_key: {}_{}_{}_{}_{}",
                butil::endpoint2str(res.cntl->remote_side()).c_str(),
                res.request->query_id(),
                res.request->write_segment_id(),
                res.request->read_segment_id(),
                res.request->parallel_id(),
                res.request->coordinator_address());
            continue;
        }
        res.channel->assertController(*res.cntl);
        LOG_TRACE(
            &Poco::Logger::get("PlanSegmentExecutor"),
            "Receiver register sender successfully, host-{} , data_key: {}_{}_{}_{}_{}",
            butil::endpoint2str(res.cntl->remote_side()).c_str(),
            res.request->query_id(),
            res.request->write_segment_id(),
            res.request->read_segment_id(),
            res.request->parallel_id(),
            res.request->coordinator_address());
    }
}


void PlanSegmentExecutor::addRepartitionExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders, bool keep_order)
{
    LOG_DEBUG(logger, "plan segment {} add repartition sink", plan_segment->getPlanSegmentId());
    ColumnsWithTypeAndName arguments;
    ColumnNumbers argument_numbers;
    for (const auto & column_name : plan_segment->getPlanSegmentOutput()->getShufflekeys())
    {
        arguments.emplace_back(plan_segment_output->getHeader().getByName(column_name));
        argument_numbers.emplace_back(plan_segment_output->getHeader().getPositionByName(column_name));
    }
    auto repartition_func = RepartitionTransform::getDefaultRepartitionFunction(arguments, context);
    size_t partition_num = senders.size();
    if (keep_order && context->getSettingsRef().exchange_enable_keep_order_parallel_shuffle && partition_num > 1)
    {
        pipeline->addSimpleTransform([&](const Block & header) {
            return std::make_shared<RepartitionTransform>(header, partition_num, argument_numbers, repartition_func);
        });

        size_t output_num = pipeline->getNumStreams();
        size_t sink_num = output_num * partition_num;
        pipeline->transform(
            [&](OutputPortRawPtrs ports) -> Processors {
                Processors new_processors;
                new_processors.reserve(output_num + sink_num);
                const auto & header = ports[0]->getHeader();
                for (size_t i = 0; i < output_num; ++i)
                {
                    auto copy_transform = std::make_shared<BufferedCopyTransform>(header, partition_num, 20);
                    connect(*ports[i], copy_transform->getInputPort());
                    auto & copy_outputs = copy_transform->getOutputs();
                    size_t partition_id = 0;
                    for (auto & copy_output : copy_outputs)
                    {
                        auto exchange_sink
                            = std::make_shared<SinglePartitionExchangeSink>(header, senders[partition_id], partition_id, options);
                        connect(copy_output, exchange_sink->getPort());
                        new_processors.emplace_back(std::move(exchange_sink));
                        partition_id++;
                    }
                    new_processors.emplace_back(std::move(copy_transform));
                }
                return new_processors;
            },
            sink_num);
    }
    else
    {
        pipeline->setSinks([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr {
            /// exchange sink only process StreamType::Main
            if (stream_type != QueryPipeline::StreamType::Main)
                return nullptr; /// return nullptr means this sink will not be added;
            return std::make_shared<MultiPartitionExchangeSink>(header, senders, repartition_func, argument_numbers, options);
        });
    }
}

void PlanSegmentExecutor::addBroadcastExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders) // NOLINT
{
    /// For broadcast exchange, we all 1:1 remote sender to one 1:N remote sender and can avoid duplicated serialization
    ExchangeUtils::mergeSenders(senders);
    LOG_DEBUG(logger, "After merge, broadcast sink size {}", senders.size());
    pipeline->setSinks([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;
        return std::make_shared<BroadcastExchangeSink>(header, senders, options);
    });
}

void PlanSegmentExecutor::addLoadBalancedExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders) // NOLINT
{
    LOG_DEBUG(logger, "plan segment {} add loadbalanced sink with {} senders", plan_segment->getPlanSegmentId(), senders.size());
    pipeline->setSinks([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;
        return std::make_shared<LoadBalancedExchangeSink>(header, senders);
    });
}

void PlanSegmentExecutor::sendSegmentStatus(const RuntimeSegmentsStatus & status) noexcept
{
    try
    {
        if (!options.need_send_plan_segment_status)
            return;
        auto address = extractExchangeStatusHostPort(plan_segment->getCoordinatorAddress());

        std::shared_ptr<RpcClient> rpc_client = RpcChannelPool::getInstance().getClient(address, BrpcChannelPoolOptions::DEFAULT_CONFIG_KEY, true);
        Protos::PlanSegmentManagerService_Stub manager(&rpc_client->getChannel());
        brpc::Controller cntl;
        Protos::SendPlanSegmentStatusRequest request;
        Protos::SendPlanSegmentStatusResponse response;
        request.set_query_id(status.query_id);
        request.set_segment_id(status.segment_id);
        request.set_is_succeed(status.is_succeed);
        request.set_is_canceled(status.is_canceled);
        request.set_code(status.code);
        request.set_message(status.message);

        manager.sendPlanSegmentStatus(&cntl, &request, &response, nullptr);
        rpc_client->assertController(cntl);
        LOG_TRACE(logger, "PlanSegment-{} send status to coordinator successfully, query id-{}.", request.segment_id(), request.query_id());
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}
}
