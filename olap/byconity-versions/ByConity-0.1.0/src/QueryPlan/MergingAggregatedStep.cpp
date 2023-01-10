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

#include <QueryPlan/MergingAggregatedStep.h>
#include <DataTypes/DataTypesNumber.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <Processors/Transforms/MergingAggregatedTransform.h>
#include <Processors/Transforms/MergingAggregatedMemoryEfficientTransform.h>

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

static Block appendGroupingColumns(Block header, const GroupingDescriptions & groupings)
{
    for (const auto & grouping: groupings)
        header.insert({std::make_shared<DataTypeUInt64>(), grouping.output_name});

    return header;
}

MergingAggregatedStep::MergingAggregatedStep(
    const DataStream & input_stream_,
    AggregatingTransformParamsPtr params_,
    bool memory_efficient_aggregation_,
    size_t max_threads_,
    size_t memory_efficient_merge_threads_)
    : MergingAggregatedStep(input_stream_,
                            {},
                            {},
                            {},
                            std::move(params_),
                            memory_efficient_aggregation_,
                            max_threads_,
                            memory_efficient_merge_threads_)
{
}

MergingAggregatedStep::MergingAggregatedStep(
    const DataStream & input_stream_,
    Names keys_,
    GroupingSetsParamsList grouping_sets_params_,
    GroupingDescriptions groupings_,
    AggregatingTransformParamsPtr params_,
    bool memory_efficient_aggregation_,
    size_t max_threads_,
    size_t memory_efficient_merge_threads_)
    : ITransformingStep(input_stream_, appendGroupingColumns(params_->getHeader(), groupings_), getTraits())
    , keys(std::move(keys_))
    , grouping_sets_params(std::move(grouping_sets_params_))
    , groupings(std::move(groupings_))
    , params(params_)
    , memory_efficient_aggregation(memory_efficient_aggregation_)
    , max_threads(max_threads_)
    , memory_efficient_merge_threads(memory_efficient_merge_threads_)
{
    /// Aggregation keys are distinct
    for (auto key : params->params.keys)
        output_stream->distinct_columns.insert(params->params.intermediate_header.getByPosition(key).name);
}

void MergingAggregatedStep::setInputStreams(const DataStreams & input_streams_)
{
    // TODO: what if input_streams and params->getHeader() are inconsistent
    input_streams = input_streams_;
    output_stream->header = appendGroupingColumns(params->getHeader(), groupings);
}

void MergingAggregatedStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_settings)
{
    if (!memory_efficient_aggregation)
    {
        /// We union several sources into one, paralleling the work.
        pipeline.resize(1);

        /// Now merge the aggregated blocks
        pipeline.addSimpleTransform([&](const Block & header)
        {
            return std::make_shared<MergingAggregatedTransform>(header, params, max_threads);
        });
    }
    else
    {
        auto num_merge_threads = memory_efficient_merge_threads
                                 ? static_cast<size_t>(memory_efficient_merge_threads)
                                 : static_cast<size_t>(max_threads);

        pipeline.addMergingAggregatedMemoryEfficientTransform(params, num_merge_threads);
    }

    computeGroupingFunctions(pipeline, groupings, keys, grouping_sets_params, build_settings);
}

void MergingAggregatedStep::describeActions(FormatSettings & settings) const
{
    return params->params.explain(settings.out, settings.offset);
}

void MergingAggregatedStep::describeActions(JSONBuilder::JSONMap & map) const
{
    params->params.explain(map);
}

void MergingAggregatedStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);
    serializeAggregatingTransformParams(params, buf);
    writeBinary(memory_efficient_aggregation, buf);
    writeBinary(max_threads, buf);
    writeBinary(memory_efficient_merge_threads, buf);

    serializeStrings(keys, buf);

    writeVarUInt(groupings.size(), buf);
    for (const auto & item : groupings)
    {
        writeBinary(item.argument_names, buf);
        writeStringBinary(item.output_name, buf);
    }

    writeVarUInt(grouping_sets_params.size(), buf);
    for (const auto & grouping_sets_param : grouping_sets_params)
    {
        writeBinary(grouping_sets_param.used_key_names, buf);
        writeBinary(grouping_sets_param.used_keys, buf);
        writeBinary(grouping_sets_param.missing_keys, buf);
    }
}

QueryPlanStepPtr MergingAggregatedStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);

    auto transform_params = deserializeAggregatingTransformParams(buf, context);

    bool memory_efficient_aggregation;
    readBinary(memory_efficient_aggregation, buf);

    size_t max_threads, memory_efficient_merge_threads;
    readBinary(max_threads, buf);
    readBinary(memory_efficient_merge_threads, buf);

    auto keys = deserializeStrings(buf);

    size_t size;
    readVarUInt(size, buf);
    GroupingDescriptions groupings;
    for (size_t i = 0; i < size; ++i)
    {
        GroupingDescription item;
        readBinary(item.argument_names, buf);
        readStringBinary(item.output_name, buf);
        groupings.emplace_back(std::move(item));
    }

    readVarUInt(size, buf);
    GroupingSetsParamsList grouping_sets_params;
    for (size_t i = 0; i < size; ++i)
    {
        GroupingSetsParams param;
        readBinary(param.used_key_names, buf);
        readBinary(param.used_keys, buf);
        readBinary(param.missing_keys, buf);
        grouping_sets_params.emplace_back(param);
    }

    auto step = std::make_unique<MergingAggregatedStep>(input_stream,
                                                        std::move(keys),
                                                        std::move(grouping_sets_params),
                                                        std::move(groupings),
                                                        std::move(transform_params),
                                                        memory_efficient_aggregation,
                                                        max_threads,
                                                        memory_efficient_merge_threads);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> MergingAggregatedStep::copy(ContextPtr) const
{
    return std::make_shared<MergingAggregatedStep>(
        input_streams[0], keys, grouping_sets_params, groupings, params, memory_efficient_aggregation, max_threads, memory_efficient_merge_threads);
}

}

