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

#include <Optimizer/DistinctOutputUtil.h>

#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/ValuesStep.h>

namespace DB
{
bool DistinctOutputQueryUtil::isDistinct(PlanNodeBase & node)
{
    IsDistinctPlanVisitor visitor;
    Void context;
    return VisitorUtil::accept(node, visitor, context);
}

bool IsDistinctPlanVisitor::visitPlanNode(PlanNodeBase &, Void &)
{
    return false;
}

bool IsDistinctPlanVisitor::visitValuesNode(ValuesNode & node, Void &)
{
    return dynamic_cast<const ValuesStep *>(node.getStep().get())->getRows() <= 1;
}

bool IsDistinctPlanVisitor::visitLimitNode(LimitNode & node, Void &)
{
    return dynamic_cast<const LimitStep *>(node.getStep().get())->getLimit() <= 1;
}

bool IsDistinctPlanVisitor::visitIntersectNode(IntersectNode & node, Void & context)
{
    if (dynamic_cast<const IntersectStep *>(node.getStep().get())->isDistinct())
        return true;

    for (auto & child : node.getChildren())
    {
        if (!VisitorUtil::accept(child, *this, context))
            return false;
    }
    return true;
}

bool IsDistinctPlanVisitor::visitEnforceSingleRowNode(EnforceSingleRowNode &, Void &)
{
    return true;
}

bool IsDistinctPlanVisitor::visitAggregatingNode(AggregatingNode &, Void &)
{
    return true;
}

bool IsDistinctPlanVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode &, Void &)
{
    return true;
}

bool IsDistinctPlanVisitor::visitFilterNode(FilterNode & node, Void & context)
{
    return VisitorUtil::accept(node.getChildren()[0], *this, context);
}

bool IsDistinctPlanVisitor::visitDistinctNode(DistinctNode &, Void &)
{
    return true;
}

bool IsDistinctPlanVisitor::visitExceptNode(ExceptNode & node, Void & context)
{
    return dynamic_cast<const ExceptStep *>(node.getStep().get())->isDistinct() || VisitorUtil::accept(node.getChildren()[0], *this, context);
}

bool IsDistinctPlanVisitor::visitMergingSortedNode(MergingSortedNode & node, Void &)
{
    return dynamic_cast<const MergingSortedStep *>(node.getStep().get())->getLimit() <= 1;
}

}
