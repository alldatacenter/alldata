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

#include <Optimizer/Rule/Transformation/PullOuterJoin.h>

#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rule/Patterns.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
/**
 * (A left join B) join C
 */
PatternPtr PullLeftJoinThroughInnerJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([](const JoinStep & join_step) {
            if (join_step.getStrictness() != ASTTableJoin::Strictness::Unspecified
                && join_step.getStrictness() != ASTTableJoin::Strictness::All && join_step.getStrictness() != ASTTableJoin::Strictness::Any)
            {
                return false;
            }

            return join_step.getKind() == ASTTableJoin::Kind::Inner && PredicateUtils::isTruePredicate(join_step.getFilter())
                && !join_step.isMagic();
        })
        ->with(
            {Patterns::join()
                 ->matchingStep<JoinStep>([](const JoinStep & join_step) {
                     if (join_step.getStrictness() != ASTTableJoin::Strictness::Unspecified
                         && join_step.getStrictness() != ASTTableJoin::Strictness::All
                         && join_step.getStrictness() != ASTTableJoin::Strictness::Any)
                     {
                         return false;
                     }
                     return join_step.getKind() == ASTTableJoin::Kind::Left && PredicateUtils::isTruePredicate(join_step.getFilter())
                         && !join_step.isMagic();
                 })
                 ->with({Patterns::any(), Patterns::any()}),
             Patterns::any()});
}

static std::optional<PlanNodePtr> createNewJoin(
    const JoinStep * inner_join,
    const JoinStep * left_join,
    PlanNodePtr first,
    PlanNodePtr second,
    PlanNodePtr C,
    Context & context,
    NamesAndTypes output_stream = {})
{
    auto & left_keys = inner_join->getLeftKeys();

    NameSet first_output;
    for (const auto & item : first->getStep()->getOutputStream().header)
    {
        first_output.insert(item.name);
    }

    for (auto & left_key : left_keys)
    {
        // C only join A
        if (!first_output.contains(left_key))
        {
            return {};
        }
    }

    // A join C
    NamesAndTypes output;
    for (const auto & item : first->getStep()->getOutputStream().header)
    {
        output.emplace_back(item.name, item.type);
    }
    for (const auto & item : C->getStep()->getOutputStream().header)
    {
        output.emplace_back(item.name, item.type);
    }
    auto new_left = std::make_shared<JoinStep>(
        DataStreams{first->getStep()->getOutputStream(), C->getStep()->getOutputStream()},
        DataStream{output},
        ASTTableJoin::Kind::Inner,
        ASTTableJoin::Strictness::All,
        inner_join->getLeftKeys(),
        inner_join->getRightKeys());
    auto new_left_node = JoinNode::createPlanNode(context.nextNodeId(), std::move(new_left), {first, C});

    DataStream data_stream{output_stream};
    if (output_stream.empty())
    {
        for (const auto & item : new_left_node->getStep()->getOutputStream().header)
        {
            data_stream.header.insert(ColumnWithTypeAndName{item.type, item.name});
        }
        for (const auto & item : second->getStep()->getOutputStream().header)
        {
            data_stream.header.insert(ColumnWithTypeAndName{item.type, item.name});
        }
    }

    auto new_left_join = std::make_shared<JoinStep>(
        DataStreams{new_left_node->getStep()->getOutputStream(), second->getStep()->getOutputStream()},
        data_stream,
        ASTTableJoin::Kind::Left,
        left_join->getStrictness(),
        left_join->getLeftKeys(),
        left_join->getRightKeys(),
        PredicateConst::TRUE_VALUE,
        left_join->isHasUsing(),
        left_join->getRequireRightKeys(),
        left_join->getAsofInequality(),
        DistributionType::UNKNOWN);


    return PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(new_left_join), {new_left_node, second});
}

TransformResult PullLeftJoinThroughInnerJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    const auto * inner_join = dynamic_cast<const JoinStep *>(node->getStep().get());
    const auto * left_join = dynamic_cast<const JoinStep *>(node->getChildren()[0]->getStep().get());

    // A
    auto first = node->getChildren()[0]->getChildren()[0];
    // B
    auto second = node->getChildren()[0]->getChildren()[1];
    auto c = node->getChildren()[1];
    auto res = createNewJoin(
        inner_join, left_join, first, second, c, *rule_context.context, node->getStep()->getOutputStream().header.getNamesAndTypes());
    if (res)
    {
        return res.value();
    }
    return {};
}

/**
 * (projection(A left join B)) join C
 */
PatternPtr PullLeftJoinProjectionThroughInnerJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([](const JoinStep & join_step) {
            return join_step.getKind() == ASTTableJoin::Kind::Inner && PredicateUtils::isTruePredicate(join_step.getFilter())
                && !join_step.isMagic();
        })
        ->with(
            {Patterns::project()->withSingle(Patterns::join()
                                                 ->matchingStep<JoinStep>([](const JoinStep & join_step) {
                                                     return join_step.getKind() == ASTTableJoin::Kind::Left
                                                         && PredicateUtils::isTruePredicate(join_step.getFilter()) && !join_step.isMagic();
                                                 })
                                                 ->with({Patterns::any(), Patterns::any()})),
             Patterns::any()});
}
TransformResult PullLeftJoinProjectionThroughInnerJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    const auto * inner_join = dynamic_cast<const JoinStep *>(node->getStep().get());
    auto project_node = node->getChildren()[0];
    const auto * left_join = dynamic_cast<const JoinStep *>(project_node->getChildren()[0]->getStep().get());

    // A
    auto first = project_node->getChildren()[0]->getChildren()[0];
    // B
    auto second = project_node->getChildren()[0]->getChildren()[1];
    auto c = node->getChildren()[1];

    auto new_join = createNewJoin(inner_join, left_join, first, second, c, *rule_context.context);

    if (!new_join)
    {
        return {};
    }

    auto result = new_join.value();

    const auto * projection_step = dynamic_cast<const ProjectionStep *>(project_node->getStep().get());
    std::unordered_map<String, ConstASTPtr> symbol_to_ast;
    for (const auto & assignment : projection_step->getAssignments())
    {
        symbol_to_ast[assignment.first] = assignment.second;
    }

    Assignments assignments;
    NameToType name_to_type;
    for (const auto & item : inner_join->getOutputStream().header)
    {
        if (symbol_to_ast.contains(item.name))
        {
            assignments.emplace_back(item.name, symbol_to_ast[item.name]);
        }
        else
        {
            assignments.emplace_back(item.name, std::make_shared<ASTIdentifier>(item.name));
        }
        name_to_type[item.name] = item.type;
    }

    auto new_project_step = std::make_shared<ProjectionStep>(result->getStep()->getOutputStream(), assignments, name_to_type);

    return PlanNodeBase::createPlanNode(rule_context.context->nextNodeId(), std::move(new_project_step), {result});
}

/**
 * (filter(A left join B)) join C
 */
PatternPtr PullLeftJoinFilterThroughInnerJoin::getPattern() const
{
    return Patterns::join()
        ->matchingStep<JoinStep>([](const JoinStep & join_step) {
            return join_step.getKind() == ASTTableJoin::Kind::Inner && PredicateUtils::isTruePredicate(join_step.getFilter())
                && !join_step.isMagic();
        })
        ->with(
            {Patterns::filter()->withSingle(Patterns::join()
                                                ->matchingStep<JoinStep>([](const JoinStep & join_step) {
                                                    return join_step.getKind() == ASTTableJoin::Kind::Left
                                                        && PredicateUtils::isTruePredicate(join_step.getFilter()) && !join_step.isMagic();
                                                })
                                                ->with({Patterns::any(), Patterns::any()})),
             Patterns::any()});
}

TransformResult PullLeftJoinFilterThroughInnerJoin::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    const auto * inner_join = dynamic_cast<const JoinStep *>(node->getStep().get());
    auto filter_node = node->getChildren()[0];
    const auto * left_join = dynamic_cast<const JoinStep *>(filter_node->getChildren()[0]->getStep().get());

    // A
    auto first = filter_node->getChildren()[0]->getChildren()[0];
    // B
    auto second = filter_node->getChildren()[0]->getChildren()[1];
    auto c = node->getChildren()[1];

    auto new_join = createNewJoin(inner_join, left_join, first, second, c, *rule_context.context);

    if (!new_join)
    {
        return {};
    }

    auto result = new_join.value();
    auto new_filter_step = filter_node->getStep()->copy(rule_context.context);
    auto new_filter_node = PlanNodeBase::createPlanNode(rule_context.context->nextNodeId(), std::move(new_filter_step), {result});
    if (inner_join->getOutputStream().header.columns() < new_filter_node->getStep()->getOutputStream().header.columns())
    {
        Assignments assignments;
        NameToType name_to_type;
        for (const auto & item : inner_join->getOutputStream().header)
        {
            assignments.emplace_back(item.name, std::make_shared<ASTIdentifier>(item.name));
            name_to_type[item.name] = item.type;
        }
        auto new_project_step = std::make_shared<ProjectionStep>(new_filter_node->getStep()->getOutputStream(), assignments, name_to_type);
        return PlanNodeBase::createPlanNode(rule_context.context->nextNodeId(), std::move(new_project_step), {new_filter_node});
    }
    return new_filter_node;
}

}
