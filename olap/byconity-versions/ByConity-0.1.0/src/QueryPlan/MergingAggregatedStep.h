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
#include <QueryPlan/ITransformingStep.h>
#include <QueryPlan/AggregatingStep.h>
#include <DataStreams/SizeLimits.h>
#include <Processors/Transforms/AggregatingTransform.h>

namespace DB
{

using AggregatingTransformParamsPtr = std::shared_ptr<AggregatingTransformParams>;

/// This step finishes aggregation. See AggregatingSortedTransform.
class MergingAggregatedStep : public ITransformingStep
{
public:
    MergingAggregatedStep(
        const DataStream & input_stream_,
        AggregatingTransformParamsPtr params_,
        bool memory_efficient_aggregation_,
        size_t max_threads_,
        size_t memory_efficient_merge_threads_);

    // used by optimizer
    MergingAggregatedStep(
        const DataStream & input_stream_,
        Names keys_,
        GroupingSetsParamsList grouping_sets_params_,
        GroupingDescriptions groupings_,
        AggregatingTransformParamsPtr params_,
        bool memory_efficient_aggregation_,
        size_t max_threads_,
        size_t memory_efficient_merge_threads_);

    String getName() const override { return "MergingAggregated"; }

    Type getType() const override { return Type::MergingAggregated; }

    void transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    void describeActions(JSONBuilder::JSONMap & map) const override;
    void describeActions(FormatSettings & settings) const override;

    AggregatingTransformParamsPtr getParams() const { return params; }

    const Names & getKeys() const { return keys; }
    const AggregateDescriptions & getAggregates() const { return params->params.aggregates; }
    const GroupingDescriptions & getGroupings() const { return groupings; }

    void serialize(WriteBuffer & buf) const override;
    static QueryPlanStepPtr deserialize(ReadBuffer & buf, ContextPtr);
    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;
    void setInputStreams(const DataStreams & input_streams_) override;

private:
    Names keys;
    GroupingSetsParamsList grouping_sets_params;
    GroupingDescriptions groupings;
    AggregatingTransformParamsPtr params;
    bool memory_efficient_aggregation;
    size_t max_threads;
    size_t memory_efficient_merge_threads;
};

}
