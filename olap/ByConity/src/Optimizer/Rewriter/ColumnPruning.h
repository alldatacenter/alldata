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

#include <Optimizer/Rewriter/Rewriter.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/SimplePlanRewriter.h>

namespace DB
{
class ColumnPruning : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "ColumnPruning"; }
};

class ColumnPruningVisitor : public SimplePlanRewriter<NameSet>
{
public:
    explicit ColumnPruningVisitor(ContextMutablePtr context_, CTEInfo & cte_info_, PlanNodePtr & root)
        : SimplePlanRewriter(context_, cte_info_), post_order_cte_helper(cte_info_, root)
    {
    }

private:
    PlanNodePtr visitLimitByNode(LimitByNode & node, NameSet & context) override;
    PlanNodePtr visitWindowNode(WindowNode & node, NameSet & context) override;
    PlanNodePtr visitDistinctNode(DistinctNode & node, NameSet & context) override;
    PlanNodePtr visitJoinNode(JoinNode & node, NameSet & context) override;
    PlanNodePtr visitSortingNode(SortingNode & node, NameSet & require) override;
    PlanNodePtr visitMergeSortingNode(MergeSortingNode & node, NameSet & context) override;
    PlanNodePtr visitMergingSortedNode(MergingSortedNode & node, NameSet & context) override;
    PlanNodePtr visitPartialSortingNode(PartialSortingNode & node, NameSet & context) override;
    PlanNodePtr visitAggregatingNode(AggregatingNode & node, NameSet & context) override;
    PlanNodePtr visitTableScanNode(TableScanNode & node, NameSet & context) override;
    PlanNodePtr visitFilterNode(FilterNode & node, NameSet & c) override;
    PlanNodePtr visitProjectionNode(ProjectionNode & node, NameSet & c) override;
    PlanNodePtr visitApplyNode(ApplyNode & node, NameSet & c) override;
    PlanNodePtr visitUnionNode(UnionNode & node, NameSet & context) override;
    PlanNodePtr visitExceptNode(ExceptNode & node, NameSet & context) override;
    PlanNodePtr visitIntersectNode(IntersectNode & node, NameSet & context) override;
    PlanNodePtr visitAssignUniqueIdNode(AssignUniqueIdNode & node, NameSet & context) override;
    PlanNodePtr visitExchangeNode(ExchangeNode & node, NameSet & context) override;
    PlanNodePtr visitCTERefNode(CTERefNode & node, NameSet & context) override;

    CTEPostorderVisitHelper post_order_cte_helper;
    std::unordered_map<CTEId, NameSet> cte_require_columns{};
};

}
