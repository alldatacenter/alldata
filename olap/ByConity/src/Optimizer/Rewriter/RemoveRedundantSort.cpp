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

#include <Optimizer/Rewriter/RemoveRedundantSort.h>

#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/CTERefStep.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/SortingStep.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/SymbolAllocator.h>
#include <QueryPlan/Void.h>
#include <Parsers/ASTFunction.h>
#include <Functions/FunctionFactory.h>

namespace DB
{
const std::unordered_set<String> RedundantSortVisitor::order_dependent_agg{"groupUniqArray","groupArray","groupArraySample","argMax","argMin","topK","topKWeighted","any","anyLast","anyHeavy",
                                                                           "first_value","last_value","deltaSum","deltaSumTimestamp","groupArrayMovingSum","groupArrayMovingAvg"};

void RemoveRedundantSort::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    if (!context->getSettingsRef().enable_redundant_sort_removal)
        return;
    RedundantSortVisitor visitor{context, plan.getCTEInfo(), plan.getPlanNode()};
    RedundantSortContext sort_context{.context = context, .can_sort_be_removed = false};
    auto result = VisitorUtil::accept(plan.getPlanNode(), visitor, sort_context);
    plan.update(result);
}

PlanNodePtr RedundantSortVisitor::processChildren(PlanNodeBase & node, RedundantSortContext & sort_context)
{
    if (node.getChildren().empty())
        return node.shared_from_this();

    PlanNodes children;
    DataStreams inputs;
    for (const auto & item : node.getChildren())
    {
        RedundantSortContext child_context{.context = sort_context.context, .can_sort_be_removed = sort_context.can_sort_be_removed};
        PlanNodePtr child = VisitorUtil::accept(*item, *this, child_context);
        children.emplace_back(child);
        inputs.push_back(child->getStep()->getOutputStream());
    }

    auto new_step = node.getStep()->copy(context);
    new_step->setInputStreams(inputs);
    node.setStep(new_step);
    node.replaceChildren(children);
    return node.shared_from_this();
}

PlanNodePtr RedundantSortVisitor::resetChild(PlanNodeBase & node, PlanNodes & children, RedundantSortContext & sort_context)
{
    DataStreams inputs;
    for (auto & child : children)
        inputs.push_back(child->getStep()->getOutputStream());
    auto new_step = node.getStep()->copy(sort_context.context);
    new_step->setInputStreams(inputs);
    node.setStep(new_step);
    node.replaceChildren(children);
    return node.shared_from_this();
}

bool RedundantSortVisitor::isOrderDependentAggregateFunction(const String& aggname)
{
    return order_dependent_agg.contains(aggname);
}

bool RedundantSortVisitor::isStateful(ConstASTPtr expression, ContextMutablePtr contextptr)
{
    StatefulVisitor visitor;
    ASTVisitorUtil::accept(expression, visitor, contextptr);
    return visitor.isStateful();
}

PlanNodePtr RedundantSortVisitor::visitProjectionNode(ProjectionNode & node, RedundantSortContext & sort_context)
{
    auto step = std::dynamic_pointer_cast<ProjectionStep>(node.getStep()->copy(sort_context.context));
    const Assignments & assignments = step->getAssignments();
    if (sort_context.can_sort_be_removed)
    {
        for (const auto & assignment: assignments)
        {
            if (RedundantSortVisitor::isStateful(assignment.second, sort_context.context))
            {
                sort_context.can_sort_be_removed = false;
                break;
            }
        }
    }

    return processChildren(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitAggregatingNode(AggregatingNode & node, RedundantSortContext & sort_context)
{
    auto step = node.getStep().get();
    const AggregateDescriptions & descs = step->getAggregates();

    bool is_order_dependent = false;
    for (auto & desc : descs)
    {
        if(isOrderDependentAggregateFunction(desc.function->getName()))
        {
            is_order_dependent = true;
            break;
        }
    }

    sort_context.can_sort_be_removed = !is_order_dependent;
    auto rewritten_child = VisitorUtil::accept(node.getChildren()[0], *this, sort_context);
    PlanNodes children{rewritten_child};
    return resetChild(node, children, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitJoinNode(JoinNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = true;
    return processChildren(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitUnionNode(UnionNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = true;
    return processChildren(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitIntersectNode(IntersectNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = true;
    return processChildren(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitExceptNode(ExceptNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = true;
    return processChildren(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitLimitNode(LimitNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = false;
    return visitPlanNode(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitLimitByNode(LimitByNode & node, RedundantSortContext & sort_context)
{
    sort_context.can_sort_be_removed = false;
    return visitPlanNode(node, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitSortingNode(SortingNode & node, RedundantSortContext & sort_context)
{
    bool remove_current = sort_context.can_sort_be_removed;
    sort_context.can_sort_be_removed = true;
    auto rewritten_child = VisitorUtil::accept(node.getChildren()[0], *this, sort_context);

    if (remove_current)
        return rewritten_child;

    PlanNodes children{rewritten_child};
    return resetChild(node, children, sort_context);
}

PlanNodePtr RedundantSortVisitor::visitCTERefNode(CTERefNode & node, RedundantSortContext & sort_context)
{
    CTEId cte_id = node.getStep()->getId();

    if (cte_require_context.contains(cte_id))
    {
        RedundantSortContext & context = cte_require_context.at(cte_id);
        context.can_sort_be_removed = context.can_sort_be_removed && sort_context.can_sort_be_removed;
    }
    else
    {
        cte_require_context.emplace(cte_id, sort_context);
    }

    RedundantSortContext child_context(cte_require_context.at(cte_id));
    auto cte_plan = post_order_cte_helper.acceptAndUpdate(cte_id, *this, child_context);

    auto new_step = std::dynamic_pointer_cast<CTERefStep>(node.getStep()->copy(sort_context.context));
    DataStreams input_streams;
    input_streams.emplace_back(cte_plan->getStep()->getOutputStream());
    new_step->setInputStreams(input_streams);
    node.setStep(new_step);
    return node.shared_from_this();
}

void StatefulVisitor::visitNode(const ConstASTPtr & node, ContextMutablePtr & context)
{
    for (ConstASTPtr child : node->children)
        ASTVisitorUtil::accept(child, *this, context);
}

void StatefulVisitor::visitASTFunction(const ConstASTPtr & node, ContextMutablePtr & context)
{
    auto & fun = node->as<const ASTFunction &>();
    const auto & function = FunctionFactory::instance().tryGet(fun.name, context);
    if (function && function->isStateful())
    {
        is_stateful = true;
        return;
    }
    visitNode(node, context);
}
}
