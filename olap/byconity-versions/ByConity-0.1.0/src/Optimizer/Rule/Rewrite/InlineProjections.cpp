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

#include <Optimizer/Rule/Rewrite/InlineProjections.h>

#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/ExpressionInliner.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
PatternPtr InlineProjections::getPattern() const
{
    return Patterns::project()->withSingle(Patterns::project());
}

TransformResult InlineProjections::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & parent = node;
    auto & child = node->getChildren()[0];
    return TransformResult::of(inlineProjections(parent, child, rule_context.context));
}

std::optional<PlanNodePtr>
InlineProjections::inlineProjections(PlanNodePtr & parent_node, PlanNodePtr & child_node, ContextMutablePtr & context)
{
    auto parent = dynamic_cast<ProjectionNode *>(parent_node.get());
    auto child = dynamic_cast<ProjectionNode *>(child_node.get());
    if (!parent || !child)
        return {};

    std::set<String> targets = extractInliningTargets(parent, child, context);
    if (targets.empty())
    {
        return std::nullopt;
    }

    const auto & parent_step = *parent->getStep();
    const auto & parent_assignments = parent_step.getAssignments();
    const NameToType & parent_type = parent_step.getNameToType();

    const auto & child_step = *child->getStep();
    const auto & child_assignments = child_step.getAssignments();
    const NameToType & child_type = child_step.getNameToType();

    // inline the expressions
    Assignments inline_assignments;
    NameToType inline_types;
    for (const auto & child_assignment : child_assignments)
    {
        String symbol = child_assignment.first;
        if (targets.contains(symbol))
        {
            inline_assignments.emplace_back(child_assignment);
            inline_types[child_assignment.first] = child_type.at(child_assignment.first);
        }
    }

    Assignments new_parent_assignments;
    NameToType new_parent_types;
    for (const auto & parent_assignment : parent_assignments)
    {
        new_parent_assignments.emplace_back(
            Assignment{parent_assignment.first, inlineReferences(parent_assignment.second, inline_assignments)});
        new_parent_types[parent_assignment.first] = parent_type.at(parent_assignment.first);
    }

    // Synthesize identity assignments for the inputs of expressions that were inlined
    // to place in the child projection.
    std::set<String> inputs;
    for (const auto & child_assignment : child_assignments)
    {
        String symbol = child_assignment.first;
        if (targets.contains(symbol))
        {
            auto expr = child_assignment.second;
            std::set<String> input_symbols = SymbolsExtractor::extract(expr);
            inputs.insert(input_symbols.begin(), input_symbols.end());
        }
    }

    Assignments new_child_assignments;
    NameToType new_child_types;
    for (const auto & child_assignment : child_assignments)
    {
        if (!targets.contains(child_assignment.first))
        {
            new_child_assignments.emplace_back(child_assignment);
            new_child_types[child_assignment.first] = child_type.at(child_assignment.first);

            if (const auto * identifier = child_assignment.second->as<ASTIdentifier>())
                if (identifier->name() != child_assignment.first)
                    inputs.erase(child_assignment.first);
        }
    }

    // add identity project for inlined expression argument
    std::unordered_map<std::string, DataTypePtr> input_types;
    for (const auto & name_and_type : child_step.getInputStreams()[0].header)
    {
        input_types.emplace(name_and_type.name, name_and_type.type);
    }

    for (const String & input : inputs)
    {
        if (input_types.contains(input))
        {
            new_child_assignments.emplace_back(Assignment{input, std::make_shared<ASTIdentifier>(input)});
            new_child_types[input] = input_types.at(input);
        }
    }

    // merge dynamic filters
    auto new_dynamic_filters = parent_step.getDynamicFilters();
    for (const auto & dynamic_filter : child_step.getDynamicFilters())
    {
        const auto & name = dynamic_filter.first;
        new_dynamic_filters.emplace(dynamic_filter);
        if (!new_parent_types.contains(name))
        {
            new_parent_assignments.emplace_back(Assignment{name, std::make_shared<ASTIdentifier>(name)});
            new_parent_types.emplace(name, new_child_types.at(name));
        }
    }

    PlanNodePtr new_child_node;
    if (Utils::isIdentity(new_child_assignments))
    {
        new_child_node = child->getChildren()[0];
    }
    else
    {
        auto new_child_step = std::make_shared<ProjectionStep>(
            child->getChildren()[0]->getStep()->getOutputStream(), new_child_assignments, new_child_types);
        new_child_node = std::make_shared<ProjectionNode>(child->getId(), std::move(new_child_step), PlanNodes{child->getChildren()[0]});
    }

    auto new_parent_step = std::make_shared<ProjectionStep>(
        new_child_node->getStep()->getOutputStream(),
        new_parent_assignments,
        new_parent_types,
        parent_step.isFinalProject() /*, new_dynamic_filters*/);
    return std::make_shared<ProjectionNode>(parent->getId(), std::move(new_parent_step), PlanNodes{new_child_node});
}

/**
 * Candidates for inlining are
 *    1. references to simple constants
 *    2. references to complex expressions that
 *      a. appear only once across all expressions
 *      b. are not identity projections
 * which come from the child, as opposed to an enclosing scope.
 */
std::set<String> InlineProjections::extractInliningTargets(ProjectionNode * parent, ProjectionNode * child, ContextMutablePtr & context)
{
    std::set<String> child_output_set;
    for (const auto & column : child->getStep()->getOutputStream().header)
    {
        child_output_set.emplace(column.name);
    }

    const auto & parent_step = *parent->getStep();
    const auto & child_step = *child->getStep();

    std::unordered_map<String, UInt32> dependencies;
    for (auto & assignment : parent_step.getAssignments())
    {
        auto expr = assignment.second;
        std::set<std::string> symbols = SymbolsExtractor::extract(expr);
        for (const auto & symbol : symbols)
        {
            if (child_output_set.contains(symbol))
            {
                dependencies[symbol]++;
            }
        }
    }

    // find references to simple constants
    std::set<String> constants;
    for (auto & dependency : dependencies)
    {
        String symbol = dependency.first;
        auto expr = child_step.getAssignments().at(symbol);
        if (expr->as<ASTLiteral>())
        {
            constants.emplace(symbol);
        }
    }

    std::set<String> singletons;
    for (auto & dependency : dependencies)
    {
        const auto & symbol = dependency.first;
        UInt32 ref = dependency.second;
        // reference appears just once across all expressions in parent project node
        if (ref != 1)
        {
            continue;
        }

        auto& expr = child_step.getAssignments().at(symbol);

        if(!ExpressionDeterminism::isDeterministic(expr, context)) {
            continue;
        }


        // skip identities, otherwise, this rule will keep firing forever
        bool identity = false;
        if (expr->as<ASTIdentifier>())
        {
            const auto & identifier = expr->as<ASTIdentifier &>();
            if (identifier.name() == symbol)
            {
                identity = true;
            }
        }
        if (!identity)
        {
            singletons.emplace(symbol);
        }
    }
    singletons.insert(constants.begin(), constants.end());

    // inline all if remaining are not used or identity.
    std::set<String> identities_or_not_used;
    for (const auto & assignment : child_step.getAssignments())
    {
        if (!ExpressionDeterminism::isDeterministic(assignment.second, context))
            break;

        if (!dependencies.contains(assignment.first))
        {
            identities_or_not_used.emplace(assignment.first);
        }
        else if (const auto * identifier = assignment.second->as<ASTIdentifier>())
        {
            if (identifier->name() == assignment.first)
            {
                identities_or_not_used.emplace(assignment.first);
            }
        }
    }

    if (singletons.size() + identities_or_not_used.size() == child_output_set.size())
    {
        return child_output_set;
    }

    return singletons;
}

ASTPtr InlineProjections::inlineReferences(const ConstASTPtr & expression, Assignments & assignments)
{
    return ExpressionInliner::inlineSymbols(expression, assignments);
}

PatternPtr InlineProjectionIntoJoin::getPattern() const
{
    return Patterns::join();
}

TransformResult InlineProjectionIntoJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    auto old_join_node = dynamic_cast<JoinNode *>(node.get());
    if (!old_join_node)
        return {};

    PlanNodePtr left = node->getChildren()[0];
    PlanNodePtr right = node->getChildren()[1];

    bool rewrite = false;
    if (left->getStep()->getType() == IQueryPlanStep::Type::Projection)
    {
        const auto * step = dynamic_cast<const ProjectionStep *>(left->getStep().get());
        if (Utils::isIdentity(*step))
        {
            left = left->getChildren()[0];
            rewrite = true;
        }
    }
    if (right->getStep()->getType() == IQueryPlanStep::Type::Projection)
    {
        const auto * step = dynamic_cast<const ProjectionStep *>(right->getStep().get());
        if (Utils::isIdentity(*step))
        {
            right = right->getChildren()[0];
            rewrite = true;
        }
    }

    if (!rewrite)
        return {};

    const auto & join_step = *old_join_node->getStep();
    auto new_join_step = std::make_shared<JoinStep>(
        DataStreams{left->getStep()->getOutputStream(), right->getStep()->getOutputStream()},
        DataStream{.header = join_step.getOutputStream().header},
        join_step.getKind(),
        join_step.getStrictness(),
        join_step.getLeftKeys(),
        join_step.getRightKeys(),
        join_step.getFilter(),
        join_step.isHasUsing(),
        join_step.getRequireRightKeys(),
        join_step.getAsofInequality(),
        join_step.getDistributionType(),
        join_step.isMagic());
    PlanNodePtr new_join_node = std::make_shared<JoinNode>(context.context->nextNodeId(), std::move(new_join_step), PlanNodes{left, right});
    return new_join_node;
}

PatternPtr InlineProjectionOnJoinIntoJoin::getPattern() const
{
    return Patterns::project()
        ->matchingStep<ProjectionStep>([](const auto & step) { return Utils::isIdentity(step); })
        ->withSingle(Patterns::join());
}

TransformResult InlineProjectionOnJoinIntoJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & context)
{
    const auto * projection_step = dynamic_cast<const ProjectionStep *>(node->getStep().get());
    const auto * join_step = dynamic_cast<const JoinStep *>(node->getChildren()[0]->getStep().get());

    auto new_join_step = std::make_shared<JoinStep>(
        join_step->getInputStreams(),
        DataStream{.header = projection_step->getOutputStream().header},
        join_step->getKind(),
        join_step->getStrictness(),
        join_step->getLeftKeys(),
        join_step->getRightKeys(),
        join_step->getFilter(),
        join_step->isHasUsing(),
        join_step->getRequireRightKeys(),
        join_step->getAsofInequality(),
        join_step->getDistributionType(),
        join_step->isMagic());

    return {PlanNodeBase::createPlanNode(context.context->nextNodeId(), new_join_step, node->getChildren()[0]->getChildren())};
}

}
