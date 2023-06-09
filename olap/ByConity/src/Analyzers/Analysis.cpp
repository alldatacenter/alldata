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

#include <Analyzers/Analysis.h>

namespace DB
{

#define MAP_GET(container, key)                                                                            \
            do {                                                                                           \
                if(auto iter = (container).find(key); iter != (container).end())                           \
                    return iter->second;                                                                   \
                else                                                                                       \
                    throw Exception("Object not found in " #container, ErrorCodes::LOGICAL_ERROR);         \
            } while(false)

#define MAP_SET(container, key, val)                                                                       \
            do {                                                                                           \
                if(!(container).emplace((key), (val)).second)                                              \
                    throw Exception("Object already exists in " #container, ErrorCodes::LOGICAL_ERROR);    \
            } while(false)                                                                                 \

void Analysis::setScope(IAST & statement, ScopePtr scope)
{
    MAP_SET(scopes, &statement, scope);
}

ScopePtr Analysis::getScope(IAST & statement)
{
    MAP_GET(scopes, &statement);
}

void Analysis::setQueryWithoutFromScope(ASTSelectQuery & query, ScopePtr scope)
{
    MAP_SET(query_without_from_scopes, &query, scope);
}

ScopePtr Analysis::getQueryWithoutFromScope(ASTSelectQuery & query)
{
    MAP_GET(query_without_from_scopes, &query);
}

void Analysis::setTableStorageScope(ASTIdentifier & db_and_table, ScopePtr scope)
{
    MAP_SET(table_storage_scopes, &db_and_table, scope);
}

ScopePtr Analysis::getTableStorageScope(ASTIdentifier & db_and_table)
{
    MAP_GET(table_storage_scopes, &db_and_table);
}

/*
void Analysis::setTableColumnMasks(ASTIdentifier & db_and_table, ASTs column_masks)
{
    MAP_SET(table_column_masks, &db_and_table, std::move(column_masks));
}

ASTs & Analysis::getTableColumnMasks(ASTIdentifier & db_and_table)
{
    MAP_GET(table_column_masks, &db_and_table);
}
*/

void Analysis::setTableAliasColumns(ASTIdentifier & db_and_table, ASTs alias_columns)
{
    MAP_SET(table_alias_columns, &db_and_table, std::move(alias_columns));
}

ASTs & Analysis::getTableAliasColumns(ASTIdentifier & db_and_table)
{
    MAP_GET(table_alias_columns, &db_and_table);
}

bool Analysis::hasExpressionType(const ASTPtr & expression)
{
    return expression_types.find(expression) != expression_types.end();
}

void Analysis::setExpressionType(const ASTPtr & expression, const DataTypePtr & type)
{
    expression_types[expression] = type;
}

DataTypePtr Analysis::getExpressionType(const ASTPtr & expression)
{
    MAP_GET(expression_types, expression);
}

JoinUsingAnalysis & Analysis::getJoinUsingAnalysis(ASTTableJoin & table_join)
{
    MAP_GET(join_using_results, &table_join);
}

JoinOnAnalysis & Analysis::getJoinOnAnalysis(ASTTableJoin & table_join)
{
    MAP_GET(join_on_results, &table_join);
}

const StorageAnalysis & Analysis::getStorageAnalysis(const IAST & ast)
{
    MAP_GET(storage_results, &ast);
}

UInt64 Analysis::getLimitByValue(ASTSelectQuery & select_query)
{
    MAP_GET(limit_by_values, &select_query);
}

UInt64 Analysis::getLimitLength(ASTSelectQuery & select_query)
{
    MAP_GET(limit_lengths, &select_query);
}

UInt64 Analysis::getLimitOffset(ASTSelectQuery & select_query)
{
    MAP_GET(limit_offsets, &select_query);
}

void Analysis::setColumnReference(const ASTPtr & ast, const ResolvedField & resolved)
{
    column_references[ast] = resolved;
}

std::optional<ResolvedField> Analysis::tryGetColumnReference(const ASTPtr & ast)
{
    if (column_references.find(ast) != column_references.end())
        return column_references.at(ast);

    return std::nullopt;
}

void Analysis::setLambdaArgumentReference(const ASTPtr & ast, const ResolvedField & resolved)
{
    lambda_argument_references[ast] = resolved;
}

std::optional<ResolvedField> Analysis::tryGetLambdaArgumentReference(const ASTPtr & ast)
{
    if (lambda_argument_references.find(ast) != lambda_argument_references.end())
        return lambda_argument_references.at(ast);

    return std::nullopt;
}

std::vector<AggregateAnalysis> & Analysis::getAggregateAnalysis(ASTSelectQuery & select_query)
{
    return aggregate_results[&select_query];
}

std::vector<std::pair<String, UInt16>> & Analysis::getInterestEvents(ASTSelectQuery & select_query)
{
    return interest_events[&select_query];
}

std::vector<ASTFunctionPtr> & Analysis::getGroupingOperations(ASTSelectQuery & select_query)
{
    return grouping_operations[&select_query];
}

void Analysis::addWindowAnalysis(ASTSelectQuery & select_query, WindowAnalysisPtr analysis)
{
    window_results_by_select_query[&select_query].push_back(analysis);
    MAP_SET(window_results_by_ast, analysis->expression, analysis);
}

WindowAnalysisPtr Analysis::getWindowAnalysis(const ASTPtr & ast)
{
    MAP_GET(window_results_by_ast, ast);
}

std::vector<WindowAnalysisPtr> & Analysis::getWindowAnalysisOfSelectQuery(ASTSelectQuery & select_query)
{
    return window_results_by_select_query[&select_query];
}

bool Analysis::needAggregate(ASTSelectQuery & select_query)
{
    return !getAggregateAnalysis(select_query).empty() || select_query.groupBy();
}

std::vector<ASTPtr> & Analysis::getScalarSubqueries(ASTSelectQuery & select_query)
{
    return scalar_subqueries[&select_query];
}

std::vector<ASTPtr> & Analysis::getInSubqueries(ASTSelectQuery & select_query)
{
    return in_subqueries[&select_query];
}

std::vector<ASTPtr> & Analysis::getExistsSubqueries(ASTSelectQuery & select_query)
{
    return exists_subqueries[&select_query];
}

std::vector<ASTPtr> & Analysis::getQuantifiedComparisonSubqueries(ASTSelectQuery & select_query)
{
    return quantified_comparison_subqueries[&select_query];
}

void Analysis::registerCTE(ASTSubquery & subquery)
{
    auto clone = std::make_shared<ASTSubquery>(subquery);
    clone->alias.clear();
    auto iter = common_table_expressions.find(clone);
    if (iter == common_table_expressions.end())
        common_table_expressions.emplace(
            clone, CTEAnalysis{.id = static_cast<CTEId>(common_table_expressions.size()), .representative = &subquery, .ref_count = 1});
    else
        iter->second.ref_count++;
}

bool Analysis::isSharableCTE(ASTSubquery & subquery)
{
    auto clone = std::make_shared<ASTSubquery>(subquery);
    clone->alias.clear();
    if (auto iter = common_table_expressions.find(clone); iter != common_table_expressions.end())
        return iter->second.ref_count >= 2;
    return false;
}

CTEAnalysis & Analysis::getCTEAnalysis(ASTSubquery & subquery)
{
    auto clone = std::make_shared<ASTSubquery>(subquery);
    clone->alias.clear();
    MAP_GET(common_table_expressions, clone);
}

ASTs & Analysis::getSelectExpressions(ASTSelectQuery & select_query)
{
    return select_expressions[&select_query];
}

GroupByAnalysis & Analysis::getGroupByAnalysis(ASTSelectQuery & select_query)
{
    return group_by_results[&select_query];
}

std::vector<std::shared_ptr<ASTOrderByElement>> & Analysis::getOrderByAnalysis(ASTSelectQuery & select_query)
{
    return order_by_results[&select_query];
}

void Analysis::setOutputDescription(IAST & ast, const FieldDescriptions & field_descs)
{
    if (auto * subquery = ast.as<ASTSubquery>())
        setOutputDescription(*subquery->children[0], field_descs);

    MAP_SET(output_descriptions, &ast, field_descs);
}

FieldDescriptions & Analysis::getOutputDescription(IAST & ast)
{
    if (auto * subquery = ast.as<ASTSubquery>())
        return getOutputDescription(*subquery->children[0]);

    MAP_GET(output_descriptions, &ast);
}

void Analysis::setRegisteredWindow(ASTSelectQuery & select_query, const String & name, ResolvedWindowPtr & window)
{
    MAP_SET(registered_windows[&select_query], name, window);
}

ResolvedWindowPtr Analysis::getRegisteredWindow(ASTSelectQuery & select_query, const String & name)
{
    MAP_GET(registered_windows[&select_query], name);
}

const std::unordered_map<String, ResolvedWindowPtr> & Analysis::getRegisteredWindows(ASTSelectQuery & select_query)
{
    return registered_windows[&select_query];
}

void Analysis::setTypeCoercion(const ASTPtr & expression, const DataTypePtr & coerced_type)
{
    type_coercions[expression] = coerced_type;
}

DataTypePtr Analysis::getTypeCoercion(const ASTPtr & expression)
{
    return type_coercions.count(expression) ? type_coercions[expression] : nullptr;
}

void Analysis::setRelationTypeCoercion(IAST & ast, const DataTypes & coerced_types)
{
    MAP_SET(relation_type_coercions, &ast, coerced_types);
}

bool Analysis::hasRelationTypeCoercion(IAST & ast)
{
    return relation_type_coercions.count(&ast);
}

const DataTypes & Analysis::getRelationTypeCoercion(IAST & ast)
{
    MAP_GET(relation_type_coercions, &ast);
}

void Analysis::setSubColumnReference(const ASTPtr & ast, const SubColumnReference & reference)
{
    MAP_SET(sub_column_references, ast, reference);
}

std::optional<SubColumnReference> Analysis::tryGetSubColumnReference(const ASTPtr & ast)
{
    if (sub_column_references.find(ast) != sub_column_references.end())
        return sub_column_references.at(ast);

    return std::nullopt;
}

void Analysis::addUsedSubColumn(const IAST * table_ast, size_t field_index, const SubColumnID & sub_column_id)
{
    auto & vec = used_sub_columns[table_ast];
    vec.resize(std::max(field_index + 1, vec.size()));
    vec[field_index].insert(sub_column_id);
}

const std::vector<SubColumnIDSet> & Analysis::getUsedSubColumns(const IAST & table_ast)
{
    return used_sub_columns[&table_ast];
}

void Analysis::addNonDeterministicFunctions(IAST & ast)
{
    non_deterministic_functions.insert(&ast);
}

}
