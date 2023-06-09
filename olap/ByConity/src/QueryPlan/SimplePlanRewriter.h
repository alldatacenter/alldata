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

#include <memory>
#include <Interpreters/Context.h>
#include <QueryPlan/CTEVisitHelper.h>
#include <QueryPlan/PlanVisitor.h>
#include <QueryPlan/CTERefStep.h>
#include <QueryPlan/PlanNode.h>

namespace DB
{
template <typename C>
class SimplePlanRewriter : public PlanNodeVisitor<PlanNodePtr, C>
{
public:
    SimplePlanRewriter(ContextMutablePtr context_, CTEInfo & cte_info) : context(std::move(context_)), cte_helper(cte_info) { }

    PlanNodePtr visitPlanNode(PlanNodeBase & node, C & c) override
    {

        if (node.getChildren().empty())
            return node.shared_from_this();
        PlanNodes children;
        DataStreams inputs;
        for (const auto & item : node.getChildren())
        {
            PlanNodePtr child = VisitorUtil::accept(*item, *this, c);
            children.emplace_back(child);
            inputs.push_back(child->getStep()->getOutputStream());
        }

        auto new_step = node.getStep()->copy(context);
        new_step->setInputStreams(inputs);
        node.setStep(new_step);

        node.replaceChildren(children);
        return node.shared_from_this();
    }

    PlanNodePtr visitCTERefNode(CTERefNode & node, C & c) override
    {
        auto cte_step = node.getStep();
        auto cte_id = cte_step->getId();
        auto cte_plan = cte_helper.acceptAndUpdate(cte_id, *this, c);
        return cte_plan;
    }

protected:
    ContextMutablePtr context;
    CTEPreorderVisitHelper cte_helper;
};
}
