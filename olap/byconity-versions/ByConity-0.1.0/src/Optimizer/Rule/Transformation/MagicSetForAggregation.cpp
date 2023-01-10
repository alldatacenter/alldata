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

#include <Optimizer/Rule/Transformation/MagicSetForAggregation.h>

#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/Rule/Patterns.h>
#include <Parsers/ASTFunction.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/AnyStep.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
namespace
{
    // The filter factor of Y filter join X, used for early pruning.
    constexpr double FILTER_FACTOR = 0.5;
    // The filter source Y max allowed table scan size, used for early pruning.
    constexpr uint32_t MAX_SEARCH_TREE_THRESHOLD = 4;
}

const std::vector<RuleType> & MagicSetRule::blockRules() const
{
    static std::vector<RuleType> block{RuleType::MAGIC_SET_FOR_AGGREGATION, RuleType::MAGIC_SET_FOR_PROJECTION_AGGREGATION};
    return block;
}

static bool checkFilterFactor(
    const PlanNodeStatisticsPtr & source_statistics,
    const PlanNodeStatisticsPtr & filter_statistics,
    const Names & source_names,
    const Names & filter_names)
{
    double filter_factor = 1.0;
    for (size_t i = 0; i < source_names.size(); i++)
    {
        auto source_name_statistic = source_statistics->getSymbolStatistics(source_names[i]);
        auto filter_name_statistic = filter_statistics->getSymbolStatistics(filter_names[i]);
        if (!source_name_statistic->isUnknown() && !filter_name_statistic->isUnknown())
        {
            filter_factor *= static_cast<double>(filter_name_statistic->getNdv()) / source_name_statistic->getNdv();
        }
    }
    return filter_factor < FILTER_FACTOR;
}

PlanNodePtr MagicSetRule::buildMagicSetAsFilterJoin(
    const PlanNodePtr & source,
    const PlanNodePtr & filter_source,
    const Names & source_names,
    const Names & filter_names,
    ContextMutablePtr & context,
    bool enforce_distinct)
{
    PlanNodePtr filter_node = filter_source;
    // add projection if
    // 1. there are unused columns to prune
    // 2. or filter symbols need reallocate to avoid the same name with source symbols
    NameSet source_name_set{source_names.begin(), source_names.end()};
    NameSet filter_name_set{filter_names.begin(), filter_names.end()};
    if (filter_source->getStep()->getOutputStream().header.columns() != filter_names.size()
        || std::any_of(filter_names.begin(), filter_names.end(), [&](const auto & symbol) { return source_name_set.contains(symbol); }))
    {
        Assignments assignments;
        NameToType name_to_type;
        NamesAndTypes names_and_types;
        for (const auto & column : filter_source->getStep()->getOutputStream().header)
        {
            if (filter_name_set.contains(column.name))
            {
                std::string new_name = context->getSymbolAllocator()->newSymbol(column.name);
                assignments.emplace_back(new_name, std::make_shared<ASTIdentifier>(column.name));
                name_to_type[new_name] = column.type;
                names_and_types.emplace_back(NameAndTypePair{new_name, column.type});
            }
        }
        filter_node = PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            std::make_shared<ProjectionStep>(DataStream{.header = names_and_types}, assignments, name_to_type),
            PlanNodes{filter_node});
    }

    Names reallocated_filter_names;
    std::transform(
        filter_node->getStep()->getOutputStream().header.begin(),
        filter_node->getStep()->getOutputStream().header.end(),
        std::back_inserter(reallocated_filter_names),
        [](auto & nameAndType) { return nameAndType.name; });

    // add aggregating if filter source names is not distinct
    if (enforce_distinct)
    {
        filter_node = PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            std::make_shared<AggregatingStep>(
                filter_node->getStep()->getOutputStream(), reallocated_filter_names, AggregateDescriptions{}, GroupingSetsParamsList{}, true),
            PlanNodes{filter_node});
    }

    assert(source_names.size() == reallocated_filter_names.size());

    auto filter_join_step = std::make_shared<JoinStep>(
        DataStreams{source->getStep()->getOutputStream(), filter_node->getStep()->getOutputStream()},
        source->getStep()->getOutputStream(),
        ASTTableJoin::Kind::Inner,
        ASTTableJoin::Strictness::All,
        source_names,
        reallocated_filter_names);
    filter_join_step->setMagic(true);

    return PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(filter_join_step), PlanNodes{source, filter_node});
}

PatternPtr MagicSetForProjectionAggregation::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) {
            return (s.getKind() == ASTTableJoin::Kind::Inner || s.getKind() == ASTTableJoin::Kind::Right) && !s.isMagic();
        })
        ->with({Patterns::project()->withSingle(Patterns::aggregating()->withSingle(Patterns::any())), Patterns::any()});
}

TransformResult MagicSetForProjectionAggregation::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    const auto & join_step = dynamic_cast<const JoinStep &>(*node->getStep());

    auto magic_set_node = node->getChildren()[1];
    auto magic_set_group_id = dynamic_cast<const AnyStep &>(*magic_set_node->getStep().get()).getGroupId();
    auto magic_set_group = rule_context.optimization_context->getOptimizerContext().getMemo().getGroupById(magic_set_group_id);
    if (magic_set_group->getMaxTableScans() > MAX_SEARCH_TREE_THRESHOLD)
    {
        return {};
    }

    auto project_node = node->getChildren()[0];
    const auto & project_step = dynamic_cast<const ProjectionStep &>(*project_node->getStep());

    auto target_agg_node = project_node->getChildren()[0];
    const auto & target_agg_step = dynamic_cast<const AggregatingStep &>(*target_agg_node->getStep());

    const auto & source_statistics = target_agg_node->getStatistics();
    const auto & filter_statistics = magic_set_node->getStatistics();
    if (!source_statistics || !filter_statistics || source_statistics.value()->getRowCount() <= filter_statistics.value()->getRowCount())
    {
        return {};
    }

    NameSet grouping_key{target_agg_step.getKeys().begin(), target_agg_step.getKeys().end()};
    std::map<String, String> projection_mapping;
    for (const auto & assignment : project_step.getAssignments())
    {
        if (assignment.second->getType() == ASTType::ASTIdentifier)
        {
            const ASTIdentifier & identifier = assignment.second->as<ASTIdentifier &>();
            if (grouping_key.contains(identifier.name()))
            {
                projection_mapping.emplace(assignment.first, identifier.name());
            }
        }
    }

    Names target_keys;
    Names filter_keys;
    for (auto left_key = join_step.getLeftKeys().begin(), right_key = join_step.getRightKeys().begin();
         left_key != join_step.getLeftKeys().end();
         ++left_key, ++right_key)
    {
        if (projection_mapping.contains(*left_key))
        {
            target_keys.emplace_back(projection_mapping.at(*left_key));
            filter_keys.emplace_back(*right_key);
        }
    }


    if (target_keys.empty())
    {
        return {};
    }

    if (!checkFilterFactor(source_statistics.value(), filter_statistics.value(), target_keys, filter_keys))
    {
        return {};
    }

    auto & context = rule_context.context;

    PlanNodePtr filter_join_node
        = buildMagicSetAsFilterJoin(target_agg_node->getChildren()[0], magic_set_node, target_keys, filter_keys, context);

    return PlanNodeBase::createPlanNode(
        context->nextNodeId(),
        join_step.copy(context),
        PlanNodes{
            PlanNodeBase::createPlanNode(
                context->nextNodeId(),
                project_step.copy(context),
                PlanNodes{
                    PlanNodeBase::createPlanNode(context->nextNodeId(), target_agg_step.copy(context), PlanNodes{filter_join_node}),
                }),
            node->getChildren()[1]});
}

PatternPtr MagicSetForAggregation::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) {
            return (s.getKind() == ASTTableJoin::Kind::Inner || s.getKind() == ASTTableJoin::Kind::Right) && !s.isMagic();
        })
        ->with({Patterns::aggregating()->withSingle(Patterns::any()), Patterns::any()});
}

TransformResult MagicSetForAggregation::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    const auto & join_step = dynamic_cast<const JoinStep &>(*node->getStep());

    auto magic_set_node = node->getChildren()[1];
    auto magic_set_group_id = dynamic_cast<const AnyStep &>(*magic_set_node->getStep().get()).getGroupId();
    auto magic_set_group = rule_context.optimization_context->getOptimizerContext().getMemo().getGroupById(magic_set_group_id);
    if (magic_set_group->getMaxTableScans() > MAX_SEARCH_TREE_THRESHOLD)
    {
        return {};
    }

    auto target_agg_node = node->getChildren()[0];
    const auto & target_agg_step = dynamic_cast<const AggregatingStep &>(*target_agg_node->getStep().get());

    const auto & source_statistics = target_agg_node->getStatistics();
    const auto & filter_statistics = magic_set_node->getStatistics();
    if (!source_statistics || !filter_statistics || source_statistics.value()->getRowCount() <= filter_statistics.value()->getRowCount())
    {
        return {};
    }

    NameSet grouping_key{target_agg_step.getKeys().begin(), target_agg_step.getKeys().end()};
    std::vector<String> target_keys;
    std::vector<String> filter_keys;
    for (auto left_key = join_step.getLeftKeys().begin(), right_key = join_step.getRightKeys().begin();
         left_key != join_step.getLeftKeys().end();
         ++left_key, ++right_key)
    {
        if (!grouping_key.contains(*left_key))
        {
            continue;
        }
        target_keys.emplace_back(*left_key);
        filter_keys.emplace_back(*right_key);
    }

    if (target_keys.empty())
    {
        return {};
    }

    if (!checkFilterFactor(source_statistics.value(), filter_statistics.value(), target_keys, filter_keys))
    {
        return {};
    }

    auto context = rule_context.context;

    PlanNodePtr filter_join_node
        = buildMagicSetAsFilterJoin(target_agg_node->getChildren()[0], magic_set_node, target_keys, filter_keys, context);

    return PlanNodeBase::createPlanNode(
        context->nextNodeId(),
        join_step.copy(context),
        PlanNodes{
            PlanNodeBase::createPlanNode(context->nextNodeId(), target_agg_step.copy(context), PlanNodes{filter_join_node}),
            node->getChildren()[1]});
}

}
