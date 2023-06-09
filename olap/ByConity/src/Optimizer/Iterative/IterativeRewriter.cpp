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

#include <Optimizer/Iterative/IterativeRewriter.h>
#include <Optimizer/Rule/Patterns.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int OPTIMIZER_TIMEOUT;
}

IterativeRewriter::IterativeRewriter(const std::vector<RulePtr> & rules_, std::string names_) : names(std::move(names_))
{
    for (const auto & rule : rules_)
    {
        auto target_type = rule->getTargetType();

        if (target_type != IQueryPlanStep::Type::Any)
            rules[target_type].emplace_back(rule);
        else
        {
            // for rules targeted to arbitrary type, copy them into each specific type's index
#define ADD_RULE_TO_INDEX(ITEM) rules[IQueryPlanStep::Type::ITEM].emplace_back(rule);

            APPLY_STEP_TYPES(ADD_RULE_TO_INDEX)

#undef ADD_RULE_TO_INDEX
        }
    }
}

void IterativeRewriter::rewrite(QueryPlan & plan, ContextMutablePtr ctx) const
{
    IterativeRewriterContext context{
        .globalContext = ctx,
        .cte_info = plan.getCTEInfo(),
        .start_time = std::chrono::system_clock::now(),
        .optimizer_timeout = ctx->getSettingsRef().iterative_optimizer_timeout};

    for (auto & item : plan.getCTEInfo().getCTEs())
        explorePlan(item.second, context);
    explorePlan(plan.getPlanNode(), context);
}

bool IterativeRewriter::explorePlan(PlanNodePtr & plan, IterativeRewriterContext & ctx) const // NOLINT(misc-no-recursion)
{
    bool progress = exploreNode(plan, ctx);

    while (plan && exploreChildren(plan, ctx))
    {
        progress = true;

        if (!exploreNode(plan, ctx))
            break;
    }

    return progress;
}

bool IterativeRewriter::exploreNode(PlanNodePtr & node, IterativeRewriterContext & ctx) const
{
    bool progress = false;
    bool done = false;

    while (node && !done)
    {
        done = true;

        auto node_type = node->getStep()->getType();
        if (auto res = rules.find(node_type); res != rules.end())
        {
            const auto & rules_of_this_type = res->second;
            for (auto iter = rules_of_this_type.begin();
                 // we can break the loop if the sub-plan has been entirely removed or the node type has been changed
                 node && node->getStep()->getType() == node_type && iter != rules_of_this_type.end();
                 ++iter)
            {
                const auto & rule = *iter;
                if (!rule->isEnabled(ctx.globalContext))
                    continue;

                checkTimeoutNotExhausted(rule->getName(), ctx);

                RuleContext rule_context{.context = ctx.globalContext, .cte_info = ctx.cte_info};
                auto rewrite_result = rule->transform(node, rule_context);

                if (!rewrite_result.empty())
                {
                    node = rewrite_result.getPlans()[0];
                    done = false;
                    progress = true;
                }
            }
        }
    }

    return progress;
}

bool IterativeRewriter::exploreChildren(PlanNodePtr & plan, IterativeRewriterContext & ctx) const // NOLINT(misc-no-recursion)
{
    bool progress = false;

    PlanNodes children;
    DataStreams inputs;

    for (PlanNodePtr child : plan->getChildren())
    {
        progress |= explorePlan(child, ctx);

        if (child)
        {
            children.emplace_back(child);
            inputs.push_back(child->getStep()->getOutputStream());
        }
    }

    if (progress)
    {
        plan->replaceChildren(children);
        auto new_step = plan->getStep()->copy(ctx.globalContext);
        new_step->setInputStreams(inputs);
        plan->setStep(new_step);
    }

    return progress;
}

void IterativeRewriter::checkTimeoutNotExhausted(const String & rule_name, const IterativeRewriterContext & context)
{
    auto now = std::chrono::system_clock::now();
    UInt64 elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - context.start_time).count();

    if (elapsed >= context.optimizer_timeout)
    {
        throw Exception(
            "The optimizer with rule [ " + rule_name + " ] exhausted the time limit of " + std::to_string(context.optimizer_timeout)
                + " ms",
            ErrorCodes::OPTIMIZER_TIMEOUT);
    }
}

}
