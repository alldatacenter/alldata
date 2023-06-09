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

#include <Interpreters/Context_fwd.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Processors/Exchange/DataTrans/Brpc/BrpcRemoteBroadcastReceiver.h>
#include <Processors/Exchange/ExchangeOptions.h>
#include <QueryPlan/ISourceStep.h>
#include <Poco/Logger.h>

namespace DB
{
class PlanSegmentInput;
using PlanSegmentInputPtr = std::shared_ptr<PlanSegmentInput>;
using PlanSegmentInputs = std::vector<PlanSegmentInputPtr>;

class PlanSegment;
class RemoteExchangeSourceStep : public ISourceStep
{
public:
    explicit RemoteExchangeSourceStep(PlanSegmentInputs inputs_, DataStream input_stream_);

    String getName() const override { return "RemoteExchangeSource"; }
    Type getType() const override { return Type::RemoteExchangeSource; }

    void initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings) override;

    PlanSegmentInputs getInput() const { return inputs; }

    void setPlanSegment(PlanSegment * plan_segment_);
    PlanSegment * getPlanSegment() const { return plan_segment; }

    void serialize(WriteBuffer & buf) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr);

    void describePipeline(FormatSettings & settings) const override;

    void setExchangeOptions(ExchangeOptions options_) { options = options_; }
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;

private:
    void registerAllReceivers(BrpcReceiverPtrs receivers, UInt32 timeout_ms);
    PlanSegmentInputs inputs;
    PlanSegment * plan_segment = nullptr;
    Poco::Logger * logger;
    size_t plan_segment_id;
    String query_id;
    String coordinator_address;
    AddressInfo read_address_info;
    ContextPtr context;
    ExchangeOptions options;
};
}
