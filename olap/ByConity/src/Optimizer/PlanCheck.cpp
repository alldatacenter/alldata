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

#include <Optimizer/PlanCheck.h>

#include <Analyzers/TypeAnalyzer.h>

namespace DB
{
void PlanCheck::checkInitPlan(QueryPlan & plan, ContextMutablePtr context)
{
    // As init plan may contain correlated symbols, pass check filter.
    SymbolChecker::check(plan, context, false);
}

void PlanCheck::checkFinalPlan(QueryPlan & plan, ContextMutablePtr context)
{
    SymbolChecker::check(plan, context, true);
}

void ReadNothingChecker::check(PlanNodePtr plan)
{
    // if the whole plan is simplify to ReadNothingNode, return.
    if (plan->getStep()->getType() == IQueryPlanStep::Type::ReadNothing)
    {
        return;
    }
    ReadNothingChecker read_nothing_check;
    Void context{};

    // if sub-plan contains ReadNothing Node, throw Exception.
    VisitorUtil::accept(plan, read_nothing_check, context);
}

Void ReadNothingChecker::visitPlanNode(PlanNodeBase & node, Void & context)
{
    for (const auto & item : node.getChildren())
    {
        VisitorUtil::accept(*item, *this, context);
    }
    return {};
}

Void ReadNothingChecker::visitReadNothingNode(ReadNothingNode &, Void &)
{
    throw Exception("ReadNothingNode must removed in query optimization", ErrorCodes::LOGICAL_ERROR);
}

void SymbolChecker::check(QueryPlan & plan, ContextMutablePtr & context, bool check_filter)
{
    SymbolChecker symbol_check{check_filter};
    VisitorUtil::accept(plan.getPlanNode(), symbol_check, context);
}

Void SymbolChecker::visitPlanNode(PlanNodeBase & node, ContextMutablePtr & context)
{
    for (const auto & item : node.getChildren())
    {
        VisitorUtil::accept(*item, *this, context);
    }
    return {};
}

Void SymbolChecker::visitProjectionNode(ProjectionNode & node, ContextMutablePtr & context)
{
    VisitorUtil::accept(node.getChildren()[0], *this, context);
    const auto & step = *node.getStep();
    auto assignments = step.getAssignments();
    const auto & input_header = node.getChildren()[0]->getStep()->getOutputStream().header;
    auto names_and_types = input_header.getNamesAndTypes();
    auto type_analyzer = TypeAnalyzer::create(context, names_and_types);
    for (auto & assignment : assignments)
    {
        ConstASTPtr value = assignment.second;
        type_analyzer.getType(value);
    }
    return {};
}

Void SymbolChecker::visitFilterNode(FilterNode & node, ContextMutablePtr & context)
{
    VisitorUtil::accept(node.getChildren()[0], *this, context);

    if (check_filter)
    {
        const auto & step = *node.getStep();
        auto predicate = step.getFilter();
        const auto & input_header = node.getChildren()[0]->getStep()->getOutputStream().header;
        TypeAnalyzer::getType(predicate, context, input_header.getNamesAndTypes());
    }
    return {};
}

}
