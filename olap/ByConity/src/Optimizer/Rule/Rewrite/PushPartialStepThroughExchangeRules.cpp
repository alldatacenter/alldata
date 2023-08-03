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

#include <Optimizer/Rule/Rewrite/PushPartialStepThroughExchangeRules.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/Rule/Pattern.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <QueryPlan/ExchangeStep.h>
#include <QueryPlan/QueryPlan.h>

namespace DB
{
PatternPtr PushPartialAggThroughExchange::getPattern() const
{
    return Patterns::aggregating()
        ->matchingStep<AggregatingStep>([](const AggregatingStep & step) { return /*!step.isTotals() && */step.isFinal(); })
        ->withSingle(
            // todo jp: support push through union
            Patterns::exchange()->matchingStep<ExchangeStep>([](const ExchangeStep & step) { return step.getInputStreams().size() == 1; }));
}

TransformResult PushPartialAggThroughExchange::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    if (!context.context->getSettingsRef().enable_push_partial_agg)
        return {};
    const auto * step = dynamic_cast<const AggregatingStep *>(node->getStep().get());

    // TODO if aggregate data is almost pure distinct, then partial aggregate is useless.
    // Consider these two cases
    // 1: if group by keys is or contains primary key (may be need function dependency ...), then don't push partial aggregate.
    // 2: for aggregate on base table, the statistics is accurate enough to perform cost-based push down. while other
    // cases, we can't relay on statistics.
    //    auto group_keys = step->getKeys();
    //    if (!group_keys.empty())
    //    {
    //        PlanNodeStatisticsPtr statistics = CardinalityEstimator::estimate(node->getChildren()[0], context.context);
    //        UInt64 row_count = statistics->getRowCount() == 0 ? 1 : statistics->getRowCount();
    //        UInt64 group_keys_ndv = 1;
    //        for (auto & key : group_keys)
    //        {
    //            SymbolStatisticsPtr symbol_stats = statistics->getSymbolStatistics(key);
    //            group_keys_ndv = group_keys_ndv * symbol_stats->getNdv();
    //        }
    //        double density = (double)group_keys_ndv / row_count;
    //        if (density > context.context.getSettingsRef().enable_partial_aggregate_ratio)
    //        {
    //            return {};
    //        }
    //    }
    auto old_exchange_node = node->getChildren()[0];
    const auto * old_exchange_step = dynamic_cast<const ExchangeStep *>(old_exchange_node->getStep().get());
    auto exchange_child = old_exchange_node->getChildren()[0];
    QueryPlanStepPtr partial_agg = std::make_shared<AggregatingStep>(
        exchange_child->getStep()->getOutputStream(),
        step->getKeys(),
        step->getAggregates(),
        step->getGroupingSetsParams(),
        false,
        step->getGroupings(),
        false,
        context.context->getSettingsRef().distributed_aggregation_memory_efficient
        );

    auto exchange_step = std::make_unique<ExchangeStep>(
        DataStreams{partial_agg->getOutputStream()},
        old_exchange_step->getExchangeMode(),
        old_exchange_step->getSchema(),
        old_exchange_step->needKeepOrder());

    PlanNodes children{exchange_child};
    auto partial_agg_node
        = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(partial_agg), children, node->getStatistics());

    PlanNodes exchange_children{partial_agg_node};
    auto exchange_node
        = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(exchange_step), exchange_children, node->getStatistics());

    PlanNodes exchange{exchange_node};
    auto exchange_header = exchange_node->getStep()->getOutputStream().header;
    ColumnNumbers keys;
    if (!step->getGroupingSetsParams().empty())
    {
        keys.push_back(exchange_header.getPositionByName("__grouping_set"));
    }

    for (const auto & key : step->getKeys())
    {
        keys.emplace_back(exchange_header.getPositionByName(key));
    }

    Aggregator::Params new_params(
        exchange_header,
        keys,
        step->getAggregates(),
        step->getParams().overflow_row,
        context.context->getSettingsRef().max_threads);
    auto transform_params = std::make_shared<AggregatingTransformParams>(new_params, step->isFinal());
    // TODO check
    QueryPlanStepPtr final_agg = std::make_shared<MergingAggregatedStep>(
        exchange_node->getStep()->getOutputStream(),
        step->getKeys(),
        step->getGroupingSetsParams(),
        step->getGroupings(),
        transform_params,
        false,
        context.context->getSettingsRef().max_threads,
        context.context->getSettingsRef().aggregation_memory_efficient_merge_threads);
    auto final_agg_node = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(final_agg), exchange, node->getStatistics());
    return final_agg_node;
}

PatternPtr PushPartialSortingThroughExchange::getPattern() const
{
    return Patterns::sorting()->withSingle(Patterns::exchange()->matchingStep<ExchangeStep>(
        [](const ExchangeStep & step) { return step.getExchangeMode() == ExchangeMode::GATHER; }));
}

TransformResult PushPartialSortingThroughExchange::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    const auto * step = dynamic_cast<const SortingStep *>(node->getStep().get());
    auto old_exchange_node = node->getChildren()[0];
    const auto * old_exchange_step = dynamic_cast<const ExchangeStep *>(old_exchange_node->getStep().get());

    PlanNodes exchange_children;
    for (size_t index = 0; index < old_exchange_node->getChildren().size(); index++)
    {
        auto exchange_child = old_exchange_node->getChildren()[index];
        if (dynamic_cast<SortingNode *>(exchange_child.get()))
        {
            return {};
        }

        SortDescription new_sort_desc;
        for (const auto & desc : step->getSortDescription())
        {
            auto new_desc = desc;
            new_desc.column_name = old_exchange_step->getOutToInputs().at(desc.column_name).at(index);
            new_sort_desc.emplace_back(new_desc);
        }

        auto before_exchange_sort
            = std::make_unique<SortingStep>(exchange_child->getStep()->getOutputStream(), new_sort_desc, step->getLimit(), true);
        PlanNodes children{exchange_child};
        auto before_exchange_sort_node = PlanNodeBase::createPlanNode(
            context.context->nextNodeId(), std::move(before_exchange_sort), children, node->getStatistics());
        exchange_children.emplace_back(before_exchange_sort_node);
    }

    auto exchange_step = old_exchange_step->copy(context.context);
    auto exchange_node = PlanNodeBase::createPlanNode(
        context.context->nextNodeId(), std::move(exchange_step), exchange_children, old_exchange_node->getStatistics());

    QueryPlanStepPtr final_sort = step->copy(context.context);
    PlanNodes exchange{exchange_node};
    auto final_sort_node
        = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(final_sort), exchange, node->getStatistics());
    return final_sort_node;
}

static bool isLimitNeeded(const LimitStep & limit, const PlanNodePtr & node)
{
    auto range = PlanNodeCardinality::extractCardinality(*node);
    return range.upperBound > limit.getLimit() + limit.getOffset();
}

PatternPtr PushPartialLimitThroughExchange::getPattern() const
{
    return Patterns::limit()->withSingle(Patterns::exchange()->matchingStep<ExchangeStep>(
        [](const ExchangeStep & step) { return step.getExchangeMode() == ExchangeMode::GATHER; }));
}

TransformResult PushPartialLimitThroughExchange::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    const auto * step = dynamic_cast<const LimitStep *>(node->getStep().get());
    auto old_exchange_node = node->getChildren()[0];
    const auto * old_exchange_step = dynamic_cast<const ExchangeStep *>(old_exchange_node->getStep().get());

    PlanNodes exchange_children;
    bool should_apply = false;
    for (const auto & exchange_child : old_exchange_node->getChildren())
    {
        if (isLimitNeeded(*step, exchange_child))
        {
            auto partial_limit = std::make_unique<LimitStep>(
                exchange_child->getStep()->getOutputStream(),
                step->getLimit() + step->getOffset(),
                0,
                false,
                false,
                step->getSortDescription(),
                true);
            auto partial_limit_node = PlanNodeBase::createPlanNode(
                context.context->nextNodeId(), std::move(partial_limit), PlanNodes{exchange_child}, node->getStatistics());
            exchange_children.emplace_back(partial_limit_node);

            should_apply = true;
        }
    }

    if (!should_apply)
        return {};

    auto exchange_step = old_exchange_step->copy(context.context);
    auto exchange_node = PlanNodeBase::createPlanNode(
        context.context->nextNodeId(), std::move(exchange_step), exchange_children, old_exchange_node->getStatistics());

    node->replaceChildren({exchange_node});
    return node;
}

}
