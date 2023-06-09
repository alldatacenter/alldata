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

#include <Optimizer/Correlation.h>
#include <Optimizer/PlanNodeCardinality.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/SymbolsExtractor.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <QueryPlan/Assignment.h>
#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
std::vector<String> Correlation::prune(PlanNodePtr & node, const Names & origin_correlation)
{
    std::set<String> subquery_symbols = SymbolsExtractor::extract(node);

    Names new_correlation;
    for (const auto & name : origin_correlation)
    {
        if (subquery_symbols.contains(name))
            new_correlation.emplace_back(name);
    }
    return new_correlation;
}

bool Correlation::containsCorrelation(PlanNodePtr & node, Names & correlation)
{
    std::set<String> symbols = SymbolsExtractor::extract(node);
    for (const auto & symbol : symbols)
    {
        if (std::find(correlation.begin(), correlation.end(), symbol) != correlation.end())
            return true;
    }
    return false;
}

bool Correlation::isCorrelated(ConstASTPtr & expression, Names & correlation)
{
    std::set<String> symbols = SymbolsExtractor::extract(expression);
    for (const auto & symbol : symbols)
    {
        if (std::find(correlation.begin(), correlation.end(), symbol) != correlation.end())
        {
            return true;
        }
    }
    return false;
}

bool Correlation::isUnreferencedScalar(PlanNodePtr & node)
{
    bool un_reference = !node->getStep()->getOutputStream().header;
    bool is_scalar = PlanNodeCardinality::isScalar(*node);
    return un_reference && is_scalar;
}

std::pair<Names, Names> DecorrelationResult::extractCorrelations(Names & correlation)
{
    Names left;
    Names right;

    // As correlation symbol belongs to outer query, hence correlation symbol is left key.
    for (auto & predicate : correlation_predicates)
    {
        const auto & fun = predicate->as<ASTFunction &>();
        ASTIdentifier & left_symbol = fun.arguments->getChildren()[0]->as<ASTIdentifier &>();
        if (std::find(correlation.begin(), correlation.end(), left_symbol.name()) != correlation.end())
        {
            left.emplace_back(left_symbol.name());
        }
        else
        {
            right.emplace_back(left_symbol.name());
        }
        ASTIdentifier & right_symbol = fun.arguments->getChildren()[1]->as<ASTIdentifier &>();
        if (std::find(correlation.begin(), correlation.end(), right_symbol.name()) != correlation.end())
        {
            left.emplace_back(right_symbol.name());
        }
        else
        {
            right.emplace_back(right_symbol.name());
        }
    }
    return std::make_pair(left, right);
}

std::pair<Names, Names> DecorrelationResult::extractJoinClause(Names & correlation)
{
    Names left;
    Names right;

    // As correlation symbol belongs to outer query, hence correlation symbol is left key.
    for (auto & predicate : correlation_predicates)
    {
        const auto & fun = predicate->as<ASTFunction &>();
        if (fun.name == "equals")
        {
            ASTIdentifier & left_symbol = fun.arguments->getChildren()[0]->as<ASTIdentifier &>();
            if (std::find(correlation.begin(), correlation.end(), left_symbol.name()) != correlation.end())
            {
                left.emplace_back(left_symbol.name());
            }
            else
            {
                right.emplace_back(left_symbol.name());
            }
            ASTIdentifier & right_symbol = fun.arguments->getChildren()[1]->as<ASTIdentifier &>();
            if (std::find(correlation.begin(), correlation.end(), right_symbol.name()) != correlation.end())
            {
                left.emplace_back(right_symbol.name());
            }
            else
            {
                right.emplace_back(right_symbol.name());
            }
        }
    }
    return std::make_pair(left, right);
}

std::vector<ConstASTPtr> DecorrelationResult::extractFilter()
{
    std::vector<ConstASTPtr> filter;
    for (auto & predicate : correlation_predicates)
    {
        const auto & fun = predicate->as<ASTFunction &>();
        if (fun.name != "equals")
        {
            filter.emplace_back(predicate);
        }
    }
    return filter;
}

std::optional<DecorrelationResult> Decorrelation::decorrelateFilters(PlanNodePtr & node, Names & correlation, Context & context)
{
    if (correlation.empty())
    {
        return DecorrelationResult{node};
    }

    DecorrelationVisitor visitor{correlation};
    std::optional<DecorrelationResult> result = VisitorUtil::accept(node, visitor, context);
    if (result.has_value())
    {
        DecorrelationResult & result_value = result.value();
        if (Correlation::containsCorrelation(result_value.node, correlation))
        {
            return std::nullopt;
        }
        DecorrelationResult decorrelation_node{.node = result_value.node, .correlation_predicates = result_value.correlation_predicates};
        return std::make_optional(decorrelation_node);
    }
    return std::nullopt;
}

std::optional<DecorrelationResult> DecorrelationVisitor::visitPlanNode(PlanNodeBase & node, Context &)
{
    PlanNodePtr node_ptr = node.shared_from_this();
    if (Correlation::containsCorrelation(node_ptr, correlation))
    {
        return std::nullopt;
    }
    DecorrelationResult result{.node = node.shared_from_this()};
    return std::make_optional(result);
}

std::optional<DecorrelationResult> DecorrelationVisitor::visitFilterNode(FilterNode & node, Context & context)
{
    PlanNodePtr & source = node.getChildren()[0];
    DecorrelationResult child{.node = source};
    std::optional<DecorrelationResult> child_result = std::make_optional(child);
    // try to decorrelate filters down the tree
    if (Correlation::containsCorrelation(source, correlation))
    {
        child_result = VisitorUtil::accept(source, *this, context);
    }
    if (!child_result.has_value())
    {
        return std::nullopt;
    }
    const auto & step = *node.getStep();
    const auto & predicate = step.getFilter();

    std::vector<ConstASTPtr> predicates = PredicateUtils::extractConjuncts(predicate);
    std::vector<ConstASTPtr> correlation_predicates;
    std::vector<ConstASTPtr> un_correlation_predicates;
    for (auto & pre : predicates)
    {
        if (Correlation::isCorrelated(pre, correlation))
        {
            correlation_predicates.emplace_back(pre);
        }
        else
        {
            un_correlation_predicates.emplace_back(pre);
        }
    }

    DecorrelationResult & child_result_value = child_result.value();

    std::set<String> correlate_predicate_symbols = SymbolsExtractor::extract(correlation_predicates);
    std::set<String> set_correlation;
    for (auto & corr : correlation)
    {
        set_correlation.emplace(corr);
    }
    std::set<String> symbols_to_propagate;
    std::set_difference(
        correlate_predicate_symbols.begin(),
        correlate_predicate_symbols.end(),
        set_correlation.begin(),
        set_correlation.end(),
        std::inserter(symbols_to_propagate, symbols_to_propagate.begin()));

    symbols_to_propagate.insert(child_result_value.symbols_to_propagate.begin(), child_result_value.symbols_to_propagate.end());
    correlation_predicates.insert(
        correlation_predicates.end(), child_result_value.correlation_predicates.begin(), child_result_value.correlation_predicates.end());

    PlanNodes children{child_result_value.node};
    if (!un_correlation_predicates.empty())
    {
        const DataStream & input = child_result_value.node->getStep()->getOutputStream();
        ASTPtr un_correlation_predicate = PredicateUtils::combineConjuncts(un_correlation_predicates);
        auto filter_step = std::make_shared<FilterStep>(input, un_correlation_predicate);
        auto filter_node = std::make_shared<FilterNode>(context.nextNodeId(), std::move(filter_step), children);
        DecorrelationResult filter_result{
            .node = filter_node,
            .symbols_to_propagate = symbols_to_propagate,
            .correlation_predicates = correlation_predicates,
            .at_most_single_row = child_result_value.at_most_single_row};
        return std::make_optional(filter_result);
    }

    DecorrelationResult filter_result{
        .node = child_result_value.node,
        .symbols_to_propagate = symbols_to_propagate,
        .correlation_predicates = correlation_predicates,
        .at_most_single_row = child_result_value.at_most_single_row};
    return std::make_optional(filter_result);
}

std::optional<DecorrelationResult> DecorrelationVisitor::visitProjectionNode(ProjectionNode & node, Context & context)
{
    PlanNodePtr & source = node.getChildren()[0];
    std::optional<DecorrelationResult> child_result = VisitorUtil::accept(source, *this, context);
    if (!child_result.has_value())
    {
        return std::nullopt;
    }

    DecorrelationResult & child_result_value = child_result.value();
    std::set<String> output_symbols;
    const auto & name_and_types = node.getStep()->getOutputStream().header;
    for (const auto & column : name_and_types)
    {
        output_symbols.emplace(column.name);
    }
    std::vector<String> symbols_to_add;
    std::set<String> symbols_to_propagate = child_result_value.symbols_to_propagate;
    // if output symbols don't have symbols_to_propagate, project it.
    for (const auto & symbol : symbols_to_propagate)
    {
        if (output_symbols.find(symbol) == output_symbols.end())
        {
            symbols_to_add.emplace_back(symbol);
        }
    }

    if (!symbols_to_add.empty())
    {
        const auto & step = *node.getStep();
        Assignments add_assignments = step.getAssignments();
        for (auto & symbol : symbols_to_add)
        {
            add_assignments.emplace_back(symbol, std::make_shared<ASTIdentifier>(symbol));
        }

        NamesAndTypes input_stream_columns;
        NameToType name_to_type = node.getOutputNamesToTypes();

        for (auto & add_ass : add_assignments)
        {
            const auto & input_name_and_types = node.getStep()->getInputStreams()[0].header;
            for (const auto & name_and_type : input_name_and_types)
            {
                if (add_ass.first == name_and_type.name)
                {
                    input_stream_columns.emplace_back(add_ass.first, name_and_type.type);
                    name_to_type[add_ass.first] = name_and_type.type;
                }
            }
        }

        DataStream input{.header = input_stream_columns};
        auto expression_step = std::make_shared<ProjectionStep>(input, add_assignments, name_to_type);
        PlanNodes children{child_result_value.node};
        auto expression_node = std::make_shared<ProjectionNode>(context.nextNodeId(), std::move(expression_step), children);

        DecorrelationResult result{
            .node = expression_node,
            .symbols_to_propagate = child_result_value.symbols_to_propagate,
            .correlation_predicates = child_result_value.correlation_predicates,
            .at_most_single_row = child_result_value.at_most_single_row};
        return std::make_optional(result);
    }
    else
    {
        DecorrelationResult result{
            .node = child_result_value.node,
            .symbols_to_propagate = child_result_value.symbols_to_propagate,
            .correlation_predicates = child_result_value.correlation_predicates,
            .at_most_single_row = child_result_value.at_most_single_row};
        return std::make_optional(result);
    }
}

}
