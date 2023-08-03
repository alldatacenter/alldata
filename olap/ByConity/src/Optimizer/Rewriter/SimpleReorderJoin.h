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
class SimpleReorderJoin : public Rewriter
{
public:
    void rewrite(QueryPlan & plan, ContextMutablePtr context) const override;
    String name() const override { return "SimpleReorderJoin"; }
};

class SimpleReorderJoinVisitor : public SimplePlanRewriter<Void>
{
public:
    explicit SimpleReorderJoinVisitor(ContextMutablePtr context_, CTEInfo & cte_info_) : SimplePlanRewriter(context_, cte_info_), cte_info(cte_info_) { }
    PlanNodePtr visitJoinNode(JoinNode &, Void &) override;

private:
    CTEInfo & cte_info;
    std::unordered_set<PlanNodeId> reordered;
    PlanNodePtr getJoinOrder(JoinGraph & graph);
    PlanNodePtr buildJoinTree(std::vector<String> & expected_output_symbols, JoinGraph & graph, PlanNodePtr join_node);
};

struct EdgeSelectivity
{
    PlanNodeId left_id;
    PlanNodeId right_id;
    String left_symbol;
    String right_symbol;
    double selectivity;
    size_t output;
    size_t min_input;
};

struct EdgeSelectivityCompare
{
    bool operator()(const EdgeSelectivity & a, const EdgeSelectivity & b)
    {
        double a_s = a.selectivity > 0.98 ? 1 : a.selectivity;
        double b_s = b.selectivity > 0.98 ? 1 : b.selectivity;
        if (std::fabs(a_s - b_s) >= 1e-7)
            return a_s > b_s;

        if (a.output != b.output)
            return a.output > b.output;

        if (a.min_input != b.min_input)
            return a.min_input > b.min_input;

        if (a.left_symbol != b.left_symbol)
            return a.left_symbol < b.left_symbol;

        return a.right_symbol < b.right_symbol;
    }
};
}
