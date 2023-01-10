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

#include <QueryPlan/ProjectionStep.h>

#include <DataTypes/DataTypeHelper.h>
#include <IO/Operators.h>
#include <Interpreters/ExpressionActions.h>
#include <Interpreters/RuntimeFilter/BuildRuntimeFilterTransform.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/ExpressionTransform.h>

namespace DB
{
ProjectionStep::ProjectionStep(
    const DataStream & input_stream_,
    Assignments assignments_,
    NameToType name_to_type_,
    bool final_project_,
    std::unordered_map<String, DynamicFilterBuildInfo> dynamic_filters_)
    : ITransformingStep(input_stream_, {}, {})
    , assignments(std::move(assignments_))
    , name_to_type(std::move(name_to_type_))
    , final_project(final_project_)
    , dynamic_filters(std::move(dynamic_filters_))
{
    for (const auto & item : assignments)
    {
        output_stream->header.insert(ColumnWithTypeAndName{name_to_type[item.first], item.first});
    }
}

void ProjectionStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
}

void ProjectionStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    auto actions = createActions(settings.context);
    auto expression = std::make_shared<ExpressionActions>(actions, settings.getActionsSettings());

    pipeline.addSimpleTransform([&](const Block & header) { return std::make_shared<ExpressionTransform>(header, expression); });
    projection(pipeline, output_stream->header, settings);
    buildDynamicFilterPipeline(pipeline, settings);
}

void ProjectionStep::buildDynamicFilterPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context) const
{
    if (dynamic_filters.empty())
        return;
    if (!build_context.distributed_settings.is_distributed)
        return;

    for (const auto & dynamic_filter : dynamic_filters)
    {
        const auto & name = dynamic_filter.first;
        const auto & id = dynamic_filter.second.id;
        const auto & original_symbol = dynamic_filter.second.original_symbol;
        const auto & types = dynamic_filter.second.types;

        bool enable_bloom_filter = types.count(DynamicFilterType::BloomFilter);
        bool enable_range_filter = types.count(DynamicFilterType::Range);

        std::shared_ptr<RuntimeFilterConsumer> consumer = std::make_shared<RuntimeFilterConsumer>(
            build_context.context->getInitialQueryId(),
            id,
            pipeline.getNumStreams(), //+ (pipeline.stream_with_non_joined_data ? 1 : 0),
            build_context.distributed_settings.parallel_size,
            build_context.distributed_settings.coordinator_address,
            build_context.distributed_settings.current_address);

        // todo@kaixi: merge into single BlockInputStream
        pipeline.addSimpleTransform([&](const Block & header) {
            Block build_key{header.getByName(name)};
            std::unordered_map<std::string, std::string> build_to_probe_map = {{name, original_symbol}};
            return std::make_shared<BuildRuntimeFilterTransform>(
                header,
                build_context.context,
                build_key, // build column
                build_to_probe_map, // build column -> probe column
                enable_bloom_filter,
                enable_range_filter,
                std::unordered_map<String, std::vector<String>>{}, // alias, note@kaixi:not used at all in optimizer
                consumer);
        });
    }
}

void ProjectionStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);

    writeVarUInt(assignments.size(), buf);
    for (const auto & item : assignments)
    {
        writeStringBinary(item.first, buf);
        serializeAST(item.second->clone(), buf);
    }

    writeVarUInt(name_to_type.size(), buf);
    for (const auto & item : name_to_type)
    {
        writeStringBinary(item.first, buf);
        serializeDataType(item.second, buf);
    }

    writeBinary(final_project, buf);

    writeVarUInt(dynamic_filters.size(), buf);
    for (const auto & item : dynamic_filters)
    {
        writeBinary(item.first, buf);

        writeBinary(item.second.id, buf);
        writeBinary(item.second.original_symbol, buf);
        const auto & types = item.second.types;
        writeVarUInt(types.size(), buf);
        for (const auto & type : types)
            writeBinary(static_cast<UInt8>(type), buf);
    }
}

QueryPlanStepPtr ProjectionStep::deserialize(ReadBuffer & buf, ContextPtr)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    size_t size;
    readVarUInt(size, buf);
    Assignments assignments;
    for (size_t index = 0; index < size; ++index)
    {
        String name;
        readStringBinary(name, buf);
        auto ast = deserializeAST(buf);
        assignments.emplace_back(name, ast);
    }

    readVarUInt(size, buf);
    NameToType name_to_type;
    for (size_t index = 0; index < size; ++index)
    {
        String name;
        readStringBinary(name, buf);
        auto data_type = deserializeDataType(buf);
        name_to_type[name] = data_type;
    }

    bool final_project;
    readBinary(final_project, buf);

    size_t dynamic_filters_size;
    readVarUInt(dynamic_filters_size, buf);
    std::unordered_map<String, DynamicFilterBuildInfo> dynamic_filters;
    for (size_t i = 0; i < dynamic_filters_size; i++)
    {
        String symbol;
        readBinary(symbol, buf);

        DynamicFilterId id;
        readBinary(id, buf);

        String original_symbol;
        readBinary(original_symbol, buf);

        DynamicFilterTypes types;
        size_t dynamic_filter_types_size;
        readVarUInt(dynamic_filter_types_size, buf);
        for (size_t j = 0; j < dynamic_filter_types_size; j++)
        {
            UInt8 type;
            readBinary(type, buf);
            types.emplace(static_cast<DynamicFilterType>(type));
        }
        dynamic_filters.emplace(symbol, DynamicFilterBuildInfo{id, original_symbol, types});
    }

    return std::make_shared<ProjectionStep>(input_stream, assignments, name_to_type, final_project, dynamic_filters);
}

std::shared_ptr<IQueryPlanStep> ProjectionStep::copy(ContextPtr) const
{
    return std::make_shared<ProjectionStep>(input_streams[0], assignments, name_to_type, final_project, dynamic_filters);
}

ActionsDAGPtr ProjectionStep::createActions(ContextPtr context) const
{
    ASTPtr expr_list = std::make_shared<ASTExpressionList>();

    NamesWithAliases output;
    for (const auto & item : assignments)
    {
        expr_list->children.emplace_back(item.second->clone());
        output.emplace_back(NameWithAlias{item.second->getColumnName(), item.first});
    }
    return createExpressionActions(context, input_streams[0].header.getNamesAndTypesList(), output, expr_list);
}
}
