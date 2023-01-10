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

#include <Optimizer/Cascades/GroupExpression.h>

#include <Functions/FunctionsHashing.h>
#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/Cascades/Memo.h>
#include <Optimizer/Rule/Patterns.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/AnyStep.h>

namespace DB
{
size_t GroupExpression::hash()
{
    size_t hash = step->hash();
    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(child_groups.size()));
    for (auto child_group : child_groups)
    {
        hash = MurmurHash3Impl64::combineHashes(hash, child_group);
    }
    return hash;
}

GroupBindingIterator::GroupBindingIterator(const Memo & memo_, GroupId id_, PatternPtr pattern_, OptContextPtr context_)
    : BindingIterator(memo_, std::move(context_))
    , group_id(id_)
    , pattern(std::move(pattern_))
    , target_group(memo.getGroupById(id_))
    , num_group_items(target_group->getLogicalExpressions().size())
    , current_item_index(0)
{
}
bool GroupBindingIterator::hasNext()
{
    if (pattern->getTargetType() == IQueryPlanStep::Type::Any || pattern->getTargetType() == IQueryPlanStep::Type::Tree)
    {
        return current_item_index == 0;
    }

    if (current_iterator)
    {
        // Check if still have bindings in current item
        if (!current_iterator->hasNext())
        {
            current_iterator.reset(nullptr);
            current_item_index++;
        }
    }

    if (current_iterator == nullptr)
    {
        // Keep checking item iterators until we find a match
        while (current_item_index < num_group_items)
        {
            auto group_expr = target_group->getLogicalExpressions()[current_item_index];
            current_iterator = std::make_unique<GroupExprBindingIterator>(memo, group_expr, pattern, context);

            if (current_iterator->hasNext())
            {
                break;
            }

            current_iterator.reset(nullptr);
            current_item_index++;
        }
    }
    return current_iterator != nullptr;
}

PlanNodePtr GroupBindingIterator::next()
{
    if (pattern->getTargetType() == IQueryPlanStep::Type::Any || pattern->getTargetType() == IQueryPlanStep::Type::Tree)
    {
        current_item_index = num_group_items;
        PlanNodes children;
        const auto & statistics = memo.getGroupById(group_id)->getStatistics();
        return PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            std::make_shared<AnyStep>(context->getMemo().getGroupById(group_id)->getStep()->getOutputStream(), group_id),
            children,
            statistics);
    }

    return current_iterator->next();
}

GroupExprBindingIterator::GroupExprBindingIterator(
    const Memo & memo_, GroupExprPtr group_expr_, const PatternPtr & pattern, OptContextPtr context_)
    : BindingIterator(memo_, std::move(context_))
    , group_expr(std::move(group_expr_))
    , first(true)
    , has_next(false)
    , current_binding(nullptr)
{
    if (group_expr->getStep()->getType() != pattern->getTargetType())
    {
        // Check root node type
        return;
    }

    if (group_expr->isDeleted())
    {
        return;
    }

    const auto & child_groups = group_expr->getChildrenGroups();
    auto child_patterns = pattern->getChildrenPatterns();

    if (child_patterns.empty())
    {
        child_patterns.resize(child_groups.size(), Patterns::any());
    }

    // Find all bindings for children
    children_bindings.resize(child_patterns.size());
    children_bindings_pos.resize(child_patterns.size(), 0);

    // Get first level children
    PlanNodes children;

    for (size_t i = 0; i < child_patterns.size(); ++i)
    {
        // Try to find a match in the given group
        PlanNodes & child_bindings = children_bindings[i];
        GroupBindingIterator iterator(memo, child_groups[i], child_patterns[i], context);

        // Get all bindings
        while (iterator.hasNext())
        {
            child_bindings.emplace_back(iterator.next());
        }

        if (child_bindings.empty())
        {
            // Child binding failed
            return;
        }

        // Push a copy
        children.emplace_back(child_bindings[0]->copy(context->nextNodeId(), context->getOptimizerContext().getContext()));
    }

    has_next = true;
    const auto & statistics = memo.getGroupById(group_expr->getGroupId())->getStatistics();
    current_binding = PlanNodeBase::createPlanNode(context->nextNodeId(), group_expr->getStep(), std::move(children), statistics);
}

bool GroupExprBindingIterator::hasNext()
{
    if (group_expr->isDeleted())
    {
        return false;
    }

    if (has_next && first)
    {
        first = false;
        return true;
    }

    if (has_next)
    {
        // The first child to be modified
        Int64 first_modified_idx = children_bindings_pos.size() - 1;
        for (; first_modified_idx >= 0; --first_modified_idx)
        {
            PlanNodes & child_binding = children_bindings[first_modified_idx];

            // Try to increment idx from the back
            size_t new_pos = ++children_bindings_pos[first_modified_idx];
            if (new_pos >= child_binding.size())
            {
                children_bindings_pos[first_modified_idx] = 0;
            }
            else
            {
                break;
            }
        }

        if (first_modified_idx < 0)
        {
            // We have explored all combinations of the child bindings
            has_next = false;
        }
        else
        {
            PlanNodes children;
            for (size_t idx = 0; idx < children_bindings_pos.size(); ++idx)
            {
                PlanNodes & child_binding = children_bindings[idx];
                children.emplace_back(
                    child_binding[children_bindings_pos[idx]]->copy(context->nextNodeId(), context->getOptimizerContext().getContext()));
            }

            const auto & statistics = memo.getGroupById(group_expr->getGroupId())->getStatistics();
            current_binding = PlanNodeBase::createPlanNode(context->nextNodeId(), group_expr->getStep(), children, statistics);
        }
    }

    return has_next;
}


PlanNodePtr Winner::buildPlanNode(CascadesContext & context, PlanNodes & children)
{
    auto stats = context.getMemo().getGroupById(group_expr->getGroupId())->getStatistics();
    auto plan_node = PlanNodeBase::createPlanNode(context.getContext()->nextNodeId(), group_expr->getStep(), children);
    plan_node->setStatistics(stats);
    if (remote_exchange)
    {
        plan_node = PlanNodeBase::createPlanNode(context.getContext()->nextNodeId(), remote_exchange->getStep(), {plan_node});
        plan_node->setStatistics(stats);
    }
    if (local_exchange)
    {
        plan_node = PlanNodeBase::createPlanNode(context.getContext()->nextNodeId(), local_exchange->getStep(), {plan_node});
        plan_node->setStatistics(stats);
    }

    return plan_node;
}

}
