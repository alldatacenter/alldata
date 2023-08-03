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

#include <QueryPlan/RollupStep.h>
#include <Processors/Transforms/RollupTransform.h>
#include <Processors/QueryPipeline.h>
#include <QueryPlan/AggregatingStep.h>

namespace DB
{

static ITransformingStep::Traits getTraits()
{
    return ITransformingStep::Traits
    {
        {
            .preserves_distinct_columns = false,
            .returns_single_stream = true,
            .preserves_number_of_streams = false,
            .preserves_sorting = false,
        },
        {
            .preserves_number_of_rows = false,
        }
    };
}

RollupStep::RollupStep(const DataStream & input_stream_, AggregatingTransformParamsPtr params_)
    : ITransformingStep(input_stream_, appendGroupingSetColumn(params_->getHeader()), getTraits())
    , keys_size(params_->params.keys_size)
    , params(std::move(params_))
{
    /// Aggregation keys are distinct
    for (auto key : params->params.keys)
        output_stream->distinct_columns.insert(params->params.src_header.getByPosition(key).name);
}

void RollupStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = appendGroupingSetColumn(params->getHeader());
}

ProcessorPtr addGroupingSetForTotals(const Block & header, const BuildQueryPipelineSettings & settings, UInt64 grouping_set_number);

void RollupStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    pipeline.resize(1);

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type == QueryPipeline::StreamType::Totals)
            return addGroupingSetForTotals(header, settings, keys_size);

        return std::make_shared<RollupTransform>(header, std::move(params));
    });
}

void RollupStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);
    serializeAggregatingTransformParams(params, buf);
}

QueryPlanStepPtr RollupStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    auto transform_params = deserializeAggregatingTransformParams(buf, context);

    auto step = std::make_unique<RollupStep>(input_stream, std::move(transform_params));

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> RollupStep::copy(ContextPtr) const
{
    return std::make_shared<RollupStep>(input_streams[0], params);
}

}
