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

#include <QueryPlan/FinishSortingStep.h>
#include <Processors/Transforms/DistinctTransform.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Merges/MergingSortedTransform.h>
#include <Processors/Transforms/PartialSortingTransform.h>
#include <Processors/Transforms/FinishSortingTransform.h>
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
            .returns_single_stream = true,
            .preserves_number_of_streams = false,
            .preserves_sorting = false,
        },
        {
            .preserves_number_of_rows = limit == 0,
        }
    };
}

FinishSortingStep::FinishSortingStep(
    const DataStream & input_stream_,
    SortDescription prefix_description_,
    SortDescription result_description_,
    size_t max_block_size_,
    UInt64 limit_)
    : ITransformingStep(input_stream_, input_stream_.header, getTraits(limit_))
    , prefix_description(std::move(prefix_description_))
    , result_description(std::move(result_description_))
    , max_block_size(max_block_size_)
    , limit(limit_)
{
    /// TODO: check input_stream is sorted by prefix_description.
    output_stream->sort_description = result_description;
    output_stream->sort_mode = DataStream::SortMode::Stream;
}

void FinishSortingStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}

void FinishSortingStep::updateLimit(size_t limit_)
{
    if (limit_ && (limit == 0 || limit_ < limit))
    {
        limit = limit_;
        transform_traits.preserves_number_of_rows = false;
    }
}

void FinishSortingStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    bool need_finish_sorting = (prefix_description.size() < result_description.size());
    if (pipeline.getNumStreams() > 1)
    {
        UInt64 limit_for_merging = (need_finish_sorting ? 0 : limit);
        auto transform = std::make_shared<MergingSortedTransform>(
                pipeline.getHeader(),
                pipeline.getNumStreams(),
                prefix_description,
                max_block_size, limit_for_merging);

        pipeline.addTransform(std::move(transform));
    }

    if (need_finish_sorting)
    {
        pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
        {
            if (stream_type != QueryPipeline::StreamType::Main)
                return nullptr;

            return std::make_shared<PartialSortingTransform>(header, result_description, limit);
        });

        /// NOTE limits are not applied to the size of temporary sets in FinishSortingTransform
        pipeline.addSimpleTransform([&](const Block & header) -> ProcessorPtr
        {
            return std::make_shared<FinishSortingTransform>(
                header, prefix_description, result_description, max_block_size, limit);
        });
    }
}

void FinishSortingStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');

    settings.out << prefix << "Prefix sort description: ";
    dumpSortDescription(prefix_description, input_streams.front().header, settings.out);
    settings.out << '\n';

    settings.out << prefix << "Result sort description: ";
    dumpSortDescription(result_description, input_streams.front().header, settings.out);
    settings.out << '\n';

    if (limit)
        settings.out << prefix << "Limit " << limit << '\n';
}

void FinishSortingStep::describeActions(JSONBuilder::JSONMap & map) const
{
    map.add("Prefix Sort Description", explainSortDescription(prefix_description, input_streams.front().header));
    map.add("Result Sort Description", explainSortDescription(result_description, input_streams.front().header));

    if (limit)
        map.add("Limit", limit);
}

void FinishSortingStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    serializeItemVector<SortColumnDescription>(prefix_description, buffer);
    serializeItemVector<SortColumnDescription>(result_description, buffer);
    writeBinary(max_block_size, buffer);
    writeBinary(limit, buffer);
}

QueryPlanStepPtr FinishSortingStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    SortDescription prefix_description;
    prefix_description = deserializeItemVector<SortColumnDescription>(buffer);

    SortDescription result_description;
    result_description = deserializeItemVector<SortColumnDescription>(buffer);

    size_t max_block_size;
    readBinary(max_block_size, buffer);

    UInt64 limit;
    readBinary(limit, buffer);

    auto step = std::make_unique<FinishSortingStep>(input_stream, prefix_description, result_description, max_block_size, limit);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> FinishSortingStep::copy(ContextPtr) const
{
    return std::make_shared<FinishSortingStep>(input_streams[0], prefix_description, result_description, max_block_size, limit);
}

}
