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

#pragma once

#include <Optimizer/ExpressionDeterminism.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/QueryPlan.h>

namespace DB
{
/**
 * Check query or materialized view is valid for materialized view rewrite.
 * A query is not supported if
 * - query contains nodes except projection, join, table scan, filter.
 * - query contains multiple aggregates.
 * - query contains non-deterministic functions.
 */
class MaterializedViewStepChecker : public StepVisitor<bool, ContextMutablePtr>
{
public:
    static bool isSupported(const ConstQueryPlanStepPtr & step, ContextMutablePtr context)
    {
        static MaterializedViewStepChecker visitor;
        return VisitorUtil::accept(step, visitor, context);
    }

protected:
    bool visitStep(const IQueryPlanStep &, ContextMutablePtr &) override { return false; }

    bool visitAggregatingStep(const AggregatingStep & step, ContextMutablePtr &) override
    {
        if (step.isGroupingSet())
            return false;
        return true;
    }

    bool visitTableScanStep(const TableScanStep &, ContextMutablePtr &) override { return true; }

    bool visitProjectionStep(const ProjectionStep & step, ContextMutablePtr & context) override
    {
        for (auto & assigment : step.getAssignments())
            if (!ExpressionDeterminism::isDeterministic(assigment.second, context))
                return false;
        return true;
    }

    bool visitFilterStep(const FilterStep & step, ContextMutablePtr & context) override
    {
        if (!ExpressionDeterminism::isDeterministic(step.getFilter(), context))
            return false;
        return true;
    }

    bool visitJoinStep(const JoinStep &, ContextMutablePtr &) override { return true; }
};

class MaterializedViewPlanChecker : public PlanNodeVisitor<bool, ContextMutablePtr>
{
public:
    bool check(PlanNodeBase & node, ContextMutablePtr context) { return VisitorUtil::accept(node, *this, context); }

    [[nodiscard]] const PlanNodePtr & getTopAggregateNode() const { return top_aggregate_node; }

protected:
    bool visitPlanNode(PlanNodeBase & node, ContextMutablePtr & context) override {
        bool supported = MaterializedViewStepChecker::isSupported(node.getStep(), context);
        if (!supported)
            return false;
        return visitChildren(node, context);
    }

    bool visitAggregatingNode(AggregatingNode & node, ContextMutablePtr & context) override
    {
        if (!allow_top_aggregate_node)
            return false;

        allow_top_aggregate_node = false;
        top_aggregate_node = node.shared_from_this();
        return visitPlanNode(node, context);
    }

    bool visitChildren(PlanNodeBase & node, ContextMutablePtr & context)
    {
        if (allow_top_aggregate_node && node.getChildren().size() > 1)
            allow_top_aggregate_node = false;
        for (const auto & child : node.getChildren())
            if (!VisitorUtil::accept(*child, *this, context))
                return false;
        return true;
    }

private:
    bool allow_top_aggregate_node = true;
    PlanNodePtr top_aggregate_node = nullptr;
};
}
