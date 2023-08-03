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
#include <Optimizer/Rewriter/Rewriter.h>
#include <Optimizer/Equivalences.h>
#include <QueryPlan/SimplePlanVisitor.h>

namespace DB
{
class UnifyJoinOutputs : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "UnifyJoinOutputs"; }

private:
    class UnionFindExtractor;
    class Rewriter;
};

class UnifyJoinOutputs::UnionFindExtractor : public SimplePlanVisitor<std::unordered_map<PlanNodeId, UnionFind<String>>>
{
public:
    static std::unordered_map<PlanNodeId, UnionFind<String>> extract(QueryPlan & plan);
private:
    explicit UnionFindExtractor(CTEInfo & cte_info) : SimplePlanVisitor(cte_info) { }
    Void visitJoinNode(JoinNode &, std::unordered_map<PlanNodeId, UnionFind<String>> &) override;
    Void visitCTERefNode(CTERefNode & node, std::unordered_map<PlanNodeId, UnionFind<String>> & context) override;
};

class UnifyJoinOutputs::Rewriter : public PlanNodeVisitor<PlanNodePtr, std::set<String>>
{
public:
    Rewriter(ContextMutablePtr context_, CTEInfo & cte_info_, std::unordered_map<PlanNodeId, UnionFind<String>> & union_find_map_)
        : context(context_), cte_helper(cte_info_), union_find_map(union_find_map_)
    {
    }
    PlanNodePtr visitPlanNode(PlanNodeBase &, std::set<String> &) override;
    PlanNodePtr visitJoinNode(JoinNode &, std::set<String> &) override;
    PlanNodePtr visitCTERefNode(CTERefNode & node, std::set<String> &) override;

private:
    ContextMutablePtr context;
    CTEPreorderVisitHelper cte_helper;
    std::unordered_map<PlanNodeId, UnionFind<String>> & union_find_map;
};
}
