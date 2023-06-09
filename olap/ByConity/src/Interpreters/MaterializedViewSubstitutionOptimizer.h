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

#pragma once

#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTFunction.h>
#include <Interpreters/SelectQueryOptions.h>
#include <Interpreters/Context.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Storages/SelectQueryInfo.h>
#include <Core/Field.h>
#include <Storages/IStorage.h>

#include <unordered_map>
#include <set>
#include <utility>

/** This class provides functions for rewriting query based on view substitution. We want to automatically using
 *  preexisting materialized views to speed up query processing.
 *
 *  The Example:
 *      Target query: SELECT a, c FROM t WHERE x = 5 AND b = 4
 *      Materialized view definition: SELECT a, b, c FROM t WHERE x = 5
 *      Result: SELECT a, c FROM mv WHERE b = 4
 */
namespace DB
{
    struct MatchResult : public std::enable_shared_from_this<MatchResult>
    {
        using ResultPtr = std::shared_ptr<MatchResult>;
        /// There may be several available materialized views, small cost means more suitable.
        /// The cost is calculated in a simple way, isn't exact.
        int cost = 0;
        String view_match_info;
        ASTSelectQuery rewriting_target_query;
        std::unordered_map<String, String> name_substitution_info;
        StoragePtr view_table;
        StoragePtr view_target_table;
        size_t read_cost {0};

        struct ResultLessCompare
        {
            bool operator()(const ResultPtr & lrs, const ResultPtr & rrs) const
            {
                if (lrs->cost == rrs->cost)
                {
                    if (lrs->read_cost == rrs->read_cost)
                        return lrs->view_table->getStorageID().getFullTableName() < rrs->view_table->getStorageID().getFullTableName();
                    else
                        return lrs->read_cost < rrs->read_cost;
                }
                else
                    return lrs->cost < rrs->cost;
            }
        };
    };

    using MatchResultSet = std::set<MatchResult::ResultPtr, MatchResult::ResultLessCompare>;

    struct MaterializedViewOptimizerResult
    {
        MaterializedViewOptimizerResult() = default;

        MaterializedViewOptimizerResult(bool rewrite_by_view_,
                                        const std::unordered_map<String, String> & name_substitution_info_,
                                        const StoragePtr & storage_, const MatchResultSet & views_match_info_) : rewrite_by_view(rewrite_by_view_),
                                                                       name_substitution_info(name_substitution_info_),
                                                                       storage(storage_), views_match_info(views_match_info_) {}

        MaterializedViewOptimizerResult(const MatchResultSet & views_match_info_) : views_match_info(views_match_info_) {}

        MaterializedViewOptimizerResult(const String & validate_info_) : validate_info(validate_info_) {}

        /// Is query rewrited by materialized view
        bool rewrite_by_view = false;

        /// The collected column name substitution info, which would be used in `ColumnSubstitutionInputStream`.
        /// It's not empty only if `rewrite_by_view` is true.
        std::unordered_map<String, String> name_substitution_info;

        /// storage of the query table after rewrite
        StoragePtr storage;

        /// <match_cost, match_result> all dependencies view match result for explain query
        MatchResultSet views_match_info;

        /// Validate information about mis match
        String validate_info;
    };

    using MaterializedViewOptimizerResultPtr = std::shared_ptr<const MaterializedViewOptimizerResult>;

    struct ViewQueryInfo
    {
        const ASTSelectQuery & query;
        std::unordered_map<String, ASTIdentifier *> select_ids;
        std::unordered_map<String, ASTFunction *> select_funcs;
        std::unordered_map<String, ASTFunction *> select_aggregate_funcs;

        ViewQueryInfo(const ASTSelectQuery & query_, const std::unordered_set<String> & quantile_state_funcs) : query(query_)
        {
            for (auto & child : query.select()->children)
            {
                if (auto * identifier = child->as<ASTIdentifier>())
                    select_ids[identifier->name()] = identifier;
                else if (auto * function = child->as<ASTFunction>())
                {
                    if (AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))
                    {
                        if (quantile_state_funcs.count(function->name))
                        {
                            const auto parameters = function->parameters;
                            function->parameters = nullptr;
                            select_aggregate_funcs[function->getColumnName()] = function;
                            // restore the parameters
                            function->parameters = parameters;
                        }
                        else
                            select_aggregate_funcs[function->getColumnName()] = function;
                    }
                    else
                        select_funcs[function->getColumnName()] = function;
                }
            }
        }
    };

    class MaterializedViewSubstitutionOptimizer
    {
    public:
        MaterializedViewSubstitutionOptimizer(ContextPtr context_, const SelectQueryOptions & options_);

        MaterializedViewOptimizerResultPtr optimize(SelectQueryInfo & query_info);

    private:
        ContextMutablePtr context;
        const SelectQueryOptions & options;
        Poco::Logger * log;

        MaterializedViewOptimizerResultPtr optimizeImpl(
            ASTSelectQuery * target_query,
            const Dependencies & dependencies,
            const StoragePtr & target_table,
            SelectQueryInfo & query_info);
    };
}
