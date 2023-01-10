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

#include <QueryPlan/DistinctStep.h>
#include <Processors/Transforms/DistinctTransform.h>
#include <Processors/QueryPipeline.h>
#include <IO/Operators.h>
#include <Common/JSONBuilder.h>

namespace DB
{

static bool checkColumnsAlreadyDistinct(const Names & columns, const NameSet & distinct_names)
{
    if (distinct_names.empty())
        return false;

    /// Now we need to check that distinct_names is a subset of columns.
    std::unordered_set<std::string_view> columns_set(columns.begin(), columns.end());
    for (const auto & name : distinct_names)
        if (columns_set.count(name) == 0)
            return false;

    return true;
}

static ITransformingStep::Traits getTraits(bool pre_distinct, bool already_distinct_columns)
{
    return ITransformingStep::Traits
    {
        {
            .preserves_distinct_columns = already_distinct_columns, /// Will be calculated separately otherwise
            .returns_single_stream = !pre_distinct && !already_distinct_columns,
            .preserves_number_of_streams = pre_distinct || already_distinct_columns,
            .preserves_sorting = true, /// Sorting is preserved indeed because of implementation.
        },
        {
            .preserves_number_of_rows = false,
        }
    };
}


DistinctStep::DistinctStep(
    const DataStream & input_stream_,
    const SizeLimits & set_size_limits_,
    UInt64 limit_hint_,
    const Names & columns_,
    bool pre_distinct_)
    : ITransformingStep(
            input_stream_,
            input_stream_.header,
            getTraits(pre_distinct_, checkColumnsAlreadyDistinct(columns_, input_stream_.distinct_columns)))
    , set_size_limits(set_size_limits_)
    , limit_hint(limit_hint_)
    , columns(columns_)
    , pre_distinct(pre_distinct_)
{
    if (!output_stream->distinct_columns.empty() /// Columns already distinct, do nothing
        && (!pre_distinct /// Main distinct
            || input_stream_.has_single_port)) /// pre_distinct for single port works as usual one
    {
        /// Build distinct set.
        for (const auto & name : columns)
            output_stream->distinct_columns.insert(name);
    }
}

void DistinctStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams[0].header;
}

void DistinctStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    if (checkColumnsAlreadyDistinct(columns, input_streams.front().distinct_columns))
        return;

    if (!pre_distinct)
        pipeline.resize(1);

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) -> ProcessorPtr
    {
        if (stream_type != QueryPipeline::StreamType::Main)
            return nullptr;

        return std::make_shared<DistinctTransform>(header, set_size_limits, limit_hint, columns);
    });
}

void DistinctStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Columns: ";

    if (columns.empty())
        settings.out << "none";
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
    }

    settings.out << '\n';
}

void DistinctStep::describeActions(JSONBuilder::JSONMap & map) const
{
    auto columns_array = std::make_unique<JSONBuilder::JSONArray>();
    for (const auto & column : columns)
        columns_array->add(column);

    map.add("Columns", std::move(columns_array));
}

void DistinctStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    set_size_limits.serialize(buffer);
    writeBinary(limit_hint, buffer);
    serializeStrings(columns, buffer);
    writeBinary(pre_distinct, buffer);
}

QueryPlanStepPtr DistinctStep::deserialize(ReadBuffer & buffer, ContextPtr )
{
    String step_description;
    readBinary(step_description, buffer);

    DataStream input_stream;
    input_stream = deserializeDataStream(buffer);

    SizeLimits size_limits;
    size_limits.deserialize(buffer);

    UInt64 limit_hint;
    readBinary(limit_hint, buffer);

    Names columns;
    columns = deserializeStrings(buffer);

    UInt8 pre_distinct;
    readBinary(pre_distinct, buffer);

    auto step = std::make_unique<DistinctStep>(input_stream, size_limits, limit_hint, columns, bool(pre_distinct));

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> DistinctStep::copy(ContextPtr) const
{
    return std::make_shared<DistinctStep>(input_streams[0], set_size_limits, limit_hint, columns, pre_distinct);
}

}
