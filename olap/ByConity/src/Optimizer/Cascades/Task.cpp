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

#include <Optimizer/Cascades/Task.h>

#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/CostModel/CostCalculator.h>
#include <Optimizer/CostModel/PlanNodeCost.h>
#include <Optimizer/Property/PropertyDeriver.h>
#include <Optimizer/Property/PropertyDeterminer.h>
#include <Optimizer/Property/PropertyEnforcer.h>
#include <Optimizer/Property/PropertyMatcher.h>
#include <Optimizer/Rule/Rule.h>
#include <QueryPlan/JoinStep.h>

namespace DB
{
void OptimizeGroup::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Optimize Group " << group->getId());

    if (group->getCostLowerBound() > context->getCostUpperBound() || // Cost LB > Cost UB
        group->hasWinner(context->getRequiredProp())) // Has optimized given the context
        return;

    // Push explore task first for logical expressions if the group has not been explored
    if (!group->hasExplored())
    {
        for (const auto & logical_expr : group->getLogicalExpressions())
        {
            pushTask(std::make_shared<OptimizeExpression>(logical_expr, context));
        }
    }

    // Push implement tasks to ensure that they are run first (for early pruning)
    for (const auto & physical_expr : group->getPhysicalExpressions())
    {
        pushTask(std::make_shared<OptimizeInput>(physical_expr, context));
    }

    // Since there is no cycle in the tree, it is safe to set the flag even before
    // all expressions are explored
    group->setExplorationFlag();
}

void OptimizeExpression::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Optimize GroupExpr " << group_expr->getGroupId());

    std::vector<RulePtr> valid_rules;

    // Construct valid transformation rules from rule set
    auto logical_rules = getTransformationRules();
    auto phys_rules = getImplementationRules();
    // If there are no stats, we won't enum plan
    if (context->getMemo().getGroupById(group_expr->getGroupId())->getStatistics())
    {
        constructValidRules(group_expr, logical_rules, valid_rules);
    }
    constructValidRules(group_expr, phys_rules, valid_rules);

    //    std::sort(valid_rules.begin(), valid_rules.end());
    // Apply rule
    for (auto & r : valid_rules)
    {
        pushTask(std::make_shared<ApplyRule>(group_expr, r, context));
        int child_group_idx = 0;
        for (const auto & child_pattern : r->getPattern()->getChildrenPatterns())
        {
            // If child_pattern has any more children (i.e non-leaf), then we will explore the
            // child before applying the rule. (assumes task pool is effectively a stack)
            if (!child_pattern->getChildrenPatterns().empty())
            {
                auto group = context->getMemo().getGroupById(group_expr->getChildrenGroups()[child_group_idx]);
                pushTask(std::make_shared<ExploreGroup>(group, context));
            }

            child_group_idx++;
        }
    }
}

void ExploreGroup::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Explore Group " << group->getId());

    if (group->hasExplored())
        return;

    for (const auto & logical_expr : group->getLogicalExpressions())
    {
        pushTask(std::make_shared<ExploreExpression>(logical_expr, context));
    }

    // Since there is no cycle in the tree, it is safe to set the flag even before
    // all expressions are explored
    group->setExplorationFlag();
}

void ExploreExpression::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Explore GroupExpr " << group_expr->getGroupId());
    std::vector<RulePtr> valid_rules;

    // Construct valid transformation rules from rule set
    auto logical_rules = getTransformationRules();
    // If there are no stats, we won't enum plan
    if (context->getMemo().getGroupById(group_expr->getGroupId())->getStatistics())
    {
        constructValidRules(group_expr, logical_rules, valid_rules);
    }

    //    std::sort(valid_rules.begin(), valid_rules.end());
    // Apply rule
    for (auto & r : valid_rules)
    {
        pushTask(std::make_shared<ApplyRule>(group_expr, r, context, true));
        int child_group_idx = 0;
        for (const auto & child_pattern : r->getPattern()->getChildrenPatterns())
        {
            // Only need to explore non-leaf children before applying rule to the
            // current group. this condition is important for early-pruning
            if (!child_pattern->getChildrenPatterns().empty())
            {
                auto group = context->getMemo().getGroupById(group_expr->getChildrenGroups()[child_group_idx]);
                pushTask(std::make_shared<ExploreGroup>(group, context));
            }

            child_group_idx++;
        }
    }
}

void ApplyRule::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Apply Rule GroupExpr " << group_expr->getGroupId());

    if (group_expr->hasRuleExplored(rule->getType()))
        return;

    GroupExprBindingIterator iterator(context->getMemo(), group_expr, rule->getPattern(), context);

    RuleContext rule_context{
        context->getOptimizerContext().getContext(), context->getOptimizerContext().getCTEInfo(), context, group_expr->getGroupId()};
    while (iterator.hasNext())
    {
        auto before = iterator.next();
        if (!rule->getPattern()->matches(before))
        {
            continue;
        }

        // Caller frees after
        TransformResult result = rule->transform(before, rule_context);

        if (result.isEraseAll())
        {
            auto group = context->getOptimizerContext().getMemo().getGroupById(group_expr->getGroupId());
            group->deleteAllExpression();
        }
        else if (result.isEraseOld())
        {
            auto group = context->getOptimizerContext().getMemo().getGroupById(group_expr->getGroupId());
            group->deleteExpression(group_expr);
        }

        for (const auto & new_expr : result.getPlans())
        {
            GroupExprPtr new_group_expr = nullptr;
            auto g_id = group_expr->getGroupId();
            if (context->getOptimizerContext().recordPlanNodeIntoGroup(new_expr, new_group_expr, rule->getType(), g_id))
            {
                // LOG_DEBUG(context->getOptimizerContext().getLog(), "Success Apply Rule For Expression In Group "
                //              << group_expr->getGroupId() << "; Rule Type: " << static_cast<int>(rule->getType()));

                for (auto type : rule->blockRules())
                {
                    new_group_expr->setRuleExplored(type);
                }
                // A new group expression is generated
                if (new_group_expr->isLogical())
                {
                    if (explore_only)
                    {
                        // Explore this logical expression
                        pushTask(std::make_shared<ExploreExpression>(new_group_expr, context));
                    }
                    else
                    {
                        // Optimize this logical expression
                        pushTask(std::make_shared<OptimizeExpression>(new_group_expr, context));
                    }
                }
                if (new_group_expr->isPhysical())
                {
                    // Cost this physical expression and optimize its inputs
                    pushTask(std::make_shared<OptimizeInput>(new_group_expr, context));
                }
            }
        }
    }

    group_expr->setRuleExplored(rule->getType());
}

void OptimizeInput::execute()
{
    //    LOG_DEBUG(context->getOptimizerContext().getLog(), "Optimize Input" << group_expr->getGroupId());

    // Init logic: only run once per task
    if (cur_child_idx == -1)
    {
        // TODO:
        // 1. We can init input cost using non-zero value for pruning
        // 2. We can calculate the current operator cost if we have maintain
        //    logical properties in group (e.g. stats, schema, cardinality)
        cur_total_cost = 0;

        // Pruning
        if (cur_total_cost > context->getCostUpperBound())
            return;

        if (group_expr->getStep()->getType() == IQueryPlanStep::Type::CTERef)
        {
            const auto cte_step = dynamic_cast<const CTERefStep *>(group_expr->getStep().get());
            CTEId cte_id = cte_step->getId();
            auto cte_def_group = context->getMemo().getCTEDefGroupByCTEId(cte_id);

            // 1. Check whether request property for this group_expr is invalid.
            if (!context->getRequiredProp().getCTEDescriptions().contains(cte_id))
                return;

            // 2. CTERefStep output property can not be determined locally, it has been determined globally,
            //  Described in CTEDescription of property. If They don't match, we just ignore required property.
            // eg, input required property: <Repartition[B], CTE(0)=Repartition[A]> don't match,
            //    we ignore local required property Repartition[B] and prefer global property Repartition[A]
            // see more DeriverVisitor::visitCTERefStep
            // 2-1. if CTEDef group hasn't been optimized for global determined property, we submit it here.
            auto cte_description_property = CTEDescription::createCTEDefGlobalProperty(context->getRequiredProp(), cte_id);
            if (!cte_def_group->hasWinner(cte_description_property))
            {
                if (wait_cte_optimization)
                    return; // We have optimized cte group but there is still no valid plan.
                wait_cte_optimization = true;
                pushTask(this->shared_from_this());
                auto ctx = std::make_shared<OptimizationContext>(context->getOptimizerContext(), cte_description_property);
                pushTask(std::make_shared<OptimizeGroup>(cte_def_group, ctx));
                return;
            }

            // 2-2. We save local required property, as we could re-optimize cte common property later.
            auto local_required_property
                = CTEDescription::createCTEDefLocalProperty(context->getRequiredProp(), cte_id, cte_step->getOutputColumns());
            context->getOptimizerContext().getCTEDefPropertyRequirements()[cte_id].emplace(local_required_property);
        }

        // Calc the CTEs that the children contains
        for (auto child_group_id : group_expr->getChildrenGroups())
            input_cte_ids.emplace_back(context->getMemo().getGroupById(child_group_id)->getCTESet());

        // Derive input properties
        exploreInputProperties();

        cur_child_idx = 0;
    }

    // Loop over (output prop, input props) pair for the GroupExpression being optimized
    // (1) Cost children (if needed); or pick the best child expression (in terms of cost)
    // (2) Enforce any missing properties as required
    // (3) Update Group/Context metadata of expression + cost
    for (; cur_prop_pair_idx < static_cast<int>(input_properties.size()); cur_prop_pair_idx++)
    {
        auto & input_props = input_properties[cur_prop_pair_idx];
        auto group_stats = context->getMemo().getGroupById(group_expr->getGroupId())->getStatistics().value_or(nullptr);

        // todo mt prune
        // Calculate local cost and update total cost
        if (cur_child_idx == 0)
        {
            // Compute the cost of the root operator
            // 1. Collect stats needed and cache them in the group
            // 2. Calculate cost based on children's stats
            std::vector<PlanNodeStatisticsPtr> children_stats;
            for (const auto & child : group_expr->getChildrenGroups())
                children_stats.emplace_back(context->getMemo().getGroupById(child)->getStatistics().value_or(nullptr));

            cur_total_cost = CostCalculator::calculate(
                                 group_expr->getStep(),
                                 group_stats,
                                 children_stats,
                                 *context->getOptimizerContext().getContext(),
                                 context->getOptimizerContext().getWorkerSize())
                                 .getCost();
        }

        for (; cur_child_idx < static_cast<int>(group_expr->getChildrenGroups().size()); cur_child_idx++)
        {
            auto & i_prop = input_props[cur_child_idx];
            auto child_group = context->getOptimizerContext().getMemo().getGroupById(group_expr->getChildrenGroups()[cur_child_idx]);

            // Check whether the child group is already optimized for the prop
            if (child_group->hasWinner(i_prop))
            { // Directly get back the best expr if the child group is optimized
                auto child_best_expr = child_group->getBestExpression(i_prop);
                cur_total_cost += child_best_expr->getCost();
                // todo mt prune
                //                if (cur_total_cost > context->getCostUpperBound())
                //                    break;
            }
            else if (prev_child_idx != cur_child_idx)
            { // We haven't optimized child group
                prev_child_idx = cur_child_idx;
                pushTask(this->shared_from_this());

                // todo mt prune
                auto cost_high = context->getCostUpperBound() - cur_total_cost;
                auto ctx = std::make_shared<OptimizationContext>(context->getOptimizerContext(), i_prop, cost_high);
                pushTask(std::make_shared<OptimizeGroup>(child_group, ctx));
                return;
            }
            else
            { // If we return from OptimizeGroup, then there is no expr for the context
                break;
            }
        }

        // Check whether we successfully optimize all child group
        if (cur_child_idx == static_cast<int>(group_expr->getChildrenGroups().size()))
        {
            PropertySet actual_input_props;
            bool all_fix_hash = true;
            for (size_t index = 0; index < group_expr->getChildrenGroups().size(); index++)
            {
                auto & i_prop = input_props[index];
                auto child_group = context->getOptimizerContext().getMemo().getGroupById(group_expr->getChildrenGroups()[index]);
                auto child_best_expr = child_group->getBestExpression(i_prop);
                actual_input_props.emplace_back(child_best_expr->getActualProperty());
                all_fix_hash &= i_prop.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::FIXED_HASH;
            }

            if (group_expr->getStep()->getType() == IQueryPlanStep::Type::Join && all_fix_hash)
            {
                auto & first_props = actual_input_props[0];
                bool match = false;
                if (first_props.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::FIXED_HASH
                    || first_props.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::BUCKET_TABLE)
                {
                    match = true;
                    auto left_equivalences = context->getMemo().getGroupById(group_expr->getChildrenGroups()[0])->getEquivalences();
                    auto right_equivalences = context->getMemo().getGroupById(group_expr->getChildrenGroups()[1])->getEquivalences();
                    NameToNameSetMap right_join_key_to_left;
                    if (auto join_step = dynamic_cast<const JoinStep *>(group_expr->getStep().get()))
                    {
                        auto left_rep_map = left_equivalences->representMap();
                        auto right_rep_map = right_equivalences->representMap();
                        for (size_t join_key_index = 0; join_key_index < join_step->getLeftKeys().size(); ++join_key_index)
                        {
                            auto left_key = join_step->getLeftKeys()[join_key_index];
                            auto right_key = join_step->getRightKeys()[join_key_index];
                            left_key = left_rep_map.count(left_key) ? left_rep_map.at(left_key) : left_key;
                            right_key = right_rep_map.count(right_key) ? right_rep_map.at(right_key) : right_key;
                            right_join_key_to_left[right_key].insert(left_key);
                        }
                    }

                    auto first_handle = first_props.getNodePartitioning().getPartitioningHandle();
                    auto first_bucket_count = first_props.getNodePartitioning().getBuckets();
                    auto first_sharding_expr = first_props.getNodePartitioning().getSharingExpr();
                    const auto first_partition_column
                        = first_props.getNodePartitioning().normalize(*left_equivalences).getPartitioningColumns();

                    for (size_t actual_prop_index = 1; actual_prop_index < actual_input_props.size(); ++actual_prop_index)
                    {
                        auto translated_prop = actual_input_props[actual_prop_index].normalize(*right_equivalences);
                        if (translated_prop.getNodePartitioning().getPartitioningHandle() != first_handle
                            || translated_prop.getNodePartitioning().getBuckets() != first_bucket_count
                            || !ASTEquality::compareTree(translated_prop.getNodePartitioning().getSharingExpr(), first_sharding_expr))
                        {
                            match = false;
                            break;
                        }
                        const auto & transformed_partition_cols = translated_prop.getNodePartitioning().getPartitioningColumns();
                        if (transformed_partition_cols.size() != first_partition_column.size())
                        {
                            match = false;
                            break;
                        }
                        for (size_t col_index = 0; col_index < transformed_partition_cols.size(); col_index++)
                        {
                            if (right_join_key_to_left[transformed_partition_cols[col_index]].count(first_partition_column[col_index]) == 0)
                            {
                                match = false;
                                break;
                            }
                        }
                    }
                }
                if (!match)
                {
                    auto new_child_requires = input_props;
                    for (auto & new_child : new_child_requires)
                    {
                        new_child.getNodePartitioningRef().setRequireHandle(true);
                    }
                    input_properties.emplace_back(new_child_requires);
                    // Reset child idx and total cost
                    prev_child_idx = -1;
                    cur_child_idx = 0;
                    cur_total_cost = 0;
                    continue;
                }
            }

            Property output_prop;
            if (group_expr->getStep()->getType() == IQueryPlanStep::Type::CTERef)
            {
                const auto cte_step = dynamic_cast<const CTERefStep *>(group_expr->getStep().get());
                CTEId cte_id = cte_step->getId();
                auto cte_def_group = context->getOptimizerContext().getMemo().getCTEDefGroupByCTEId(cte_id);
                auto cte_global_property = CTEDescription::createCTEDefGlobalProperty(context->getRequiredProp(), cte_id);
                auto cte_def_best_expr = cte_def_group->getBestExpression(cte_global_property);
                output_prop = cte_def_best_expr->getActualProperty().translate(cte_step->getReverseOutputColumns());
                output_prop.getCTEDescriptions().emplace(cte_id, cte_global_property);
            }
            else
            {
                output_prop = PropertyDeriver::deriveProperty(group_expr->getStep(), actual_input_props, *context->getOptimizerContext().getContext());
            }

            // Not need to do pruning here because it has been done when we get the
            // best expr from the child group
            auto equivalences = context->getMemo().getGroupById(group_expr->getGroupId())->getEquivalences();

            // Enforce property if the requirement does not meet
            auto require = context->getRequiredProp();
            bool is_preferred = require.isPreferred();

            GroupExprPtr remote_exchange;
            GroupExprPtr local_exchange;
            Property actual = output_prop;
            if (!is_preferred
                && !PropertyMatcher::matchNodePartitioning(
                    *context->getOptimizerContext().getContext(),
                    require.getNodePartitioningRef(),
                    output_prop.getNodePartitioning(),
                    *equivalences))
            {
                // add remote exchange
                remote_exchange
                    = PropertyEnforcer::enforceNodePartitioning(group_expr, require, actual, *context->getOptimizerContext().getContext());
                actual = PropertyDeriver::deriveProperty(remote_exchange->getStep(), actual, *context->getOptimizerContext().getContext());
                // add cost
                cur_total_cost += CostCalculator::calculate(
                                      remote_exchange->getStep(),
                                      group_stats,
                                      {group_stats},
                                      *context->getOptimizerContext().getContext(),
                                      context->getOptimizerContext().getWorkerSize())
                                      .getCost();
            }

            if (!is_preferred
                && !PropertyMatcher::matchStreamPartitioning(
                    *context->getOptimizerContext().getContext(),
                    require.getStreamPartitioning(),
                    actual.getStreamPartitioning(),
                    *equivalences))
            {
                // add local exchange
                local_exchange = PropertyEnforcer::enforceStreamPartitioning(
                    remote_exchange ? remote_exchange : group_expr, require, actual, *context->getOptimizerContext().getContext());
                actual = PropertyDeriver::deriveProperty(local_exchange->getStep(), actual, *context->getOptimizerContext().getContext());
                // add cost
                cur_total_cost += CostCalculator::calculate(
                                      local_exchange->getStep(),
                                      group_stats,
                                      {group_stats},
                                      *context->getOptimizerContext().getContext(),
                                      context->getOptimizerContext().getWorkerSize())
                                      .getCost();
            }

            // Add cost for cte
            for (auto & cte : actual.getCTEDescriptions())
            {
                CTEId cte_id = cte.first;
                if (!require.getCTEDescriptions().contains(cte_id))
                {
                    bool contains = std::any_of(input_props.begin(), input_props.end(), [&](const Property & prop) {
                        return prop.getCTEDescriptions().contains(cte_id);
                    });
                    // if cte don't exist in required property but exists both in input_props and actual, we add the cost of cte plan.
                    if (contains)
                    {
                        auto cte_def_group = context->getOptimizerContext().getMemo().getCTEDefGroupByCTEId(cte_id);
                        auto cte_global_property = CTEDescription::createCTEDefGlobalProperty(actual, cte_id, cte_def_group->getCTESet());
                        auto cte_best_expr = cte_def_group->getBestExpression(cte_global_property);
                        cur_total_cost += cte_best_expr->getCost();
                    }
                }
            }

            // todo mt prune
            // If the cost is smaller than the winner, update the context upper bound
            //            context->setCostUpperBound(context->getCostUpperBound() - cur_total_cost);
            auto cur_group = context->getMemo().getGroupById(group_expr->getGroupId());
            if (!context->getOptimizerContext().getContext()->getSettingsRef().enable_cbo)
                cur_total_cost = 0;
            cur_group->setExpressionCost(
                std::make_shared<Winner>(group_expr, remote_exchange, local_exchange, input_props, actual, cur_total_cost),
                context->getRequiredProp());
        }

        // Reset child idx and total cost
        prev_child_idx = -1;
        cur_child_idx = 0;
        cur_total_cost = 0;

        // Explore and derive all possible input properties
        if (cur_prop_pair_idx + 1 == static_cast<int>(input_properties.size()))
            exploreInputProperties();
    }
}

void OptimizeInput::exploreInputProperties()
{
    // initialize input properties with default required property.
    if (input_properties.empty())
    {
        explored_properties.emplace_back(context->getRequiredProp());
        input_properties = PropertyDeterminer::determineRequiredProperty(group_expr->getStep(), context->getRequiredProp(), input_cte_ids);
    }

    // explore cte properties.
    if (input_cte_ids.size() > 1 && !is_cte_property_enumerated)
    {
        std::map<CTEId, size_t> cte_id_counts; // use map ensure that the optimization order.
        for (auto & cte_ids : input_cte_ids)
            for (auto cte_id : cte_ids)
                ++cte_id_counts[cte_id];

        for (auto & item : cte_id_counts)
        {
            auto cte_id = item.first;

            // Check Whether this group is common ancestor of cte.
            if (item.second < 2 || context->getRequiredProp().getCTEDescriptions().contains(cte_id))
                continue;

            // If there is no statistics or config `enable_cte_no_statistics` is not set, we don't explore share cte.
            // if (!context->getOptimizerContext().getContext().getSettingsRef().enable_cte_no_statistics)
            // {
            //     auto cte_def_group = context->getOptimizerContext().getMemo().getCTEDefGroupByCTEId(cte_id);
            //     if (!cte_def_group->getStatistics())
            //         continue;
            // }

            // Explore shared cte as input properties requirements.
            auto & cte_def_required_properties = context->getOptimizerContext().getCTEDefPropertyRequirements()[cte_id];
            if (cte_def_required_properties.empty())
            {
                // If we don't know any required property for CTEDef, we request ARBITRARY distribution.
                // It allows the CTERefs to enforce any missing requirements if needed.
                addInputPropertiesForCTE(cte_id, CTEDescription{});
            }
            else
            {
                if (context->getOptimizerContext().getContext()->getSettingsRef().enable_cte_property_enum)
                {
                    // CTERef may require identical properties. These properties can be enforced in the CTEDef to
                    // avoid repeated work.
                    for (auto & winner : cte_def_required_properties)
                        addInputPropertiesForCTE(cte_id, CTEDescription{winner});
                }
                if (context->getOptimizerContext().getContext()->getSettingsRef().enable_cte_common_property)
                {
                    // It is too expensive to enumerate all possible properties, especially if there are lots CTERef.
                    // We can only optimize for common property instead.
                    auto common_property = PropertyMatcher::compatibleCommonRequiredProperty(cte_def_required_properties);
                    addInputPropertiesForCTE(cte_id, CTEDescription{common_property});
                }
                is_cte_property_enumerated = true;
            }
        }
    }
}

void OptimizeInput::addInputPropertiesForCTE(size_t cte_id, CTEDescription cte_description)
{
    //  bucket_table satisfy repartition requirement, we can't get the common property if we allow bucket table. see tpcds-q11.
    if (cte_description.getNodePartitioning().getPartitioningHandle() == Partitioning::Handle::FIXED_HASH)
        cte_description.getNodePartitioningRef().setRequireHandle(true);

    if (explored_cte_properties[cte_id].contains(cte_description))
        return;
    explored_cte_properties[cte_id].emplace(cte_description);

    std::vector<Property> new_properties;
    for (auto & explored_property : explored_properties)
    {
        if (!explored_property.getCTEDescriptions().contains(cte_id))
        {
            auto new_property = explored_property;
            new_property.getCTEDescriptions().emplace(cte_id, cte_description);
            new_properties.emplace_back(new_property);
            auto new_required_properties
                = PropertyDeterminer::determineRequiredProperty(group_expr->getStep(), new_property, input_cte_ids);
            input_properties.insert(input_properties.end(), new_required_properties.begin(), new_required_properties.end());
        }
    }
    explored_properties.insert(explored_properties.end(), new_properties.begin(), new_properties.end());
}

void OptimizerTask::pushTask(const OptimizerTaskPtr & task)
{
    context->pushTask(task);
}
const std::vector<RulePtr> & OptimizerTask::getTransformationRules() const
{
    return context->getTransformationRules();
}
const std::vector<RulePtr> & OptimizerTask::getImplementationRules() const
{
    return context->getImplementationRules();
}

void OptimizerTask::constructValidRules(
    const GroupExprPtr & group_expr, const std::vector<RulePtr> & rules, std::vector<RulePtr> & valid_rules)
{
    for (const auto & rule : rules)
    {
        if (!rule->isEnabled(context->getOptimizerContext().getContext()))
        {
            continue;
        }
        // Check if we can apply the rule
        if (group_expr->hasRuleExplored(rule->getType()))
            continue;
        if (group_expr->getStep()->getType() != rule->getTargetType())
        {
            // match head pattern
            continue;
        }

        // This check exists only as an "early" reject. As is evident, we do not check
        // the full pattern here. Checking the full pattern happens when actually trying to
        // apply the rule (via a GroupExprBindingIterator).
        auto child_pattern_size = rule->getPattern()->getChildrenPatterns().size();
        if (child_pattern_size > 0 && group_expr->getChildrenGroups().size() != child_pattern_size)
            continue;

        valid_rules.emplace_back(rule);
    }
}

}
