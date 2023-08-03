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

#include <Optimizer/Rule/Rewrite/SimplifyExpressionRules.h>

#include <Optimizer/ExpressionInterpreter.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rule/Patterns.h>
#include <Optimizer/SimplifyExpressions.h>
#include <Optimizer/UnwrapCastInComparison.h>
#include <Optimizer/LiteralEncoder.h>
#include <Optimizer/Utils.h>
#include <Parsers/formatAST.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/JoinStep.h>

namespace DB
{
PatternPtr CommonPredicateRewriteRule::getPattern() const
{
    return Patterns::filter();
}

TransformResult CommonPredicateRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    if (!context->getSettingsRef().enable_common_predicate_rewrite)
    {
        return {};
    }

    auto * old_filter_node = dynamic_cast<FilterNode *>(node.get());
    if (!old_filter_node)
        return {};

    const auto & step = *old_filter_node->getStep();
    auto predicate = step.getFilter();

    ConstASTPtr rewritten = CommonPredicatesRewriter::rewrite(predicate, context);
    if (rewritten->getColumnName() == predicate->getColumnName())
    {
        return {};
    }

    auto filter_step
        = std::make_shared<FilterStep>(node->getChildren()[0]->getStep()->getOutputStream(), rewritten, step.removesFilterColumn());
    auto filter_node = FilterNode::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{node->getChildren()[0]});

    return filter_node;
}

PatternPtr SwapPredicateRewriteRule::getPattern() const
{
    return Patterns::filter();
}

TransformResult SwapPredicateRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    if (!context->getSettingsRef().enable_swap_predicate_rewrite)
    {
        return {};
    }
    auto * old_filter_node = dynamic_cast<FilterNode *>(node.get());
    if (!old_filter_node)
        return {};

    const auto & step = *old_filter_node->getStep();
    const auto & predicate = step.getFilter();

    ConstASTPtr rewritten = SwapPredicateRewriter::rewrite(predicate, context);
    if (rewritten->getColumnName() == predicate->getColumnName())
    {
        return {};
    }

    auto filter_step
        = std::make_shared<FilterStep>(node->getChildren()[0]->getStep()->getOutputStream(), rewritten, step.removesFilterColumn());
    auto filter_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{node->getChildren()[0]});

    return filter_node;
}

PatternPtr SimplifyPredicateRewriteRule::getPattern() const
{
    return Patterns::filter();
}

TransformResult SimplifyPredicateRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    auto * old_filter_node = dynamic_cast<FilterNode *>(node.get());
    if (!old_filter_node)
        return {};

    const auto & step = *old_filter_node->getStep();
    auto predicate = step.getFilter();

    ConstASTPtr rewritten = ExpressionInterpreter::optimizePredicate(predicate, step.getOutputStream().header.getNamesToTypes(), context);

    if (PredicateUtils::isTruePredicate(rewritten))
        return node->getChildren()[0];

    if (rewritten->getColumnName() == predicate->getColumnName())
        return {};

    auto filter_step
        = std::make_shared<FilterStep>(node->getChildren()[0]->getStep()->getOutputStream(), rewritten, step.removesFilterColumn());
    auto filter_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{node->getChildren()[0]});

    return filter_node;
}

PatternPtr UnWarpCastInPredicateRewriteRule::getPattern() const
{
    return Patterns::filter();
}

TransformResult UnWarpCastInPredicateRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    if (!context->getSettingsRef().enable_unwarp_cast_in)
    {
        return {};
    }
    auto * old_filter_node = dynamic_cast<FilterNode *>(node.get());
    if (!old_filter_node)
        return {};

    const auto & step = *old_filter_node->getStep();
    auto predicate = step.getFilter();

    auto column_types = step.getOutputStream().header.getNamesToTypes();
    ASTPtr rewritten = unwrapCastInComparison(predicate, context, column_types);
    if (!rewritten)
    {
        rewritten = predicate->clone();
    }

    if (rewritten->getColumnName() == predicate->getColumnName())
    {
        return {};
    }

    auto filter_step
        = std::make_shared<FilterStep>(node->getChildren()[0]->getStep()->getOutputStream(), rewritten, step.removesFilterColumn());
    auto filter_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{node->getChildren()[0]});

    return filter_node;
}

PatternPtr SimplifyJoinFilterRewriteRule::getPattern() const
{
    return Patterns::join()->matchingStep<JoinStep>([&](const JoinStep & s) { return !PredicateUtils::isTruePredicate(s.getFilter()); });
}

TransformResult SimplifyJoinFilterRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;

    auto * old_join_node = dynamic_cast<JoinNode *>(node.get());
    if (!old_join_node)
        return {};

    const auto & step = *old_join_node->getStep();
    auto filter = step.getFilter();

    NamesAndTypes column_types;

    bool has_outer_join_semantic = isAny(step.getStrictness()) || isAll(step.getStrictness()) || isAsof(step.getStrictness());
    bool make_nullable_for_left = has_outer_join_semantic && isRightOrFull(step.getKind());
    bool make_nullable_for_right = has_outer_join_semantic && isLeftOrFull(step.getKind());

    auto type_with_nullable = [&](bool make_nullable, const NamesAndTypes & header) {
        if (make_nullable)
        {
            for (const auto & column : header)
            {
                if (column.type->canBeInsideNullable())
                {
                    NameAndTypePair name_and_type{column.name, makeNullable(column.type)};
                    column_types.emplace_back(name_and_type);
                }
                else
                {
                    column_types.emplace_back(column);
                }
            }
        }
        else
        {
            column_types.insert(column_types.end(), header.begin(), header.end());
        }
    };

    type_with_nullable(make_nullable_for_left, step.getInputStreams()[0].header.getNamesAndTypes());
    type_with_nullable(make_nullable_for_right, step.getInputStreams()[1].header.getNamesAndTypes());

    NameToType name_to_type;
    for (const auto & item: column_types)
        name_to_type.emplace(item.name, item.type);

    ASTPtr rewritten = ExpressionInterpreter::optimizePredicate(filter, name_to_type, context);

    if (rewritten->getColumnName() == filter->getColumnName())
    {
        return {};
    }

    QueryPlanStepPtr join_step = std::make_shared<JoinStep>(
        step.getInputStreams(),
        step.getOutputStream(),
        step.getKind(),
        step.getStrictness(),
        step.getLeftKeys(),
        step.getRightKeys(),
        rewritten,
        step.isHasUsing(),
        step.getRequireRightKeys(),
        step.getAsofInequality(),
        step.getDistributionType());

    PlanNodePtr join_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(join_step), node->getChildren());
    return join_node;
}

PatternPtr SimplifyExpressionRewriteRule::getPattern() const
{
    return Patterns::project();
}

TransformResult SimplifyExpressionRewriteRule::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;

    const auto * project = dynamic_cast<const ProjectionStep *>(node->getStep().get());
    if (!project)
        return {};

    Assignments assignments;
    NameToType name_to_type;
    auto column_types = node->getChildren()[0]->getCurrentDataStream().header.getNamesToTypes();
    auto interpreter = ExpressionInterpreter::basicInterpreter(std::move(column_types), context);
    bool rewrite = false;
    for (const auto & assignment : project->getAssignments())
    {
        auto res = interpreter.optimizeExpression(assignment.second);
        assignments.emplace_back(assignment.first, res.second);
        name_to_type.emplace(assignment.first, res.first);
        // auto output_types = project->getOutputStream().header.getNamesToTypes();
        // assert(res.first->equals(*output_types.at(assignment.first)));
        if (!ASTEquality::compareTree(assignments.back().second, assignment.second))
            rewrite = true;
    }
    if (!rewrite)
        return {};

    return PlanNodeBase::createPlanNode(
        context->nextNodeId(),
        std::make_shared<ProjectionStep>(
            node->getChildren()[0]->getStep()->getOutputStream(),
            assignments,
            name_to_type,
            project->isFinalProject(),
            project->getDynamicFilters()),
        PlanNodes{node->getChildren()[0]});
}

PatternPtr MergePredicatesUsingDomainTranslator::getPattern() const
{
    return Patterns::filter();
}

TransformResult MergePredicatesUsingDomainTranslator::transformImpl(PlanNodePtr node, const Captures &, RuleContext & rule_context)
{
    auto & context = rule_context.context;
    auto * old_filter_node = dynamic_cast<FilterNode *>(node.get());
    if (!old_filter_node)
        return {};

    const auto & step = *old_filter_node->getStep();
    auto predicate = step.getFilter()->clone();

    using ExtractionReuslt = DB::Predicate::ExtractionResult;
    using DomainTranslator = DB::Predicate::DomainTranslator;

    DomainTranslator domain_translator = DomainTranslator(context);
    ExtractionReuslt rewritten = domain_translator.getExtractionResult(predicate, step.getOutputStream().header.getNamesAndTypes());

    if (domain_translator.isIgnored() || predicate->getColumnName() == rewritten.remaining_expression->getColumnName())
        return {};

    ASTPtr combine_extraction_result = PredicateUtils::combineConjuncts({
        domain_translator.toPredicate(rewritten.tuple_domain),
        rewritten.remaining_expression});

    if (combine_extraction_result->getColumnName() == predicate->getColumnName())
        return {};

    auto filter_step
        = std::make_shared<FilterStep>(node->getChildren()[0]->getStep()->getOutputStream(), combine_extraction_result, step.removesFilterColumn());
    auto filter_node = PlanNodeBase::createPlanNode(context->nextNodeId(), std::move(filter_step), PlanNodes{node->getChildren()[0]});

    return filter_node;
}

}
