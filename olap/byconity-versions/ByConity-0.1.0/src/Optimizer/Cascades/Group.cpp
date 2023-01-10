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

#include <Optimizer/Cascades/Group.h>

#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/Cascades/GroupExpression.h>
#include <Optimizer/Rule/Transformation/JoinEnumOnGraph.h>
#include <QueryPlan/CTERefStep.h>
#include <QueryPlan/AnyStep.h>

namespace DB
{
void Group::addExpression(const GroupExprPtr & expression, CascadesContext & context)
{
    expression->setGroupId(id);

    if (expression->isPhysical())
    {
        physical_expressions.emplace_back(expression);
    }

    if (expression->isLogical())
    {
        logical_expressions.emplace_back(expression);
        if (!(expression->getStep()->getType() == IQueryPlanStep::Type::Join
              && dynamic_cast<const JoinStep &>(*expression->getStep()).supportReorder(context.isSupportFilter())))
        {
            join_sets.insert(JoinSet(id));
        }

        if (expression->getStep()->getType() == IQueryPlanStep::Type::Join)
        {
            simple_children = false;
            UInt32 children_table_scans = context.getMemo().getGroupById(expression->getChildrenGroups()[0])->max_table_scans
                + context.getMemo().getGroupById(expression->getChildrenGroups()[0])->max_table_scans;
            max_table_scans = std::max(max_table_scans, children_table_scans);
        }

        if (expression->getStep()->getType() == IQueryPlanStep::Type::TableScan)
        {
            is_table_scan = true;
            max_table_scans = std::max(max_table_scans, 1u);
        }
        if (expression->getStep()->getType() == IQueryPlanStep::Type::Projection && expression->getChildrenGroups().size() == 1)
        {
            is_table_scan = context.getMemo().getGroupById(expression->getChildrenGroups()[0])->isTableScan();
        }

        if (expression->getStep()->getType() == IQueryPlanStep::Type::CTERef)
        {
            const auto & with_clause_step = dynamic_cast<const CTERefStep &>(*expression->getStep());
            const auto & cte_def_contains_cte_ids = context.getMemo().getCTEDefGroupByCTEId(with_clause_step.getId())->getCTESet();
            cte_set.emplace(with_clause_step.getId());
            cte_set.insert(cte_def_contains_cte_ids.begin(), cte_def_contains_cte_ids.end());
        }
        for (auto group_id : expression->getChildrenGroups())
            for (auto cte_id : context.getMemo().getGroupById(group_id)->getCTESet())
                cte_set.emplace(cte_id);
    }

    if (!stats_derived)
    {
        std::vector<PlanNodeStatisticsPtr> children_stats;
        std::vector<bool> is_table_scans;
        for (const auto & child : expression->getChildrenGroups())
        {
            children_stats.emplace_back(context.getMemo().getGroupById(child)->getStatistics().value_or(nullptr));
            simple_children &= context.getMemo().getGroupById(child)->isSimpleChildren();
            is_table_scans.emplace_back(context.getMemo().getGroupById(child)->isTableScan());
        }
        statistics = CardinalityEstimator::estimate(
            expression->getStep(), context.getCTEInfo(), children_stats, context.getContext(), simple_children, is_table_scans);

        stats_derived = true;
    }

    if (!equivalences)
    {
        if (context.getContext()->getSettingsRef().enable_equivalences)
        {
            std::vector<SymbolEquivalencesPtr> children;
            for (const auto & child : expression->getChildrenGroups())
            {
                children.emplace_back(context.getMemo().getGroupById(child)->getEquivalences());
            }
            equivalences = SymbolEquivalencesDeriver::deriveEquivalences(expression->getStep(), children);
        }
        else
        {
            equivalences = std::make_shared<SymbolEquivalences>();
        }
    }
}

bool Group::setExpressionCost(const WinnerPtr & expr, const Property & property)
{
    auto it = lowest_cost_expressions.find(property);
    if (it == lowest_cost_expressions.end())
    {
        // not exist so insert
        lowest_cost_expressions[property] = expr;
        return true;
    }

    if (it->second->getCost() > expr->getCost() + 1e-2)
    {
        // this is lower cost
        lowest_cost_expressions[property] = expr;
        return true;
    }

    return false;
}

void Group::deleteExpression(const GroupExprPtr & expression)
{
    expression->setDeleted(true);
    for (auto itr = lowest_cost_expressions.begin(); itr != lowest_cost_expressions.end(); ++itr)
    {
        if (itr->second->getGroupExpr() == expression)
        {
            lowest_cost_expressions.erase(itr);
            break;
        }
    }
    // TODO: join_sets
}

void Group::deleteAllExpression()
{
    for (const auto & expr : getLogicalExpressions())
    {
        expr->setDeleted(true);
    }
    for (const auto & expr : getPhysicalExpressions())
    {
        expr->setDeleted(true);
    }
    lowest_cost_expressions.clear();
    cost_lower_bound = -1;
    join_sets.clear();
}

PlanNodePtr Group::createLeafNode(ContextMutablePtr context) const
{
    auto leaf_step = std::make_shared<AnyStep>(getStep()->getOutputStream(), id);
    return AnyNode::createPlanNode(context->nextNodeId(), std::move(leaf_step));
}


}
