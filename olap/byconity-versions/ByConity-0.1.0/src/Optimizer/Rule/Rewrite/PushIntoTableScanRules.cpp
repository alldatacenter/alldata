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

#include <Optimizer/Rule/Rewrite/PushIntoTableScanRules.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/SymbolsExtractor.h>
#include <QueryPlan/SymbolMapper.h>
#include <QueryPlan/TableScanStep.h>
#include <QueryPlan/LimitStep.h>

namespace DB
{

PatternPtr PushFilterIntoTableScan::getPattern() const
{
    return Patterns::filter()->withSingle(
        Patterns::tableScan()->matchingStep<TableScanStep>([](const auto & step) { return !step.hasFilter(); }));
}

TransformResult PushFilterIntoTableScan::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto table_scan = node->getChildren()[0];

    auto filter_step = dynamic_cast<const FilterStep *>(node->getStep().get());
    auto filter_conjuncts = PredicateUtils::extractConjuncts(filter_step->getFilter());

    auto pushdown_filters = extractPushDownFilter(filter_conjuncts, rule_context.context);
    if (!pushdown_filters.empty())
    {
        auto copy_table_step = table_scan->getStep()->copy(rule_context.context);
        auto table_step = dynamic_cast<TableScanStep *>(copy_table_step.get());

        std::unordered_map<String, String> inv_alias;
        for (auto & item : table_step->getColumnAlias())
            inv_alias.emplace(item.second, item.first);

        auto mapper = SymbolMapper::symbolMapper(inv_alias);

        std::vector<ConstASTPtr> conjuncts;
        for (auto & filter : pushdown_filters)
        {
            bool all_in = true;
            auto symbols = SymbolsExtractor::extract(filter);
            for (const auto & item : symbols)
                all_in &= inv_alias.contains(item);

            if (all_in)
                conjuncts.emplace_back(mapper.map(filter));
        }

        bool applied = table_step->setFilter(conjuncts);
        if (!applied)
            return {}; // repeat calls
        table_scan->setStep(copy_table_step);
    }

    auto remaining_filters = removeStorageFilter(filter_conjuncts);
    if (remaining_filters.size() == filter_conjuncts.size())
        return {};

    ConstASTPtr new_predicate = PredicateUtils::combineConjuncts(remaining_filters);
    if (PredicateUtils::isTruePredicate(new_predicate))
        return table_scan;

    auto new_filter_step
        = std::make_shared<FilterStep>(table_scan->getStep()->getOutputStream(), new_predicate, filter_step->removesFilterColumn());
    return PlanNodeBase::createPlanNode(
        rule_context.context->nextNodeId(), std::move(new_filter_step), PlanNodes{table_scan}, node->getStatistics());
}

std::vector<ConstASTPtr> PushFilterIntoTableScan::extractPushDownFilter(const std::vector<ConstASTPtr> & conjuncts, ContextMutablePtr & context)
{
    std::vector<ConstASTPtr> filters;
    for (auto & conjunct : conjuncts)
    {
        if (auto dynamic_filter = DynamicFilters::extractDescription(conjunct))
        {
            auto & description = dynamic_filter.value();
            if (!DynamicFilters::isSupportedForTableScan(description))
                continue;
        }

        if (!ExpressionDeterminism::isDeterministic(conjunct, context))
            continue;

        filters.emplace_back(conjunct);
    }
    return filters;
}

std::vector<ConstASTPtr> PushFilterIntoTableScan::removeStorageFilter(const std::vector<ConstASTPtr> & conjuncts)
{
    std::vector<ConstASTPtr> remove_array_set_check;
    for (auto & conjunct : conjuncts)
    {
        // Attention !!!
        // arraySetCheck must push into storage, it is not executable in engine.
        if (conjunct->as<ASTFunction>())
        {
            const ASTFunction & fun = conjunct->as<const ASTFunction &>();
            if (fun.name == "arraySetCheck")
            {
                continue;
            }
        }
        remove_array_set_check.emplace_back(conjunct);
    }
    return remove_array_set_check;
}

PatternPtr PushLimitIntoTableScan::getPattern() const
{
    return Patterns::limit()
        ->matchingStep<LimitStep>([](auto const & limit_step) { return !limit_step.isAlwaysReadTillEnd(); })
        ->withSingle(Patterns::tableScan()->matchingStep<TableScanStep>([](const auto & step) { return !step.hasFilter(); }));
}

TransformResult PushLimitIntoTableScan::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto limit_step = dynamic_cast<const LimitStep *>(node->getStep().get());
    auto table_scan = node->getChildren()[0];

    auto copy_table_step = table_scan->getStep()->copy(rule_context.context);

    auto table_step = dynamic_cast<TableScanStep *>(copy_table_step.get());
    bool applied = table_step->setLimit(limit_step->getLimit() + limit_step->getOffset(), rule_context.context);
    if (!applied)
        return {}; // repeat calls

    table_scan->setStep(copy_table_step);
    node->replaceChildren({table_scan});
    return node;
}

}
