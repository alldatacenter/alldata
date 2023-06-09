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

#include <memory>
#include <Optimizer/Rewriter/PredicatePushdown.h>

#include <Analyzers/TypeAnalyzer.h>
#include <DataTypes/getLeastSupertype.h>
#include <DataTypes/DataTypeNullable.h>
#include <Optimizer/DynamicFilters.h>
#include <Optimizer/EqualityInference.h>
#include <Optimizer/ExpressionDeterminism.h>
#include <Optimizer/ExpressionEquivalence.h>
#include <Optimizer/ExpressionInliner.h>
#include <Optimizer/ExpressionInterpreter.h>
#include <Optimizer/SymbolUtils.h>
#include <Optimizer/Utils.h>
#include <Optimizer/makeCastFunction.h>
#include <Parsers/formatAST.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/AssignUniqueIdStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/UnionStep.h>
#include <Common/FieldVisitorConvertToNumber.h>
#include <QueryPlan/SymbolMapper.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int PLAN_BUILD_ERROR;
}

void PredicatePushdown::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    auto cte_reference_counts = plan.getCTEInfo().collectCTEReferenceCounts(plan.getPlanNode());
    PredicateVisitor visitor{dynamic_filtering, context, plan.getCTEInfo(), cte_reference_counts};
    PredicateContext predicate_context{
        .predicate = PredicateConst::TRUE_VALUE, .extra_predicate_for_simplify_outer_join = PredicateConst::TRUE_VALUE};
    auto result = VisitorUtil::accept(plan.getPlanNode(), visitor, predicate_context);
    plan.update(result);
}

PlanNodePtr PredicateVisitor::visitPlanNode(PlanNodeBase & node, PredicateContext & predicate_context)
{
    PredicateContext true_context{.predicate = PredicateConst::TRUE_VALUE,
                                  .extra_predicate_for_simplify_outer_join = PredicateConst::TRUE_VALUE};
    PlanNodePtr rewritten = processChild(node, true_context);
    if (!PredicateUtils::isTruePredicate(predicate_context.predicate))
    {
        // we cannot push our predicate down any further
        auto filter_step = std::make_shared<FilterStep>(rewritten->getStep()->getOutputStream(), predicate_context.predicate);
        auto filter_node = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{rewritten});
        return filter_node;
    }
    return rewritten;
}

PlanNodePtr PredicateVisitor::visitProjectionNode(ProjectionNode & node, PredicateContext & predicate_context)
{
    const auto & step = *node.getStep();
    auto assignments = step.getAssignments();
    std::set<String> deterministic_symbols = ExpressionDeterminism::getDeterministicSymbols(assignments, context);

    // Push down conjuncts from the inherited predicate that only
    // depend on deterministic assignments with certain limitations.
    std::vector<ConstASTPtr> deterministic_conjuncts;
    std::vector<ConstASTPtr> non_deterministic_conjuncts;
    std::vector<ConstASTPtr> predicates = PredicateUtils::extractConjuncts(predicate_context.predicate);
    for (auto & predicate : predicates)
    {
        std::set<std::string> symbols = SymbolsExtractor::extract(predicate);
        bool contains = true;
        for (const auto & symbol : symbols)
        {
            if (!deterministic_symbols.contains(symbol))
            {
                contains = false;
            }
        }
        if (contains)
        {
            deterministic_conjuncts.emplace_back(predicate);
        }
        else
        {
            non_deterministic_conjuncts.emplace_back(predicate);
        }
    }

    // We partition the expressions in the deterministic_conjuncts into two lists,
    // and only inline the expressions that are in the inlining targets list.
    std::vector<ConstASTPtr> inlining_conjuncts;
    std::vector<ConstASTPtr> non_inlining_conjuncts;
    for (auto & conjunct : deterministic_conjuncts)
    {
        if (PredicateUtils::isInliningCandidate(conjunct, node))
        {
            inlining_conjuncts.emplace_back(conjunct);
        }
        else
        {
            non_inlining_conjuncts.emplace_back(conjunct);
        }
    }


    std::vector<ConstASTPtr> inlined_deterministic_conjuncts;
    for (auto & conjunct : inlining_conjuncts)
    {
        auto inlined = ExpressionInliner::inlineSymbols(conjunct, assignments);
        inlined_deterministic_conjuncts.emplace_back(inlined);
    }

    PredicateContext expression_context{
        .predicate = PredicateUtils::combineConjuncts(inlined_deterministic_conjuncts),
        .extra_predicate_for_simplify_outer_join =
            ExpressionInliner::inlineSymbols(predicate_context.extra_predicate_for_simplify_outer_join, assignments)};
    PlanNodePtr rewritten = processChild(node, expression_context);

    // All deterministic conjuncts that contains non-inlining targets, and non-deterministic conjuncts,
    // if any, will be in the filter node.
    for (auto & conjunct : non_deterministic_conjuncts)
    {
        non_inlining_conjuncts.emplace_back(conjunct);
    }
    if (!non_inlining_conjuncts.empty())
    {
        auto filter_step = std::make_shared<FilterStep>(
            rewritten->getStep()->getOutputStream(), PredicateUtils::combineConjuncts(non_inlining_conjuncts));
        auto filter_node = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{rewritten});
        rewritten = filter_node;
    }
    return rewritten;
}

PlanNodePtr PredicateVisitor::visitFilterNode(FilterNode & node, PredicateContext & predicate_context)
{
    const auto & step = *node.getStep();
    auto predicates = std::vector<ConstASTPtr>{step.getFilter(), predicate_context.predicate};
    auto predicate = PredicateUtils::combineConjuncts(predicates);
    PredicateContext filter_context{
        .predicate = predicate,
        .extra_predicate_for_simplify_outer_join = predicate_context.extra_predicate_for_simplify_outer_join};
    PlanNodePtr rewritten = process(*node.getChildren()[0], filter_context);

    if (rewritten->getStep()->getType() != IQueryPlanStep::Type::Filter)
    {
        return rewritten;
    }

    if (rewritten->getStep()->getType() == IQueryPlanStep::Type::Filter)
    {
        if (rewritten->getChildren()[0] != node.getChildren()[0])
        {
            return rewritten;
        }
        auto rewritten_step_ptr = rewritten->getStep();
        const auto & rewritten_step = dynamic_cast<const FilterStep &>(*rewritten_step_ptr);

        // TODO a more reasonable way to do this
        // see ExpressionEquivalence
        if (step.getFilter() != rewritten_step.getFilter())
        {
            return rewritten;
        }
    }
    return node.shared_from_this();
}

PlanNodePtr PredicateVisitor::visitAggregatingNode(AggregatingNode & node, PredicateContext & predicate_context)
{

    const auto & step = *node.getStep();
    const auto & keys = step.getKeys();

    // TODO: in case of grouping sets, we should be able to push the filters over grouping keys below the aggregation
    // and also preserve the filter above the aggregation if it has an empty grouping set
    if (keys.empty())
    {
        return visitPlanNode(node, predicate_context);
    }

    ConstASTPtr inherited_predicate = predicate_context.predicate;
    EqualityInference equality_inference = EqualityInference::newInstance(inherited_predicate, context);

    std::vector<ConstASTPtr> pushdown_conjuncts;
    std::vector<ConstASTPtr> post_aggregation_conjuncts;

    // Strip out non-deterministic conjuncts
    for (auto & conjunct : PredicateUtils::extractConjuncts(inherited_predicate))
    {
        if (!ExpressionDeterminism::isDeterministic(conjunct, context))
        {
            post_aggregation_conjuncts.emplace_back(conjunct);
        }
    }

    inherited_predicate = ExpressionDeterminism::filterDeterministicConjuncts(inherited_predicate, context);

    // Sort non-equality predicates by those that can be pushed down and those that cannot
    std::set<String> grouping_keys;
    for (const auto & key : keys)
    {
        grouping_keys.emplace(key);
    }

    for (auto & conjunct : EqualityInference::nonInferrableConjuncts(inherited_predicate, context))
    {
        ASTPtr rewritten = equality_inference.rewrite(conjunct, grouping_keys);
        if (rewritten != nullptr)
        {
            pushdown_conjuncts.emplace_back(rewritten);
        }
        else
        {
            post_aggregation_conjuncts.emplace_back(conjunct);
        }
    }

    // Add the equality predicates back in
    EqualityPartition equality_partition = equality_inference.partitionedBy(grouping_keys);
    for (auto & conjunct : equality_partition.getScopeEqualities())
    {
        pushdown_conjuncts.emplace_back(conjunct);
    }
    for (auto & conjunct : equality_partition.getScopeComplementEqualities())
    {
        post_aggregation_conjuncts.emplace_back(conjunct);
    }
    for (auto & conjunct : equality_partition.getScopeStraddlingEqualities())
    {
        post_aggregation_conjuncts.emplace_back(conjunct);
    }

    PredicateContext agg_context{
        .predicate = PredicateUtils::combineConjuncts(pushdown_conjuncts),
        .extra_predicate_for_simplify_outer_join = PredicateConst::TRUE_VALUE};
    PlanNodePtr rewritten = process(*node.getChildren()[0], agg_context);

    PlanNodePtr output = node.shared_from_this();
    if (rewritten != node.getChildren()[0])
    {
        output = std::make_shared<AggregatingNode>(context->nextNodeId(), node.getStep(), PlanNodes{rewritten});
    }
    if (!post_aggregation_conjuncts.empty())
    {
        auto filter_step = std::make_shared<FilterStep>(
            output->getStep()->getOutputStream(), PredicateUtils::combineConjuncts(post_aggregation_conjuncts));
        output = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{output});
    }
    return output;
}

PlanNodePtr PredicateVisitor::visitJoinNode(JoinNode & node, PredicateContext & predicate_context)
{
    ConstASTPtr & inherited_predicate = predicate_context.predicate;
    auto step = node.getStep();

    // RequireRightKeys is clickhouse sql only, we don't process this kind of join.
    if(step->getRequireRightKeys().has_value())
    {
        return visitPlanNode(node, predicate_context);
    }

    // Asof is clickhouse sql only, we don't process this kind of join.
    if (step->getStrictness() == ASTTableJoin::Strictness::Asof)
    {
        return visitPlanNode(node, predicate_context);
    }

    PlanNodePtr & left = node.getChildren()[0];
    PlanNodePtr & right = node.getChildren()[1];
    ConstASTPtr left_effective_predicate = EffectivePredicateExtractor::extract(left, cte_info, context);
    ConstASTPtr right_effective_predicate = EffectivePredicateExtractor::extract(right, cte_info, context);
    ConstASTPtr join_predicate = PredicateUtils::extractJoinPredicate(node);

    std::set<String> left_symbols;
    for (const auto & column : left->getStep()->getOutputStream().header)
    {
        left_symbols.emplace(column.name);
    }
    std::set<String> right_symbols;
    for (const auto & column : right->getStep()->getOutputStream().header)
    {
        right_symbols.emplace(column.name);
    }

    std::vector<ConstASTPtr> conjuncts_for_simplify_outer_join{
        inherited_predicate, predicate_context.extra_predicate_for_simplify_outer_join};
    auto predicate_for_simplify_outer_join = PredicateUtils::combineConjuncts(conjuncts_for_simplify_outer_join);
    tryNormalizeOuterToInnerJoin(node, predicate_for_simplify_outer_join, context);
    step = node.getStep(); // update step since it may be changed by `tryNormalizeOuterToInnerJoin`

    ConstASTPtr left_predicate;
    ConstASTPtr right_predicate;
    ConstASTPtr post_join_predicate;
    ConstASTPtr new_join_predicate;

    ASTTableJoin::Kind kind = step->getKind();
    if (kind == ASTTableJoin::Kind::Inner || kind == ASTTableJoin::Kind::Cross)
    {
        InnerJoinResult inner_result = processInnerJoin(
            inherited_predicate, left_effective_predicate, right_effective_predicate, join_predicate, left_symbols, right_symbols, context);
        left_predicate = inner_result.left_predicate;
        right_predicate = inner_result.right_predicate;
        post_join_predicate = inner_result.post_join_predicate;
        new_join_predicate = inner_result.join_predicate;
    }
    else if (kind == ASTTableJoin::Kind::Left)
    {
        OuterJoinResult left_result = processOuterJoin(
            inherited_predicate, left_effective_predicate, right_effective_predicate, join_predicate, left_symbols, right_symbols, context);
        left_predicate = left_result.outer_predicate;
        right_predicate = left_result.inner_predicate;
        post_join_predicate = left_result.post_join_predicate;
        new_join_predicate = left_result.join_predicate;
    }
    else if (kind == ASTTableJoin::Kind::Right)
    {
        OuterJoinResult left_result = processOuterJoin(
            inherited_predicate, right_effective_predicate, left_effective_predicate, join_predicate, right_symbols, left_symbols, context);
        left_predicate = left_result.inner_predicate;
        right_predicate = left_result.outer_predicate;
        post_join_predicate = left_result.post_join_predicate;
        new_join_predicate = left_result.join_predicate;
    }
    else if (kind == ASTTableJoin::Kind::Full)
    {
        left_predicate = PredicateConst::TRUE_VALUE;
        right_predicate = PredicateConst::TRUE_VALUE;
        post_join_predicate = inherited_predicate;
        new_join_predicate = join_predicate;
    }
    else
    {
        throw Exception("Unsupported join type : Comma", ErrorCodes::NOT_IMPLEMENTED);
    }

    // Create identity projections for all existing symbols
    Assignments left_assignments;
    NameToType left_types;
    const DataStream & left_output = left->getStep()->getOutputStream();
    for (const auto & column : left_output.header)
    {
        left_assignments.emplace_back(column.name, std::make_shared<ASTIdentifier>(column.name));
        left_types[column.name] = column.type;
    }

    Assignments right_assignments;
    NameToType right_types;
    const DataStream & right_output = right->getStep()->getOutputStream();
    for (const auto & column : right_output.header)
    {
        right_assignments.emplace_back(column.name, std::make_shared<ASTIdentifier>(column.name));
        right_types[column.name] = column.type;
    }

    // Create new projections for the new join clauses
    std::set<std::pair<String, String>> join_clauses;
    std::vector<ConstASTPtr> join_filters;
    auto left_type_analyzer = TypeAnalyzer::create(context, left->getStep()->getOutputStream().header.getNamesAndTypes());
    auto right_type_analyzer = TypeAnalyzer::create(context, right->getStep()->getOutputStream().header.getNamesAndTypes());
    for (auto & conjunct : PredicateUtils::extractConjuncts(new_join_predicate))
    {
        if (PredicateUtils::isJoinClause(conjunct, left_symbols, right_symbols, context))
        {
            const auto & equality = conjunct->as<ASTFunction &>();
            std::set<std::string> left_equality_symbols = SymbolsExtractor::extract(equality.arguments->getChildren()[0]);
            std::set<std::string> right_equality_symbols = SymbolsExtractor::extract(equality.arguments->getChildren()[1]);
            bool left_aligned_comparison = SymbolUtils::containsAll(left_symbols, left_equality_symbols);
            bool right_aligned_comparison = SymbolUtils::containsAll(right_symbols, right_equality_symbols);
            ASTPtr & left_expression = (left_aligned_comparison && right_aligned_comparison) ? equality.arguments->getChildren()[0]
                                                                                             : equality.arguments->getChildren()[1];
            ASTPtr & right_expression = (left_aligned_comparison && right_aligned_comparison) ? equality.arguments->getChildren()[1]
                                                                                              : equality.arguments->getChildren()[0];
            String left_symbol = left_expression->as<ASTIdentifier>() ?
                left_expression->getColumnName() : context->getSymbolAllocator()->newSymbol(left_expression->getColumnName());
            if (!left_symbols.contains(left_symbol))
            {
                left_assignments.emplace_back(left_symbol, left_expression);
                left_types[left_symbol] = left_type_analyzer.getType(left_expression);
            }
            String right_symbol = right_expression->as<ASTIdentifier>() ?
                right_expression->getColumnName() : context->getSymbolAllocator()->newSymbol(right_expression->getColumnName());
            if (!right_symbols.contains(right_symbol))
            {
                right_assignments.emplace_back(right_symbol, right_expression);
                right_types[right_symbol] = right_type_analyzer.getType(right_expression);
            }
            join_clauses.emplace(std::make_pair(left_symbol, right_symbol));
        }
        else
        {
            join_filters.emplace_back(conjunct);
        }
    }

    ConstASTPtr left_implicit_filter = PredicateConst::TRUE_VALUE;
    ConstASTPtr right_implicit_filter = PredicateConst::TRUE_VALUE;

    // TODO: broaden this by consider SEMI JOIN & ANTI JOIN
    if (isRegularJoin(*step))
    {
        auto build_implicit_filter = [](const Names & join_keys) {
            std::vector<ConstASTPtr> conjuncts;

            for (const auto & key : join_keys)
            {
                conjuncts.push_back(makeASTFunction("isNotNull", std::make_shared<ASTIdentifier>(key)));
            }

            return PredicateUtils::combineConjuncts(conjuncts);
        };

        if (!isLeftOrFull(step->getKind()))
            left_implicit_filter = build_implicit_filter(step->getLeftKeys());

        if (!isRightOrFull(step->getKind()))
            right_implicit_filter = build_implicit_filter(step->getRightKeys());
    }

    // create dynamic filters if enabled
    // add dynamic filter predicates to left side of join
    // add dynamic filter build expressions to right side projection of join
    auto dynamic_filters_results = createDynamicFilters(*step);
    if (!dynamic_filters_results.executors.empty())
    {
        std::vector<ConstASTPtr> predicates = std::move(dynamic_filters_results.executors);
        predicates.emplace_back(left_predicate);
        left_predicate = PredicateUtils::combineConjuncts(predicates);
    }

    // TODO: combine join implicit filter with inherited extra_predicate_for_simplify_outer_join
    PredicateContext left_context{.predicate = left_predicate, .extra_predicate_for_simplify_outer_join = left_implicit_filter};
    PredicateContext right_context{.predicate = right_predicate, .extra_predicate_for_simplify_outer_join = right_implicit_filter};
    PlanNodePtr left_source;
    PlanNodePtr right_source;

    bool join_clauses_unmodified = PredicateUtils::isJoinClauseUnmodified(join_clauses, step->getLeftKeys(), step->getRightKeys());
    if (!join_clauses_unmodified)
    {
        auto left_expression_step = std::make_shared<ProjectionStep>(left->getStep()->getOutputStream(), left_assignments, left_types);
        auto left_expression_node
            = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(left_expression_step), PlanNodes{left});

        auto right_expression_step = std::make_shared<ProjectionStep>(right->getStep()->getOutputStream(), right_assignments, right_types);
        auto right_expression_node
            = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(right_expression_step), PlanNodes{right});

        left_source = process(*left_expression_node, left_context);
        right_source = process(*right_expression_node, right_context);
    }
    else
    {
        left_source = process(*left, left_context);
        right_source = process(*right, right_context);
    }

    auto left_source_expression_step
        = std::make_shared<ProjectionStep>(left_source->getStep()->getOutputStream(), std::move(left_assignments), std::move(left_types));
    auto left_source_expression_node
        = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(left_source_expression_step), PlanNodes{left_source});

    auto right_source_expression_step = std::make_shared<ProjectionStep>(
        right_source->getStep()->getOutputStream(), std::move(right_assignments), std::move(right_types), false, std::move(dynamic_filters_results.dynamic_filters));
    auto right_source_expression_node
        = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(right_source_expression_step), PlanNodes{right_source});

    PlanNodePtr output_node = node.shared_from_this();

    const DataStream & left_data_stream = left_source_expression_node->getStep()->getOutputStream();
    const DataStream & right_data_stream = right_source_expression_node->getStep()->getOutputStream();

    DataStreams streams = {left_data_stream, right_data_stream};

    auto left_header = left_data_stream.header;
    auto right_header = right_data_stream.header;
    NamesAndTypes output;
    for (const auto & item : left_header)
    {
        output.emplace_back(item.name, item.type);
    }
    for (const auto & item : right_header)
    {
        output.emplace_back(item.name, item.type);
    }

    // cast extracted join keys to super type
    Names left_keys;
    Names right_keys;
    {
        bool need_project_left = false;
        bool need_project_right = false;
        Assignments left_project_assignments;
        NameToType left_project_types = left_source_expression_node->getOutputNamesToTypes();
        Assignments right_project_assignments;
        NameToType right_project_types = right_source_expression_node->getOutputNamesToTypes();
        const bool allow_extended_type_conversion = context->getSettingsRef().allow_extended_type_conversion;

        auto put_identities = [](Assignments & assignments, const Names & input_symbols)
        {
            for (const auto & symbol: input_symbols)
                assignments.emplace_back(symbol, std::make_shared<ASTIdentifier>(symbol));
        };

        put_identities(left_project_assignments, left_source_expression_node->getOutputNames());
        put_identities(right_project_assignments, right_source_expression_node->getOutputNames());

        for (const auto & clause : join_clauses)
        {
            String left_key = clause.first;
            String right_key = clause.second;
            auto left_type = left_project_types.at(left_key);
            auto right_type = right_project_types.at(right_key);
            auto res_type = getLeastSupertype({left_type, right_type}, allow_extended_type_conversion);

            auto add_join_key = [&](
                                    const auto & name,
                                    const auto & type,
                                    auto & keys,
                                    auto & assignments,
                                    auto & name_to_type,
                                    auto & need_project)
            {
                if (removeNullable(res_type)->equals(*removeNullable(type)))
                    keys.emplace_back(name);
                else
                {
                    auto casted_name = context->getSymbolAllocator()->newSymbol(name);
                    assignments.emplace_back(casted_name, makeCastFunction(std::make_shared<ASTIdentifier>(name), res_type));
                    name_to_type[casted_name] = res_type;
                    keys.emplace_back(casted_name);
                    need_project = true;
                }
            };

            add_join_key(left_key, left_type, left_keys, left_project_assignments, left_project_types, need_project_left);
            add_join_key(right_key, right_type, right_keys, right_project_assignments, right_project_types, need_project_right);
        }

        if (need_project_left)
        {
            auto left_project_step
                = std::make_shared<ProjectionStep>(left_source_expression_node->getStep()->getOutputStream(), left_project_assignments, left_project_types);
            left_source_expression_node
                = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(left_project_step), PlanNodes{left_source_expression_node});
        }

        if (need_project_right)
        {
            auto right_project_step
                = std::make_shared<ProjectionStep>(right_source_expression_node->getStep()->getOutputStream(), right_project_assignments, right_project_types);
            right_source_expression_node
                = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(right_project_step), PlanNodes{right_source_expression_node});
        }
    }

    ASTPtr new_join_filter = PredicateUtils::combineConjuncts(join_filters);

    std::shared_ptr<JoinStep> join_step;
    if (kind == ASTTableJoin::Kind::Cross)
    {
        join_step = std::make_shared<JoinStep>(
            streams,
            DataStream{.header = output},
            ASTTableJoin::Kind::Inner,
            ASTTableJoin::Strictness::All,
            std::move(left_keys),
            std::move(right_keys),
            new_join_filter,
            step->isHasUsing(),
            step->getRequireRightKeys(),
            step->getAsofInequality(),
            step->getDistributionType(),
            step->isMagic());
    }
    else
    {
        join_step = std::make_shared<JoinStep>(
            streams,
            DataStream{.header = output},
            kind,
            step->getStrictness(),
            std::move(left_keys),
            std::move(right_keys),
            new_join_filter,
            step->isHasUsing(),
            step->getRequireRightKeys(),
            step->getAsofInequality(),
            step->getDistributionType(),
            step->isMagic());
    }

    auto join_node = std::make_shared<JoinNode>(
        context->nextNodeId(), join_step, PlanNodes{left_source_expression_node, right_source_expression_node});

    /**
     * Predicate push down may produce nest loop join with right join, which is not supported by nest loop join.
     * todo: remove this if hash join support filter or nest loop join support right join.
     */
    if (join_step->enforceNestLoopJoin() && join_step->supportSwap() && join_step->getKind() == ASTTableJoin::Kind::Right)
    {
        join_step = std::make_shared<JoinStep>(
            DataStreams{join_step->getInputStreams()[1], join_step->getInputStreams()[0]},
            join_step->getOutputStream(),
            ASTTableJoin::Kind::Left,
            join_step->getStrictness(),
            join_step->getRightKeys(),
            join_step->getLeftKeys(),
            join_step->getFilter(),
            join_step->isHasUsing(),
            join_step->getRequireRightKeys(),
            join_step->getAsofInequality(),
            join_step->getDistributionType(),
            join_step->isMagic());
        join_node = std::make_shared<JoinNode>(
            context->nextNodeId(), join_step, PlanNodes{right_source_expression_node, left_source_expression_node});
    }

    if (!PredicateUtils::isTruePredicate(post_join_predicate))
    {
        auto filter_step = std::make_shared<FilterStep>(join_node->getStep()->getOutputStream(), post_join_predicate);
        auto filter_node = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{join_node});
        output_node = filter_node;
    }
    else
    {
        output_node = join_node;
    }

    NamesAndTypes join_node_output = node.getStep()->getOutputStream().header.getNamesAndTypes();
    NamesAndTypes output_node_output = output_node->getStep()->getOutputStream().header.getNamesAndTypes();
    if (join_node_output != output_node_output)
    {
        Assignments output_assignments;
        NameToType output_types;
        for (auto & column : output_node_output)
        {
            Assignment output_assignment{column.name, std::make_shared<ASTIdentifier>(column.name)};
            output_assignments.emplace_back(output_assignment);
            output_types[column.name] = column.type;
        }
        auto output_expression_step
            = std::make_shared<ProjectionStep>(output_node->getStep()->getOutputStream(), std::move(output_assignments), std::move(output_types));
        auto output_expression_node
            = std::make_shared<ProjectionNode>(context->nextNodeId(), std::move(output_expression_step), PlanNodes{output_node});
        output_node = output_expression_node;
    }
    return output_node;
}

PredicateVisitor::DynamicFilterResult PredicateVisitor::createDynamicFilters(const JoinStep & join) const
{
    if (!dynamic_filtering || (join.getKind() != ASTTableJoin::Kind::Inner && join.getKind() != ASTTableJoin::Kind::Right))
        return {};

    std::unordered_map<std::string, DataTypePtr> right_name_to_types;
    for (const auto & item : join.getInputStreams()[1].header)
        right_name_to_types.emplace(item.name, item.type);

    std::vector<ConstASTPtr> executors;
    std::unordered_map<String, DynamicFilterBuildInfo> dynamic_filters;

    for (auto left = join.getLeftKeys().begin(), right = join.getRightKeys().begin(); left != join.getLeftKeys().end(); left++, right++)
    {
        // generate unique id
        auto id = context->nextNodeId();
        DynamicFilterTypes types;
        if (context->getSettingsRef().enable_dynamic_filter_for_bloom_filter)
        {
            executors.emplace_back(DynamicFilters::createDynamicFilterExpression(id, *left, DynamicFilterType::BloomFilter));
            types.emplace(DynamicFilterType::BloomFilter);
        }
        if (right_name_to_types.at(*right)->isComparable())
        {
            executors.emplace_back(DynamicFilters::createDynamicFilterExpression(id, *left, DynamicFilterType::Range));
            types.emplace(DynamicFilterType::Range);
        }
        if (!types.empty())
            dynamic_filters.emplace(*right, DynamicFilterBuildInfo{id, *left, types});
    }
    // todo@kaixi: generate for any join filters
    return DynamicFilterResult{dynamic_filters, executors};
}

PlanNodePtr PredicateVisitor::visitExchangeNode(ExchangeNode & node, PredicateContext & predicate_context)
{
    return processChild(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitWindowNode(WindowNode & node, PredicateContext & predicate_context)
{
    //    auto & step_ptr = node.getStep();
    //    auto & step = dynamic_cast<WindowStep &>(*step_ptr);
    //
    //    const WindowDescription & window_desc = step.getWindow();
    //    WindowPartitionScheme scheme = window_desc.scheme;
    //    Strings partition_symbols = scheme.partition_keys;
    //
    //    // TODO: This could be broader. We can push down conjucts if they are constant for all rows in a window partition.
    //    // The simplest way to guarantee this is if the conjucts are deterministic functions of the partitioning symbols.
    //    // This can leave out cases where they're both functions of some set of common expressions and the partitioning
    //    // function is injective, but that's a rare case. The majority of window nodes are expected to be partitioned by
    //    // pre-projected symbols.
    //    ASTPtr predicate = predicate_context.predicate;
    //    ContextMutablePtr context = predicate_context.context;
    //    std::vector<ASTPtr> conjuncts = PredicateUtils::extractConjuncts(predicate);
    //
    //    std::vector<ASTPtr> push_down_conjuncts;
    //    std::vector<ASTPtr> non_push_down_conjuncts;
    //    for (auto & conjunct : conjuncts)
    //    {
    //        std::set<String> unique_symbols = SymbolsExtractor::extract(conjunct);
    //        if (DeterminismEvaluator::isDeterministic(conjunct, context) && PredicateUtils::containsAll(unique_symbols, partition_symbols))
    //        {
    //            push_down_conjuncts.emplace_back(conjunct);
    //        }
    //        else
    //        {
    //            non_push_down_conjuncts.emplace_back(conjunct);
    //        }
    //    }
    //
    //    PredicateContext window_context{
    //        .predicate = PredicateUtils::combineConjuncts(push_down_conjuncts), .context = context, .types = predicate_context.types};
    //    PlanNodePtr rewritten = processChild(node, window_context);
    //
    //    ASTPtr non_push_down_predicate = PredicateUtils::combineConjuncts(non_push_down_conjuncts);
    //    if (!PredicateUtils::isTruePredicate(non_push_down_predicate))
    //    {
    //        // Drop in a FilterNode b/c we cannot push our predicate down any further
    //        ASTPtr extra_predicate = PredicateUtils::combineConjuncts(non_push_down_conjuncts);
    //        auto filter_step = std::make_shared<FilterStep>(rewritten->getStep()->getOutputStream(), extra_predicate);
    //        auto filter_node = std::make_shared<FilterNode>(context->nextNodeId(), std::move(filter_step), PlanNodes{rewritten});
    //        return filter_node;
    //    }
    //    return rewritten;
    return visitPlanNode(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitMergeSortingNode(MergeSortingNode & node, PredicateContext & predicate_context)
{
    return processChild(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitPartialSortingNode(PartialSortingNode & node, PredicateContext & predicate_context)
{
    return processChild(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitUnionNode(UnionNode & node, PredicateContext & predicate_context)
{
    const auto & step = *node.getStep();
    PlanNodes children;
    DataStreams inputs;
    ConstASTPtr predicate = predicate_context.predicate;
    for (size_t i = 0; i < node.getChildren().size(); i++)
    {
        Assignments assignments;
        for (const auto & output_to_input : step.getOutToInputs())
        {
            assignments.emplace_back(output_to_input.first, std::make_shared<ASTIdentifier>(output_to_input.second[i]));
        }
        ASTPtr source_predicate = ExpressionInliner::inlineSymbols(predicate, assignments);
        ASTPtr source_extra_predicate
            = ExpressionInliner::inlineSymbols(predicate_context.extra_predicate_for_simplify_outer_join, assignments);
        PredicateContext source_context{
            .predicate = source_predicate,
            .extra_predicate_for_simplify_outer_join = source_extra_predicate};
        PlanNodePtr child = process(*node.getChildren()[i], source_context);
        children.emplace_back(child);
        inputs.push_back(child->getStep()->getOutputStream());
    }
    auto new_step = std::dynamic_pointer_cast<UnionStep>(node.getStep()->copy(context));
    new_step->setInputStreams(inputs);
    node.setStep(new_step);
    node.replaceChildren(children);
    return node.shared_from_this();
}

PlanNodePtr PredicateVisitor::visitDistinctNode(DistinctNode & node, PredicateContext & predicate_context)
{
    return processChild(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode & node, PredicateContext & predicate_context)
{
    std::set<String> predicate_symbols = SymbolsExtractor::extract(predicate_context.predicate);
    const auto & step = *node.getStep();
    if (predicate_symbols.contains(step.getName()))
    {
        throw Exception("UniqueId in predicate is not yet supported", ErrorCodes::NOT_IMPLEMENTED);
    }
    return processChild(node, predicate_context);
}

PlanNodePtr PredicateVisitor::visitCTERefNode(CTERefNode & node, PredicateContext & predicate_context)
{
    const auto * cte_step = dynamic_cast<const CTERefStep *>(node.getStep().get());
    auto mapper = SymbolMapper::symbolMapper(cte_step->getOutputColumns());

    // remove dynamic filters, which don't support appears in OR.
    // todo: support dynamic filter.
    auto filters = DynamicFilters::extractDynamicFilters(predicate_context.predicate);
    ConstASTPtr mapped_filter = mapper.map(PredicateUtils::combineConjuncts(filters.second));

    auto & common_filters = cte_common_filters[cte_step->getId()];
    common_filters.emplace_back(mapped_filter);

    // only push filter through cte when filters exist above all cte, and only push filter in the first time.
    if (common_filters.size() == cte_reference_counts.at(cte_step->getId()))
    {
        auto & cte_def = cte_info.getCTEDef(cte_step->getId());
        if (PredicateUtils::isTruePredicate(cte_step->getFilter()))
        {
            auto optimized_expression = ExpressionInterpreter::optimizePredicate(
                PredicateUtils::combineDisjuncts(common_filters), cte_def->getStep()->getOutputStream().header.getNamesToTypes(), context);
            PredicateContext cte_predicate_context{
                .predicate = optimized_expression, .extra_predicate_for_simplify_outer_join = PredicateConst::TRUE_VALUE};
            cte_def = VisitorUtil::accept(cte_def, *this, cte_predicate_context);
        }
        else
        {
            PredicateContext cte_predicate_context{
                .predicate = PredicateConst::TRUE_VALUE, .extra_predicate_for_simplify_outer_join = PredicateConst::TRUE_VALUE};
            cte_def = VisitorUtil::accept(cte_def, *this, cte_predicate_context);
        }
    }

    if (!PredicateUtils::isTruePredicate(mapped_filter))
    {
        auto new_step = std::make_shared<CTERefStep>(
            cte_step->getOutputStream(),
            cte_step->getId(),
            cte_step->getOutputColumns(),
            PredicateUtils::combineConjuncts(std::vector<ConstASTPtr>{{cte_step->getFilter(), mapped_filter}}));
        node.setStep(new_step);
    }
    return visitPlanNode(node, predicate_context);
}

PlanNodePtr PredicateVisitor::process(PlanNodeBase & node, PredicateContext & predicate_context)
{
    return VisitorUtil::accept(node, *this, predicate_context);
}

PlanNodePtr PredicateVisitor::processChild(PlanNodeBase & node, PredicateContext & predicate_context)
{
    if (node.getChildren().empty())
        return node.shared_from_this();

    PlanNodes children;
    DataStreams inputs;
    for (const auto & item : node.getChildren())
    {
        PlanNodePtr child = process(*item, predicate_context);
        children.emplace_back(child);
        inputs.push_back(child->getStep()->getOutputStream());
    }

    auto new_step = node.getStep()->copy(context);
    new_step->setInputStreams(inputs);
    node.setStep(new_step);
    node.replaceChildren(children);
    return node.shared_from_this();
}

InnerJoinResult PredicateVisitor::processInnerJoin(
    ConstASTPtr & inherited_predicate,
    ConstASTPtr & left_predicate,
    ConstASTPtr & right_predicate,
    ConstASTPtr & join_predicate,
    std::set<String> & left_symbols,
    std::set<String> & right_symbols,
    ContextMutablePtr & context)
{
    std::vector<ConstASTPtr> left_conjuncts;
    std::vector<ConstASTPtr> right_conjuncts;
    std::vector<ConstASTPtr> join_conjuncts;

    for (auto & predicate : PredicateUtils::extractConjuncts(inherited_predicate))
    {
        if (!ExpressionDeterminism::isDeterministic(predicate, context))
        {
            join_conjuncts.emplace_back(predicate);
        }
    }
    // Strip out non-deterministic conjuncts
    inherited_predicate = ExpressionDeterminism::filterDeterministicConjuncts(inherited_predicate, context);

    for (auto & predicate : PredicateUtils::extractConjuncts(join_predicate))
    {
        if (!ExpressionDeterminism::isDeterministic(predicate, context))
        {
            join_conjuncts.emplace_back(predicate);
        }
    }
    join_predicate = ExpressionDeterminism::filterDeterministicConjuncts(join_predicate, context);
    left_predicate = ExpressionDeterminism::filterDeterministicConjuncts(left_predicate, context);
    right_predicate = ExpressionDeterminism::filterDeterministicConjuncts(right_predicate, context);

    // Attempt to simplify the effective left/right predicates with the predicate we're pushing down
    // This, effectively, inlines any constants derived from such predicate
    std::set<String> left_scope;
    for (const auto & symbol : left_symbols)
    {
        left_scope.emplace(symbol);
    }
    std::set<String> right_scope;
    for (const auto & symbol : right_symbols)
    {
        right_scope.emplace(symbol);
    }
    EqualityInference predicate_inference = EqualityInference::newInstance(inherited_predicate, context);
    ASTPtr simplified_left_predicate = predicate_inference.rewrite(left_predicate, left_scope);
    ASTPtr simplified_right_predicate = predicate_inference.rewrite(right_predicate, right_scope);

    // simplify predicate based on known equalities guaranteed by the left/right side
    EqualityInference assertions = EqualityInference::newInstance(std::vector<ConstASTPtr>{left_predicate, right_predicate}, context);

    std::set<String> union_scope;
    std::set_union(
        left_scope.begin(), left_scope.end(), right_scope.begin(), right_scope.end(), std::inserter(union_scope, union_scope.end()));
    inherited_predicate = assertions.rewrite(inherited_predicate, union_scope);

    if (!inherited_predicate)
        throw Exception("Unexpected error in predicate pushdown", ErrorCodes::PLAN_BUILD_ERROR);

    // Generate equality inferences
    EqualityInference all_inference = EqualityInference::newInstance(
        std::vector<ConstASTPtr>{
            inherited_predicate, left_predicate, right_predicate, join_predicate, simplified_left_predicate, simplified_right_predicate},
        context);
    EqualityInference all_inference_without_left_inferred = EqualityInference::newInstance(
        std::vector<ConstASTPtr>{inherited_predicate, right_predicate, join_predicate, simplified_right_predicate}, context);
    EqualityInference all_inference_without_right_inferred = EqualityInference::newInstance(
        std::vector<ConstASTPtr>{inherited_predicate, left_predicate, join_predicate, simplified_left_predicate}, context);

    // Add equalities from the inference back in
    auto equalities = all_inference.partitionedBy(left_scope).getScopeStraddlingEqualities();
    auto equalities_without_left = all_inference_without_left_inferred.partitionedBy(left_scope).getScopeEqualities();
    auto equalities_without_right = all_inference_without_right_inferred.partitionedBy(right_scope).getScopeEqualities();

    for (auto & equality_without_left : equalities_without_left)
    {
        left_conjuncts.emplace_back(equality_without_left);
    }
    for (auto & equality_without_right : equalities_without_right)
    {
        right_conjuncts.emplace_back(equality_without_right);
    }
    for (auto & equality : equalities)
    {
        join_conjuncts.emplace_back(equality);
    }

    // Sort through conjuncts in inheritedPredicate that were not used for inference
    auto non_inferrable_conjuncts = EqualityInference::nonInferrableConjuncts(inherited_predicate, context);
    for (auto & non_inferrable_conjunct : non_inferrable_conjuncts)
    {
        ASTPtr left_rewritten_conjunct = all_inference.rewrite(non_inferrable_conjunct, left_scope);
        if (left_rewritten_conjunct != nullptr)
        {
            // as left_rewritten_conjunct will be modified, so clone here.
            left_conjuncts.emplace_back(left_rewritten_conjunct->clone());
        }

        ASTPtr right_rewritten_conjunct = all_inference.rewrite(non_inferrable_conjunct, right_scope);
        if (right_rewritten_conjunct != nullptr)
        {
            right_conjuncts.emplace_back(right_rewritten_conjunct->clone());
        }

        // Drop predicate after join only if unable to push down to either side
        if (left_rewritten_conjunct == nullptr && right_rewritten_conjunct == nullptr)
        {
            join_conjuncts.emplace_back(non_inferrable_conjunct);
        }
    }

    // See if we can push the right effective predicate to the left side
    auto right_non_inferrable_conjuncts = EqualityInference::nonInferrableConjuncts(simplified_right_predicate, context);
    for (auto & conjunct : right_non_inferrable_conjuncts)
    {
        ASTPtr rewritten = all_inference.rewrite(conjunct, left_scope);
        if (rewritten != nullptr)
        {
            left_conjuncts.emplace_back(rewritten);
        }
    }

    // See if we can push the left effective predicate to the right side
    auto left_non_inferrable_conjuncts = EqualityInference::nonInferrableConjuncts(simplified_left_predicate, context);
    for (auto & conjunct : left_non_inferrable_conjuncts)
    {
        ASTPtr rewritten = all_inference.rewrite(conjunct, right_scope);
        if (rewritten != nullptr)
        {
            right_conjuncts.emplace_back(rewritten);
        }
    }

    // See if we can push any parts of the join predicates to either side
    auto join_non_inferrable_conjuncts = EqualityInference::nonInferrableConjuncts(join_predicate, context);
    for (auto & conjunct : join_non_inferrable_conjuncts)
    {
        ASTPtr left_rewritten = all_inference.rewrite(conjunct, left_scope);
        if (left_rewritten != nullptr)
        {
            left_conjuncts.emplace_back(left_rewritten->clone());
        }

        ASTPtr right_rewritten = all_inference.rewrite(conjunct, right_scope);
        if (right_rewritten != nullptr)
        {
            right_conjuncts.emplace_back(right_rewritten->clone());
        }

        if (left_rewritten == nullptr && right_rewritten == nullptr)
        {
            join_conjuncts.emplace_back(conjunct);
        }
    }

    return InnerJoinResult{
        PredicateUtils::combineConjuncts(left_conjuncts),
        PredicateUtils::combineConjuncts(right_conjuncts),
        PredicateUtils::combineConjuncts(join_conjuncts),
        PredicateConst::TRUE_VALUE};
}

OuterJoinResult PredicateVisitor::processOuterJoin(
    ConstASTPtr & inherited_predicate,
    ConstASTPtr & outer_predicate,
    ConstASTPtr & inner_predicate,
    ConstASTPtr & join_predicate,
    std::set<String> & outer_symbols,
    std::set<String> & inner_symbols,
    ContextMutablePtr & context)
{
    std::vector<ConstASTPtr> outer_pushdown_conjuncts;
    std::vector<ConstASTPtr> inner_pushdown_conjuncts;
    std::vector<ConstASTPtr> post_join_conjuncts;
    std::vector<ConstASTPtr> join_conjuncts;

    // Strip out non-deterministic conjuncts
    auto predicates = PredicateUtils::extractConjuncts(inherited_predicate);
    for (auto & pre : predicates)
    {
        if (!ExpressionDeterminism::isDeterministic(pre, context))
        {
            post_join_conjuncts.emplace_back(pre);
        }
    }

    inherited_predicate = ExpressionDeterminism::filterDeterministicConjuncts(inherited_predicate, context);
    outer_predicate = ExpressionDeterminism::filterDeterministicConjuncts(outer_predicate, context);
    inner_predicate = ExpressionDeterminism::filterDeterministicConjuncts(inner_predicate, context);

    std::vector<ConstASTPtr> join_predicates = PredicateUtils::extractConjuncts(join_predicate);
    for (auto & pre : join_predicates)
    {
        if (!ExpressionDeterminism::isDeterministic(pre, context))
        {
            join_conjuncts.emplace_back(pre);
        }
    }

    join_predicate = ExpressionDeterminism::filterDeterministicConjuncts(join_predicate, context);

    // Generate equality inferences
    EqualityInference inherited_inference = EqualityInference::newInstance(inherited_predicate, context);
    EqualityInference outer_inference
        = EqualityInference::newInstance(std::vector<ConstASTPtr>{inherited_predicate, outer_predicate}, context);

    EqualityPartition equality_partition = inherited_inference.partitionedBy(outer_symbols);
    const auto & scope_equalities = equality_partition.getScopeEqualities();
    auto outer_only_inherited_equalities = PredicateUtils::combineConjuncts(scope_equalities);
    EqualityInference potential_null_symbol_inference = EqualityInference::newInstance(
        std::vector<ConstASTPtr>{outer_only_inherited_equalities, outer_predicate, inner_predicate, join_predicate}, context);

    // Push outer and join equalities into the inner side. For example:
    // SELECT * FROM nation LEFT OUTER JOIN region ON nation.regionkey = region.regionkey and nation.name = region.name WHERE nation.name = 'blah'

    EqualityInference potential_null_symbol_inference_without_inner_inferred = EqualityInference::newInstance(
        std::vector<ConstASTPtr>{outer_only_inherited_equalities, outer_predicate, join_predicate}, context);

    EqualityPartition potential_null_symbol_inference_without_inner_inferred_partition =
        potential_null_symbol_inference_without_inner_inferred.partitionedBy(inner_symbols);
    for (const auto & conjunct : potential_null_symbol_inference_without_inner_inferred_partition.getScopeEqualities())
    {
        inner_pushdown_conjuncts.emplace_back(conjunct);
    }

    // TODO: we can further improve simplifying the equalities by considering other relationships from the outer side
    EqualityPartition join_equality_partition = EqualityInference::newInstance(join_predicate, context).partitionedBy(inner_symbols);

    for (const auto & conjunct : join_equality_partition.getScopeEqualities())
    {
        inner_pushdown_conjuncts.emplace_back(conjunct);
    }
    for (const auto & conjunct : join_equality_partition.getScopeComplementEqualities())
    {
        join_conjuncts.emplace_back(conjunct);
    }
    for (const auto & conjunct : join_equality_partition.getScopeStraddlingEqualities())
    {
        join_conjuncts.emplace_back(conjunct);
    }

    // Add the equalities from the inferences back in
    for (const auto & conjunct : equality_partition.getScopeEqualities())
    {
        outer_pushdown_conjuncts.emplace_back(conjunct);
    }
    for (const auto & conjunct : equality_partition.getScopeComplementEqualities())
    {
        post_join_conjuncts.emplace_back(conjunct);
    }
    for (const auto & conjunct : equality_partition.getScopeStraddlingEqualities())
    {
        post_join_conjuncts.emplace_back(conjunct);
    }

    // See if we can push inherited predicates down
    for (const auto & conjunct : EqualityInference::nonInferrableConjuncts(inherited_predicate, context))
    {
        ASTPtr outer_rewritten = outer_inference.rewrite(conjunct, outer_symbols);
        if (outer_rewritten != nullptr)
        {
            outer_pushdown_conjuncts.emplace_back(outer_rewritten->clone());

            // A conjunct can only be pushed down into an inner side if it can be rewritten in terms of the outer side
            ASTPtr inner_rewritten = potential_null_symbol_inference.rewrite(outer_rewritten, inner_symbols);
            if (inner_rewritten != nullptr)
            {
                inner_pushdown_conjuncts.emplace_back(inner_rewritten);
            }
        }
        else
        {
            post_join_conjuncts.emplace_back(conjunct);
        }
    }

    // See if we can push down any outer effective predicates to the inner side
    for (const auto & conjunct : EqualityInference::nonInferrableConjuncts(outer_predicate, context))
    {
        auto rewritten = potential_null_symbol_inference.rewrite(conjunct, inner_symbols);
        if (rewritten != nullptr)
        {
            inner_pushdown_conjuncts.emplace_back(rewritten);
        }
    }

    // See if we can push down join predicates to the inner side
    for (const auto & conjunct : EqualityInference::nonInferrableConjuncts(join_predicate, context))
    {
        ASTPtr inner_rewritten = potential_null_symbol_inference.rewrite(conjunct, inner_symbols);
        if (inner_rewritten != nullptr)
        {
            inner_pushdown_conjuncts.emplace_back(inner_rewritten);
        }
        else
        {
            join_conjuncts.emplace_back(conjunct);
        }
    }

    return OuterJoinResult{
        PredicateUtils::combineConjuncts(outer_pushdown_conjuncts),
        PredicateUtils::combineConjuncts(inner_pushdown_conjuncts),
        PredicateUtils::combineConjuncts(join_conjuncts),
        PredicateUtils::combineConjuncts(post_join_conjuncts)};
}

void PredicateVisitor::tryNormalizeOuterToInnerJoin(JoinNode & node, const ConstASTPtr & inherited_predicate, ContextMutablePtr context)
{
    using Kind = ASTTableJoin::Kind;
    using Strictness = ASTTableJoin::Strictness;

    const auto & step = *node.getStep();
    Kind kind = step.getKind();
    Strictness strictness = step.getStrictness();

    if (kind != Kind::Left && kind != Kind::Right && kind != Kind::Full)
        return;

    // TODO: ANTI JOINs also can be optimized
    if (strictness != Strictness::All && strictness != Strictness::Any)
        return;

    auto column_types = step.getOutputStream().header.getNamesToTypes();

    auto build_symbols = [&](const PlanNodePtr & source) {
        std::unordered_map<String, Field> result;

        // note we cannot use type information of `source` node as outer join will change the column types of non-outer side
        for (const auto & name: source->getStep()->getOutputStream().header.getNames())
            if (column_types.count(name))
                result.emplace(name, column_types.at(name)->getDefault());

        return result;
    };

    std::unordered_map<String, Field> left_symbols = build_symbols(node.getChildren()[0]);
    std::unordered_map<String, Field> right_symbols = build_symbols(node.getChildren()[1]);

    if (isRightOrFull(kind) && canConvertOuterToInner(left_symbols, inherited_predicate, context, column_types))
    {
        kind = useInnerForRightSide(kind);
    }

    if (isLeftOrFull(kind) && canConvertOuterToInner(right_symbols, inherited_predicate, context, column_types))
    {
        kind = useInnerForLeftSide(kind);
    }

    auto new_step = std::dynamic_pointer_cast<JoinStep>(step.copy(context));
    new_step->setKind(kind);
    node.setStep(new_step);
}

bool PredicateVisitor::canConvertOuterToInner(
    const std::unordered_map<String, Field> & inner_symbols_for_outer_join,
    const ConstASTPtr & inherited_predicate,
    ContextMutablePtr context,
    const NameToType & column_types)
{
    auto interpreter = ExpressionInterpreter::optimizedInterpreter(column_types, inner_symbols_for_outer_join, context);
    for (auto & conjunct : PredicateUtils::extractConjuncts(inherited_predicate))
    {
        auto result = interpreter.evaluateConstantExpression(conjunct);
        if (result.has_value())
        {
            auto & val = result->second;
            if (val.isNull() || !applyVisitor(FieldVisitorConvertToNumber<bool>(), val))
                return true;
        }
    }
    return false;
}

ASTTableJoin::Kind PredicateVisitor::useInnerForLeftSide(ASTTableJoin::Kind kind)
{
    Utils::checkArgument(kind == ASTTableJoin::Kind::Full || kind == ASTTableJoin::Kind::Left);
    return kind == ASTTableJoin::Kind::Full ? ASTTableJoin::Kind::Right : ASTTableJoin::Kind::Inner;
}

ASTTableJoin::Kind PredicateVisitor::useInnerForRightSide(ASTTableJoin::Kind kind)
{
    Utils::checkArgument(kind == ASTTableJoin::Kind::Full || kind == ASTTableJoin::Kind::Right);
    return kind == ASTTableJoin::Kind::Full ? ASTTableJoin::Kind::Left : ASTTableJoin::Kind::Inner;
}

bool PredicateVisitor::isRegularJoin(const JoinStep & step)
{
    auto strictness = step.getStrictness();

    return isAll(strictness) || isAny(strictness);
}

ASTPtr EffectivePredicateExtractor::extract(PlanNodePtr & node, CTEInfo & cte_info, ContextMutablePtr & context)
{
    EffectivePredicateVisitor visitor{cte_info};
    return VisitorUtil::accept(node, visitor, context);
}

ASTPtr EffectivePredicateExtractor::extract(PlanNodeBase & node, CTEInfo & cte_info, ContextMutablePtr & context)
{
    EffectivePredicateVisitor visitor{cte_info};
    return VisitorUtil::accept(node, visitor, context);
}

ASTPtr EffectivePredicateVisitor::visitPlanNode(PlanNodeBase &, ContextMutablePtr &)
{
    return PredicateConst::TRUE_VALUE;
}

ASTPtr EffectivePredicateVisitor::visitLimitNode(LimitNode & node, ContextMutablePtr & context)
{
    return process(node, context);
}

ASTPtr EffectivePredicateVisitor::visitEnforceSingleRowNode(EnforceSingleRowNode & node, ContextMutablePtr & context)
{
    // EnforceSingleRowNode will produce at lease one row, can't propagate predicate.
    return visitPlanNode(node, context);
}

ASTPtr EffectivePredicateVisitor::visitProjectionNode(ProjectionNode & node, ContextMutablePtr & context)
{
    // TODO: add simple algebraic solver for projection translation (right now only considers identity projections)

    // Clear predicates involving symbols which are keys to non-identity assignments.
    // Assignment such as `s -> x + 1` establishes new semantics for symbol `s`.
    // If symbol `s` was present is the source plan and was included in underlying predicate, the predicate is no more valid.
    // Also, if symbol `s` is present in a project assignment's value, e.g. `s1 -> s + 1`, this assignment should't be used to derive equality.

    ASTPtr underlying_predicate = process(node, context);

    auto & step_ptr = *node.getStep();

    Assignments non_identity_assignments;
    std::set<String> newly_assigned_symbols;
    for (const auto & assignment : step_ptr.getAssignments())
    {
        String symbol = assignment.first;
        auto value = assignment.second;
        if (value->as<ASTIdentifier>() && value->getColumnName() == symbol)
            continue;

        // skip NULL
        if (value->as<ASTLiteral>() && value->as<ASTLiteral &>().value.isNull())
            continue;

        // skip id_1 := cast(id, SomeType)
        if (const auto * func = value->as<ASTFunction>())
            if (Poco::toLower(func->name) == "cast" && func->arguments->children[0]->as<ASTIdentifier>())
                continue;

        non_identity_assignments.emplace_back(assignment);
        newly_assigned_symbols.emplace(symbol);
    }

    std::vector<ConstASTPtr> valid_underlying_equalities;
    for (auto & conjunct : PredicateUtils::extractConjuncts(underlying_predicate))
    {
        std::set<String> uniques = SymbolsExtractor::extract(conjunct);
        std::set<String> symbols_to_propagate;
        std::set_intersection(
            uniques.begin(),
            uniques.end(),
            newly_assigned_symbols.begin(),
            newly_assigned_symbols.end(),
            std::inserter(symbols_to_propagate, symbols_to_propagate.begin()));
        if (symbols_to_propagate.empty())
        {
            valid_underlying_equalities.emplace_back(conjunct);
        }
    }
    std::vector<ConstASTPtr> projection_equalities;
    for (auto & assignment : non_identity_assignments)
    {
        std::set<String> uniques = SymbolsExtractor::extract(assignment.second);
        std::set<String> symbols_to_propagate;
        std::set_intersection(
            uniques.begin(),
            uniques.end(),
            newly_assigned_symbols.begin(),
            newly_assigned_symbols.end(),
            std::inserter(symbols_to_propagate, symbols_to_propagate.begin()));
        if (symbols_to_propagate.empty())
        {
            auto fun = makeASTFunction("equals", std::make_shared<ASTIdentifier>(assignment.first), assignment.second->clone());
            projection_equalities.emplace_back(fun);
        }
    }

    projection_equalities.insert(projection_equalities.end(), valid_underlying_equalities.begin(), valid_underlying_equalities.end());

    std::vector<String> output_symbols;
    for (const auto & output : step_ptr.getOutputStream().header)
    {
        output_symbols.emplace_back(output.name);
    }

    auto expression = PredicateUtils::combineConjuncts(projection_equalities);
    return pullExpressionThroughSymbols(expression, output_symbols, context);
}

ASTPtr EffectivePredicateVisitor::visitFilterNode(FilterNode & node, ContextMutablePtr & context)
{
    ASTPtr underlying_predicate = process(node, context);

    const auto & step= *node.getStep();
    auto predicate = step.getFilter();

    // Remove non-deterministic conjuncts
    predicate = ExpressionDeterminism::filterDeterministicConjuncts(predicate, context);
    std::vector<ConstASTPtr> predicates{predicate, underlying_predicate};
    return PredicateUtils::combineConjuncts(predicates);
}

ASTPtr EffectivePredicateVisitor::visitAggregatingNode(AggregatingNode & node, ContextMutablePtr & context)
{
    // GROUP BY () always produces a group, regardless of whether there's any
    // input (unlike the case where there are group by keys, which produce
    // no output if there's no input).
    // Therefore, we can't say anything about the effective predicate of the
    // output of such an aggregation.
    const auto & step_ptr = *node.getStep();

    if (step_ptr.getKeys().empty())
    {
        return PredicateConst::TRUE_VALUE;
    }

    ASTPtr underlying_predicate = process(node, context);

    return pullExpressionThroughSymbols(underlying_predicate, step_ptr.getKeys(), context);
}

ASTPtr EffectivePredicateVisitor::visitJoinNode(JoinNode &, ContextMutablePtr &)
{
    // TODO implement
    return PredicateConst::TRUE_VALUE;
}

ASTPtr EffectivePredicateVisitor::visitExchangeNode(ExchangeNode &, ContextMutablePtr &)
{
    // TODO implement
    return PredicateConst::TRUE_VALUE;
}

ASTPtr EffectivePredicateVisitor::visitWindowNode(WindowNode & node, ContextMutablePtr & context)
{
    return process(node, context);
}

ASTPtr EffectivePredicateVisitor::visitMergeSortingNode(MergeSortingNode & node, ContextMutablePtr & context)
{
    return process(node, context);
}

ASTPtr EffectivePredicateVisitor::visitUnionNode(UnionNode &, ContextMutablePtr &)
{
    // TODO implement
    return PredicateConst::TRUE_VALUE;
}

ASTPtr EffectivePredicateVisitor::visitTableScanNode(TableScanNode &, ContextMutablePtr &)
{
    return PredicateConst::TRUE_VALUE;
}

ASTPtr EffectivePredicateVisitor::visitDistinctNode(DistinctNode & node, ContextMutablePtr & context)
{
    return process(node, context);
}

ASTPtr EffectivePredicateVisitor::visitAssignUniqueIdNode(AssignUniqueIdNode & node, ContextMutablePtr & context)
{
    return process(node, context);
}

ASTPtr EffectivePredicateVisitor::process(PlanNodeBase & node, ContextMutablePtr & context)
{
    return VisitorUtil::accept(node.getChildren()[0], *this, context);
}

ASTPtr EffectivePredicateVisitor::pullExpressionThroughSymbols(ASTPtr & expression, std::vector<String> symbols, ContextMutablePtr & context)
{
    EqualityInference equality_inference = EqualityInference::newInstance(expression, context);

    std::vector<ConstASTPtr> effective_conjuncts;
    std::set<String> scope(symbols.begin(), symbols.end());

    for (auto & conjunct : EqualityInference::nonInferrableConjuncts(expression, context))
    {
        if (ExpressionDeterminism::isDeterministic(conjunct, context))
        {
            ASTPtr rewritten = equality_inference.rewrite(conjunct, scope);
            if (rewritten != nullptr)
            {
                effective_conjuncts.emplace_back(rewritten);
            }
        }
    }
    std::vector<ConstASTPtr> equalities = equality_inference.partitionedBy(scope).getScopeEqualities();
    effective_conjuncts.insert(effective_conjuncts.end(), equalities.begin(), equalities.end());

    return PredicateUtils::combineConjuncts(effective_conjuncts);
}

ASTPtr EffectivePredicateVisitor::visitCTERefNode(CTERefNode & node, ContextMutablePtr & context)
{
    const auto * step = dynamic_cast<const CTERefStep *>(node.getStep().get());
    return VisitorUtil::accept(step->toInlinedPlanNode(cte_info, context), *this, context);
}

}
