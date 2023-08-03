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

#include <Optimizer/Rule/Transformation/MagicSetPushDown.h>

#include <Optimizer/Cascades/CascadesOptimizer.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/Rule/Transformation/InnerJoinCommutation.h>
#include <Parsers/ASTFunction.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/AnyStep.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/ProjectionStep.h>

#include <algorithm>

namespace DB
{

PatternPtr MagicSetPushThroughProject::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([](const JoinStep & s) { return s.isMagic(); })
        ->with({Patterns::project()->with({Patterns::any()}), Patterns::any()});
}

TransformResult MagicSetPushThroughProject::transformImpl(PlanNodePtr magic_set, const Captures &, RuleContext & rule_context)
{
    auto & projection = magic_set->getChildren()[0];
    const auto * projection_step = dynamic_cast<const ProjectionStep *>(projection->getStep().get());
    const auto * magic_set_step = dynamic_cast<const JoinStep *>(magic_set->getStep().get());

    Names mapping;
    mapping.resize(magic_set_step->getLeftKeys().size());
    size_t found = 0;
    for (const auto & assignment : projection_step->getAssignments())
    {
        if (assignment.second->getType() == ASTType::ASTIdentifier)
        {
            const auto & symbol = assignment.second->as<ASTIdentifier &>();
            for (size_t i = 0; i < magic_set_step->getLeftKeys().size(); i++)
            {
                if (assignment.first == magic_set_step->getLeftKeys()[i])
                {
                    mapping[i] = symbol.name();
                    found++;
                    break;
                }
            }
        }
    }

    if (found != mapping.size())
        return {};

    auto context = rule_context.context;
    PlanNodePtr new_filter_join_node = buildMagicSetAsFilterJoin(
        projection->getChildren()[0], magic_set->getChildren()[1], mapping, magic_set_step->getRightKeys(), context, false);

    return TransformResult{PlanNodeBase::createPlanNode(context->nextNodeId(), projection->getStep(), PlanNodes{new_filter_join_node}), true};
}

static bool all_of(const Names & target, const Names & names)
{
    NameSet target_set{target.begin(), target.end()};
    return std::all_of(names.cbegin(), names.cend(), [&](const auto & name) { return target_set.contains(name); });
}

static bool all_of(const NamesAndTypes & target, const Names & names)
{
    NameSet target_set;
    std::transform(target.begin(), target.end(), std::inserter(target_set, target_set.end()), [&](const auto & e) { return e.name; });
    return std::all_of(names.cbegin(), names.cend(), [&](const auto & name) { return target_set.contains(name); });
}

static std::optional<Names> try_mapping(const Names & from, const Names & to, const Names & names)
{
    std::unordered_map<std::string, std::string> join_keys;
    for (size_t i = 0; i < from.size(); i++)
        join_keys.emplace(from[i], to[i]);

    Names mapped;
    for (const auto & name : names)
    {
        if (!join_keys.contains(name))
            return {};
        mapped.emplace_back(join_keys.at(name));
    }
    return mapped;
}

PatternPtr MagicSetPushThroughJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([](const JoinStep & s) { return s.isMagic(); })
        ->with(
            {Patterns::join()
                 ->matchingStep<JoinStep>([](const JoinStep & s) { return !s.isMagic(); })
                 ->with({Patterns::any(), Patterns::any()}),
             Patterns::any()});
}

TransformResult MagicSetPushThroughJoin::transformImpl(PlanNodePtr magic_set, const Captures &, RuleContext & rule_context)
{
    auto join = magic_set->getChildren()[0];
    const auto * join_step = dynamic_cast<const JoinStep *>(join->getStep().get());
    const auto * magic_set_step = dynamic_cast<const JoinStep *>(magic_set->getStep().get());
    auto context = rule_context.context;

    auto left_node = join->getChildren()[0];
    auto right_node = join->getChildren()[1];

    if (all_of(join_step->getInputStreams()[0].header.getNamesAndTypes(), magic_set_step->getLeftKeys()))
    {
        auto left_node_with_magic_set = buildMagicSetAsFilterJoin(
            left_node,
            magic_set->getChildren()[1],
            magic_set_step->getLeftKeys(),
            magic_set_step->getRightKeys(),
            rule_context.context,
            false);

        PlanNodes ret;
        ret.emplace_back(PlanNodeBase::createPlanNode(context->nextNodeId(), join->getStep(), PlanNodes{left_node_with_magic_set, right_node}));

        if (InnerJoinCommutation::supportSwap(dynamic_cast<const JoinStep &>(*ret.back()->getStep())))
            ret.emplace_back(InnerJoinCommutation::swap(dynamic_cast<JoinNode &>(*ret.back()), rule_context));

        // try push filter join to right side
        std::optional<Names> mapped = try_mapping(join_step->getLeftKeys(), join_step->getRightKeys(), magic_set_step->getLeftKeys());
        if (mapped.has_value())
        {
            auto right_node_with_magic_set = buildMagicSetAsFilterJoin(
                right_node, magic_set->getChildren()[1], mapped.value(), magic_set_step->getRightKeys(), context, false);

            ret.emplace_back(
                PlanNodeBase::createPlanNode(context->nextNodeId(), join->getStep(), PlanNodes{left_node, right_node_with_magic_set}));
            if (auto swap_join = trySwapJoin(ret.back(), rule_context))
                ret.emplace_back(swap_join.value());

            // push filter join to both side
            ret.emplace_back(PlanNodeBase::createPlanNode(
                context->nextNodeId(), join->getStep(), PlanNodes{left_node_with_magic_set, right_node_with_magic_set}));
            if (auto swap_join = trySwapJoin(ret.back(), rule_context))
                ret.emplace_back(swap_join.value());
        }

        return TransformResult{ret, true};
    }

    if (all_of(join_step->getInputStreams()[1].header.getNamesAndTypes(), magic_set_step->getLeftKeys()))
    {
        auto new_join = join_step->copy(context);
        dynamic_cast<JoinStep *>(new_join.get())->setMagic(true);

        auto right_node_with_magic_set = buildMagicSetAsFilterJoin(
            right_node, magic_set->getChildren()[1], magic_set_step->getLeftKeys(), magic_set_step->getRightKeys(), context, false);

        PlanNodes ret;
        ret.emplace_back(PlanNodeBase::createPlanNode(context->nextNodeId(), join->getStep(), PlanNodes{left_node, right_node_with_magic_set}));

        if (InnerJoinCommutation::supportSwap(dynamic_cast<const JoinStep &>(*ret.back()->getStep())))
        {
            ret.emplace_back(InnerJoinCommutation::swap(dynamic_cast<JoinNode &>(*ret.back()), rule_context));
        }

        std::optional<Names> mapped = try_mapping(join_step->getRightKeys(), join_step->getLeftKeys(), magic_set_step->getLeftKeys());
        if (mapped.has_value())
        {
            auto left_node_with_magic_set = buildMagicSetAsFilterJoin(
                left_node, magic_set->getChildren()[1], mapped.value(), magic_set_step->getRightKeys(), context, false);

            ret.emplace_back(
                PlanNodeBase::createPlanNode(context->nextNodeId(), join->getStep(), PlanNodes{left_node_with_magic_set, right_node}));
            if (auto swap_join = trySwapJoin(ret.back(), rule_context))
                ret.emplace_back(swap_join.value());

            ret.emplace_back(PlanNodeBase::createPlanNode(
                context->nextNodeId(), join->getStep(), PlanNodes{left_node_with_magic_set, right_node_with_magic_set}));
            if (auto swap_join = trySwapJoin(ret.back(), rule_context))
                ret.emplace_back(swap_join.value());
        }

        return TransformResult{ret, true};
    }

    // do nothing if symbols from both two sides
    // note@kaixi: maybe be able to push down
    return {};
}

std::optional<PlanNodePtr> MagicSetPushThroughJoin::trySwapJoin(const PlanNodePtr & node, RuleContext & rule_context)
{
    if (const auto * step = dynamic_cast<const JoinStep *>(node->getStep().get()))
        if (InnerJoinCommutation::supportSwap(*step))
            return InnerJoinCommutation::swap(dynamic_cast<JoinNode &>(*node), rule_context);
    return {};
}

const std::vector<RuleType> & MagicSetPushThroughJoin::blockRules() const
{
    static std::vector<RuleType> block{
        RuleType::MAGIC_SET_FOR_AGGREGATION, RuleType::MAGIC_SET_FOR_PROJECTION_AGGREGATION, RuleType::JOIN_ENUM_ON_GRAPH};
    return block;
}

PatternPtr MagicSetPushThroughFilter::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) { return s.isMagic(); })
        ->with({Patterns::filter()->with({Patterns::any()}), Patterns::any()});
}

TransformResult MagicSetPushThroughFilter::transformImpl(PlanNodePtr magic_set, const Captures &, RuleContext & rule_context)
{
    auto & filter = magic_set->getChildren()[0];
    auto context = rule_context.context;
    return TransformResult{
        PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            filter->getStep(),
            PlanNodes{PlanNodeBase::createPlanNode(
                context->nextNodeId(), magic_set->getStep(), PlanNodes{filter->getChildren()[0], magic_set->getChildren()[1]})}),
        true};
}

PatternPtr MagicSetPushThroughAggregating::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([&](const JoinStep & s) { return s.isMagic(); })
        ->with({Patterns::aggregating()->with({Patterns::any()}), Patterns::any()});
}

TransformResult MagicSetPushThroughAggregating::transformImpl(PlanNodePtr magic_set, const Captures &, RuleContext & rule_context)
{
    auto aggregating = magic_set->getChildren()[0];
    const auto & aggregating_step = dynamic_cast<const AggregatingStep &>(*aggregating->getStep().get());
    const auto & magic_set_step = dynamic_cast<const JoinStep &>(*magic_set->getStep().get());

    if (!all_of(aggregating_step.getKeys(), magic_set_step.getLeftKeys()))
        return {};

    auto context = rule_context.context;
    return TransformResult{
        PlanNodeBase::createPlanNode(
            context->nextNodeId(),
            aggregating_step.copy(context),
            PlanNodes{buildMagicSetAsFilterJoin(
                aggregating->getChildren()[0],
                magic_set->getChildren()[1],
                magic_set_step.getLeftKeys(),
                magic_set_step.getRightKeys(),
                context,
                false)}),
        true};
}

}
