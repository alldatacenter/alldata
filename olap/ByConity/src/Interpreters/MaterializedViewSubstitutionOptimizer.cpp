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

#include <Interpreters/MaterializedViewSubstitutionOptimizer.h>
#include <Interpreters/IdentifierSemantic.h>
#include <Interpreters/SelectQueryOptions.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/predicateExpressionsUtils.h>
#include <Interpreters/PredicateImplicationChecker.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/queryToString.h>
#include <Storages/StorageMaterializedView.h>
#include <Common/TypePromotion.h>
#include <DataTypes/MapHelpers.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <boost/rational.hpp>
#include <Parsers/ASTSampleRatio.h>
#include <Common/SettingsChanges.h>
#include <QueryPlan/QueryPlan.h>
#include <Common/JSONBuilder.h>

#include <map>
#include <unordered_set>
#include <memory>
#include <queue>
#include <limits>


#if !defined(__GLIBCXX_BITSIZE_INT_N_0) && defined(__SIZEOF_INT128__)
#define GCC_VERSION (__GNUC__ * 10000 + __GNUC_MINOR__ * 100 + __GNUC_PATCHLEVEL__)
#if (GCC_VERSION <= 100200) // gcc 10.2.0
namespace std
{
template <>
struct numeric_limits<__uint128_t>
{
    static constexpr bool is_specialized = true;
    static constexpr bool is_signed = false;
    static constexpr bool is_integer = true;
    static constexpr int radix = 2;
    static constexpr int digits = 128;
    static constexpr __uint128_t min () { return 0; } // used in boost 1.65.1+
    static constexpr __uint128_t max () { return __uint128_t(0) - 1; } // used in boost 1.68.0+
};
}
#endif
#undef GCC_VERSION
#endif

namespace Poco
{
class Logger;
}


namespace DB
{
    static const int MAX = std::numeric_limits<int>::max();
    static const std::unordered_set<String> MERGE_TREES{"MergeTree", "ReplicatedMergeTree"};
    static const std::unordered_set<String> AGGREGATING_MERGE_TREES{"AggregatingMergeTree",
                                                                    "ReplicatedAggregatingMergeTree",
                                                                    "HaAggregatingMergeTree"};
    /// TODO: complete this indeterministic functions set
    static const std::unordered_set<String> INDETERMINISTIC_FUNCS{"rand", "rand64", "randConstant", "now", "hostName", "host"};

    static const std::unordered_set<String> QUANTILE_FUNCS{"quantile", "quantileDeterministic", "quantileExact",
                                                           "quantileExactWeighted", "quantileTiming",
                                                           "quantileTimingWeighted", "quantileTDigest",
                                                           "quantileTDigestWeighted"};

    static const std::unordered_set<String> QUANTILE_STATE_FUNCS{"quantileState", "quantileDeterministicState",
                                                                 "quantileExactState", "quantileExactWeightedState",
                                                                 "quantileTimingState", "quantileTimingWeightedState",
                                                                 "quantileTDigestState","quantileTDigestWeightedState"};

    namespace ErrorCodes
    {
        extern const int UNKNOWN_IDENTIFIER;
        extern const int ILLEGAL_AGGREGATION;
    }

    MaterializedViewSubstitutionOptimizer::MaterializedViewSubstitutionOptimizer(ContextPtr context_, const SelectQueryOptions & options_)
            : context(Context::createCopy(context_)), options(options_),
              log(&Poco::Logger::get("MaterializedViewSubstitutionOptimizer")) {}

    std::pair<ASTPtr, String> validateAndExtractDbTable(const ASTSelectQuery * select_query)
    {
        if (!select_query->tables() || !select_query->select() || select_query->with())
            return {nullptr, "query -" + queryToString(*select_query) + " ,validation failure : lack of table or select elements. not support with"};

        if (select_query->group_by_with_totals || select_query->group_by_with_rollup || select_query->group_by_with_cube)
            return {nullptr, "query -" + queryToString(*select_query) + ", validation failure : not support totals , rollup and cube" };

        if (select_query->tables()->children.size() == 1)
        {
            auto & tables_in_select_query = select_query->tables()->as<ASTTablesInSelectQuery &>();

            /// table join and array join are not support
            if (tables_in_select_query.children.size() != 1)
                return {nullptr, "query -" + queryToString(*select_query) + ", validation failure: not support join and array join"};
            auto & tables_element = tables_in_select_query.children[0]->as<ASTTablesInSelectQueryElement &>();
            if (tables_element.table_expression)
            {
                auto & table_expression = tables_element.table_expression->as<ASTTableExpression &>();
                if (table_expression.sample_size)
                {
                    boost::rational<ASTSampleRatio::BigNum> relative_sample_size = 0;
                    relative_sample_size.assign(
                        table_expression.sample_size->as<ASTSampleRatio &>().ratio.numerator,
                        table_expression.sample_size->as<ASTSampleRatio &>().ratio.denominator);

                    boost::rational<ASTSampleRatio::BigNum> relative_sample_offset = 0;
                    if (table_expression.sample_offset)
                        relative_sample_offset.assign(
                            table_expression.sample_offset->as<ASTSampleRatio &>().ratio.numerator,
                            table_expression.sample_offset->as<ASTSampleRatio &>().ratio.denominator);

                    if (relative_sample_offset < 0 || relative_sample_size > 1 || relative_sample_offset > 1)
                        return {nullptr, "query -" + queryToString(*select_query) + ", validation failure: not support sample size"};

                    if (relative_sample_size != boost::rational<ASTSampleRatio::BigNum>(1))
                        return {nullptr, "query -" + queryToString(*select_query) + ", validation failure: not support sample size"};
                }

                /// A select with no nested subquery or table function is support
                if (table_expression.database_and_table_name)
                {
                    return {table_expression.database_and_table_name, ""} ;
                }
            }
        }

        return {nullptr, "query -" + queryToString(*select_query) + " ,validation failure : not support multiple table elements"};
    }

    MaterializedViewOptimizerResultPtr MaterializedViewSubstitutionOptimizer::optimize(SelectQueryInfo & query_info)
    {
        /**
         * 1. No need to rewrite when only analyze.
         * 2. If inputs have been specified, we can't rewrite the query.
         * 3. Tea limit does not support to rewrite.
         * 4. A inner query (query that inside a CREATE SELECT or INSERT SELECT) also does not support,
         *    since it would cause unexpected effect.
         * 5. We won't rewrite if we have not enable it.
         */

        if (!query_info.query || options.only_analyze || !context->getSettingsRef().enable_view_based_query_rewrite)
            return std::make_shared<const MaterializedViewOptimizerResult>("validation failure: query is empty , or only for analyse,"
                                                                           " the query has specified input streams or storage, or has tea limit , "
                                                                           "or has inner query or enable_view_based_query_rewrite is disabled");

        if (auto * target_query = query_info.query->as<ASTSelectQuery>())
        {
            auto ast_db_tbl = validateAndExtractDbTable(target_query);
            if (ast_db_tbl.first)
            {
                auto target_table_id = context->resolveStorageID(ast_db_tbl.first);
                Dependencies dependencies = DatabaseCatalog::instance().getDependencies(target_table_id);
                if (!dependencies.empty())
                {
                    auto target_table = DatabaseCatalog::instance().getTable(target_table_id, context);
                    return optimizeImpl(target_query, dependencies, target_table, query_info);
                }
                else
                    return std::make_shared<const MaterializedViewOptimizerResult>(
                        "validation failure: query-" + queryToString(*query_info.query) +
                        ", database-" + target_table_id.getDatabaseName() + ", table-" + target_table_id.getTableName() + " have on dependency materialized views");
            }
            else
                return std::make_shared<const MaterializedViewOptimizerResult>(ast_db_tbl.second);
        }

        return std::make_shared<const MaterializedViewOptimizerResult>("validation failure: query-" + queryToString(*query_info.query) + " is not select query");
    }

    /// Get info about whether there are identifier, nested aggregate functions and indeterministic functions inside current function,
    /// we need these info when rewrite.
    void extractFunctionInfo(ASTFunction * function, bool & exist_identifier, bool & exist_nested_aggregate_func,
                             bool & exist_nested_indeterministic_func)
    {
        for (auto & child : function->arguments->children)
        {
            if (child->as<ASTIdentifier>())
            {
                exist_identifier = true;
            }
            else if (auto * internal_function = child->as<ASTFunction>())
            {
                if (AggregateFunctionFactory::instance().isAggregateFunctionName(internal_function->name))
                    exist_nested_aggregate_func = true;

                if (INDETERMINISTIC_FUNCS.count(internal_function->name))
                    exist_nested_indeterministic_func = true;

                extractFunctionInfo(internal_function, exist_identifier, exist_nested_aggregate_func, exist_nested_indeterministic_func);
            }
        }
    }


    /**
     * Match and rewrite column. The column have to appear in view select expression list
     *
     * @param column The column that going to be rewrited
     * @param result The match result
     * @param view_query_info The view query info
     * @param select_col Indicate whether the column is from select expression list
     * @param group_col Indicate whether the column is from group expression list
     * @param recursion_level The recursion level
     */
    void matchAndRewriteColumn(ASTPtr & column, MatchResult & result, ViewQueryInfo & view_query_info, bool select_col,
                        bool group_col, int recursion_level = 0)
    {
        if (result.cost == MAX)
            return;

        String name_before_rewrite = column->getColumnName();
        String alias_before_rewrite = column->tryGetAlias();

        if (column->as<ASTLiteral>())
        {
            /// No need to rewrite a literal, just add recursion_level to the cost
            result.cost += recursion_level;
            return;
        }
        else if (auto * identifier = column->as<ASTIdentifier>())
        {
            auto select_col_match = view_query_info.select_ids.find(identifier->name());
            if (select_col_match != view_query_info.select_ids.end())
            {
                auto alias = select_col_match->second->tryGetAlias();
                auto new_name = alias.empty() ? select_col_match->second->name() : alias;
                identifier->setShortName(new_name);

               // after rewrite a implicit map column, we should reset the flag.
               if (!isMapImplicitKey(identifier->name()))
                   identifier->is_implicit_map_key = false;

                result.cost += recursion_level;
            }
            else
            {
                String select_id_str;
                for (const auto & select_id : view_query_info.select_ids)
                    select_id_str.append(select_id.first).append(",");
                result.view_match_info = "matchAndRewriteColumn - Match failed due to column as ASTIdentifier-" +
                        queryToString(*identifier) + " not find in view select ids-" + select_id_str;
                result.cost = MAX;
                LOG_DEBUG(&Poco::Logger::get("matchAndRewriteColumn"), result.view_match_info);
            }
        }
        else if (auto * function = column->as<ASTFunction>())
        {
            bool exist_identifier = false;
            bool exist_nested_aggregate_func = false;
            bool exist_nested_indeterministic_func = false;
            extractFunctionInfo(function, exist_identifier, exist_nested_aggregate_func, exist_nested_indeterministic_func);

            if (AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))
            {
                if (group_col)
                    throw Exception("Unknown identifier (in GROUP BY): " + function->getColumnName(), ErrorCodes::UNKNOWN_IDENTIFIER);

                if (exist_nested_aggregate_func)
                    throw Exception("Nested aggregate function is found inside current aggregate function " +
                                    function->getColumnName() + " in query.",
                                    ErrorCodes::ILLEGAL_AGGREGATION);

                if (exist_nested_indeterministic_func)
                {
                    result.view_match_info = "matchAndRewriteColumn - Match failed due to function name- " + function->name + ", AST-" +
                            queryToString(*function) + " exist nested indeterministic func.";
                    result.cost = MAX;
                    LOG_DEBUG(&Poco::Logger::get("matchAndRewriteColumn"), result.view_match_info);
                    return;
                }

                /// If it is a quantile function, we set the parameters to null, then it won't affect the matching.
                /// Example: quantile(0.8)(x) can match to quantileState(0.1)(x) and can be rewrited to quantileMerge(0.8)(`quantileState(0.1)(x)`).
                const auto parameters = function->parameters;
                if (QUANTILE_FUNCS.count(function->name))
                    function->parameters = nullptr;

                /// Find whether there is a stateful aggregate function (with the `-State` suffix) can match
                auto func_name = function->name;
                function->name = func_name + "State";
                auto column_name = function->getColumnName();
                auto select_col_match = view_query_info.select_aggregate_funcs.find(column_name);
                if (select_col_match != view_query_info.select_aggregate_funcs.end())
                {
                    auto alias = select_col_match->second->tryGetAlias();
                    auto new_name = alias.empty() ? select_col_match->second->getColumnName() : alias;
                    function->name = func_name + "Merge";

                    function->arguments->children.clear();
                    function->arguments->children.push_back(std::make_shared<ASTIdentifier>(new_name));

                    // restore the parameters
                    function->parameters = parameters;

                    result.cost += recursion_level;
                }
                /// If we haven't found a stateful aggregate function to match the column,
                /// then if there is no group expression in the view query and also no aggregate functions in
                /// view query's select expression, we still have the possibility to rewrite the column since data in
                /// the view are detail data not aggregate data. We can rewrite it as long as the arguments of
                /// this function appear in view select expression list.
                else if (!view_query_info.query.groupBy() && view_query_info.select_aggregate_funcs.empty())
                {
                    function->name = func_name; // reset the function name
                    for (auto & child : function->arguments->children)
                    {
                        matchAndRewriteColumn(child, result, view_query_info, select_col, group_col, recursion_level + 1);
                    }
                }
                else
                {
                    String select_function_str;
                    for (const auto & select_function : view_query_info.select_aggregate_funcs)
                        select_function_str.append(select_function.first).append(",");
                    result.view_match_info = "matchAndRewriteColumn - Match failed due to column name- " + column_name +
                            " can not find in view functions-" + select_function_str;
                    result.cost = MAX;
                    LOG_DEBUG(&Poco::Logger::get("matchAndRewriteColumn"), result.view_match_info);
                }
            }
            else if (INDETERMINISTIC_FUNCS.count(function->name))
            {
                /// For indeterministic functions, if there are identifiers inside it's arguments, then we can terminate the
                /// whole rewriting. Otherwise, just keep it and add recursion_level to the cost.
                if (exist_identifier)
                {
                    result.view_match_info = "matchAndRewriteColumn" + function->name + " is indeterministic function";
                    result.cost = MAX;
                    LOG_DEBUG(&Poco::Logger::get("matchAndRewriteColumn"), result.view_match_info);
                }
                else
                    result.cost += recursion_level;
            }
            else
            {
                /// rewrite function that is not an aggregate/indeterministic function
                auto column_name = function->getColumnName();
                auto select_col_match = view_query_info.select_funcs.find(column_name);
                LOG_DEBUG(&Poco::Logger::get("matchAndRewriteColumn"), "normal function name-{} is match view-{}", column_name, (select_col_match == view_query_info.select_funcs.end()));
                if (select_col_match != view_query_info.select_funcs.end() && !exist_nested_aggregate_func && !exist_nested_indeterministic_func)
                {
                    auto alias = select_col_match->second->tryGetAlias();
                    auto new_name = alias.empty() ? column_name : alias;
                    column = std::make_shared<ASTIdentifier>(new_name);

                    result.cost += recursion_level;
                }
                else
                {
                    for (auto & child : function->arguments->children)
                    {
                        matchAndRewriteColumn(child, result, view_query_info, select_col, group_col, recursion_level + 1);
                    }
                }
            }
        }
        else
            result.cost = MAX;

        if (result.cost == MAX)
            return;

        if (select_col && recursion_level == 0)
        {
            /// After rewrite column in select expression, we should set it's alias to original column's alias or column name
            /// then the required result columns' names will keep consistent after rewriting.
            column->setAlias(alias_before_rewrite.empty() ? name_before_rewrite : alias_before_rewrite);
        }

        /// Collect column name substitution info, which would be used in `ColumnNameSubstitutionInputStream`.
        result.name_substitution_info[column->getColumnName()] = name_before_rewrite;
    }

    /// Match and rewrite select expression list
    void matchSelect(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchSelect"), "Start match select...");
        if (result.cost == MAX)
            return;

        auto & target_select_expression_list = result.rewriting_target_query.select();
        for (auto & child : target_select_expression_list->children)
        {
            matchAndRewriteColumn(child, result, view_query_info, true, false);
            if (result.cost == MAX)
            {
                LOG_DEBUG(&Poco::Logger::get("matchSelect"), "Match failed due to column-{} rewrite match failed", queryToString(child) );
                return;
            }
        }
    }

    /// Match and rewrite group expression list
    void matchGroup(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchGroup"), "Start match group...");
        if (result.cost == MAX)
            return;

        const auto & target_group_expression_list = result.rewriting_target_query.groupBy();
        const auto & view_group_expression_list = view_query_info.query.groupBy();

        if (!target_group_expression_list && !view_group_expression_list)
        {
            /// do nothing
        }
        else if (!target_group_expression_list && view_group_expression_list)
        {
            for (auto & child : result.rewriting_target_query.select()->children)
            {
                auto * literal = child->as<ASTLiteral>();
                auto * function = child->as<ASTFunction>();

                if (literal || (function && AggregateFunctionFactory::instance().isAggregateFunctionName(function->name)))
                {
                    /// If there is no group expression in target query, but there is group expression in view query,
                    /// then if the columns in target select expression are literals or aggregate functions,
                    /// we still have the possibility to rewrite it.
                    /// The example:
                    ///     Target query: SELECT count(), sum(a) FROM t WHERE a > 1
                    ///     Materialized view definition: SELECT a, b, countState() as c, sumState(a) as s FROM t WHERE a > 0 group by a, b
                    ///     Rewrite result: SELECT countMerge(c), sumMerge(s) FROM mv WHERE a > 1
                }
                else
                {
                    result.cost = MAX;
                    return;
                }
            }
        }
        else
        {
            for (auto & child : target_group_expression_list->children)
            {
                matchAndRewriteColumn(child, result, view_query_info, false, true);
                if (result.cost == MAX)
                    return;
            }

            auto target_group_size = target_group_expression_list->children.size();
            int group_dist = int(
                    view_group_expression_list ? view_group_expression_list->children.size() - target_group_size
                                               : target_group_size);
            result.cost += group_dist * 3;
        }
    }

    /// Match and rewrite order expression list.
    void matchOrder(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchOrder"), "Start match order...");
        const auto & target_order_expression_list = result.rewriting_target_query.orderBy();
        if (!target_order_expression_list || result.cost == MAX)
            return;

        std::queue<std::pair<ASTPtr, ASTs::size_type>> ast_queue;
        for (ASTs::size_type i = 0; i < target_order_expression_list->children.size(); ++i)
        {
            ast_queue.push({target_order_expression_list, i});
        }

        while (!ast_queue.empty())
        {
            auto & pair = ast_queue.front();
            auto & node = pair.first->children[pair.second];
            auto * identifier = node->as<ASTIdentifier>();
            auto * function = node->as<ASTFunction>();

            if (identifier || (function && function->name != "tuple"))
            {
                matchAndRewriteColumn(node, result, view_query_info, false, false);
                if (result.cost == MAX)
                    return;
            }
            else
            {
                for (ASTs::size_type i = 0; i < node->children.size(); ++i)
                {
                    ast_queue.push({node, i});
                }
            }

            ast_queue.pop();
        }
    }

    /// Match and rewrite limit by expression list.
    void matchLimit(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchLimit"), "Start match limit...");
        const auto & target_limit_by_expression_list = result.rewriting_target_query.limitBy();
        if (!target_limit_by_expression_list || result.cost == MAX)
            return;

        for (auto & child : target_limit_by_expression_list->children)
        {
            matchAndRewriteColumn(child, result, view_query_info, false, false);
            if (result.cost == MAX)
                return;
        }
    }

    ASTPtr composeExpression(const ASTPtr & expression1, const ASTPtr & expression2)
    {
        if (!expression1 && !expression2)
            return nullptr;
        else if (!expression1 && expression2)
            return expression2;
        else if (expression1 && !expression2)
            return expression1;
        else
            return composeAnd(ASTs{expression1, expression2});
    }

    /// Match and rewrite target predicate expression.
    /// Any column in predicate expression have to appear in view select expression
    void matchAndRewritePredicateExpression(const ASTPtr & target_predicate_expression, MatchResult & result,
                                            ViewQueryInfo & view_query_info)
    {
        if (result.cost == MAX)
            return;

        std::queue<ASTPtr> ast_queue;
        ast_queue.push(target_predicate_expression);
        while (!ast_queue.empty())
        {
            auto & node = ast_queue.front();
            auto * identifier = node->as<ASTIdentifier>();
            auto * function = node->as<ASTFunction>();

            if (identifier)
            {
                matchAndRewriteColumn(node, result, view_query_info, false, false);
                if (result.cost == MAX)
                    return;
            }
            else if (function && (isComparisonFunctionName(function->name) || isInFunctionName(function->name) || isLikeFunctionName(function->name)))
            {
                auto & expr_lhs = function->arguments->children.at(0);
                matchAndRewriteColumn(expr_lhs, result, view_query_info, false, false);
                if (result.cost == MAX)
                    return;
            }
            else
            {
                for (auto & child : node->children)
                {
                    ast_queue.push(child);
                }
            }

            ast_queue.pop();
        }
    }

    /// Match and rewrite having expression.
    void matchHaving(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchHaving"), "Start match having...");
        const auto & target_having_expression = result.rewriting_target_query.having();
        if (!target_having_expression || result.cost == MAX)
            return;

        matchAndRewritePredicateExpression(target_having_expression, result, view_query_info);
    }

    /**
     * We first compose prewhere and where into an AND expression, then match between target and view.
     * Finally, rewrite the target.
     * Notes: prewhere and implicitwhere expression would be merged into where expression when rewrite. i.e. only where_expression will be
     *        kept, prewhere_expression and implicitwhere_expression will be set to nullptr.
     */
    void matchWhere(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchWhere"), "Start match where...");
        if (result.cost == MAX)
            return;

        auto target_expression = composeExpression(result.rewriting_target_query.prewhere(),
                                                                     result.rewriting_target_query.where());
        if (target_expression)
            LOG_DEBUG(&Poco::Logger::get("matchWhere"), "target where expression-{}", queryToString(target_expression));
        auto view_expression = composeExpression(view_query_info.query.prewhere(), view_query_info.query.where());

        if (view_expression)
            LOG_DEBUG(&Poco::Logger::get("matchWhere"), "view where expression-{}", queryToString(view_expression));

        if (!target_expression && !view_expression)
        {
            /// do nothing
        }
        else if (!target_expression && view_expression)
        {
            result.view_match_info = "matchWhere - target where expression is not exist " + queryToString(view_expression) + " exist match failed";
            result.cost = MAX;
            LOG_DEBUG(&Poco::Logger::get("matchWhere"), result.view_match_info);
        }
        else if (target_expression && !view_expression)
        {
            matchAndRewritePredicateExpression(target_expression, result, view_query_info);
            if (result.cost == MAX)
            {
                result.view_match_info = "matchWhere - target where expression-" + queryToString(target_expression) + " without view where expression match failed";
                LOG_DEBUG(&Poco::Logger::get("matchWhere"), result.view_match_info);
                return;
            }

            int predicate_dist = int(decomposeOr(toDNF(target_expression)).size());
            result.cost += predicate_dist * 5;
            result.rewriting_target_query.setExpression(ASTSelectQuery::Expression::PREWHERE, nullptr);
            result.rewriting_target_query.setExpression(ASTSelectQuery::Expression::WHERE, std::move(target_expression));
        }
        else
        {
            /// In order to match between target predicate and view predicate,
            /// we should check if target implies view.
            PredicateImplicationChecker checker(target_expression, view_expression, true);
            if (!checker.implies())
            {
                result.view_match_info = "matchWhere - PredicateImplicationChecker implies failed";
                LOG_DEBUG(&Poco::Logger::get("matchWhere"), result.view_match_info);
                result.cost = MAX;
                return;
            }

            auto target_predicates = checker.getFirstPredicates();
            if (target_predicates.size() <= 1)
            {
                /// This means we may have removed duplicate predicates (see the implementation of PredicateImplicationChecker).
                /// Using the target_predicates to reset the target_expression, then rewrite the target_expression.
                target_expression = composeOr(target_predicates);
            }

            if (target_expression)
            {
                LOG_DEBUG(&Poco::Logger::get("matchWhere"),  "target expression-{}",  queryToString(target_expression));
                matchAndRewritePredicateExpression(target_expression, result, view_query_info);
            }

            if (result.cost == MAX)
                return;

            result.rewriting_target_query.setExpression(ASTSelectQuery::Expression::PREWHERE, nullptr);
            result.rewriting_target_query.setExpression(ASTSelectQuery::Expression::WHERE, std::move(target_expression));
        }
    }

    void matchDistinct(MatchResult & result, ViewQueryInfo & view_query_info)
    {
        LOG_DEBUG(&Poco::Logger::get("matchDistinct"), "Start match distinct...");
        if (result.cost == MAX)
            return;

        auto target_distinct = result.rewriting_target_query.distinct;
        auto view_distinct = view_query_info.query.distinct;

        if (target_distinct == view_distinct)
        {
            /// do nothing
        }
        else if (target_distinct && !view_distinct)
        {
            /// do nothing
        }
        else
        {
            result.view_match_info = "matchDistinct - Match failed due to view query and target query distinct mismatch. target distinct-" +
                    std::to_string(target_distinct) + ", view distinct-" + std::to_string(view_distinct);
            result.cost = MAX;
            LOG_DEBUG(&Poco::Logger::get("matchDistinct"), result.view_match_info);
        }
    }

    /// Return whether the first is a subset of the second
    bool subset(const std::unordered_set<String> & first, const std::unordered_set<String> & second)
    {
        if (first.empty() && !second.empty())
            return false;

        for (const auto & element : first)
        {
            if (!second.count(element))
                return false;
        }

        return true;
    }

    void checkPartitions(MatchResult & result, const StoragePtr & target_table, SelectQueryInfo & query_info, ContextMutablePtr context)
    {
        LOG_DEBUG(&Poco::Logger::get("checkPartitions"), "Start check partitions...");
        if (result.cost == MAX)
            return;

        auto target_table_data = dynamic_cast<MergeTreeData *>(target_table.get());
        if (!target_table_data)
        {
            result.view_match_info = "checkPartitions - target table is not merge tree data match fail.";
            result.cost = MAX;
            return;
        }


        auto view_target_data = dynamic_cast<MergeTreeData *>(result.view_target_table.get());
        if (!view_target_data)
        {
            result.view_match_info = "checkPartitions - view target table is not merge tree data match fail.";
            result.cost = MAX;
            return;
        }

        if (target_table_data->getSettings()->disable_block_output)
        {
            LOG_DEBUG(&Poco::Logger::get("checkPartitions"), "Target storage disable write blocks so cancel partition check");
            return;
        }

        /// Required partition set
        auto required_partitions = target_table_data->getRequiredPartitions(query_info, context);
        std::unordered_set<String> required_part_name_set;
        for (const auto & part : required_partitions)
        {
            String partition_id = part->partition.getID(*target_table_data);
            required_part_name_set.emplace(partition_id);
        }

        /// View target partition set
        auto view_partitions = view_target_data->getDataPartsVector();
        std::unordered_set<String> view_part_name_set;
        for (const auto & part : view_partitions)
        {
            String partition_id = part->partition.getID(*view_target_data);
            view_part_name_set.emplace(partition_id);
        }

        auto & materialized_view = dynamic_cast<StorageMaterializedView &>(*(result.view_table));
        if (materialized_view.isRefreshing())
            view_part_name_set.erase(materialized_view.getRefreshingPartition());

        if (!subset(required_part_name_set, view_part_name_set))
        {
            String required_partitions_str;
            for (const auto & require_partition : required_part_name_set)
                required_partitions_str.append(require_partition).append(",");
            String owned_partitions_str;
            for (const auto & own_partition : view_part_name_set)
                owned_partitions_str.append(own_partition).append(",");
            result.view_match_info = "checkPartitions - require partitions-" + required_partitions_str +
                                                           " owned partitions-" + owned_partitions_str + " match fail.";
            result.cost = MAX;
            LOG_DEBUG(&Poco::Logger::get("checkPartitions"), result.view_match_info);
        }
    }

    std::pair<String, size_t> getReadCost(const StoragePtr & storage, const ASTPtr & query, ContextMutablePtr context)
    {
        size_t read_cost = 0;
        String exception;
        try
        {
            auto * select_ast = query->as<ASTSelectQuery>();
            if (select_ast)
            {
                SettingChange setting;
                setting.name = "enable_view_based_query_rewrite";
                setting.value = Field(false);
                if (select_ast->settings())
                {
                    auto * set_ast = select_ast->settings()->as<ASTSetQuery>();
                    auto it = std::find_if(set_ast->changes.begin(), set_ast->changes.end(), [&](const SettingChange & change) {
                        return change.name == "enable_view_based_query_rewrite";
                    });
                    if (it != set_ast->changes.end())
                        it->value = Field(false);
                    else
                        set_ast->changes.emplace_back(setting);
                }
            }
            context->setSetting("enable_view_based_query_rewrite", false);

            ///TODO: redesign to get read cost
            QueryPlan query_plan;
            QueryPlan::ExplainPlanOptions explain_options;
            InterpreterSelectQuery interpreter(query, context, SelectQueryOptions(QueryProcessingStage::FetchColumns));
            interpreter.buildQueryPlan(query_plan);
            explain_options.indexes = true;
            JSONBuilder::ItemPtr item = query_plan.explainPlan(explain_options);
            context->setSetting("enable_view_based_query_rewrite", true);
        }
        catch (...)
        {
            context->setSetting("enable_view_based_query_rewrite", true);
            exception = getCurrentExceptionMessage(true);
        }

        LOG_DEBUG(&Poco::Logger::get("getReadCost"), "get read cost-{}  from table-{}", read_cost, storage->getStorageID().getFullTableName());
        return {exception, read_cost};
    }

    void estimateReadCost(MatchResult & result,  ContextMutablePtr context)
    {
        if (result.cost == MAX)
            return;
        Names require_columns;
        auto query = result.rewriting_target_query.clone();
        StoragePtr mv_target_table = result.view_target_table;
        String exception;
        size_t read_cost;
        std::tie(exception, read_cost) = getReadCost(mv_target_table, query, context);
        if (exception.empty())
            result.read_cost = read_cost;
        else
        {
            result.cost = MAX;
            result.view_match_info = exception;
        }
    }

    void replaceTable(MatchResult & result)
    {
        if (result.cost == MAX)
            return;

        auto & tables_in_select_query = result.rewriting_target_query.tables()->as<ASTTablesInSelectQuery &>();
        auto & tables_element = tables_in_select_query.children[0]->as<ASTTablesInSelectQueryElement &>();
        auto & table_expression = tables_element.table_expression->as<ASTTableExpression &>();
        table_expression.database_and_table_name = std::make_shared<ASTTableIdentifier>(result.view_target_table->getStorageID().getDatabaseName(),
                                                                                        result.view_target_table->getStorageID().getTableName());
    }

    MatchResult match(
        MatchResult & result,
        ASTSelectQuery & view_query,
        const StoragePtr & target_table,
        SelectQueryInfo & query_info,
        ContextMutablePtr context)
    {
        ViewQueryInfo view_query_info(view_query, QUANTILE_STATE_FUNCS);

        LOG_DEBUG(&Poco::Logger::get("match"), "ViewQueryInfo select ids:");
        for (auto & select_id : view_query_info.select_ids)
        {
            if (select_id.second)
                LOG_DEBUG(&Poco::Logger::get("match"), "key-{}, value-{}", select_id.first, queryToString(*select_id.second));
        }

        LOG_DEBUG(&Poco::Logger::get("match"), "ViewQueryInfo select funcs:");
        for (auto & select_func: view_query_info.select_funcs)
        {
            if (select_func.second)
                LOG_DEBUG(&Poco::Logger::get("match"), "key-{}, value-{}", select_func.first, queryToString(*select_func.second));
        }

        LOG_DEBUG(&Poco::Logger::get("match"), "ViewQueryInfo select aggregate funcs:");
        for (auto & select_agg_func: view_query_info.select_aggregate_funcs)
        {
            if (select_agg_func.second)
                LOG_DEBUG(&Poco::Logger::get("match"), "key-{}, value-{}", select_agg_func.first, queryToString(*select_agg_func.second));
        }

        matchDistinct(result, view_query_info);
        matchSelect(result, view_query_info);
        matchGroup(result, view_query_info);
        matchOrder(result, view_query_info);
        matchLimit(result, view_query_info);
        matchHaving(result, view_query_info);
        matchWhere(result, view_query_info);
        checkPartitions(result, target_table, query_info, context);
        replaceTable(result);
        if (context->getSettingsRef().enable_mv_estimate_read_cost)
            estimateReadCost(result, context);
        return result;
    }

    /// We should reset the query's children after rewriting
    /// since `where_expression` may need to be replaced in the `children` and
    /// `prewhere_expression` may need to be erased from the `children`, and other cases.
    void resetChildren(ASTSelectQuery & select_query)
    {
        select_query.children.clear();
        auto add = [&select_query](const ASTPtr & member) { if (member) select_query.children.push_back(member); };
        /// Add exactly in the same order, in which they were inserted into `children` in ASTSelectQuery::clone().
        add(select_query.with());
        add(select_query.select());
        add(select_query.tables());
        add(select_query.prewhere());
        add(select_query.where());
        add(select_query.groupBy());
        add(select_query.having());
        add(select_query.orderBy());
        add(select_query.limitBy());
        add(select_query.limitOffset());
        add(select_query.limitLength());
        add(select_query.settings());
    }

    /// Return whether the materialized view satisfies some constraints
    std::pair<bool, String> satisfyViewConstraints(StorageMaterializedView & materialized_view, ASTSelectQuery & view_query)
    {
        auto view_target_table = materialized_view.tryGetTargetTable();
        if (view_target_table && (MERGE_TREES.count(view_target_table->getName()) || AGGREGATING_MERGE_TREES.count(view_target_table->getName())))
        {
            if (AGGREGATING_MERGE_TREES.count(view_target_table->getName()))
            {
                /// View query with no group by is not support. The AggregatingMergeTree will replaces all rows with the
                /// same sorting key, we can think it will do a group operation on the sorting key. So specify group by
                /// in the view query is natural. Besides, if we use a view whose inner query with no group by to rewrite
                /// a target query also with no group by, we might get wrong query result.
                if (!view_query.groupBy())
                {
                    return {false, "View query with no group by is not support"};
                }

                std::unordered_set<String> sorting_columns;
                auto sorting_key_names = view_target_table->getInMemoryMetadataPtr()->getSortingKeyColumns();
                for (auto & key_name : sorting_key_names)
                    sorting_columns.emplace(key_name);

                for (auto & child : view_query.select()->children)
                {
                    /// Every column which is not under aggregate function should appear in sorting columns.
                    /// If not, the view is not support to be used in rewriting.
                    /// Same reason, the AggregatingMergeTree will replaces all rows with the same sorting key,
                    /// We might get wrong query result if we use such view to rewrite a query.
                    auto * identifier = child->as<ASTIdentifier>();
                    auto * function = child->as<ASTFunction>();
                    if ((identifier ||
                         (function && !AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))) &&
                        !sorting_columns.count(child->getAliasOrColumnName()))
                    {
                        return {false, "view select column " + child->getAliasOrColumnName() + " is not in target table sorting key"};
                    }
                }
            }
        }
        else
        {
            /// Other merge trees are not support in rewriting
            String storages;
            for (const auto & merge_tree : MERGE_TREES)
                storages.append(merge_tree).append(",");
            for (const auto & aggregate_tree : AGGREGATING_MERGE_TREES)
                storages.append(aggregate_tree).append(",");
            return {false, "view rewrite target storage type only support " + storages};
        }

        return {true, ""};
    }

    MaterializedViewOptimizerResultPtr MaterializedViewSubstitutionOptimizer::optimizeImpl(
        ASTSelectQuery * target_query,
        const Dependencies & dependencies,
        const StoragePtr & target_table,
        SelectQueryInfo & query_info)
    {
        const auto & target_query_str = queryToString(*target_query);
        LOG_DEBUG(log, "Query {" + target_query_str +
                       "} possibly can be rewrited by it's materialized views, start finding a suitable view to rewrite it!");

        MatchResultSet candidates;
        for (const auto & view_table_id : dependencies)
        {
            auto view_table_ptr = DatabaseCatalog::instance().getTable(view_table_id, context);
            if (!view_table_ptr)
                continue;

            LOG_DEBUG(log, "Estimate database-{} table-{} for query rewrite", view_table_id.getDatabaseName(), view_table_id.getTableName());
            auto * materialized_view = dynamic_cast<StorageMaterializedView *>(view_table_ptr.get());
            if (!materialized_view)
                continue;

            MatchResult result;
            result.view_table = view_table_ptr;
            result.view_target_table = materialized_view->getTargetTable();
            result.rewriting_target_query = target_query->clone()->as<ASTSelectQuery &>();
            result.view_match_info = "";

            if (!materialized_view->supportsSampling() && result.rewriting_target_query.sampleSize())
                result.view_match_info = "Match failed target table-" + result.view_target_table->getStorageID().getFullTableName() + " not support sample";

            auto * view_query = materialized_view->normalizeInnerQuery()->as<ASTSelectQuery>();

            if (!view_query)
                continue;

            LOG_DEBUG(log, "Normalized View query-{}",  queryToString(*view_query));

            /// If there is having expression in view, then such view should be ignored,
            /// since aggregate value in view is a partial value, this will cause incorrect
            /// result even if the having expression in view can match the target.
            if (view_query->having())
                result.view_match_info = "Match failed due to view inner query contain having expression.";

            /// If there is `limit` in view, then such view should be ignored,
            /// since the data in the view is incomplete.
            if (view_query->limitBy() || view_query->limitOffset() || view_query->limitLength())
                result.view_match_info = "Match failed due to view inner query contain limit expressions.";

            std::pair<bool, String> constraint_result = satisfyViewConstraints(*materialized_view, *view_query);
            if (!constraint_result.first)
                result.view_match_info = constraint_result.second;

            auto ast_db_tbl = validateAndExtractDbTable(view_query);
            if (!ast_db_tbl.first)
                result.view_match_info = ast_db_tbl.second;

            if (!result.view_match_info.empty())
            {
                result.cost = MAX;
                candidates.insert(std::make_shared<MatchResult>(result));
                LOG_DEBUG(log, result.view_match_info);
                continue;
            }

            match(result, *view_query, target_table, query_info, context);
            candidates.insert(std::make_shared<MatchResult>(result));
        }

        LOG_DEBUG(log, "Materialized view candidates size-{}", candidates.size());

        if (!candidates.empty())
        {
            auto & best_match = *candidates.begin();
            if (best_match->cost != MAX)
            {
                if (context->getSettingsRef().enable_mv_estimate_read_cost)
                {
                    String exception;
                    size_t target_read_cost;
                    auto clone_target_query = query_info.query->clone();
                    std::tie(exception, target_read_cost) = getReadCost(target_table, clone_target_query, context);
                    bool disable_block_output = false;
                    if (auto merge_tree_data = dynamic_cast<MergeTreeData *>(target_table.get()))
                        disable_block_output = merge_tree_data->getSettings()->disable_block_output;
                    if (exception.empty() && !disable_block_output && target_read_cost < best_match->read_cost)
                    {
                        LOG_DEBUG(log, "Haven't find a suitable materialized view that can rewrite query {" + target_query_str +
                                          "}, due to view read cost-{} is smaller than target table cost-{} utilize original query to execute",
                                  best_match->read_cost, target_read_cost);
                        return std::make_shared<const MaterializedViewOptimizerResult>(candidates);
                    }
                }
                *target_query = best_match->rewriting_target_query;
                //resetChildren(*target_query);
                LOG_DEBUG(log, "Query has been rewrited from {" + target_query_str + "} to {" + queryToString(*target_query) + "}.");
                return std::make_shared<const MaterializedViewOptimizerResult>(true, best_match->name_substitution_info, best_match->view_target_table, candidates);
            }
        }

        LOG_DEBUG(log, "Not find a suitable materialized view that can rewrite query {" + target_query_str + "}, sad...");
        return std::make_shared<const MaterializedViewOptimizerResult>(candidates);
    }
}
