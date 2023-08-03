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

#include <Analyzers/QueryAnalyzer.h>
#include <Analyzers/ExprAnalyzer.h>
#include <Analyzers/ScopeAwareEquals.h>
#include <Analyzers/analyze_common.h>
#include <Analyzers/function_utils.h>
#include <Analyzers/ExpressionVisitor.h>
#include <Analyzers/tryEvaluateConstantExpression.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeNothing.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/getLeastSupertype.h>
#include <Interpreters/QueryAliasesVisitor.h>
#include <Interpreters/QueryNormalizer.h>
#include <Interpreters/RequiredSourceColumnsVisitor.h>
#include <Interpreters/TranslateQualifiedNamesVisitor.h>
#include <Interpreters/convertFieldToType.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Parsers/ASTVisitor.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTAsterisk.h>
#include <Parsers/ASTQualifiedAsterisk.h>
#include <Parsers/ASTColumnsMatcher.h>
#include <Parsers/ASTFieldReference.h>
#include <Parsers/queryToString.h>
#include <QueryPlan/Void.h>
#include <Storages/IStorage.h>
#include <Storages/StorageDistributed.h>
#include <Storages/StorageMemory.h>
#include <Optimizer/Utils.h>

#include <unordered_map>
#include <sstream>

using namespace std::string_literals;

namespace DB
{

namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
    extern const int TYPE_MISMATCH;
    extern const int UNKNOWN_IDENTIFIER;
    extern const int UNION_ALL_RESULT_STRUCTURES_MISMATCH;
    extern const int BAD_ARGUMENTS;
    extern const int EXPECTED_ALL_OR_ANY;
    extern const int ILLEGAL_AGGREGATION;
    extern const int NOT_AN_AGGREGATE;
    extern const int INVALID_JOIN_ON_EXPRESSION;
    extern const int UNKNOWN_JOIN;
    extern const int ILLEGAL_TYPE_OF_COLUMN_FOR_FILTER;
}

class QueryAnalyzerVisitor : public ASTVisitor<Void, const Void>
{
public:
    Void process(ASTPtr & node) { return ASTVisitorUtil::accept(node, *this, {}); }

    Void visitASTSelectIntersectExceptQuery(ASTPtr & node, const Void &) override;
    Void visitASTSelectWithUnionQuery(ASTPtr & node, const Void &) override;
    Void visitASTSelectQuery(ASTPtr & node, const Void &) override;
    Void visitASTSubquery(ASTPtr & node, const Void &) override;

    QueryAnalyzerVisitor(ContextMutablePtr context_, Analysis & analysis_, ScopePtr outer_query_scope_):
        context(std::move(context_))
        , analysis(analysis_)
        , outer_query_scope(outer_query_scope_)
        , use_ansi_semantic(context->getSettingsRef().dialect_type == DialectType::ANSI)
        , enable_shared_cte(context->getSettingsRef().cte_mode != CTEMode::INLINED)
        , enable_implicit_type_conversion(context->getSettingsRef().enable_implicit_type_conversion)
        , allow_extended_conversion(context->getSettingsRef().allow_extended_type_conversion)
    {}

private:
    ContextMutablePtr context;
    Analysis & analysis;
    const ScopePtr outer_query_scope;
    const bool use_ansi_semantic;
    const bool enable_shared_cte;
    const bool enable_implicit_type_conversion;
    const bool allow_extended_conversion;

    void analyzeSetOperation(ASTPtr & node, ASTs & selects);

    /// FROM clause
    ScopePtr analyzeWithoutFrom(ASTSelectQuery & select_query);
    ScopePtr analyzeFrom(ASTTablesInSelectQuery & tables_in_select, ASTSelectQuery & select_query);
    ScopePtr analyzeTableExpression(ASTTableExpression & table_expression, const QualifiedName & column_prefix);
    ScopePtr analyzeTable(ASTTableIdentifier & db_and_table, const QualifiedName & column_prefix);
    ScopePtr analyzeSubquery(ASTPtr & node, const QualifiedName & column_prefix);
    ScopePtr analyzeTableFunction(ASTFunction & table_function, const QualifiedName & column_prefix);
    ScopePtr analyzeJoin(
        ASTTableJoin & table_join,
        ScopePtr left_scope,
        ScopePtr right_scope,
        const String & right_table_qualifier,
        ASTSelectQuery & select_query);
    ScopePtr analyzeJoinUsing(
        ASTTableJoin & table_join,
        ScopePtr left_scope,
        ScopePtr right_scope,
        const String & right_table_qualifier,
        ASTSelectQuery & select_query);
    ScopePtr analyzeJoinOn(ASTTableJoin & table_join, ScopePtr left_scope, ScopePtr right_scope, const String & right_table_qualifier);

    void analyzeWindow(ASTSelectQuery & select_query);
    void analyzePrewhere(ASTSelectQuery & select_query, ScopePtr source_scope);
    void analyzeWhere(ASTSelectQuery & select_query, ScopePtr source_scope);
    ASTs analyzeSelect(ASTSelectQuery & select_query, ScopePtr source_scope);
    void analyzeGroupBy(ASTSelectQuery & select_query, ASTs & select_expressions, ScopePtr source_scope);
    // void analyzeInterestEvents(ASTSelectQuery & select_query);
    void analyzeHaving(ASTSelectQuery & select_query, ScopePtr source_scope);
    ScopePtr buildOrderByScope(ASTSelectQuery & select_query, ScopePtr source_scope);
    void analyzeOrderBy(ASTSelectQuery & select_query, ASTs & select_expressions, ScopePtr output_scope);
    void analyzeLimitBy(ASTSelectQuery & select_query, ScopePtr output_scope);
    void analyzeLimitAndOffset(ASTSelectQuery & select_query);

    /// utils
    ScopePtr createScope(FieldDescriptions field_descriptions, ScopePtr parent = nullptr);
    DatabaseAndTableWithAlias extractTableWithAlias(const ASTTableExpression & table_expression);
    void verifyNoAggregateWindowOrGroupingOperations(ASTPtr & expression, const String & statement_name);
    void verifyAggregate(ASTSelectQuery & select_query, ScopePtr source_scope);
    void verifyNoFreeReferencesToLambdaArgument(ASTSelectQuery & select_query);
    void verifyNoReferenceToOrderByScopeInsideAggregateOrWindow(ASTSelectQuery & select_query, ScopePtr order_by_scope);
    UInt64 analyzeUIntConstExpression(const ASTPtr & expression);
};

static NameSet collectNames(ScopePtr scope);
static String qualifyJoinedName(const String & name, const String & table_qualifier, const NameSet & source_names);

AnalysisPtr QueryAnalyzer::analyze(ASTPtr & ast, ContextMutablePtr context)
{
    AnalysisPtr analysis_ptr = std::make_unique<Analysis>();
    analyze(ast, nullptr, std::move(context), *analysis_ptr);
    return analysis_ptr;
}

void QueryAnalyzer::analyze(ASTPtr & query, ScopePtr outer_query_scope, ContextMutablePtr context, Analysis & analysis)
{
    QueryAnalyzerVisitor analyzer_visitor {std::move(context), analysis, outer_query_scope};
    analyzer_visitor.process(query);
}

Void QueryAnalyzerVisitor::visitASTSelectIntersectExceptQuery(ASTPtr & node, const Void &)
{
    auto & intersect_or_except = node->as<ASTSelectIntersectExceptQuery &>();
    auto list_of_selects = intersect_or_except.getListOfSelects();

    assert(intersect_or_except.final_operator != ASTSelectIntersectExceptQuery::Operator::UNKNOWN);
    analyzeSetOperation(node, list_of_selects);
    return {};
}

Void QueryAnalyzerVisitor::visitASTSelectWithUnionQuery(ASTPtr & node, const Void &)
{
    auto & select_with_union = node->as<ASTSelectWithUnionQuery &>();

    // some ASTSelectWithUnionQueries are generated by QueryRewriter,
    // thus are not normalized. see also: JoinToSubqueryTransformVisitor.cpp
    bool normalized =
        (select_with_union.is_normalized && (select_with_union.union_mode == ASTSelectWithUnionQuery::Mode::ALL ||
                                             select_with_union.union_mode == ASTSelectWithUnionQuery::Mode::DISTINCT))
        || select_with_union.list_of_selects->children.size() == 1;
    if (!normalized)
        throw Exception("Invalid ASTSelectWithUnionQuery found", ErrorCodes::LOGICAL_ERROR);

    analyzeSetOperation(node, select_with_union.list_of_selects->children);
    return {};
}

Void QueryAnalyzerVisitor::visitASTSelectQuery(ASTPtr & node, const Void &)
{
    auto & select_query = node->as<ASTSelectQuery &>();
    ScopePtr source_scope;

    if (select_query.tables())
        source_scope = analyzeFrom(select_query.refTables()->as<ASTTablesInSelectQuery &>(), select_query);
    else
        source_scope = analyzeWithoutFrom(select_query);

    analyzeWindow(select_query);
    analyzeWhere(select_query, source_scope);
    // analyze SELECT first since SELECT item may be referred in GROUP BY/ORDER BY
    ASTs select_expression = analyzeSelect(select_query, source_scope);
    analyzeGroupBy(select_query, select_expression, source_scope);
    // analyzeInterestEvents(select_query);
    analyzeHaving(select_query, source_scope);
    ScopePtr order_by_scope = buildOrderByScope(select_query, source_scope);
    analyzeOrderBy(select_query, select_expression, order_by_scope);
    analyzeLimitBy(select_query, order_by_scope);
    analyzeLimitAndOffset(select_query);
    verifyAggregate(select_query, source_scope);
    verifyNoFreeReferencesToLambdaArgument(select_query);
    if (order_by_scope != source_scope)
        verifyNoReferenceToOrderByScopeInsideAggregateOrWindow(select_query, order_by_scope);
    // TODO: check useless ORDER BY in subquery(ANSI sql)
    return {};
}

Void QueryAnalyzerVisitor::visitASTSubquery(ASTPtr & node, const Void &)
{
    auto & subquery = node->as<ASTSubquery &>();

    if (enable_shared_cte && subquery.isWithClause())
    {
        analysis.registerCTE(subquery);

        // CTE has been analyzed
        if (analysis.isSharableCTE(subquery))
        {
            auto * representative = analysis.getCTEAnalysis(subquery).representative;
            analysis.setOutputDescription(subquery, analysis.getOutputDescription(*representative));
            return {};
        }
    }

    process(node->children.front());
    return {};
}

void QueryAnalyzerVisitor::analyzeSetOperation(ASTPtr & node, ASTs & selects)
{
    size_t column_size = 0;

    // analyze union elements
    for (auto & select: selects)
    {
        process(select);

        if (column_size == 0)
            column_size = analysis.getOutputDescription(*select).size();
        else if (column_size != analysis.getOutputDescription(*select).size())
            throw Exception("Different number of columns for UNION ALL elements: " + serializeAST(*node),
                            ErrorCodes::UNION_ALL_RESULT_STRUCTURES_MISMATCH);
    }

    // analyze output fields
    FieldDescriptions output_desc;
    if (selects.size() == 1)
    {
        output_desc = analysis.getOutputDescription(*selects[0]);
    }
    else
    {
        auto & first_input_desc = analysis.getOutputDescription(*selects[0]);

        for (size_t column_idx = 0; column_idx < column_size; ++column_idx)
        {
            DataTypes elem_types;
            for (auto & select: selects)
                elem_types.push_back(analysis.getOutputDescription(*select)[column_idx].type);

            // promote output type to super type if necessary
            auto output_type = getLeastSupertype(elem_types, allow_extended_conversion);
            output_desc.emplace_back(first_input_desc[column_idx].name, output_type);
        }
    }

    // record type coercion
    if (selects.size() != 1)
    {
        for (auto & select: selects)
        {
            auto & input_desc = analysis.getOutputDescription(*select);
            DataTypes type_coercion(column_size, nullptr);

            for (size_t column_idx = 0; column_idx < column_size; ++column_idx)
            {
                auto input_type = input_desc[column_idx].type;
                auto output_type = output_desc[column_idx].type;

                if (!input_type->equals(*output_type))
                {
                    if (enable_implicit_type_conversion)
                    {
                        type_coercion[column_idx] = output_type;
                    }
                    else
                    {
                        std::ostringstream errormsg;
                        errormsg << "Column type mismatch for UNION: " << serializeAST(*node) << ", expect type: "
                                 << output_type->getName() << ", but found type: " << input_type->getName();
                        throw Exception(errormsg.str(), ErrorCodes::UNION_ALL_RESULT_STRUCTURES_MISMATCH);
                    }
                }
            }

            analysis.setRelationTypeCoercion(*select, type_coercion);
        }
    }

    analysis.setOutputDescription(*node, output_desc);
}

ScopePtr QueryAnalyzerVisitor::analyzeWithoutFrom(ASTSelectQuery & select_query)
{
    FieldDescriptions fields;
    fields.emplace_back("dummy", std::make_shared<DataTypeUInt8>());
    const auto *scope = createScope(fields);
    analysis.setQueryWithoutFromScope(select_query, scope);
    return scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeFrom(ASTTablesInSelectQuery & tables_in_select, ASTSelectQuery & select_query)
{
    if (tables_in_select.children.empty())
        throw Exception("ASTTableInSelectQuery can not be empty.", ErrorCodes::LOGICAL_ERROR);

    auto analyze_first_table_element = [&](ASTPtr & ast) -> ScopePtr
    {
        auto & table_element = ast->as<ASTTablesInSelectQueryElement &>();

        if (auto * table_expression = table_element.table_expression->as<ASTTableExpression>())
        {
            auto table_with_alias = extractTableWithAlias(*table_expression);
            return analyzeTableExpression(*table_expression, QualifiedName::extractQualifiedName(table_with_alias));
        }
        else if (table_element.array_join)
        {
            // TODO: support array join
            throw Exception("Array join is not supported yet.", ErrorCodes::NOT_IMPLEMENTED);
        }
        else
        {
            throw Exception("Invalid table table_element.", ErrorCodes::LOGICAL_ERROR);
        }
    };

    ScopePtr current_scope = analyze_first_table_element(tables_in_select.children[0]);

    for (size_t idx = 1; idx < tables_in_select.children.size(); ++idx)
    {
        auto & table_element = tables_in_select.children[idx]->as<ASTTablesInSelectQueryElement &>();

        if (auto * table_expression = table_element.table_expression->as<ASTTableExpression>())
        {
            auto table_with_alias = extractTableWithAlias(*table_expression);
            ScopePtr joined_table_scope = analyzeTableExpression(*table_expression, QualifiedName::extractQualifiedName(table_with_alias));
            auto & table_join = table_element.table_join->as<ASTTableJoin &>();
            current_scope = analyzeJoin(table_join, current_scope, joined_table_scope, table_with_alias.getQualifiedNamePrefix(true), select_query);
        }
        else if (table_element.array_join)
        {
            // TODO: support array join
            throw Exception("Array join is not supported yet.", ErrorCodes::NOT_IMPLEMENTED);
        }
        else
            throw Exception("Invalid table element.", ErrorCodes::LOGICAL_ERROR);
    }

    analysis.setScope(tables_in_select, current_scope);
    return current_scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeTableExpression(ASTTableExpression & table_expression, const QualifiedName & column_prefix)
{
    ScopePtr scope;

    if (table_expression.database_and_table_name)
        scope = analyzeTable(table_expression.database_and_table_name->as<ASTTableIdentifier &>(), column_prefix);
    else if (table_expression.subquery)
        scope = analyzeSubquery(table_expression.subquery, column_prefix);
    else if (table_expression.table_function)
        scope = analyzeTableFunction(table_expression.table_function->as<ASTFunction &>(), column_prefix);
    else
        throw Exception("Invalid ASTTableExpression: " + serializeAST(table_expression), ErrorCodes::LOGICAL_ERROR);

    return scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeTable(ASTTableIdentifier & db_and_table, const QualifiedName & column_prefix)
{
    // get storage information
    StoragePtr storage;
    String full_table_name;

    {
        auto storage_id = context->tryResolveStorageID(db_and_table.getTableId());
        storage = DatabaseCatalog::instance().getTable(storage_id, context);
        full_table_name = storage_id.getFullTableName();

        if (storage_id.getDatabaseName() != "system" &&
            !(dynamic_cast<const MergeTreeMetaBase *>(storage.get()) || dynamic_cast<const StorageMemory *>(storage.get())))
            throw Exception("Only cnch tables & system tables are supported", ErrorCodes::NOT_IMPLEMENTED);

        analysis.storage_results[&db_and_table] = StorageAnalysis { storage_id.getDatabaseName(), storage_id.getTableName(), storage};
    }

    StorageMetadataPtr storage_metadata = storage->getInMemoryMetadataPtr();

    // For StorageDistributed, the metadata of distributed table may diff with the metadata of local table.
    // In this case, we use the one of local table.
    if (const auto * storage_distributed = dynamic_cast<const StorageDistributed *>(storage.get()))
    {
        // when diff server diff remote database name, the database name is empty.
        if (!storage_distributed->getRemoteDatabaseName().empty())
        {
            StorageID local_id {storage_distributed->getRemoteDatabaseName(), storage_distributed->getRemoteTableName()};
            auto storage_local = DatabaseCatalog::instance().getTable(local_id, context);
            storage_metadata = storage_local->getInMemoryMetadataPtr();
        }
    }

    const auto & columns_description = storage_metadata->getColumns();
    ScopePtr scope;
    FieldDescriptions fields;
    ASTIdentifier * origin_table_ast = &db_and_table;

    auto add_field = [&](const String & name, const DataTypePtr & type, bool substitude_for_asterisk)
    {
        fields.emplace_back(name, type, column_prefix, storage, origin_table_ast, name, fields.size(), substitude_for_asterisk);
    };

    // get columns
    {
        for (const auto & column : columns_description.getOrdinary())
        {
            add_field(column.name, column.type, true);
        }

        for (const auto & column : columns_description.getMaterialized())
        {
            add_field(column.name, column.type, false);
        }

        for (const auto & column : storage->getVirtuals())
        {
            add_field(column.name, column.type, false);
        }

        for (const auto & column: columns_description.getSubcolumnsOfAllPhysical())
        {
            add_field(column.name, column.type, false);
        }

        scope = createScope(fields);
        analysis.setTableStorageScope(db_and_table, scope);
    }

    // get alias columns
    {
        ASTPtr alias_columns = std::make_shared<ASTExpressionList>();
        for (const auto & column : columns_description.getAliases())
        {
            const auto column_default = columns_description.getDefault(column.name);
            if (column_default)
            {
                alias_columns->children.emplace_back(setAlias(column_default->expression->clone(), column.name));
            }
            else
            {
                throw Exception("Alias column " + column.name + " not found for table " + full_table_name, ErrorCodes::LOGICAL_ERROR);
            }
        }

        // users are allowed to define alias columns like: CREATE TABLE t (a Int32, b ALIAS a + 1, c ALIAS b + 1)
        if (!alias_columns->children.empty())
        {
            Aliases aliases;
            QueryAliasesVisitor::Data query_aliases_data{aliases};
            QueryAliasesVisitor(query_aliases_data).visit(alias_columns);
            NameSet source_columns_set;
            QueryNormalizer::Data normalizer_data(aliases, source_columns_set, false, context->getSettingsRef(), true, context, nullptr, nullptr);
            QueryNormalizer(normalizer_data).visit(alias_columns);
        }

        for (auto & alias_col : alias_columns->children)
        {
            auto col_type = ExprAnalyzer::analyze(alias_col, scope, context, analysis, "alias column"s);
            auto col_name = alias_col->tryGetAlias();
            add_field(col_name, col_type, false);
        }

        scope = createScope(fields);
        analysis.setScope(db_and_table, scope);
        analysis.setTableAliasColumns(db_and_table, std::move(alias_columns->children));
    }

    return scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeSubquery(ASTPtr & node, const QualifiedName & column_prefix)
{
    process(node);

    auto & subquery = node->as<ASTSubquery &>();
    FieldDescriptions field_descriptions;
    for (auto & col: analysis.getOutputDescription(subquery))
    {
        field_descriptions.emplace_back(col.withNewPrefix(column_prefix));
    }

    const auto * subquery_scope = createScope(field_descriptions);
    analysis.setScope(subquery, subquery_scope);
    return subquery_scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeTableFunction(ASTFunction & /*table_function*/, const QualifiedName & /*column_prefix*/)
{
    throw Exception("table function is not supported yet", ErrorCodes::NOT_IMPLEMENTED);
    /*
    // execute table function
    StoragePtr storage = context->getQueryContext()->executeTableFunction(table_function.ptr());
    StorageID storage_id = storage->getStorageID();
    analysis.storage_results[&table_function] = StorageAnalysis {storage_id.getDatabaseName(), storage_id.getTableName(), storage};

    // get columns information
    auto storage_metadata = storage->getInMemoryMetadataPtr();
    const auto & columns_description = storage_metadata->getColumns();
    FieldDescriptions field_descriptions;

    for (const auto & column : columns_description.getAllPhysical())
    {
        field_descriptions.emplace_back(column.name, column.type, column_prefix, storage, &table_function,
                                        column.name, field_descriptions.size(), true);
    }

    const auto * table_function_scope = createScope(field_descriptions);
    analysis.setScope(table_function, table_function_scope);
    return table_function_scope;
     */
}

ScopePtr QueryAnalyzerVisitor::analyzeJoin(ASTTableJoin & table_join, ScopePtr left_scope, ScopePtr right_scope,
                                           const String & right_table_qualifier, ASTSelectQuery & select_query)
{
    // set join strictness if unspecified
    {
        auto join_default_strictness = context->getSettingsRef().join_default_strictness;
        if (table_join.strictness == ASTTableJoin::Strictness::Unspecified && !isCrossJoin(table_join))
        {
            if (join_default_strictness == JoinStrictness::ANY)
                table_join.strictness = ASTTableJoin::Strictness::Any;
            else if (join_default_strictness == JoinStrictness::ALL)
                table_join.strictness = ASTTableJoin::Strictness::All;
            else
                throw Exception(
                    "Expected ANY or ALL in JOIN section, because setting (join_default_strictness) is empty",
                    DB::ErrorCodes::EXPECTED_ALL_OR_ANY);
        }
    }

    if (context->getSettings().any_join_distinct_right_table_keys)
    {
        if (table_join.strictness == ASTTableJoin::Strictness::Any &&
            table_join.kind == ASTTableJoin::Kind::Inner)
        {
            table_join.strictness = ASTTableJoin::Strictness::Semi;
            table_join.kind = ASTTableJoin::Kind::Left;
        }

        if (table_join.strictness == ASTTableJoin::Strictness::Any)
            table_join.strictness = ASTTableJoin::Strictness::RightAny;
    }
    else
    {
        if (table_join.strictness == ASTTableJoin::Strictness::Any)
            if (table_join.kind == ASTTableJoin::Kind::Full)
                throw Exception("ANY FULL JOINs are not implemented.", ErrorCodes::NOT_IMPLEMENTED);
    }

    {
        if (isCrossJoin(table_join))
        {
            if (table_join.strictness != ASTTableJoin::Strictness::Unspecified)
                throw Exception("CROSS/COMMA join must leave join strictness unspecified.", ErrorCodes::UNKNOWN_JOIN);

            if (joinCondition(table_join))
                throw Exception("CROSS/COMMA join must not have join conditions.", ErrorCodes::UNKNOWN_JOIN);
        }
        else if (isSemiOrAntiJoin(table_join))
        {
            if (!isInner(table_join.kind))
                throw Exception("Illegal join kind for SEMI/ANTI join.", ErrorCodes::UNKNOWN_JOIN);
        }
        else if (isAsofJoin(table_join))
        {
            if (!(isInner(table_join.kind) || isLeft(table_join.kind)))
            {
                throw Exception("Join kind for Asof join must be either Inner or Left", ErrorCodes::UNKNOWN_JOIN);
            }
        }
    }

    ScopePtr joined_scope;

    if (table_join.using_expression_list)
        joined_scope = analyzeJoinUsing(table_join, left_scope, right_scope, right_table_qualifier, select_query);
    else
        joined_scope = analyzeJoinOn(table_join, left_scope, right_scope, right_table_qualifier);

    analysis.setScope(table_join, joined_scope);

    // validate join criteria for asof join
    {
        if (isAsofJoin(table_join))
        {
            if (table_join.using_expression_list)
            {
                if (analysis.join_using_results.at(&table_join).left_join_fields.size() < 2)
                    throw Exception("Join using list must have at least 2 elements for asof join", ErrorCodes::LOGICAL_ERROR);
            }
            else if (table_join.on_expression)
            {
                auto & join_on_result = analysis.join_on_results.at(&table_join);
                if (join_on_result.equality_conditions.empty())
                    throw Exception("Asof join must have at least 1 equality condition", ErrorCodes::LOGICAL_ERROR);
                if (join_on_result.inequality_conditions.size() != 1)
                    throw Exception("Asof join must have exact 1 inequality condition", ErrorCodes::LOGICAL_ERROR);
                if (!join_on_result.complex_expressions.empty())
                    throw Exception("Asof join must not have complex condition", ErrorCodes::LOGICAL_ERROR);
            }
            else
                throw Exception("Asof join must have join conditions", ErrorCodes::LOGICAL_ERROR);
        }
    }

    if (isSemiOrAntiJoin(table_join))
        return left_scope;
    else
        return joined_scope;
}

ScopePtr QueryAnalyzerVisitor::analyzeJoinUsing(ASTTableJoin & table_join, ScopePtr left_scope, ScopePtr right_scope,
                                                const String & right_table_qualifier, ASTSelectQuery & select_query)
{
    auto & expr_list = table_join.using_expression_list->children;

    if (expr_list.empty())
        throw Exception("Empty join using list is not allowed.", ErrorCodes::LOGICAL_ERROR);

    std::vector<ASTPtr> join_key_asts;
    std::vector<size_t> left_join_fields;
    std::vector<size_t> right_join_fields;
    DataTypes left_coercions;
    DataTypes right_coercions;
    std::unordered_map<size_t, size_t> left_join_field_reverse_map;
    std::unordered_map<size_t, size_t> right_join_field_reverse_map;
    std::vector<bool> require_right_keys;  // Clickhouse semantic specific
    NameSet seen_names;
    FieldDescriptions output_fields;

    if (use_ansi_semantic)
    {
        auto resolve_join_key = [&](const QualifiedName & name, ScopePtr scope, bool left, std::vector<size_t> & join_field_indices) -> ResolvedField
        {
            std::optional<ResolvedField> resolved = scope->resolveFieldByAnsi(name);

            if (!resolved)
                throw Exception("Can not find column '" + name.toString() + "' in join " + (left ? "left" : "right") + " side",
                                ErrorCodes::UNKNOWN_IDENTIFIER);

            join_field_indices.emplace_back(resolved->hierarchy_index);
            return *resolved;
        };

        /// Step 1. resolve join key
        for (const auto& join_key_ast : expr_list)
        {
            auto * iden = join_key_ast->as<ASTIdentifier>();

            if (!iden)
                throw Exception("Expression except identifier is not allowed in join using list.", ErrorCodes::LOGICAL_ERROR);

            if (iden->compound())
                throw Exception("Compound identifier is not allowed in join using list.", ErrorCodes::LOGICAL_ERROR);

            if (!seen_names.insert(iden->name()).second)
                throw Exception("Duplicated join key found in join using list.", ErrorCodes::LOGICAL_ERROR);

            join_key_asts.push_back(join_key_ast);
            QualifiedName qualified_name {iden->name()};

            auto left_field = resolve_join_key(qualified_name, left_scope, true, left_join_fields);
            auto right_field = resolve_join_key(qualified_name, right_scope, false, right_join_fields);
            auto left_type = left_field.getFieldDescription().type;
            auto right_type = right_field.getFieldDescription().type;
            DataTypePtr output_type = nullptr;

            if (left_type->equals(*right_type))
            {
                output_type = left_type;
            }
            else if (enable_implicit_type_conversion)
            {
                try
                {
                    output_type = getLeastSupertype({left_type, right_type}, allow_extended_conversion);
                }
                catch (DB::Exception & ex)
                {
                    throw Exception(
                        "Type mismatch of columns to JOIN by: " + left_type->getName() + " at left, " + right_type->getName() + " at right. "
                            + "Can't get supertype: " + ex.message(),
                        ErrorCodes::TYPE_MISMATCH);
                }
            }

            if (!output_type)
                throw Exception("Type mismatch for join keys: " + serializeAST(table_join), ErrorCodes::TYPE_MISMATCH);

            left_coercions.push_back(left_type->equals(*output_type) ? nullptr : output_type);
            right_coercions.push_back(right_type->equals(*output_type) ? nullptr : output_type);
            output_fields.emplace_back(left_field.getFieldDescription().name, output_type);
        }

        /// Step 2. add non join fields
        auto add_non_join_fields = [&](ScopePtr scope, std::vector<size_t> & join_fields_list)
        {
            std::unordered_set<size_t> join_fields {join_fields_list.begin(), join_fields_list.end()};

            for (size_t i = 0; i < scope->size(); ++i)
            {
                if (join_fields.find(i) == join_fields.end())
                    output_fields.push_back(scope->at(i));
            }
        };

        add_non_join_fields(left_scope, left_join_fields);
        add_non_join_fields(right_scope, right_join_fields);
    }
    else
    {
        DataTypes join_key_output_types;

        /// Step 1. resolve join key
        for (size_t i = 0, true_index = 0; i < expr_list.size(); ++i)
        {
            const auto & join_key_ast = expr_list[i];
            String key_name = join_key_ast->getAliasOrColumnName();

            // see also 00702_join_with_using:
            // SELECT * FROM using1 ALL LEFT JOIN (SELECT * FROM using2) js2 USING (a, a, a, b, b, b, a, a) ORDER BY a;
            if (!seen_names.insert(key_name).second)
                continue;

            join_key_asts.push_back(join_key_ast);

            // Clickhouse allow join key is an expression. In this case, engine will calculate the expression
            // and use the result column as the join key. Note that this only apply for the left table.
            // e.g. 00057_join_aliases
            //     SELECT number, number / 2 AS n, j1, j2
            //     FROM system.numbers ANY LEFT JOIN
            //         (SELECT number / 3 AS n, number AS j1, 'Hello' AS j2 FROM system.numbers LIMIT 10)
            //     USING n
            //     LIMIT 10
            // In rewrite phase, identifier 'n' will be rewritten to 'number / 2 AS n'.
            // For this case, we record join field as -1, and plan an additional projection before join.
            auto left_type = ExprAnalyzer::analyze(join_key_ast, left_scope, context, analysis, "JOIN USING"s);
            size_t left_field_index = -1;
            if (auto col_ref = analysis.tryGetColumnReference(join_key_ast))
            {
                left_field_index = col_ref->hierarchy_index;
            }
            left_join_fields.emplace_back(left_field_index);
            left_join_field_reverse_map[left_field_index] = true_index;


            // resolve name in right table
            auto resolved = right_scope->resolveFieldByClickhouse(key_name);

            if (!resolved)
                throw Exception("Can not find column " + key_name + " on right side", ErrorCodes::UNKNOWN_IDENTIFIER);

            right_join_fields.emplace_back(resolved->hierarchy_index);
            right_join_field_reverse_map[resolved->hierarchy_index] = true_index;

            auto right_type = resolved->getFieldDescription().type;
            DataTypePtr output_type = nullptr;

            if (left_type->equals(*right_type))
            {
                output_type = left_type;
            }
            else if (enable_implicit_type_conversion)
            {
                try
                {
                    output_type = getLeastSupertype({left_type, right_type}, allow_extended_conversion);
                }
                catch (DB::Exception & ex)
                {
                    throw Exception(
                        "Type mismatch of columns to JOIN by: " + left_type->getName() + " at left, " + right_type->getName() + " at right. "
                            + "Can't get supertype: " + ex.message(),
                        ErrorCodes::TYPE_MISMATCH);
                }
            }

            if (!output_type)
                throw Exception("Type mismatch for join keys: " + serializeAST(table_join), ErrorCodes::TYPE_MISMATCH);

            join_key_output_types.push_back(output_type);
            left_coercions.push_back(left_type->equals(*output_type) ? nullptr : output_type);
            right_coercions.push_back(right_type->equals(*output_type) ? nullptr : output_type);

            ++true_index;
        }

        /// Step 2. build output info
        // process left
        for (size_t i = 0; i < left_scope->size(); ++i)
        {
            const auto & input_field = left_scope->at(i);

            // TODO: one column used as multiple keys? e.g. SELECT a AS c, a AS d FROM (SELECT a, b FROM x) JOIN (SELECT c, d FROM y) USING (c, d)
            if (left_join_field_reverse_map.count(i))
            {
                output_fields.emplace_back(input_field.name, join_key_output_types[left_join_field_reverse_map[i]]);
            }
            else
            {
                output_fields.emplace_back(input_field);
            }
        }

        // process right
        auto select_query_ptr = select_query.ptr();
        RequiredSourceColumnsVisitor::Data columns_context;
        RequiredSourceColumnsVisitor(columns_context).visit(select_query_ptr);

        auto source_columns = collectNames(left_scope);
        auto required_columns = columns_context.requiredColumns();
        require_right_keys.resize(right_join_fields.size(), false);

        for (size_t i = 0; i < right_scope->size(); ++i)
        {
            const auto & input_field = right_scope->at(i);
            auto new_name = qualifyJoinedName(input_field.name, right_table_qualifier, source_columns);

            if (!right_join_field_reverse_map.count(i))
            {
                output_fields.emplace_back(input_field.withNewName(new_name));
            }
            else if (required_columns.count(new_name) && !source_columns.count(new_name))
            {
                auto join_key_index = right_join_field_reverse_map[i];
                output_fields.emplace_back(new_name, join_key_output_types[join_key_index]);
                require_right_keys[join_key_index] = true;
            }
        }
    }

    analysis.join_using_results[&table_join] = JoinUsingAnalysis {join_key_asts
                                                                 , left_join_fields
                                                                 , left_coercions
                                                                 , right_join_fields
                                                                 , right_coercions
                                                                 , left_join_field_reverse_map
                                                                 , right_join_field_reverse_map
                                                                 , require_right_keys};
    return createScope(output_fields);
}

ScopePtr QueryAnalyzerVisitor::analyzeJoinOn(ASTTableJoin & table_join, ScopePtr left_scope, ScopePtr right_scope, const String & right_table_qualifier)
{
    ScopePtr output_scope;
    {
        FieldDescriptions output_fields;

        if (use_ansi_semantic)
        {
            for (const auto & f: left_scope->getFields())
                output_fields.emplace_back(f);
            for (const auto & f: right_scope->getFields())
                output_fields.emplace_back(f);
        }
        else
        {
            for (const auto & f: left_scope->getFields())
                output_fields.emplace_back(f);

            auto source_names = collectNames(left_scope);

            for (const auto & f: right_scope->getFields())
            {
                auto new_name = qualifyJoinedName(f.name, right_table_qualifier, source_names);
                output_fields.emplace_back(f.withNewName(new_name));
            }
        }

        output_scope = createScope(output_fields);
    }

    if (table_join.on_expression)
    {
        // forbid arrayJoin in JOIN ON, see also the same check in CollectJoinOnKeysVisitor
        auto array_join_exprs = extractExpressions(context, analysis, table_join.on_expression, false,
                                                   [](const ASTPtr & node)
                                                   {
                                                       if (const auto * func = node->as<ASTFunction>())
                                                           if (func->name == "arrayJoin")
                                                               return true;

                                                       return false;
                                                   });
        if (!array_join_exprs.empty())
            throw Exception("Not allowed function in JOIN ON. Unexpected '" + queryToString(table_join.on_expression) + "'",
                            ErrorCodes::INVALID_JOIN_ON_EXPRESSION);

        /// We split ON expression into CNF factors, and classify each factor into 3 categories:
        /// - Join Equality Condition: expressions with structure `expression_depends_left_table_fields = expression_depends_right_table_fields`
        /// - Join Inequality Condition: expressions with structure `expression_depends_left_table_fields >/>=/</<= expression_depends_right_table_fields`
        /// - Complex Expression: expressions not belong to above 2 categories
        std::vector<JoinEqualityCondition> equality_conditions;
        std::vector<JoinInequalityCondition> inequality_conditions;
        std::vector<ASTPtr> complex_expressions;

        /// return  -1 - no dependencies
        ///          0 - dependencies come from both left side & right side
        ///          1 - dependencies only come from left side
        ///          2 - dependencies only come from right side
        auto choose_table_for_dependencies = [&](const std::vector<ASTPtr> & dependencies) -> int
        {
            int table = -1;

            for (const auto & dep : dependencies)
            {
                auto & resolved = analysis.column_references.at(dep);
                int table_for_this_dep = resolved.local_index < left_scope->size() ? 1 : 2;

                if (table < 0)
                    table = table_for_this_dep;
                else if (table != table_for_this_dep)
                    return 0;
            }

            return table;
        };

        for (auto & conjunct: expressionToCnf(table_join.on_expression))
        {
            ExprAnalyzer::analyze(conjunct, output_scope, context, analysis, "JOIN ON expression"s);

            bool is_join_expr = false;

            if (auto *func = conjunct->as<ASTFunction>(); func && isComparisonFunction(*func))
            {
                auto & left_arg = func->arguments->children[0];
                auto & right_arg = func->arguments->children[1];

                // extract column references in arguments, but exclude column references from outer scope
                auto left_dependencies = extractReferencesToScope(context, analysis, left_arg, output_scope);
                auto right_dependencies = extractReferencesToScope(context, analysis, right_arg, output_scope);

                size_t table_for_left = choose_table_for_dependencies(left_dependencies);
                size_t table_for_right = choose_table_for_dependencies(right_dependencies);

                using namespace ASOF;

                auto add_join_exprs = [&](const ASTPtr & left_ast, const ASTPtr & right_ast, Inequality inequality)
                {
                    DataTypePtr left_coercion = nullptr;
                    DataTypePtr right_coercion = nullptr;

                    // for non-ASOF join, inequality_conditions will be included in join filter, so don't have to do type coercion
                    if (func->name == "equals" || isAsofJoin(table_join))
                    {
                        DataTypePtr left_type = analysis.getExpressionType(left_ast);
                        DataTypePtr right_type = analysis.getExpressionType(right_ast);

                        if (!left_type->equals(*right_type))
                        {
                            DataTypePtr super_type = nullptr;

                            if (enable_implicit_type_conversion)
                            {
                                try
                                {
                                    super_type = getLeastSupertype({left_type, right_type}, allow_extended_conversion);
                                }
                                catch (DB::Exception & ex)
                                {
                                    throw Exception(
                                        "Type mismatch of columns to JOIN by: " + left_type->getName() + " at left, " + right_type->getName() + " at right. "
                                            + "Can't get supertype: " + ex.message(),
                                        ErrorCodes::TYPE_MISMATCH);
                                }
                                if (!left_type->equals(*super_type))
                                    left_coercion = super_type;
                                if (!right_type->equals(*super_type))
                                    right_coercion = super_type;
                            }

                            if (!super_type){
                                throw Exception("Type mismatch for join keys: " + serializeAST(table_join), ErrorCodes::TYPE_MISMATCH);
                            }
                        }
                    }

                    if (func->name == "equals")
                        equality_conditions.emplace_back(left_ast, right_ast, left_coercion, right_coercion);
                    else
                        inequality_conditions.emplace_back(left_ast, right_ast, inequality, left_coercion, right_coercion);

                    is_join_expr = true;
                };

                if (table_for_left == 1 && table_for_right == 2)
                    add_join_exprs(left_arg, right_arg, getInequality(func->name));
                else if (table_for_left == 2 && table_for_right == 1)
                    add_join_exprs(right_arg, left_arg, reverseInequality(getInequality(func->name)));
            }

            if (!is_join_expr)
                complex_expressions.push_back(conjunct);
        }

        analysis.join_on_results[&table_join] = JoinOnAnalysis {equality_conditions, inequality_conditions, complex_expressions};
    }

    return output_scope;
}

void QueryAnalyzerVisitor::analyzeWindow(ASTSelectQuery & select_query)
{
    if (!select_query.window())
        return;

    auto & window_list = select_query.window()->children;

    for (auto & window: window_list)
    {
        auto & window_elem = window->as<ASTWindowListElement &>();

        const auto & registered_windows = analysis.getRegisteredWindows(select_query);

        if (registered_windows.find(window_elem.name) != registered_windows.end())
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                            "Window '{}' is defined twice in the WINDOW clause",
                            window_elem.name);

        auto resolved_window = resolveWindow(window_elem.definition, registered_windows, context);
        analysis.setRegisteredWindow(select_query, window_elem.name, resolved_window);
    }
}

void QueryAnalyzerVisitor::analyzePrewhere(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    if (!select_query.prewhere())
        return;

    ExprAnalyzerOptions expr_options {"PREWHERE expression"};
    expr_options
        .selectQuery(select_query)
        .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED);
    ExprAnalyzer::analyze(select_query.prewhere(), source_scope, context, analysis, expr_options);
}

void QueryAnalyzerVisitor::analyzeWhere(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    if (!select_query.where())
        return;

    ExprAnalyzerOptions expr_options {"WHERE expression"};
    expr_options
        .selectQuery(select_query)
        .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED);
    auto filter_type = ExprAnalyzer::analyze(select_query.where(), source_scope, context, analysis, expr_options);

    if (auto inner_type = removeNullable(removeLowCardinality(filter_type)))
    {
        if (!inner_type->equals(DataTypeUInt8()) && !inner_type->equals(DataTypeNothing()))
            throw Exception("Illegal type " + filter_type->getName() + " for WHERE. Must be UInt8 or Nullable(UInt8).",
                            ErrorCodes::ILLEGAL_TYPE_OF_COLUMN_FOR_FILTER);
    }
}

ASTs QueryAnalyzerVisitor::analyzeSelect(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    if (!select_query.select() || select_query.refSelect()->children.empty())
        throw Exception("Empty select list found", ErrorCodes::LOGICAL_ERROR);

    ASTs select_expressions;
    FieldDescriptions output_description;
    ExprAnalyzerOptions expr_options {"SELECT expression"};

    expr_options
        .selectQuery(select_query)
        .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED)
        .aggregateSupport(ExprAnalyzerOptions::AggregateSupport::ALLOWED)
        .windowSupport(ExprAnalyzerOptions::WindowSupport::ALLOWED);

    auto add_select_expression = [&](const ASTPtr & expression)
    {
        auto expression_type = ExprAnalyzer::analyze(expression, source_scope, context, analysis, expr_options);

        auto get_output_name = [&](const ASTPtr & expr) -> String
        {
            // clickhouse semantic
            if (!use_ansi_semantic)
            {
                if (expr->as<ASTFieldReference>())
                {
                    auto col_ref = analysis.tryGetColumnReference(expr);
                    if (!col_ref)
                        throw Exception("invalid select expression", ErrorCodes::LOGICAL_ERROR);

                    return col_ref->getFieldDescription().name;
                }
                else
                    return expr->getAliasOrColumnName();
            }

            // AnsiSql semantic
            if (!expr->tryGetAlias().empty())
                return expr->tryGetAlias();
            else if (auto field_ref = analysis.tryGetColumnReference(expr))
                return field_ref->getFieldDescription().name;
            else
                return expr->getAliasOrColumnName();
        };

        auto output_name = get_output_name(expression);
        FieldDescription output_field {output_name, expression_type};

        if (auto source_field = analysis.tryGetColumnReference(expression))
        {
            output_field.copyOriginInfo(source_field->getFieldDescription());
        }

        select_expressions.push_back(expression);
        output_description.push_back(output_field);
    };

    for (auto & select_item: select_query.refSelect()->children)
    {
        if (select_item->as<ASTAsterisk>())
        {
            for (size_t field_index = 0; field_index < source_scope->size(); ++field_index)
            {
                if (source_scope->at(field_index).substituted_by_asterisk)
                {
                    auto field_reference = std::make_shared<ASTFieldReference>(field_index);
                    add_select_expression(field_reference);
                }
            }
        }
        else if (select_item->as<ASTQualifiedAsterisk>())
        {
            if (select_item->children.empty() && select_item->getChildren()[0]->as<ASTTableIdentifier>())
                throw Exception("Unable to resolve qualified asterisk", ErrorCodes::UNKNOWN_IDENTIFIER);

            ASTIdentifier& astidentifier = select_item->getChildren()[0]->as<ASTTableIdentifier&>();
            auto prefix =  QualifiedName::extractQualifiedName(astidentifier);
            bool matched = false;

            for (size_t field_index = 0; field_index < source_scope->size(); ++field_index)
            {
                if (source_scope->at(field_index).substituted_by_asterisk && source_scope->at(field_index).prefix.hasSuffix(prefix))
                {
                    matched = true;
                    auto field_reference = std::make_shared<ASTFieldReference>(field_index);
                    add_select_expression(field_reference);
                }
            }
            if(!matched)
                throw Exception("Can not find column of " + prefix.toString() + " in Scope", ErrorCodes::UNKNOWN_IDENTIFIER);
        }
        else if (auto * asterisk_pattern = select_item->as<ASTColumnsMatcher>())
        {
            for (size_t field_index = 0; field_index < source_scope->size(); ++field_index)
            {
                if (source_scope->at(field_index).substituted_by_asterisk && asterisk_pattern->isColumnMatching(source_scope->at(field_index).name))
                {
                    auto field_reference = std::make_shared<ASTFieldReference>(field_index);
                    add_select_expression(field_reference);
                }
            }
        }
        else
            add_select_expression(select_item);
    }

    analysis.select_expressions[&select_query] = select_expressions;
    analysis.setOutputDescription(select_query, output_description);
    return select_expressions;
}

void QueryAnalyzerVisitor::analyzeGroupBy(ASTSelectQuery & select_query, ASTs & select_expressions, ScopePtr source_scope)
{
    std::vector<ASTPtr> grouping_expressions;
    std::vector<std::vector<ASTPtr>> grouping_sets;

    if (select_query.groupBy())
    {
        bool allow_group_by_position = context->getSettingsRef().enable_replace_group_by_literal_to_symbol
            && !select_query.group_by_with_rollup && !select_query.group_by_with_cube && !select_query.group_by_with_grouping_sets;

        auto analyze_grouping_set = [&](ASTs & grouping_expr_list)
        {
            std::vector<ASTPtr> analyzed_grouping_set;

            for (ASTPtr grouping_expr: grouping_expr_list)
            {
                if (allow_group_by_position)
                    if (auto * literal = grouping_expr->as<ASTLiteral>();
                        literal &&
                        literal->tryGetAlias().empty() &&                 // avoid aliased expr being interpreted as positional argument
                                                                          // e.g. SELECT 1 AS a ORDER BY a
                        literal->value.getType() == Field::Types::UInt64)
                    {
                        auto index = literal->value.get<UInt64>();
                        if (index > select_expressions.size() || index < 1)
                        {
                            throw Exception("Group by index is greater than the number of select elements", ErrorCodes::BAD_ARGUMENTS);
                        }
                        grouping_expr = select_expressions[index - 1];
                    }

                // if grouping expr has been analyzed(i.e. it is from select expression), only check there is no agg/window/grouping
                if (analysis.hasExpressionType(grouping_expr))
                {
                    verifyNoAggregateWindowOrGroupingOperations(grouping_expr, "GROUP BY expression");
                }
                else
                {
                    ExprAnalyzer::analyze(grouping_expr, source_scope, context, analysis, "GROUP BY expression"s);
                }

                analyzed_grouping_set.push_back(grouping_expr);
            }

            grouping_sets.push_back(analyzed_grouping_set);
            grouping_expressions.insert(grouping_expressions.end(), analyzed_grouping_set.begin(), analyzed_grouping_set.end());
        };

        if (select_query.group_by_with_grouping_sets)
        {
            for (auto & grouping_set_element: select_query.groupBy()->children)
                analyze_grouping_set(grouping_set_element->children);
        }
        else
        {
            analyze_grouping_set(select_query.groupBy()->children);
        }
    }

    analysis.group_by_results[&select_query] = GroupByAnalysis {std::move(grouping_expressions), std::move(grouping_sets)};
}

/*
void QueryAnalyzerVisitor::analyzeInterestEvents(ASTSelectQuery & select_query)
{
    if (select_query.interestEvents())
    {
        std::vector<std::pair<String, UInt16>> interestevents_info_list;

        auto explicitAST = typeid_cast<const ASTInterestEvents *>(select_query.interestEvents().get());
        if (!explicitAST)
            return;

        auto event_list = typeid_cast<const ASTExpressionList *>(explicitAST->events.get());
        if (!event_list)
        {
            throw Exception(
                "INTEREST EVENTS should be list of string literal, but it is: " + serializeAST(*select_query.interestEvents()),
                ErrorCodes::BAD_ARGUMENTS);
        }

        UInt16 numEvents = interestevents_info_list.size();
        for (auto & child : event_list->children)
        {
            const ASTLiteral * lit = typeid_cast<ASTLiteral *>(child.get());
            if (!lit)
            {
                throw Exception(
                    "INTEREST EVENTS should be list of string literal, but it is: " + serializeAST(*select_query.interestEvents()),
                    ErrorCodes::BAD_ARGUMENTS);
            }

            String event = lit->value.safeGet<String>();

            interestevents_info_list.emplace_back(event, numEvents++);
        }

        if (interestevents_info_list.empty())
        {
            interestevents_info_list.emplace_back("empty", 1);
        }

        analysis.interest_events[&select_query] = std::move(interestevents_info_list);
    }
}
*/

void QueryAnalyzerVisitor::analyzeHaving(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    if (!select_query.having())
        return;

    ExprAnalyzerOptions expr_options {"HAVING clause"};
    expr_options
        .selectQuery(select_query)
        .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED)
        .aggregateSupport(ExprAnalyzerOptions::AggregateSupport::ALLOWED);
    ExprAnalyzer::analyze(select_query.having(), source_scope, context, analysis, expr_options);
}

ScopePtr QueryAnalyzerVisitor::buildOrderByScope(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    ScopePtr order_by_scope;

    if (use_ansi_semantic)
    {
        order_by_scope = createScope(analysis.getOutputDescription(select_query), source_scope);
    }
    else
    {
        order_by_scope = source_scope;
    }

    analysis.setScope(select_query, order_by_scope);
    return order_by_scope;
}

void QueryAnalyzerVisitor::analyzeOrderBy(ASTSelectQuery & select_query, ASTs & select_expressions, ScopePtr output_scope)
{
    if (select_query.orderBy())
    {
        std::vector<std::shared_ptr<ASTOrderByElement>> result;

        ExprAnalyzerOptions expr_options {"ORDER BY expression"};

        expr_options
            .selectQuery(select_query)
            .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED)
            .aggregateSupport(ExprAnalyzerOptions::AggregateSupport::ALLOWED)
            .windowSupport(ExprAnalyzerOptions::WindowSupport::ALLOWED);

        for (auto order_item: select_query.orderBy()->children)
        {
            auto & order_elem = order_item->as<ASTOrderByElement &>();

            if (context->getSettingsRef().enable_replace_order_by_literal_to_symbol)
                if (auto *literal = order_elem.children.front()->as<ASTLiteral>();
                    literal &&
                    literal->tryGetAlias().empty() &&                   // avoid aliased expr being interpreted as positional argument
                                                                        // e.g. SELECT 1 AS a ORDER BY a
                    literal->value.getType() == Field::Types::UInt64)
                {
                    auto index = literal->value.get<UInt64>();
                    if (index > select_expressions.size() || index < 1)
                    {
                        throw Exception("Order by index is greater than the number of select elements", ErrorCodes::BAD_ARGUMENTS);
                    }

                    order_item = order_item->clone();
                    order_item->children.clear();
                    // TODO: use ASTFieldReference to avoid unnecessary projection
                    order_item->children.push_back(select_expressions[index - 1]);
                }

            if (!analysis.hasExpressionType(order_item->children.front()))
                ExprAnalyzer::analyze(order_item, output_scope, context, analysis, expr_options);

            result.push_back(std::dynamic_pointer_cast<ASTOrderByElement>(order_item));
        }

        analysis.order_by_results[&select_query] = result;
    }
}

void QueryAnalyzerVisitor::analyzeLimitBy(ASTSelectQuery & select_query, ScopePtr output_scope)
{
    if (select_query.limitBy())
    {
        ExprAnalyzerOptions expr_options {"LIMIT BY expression"};

        expr_options
            .selectQuery(select_query)
            .subquerySupport(ExprAnalyzerOptions::SubquerySupport::CORRELATED)
            .aggregateSupport(ExprAnalyzerOptions::AggregateSupport::ALLOWED)
            .windowSupport(ExprAnalyzerOptions::WindowSupport::ALLOWED);

        for (auto & limit_item: select_query.limitBy()->children)
        {
            ExprAnalyzer::analyze(limit_item, output_scope, context, analysis, expr_options);
        }

        auto limit_by_value = analyzeUIntConstExpression(select_query.limitByLength());
        analysis.limit_by_values[&select_query] = limit_by_value;
    }
}

void QueryAnalyzerVisitor::analyzeLimitAndOffset(ASTSelectQuery & select_query)
{
    if (select_query.limit_with_ties)
        throw Exception("LIMIT/OFFSET FETCH WITH TIES not implemented", ErrorCodes::NOT_IMPLEMENTED);

    if (select_query.limitLength())
        analysis.limit_lengths[&select_query] = analyzeUIntConstExpression(select_query.limitLength());

    if (select_query.limitOffset())
        analysis.limit_offsets[&select_query] = analyzeUIntConstExpression(select_query.limitOffset());
}

ScopePtr QueryAnalyzerVisitor::createScope(FieldDescriptions field_descriptions, ScopePtr parent)
{
    bool query_boundary = false;

    if (!parent)
    {
        parent = outer_query_scope;
        query_boundary = true;
    }

    return analysis.scope_factory.createScope(Scope::ScopeType::RELATION, parent, query_boundary,
                                              std::move(field_descriptions));
}

DatabaseAndTableWithAlias QueryAnalyzerVisitor::extractTableWithAlias(const ASTTableExpression & table_expression)
{
    return DatabaseAndTableWithAlias{table_expression, context->getCurrentDatabase()};
}

namespace
{
    class VerifyNoAggregateWindowOrGroupingOperationsVisitor: public ExpressionVisitor<const Void>
    {
    public:
        VerifyNoAggregateWindowOrGroupingOperationsVisitor(String statement_name_, ContextPtr context_)
            : ExpressionVisitor(context_), statement_name(std::move(statement_name_))
        {}

    protected:
        void visitExpression(ASTPtr &, IAST &, const Void &) override {}

        void visitAggregateFunction(ASTPtr &, ASTFunction &, const Void &) override
        {
            throwException("Aggregate function", ErrorCodes::ILLEGAL_AGGREGATION);
        }

        void visitWindowFunction(ASTPtr &, ASTFunction &, const Void &) override
        {
            throwException("Window function", ErrorCodes::ILLEGAL_AGGREGATION);
        }

        void visitGroupingOperation(ASTPtr &, ASTFunction &, const Void &) override
        {
            throwException("Grouping operation", ErrorCodes::ILLEGAL_AGGREGATION);
        }

    private:
        String statement_name;

        [[ noreturn ]] void throwException(const String & expr_type, int code)
        {
            throw Exception(expr_type + " is not allowed in " + statement_name, code);
        }
    };
}

void QueryAnalyzerVisitor::verifyNoAggregateWindowOrGroupingOperations(ASTPtr & expression, const String & statement_name)
{
    VerifyNoAggregateWindowOrGroupingOperationsVisitor visitor {statement_name, context};
    traverseExpressionTree(expression, visitor, {}, analysis, context);
}

void QueryAnalyzerVisitor::verifyNoReferenceToOrderByScopeInsideAggregateOrWindow(ASTSelectQuery & select_query, ScopePtr order_by_scope)
{
    auto verifyNoReferenceToOrderByScope = [&](const ASTPtr & expr, const String & error_msg, int error_code)
    {
        auto violations = extractReferencesToScope(context, analysis, expr, order_by_scope);
        if (!violations.empty())
            throw Exception(error_msg + serializeAST(*violations[0]) + " must not have reference to query output columns", error_code);
    };

    for (const auto & aggregate: analysis.getAggregateAnalysis(select_query))
        verifyNoReferenceToOrderByScope(aggregate.expression, "Aggregate function ", ErrorCodes::ILLEGAL_AGGREGATION);

    for (const auto & window: analysis.getWindowAnalysisOfSelectQuery(select_query))
        verifyNoReferenceToOrderByScope(window->expression, "Window function ", ErrorCodes::ILLEGAL_AGGREGATION);
}

namespace
{
    class PostAggregateExpressionVisitor: public ExpressionVisitor<const Void>
    {
    protected:
        void visitExpression(ASTPtr &, IAST &, const Void &) override {}

        void visitIdentifier(ASTPtr &node, ASTIdentifier &, const Void &) override
        {
            verifyNoReferenceToNonGroupingKey(node);
        }

        void visitFieldReference(ASTPtr &node, ASTFieldReference &, const Void &) override
        {
            verifyNoReferenceToNonGroupingKey(node);
        }

        void visitScalarSubquery(ASTPtr &node, ASTSubquery &, const Void &) override
        {
            verifyNoReferenceToNonGroupingKeyInSubquery(node);
        }

        void visitInSubquery(ASTPtr &, ASTFunction & function, const Void &) override
        {
            verifyNoReferenceToNonGroupingKeyInSubquery(function.arguments->children[1]);
        }

        void visitQuantifiedComparisonSubquery(ASTPtr &, ASTQuantifiedComparison & ast, const Void &) override
        {
            verifyNoReferenceToNonGroupingKeyInSubquery(ast.children[1]);
        }

        void visitExistsSubquery(ASTPtr &node, ASTFunction &, const Void &) override
        {
            verifyNoReferenceToNonGroupingKeyInSubquery(node);
        }

    private:
        ContextPtr context;
        Analysis & analysis;
        ScopePtr source_scope;
        std::unordered_set<size_t> grouping_field_indices;

        void verifyNoReferenceToNonGroupingKey(ASTPtr & node);
        void verifyNoReferenceToNonGroupingKeyInSubquery(ASTPtr & node);

    public:
        PostAggregateExpressionVisitor(ContextPtr context_, Analysis & analysis_, ScopePtr source_scope_, std::unordered_set<size_t> grouping_field_indices_):
            ExpressionVisitor(context_), context(context_), analysis(analysis_), source_scope(source_scope_), grouping_field_indices(std::move(grouping_field_indices_))
        {}
    };

    void PostAggregateExpressionVisitor::verifyNoReferenceToNonGroupingKey(ASTPtr & node)
    {
        // cases when col_ref->scope != source_scope:
        // 1. outer query scope
        // 2. order by scope
        if (auto col_ref = analysis.tryGetColumnReference(node); col_ref && col_ref->scope == source_scope)
        {
            if (!grouping_field_indices.count(col_ref->local_index))
                throw Exception(serializeAST(*node) + " is not a grouping key", ErrorCodes::NOT_AN_AGGREGATE);
        }
    }

    void PostAggregateExpressionVisitor::verifyNoReferenceToNonGroupingKeyInSubquery(ASTPtr & node)
    {
        auto col_ref_exprs = extractReferencesToScope(context, analysis, node, source_scope, true);
        for (const auto & col_ref_expr: col_ref_exprs)
        {
            if (auto col_ref = analysis.tryGetColumnReference(col_ref_expr))
                if (!grouping_field_indices.count(col_ref->local_index))
                    throw Exception("Non grouping key used in subquery: " + serializeAST(*node), ErrorCodes::NOT_AN_AGGREGATE);
        }
    }

    class PostAggregateExpressionTraverser: public ExpressionTraversalVisitor<const Void>
    {
    protected:
        // don't go into aggregate function, since it will be evaluated during aggregate phase
        void visitAggregateFunction(ASTPtr &, ASTFunction &, const Void &) override {}

    public:
        void process(ASTPtr & node, const Void & traversal_context) override
        {
            // don't go into grouping expression
            if (grouping_expressions.count(node))
                return;

            ExpressionTraversalVisitor::process(node, traversal_context);
        }

        PostAggregateExpressionTraverser(ExpressionVisitor<const Void> & user_visitor_, const Void & user_context_, Analysis & analysis_,
                                         ContextPtr context_, ScopeAwaredASTSet grouping_expressions_):
            ExpressionTraversalVisitor(user_visitor_, user_context_, analysis_, context_), grouping_expressions(std::move(grouping_expressions_))
        {}

        using ExpressionTraversalVisitor::process;

    private:
        ScopeAwaredASTSet grouping_expressions;
    };

}

void QueryAnalyzerVisitor::verifyAggregate(ASTSelectQuery & select_query, ScopePtr source_scope)
{
    if (!analysis.needAggregate(select_query))
    {
        // verify no grouping operation exists if query won't be aggregated
        if (!analysis.getGroupingOperations(select_query).empty())
        {
            auto & representative = analysis.getGroupingOperations(select_query)[0];
            throw Exception("Invalid grouping operation: " + serializeAST(*representative), ErrorCodes::BAD_ARGUMENTS);
        }

        return;
    }

    ScopeAwaredASTSet grouping_expressions = createScopeAwaredASTSet(analysis);
    std::unordered_set<size_t> grouping_field_indices;
    auto & group_by_analysis = analysis.getGroupByAnalysis(select_query);

    for (const auto & group_by_key: group_by_analysis.grouping_expressions)
    {
        grouping_expressions.emplace(group_by_key);
        if (auto col_ref = analysis.tryGetColumnReference(group_by_key); col_ref && col_ref->scope == source_scope)
            grouping_field_indices.emplace(col_ref->local_index);
    }

    // verify argument of grouping operations are grouping keys
    if (auto & grouping_ops = analysis.getGroupingOperations(select_query); !grouping_ops.empty())
    {
        for (const auto & grouping_op: grouping_ops)
            for (const auto & grouping_op_arg: grouping_op->arguments->children)
                if (!grouping_expressions.count(grouping_op_arg))
                    throw Exception("Invalid grouping operation: " + serializeAST(*grouping_op), ErrorCodes::BAD_ARGUMENTS);
    }

    // verify no reference to non grouping fields after aggregate
    PostAggregateExpressionVisitor post_agg_visitor {context, analysis, source_scope, grouping_field_indices};
    PostAggregateExpressionTraverser post_agg_traverser {post_agg_visitor, {}, analysis, context, grouping_expressions};

    ASTs source_expressions;

    if (select_query.having())
        source_expressions.push_back(select_query.having());

    for (const auto & expr: analysis.getSelectExpressions(select_query))
        source_expressions.push_back(expr);

    for (const auto & expr: analysis.getOrderByAnalysis(select_query))
        source_expressions.push_back(expr);

    if (select_query.limitBy())
        source_expressions.push_back(select_query.limitBy());

    for (auto & expr: source_expressions)
        post_agg_traverser.process(expr);
}

namespace
{

    class FreeReferencesToLambdaArgumentVisitor: public ExpressionVisitor<const Void>
    {
    public:
        FreeReferencesToLambdaArgumentVisitor(String function_name_, int error_code_, ContextPtr context_, Analysis & analysis_)
            : ExpressionVisitor(context_), analysis(analysis_), function_name(std::move(function_name_)), error_code(error_code_)
        {}

    protected:
        void visitExpression(ASTPtr &, IAST &, const Void &) override {}

        void visitLambdaExpression(ASTPtr &, ASTFunction & ast, const Void &) override
        {
            lambda_scopes.emplace(analysis.getScope(ast));
        }

        void visitIdentifier(ASTPtr & node, ASTIdentifier &, const Void &) override
        {
            if (auto lambda_ref = analysis.tryGetLambdaArgumentReference(node); lambda_ref && !lambda_scopes.count(lambda_ref->scope))
                throw Exception("Free lambda argument reference found in "  + function_name + ": " + serializeAST(*node), error_code);
        }

        // no need to override visitFieldReference, as it can not refer to lambda argument
    private:
        Analysis & analysis;
        String function_name;
        int error_code;
        std::unordered_set<ScopePtr> lambda_scopes;
    };
}

void QueryAnalyzerVisitor::verifyNoFreeReferencesToLambdaArgument(ASTSelectQuery & select_query)
{
    FreeReferencesToLambdaArgumentVisitor aggregate_visitor {"aggregate function", ErrorCodes::UNKNOWN_IDENTIFIER, context, analysis};
    FreeReferencesToLambdaArgumentVisitor window_visitor {"window function", ErrorCodes::UNKNOWN_IDENTIFIER, context, analysis};

    for (auto & aggregate: analysis.getAggregateAnalysis(select_query))
    {
        ASTPtr expr = aggregate.expression;
        traverseExpressionTree(expr, aggregate_visitor, {}, analysis, context);
    }

    for (auto & window: analysis.getWindowAnalysisOfSelectQuery(select_query))
    {
        ASTPtr expr = window->expression;
        traverseExpressionTree(expr, window_visitor, {}, analysis, context);
    }
}


UInt64 QueryAnalyzerVisitor::analyzeUIntConstExpression(const ASTPtr & expression)
{
    auto val = tryEvaluateConstantExpression(expression, context);

    if (!val)
        throw Exception("Element of set in IN, VALUES or LIMIT is not a constant expression: " + serializeAST(*expression),
                  ErrorCodes::BAD_ARGUMENTS);

    auto uint_val = convertFieldToType(*val, DataTypeUInt64());

    return uint_val.safeGet<UInt64>();
}

NameSet collectNames(ScopePtr scope)
{
    NameSet result;

    for (const auto & field : scope->getFields())
        result.insert(field.name);

    return result;
}

String qualifyJoinedName(const String & name, const String & table_qualifier, const NameSet & source_names)
{
    if (source_names.count(name) || !isValidIdentifierBegin(name.at(0)))
        return table_qualifier + name;

    return name;
}

}
