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
#include <QueryPlan/CTEVisitHelper.h>
#include <QueryPlan/PlanVisitor.h>

namespace DB
{
template <typename C>
class SimplePlanVisitor : public PlanNodeVisitor<Void, C>
{
public:
    explicit SimplePlanVisitor(CTEInfo & cte_info) : cte_helper(cte_info) { }

    Void visitPlanNode(PlanNodeBase & node, C & c) override
    {
        for (const auto & child : node.getChildren())
            VisitorUtil::accept(*child, *this, c);
        return Void{};
    }

    Void visitCTERefNode(CTERefNode & node, C & c) override
    {
        const auto *cte_step = dynamic_cast<const CTERefStep *>(node.getStep().get());
        auto cte_id = cte_step->getId();
        cte_helper.accept(cte_id, *this, c);
        return Void{};
    }

protected:
    CTEPreorderVisitHelper cte_helper;
};
}
