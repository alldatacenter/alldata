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

#include <Interpreters/Context.h>
#include <Parsers/ASTFunction.h>
#include <Functions/IFunction.h>
#include <Functions/FunctionHelpers.h>
#include <DataTypes/DataTypesNumber.h>

namespace DB
{
using DynamicFilterId = UInt32;

enum class DynamicFilterType : UInt8
{
    Range,
    BloomFilter,
    // Values
};

using DynamicFilterTypes = std::unordered_set<DynamicFilterType>;

struct DynamicFilterBuildInfo
{
    DynamicFilterId id;
    String original_symbol;
    DynamicFilterTypes types;
};

struct DynamicFilterDescription
{
    DynamicFilterDescription(
        DynamicFilterId id_,
        std::string original_symbol_,
        ConstASTPtr expr_,
        DynamicFilterType type_)
        : id(id_), original_symbol(std::move(original_symbol_)), expr(std::move(expr_)), type(type_) { }

    DynamicFilterId id;
    std::string original_symbol;
    ConstASTPtr expr;
    DynamicFilterType type;
};

class SymbolStatistics;
using SymbolStatisticsPtr = std::shared_ptr<SymbolStatistics>;
class PlanNodeStatistics;
using PlanNodeStatisticsPtr = std::shared_ptr<PlanNodeStatistics>;
class FilterStep;
class RuntimeFilterManager;

/**
 * Dynamic Filter, also as runtime filter, improve the performance of queries with selective joins
 * by filtering data early that would be filtered by join condition.
 *
 * When dynamic filtering is enabled, values are collected from the build side of join, and sent to
 * probe side of join in runtime.
 *
 * Dynamic Filter could be used for dynamic partition pruning, reduce table scan data with index,
 * reduce exchange shuffle, and so on.
 *
 * Dynamic Filter has two parts in plan:
 *  1. build side, model as projection attribute.
 *  2. consumer side, model as a filter predicates.
 */
class DynamicFilters
{
public:
    static ConstASTPtr createDynamicFilterExpression(DynamicFilterId id, const std::string & symbol, DynamicFilterType type);
    static ConstASTPtr createDynamicFilterExpression(const DynamicFilterDescription & description);

    static DynamicFilterId extractId(const ConstASTPtr & dynamic_filter);
    static std::optional<DynamicFilterDescription> extractDescription(const ConstASTPtr & dynamic_filter);

    /* dynamic_filters, static_filters */
    static std::pair<std::vector<ConstASTPtr>, std::vector<ConstASTPtr>> extractDynamicFilters(const ConstASTPtr & conjuncts);

    static bool isDynamicFilter(const ConstASTPtr & expr);
    static bool isSupportedForTableScan(const DynamicFilterDescription & description);

    static double estimateSelectivity(
        const DynamicFilterDescription & description,
        const SymbolStatisticsPtr & filter_source,
        const PlanNodeStatisticsPtr & child_stats,
        const FilterStep & step,
        ContextMutablePtr & context);

    static std::vector<ASTPtr> createDynamicFilterRuntime(
        const DynamicFilterDescription & description,
        const String & query_id,
        const size_t & segment_id,
        UInt64 timeout_ms,
        RuntimeFilterManager & manager,
        const String & caller = "");

    static std::string toString(const DynamicFilterDescription & description);
};

}
