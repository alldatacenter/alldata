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

#include <QueryPlan/MergingSortedStep.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Merges/MergingSortedTransform.h>
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

MergingSortedStep::MergingSortedStep(
    const DataStream & input_stream_,
    SortDescription sort_description_,
    size_t max_block_size_,
    UInt64 limit_)
    : ITransformingStep(input_stream_, input_stream_.header, getTraits(limit_))
    , sort_description(std::move(sort_description_))
    , max_block_size(max_block_size_)
    , limit(limit_)
{
    /// TODO: check input_stream is partially sorted (each port) by the same description.
    output_stream->sort_description = sort_description;
    output_stream->sort_mode = DataStream::SortMode::Stream;
}

void MergingSortedStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}

void MergingSortedStep::updateLimit(size_t limit_)
{
    if (limit_ && (limit == 0 || limit_ < limit))
    {
        limit = limit_;
        transform_traits.preserves_number_of_rows = false;
    }
}

void MergingSortedStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    /// If there are several streams, then we merge them into one
    if (pipeline.getNumStreams() > 1)
    {

        auto transform = std::make_shared<MergingSortedTransform>(
                pipeline.getHeader(),
                pipeline.getNumStreams(),
                sort_description,
                max_block_size, limit);

        pipeline.addTransform(std::move(transform));
    }
}

void MergingSortedStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Sort description: ";
    dumpSortDescription(sort_description, input_streams.front().header, settings.out);
    settings.out << '\n';

    if (limit)
        settings.out << prefix << "Limit " << limit << '\n';
}

void MergingSortedStep::describeActions(JSONBuilder::JSONMap & map) const
{
    map.add("Sort Description", explainSortDescription(sort_description, input_streams.front().header));

    if (limit)
        map.add("Limit", limit);
}

void MergingSortedStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    serializeItemVector<SortColumnDescription>(sort_description, buffer);
    writeBinary(max_block_size, buffer);
    writeBinary(limit, buffer);
}

QueryPlanStepPtr MergingSortedStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    SortDescription sort_description;
    sort_description = deserializeItemVector<SortColumnDescription>(buffer);

    size_t max_block_size;
    readBinary(max_block_size, buffer);

    UInt64 limit;
    readBinary(limit, buffer);

    auto step = std::make_unique<MergingSortedStep>(input_stream, sort_description, max_block_size, limit);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> MergingSortedStep::copy(ContextPtr) const
{
    return std::make_shared<MergingSortedStep>(input_streams[0], sort_description, max_block_size, limit);
}

}
