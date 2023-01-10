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

#include <QueryPlan/WindowStep.h>

#include <IO/Operators.h>
#include <Interpreters/ExpressionActions.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/Transforms/WindowTransform.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <Common/JSONBuilder.h>

namespace DB
{

static ITransformingStep::Traits getTraits()
{
    return ITransformingStep::Traits
    {
        {
            .preserves_distinct_columns = true,
            .returns_single_stream = false,
            .preserves_number_of_streams = true,
            .preserves_sorting = true,
        },
        {
            .preserves_number_of_rows = true
        }
    };
}

static Block addWindowFunctionResultColumns(const Block & block,
    std::vector<WindowFunctionDescription> window_functions)
{
    auto result = block;

    for (const auto & f : window_functions)
    {
        ColumnWithTypeAndName column_with_type;
        column_with_type.name = f.column_name;
        column_with_type.type = f.aggregate_function->getReturnType();
        column_with_type.column = column_with_type.type->createColumn();

        result.insert(column_with_type);
    }

    return result;
}

WindowStep::WindowStep(const DataStream & input_stream_, const WindowDescription & window_description_, bool need_sort_)
    : WindowStep(input_stream_, window_description_, window_description_.window_functions, need_sort_)
{}

WindowStep::WindowStep(const DataStream & input_stream_,
        const WindowDescription & window_description_,
        const std::vector<WindowFunctionDescription> & window_functions_,
        bool need_sort_)
    : ITransformingStep(
        input_stream_,
            addWindowFunctionResultColumns(input_stream_.header,
                window_functions_),
        getTraits())
    , window_description(window_description_)
    , window_functions(window_functions_)
    , input_header(input_stream_.header)
    , need_sort(need_sort_)
{
    // We don't remove any columns, only add, so probably we don't have to update
    // the output DataStream::distinct_columns.

    window_description.checkValid();

}

void WindowStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = addWindowFunctionResultColumns(input_streams_[0].header, window_functions);
}

void WindowStep::transformPipeline(QueryPipeline & pipeline,
                                   const BuildQueryPipelineSettings &s)
{
    if (need_sort && !window_description.full_sort_description.empty())
    {
        DataStream input_stream {input_header};
        PartialSortingStep partial_sorting_step{input_stream, window_description.full_sort_description, 0};
        partial_sorting_step.transformPipeline(pipeline, s);
        MergeSortingStep merge_sorting_step{input_stream, window_description.full_sort_description, 0};
        merge_sorting_step.transformPipeline(pipeline, s);
        MergingSortedStep merging_sorted_step{input_stream, window_description.full_sort_description, s.context->getSettingsRef().max_block_size, 0};
        merging_sorted_step.transformPipeline(pipeline, s);
    }

    // This resize is needed for cases such as `over ()` when we don't have a
    // sort node, and the input might have multiple streams. The sort node would
    // have resized it.
    pipeline.resize(1);

    pipeline.addSimpleTransform([&](const Block & /*header*/)
    {
        return std::make_shared<WindowTransform>(input_header,
            output_stream->header, window_description, window_functions,
            s.actions_settings.dialect_type);
    });

    assertBlocksHaveEqualStructure(pipeline.getHeader(), output_stream->header,
        "WindowStep transform for '" + window_description.window_name + "'");
}

void WindowStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Window: (";
    if (!window_description.partition_by.empty())
    {
        settings.out << "PARTITION BY ";
        for (size_t i = 0; i < window_description.partition_by.size(); ++i)
        {
            if (i > 0)
            {
                settings.out << ", ";
            }

            settings.out << window_description.partition_by[i].column_name;
        }
    }
    if (!window_description.partition_by.empty()
        && !window_description.order_by.empty())
    {
        settings.out << " ";
    }
    if (!window_description.order_by.empty())
    {
        settings.out << "ORDER BY "
            << dumpSortDescription(window_description.order_by);
    }
    settings.out << ")\n";

    for (size_t i = 0; i < window_functions.size(); ++i)
    {
        settings.out << prefix << (i == 0 ? "Functions: "
                                          : "           ");
        settings.out << window_functions[i].column_name << "\n";
    }
}

void WindowStep::describeActions(JSONBuilder::JSONMap & map) const
{
    if (!window_description.partition_by.empty())
    {
        auto partion_columns_array = std::make_unique<JSONBuilder::JSONArray>();
        for (const auto & descr : window_description.partition_by)
            partion_columns_array->add(descr.column_name);

        map.add("Partition By", std::move(partion_columns_array));
    }

    if (!window_description.order_by.empty())
        map.add("Sort Description", explainSortDescription(window_description.order_by, {}));

    auto functions_array = std::make_unique<JSONBuilder::JSONArray>();
    for (const auto & func : window_functions)
        functions_array->add(func.column_name);

    map.add("Functions", std::move(functions_array));
}

std::shared_ptr<IQueryPlanStep> WindowStep::copy(ContextPtr) const
{
    return std::make_shared<WindowStep>(input_streams[0], window_description, window_functions, need_sort);
}
void WindowStep::serialize(WriteBuffer & buffer) const
{
    IQueryPlanStep::serializeImpl(buffer);
    window_description.serialize(buffer);
    writeBinary(window_functions.size(), buffer);
    for (const auto & item : window_functions)
        item.serialize(buffer);
    writeBinary(need_sort, buffer);
}

QueryPlanStepPtr WindowStep::deserialize(ReadBuffer & buf, ContextPtr)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    WindowDescription window_description;
    std::vector<WindowFunctionDescription> window_functions;

    window_description.deserialize(buf);
    size_t size;
    readBinary(size, buf);
    for (size_t index = 0; index < size; ++index)
    {
        WindowFunctionDescription desc;
        desc.deserialize(buf);
        window_functions.emplace_back(desc);
    }
    bool need_sort;
    readBinary(need_sort, buf);
    auto step = std::make_shared<WindowStep>(input_stream, window_description, window_functions, need_sort);
    step->setStepDescription(step_description);
    return step;
}

}
