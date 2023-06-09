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
#include <Analyzers/TypeAnalyzer.h>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/ExpressionInterpreter.h>
#include <QueryPlan/FilterStep.h>

namespace DB
{
using FilterEstimateResult = std::pair<double, std::unordered_map<String, SymbolStatisticsPtr>>;
using FilterEstimateResults = std::vector<FilterEstimateResult>;

struct FilterEstimatorContext
{
    ContextMutablePtr & context;
    const ExpressionInterpreter & interpreter;
    std::optional<Field> calculateConstantExpression(const ConstASTPtr & node)
    {
        auto field_with_type = interpreter.evaluateConstantExpression(node);
        if (field_with_type.has_value())
            return field_with_type->second;
        else
            return std::nullopt;
    }
};

class FilterEstimator
{
public:
    constexpr static double DEFAULT_SELECTIVITY = 0.9;

    /**
     * Returns an option of PlanNodeStatistics for a Filter logical plan node.
     * For a given compound expression condition, this method computes filter selectivity
     * (or the percentage of rows meeting the filter condition), which
     * is used to compute row count, size in bytes, and the updated statistics after a given
     * predicated is applied.
     *
     * @return PlanNodeStatisticsPtr When there is no statistics collected, it returns None.
     */
    static PlanNodeStatisticsPtr
    estimate(PlanNodeStatisticsPtr & child_stats, const FilterStep & step, ContextMutablePtr & context, bool is_on_base_table = true);

    static double estimateFilterSelectivity(
        PlanNodeStatisticsPtr & child_stats, ConstASTPtr & predicate, const NamesAndTypes & column_types, ContextMutablePtr & context);

private:
    /**
     * Returns a percentage of rows meeting a condition in Filter node.
     * If it's a single condition, we calculate the percentage directly.
     * If it's a compound condition, it is decomposed into multiple single conditions linked with
     * AND, OR, NOT.
     */
    static FilterEstimateResult estimateFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateAndFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateOrFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateNotFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);

    /**
     * Combine symbol statistics.
     *
     * for example: combine a map like
     *
     * {a, statistics-1}
     * {b, statistics-2}
     * {a, statistics-3}
     *
     * to:
     *
     * {a, statistics-1, statistics-2}
     * {b, statistics-2}
     */
    static std::unordered_map<String, std::vector<SymbolStatisticsPtr>> combineSymbolStatistics(FilterEstimateResults &);

    /**
     * Returns a percentage of rows meeting a single condition in Filter node.
     * Currently we only support binary predicates where one side is a column,
     * and the other is a literal.
     */
    static FilterEstimateResult estimateSingleFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult
    estimateEqualityFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult
    estimateNotEqualityFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateRangeFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateInFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateNotInFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateNullFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult
    estimateNotNullFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult estimateLikeFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
    static FilterEstimateResult
    estimateNotLikeFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context);
};

}
