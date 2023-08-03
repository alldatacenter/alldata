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

#include <IO/Operators.h>
#include <Interpreters/ExpressionActions.h>
#include <Processors/QueryPipeline.h>
#include <Processors/Transforms/DistinctTransform.h>
#include <Processors/Transforms/TotalsHavingTransform.h>
#include <QueryPlan/TotalsHavingStep.h>
#include <Common/JSONBuilder.h>

namespace DB
{

static ITransformingStep::Traits getTraits(bool has_filter)
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
            .preserves_number_of_rows = !has_filter,
        }
    };
}

TotalsHavingStep::TotalsHavingStep(
    const DataStream & input_stream_,
    bool overflow_row_,
    const ActionsDAGPtr & actions_dag_,
    const std::string & filter_column_,
    TotalsMode totals_mode_,
    double auto_include_threshold_,
    bool final_)
    : ITransformingStep(
        input_stream_,
        TotalsHavingTransform::transformHeader(input_stream_.header, actions_dag_.get(), final_),
        getTraits(!filter_column_.empty()))
    , overflow_row(overflow_row_)
    , actions_dag(actions_dag_)
    , filter_column_name(filter_column_)
    , totals_mode(totals_mode_)
    , auto_include_threshold(auto_include_threshold_)
    , final(final_)
{
}

void TotalsHavingStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = input_streams_;
    output_stream->header = TotalsHavingTransform::transformHeader(input_streams_[0].header, actions_dag.get(), final);
}

void TotalsHavingStep::transformPipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings & settings)
{
    auto expression_actions = actions_dag ? std::make_shared<ExpressionActions>(actions_dag, settings.getActionsSettings()) : nullptr;

    auto totals_having = std::make_shared<TotalsHavingTransform>(
        pipeline.getHeader(),
        overflow_row,
        expression_actions,
        filter_column_name,
        totals_mode,
        auto_include_threshold,
        final);

    pipeline.addTotalsHavingTransform(std::move(totals_having));
}

static String totalsModeToString(TotalsMode totals_mode, double auto_include_threshold)
{
    switch (totals_mode)
    {
        case TotalsMode::BEFORE_HAVING:
            return "before_having";
        case TotalsMode::AFTER_HAVING_INCLUSIVE:
            return "after_having_inclusive";
        case TotalsMode::AFTER_HAVING_EXCLUSIVE:
            return "after_having_exclusive";
        case TotalsMode::AFTER_HAVING_AUTO:
            return "after_having_auto threshold " + std::to_string(auto_include_threshold);
    }

    __builtin_unreachable();
}

void TotalsHavingStep::describeActions(FormatSettings & settings) const
{
    String prefix(settings.offset, ' ');
    settings.out << prefix << "Filter column: " << filter_column_name << '\n';
    settings.out << prefix << "Mode: " << totalsModeToString(totals_mode, auto_include_threshold) << '\n';

    if (actions_dag)
    {
        bool first = true;
        auto expression = std::make_shared<ExpressionActions>(actions_dag);
        for (const auto & action : expression->getActions())
        {
            settings.out << prefix << (first ? "Actions: "
                                             : "         ");
            first = false;
            settings.out << action.toString() << '\n';
        }
    }
}

void TotalsHavingStep::describeActions(JSONBuilder::JSONMap & map) const
{
    map.add("Mode", totalsModeToString(totals_mode, auto_include_threshold));
    if (actions_dag)
    {
        map.add("Filter column", filter_column_name);
        auto expression = std::make_shared<ExpressionActions>(actions_dag);
        map.add("Expression", expression->toTree());
    }
}

void TotalsHavingStep::serialize(WriteBuffer & buf) const
{
    IQueryPlanStep::serializeImpl(buf);
    writeBinary(overflow_row, buf);

    if (!actions_dag)
        throw Exception("ActionsDAG cannot be nullptr", ErrorCodes::LOGICAL_ERROR);
    actions_dag->serialize(buf);

    writeBinary(filter_column_name, buf);
    serializeEnum(totals_mode, buf);
    writeBinary(auto_include_threshold, buf);
    writeBinary(final, buf);
}

QueryPlanStepPtr TotalsHavingStep::deserialize(ReadBuffer & buf, ContextPtr context)
{
    String step_description;
    readBinary(step_description, buf);

    DataStream input_stream = deserializeDataStream(buf);

    bool overflow_row;
    readBinary(overflow_row, buf);

    ActionsDAGPtr actions_dag = ActionsDAG::deserialize(buf, context);

    String filter_column_name;
    readBinary(filter_column_name, buf);

    TotalsMode totals_mode;
    deserializeEnum(totals_mode, buf);

    double auto_include_threshold;
    readBinary(auto_include_threshold, buf);

    bool final;
    readBinary(final, buf);

    auto step = std::make_unique<TotalsHavingStep>(input_stream, overflow_row, std::move(actions_dag), filter_column_name, totals_mode, auto_include_threshold, final);

    step->setStepDescription(step_description);
    return step;
}

std::shared_ptr<IQueryPlanStep> TotalsHavingStep::copy(ContextPtr) const
{
    return std::make_shared<TotalsHavingStep>(
        input_streams[0], overflow_row, actions_dag, filter_column_name, totals_mode, auto_include_threshold, final);
}

}
