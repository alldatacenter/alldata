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

#include <Optimizer/Rewriter/RemoveUnusedCTE.h>

#include <Optimizer/PlanNodeSearcher.h>
#include <QueryPlan/SimplePlanRewriter.h>

namespace DB
{

class RemoveUnusedCTE::Rewriter : public SimplePlanRewriter<Void>
{
public:
    explicit Rewriter(
        ContextMutablePtr & context_,
        CTEInfo & cte_info_,
        std::unordered_map<CTEId, UInt64> & common_table_expression_ref_counts_)
        : SimplePlanRewriter(context_, cte_info_)
        , common_table_expression_ref_counts(common_table_expression_ref_counts_)
    {
    }

    PlanNodePtr visitPlanNode(PlanNodeBase & node, Void & c) override
    {
        PlanNodes children;
        for (const auto & item : node.getChildren())
        {
            PlanNodePtr child = VisitorUtil::accept(*item, *this, c);
            children.emplace_back(child);
        }
        node.replaceChildren(children);
        return node.shared_from_this();
    }

    PlanNodePtr visitCTERefNode(CTERefNode & node, Void & c) override
    {
        const auto * with_step = dynamic_cast<const CTERefStep *>(node.getStep().get());
        if (common_table_expression_ref_counts[with_step->getId()] >= 2)
        {
            cte_helper.acceptAndUpdate(with_step->getId(), *this, c);
            return node.shared_from_this();
        }

        auto inlined_plan = with_step->toInlinedPlanNode(cte_helper.getCTEInfo(), context);
        auto res = VisitorUtil::accept(inlined_plan, *this, c);
        return res;
    }

private:
    std::unordered_map<CTEId, UInt64> & common_table_expression_ref_counts;
};

static void removeUnusedCTEFromCTEInfo(CTEInfo & cte_info, std::unordered_map<CTEId, UInt64> & cte_reference_counts)
{
    auto & cte_map = cte_info.getCTEs();
    for (auto it = cte_map.begin(); it != cte_map.end();)
    {
        if (cte_reference_counts[it->first] < 2)
            it = cte_map.erase(it);
        else
            ++it;
    }
}

void RemoveUnusedCTE::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    auto & cte_info = plan.getCTEInfo();
    if (cte_info.empty())
        return;
    auto cte_reference_counts = cte_info.collectCTEReferenceCounts(plan.getPlanNode());
    Rewriter rewriter(context, cte_info, cte_reference_counts);
    Void c;
    auto result = VisitorUtil::accept(plan.getPlanNode(), rewriter, c);
    removeUnusedCTEFromCTEInfo(cte_info, cte_reference_counts);
    plan.update(result);
}
}
