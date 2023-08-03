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

#include <QueryPlan/PartialSortingStep.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/PartialSortingTransform.h>
#include <Processors/Transforms/LimitsCheckingTransform.h>
#include <IO/Operators.h>
#include <Common/JSONBuilder.h>

namespace DB
{

static ITransformingStep::Traits getTraits(size_t limit)
{
    return ITransformingStep::Traits
    {
        {
            .preserves_distinct_columns = true,
            .returns_single_stream = false,
            .preserves_number_of_streams = true,
            .preserves_sorting = false,
        },
        {
            .preserves_number_of_rows = limit == 0,
        }
    };
}

PartialSortingStep::PartialSortingStep(
    const DataStream & input_stream_,
    SortDescription sort_description_,
    UInt64 limit_,
    SizeLimits size_limits_)
    : ITransformingStep(input_stream_, input_stream_.header, getTraits(limit_))
    , sort_description(std::move(sort_description_))
    , limit(limit_)
    , size_limits(size_limits_)
{
    output_stream->sort_description = sort_description;
    output_stream->sort_mode = DataStream::SortMode::Chunk;
}

void PartialSortingStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}

void PartialSortingStep::updateLimit(size_t limit_)
{
    if (limit_ && (limit == 0 || limit_ < limit))
    {
        limit = limit_;
        transform_traits.preserves_number_of_rows = false;
    }
}

void PartialSortingStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    if (size_limits.max_rows == 0)
    {
        size_limits.max_rows = settings.context->getSettingsRef().max_rows_to_sort;
        size_limits.max_bytes= settings.context->getSettingsRef().max_bytes_to_sort;
        size_limits.overflow_mode = settings.context->getSettingsRef().sort_overflow_mode;
    }

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;

        return std::make_shared<PartialSortingTransform>(header, sort_description, limit);
    });

    StreamLocalLimits limits;
    limits.mode = LimitsMode::LIMITS_CURRENT; //-V1048
    limits.size_limits = size_limits;

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;

        auto transform = std::make_shared<LimitsCheckingTransform>(header, limits);
        return transform;
    });
}

void PartialSortingStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Sort description: ";
    dumpSortDescription(sort_description, input_streams.front().header, settings.out);
    settings.out << '\n';

    if (limit)
        settings.out << prefix << "Limit " << limit << '\n';
}

void PartialSortingStep::describeActions(JSONBuilder::JSONMap & map) const
{
    map.add("Sort Description", explainSortDescription(sort_description, input_streams.front().header));

    if (limit)
        map.add("Limit", limit);
}

void PartialSortingStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    serializeItemVector<SortColumnDescription>(sort_description, buffer);
    writeBinary(limit, buffer);
    size_limits.serialize(buffer);
}

QueryPlanStepPtr PartialSortingStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    SortDescription sort_description;
    sort_description = deserializeItemVector<SortColumnDescription>(buffer);

    UInt64 limit;
    readBinary(limit, buffer);

    SizeLimits size_limits;
    size_limits.deserialize(buffer);

    auto step = std::make_unique<PartialSortingStep>(input_stream, sort_description, limit, size_limits);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> PartialSortingStep::copy(ContextPtr) const
{
    return std::make_shared<PartialSortingStep>(input_streams[0], sort_description, limit, size_limits);
}

}
