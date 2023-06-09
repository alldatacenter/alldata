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

#include <Optimizer/ExpressionExtractor.h>

#include <Optimizer/PredicateUtils.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
std::vector<ConstASTPtr> ExpressionExtractor::extract(PlanNodePtr & node)
{
    std::vector<ConstASTPtr> expressions;
    ExpressionVisitor visitor;
    VisitorUtil::accept(node, visitor, expressions);
    return expressions;
}

Void ExpressionVisitor::visitPlanNode(PlanNodeBase & node, std::vector<ConstASTPtr> & expressions)
{
    for (const auto & item : node.getChildren())
    {
        VisitorUtil::accept(*item, *this, expressions);
    }
    return Void{};
}

Void ExpressionVisitor::visitProjectionNode(ProjectionNode & node, std::vector<ConstASTPtr> & expressions)
{
    const auto & step = *node.getStep();
    auto assignments = step.getAssignments();
    for (auto & ass : assignments)
    {
        expressions.emplace_back(ass.second->clone());
    }
    return visitPlanNode(node, expressions);
}

Void ExpressionVisitor::visitFilterNode(FilterNode & node, std::vector<ConstASTPtr> & expressions)
{
    const auto & step = *node.getStep();
    ASTPtr filter = step.getFilter()->clone();
    expressions.emplace_back(filter);
    return visitPlanNode(node, expressions);
}

Void ExpressionVisitor::visitAggregatingNode(AggregatingNode & node, std::vector<ConstASTPtr> & expressions)
{
    const auto & step = *node.getStep();
    for (const auto & agg : step.getAggregates())
    {
        for (const auto & name : agg.argument_names)
        {
            expressions.emplace_back(std::make_shared<ASTIdentifier>(name));
        }
    }
    return visitPlanNode(node, expressions);
}

Void ExpressionVisitor::visitApplyNode(ApplyNode & node, std::vector<ConstASTPtr> & expressions)
{
    const auto & step = *node.getStep();
    expressions.emplace_back(step.getAssignment().second->clone());
    return visitPlanNode(node, expressions);
}

Void ExpressionVisitor::visitJoinNode(JoinNode & node, std::vector<ConstASTPtr> & expressions)
{
    const auto & step = *node.getStep();
    if (!PredicateUtils::isTruePredicate(step.getFilter()))
    {
        expressions.emplace_back(step.getFilter()->clone());
    }
    return visitPlanNode(node, expressions);
}

ConstASTSet SubExpressionExtractor::extract(ConstASTPtr node)
{
    ConstASTSet result;
    if (node == nullptr)
    {
        return result;
    }
    SubExpressionVisitor visitor;
    ASTVisitorUtil::accept(node, visitor, result);
    return result;
}

Void SubExpressionVisitor::visitNode(const ConstASTPtr & node, ConstASTSet & context)
{
    for (ConstASTPtr child : node->children)
    {
        ASTVisitorUtil::accept(child, *this, context);
    }
    return Void{};
}

Void SubExpressionVisitor::visitASTFunction(const ConstASTPtr & node, ConstASTSet & context)
{
    context.emplace(node);
    return visitNode(node, context);
}

Void SubExpressionVisitor::visitASTIdentifier(const ConstASTPtr & node, ConstASTSet & context)
{
    context.emplace(node);
    return Void{};
}

}
