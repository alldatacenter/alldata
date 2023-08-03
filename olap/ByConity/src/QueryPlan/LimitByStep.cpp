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

#include <QueryPlan/LimitByStep.h>
#include <Processors/Transforms/LimitByTransform.h>
#include <Processors/QueryPipeline.h>
#include <IO/Operators.h>
#include <Common/JSONBuilder.h>

namespace DB
{

static ITransformingStep::Traits getTraits()
{
    return ITransformingStep::Traits
    {
        {
            .preserves_distinct_columns = true,
            .returns_single_stream = true,
            .preserves_number_of_streams = false,
            .preserves_sorting = true,
        },
        {
            .preserves_number_of_rows = false,
        }
    };
}

LimitByStep::LimitByStep(
    const DataStream & input_stream_,
    size_t group_length_, size_t group_offset_, const Names & columns_)
    : ITransformingStep(input_stream_, input_stream_.header, getTraits())
    , group_length(group_length_)
    , group_offset(group_offset_)
    , columns(columns_)
{
}

void LimitByStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}


void LimitByStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    pipeline.resize(1);

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;

        return std::make_shared<LimitByTransform>(header, group_length, group_offset, columns);
    });
}

void LimitByStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');

    settings.out << prefix << "Columns: ";

    if (columns.empty())
        settings.out << "none\n";
    else
    {
        bool first = true;
        for (const auto & column : columns)
        {
            if (!first)
                settings.out << ", ";
            first = false;

            settings.out << column;
        }
        settings.out << '\n';
    }

    settings.out << prefix << "Length " << group_length << '\n';
    settings.out << prefix << "Offset " << group_offset << '\n';
}

void LimitByStep::describeActions(JSONBuilder::JSONMap & map) const
{
    auto columns_array = std::make_unique<JSONBuilder::JSONArray>();
    for (const auto & column : columns)
        columns_array->add(column);

    map.add("Columns", std::move(columns_array));
    map.add("Length", group_length);
    map.add("Offset", group_offset);
}

void LimitByStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    writeBinary(group_length, buffer);
    writeBinary(group_offset, buffer);
    serializeStrings(columns, buffer);
}

QueryPlanStepPtr LimitByStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    size_t group_length, group_offset;
    readBinary(group_length, buffer);
    readBinary(group_offset, buffer);

    Names column = deserializeStrings(buffer);

    auto step = std::make_unique<LimitByStep>(input_stream, group_length, group_offset, column);
    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> LimitByStep::copy(ContextPtr) const
{
    return std::make_shared<LimitByStep>(input_streams[0], group_length, group_offset, columns);
}

}
