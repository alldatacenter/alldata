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

#include <QueryPlan/UnionStep.h>

#include <Interpreters/ExpressionActions.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTIdentifier.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Sources/NullSource.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <QueryPlan/PlanSerDerHelper.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

//static Block checkHeaders(const DataStreams & input_streams)
//{
//    if (input_streams.empty())
//        throw Exception(ErrorCodes::LOGICAL_ERROR, "Cannot unite an empty set of query plan steps");
//
//    Block res = input_streams.front().header;
//    for (const auto & stream : input_streams)
//        assertBlocksHaveEqualStructure(stream.header, res, "UnionStep");
//
//    return res;
//}

UnionStep::UnionStep(
    DataStreams input_streams_,
    DataStream output_stream_,
    std::unordered_map<String, std::vector<String>> output_to_inputs_,
    size_t max_threads_,
    bool local_)
    : SetOperationStep(input_streams_, output_stream_, output_to_inputs_), max_threads(max_threads_), local(local_)
{
    header = Block();
    for (auto & item : output_stream->header)
        header.insert(ColumnWithTypeAndName(item.type, item.name));

    if (header.columns() > 1 && header.has("_dummy"))
        header.erase("_dummy");
}

QueryPipelinePtr UnionStep::updatePipeline(QueryPipelines pipelines, const BuildQueryPipelineSettings & settings)
{
    auto pipeline = std::make_unique<QueryPipeline>();
    QueryPipelineProcessorsCollector collector(*pipeline, this);

    if (pipelines.empty())
    {
        pipeline->init(Pipe(std::make_shared<NullSource>(output_stream->header)));
        processors = collector.detachProcessors();
        return pipeline;
    }

    size_t index = 0;
    for (auto & cur_pipeline : pipelines)
    {
        ASTPtr expr_list = std::make_shared<ASTExpressionList>();
        NamesWithAliases output_names;
        bool need_rename = false;
        for (const auto & item : output_stream->header)
        {
            auto rename_from = output_to_inputs.at(item.name).at(index);
            output_names.emplace_back(rename_from, item.name);
            ASTPtr identifier = std::make_shared<ASTIdentifier>(rename_from);
            identifier->setAlias(item.name);
            expr_list->children.emplace_back(identifier);
            if (item.name != rename_from)
            {
                need_rename = true;
            }
        }
        if (need_rename)
        {
            auto project_action
                = createExpressionActions(settings.context, cur_pipeline->getHeader().getNamesAndTypesList(), output_names, expr_list);
            auto expression = std::make_shared<ExpressionActions>(project_action, settings.getActionsSettings());
            cur_pipeline->addSimpleTransform(
                [&](const Block & header_) { return std::make_shared<ExpressionTransform>(header_, expression); });

            if (!blocksHaveEqualStructure(cur_pipeline->getHeader(), getOutputStream().header))
            {
                auto actions_dag = ActionsDAG::makeConvertingActions(
                    cur_pipeline->getHeader().getColumnsWithTypeAndName(),
                    getOutputStream().header.getColumnsWithTypeAndName(),
                    ActionsDAG::MatchColumnsMode::Position);
                auto converting_actions = std::make_shared<ExpressionActions>(std::move(actions_dag));
                cur_pipeline->addSimpleTransform(
                    [&](const Block & cur_header) { return std::make_shared<ExpressionTransform>(cur_header, converting_actions); });
            }
        }

        /// Headers for union must be equal.
        /// But, just in case, convert it to the same header if not.
        if (!isCompatibleHeader(cur_pipeline->getHeader(), getOutputStream().header))
        {
            auto converting_dag = ActionsDAG::makeConvertingActions(
                cur_pipeline->getHeader().getColumnsWithTypeAndName(),
                getOutputStream().header.getColumnsWithTypeAndName(),
                ActionsDAG::MatchColumnsMode::Name);

            auto converting_actions = std::make_shared<ExpressionActions>(std::move(converting_dag));
            cur_pipeline->addSimpleTransform(
                [&](const Block & cur_header) { return std::make_shared<ExpressionTransform>(cur_header, converting_actions); });
        }
        index++;
    }

    if (max_threads == 0)
    {
        max_threads = settings.context->getSettingsRef().max_threads;
    }

    *pipeline = QueryPipeline::unitePipelines(std::move(pipelines), max_threads);

    processors = collector.detachProcessors();
    return pipeline;
}

void UnionStep::describePipeline(FormatSettings & settings) const
{
    IQueryPlanStep::describePipeline(processors, settings);
}

void UnionStep::serialize(WriteBuffer & buffer) const
{
    writeBinary(input_streams.size(), buffer);
    for (const auto & input_stream : input_streams)
        serializeDataStream(input_stream, buffer);

    writeBinary(max_threads, buffer);

    serializeDataStream(output_stream.value(), buffer);

    writeBinary(local, buffer);

    writeVarUInt(output_to_inputs.size(), buffer);
    for (const auto & item : output_to_inputs)
    {
        writeStringBinary(item.first, buffer);
        writeVarUInt(item.second.size(), buffer);
        for (const auto & str : item.second)
        {
            writeStringBinary(str, buffer);
        }
    }
}

QueryPlanStepPtr UnionStep::deserialize(ReadBuffer & buffer, ContextPtr)
{
    size_t size;
    readBinary(size, buffer);

    DataStreams input_streams(size);
    for (size_t i = 0; i < size; ++i)
        input_streams[i] = deserializeDataStream(buffer);

    size_t max_threads;
    readBinary(max_threads, buffer);

    auto output_stream = deserializeDataStream(buffer);

    bool local;
    readBinary(local, buffer);

    std::unordered_map<String, std::vector<String>> output_to_inputs;
    readVarUInt(size, buffer);
    for (size_t index = 0; index < size; index++)
    {
        String output;
        readStringBinary(output, buffer);
        size_t count;
        readVarUInt(count, buffer);
        for (size_t i = 0; i < count; i++)
        {
            String str;
            readStringBinary(str, buffer);
            output_to_inputs[output].emplace_back(str);
        }
    }
    return std::make_unique<UnionStep>(input_streams, output_stream, output_to_inputs, local, max_threads);
}

std::shared_ptr<IQueryPlanStep> UnionStep::copy(ContextPtr) const
{
    return std::make_shared<UnionStep>(input_streams, output_stream.value(), output_to_inputs, max_threads, local);
}

}
