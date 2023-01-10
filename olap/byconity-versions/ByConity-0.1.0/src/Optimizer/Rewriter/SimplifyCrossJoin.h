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

#include <Interpreters/Context.h>
#include <Optimizer/JoinGraph.h>
#include <Optimizer/Rewriter/Rewriter.h>
#include <Parsers/ASTVisitor.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/SimplePlanRewriter.h>

#include <utility>

namespace DB
{
class SimplifyCrossJoin : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "SimplifyCrossJoin"; }
};

class SimplifyCrossJoinVisitor : public SimplePlanRewriter<Void>
{
public:
    explicit SimplifyCrossJoinVisitor(ContextMutablePtr context_, CTEInfo & cte_info) : SimplePlanRewriter(context_, cte_info) { }
    PlanNodePtr visitJoinNode(JoinNode &, Void &) override;

private:
    std::unordered_set<PlanNodeId> reordered;
    static bool isOriginalOrder(std::vector<UInt32> & join_order);
    static std::vector<UInt32> getJoinOrder(JoinGraph & graph);
    PlanNodePtr buildJoinTree(std::vector<String> & expected_output_symbols, JoinGraph & graph, std::vector<UInt32> & join_order);
};

class ComparePlanNode
{
public:
    std::unordered_map<PlanNodeId, UInt32> priorities;

    explicit ComparePlanNode(std::unordered_map<PlanNodeId, UInt32> priorities_) : priorities(std::move(priorities_)) { }

    bool operator()(const PlanNodePtr & node1, const PlanNodePtr & node2)
    {
        PlanNodeId id1 = node1->getId();
        PlanNodeId id2 = node2->getId();
        UInt32 value1 = priorities[id1];
        UInt32 value2 = priorities[id2];
        return value1 > value2;
    }
};

}
