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

#include <QueryPlan/FilterStep.h>

#include <IO/Operators.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>
#include <Interpreters/ExpressionActions.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/ExpressionTransform.h>
#include <Processors/Transforms/FilterTransform.h>
#include <Common/JSONBuilder.h>
#include <Processors/Port.h>
#include <Optimizer/DynamicFilters.h>
#include <Functions/FunctionsInBloomFilter.h>
#include <Optimizer/PredicateUtils.h>

namespace DB
{

static ITransformingStep::Traits getTraits(const ActionsDAGPtr & expression)
{
    return ITransformingStep::Traits{
        {
            .preserves_distinct_columns = !expression->hasArrayJoin(), /// I suppose it actually never happens
            .returns_single_stream = false,
            .preserves_number_of_streams = true,
            .preserves_sorting = true,
        },
        {
            .preserves_number_of_rows = false,
        }};
}

FilterStep::FilterStep(const DataStream & input_stream_, ActionsDAGPtr actions_dag_, String filter_column_name_, bool remove_filter_column_)
    : ITransformingStep(
        input_stream_,
        FilterTransform::transformHeader(input_stream_.header, *actions_dag_, filter_column_name_, remove_filter_column_),
        getTraits(actions_dag_))
    , actions_dag(std::move(actions_dag_))
    , filter_column_name(std::move(filter_column_name_))
    , remove_filter_column(remove_filter_column_)
{
    /// TODO: it would be easier to remove all expressions from filter step. It should only filter by column name.
    updateDistinctColumns(output_stream->header, output_stream->distinct_columns);
}

// todo optimizer
FilterStep::FilterStep(const DataStream & input_stream_, const ConstASTPtr & filter_, bool remove_filter_column_)
    : ITransformingStep(input_stream_, input_stream_.header, {})
    , filter(filter_)
    , filter_column_name(filter->getColumnName())
    , remove_filter_column(remove_filter_column_)
{
}

void FilterStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = input_streams_[0].header;
}

void FilterStep::updateInputStream(DataStream input_stream, bool keep_header)
{
    Block out_header = std::move(output_stream->header);
    if (keep_header)
        out_header = FilterTransform::transformHeader(input_stream.header, *actions_dag, filter_column_name, remove_filter_column);

    output_stream = createOutputStream(input_stream, std::move(out_header), getDataStreamTraits());

    input_streams.clear();
    input_streams.emplace_back(std::move(input_stream));
}

ActionsDAGPtr FilterStep::createActions(ContextPtr context, const ASTPtr & rewrite_filter) const
{
    Names output;
    for (const auto & item : input_streams[0].header)
        output.emplace_back(item.name);
    output.push_back(rewrite_filter->getColumnName());

    return createExpressionActions(context, input_streams[0].header.getNamesAndTypesList(), output, rewrite_filter);
}


void FilterStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    if (!actions_dag)
    {
        auto rewrite_filter = rewriteDynamicFilter(filter, pipeline, settings);
        actions_dag = createActions(settings.context, rewrite_filter->clone());
        filter_column_name = rewrite_filter->getColumnName();
    }

    auto expression = std::make_shared<ExpressionActions>(actions_dag, settings.getActionsSettings());

    pipeline.addSimpleTransform([&](const Block & header, QueryPipeline::StreamType stream_type) {
        bool on_totals = stream_type == QueryPipeline::StreamType::Totals;
        return std::make_shared<FilterTransform>(header, expression, filter_column_name, remove_filter_column, on_totals);
    });

    if (!blocksHaveEqualStructure(pipeline.getHeader(), output_stream->header))
    {
        auto convert_actions_dag = ActionsDAG::makeConvertingActions(
            pipeline.getHeader().getColumnsWithTypeAndName(),
            output_stream->header.getColumnsWithTypeAndName(),
            ActionsDAG::MatchColumnsMode::Name);
        auto convert_actions = std::make_shared<ExpressionActions>(convert_actions_dag, settings.getActionsSettings());

        pipeline.addSimpleTransform([&](const Block & header) { return std::make_shared<ExpressionTransform>(header, convert_actions); });
    }
}

void FilterStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Filter column: " << filter_column_name;

    if (remove_filter_column)
        settings.out << " (removed)";
    settings.out << '\n';

    bool first = true;
    auto expression = std::make_shared<ExpressionActions>(actions_dag);
    for (const auto & action : expression->getActions())
    {
        settings.out << prefix << (first ? "Actions: " : "         ");
        first = false;
        settings.out << action.toString() << '\n';
    }

    settings.out << prefix << "Positions:";
    for (const auto & pos : expression->getResultPositions())
        settings.out << ' ' << pos;
    settings.out << '\n';
}

void FilterStep::describeActions(JSONBuilder::JSONMap & map) const
{
    map.add("Filter Column", filter_column_name);
    map.add("Removes Filter", remove_filter_column);

    auto expression = std::make_shared<ExpressionActions>(actions_dag);
    map.add("Expression", expression->toTree());
}

void FilterStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);

    if (!actions_dag)
    {
        writeBinary(false, buf);
        if (filter)
        {
            serializeAST(filter->clone(), buf);
        }
        else
        {
            throw Exception("ActionsDAG cannot be nullptr", ErrorCodes::LOGICAL_ERROR);
        }
    }
    else
    {
        writeBinary(true, buf);
        actions_dag->serialize(buf);
    }

    writeBinary(filter_column_name, buf);
    writeBinary(remove_filter_column, buf);
}

QueryPlanStepPtr FilterStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);
    bool has_actions;
    readBinary(has_actions, buf);

    ActionsDAGPtr actions_dag;
    ASTPtr filter;
    if (has_actions)
    {
        actions_dag = ActionsDAG::deserialize(buf, context);
    }
    else
    {
        filter = deserializeAST(buf);
    }

    String filter_column_name;
    readBinary(filter_column_name, buf);

    bool remove_filter_column;
    readBinary(remove_filter_column, buf);
    QueryPlanStepPtr step;
    if (actions_dag)
    {
        step = std::make_unique<FilterStep>(input_stream, std::move(actions_dag), filter_column_name, remove_filter_column);
    }
    else
    {
        step = std::make_unique<FilterStep>(input_stream, filter, remove_filter_column);
    }

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> FilterStep::copy(ContextPtr) const
{
    return std::make_shared<FilterStep>(input_streams[0], filter, remove_filter_column);
}

ConstASTPtr FilterStep::rewriteDynamicFilter(const ConstASTPtr & filter, QueryPipeline & pipeline, const BuildQueryPipelineSettings & build_context)
{
    auto filters = DynamicFilters::extractDynamicFilters(filter);
    if (filters.first.empty())
        return filter;

    std::vector<ConstASTPtr> predicates = std::move(filters.second);
    for (auto & dynamic_filter : filters.first)
    {
        auto description = DynamicFilters::extractDescription(dynamic_filter).value();
        pipeline.addRuntimeFilterHolder(RuntimeFilterHolder{
            build_context.distributed_settings.query_id, build_context.distributed_settings.plan_segment_id, description.id});

        auto dynamic_filters = DynamicFilters::createDynamicFilterRuntime(
            description,
            build_context.context->getInitialQueryId(),
            build_context.distributed_settings.plan_segment_id,
            build_context.context->getSettingsRef().wait_runtime_filter_timeout_for_filter,
            RuntimeFilterManager::getInstance(),
            "FilterStep");
        predicates.insert(predicates.end(), dynamic_filters.begin(), dynamic_filters.end());
    }

    return PredicateUtils::combineConjuncts(predicates);
}

}
