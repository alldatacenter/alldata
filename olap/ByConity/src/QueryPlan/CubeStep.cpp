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

#include <QueryPlan/CubeStep.h>
#include <Processors/Transforms/CubeTransform.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/QueryPipeline.h>
#include <QueryPlan/AggregatingStep.h>
#include <DataTypes/DataTypesNumber.h>

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

CubeStep::CubeStep(const DataStream & input_stream_, AggregatingTransformParamsPtr params_)
    : ITransformingStep(input_stream_, appendGroupingSetColumn(params_->getHeader()), getTraits())
    , keys_size(params_->params.keys_size)
    , params(std::move(params_))
{
    /// Aggregation keys are distinct
    for (auto key : params->params.keys)
        output_stream->distinct_columns.insert(params->params.src_header.getByPosition(key).name);
}

void CubeStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = appendGroupingSetColumn(params->getHeader());
}

ProcessorPtr addGroupingSetForTotals(const Block & header, const BuildQueryPipelineSettings & settings, UInt64 grouping_set_number)
{
    auto dag = std::make_shared<ActionsDAG>(header.getColumnsWithTypeAndName());

    auto grouping_col = ColumnUInt64::create(1, grouping_set_number);
    const auto * grouping_node = &dag->addColumn(
            {ColumnPtr(std::move(grouping_col)), std::make_shared<DataTypeUInt64>(), "__grouping_set"});

    grouping_node = &dag->materializeNode(*grouping_node);
    auto & index = dag->getIndex();
    index.insert(index.begin(), grouping_node);

    auto expression = std::make_shared<ExpressionActions>(dag, settings.getActionsSettings());
    return std::make_shared<ExpressionTransform>(header, expression);
}

void CubeStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    pipeline.resize(1);

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type == QueryPipeline::StreamType::Totals)
            return addGroupingSetForTotals(header, settings, (UInt64(1) << keys_size) - 1);

        return std::make_shared<CubeTransform>(header, std::move(params));
    });
}

const Aggregator::Params & CubeStep::getParams() const
{
    return params->params;
}

void CubeStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);
    serializeAggregatingTransformParams(params, buf);
}

QueryPlanStepPtr CubeStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    auto transform_params = deserializeAggregatingTransformParams(buf, context);

    auto step = std::make_unique<CubeStep>(input_stream, std::move(transform_params));

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> CubeStep::copy(ContextPtr) const
{
    return std::make_shared<CubeStep>(input_streams[0], params);
}

}
