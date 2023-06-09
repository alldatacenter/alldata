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

#include <Analyzers/QueryRewriter.h>

#include <Analyzers/ExecutePrewhereSubqueryVisitor.h>
#include <Analyzers/ImplementFunctionVisitor.h>
#include <Analyzers/ReplaceViewWithSubqueryVisitor.h>
#include <Analyzers/CheckAliasVisitor.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Interpreters/ApplyWithSubqueryVisitor.h>
#include <Interpreters/ApplyWithAliasVisitor.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Interpreters/QueryAliasesVisitor.h>
#include <Interpreters/QueryNormalizer.h>
#include <Interpreters/TranslateQualifiedNamesVisitor.h>
#include <Interpreters/CrossToInnerJoinVisitor.h>
#include <Interpreters/JoinToSubqueryTransformVisitor.h>
#include <Interpreters/LogicalExpressionsOptimizer.h>
#include <Interpreters/JoinedTables.h>
#include <Interpreters/TreeRewriter.h>
#include <Interpreters/TreeOptimizer.h>
#include <Interpreters/MarkTableIdentifiersVisitor.h>
#include <Interpreters/FunctionNameNormalizer.h>
#include <Interpreters/misc.h>
#include <Interpreters/CollectJoinOnKeysVisitor.h>
#include <Interpreters/getTableExpressions.h>
#include <Interpreters/TableJoin.h>
#include <Interpreters/NormalizeSelectWithUnionQueryVisitor.h>
#include <Interpreters/SelectIntersectExceptQueryVisitor.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/queryToString.h>

namespace DB
{

namespace
{
    thread_local int graphviz_index;

    template <char const * func_name>
    struct CustomizeFunctionsData
    {
        using TypeToVisit = ASTFunction;

        const String & customized_func_name;

        void visit(ASTFunction & func, ASTPtr &) const
        {
            if (Poco::toLower(func.name) == func_name)
            {
                func.name = customized_func_name;
            }
        }
    };

    char countdistinct[] = "countdistinct";
    using CustomizeCountDistinctVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<countdistinct>>, true>;

    char countifdistinct[] = "countifdistinct";
    using CustomizeCountIfDistinctVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<countifdistinct>>, true>;

    char in[] = "in";
    using CustomizeInVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<in>>, true>;

    char notIn[] = "notin";
    using CustomizeNotInVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<notIn>>, true>;

    char globalIn[] = "globalin";
    using CustomizeGlobalInVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<globalIn>>, true>;

    char globalNotIn[] = "globalnotin";
    using CustomizeGlobalNotInVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsData<globalNotIn>>, true>;

    template <char const * func_suffix>
    struct CustomizeFunctionsSuffixData
    {
        using TypeToVisit = ASTFunction;

        const String & customized_func_suffix;

        void visit(ASTFunction & func, ASTPtr &) const
        {
            if (endsWith(Poco::toLower(func.name), func_suffix))
            {
                size_t prefix_len = func.name.length() - strlen(func_suffix);
                func.name = func.name.substr(0, prefix_len) + customized_func_suffix;
            }
        }
    };

    /// Swap 'if' and 'distinct' suffixes to make execution more optimal.
    char ifDistinct[] = "ifdistinct";
    using CustomizeIfDistinctVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeFunctionsSuffixData<ifDistinct>>, true>;

    /// Used to rewrite all aggregate functions to add -OrNull suffix to them if setting `aggregate_functions_null_for_empty` is set.
    struct CustomizeAggregateFunctionsSuffixData
    {
        using TypeToVisit = ASTFunction;

        const String & customized_func_suffix;

        void visit(ASTFunction & func, ASTPtr &) const
        {
            const auto & instance = AggregateFunctionFactory::instance();
            if (instance.isAggregateFunctionName(func.name) && !endsWith(func.name, customized_func_suffix))
            {
                auto properties = instance.tryGetProperties(func.name);
                if (properties && !properties->returns_default_when_only_null)
                {
                    func.name += customized_func_suffix;
                }
            }
        }
    };

    // Used to rewrite aggregate functions with -OrNull suffix in some cases, such as sumIfOrNull, we should rewrite to sumOrNullIf
    struct CustomizeAggregateFunctionsMoveSuffixData
    {
        using TypeToVisit = ASTFunction;

        const String & customized_func_suffix;

        String moveSuffixAhead(const String & name) const
        {
            auto prefix = name.substr(0, name.size() - customized_func_suffix.size());

            auto prefix_size = prefix.size();

            if (endsWith(prefix, "MergeState"))
                return prefix.substr(0, prefix_size - 10) + customized_func_suffix + "MergeState";

            if (endsWith(prefix, "Merge"))
                return prefix.substr(0, prefix_size - 5) + customized_func_suffix + "Merge";

            if (endsWith(prefix, "State"))
                return prefix.substr(0, prefix_size - 5) + customized_func_suffix + "State";

            if (endsWith(prefix, "If"))
                return prefix.substr(0, prefix_size - 2) + customized_func_suffix + "If";

            return name;
        }

        void visit(ASTFunction & func, ASTPtr &) const
        {
            const auto & instance = AggregateFunctionFactory::instance();
            if (instance.isAggregateFunctionName(func.name))
            {
                if (endsWith(func.name, customized_func_suffix))
                {
                    auto properties = instance.tryGetProperties(func.name);
                    if (properties && !properties->returns_default_when_only_null)
                    {
                        func.name = moveSuffixAhead(func.name);
                    }
                }
            }
        }
    };

    using CustomizeAggregateFunctionsOrNullVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeAggregateFunctionsSuffixData>, true>;
    using CustomizeAggregateFunctionsMoveOrNullVisitor = InDepthNodeVisitor<OneTypeMatcher<CustomizeAggregateFunctionsMoveSuffixData>, true>;

    void expandCte(ASTPtr & query, ContextMutablePtr context)
    {
        ApplyWithSubqueryVisitor::visit(query);
        GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-expand-cte");
    }

    void checkAlias(ASTPtr & query)
    {
        CheckAliasVisitor().visit(query);
    }


    void expandView(ASTPtr & query, ContextMutablePtr context)
    {
        ReplaceViewWithSubquery data{context};
        ReplaceViewWithSubqueryVisitor(data).visit(query);
        GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-expand-view");
    }

    void normalizeUnion(ASTPtr & query, ContextMutablePtr context)
    {
        {
            SelectIntersectExceptQueryVisitor::Data data{context->getSettingsRef()};
            SelectIntersectExceptQueryVisitor{data}.visit(query);
        }

        {
            NormalizeSelectWithUnionQueryVisitor::Data data{context->getSettingsRef().union_default_mode};
            NormalizeSelectWithUnionQueryVisitor{data}.visit(query);
        }
    }

    void normalizeFunctions(ASTPtr & query, ContextMutablePtr context)
    {
        const auto & settings = context->getSettingsRef();

        CustomizeCountDistinctVisitor::Data data_count_distinct{settings.count_distinct_implementation};
        CustomizeCountDistinctVisitor(data_count_distinct).visit(query);

        CustomizeCountIfDistinctVisitor::Data data_count_if_distinct{settings.count_distinct_implementation.toString() + "If"};
        CustomizeCountIfDistinctVisitor(data_count_if_distinct).visit(query);

        CustomizeIfDistinctVisitor::Data data_distinct_if{"DistinctIf"};
        CustomizeIfDistinctVisitor(data_distinct_if).visit(query);

        if (settings.transform_null_in)
        {
            CustomizeInVisitor::Data data_null_in{"nullIn"};
            CustomizeInVisitor(data_null_in).visit(query);

            CustomizeNotInVisitor::Data data_not_null_in{"notNullIn"};
            CustomizeNotInVisitor(data_not_null_in).visit(query);

            CustomizeGlobalInVisitor::Data data_global_null_in{"globalNullIn"};
            CustomizeGlobalInVisitor(data_global_null_in).visit(query);

            CustomizeGlobalNotInVisitor::Data data_global_not_null_in{"globalNotNullIn"};
            CustomizeGlobalNotInVisitor(data_global_not_null_in).visit(query);
        }

        /// Rewrite all aggregate functions to add -OrNull suffix to them
        if (settings.aggregate_functions_null_for_empty)
        {
            CustomizeAggregateFunctionsOrNullVisitor::Data data_or_null{"OrNull"};
            CustomizeAggregateFunctionsOrNullVisitor(data_or_null).visit(query);
        }

        /// Move -OrNull suffix ahead, this should execute after add -OrNull suffix
        CustomizeAggregateFunctionsMoveOrNullVisitor::Data data_or_null{"OrNull"};
        CustomizeAggregateFunctionsMoveOrNullVisitor(data_or_null).visit(query);

        /// Rewrite function names to their canonical ones.
        if (settings.normalize_function_names)
            FunctionNameNormalizer().visit(query.get());

        GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-normal-functions");
    }

    void implementFunctions(ASTPtr & query, ContextMutablePtr context)
    {
        ImplementFunction data{context};
        ImplementFunctionVisitor(data).visit(query);
        GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-implement-functions");
    }

    struct MarkTableIdentifiersRecursively
    {
        using TypeToVisit = ASTSelectQuery;

        void visit(ASTSelectQuery &, ASTPtr & ast)
        {
            Aliases aliases;
            /// Mark table ASTIdentifiers with not a column marker
            MarkTableIdentifiersVisitor::Data identifiers_data{aliases};
            MarkTableIdentifiersVisitor(identifiers_data).visit(ast);
        }
    };

    using MarkTableIdentifiersRecursivelyMatcher = OneTypeMatcher<MarkTableIdentifiersRecursively>;
    using MarkTableIdentifiersRecursivelyVisitor = InDepthNodeVisitor<MarkTableIdentifiersRecursivelyMatcher, true>;

    // mark 2-nd argument of IN predicate as ASTTableIdentifier
    // mark 1-st argument of joinGet/dictGet as ASTTableIdentifier
    // this logic is included `normalizeNameAndAliases` for CLICKHOUSE-semantic rewriting
    void markTableIdentifiers(ASTPtr & query)
    {
        MarkTableIdentifiersRecursivelyVisitor::Data data;
        MarkTableIdentifiersRecursivelyVisitor(data).visit(query);
    }

    struct ReplaceTable
    {
        using TypeToVisit = ASTTableExpression;

        StorageID storage_id;

        void visit(ASTTableExpression & table_expr, ASTPtr &)
        {
            table_expr.children.clear();
            table_expr.database_and_table_name.reset();
            table_expr.subquery.reset();
            table_expr.table_function.reset();

            table_expr.database_and_table_name = std::make_shared<ASTTableIdentifier>(storage_id);
            table_expr.children.push_back(table_expr.database_and_table_name);
        }
    };

    using ReplaceTableMatcher = OneTypeMatcher<ReplaceTable>;
    using ReplaceTableVisitor = InDepthNodeVisitor<ReplaceTableMatcher, true>;

    struct RewriteInTableExpression
    {
        using TypeToVisit = ASTFunction;

        static ASTPtr makeSubqueryTemplate()
        {
            ParserSubquery parser(ParserSettings::CLICKHOUSE);
            ASTPtr subquery_template = parseQuery(parser, "(select * from t)", 0, DBMS_DEFAULT_MAX_PARSER_DEPTH);
            if (!subquery_template)
                throw Exception("Cannot parse subquery template", ErrorCodes::LOGICAL_ERROR);

            return subquery_template;
        }

        void visit(ASTFunction & func, ASTPtr &)
        {
            if (checkFunctionIsInOrGlobalInOperator(func))
            {
                auto & right_op = func.arguments->children[1];
                if (const auto * table_id = right_op->as<ASTTableIdentifier>())
                {
                    static ASTPtr subquery_template = makeSubqueryTemplate();
                    ASTPtr subquery = subquery_template->clone();
                    ReplaceTable data {table_id->getTableId()};
                    ReplaceTableVisitor(data).visit(subquery);
                    right_op = subquery;
                }
            }
        }
    };

    using RewriteInTableExpressionMatcher = OneTypeMatcher<RewriteInTableExpression>;
    using RewriteInTableExpressionVisitor = InDepthNodeVisitor<RewriteInTableExpressionMatcher, true>;

    // rewrite `expr IN table_name` to `expr IN (SELECT * FROM table_name)`
    void rewriteInTableExpression(ASTPtr & query)
    {
        RewriteInTableExpressionMatcher::Data data;
        RewriteInTableExpressionVisitor(data).visit(query);
    }

    void applyWithAlias(ASTPtr & query, ContextMutablePtr context)
    {
        if (context->getSettingsRef().enable_global_with_statement)
        {
            ApplyWithAliasVisitor().visit(query);
            GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-apply-with-alias");
        }
    }

    void normalizeNameAndAliases(ASTPtr & query, Aliases & aliases, const NameSet & source_columns_set, const Settings & settings,
                                 ContextMutablePtr context, ConstStoragePtr /*storage*/, const StorageMetadataPtr & metadata_snapshot)
    {
        /// Creates a dictionary `aliases`: alias -> ASTPtr
        QueryAliasesVisitor(aliases).visit(query);

        /// Mark table ASTIdentifiers with not a column marker
        MarkTableIdentifiersVisitor::Data identifiers_data{aliases};
        MarkTableIdentifiersVisitor(identifiers_data).visit(query);

        rewriteInTableExpression(query);

        /// Common subexpression elimination. Rewrite rules.
        QueryNormalizer::Data normalizer_data(aliases, source_columns_set, false, settings, true, context, nullptr, metadata_snapshot);
        QueryNormalizer(normalizer_data).visit(query);

        GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-normalize-name-and-alias");
    }

    void rewriteMultipleJoins(ASTPtr & query, const TablesWithColumns & tables, const String & database, const Settings & settings)
    {
        ASTSelectQuery & select = query->as<ASTSelectQuery &>();

        Aliases aliases;
        if (ASTPtr with = select.with())
            QueryAliasesNoSubqueriesVisitor(aliases).visit(with);
        QueryAliasesNoSubqueriesVisitor(aliases).visit(select.select());

        CrossToInnerJoinVisitor::Data cross_to_inner{tables, aliases, database};
        cross_to_inner.cross_to_inner_join_rewrite = false;
        CrossToInnerJoinVisitor(cross_to_inner).visit(query);

        JoinToSubqueryTransformVisitor::Data join_to_subs_data{tables, settings.dialect_type, aliases};
        JoinToSubqueryTransformVisitor(join_to_subs_data).visit(query);
    }

    void rewriteSelectQuery(ASTPtr & node, ContextMutablePtr context)
    {
        /// InterpreterSelectQuery logics:
        JoinedTables joined_tables(context, node->as<ASTSelectQuery &>(), false);
        joined_tables.resolveTables();

        // 1. Rewrite join
        if (joined_tables.resolveTables() && joined_tables.tablesCount() > 1)
        {
            rewriteMultipleJoins(node, joined_tables.tablesWithColumns(), context->getCurrentDatabase(), context->getSettingsRef());

            joined_tables.reset(node->as<ASTSelectQuery &>());
            joined_tables.resolveTables();

            GraphvizPrinter::printAST(node, context, std::to_string(graphviz_index++) + "-AST-multi-join-to-subquery");
        }

        /// TreeRewriter logics:
        auto & select_query = node->as<ASTSelectQuery &>();
        const auto & settings = context->getSettingsRef();

        TablesWithColumns tables_with_columns = joined_tables.tablesWithColumns();
        NamesAndTypesList source_columns;

        if (!tables_with_columns.empty())
        {
            source_columns = tables_with_columns.front().columns;
        }
        else
        {
            source_columns.emplace_back("dummy", std::make_shared<DataTypeUInt8>());
        }

        StoragePtr storage = joined_tables.getLeftTableStorage();
        TreeRewriterResult result {source_columns, storage, storage ? storage->getInMemoryMetadataPtr() : nullptr};

        if (tables_with_columns.size() > 1)
        {
            result.analyzed_join = std::make_shared<TableJoin>();
            const auto & right_table = tables_with_columns[1];
            NamesAndTypesList cols_from_joined = right_table.columns;
            /// query can use materialized or aliased columns from right joined table,
            /// we want to request it for right table
            cols_from_joined.insert(cols_from_joined.end(), right_table.hidden_columns.begin(), right_table.hidden_columns.end());
            result.analyzed_join->setColumnsFromJoinedTable(cols_from_joined);

            result.analyzed_join->deduplicateAndQualifyColumnNames(
                result.source_columns_set, right_table.table.getQualifiedNamePrefix());
        }

        // 2. Rewrite qualified names
        {
            TranslateQualifiedNamesVisitor::Data visitor_data(result.source_columns_set, tables_with_columns);
            TranslateQualifiedNamesVisitor visitor(visitor_data);
            visitor.visit(node);
        }

        // 3. Optimizes logical expressions.
        LogicalExpressionsOptimizer(&select_query, settings.optimize_min_equality_disjunction_chain_length.value).perform();

        // 4. Normalize
        {
            normalizeFunctions(node, context);

            NameSet all_source_columns_set = result.source_columns_set;
            if (result.analyzed_join)
            {
                for (const auto & col : result.analyzed_join->columnsFromJoinedTable())
                    all_source_columns_set.insert(col.name);
            }
            normalizeNameAndAliases(node, result.aliases, all_source_columns_set, settings, context, result.storage,
                                    result.metadata_snapshot);
        }

        // 5. Call `TreeOptimizer` since some optimizations will change the query result
        TreeOptimizer::apply(node, result, tables_with_columns, context, false);

        // 6. Check JOIN ON
        if (const auto * join_ast = select_query.join(); join_ast && tables_with_columns.size() >= 2)
        {
            auto & table_join = join_ast->table_join->as<ASTTableJoin &>();
            if (table_join.on_expression)
            {
                bool is_asof = (table_join.strictness == ASTTableJoin::Strictness::Asof);
                CollectJoinOnKeysVisitor::Data data{*result.analyzed_join, tables_with_columns[0], tables_with_columns[1], result.aliases, is_asof};
                CollectJoinOnKeysVisitor(data).visit(table_join.on_expression);
            }
        }

        // 7. Execute subquery in prewhere
        if (select_query.prewhere())
        {
            ExecutePrewhereSubquery execute_prewhere_subquery(context);
            ExecutePrewhereSubqueryVisitor(execute_prewhere_subquery).visit(select_query.refPrewhere());
        }
    }

    class MarkTupleLiteralsAsLegacyData
    {
    public:
        using TypeToVisit = ASTLiteral;

        static void visit(ASTLiteral & literal, ASTPtr &)
        {
            if (literal.value.getType() == Field::Types::Tuple)
                literal.use_legacy_column_name_of_tuple = true;
        }
    };

    using MarkTupleLiteralsAsLegacyMatcher = OneTypeMatcher<MarkTupleLiteralsAsLegacyData>;
    using MarkTupleLiteralsAsLegacyVisitor = InDepthNodeVisitor<MarkTupleLiteralsAsLegacyMatcher, true>;

    void markTupleLiteralsAsLegacy(ASTPtr & query, ContextMutablePtr context)
    {
        if (context->getSettingsRef().legacy_column_name_of_tuple_literal)
        {
            MarkTupleLiteralsAsLegacyVisitor::Data data;
            MarkTupleLiteralsAsLegacyVisitor(data).visit(query);
        }
    }
}

ASTPtr QueryRewriter::rewrite(ASTPtr query, ContextMutablePtr context, bool enable_materialized_view)
{
    (void) enable_materialized_view;
    graphviz_index = GraphvizPrinter::PRINT_AST_INDEX;
    GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-init");

    if (context->getSettingsRef().dialect_type == DialectType::ANSI)
    {
        /// Statement rewriting
        expandCte(query, context);
        expandView(query, context);
        normalizeUnion(query, context); // queries in union may not be normalized, hence normalize them here
        checkAlias(query);

        /// Expression rewriting
        markTupleLiteralsAsLegacy(query, context);
        markTableIdentifiers(query);
        rewriteInTableExpression(query);
        normalizeFunctions(query, context);
        implementFunctions(query, context);
    }
    else
    {
        applyWithAlias(query, context);
        expandCte(query, context);
        expandView(query, context);
        normalizeUnion(query, context);

        markTupleLiteralsAsLegacy(query, context);

        // select query level rewriter, top down rewrite each subquery.
        std::function<void(ASTPtr &)> rewrite_query = [&](ASTPtr & ast) {
            if (ast->as<ASTSelectQuery>())
            {
                rewriteSelectQuery(ast, context);
                GraphvizPrinter::printAST(ast, context, std::to_string(graphviz_index++) + "-AST");
            }

            // top down rewrite
            for (ASTPtr item : ast->children)
                rewrite_query(item);
        };

        rewrite_query(query);
    }

    GraphvizPrinter::printAST(query, context, std::to_string(graphviz_index++) + "-AST-done");

    return query;
}

}
