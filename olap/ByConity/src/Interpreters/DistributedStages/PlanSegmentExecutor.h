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
#include <Interpreters/Context_fwd.h>
#include <Interpreters/DistributedStages/PlanSegment.h>
#include <Interpreters/DistributedStages/PlanSegmentProcessList.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <Processors/Executors/PipelineExecutor.h>
#include <Processors/QueryPipeline.h>
#include <boost/core/noncopyable.hpp>
#include <Poco/Logger.h>
#include <common/types.h>

namespace DB
{
class ThreadGroupStatus;
struct BlockIO;
struct RuntimeSegmentsStatus
{
    RuntimeSegmentsStatus(
        const String & queryId_, int32_t segmentId_, bool isSucceed_, bool isCanceled_, const String & message_, int32_t code_)
        : query_id(queryId_), segment_id(segmentId_), is_succeed(isSucceed_), is_canceled(isCanceled_), message(message_), code(code_)
    {
    }

    RuntimeSegmentsStatus() { }

    String query_id;
    int32_t segment_id;
    bool is_succeed;
    bool is_canceled;
    String message;
    int32_t code;
};

class PlanSegmentExecutor : private boost::noncopyable
{
public:
    explicit PlanSegmentExecutor(PlanSegmentPtr plan_segment_, ContextMutablePtr context_);
    explicit PlanSegmentExecutor(PlanSegmentPtr plan_segment_, ContextMutablePtr context_, ExchangeOptions options_);

    RuntimeSegmentsStatus execute(std::shared_ptr<ThreadGroupStatus> thread_group = nullptr);
    BlockIO lazyExecute(bool add_output_processors = false);

    static void registerAllExchangeReceivers(const QueryPipeline & pipeline, UInt32 register_timeout_ms);

protected:
    void doExecute(std::shared_ptr<ThreadGroupStatus> thread_group);
    QueryPipelinePtr buildPipeline();
    void buildPipeline(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders);

private:
    ContextMutablePtr context;
    PlanSegmentPtr plan_segment;
    PlanSegmentOutputPtr plan_segment_output;
    ExchangeOptions options;
    Poco::Logger * logger;

    void addRepartitionExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders, bool keep_order);

    void addBroadcastExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders);

    void addLoadBalancedExchangeSink(QueryPipelinePtr & pipeline, BroadcastSenderPtrs & senders);

    void sendSegmentStatus(const RuntimeSegmentsStatus & status) noexcept;
};

}
