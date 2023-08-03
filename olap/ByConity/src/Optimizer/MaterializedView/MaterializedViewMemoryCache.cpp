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

#include <Optimizer/MaterializedView/MaterializedViewMemoryCache.h>

#include <Analyzers/QueryAnalyzer.h>
#include <Analyzers/QueryRewriter.h>
#include <Interpreters/InterpreterSelectQueryUseOptimizer.h>
#include <Interpreters/SegmentScheduler.h>
#include <Optimizer/Iterative/IterativeRewriter.h>
#include <Optimizer/PlanOptimizer.h>
#include <Optimizer/Rewriter/PredicatePushdown.h>
#include <Optimizer/Rule/Rules.h>
#include <QueryPlan/QueryPlanner.h>
#include <Storages/StorageMaterializedView.h>

namespace DB
{
MaterializedViewMemoryCache & MaterializedViewMemoryCache::instance()
{
    static MaterializedViewMemoryCache cache;
    return cache;
}

std::optional<MaterializedViewStructurePtr>
MaterializedViewMemoryCache::getMaterializedViewStructure(const StorageID & database_and_table_name, ContextMutablePtr context)
{
    auto dependent_table = DatabaseCatalog::instance().tryGetTable(database_and_table_name, context);
    if (!dependent_table)
        return {};

    auto materialized_view = dynamic_pointer_cast<StorageMaterializedView>(dependent_table);
    if (!materialized_view)
        return {};

    auto query_ptr = QueryRewriter::rewrite(materialized_view->getInnerQuery(), context, false);
    AnalysisPtr analysis = QueryAnalyzer::analyze(query_ptr, context);

    if (!analysis->non_deterministic_functions.empty())
        return {};

    QueryPlanPtr query_plan = QueryPlanner::plan(query_ptr, *analysis, context);

    static Rewriters rewriters
        = {std::make_shared<PredicatePushdown>(),
           std::make_shared<IterativeRewriter>(Rules::simplifyExpressionRules(), "SimplifyExpression"),
           std::make_shared<IterativeRewriter>(Rules::removeRedundantRules(), "RemoveRedundant"),
           std::make_shared<IterativeRewriter>(Rules::inlineProjectionRules(), "InlineProjection"),
           std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression")};

    for (auto & rewriter : rewriters)
        rewriter->rewrite(*query_plan, context);

    GraphvizPrinter::printLogicalPlan(*query_plan, context, "MaterializedViewMemoryCache");
    return MaterializedViewStructure::buildFrom(*materialized_view, query_plan->getPlanNode(), context);
}
}
