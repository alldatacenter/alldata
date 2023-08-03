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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Functions/FunctionFactory.h>
//#include <NavigationFunctions/NavigationFunctionFactory.h>
#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/PredicateUtils.h>

namespace DB
{
std::set<String> ExpressionDeterminism::getDeterministicSymbols(Assignments & assignments, ContextPtr context)
{
    std::set<String> deterministic_symbols;
    for (auto & assignment : assignments)
    {
        if (ExpressionDeterminism::isDeterministic(assignment.second, context))
        {
            deterministic_symbols.emplace(assignment.first);
        }
    }
    return deterministic_symbols;
}

ConstASTPtr ExpressionDeterminism::filterDeterministicConjuncts(ConstASTPtr predicate, ContextPtr context)
{
    if (predicate == PredicateConst::TRUE_VALUE || predicate == PredicateConst::FALSE_VALUE)
    {
        return predicate;
    }
    std::vector<ConstASTPtr> predicates = PredicateUtils::extractConjuncts(predicate);
    std::vector<ConstASTPtr> deterministic;
    for (auto & pre : predicates)
    {
        if (ExpressionDeterminism::isDeterministic(pre, context))
        {
            deterministic.emplace_back(pre);
        }
    }
    return PredicateUtils::combineConjuncts(deterministic);
}

ConstASTPtr ExpressionDeterminism::filterNonDeterministicConjuncts(ConstASTPtr predicate, ContextPtr context)
{
    std::vector<ConstASTPtr> predicates = PredicateUtils::extractConjuncts(predicate);
    std::vector<ConstASTPtr> non_deterministic;
    for (auto & pre : predicates)
    {
        if (!ExpressionDeterminism::isDeterministic(pre, context))
        {
            non_deterministic.emplace_back(pre);
        }
    }
    return PredicateUtils::combineConjuncts(non_deterministic);
}

std::set<ConstASTPtr> ExpressionDeterminism::filterDeterministicPredicates(std::vector<ConstASTPtr> & predicates, ContextPtr context)
{
    std::set<ConstASTPtr> deterministic;
    for (auto & predicate : predicates)
    {
        if (isDeterministic(predicate, context))
        {
            deterministic.emplace(predicate);
        }
    }
    return deterministic;
}

bool ExpressionDeterminism::isDeterministic(ConstASTPtr expression, ContextPtr context)
{
    return getExpressionProperty(std::move(expression), std::move(context)).is_deterministic;
}

bool ExpressionDeterminism::canChangeOutputRows(ConstASTPtr expression, ContextPtr context)
{
    return getExpressionProperty(std::move(expression), std::move(context)).can_change_output_rows;
}

ExpressionDeterminism::ExpressionProperty ExpressionDeterminism::getExpressionProperty(ConstASTPtr expression, ContextPtr context)
{
    bool is_deterministic = true;
    DeterminismVisitor visitor{is_deterministic};
    ASTVisitorUtil::accept(expression, visitor, context);
    return {.is_deterministic = visitor.isDeterministic(),
            .can_change_output_rows = visitor.canChangeOutputRows()};
}

DeterminismVisitor::DeterminismVisitor(bool isDeterministic) : is_deterministic(isDeterministic)
{
}

Void DeterminismVisitor::visitNode(const ConstASTPtr & node, ContextPtr & context)
{
    for (ConstASTPtr child : node->children)
    {
        ASTVisitorUtil::accept(child, *this, context);
    }
    return Void{};
}

Void DeterminismVisitor::visitASTFunction(const ConstASTPtr & node, ContextPtr & context)
{
    visitNode(node, context);
    const auto & fun = node->as<const ASTFunction &>();
    if (!context->isFunctionDeterministic(fun.name))
    {
        is_deterministic = false;
    }
    if (fun.name == "arrayJoin")
    {
        can_change_output_rows = true;
    }
    return Void{};
}

}
