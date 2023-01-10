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

#include <Optimizer/Cascades/Memo.h>

#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <QueryPlan/AnyStep.h>

namespace DB
{
GroupExprPtr Memo::insertGroupExpr(GroupExprPtr group_expr, CascadesContext & context, GroupId target)
{
    // If leaf, then just return
    if (group_expr->getStep()->getType() == IQueryPlanStep::Type::Any)
    {
        const auto * leaf = dynamic_cast<const AnyStep *>(group_expr->getStep().get());
        group_expr->setGroupId(leaf->getGroupId());
        return nullptr;
    }

    if (group_expr->getStep()->getType() == IQueryPlanStep::Type::Join)
    {
        const auto * join = dynamic_cast<const JoinStep *>(group_expr->getStep().get());
        if (join->isMagic())
        {
            groups[group_expr->getChildrenGroups()[1]]->setMagic(true);
        }
    }

    group_expr->setGroupId(target);
    // Lookup in hash table
    auto it = group_expressions.find(group_expr);
    // duplicate group expression
    if (it != group_expressions.end())
    {
        return *it;
    }

    group_expressions.insert(group_expr);

    // New expression, so try to insert into an existing group or
    // create a new group if none specified
    GroupId group_id;
    if (target == UNDEFINED_GROUP)
    {
        group_id = addNewGroup();
        // LOG_DEBUG(context.getLog(), "New Group Id " << group_id << "; Rule Type: "
        //                                             << static_cast<int>(group_expr->getProducerRule()));
    }
    else
    {
        group_id = target;
    }

    auto group = getGroupById(group_id);
    group->addExpression(group_expr, context);
    return group_expr;
}


}
