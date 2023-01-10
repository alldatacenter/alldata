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

#include <Optimizer/PlanOptimizer.h>

#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/Iterative/IterativeRewriter.h>
#include <Optimizer/PlanCheck.h>
#include <Optimizer/Rewriter/AddDynamicFilters.h>
#include <Optimizer/Rewriter/AddExchange.h>
#include <Optimizer/Rewriter/ColumnPruning.h>
#include <Optimizer/Rewriter/RemoveRedundantSort.h>
#include <Optimizer/Rewriter/PredicatePushdown.h>
#include <Optimizer/Rewriter/RemoveApply.h>
#include <Optimizer/Rewriter/RemoveUnusedCTE.h>
#include <Optimizer/Rewriter/SimpleReorderJoin.h>
#include <Optimizer/Rewriter/SimplifyCrossJoin.h>
#include <Optimizer/Rewriter/UnifyJoinOutputs.h>
#include <Optimizer/Rewriter/UnifyNullableType.h>
#include <Optimizer/Rewriter/RemoveUnusedCTE.h>
#include <Optimizer/Rewriter/MaterializedViewRewriter.h>
#include <Optimizer/Rule/Rules.h>
#include <QueryPlan/GraphvizPrinter.h>
#include <QueryPlan/PlanPattern.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int OPTIMIZER_NONSUPPORT;
    extern const int OPTIMIZER_TIMEOUT;
}

const Rewriters & PlanOptimizer::getSimpleRewriters()
{
    static Rewriters simple_rewrites = {
        // rules for normalize plan (DO NOT change !!!)
        std::make_shared<ColumnPruning>(),
        std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression"),
        std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
        std::make_shared<IterativeRewriter>(Rules::mergePredicatesRules(), "MergePredicates"),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
        std::make_shared<IterativeRewriter>(Rules::pushDownLimitRules(), "PushDownLimit"),
        std::make_shared<IterativeRewriter>(Rules::distinctToAggregateRules(), "DistinctToAggregate"),

        std::make_shared<RemoveRedundantSort>(),
        std::make_shared<PredicatePushdown>(),

        // normalize plan after predicate push down
        std::make_shared<ColumnPruning>(),
        std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
        std::make_shared<IterativeRewriter>(Rules::mergePredicatesRules(), "MergePredicates"),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),

        //add reorder adjacent windows
        std::make_shared<IterativeRewriter>(Rules::swapAdjacentRules(),"SwapAdjacent"),

        std::make_shared<MaterializedViewRewriter>(),

        // add exchange
        std::make_shared<AddExchange>(),
        std::make_shared<IterativeRewriter>(Rules::pushPartialStepRules(), "PushPartialStep"),
        std::make_shared<IterativeRewriter>(Rules::pushIntoTableScanRules(), "PushIntoTableScan"),
    };
    return simple_rewrites;
}

const Rewriters & PlanOptimizer::getFullRewriters()
{
    // the order of rules matters, DO NOT change.
    static Rewriters full_rewrites = {

        std::make_shared<RemoveUnusedCTE>(),
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),

        // remove subquery rely on specific pattern
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),

        // when correlated-subquery exists, we can't perform simplify expression actions, because
        // type analyzer relay on input columns to resolve the data type of identifiers. for correlated
        // symbols, it's unknown. after subquery removed, simplify expression is able to execute.
        // Normalize expression, like, common predicate rewrite, swap predicate rewrite, these rules
        // they don't need type analyzer.
        // Simplify expression, like, expression interpret, unwrap cast. these rules require type analyzer.
        std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression"),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),

        // rules for normalize Union/Except/Intersect
        std::make_shared<IterativeRewriter>(Rules::mergeSetRules(), "MergeSetNode"),
        std::make_shared<IterativeRewriter>(Rules::implementSetRules(), "ImplementSetNode"),

        // rules for remove subquery, the order of subquery rules matters, DO NOT change !!!.
        std::make_shared<RemoveUnCorrelatedInSubquery>(),
        std::make_shared<RemoveCorrelatedInSubquery>(),
        std::make_shared<RemoveUnCorrelatedExistsSubquery>(),
        std::make_shared<RemoveCorrelatedExistsSubquery>(),
        std::make_shared<RemoveUnCorrelatedScalarSubquery>(),
        std::make_shared<RemoveCorrelatedScalarSubquery>(),
        std::make_shared<RemoveUnCorrelatedQuantifiedComparisonSubquery>(),
        std::make_shared<RemoveCorrelatedQuantifiedComparisonSubquery>(),

        // rules after subquery removed, DO NOT change !!!.
        std::make_shared<IterativeRewriter>(Rules::pushDownLimitRules(), "PushDownLimit"),
        std::make_shared<IterativeRewriter>(Rules::distinctToAggregateRules(), "DistinctToAggregate"),
        std::make_shared<IterativeRewriter>(Rules::pushAggRules(), "PushAggregateThroughJoin"),

        std::make_shared<RemoveRedundantSort>(),

        // subquery remove may generate outer join, make sure data type is correct.
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),

        // predicate push down
        std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
        std::make_shared<IterativeRewriter>(Rules::mergePredicatesRules(), "MergePredicates"),
        std::make_shared<PredicatePushdown>(),

        // predicate push down may convert outer-join to inner-join, make sure data type is correct.
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),

        // Join graph requires projection inline/merge/pull up, as projection will break join graph.
        // Join graph will pull up predicates, hence, apply predicate push down after it.

        // simplify cross join
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),
        std::make_shared<SimplifyCrossJoin>(),
        std::make_shared<PredicatePushdown>(),
        // predicate push down may convert outer-join to inner-join, make sure data type is correct.
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),

        // simple join order (primary for large joins reorder)
        std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
        std::make_shared<IterativeRewriter>(Rules::mergePredicatesRules(), "MergePredicates"),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),
        std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression"),
        std::make_shared<SimpleReorderJoin>(),
        std::make_shared<PredicatePushdown>(),

        // predicate push down may convert outer-join to inner-join, make sure data type is correct.
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),

        // prepare for cascades
        std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
        std::make_shared<IterativeRewriter>(Rules::mergePredicatesRules(), "MergePredicates"),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),
        std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression"),
        std::make_shared<UnifyJoinOutputs>(),

        //add reorder adjacent windows
        std::make_shared<IterativeRewriter>(Rules::swapAdjacentRules(),"SwapAdjacent"),

        //
        std::make_shared<MaterializedViewRewriter>(),

        // Cost-based optimizer
        std::make_shared<CascadesOptimizer>(),

        // remove not inlined CTEs
        std::make_shared<RemoveUnusedCTE>(),

        // add runtime filters
        std::make_shared<AddDynamicFilters>(),

        // final UnifyNullableType, make sure type is correct.
        std::make_shared<ColumnPruning>(),
        std::make_shared<UnifyNullableType>(),
        std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
        std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),

        // push partial step through exchange
        // TODO cost-base partial aggregate push down
        std::make_shared<IterativeRewriter>(Rules::pushPartialStepRules(), "PushPartialStep"),
        // push predicate into storage
        std::make_shared<IterativeRewriter>(Rules::pushIntoTableScanRules(), "PushIntoTableScan"),
        // TODO cost-based projection push down
    };

    return full_rewrites;
}

void PlanOptimizer::optimize(QueryPlan & plan, ContextMutablePtr context)
{
    int i = GraphvizPrinter::PRINT_PLAN_OPTIMIZE_INDEX;
    GraphvizPrinter::printLogicalPlan(plan, context, std::to_string(i++) + "_Init_Plan");

    // Check init plan to satisfy with :
    // 1 Symbol exist check
    PlanCheck::checkInitPlan(plan, context);
    auto start = std::chrono::high_resolution_clock::now();

    auto rewrite = [&](const Rewriters & rewriters) {
        for (auto & rewriter : rewriters)
        {
            auto rewriter_begin = std::chrono::high_resolution_clock::now();
            rewriter->rewrite(plan, context);
            auto now = std::chrono::high_resolution_clock::now();
            UInt64 elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - start).count();
            UInt64 single_rewriter_duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - rewriter_begin).count();

            if (single_rewriter_duration >= 1000)
                LOG_WARNING(
                    &Poco::Logger::get("PlanOptimizer"),
                    "the execute time of " + rewriter->name() + " rewriter greater than or equal to 1 second");

            GraphvizPrinter::printLogicalPlan(
                plan, context, std::to_string(i++) + "_" + rewriter->name() + "_" + std::to_string(single_rewriter_duration) + "ms");

            if (elapsed >= context->getSettingsRef().plan_optimizer_timeout)
            {
                throw Exception(
                    "PlanOptimizer exhausted the time limit of " + std::to_string(context->getSettingsRef().plan_optimizer_timeout) + " ms",
                    ErrorCodes::OPTIMIZER_TIMEOUT);
            }
        }
    };

    if (PlanPattern::isSimpleQuery(plan))
    {
        rewrite(getSimpleRewriters());
    }
    else
    {
        rewrite(getFullRewriters());
    }

    // Check final plan to satisfy with :
    // 1 Symbol exist check
    PlanCheck::checkFinalPlan(plan, context);
}
}
