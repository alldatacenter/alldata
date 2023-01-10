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

#include <DataTypes/FieldToDataType.h>
#include <Functions/InternalFunctionsDynamicFilter.h>
#include <Optimizer/CardinalityEstimate/FilterEstimator.h>
#include <Optimizer/PredicateUtils.h>
#include <Parsers/ASTFunction.h>

namespace DB
{
PlanNodeStatisticsPtr FilterEstimator::estimate(
    PlanNodeStatisticsPtr & opt_child_stats, const FilterStep & step, ContextMutablePtr & context, bool is_on_base_table)
{
    if (!opt_child_stats)
    {
        return nullptr;
    }

    PlanNodeStatisticsPtr filter_stats = opt_child_stats->copy();

    // if the child of filter step is table scan, or projection + table scan, e.g.
    // which don't change the cardinality of data. then proceed filter estimate.
    // if the child of filter step is join/aggregate, we consider the statistics is some how
    // useless, and prefer use default selectivity.
    if (!is_on_base_table)
    {
        // Prefer default selectivity when is_on_base_table flag is false.
        UInt64 row_count = filter_stats->getRowCount() * DEFAULT_SELECTIVITY;

        // make row count at least 1.
        row_count = row_count > 1 ? row_count : 1;
        filter_stats->updateRowCount(row_count);
        for (auto & symbol_stats : filter_stats->getSymbolStatistics())
        {
            symbol_stats.second = symbol_stats.second->applySelectivity(DEFAULT_SELECTIVITY);
            // NDV must less or equals to row count
            symbol_stats.second->setNdv(std::min(filter_stats->getRowCount(), symbol_stats.second->getNdv()));
        }
        return filter_stats;
    }

    auto predicate = step.getFilter();
    auto interpreter = ExpressionInterpreter::basicInterpreter(step.getInputStreams()[0].header.getNamesToTypes(), context);
    FilterEstimatorContext estimator_context{.context = context, .interpreter = interpreter};
    FilterEstimateResult result = estimateFilter(*filter_stats, predicate, estimator_context);

    double selectivity = result.first;

    if (selectivity <= 0.0)
    {
        return std::make_shared<PlanNodeStatistics>(0);
    }
    if (selectivity >= 1.0)
    {
        return filter_stats;
    }

    UInt64 filtered_row_count = filter_stats->getRowCount() * selectivity;
    // make row count at least 1.
    filter_stats->updateRowCount(filtered_row_count > 1 ? filtered_row_count : 1);
    std::unordered_map<String, SymbolStatisticsPtr> & symbol_statistics_in_filter = result.second;
    for (auto & symbol_statistics : filter_stats->getSymbolStatistics())
    {
        // for symbol in filters. use the filtered statistics.
        if (!symbol_statistics_in_filter.empty() && symbol_statistics_in_filter.contains(symbol_statistics.first))
        {
            symbol_statistics.second = symbol_statistics_in_filter[symbol_statistics.first];
        }
        else
        {
            symbol_statistics.second = symbol_statistics.second->applySelectivity(selectivity);
            // NDV must less or equals to row count
            symbol_statistics.second->setNdv(std::min(filter_stats->getRowCount(), symbol_statistics.second->getNdv()));
        }
    }

    // make sure row count at least 1.
    return filter_stats;
}

std::optional<Field> castStringType(SymbolStatistics & symbol_statistics, Field literal, FilterEstimatorContext & context)
{
    DataTypePtr type = applyVisitor(FieldToDataType(), literal);
    // if not string, just return

    if (type->getTypeId() == TypeIndex::String && symbol_statistics.isImplicitConvertableFromString())
    {
        auto target_type_name = symbol_statistics.getType()->getName();
        auto cast = makeASTFunction("cast", std::make_shared<ASTLiteral>(literal), std::make_shared<ASTLiteral>(target_type_name));
        return context.calculateConstantExpression(cast);
    }

    return std::nullopt;
}

double FilterEstimator::estimateFilterSelectivity(
    PlanNodeStatisticsPtr & child_stats, ConstASTPtr & predicate, const NamesAndTypes & column_types, ContextMutablePtr & context)
{
    NameToType name_to_type;
    for (const auto & item : column_types)
        name_to_type.emplace(item.name, item.type);
    auto interpreter = ExpressionInterpreter::basicInterpreter(name_to_type, context);
    FilterEstimatorContext estimator_context{.context = context, .interpreter = interpreter};
    return estimateFilter(*child_stats, predicate, estimator_context).first;
}

ConstASTPtr tryGetIdentifier(ConstASTPtr node)
{
    if (auto cast_func = node->as<ASTFunction>())
    {
        if (Poco::toLower(cast_func->name) == "cast")
        {
            return cast_func->arguments->getChildren()[0];
        }
    }
    return node;
}

FilterEstimateResult FilterEstimator::estimateFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    if (predicate->as<ASTLiteral>())
    {
        if (PredicateUtils::isTruePredicate(predicate))
        {
            return {1.0, {}};
        }
        if (PredicateUtils::isFalsePredicate(predicate))
        {
            return {0.0, {}};
        }
        std::optional<Field> result = context.calculateConstantExpression(predicate);
        if (result.has_value() && result->isNull())
        {
            return {0.0, {}};
        }
    }
    if (!predicate->as<const ASTFunction>())
    {
        return {1.0, {}};
    }
    auto & function = predicate->as<const ASTFunction &>();
    if (function.name == "and")
    {
        return estimateAndFilter(stats, predicate, context);
    }
    if (function.name == "or")
    {
        return estimateOrFilter(stats, predicate, context);
    }
    if (function.name == "not")
    {
        return estimateNotFilter(stats, predicate, context);
    }
    return estimateSingleFilter(stats, predicate, context);
}

FilterEstimateResult
FilterEstimator::estimateAndFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    std::vector<ConstASTPtr> conjuncts = PredicateUtils::extractConjuncts(predicate);

    FilterEstimateResults results;
    double selectivity = 1.0;
    for (auto & conjunct : conjuncts)
    {
        FilterEstimateResult result = estimateFilter(stats, conjunct, context);
        results.emplace_back(result);
        selectivity = selectivity * result.first;
        // for AND predicate, must update statistics.
        // for example, a > 1 and a < 10, if we don't update statistics after apply predicate 'a > 1',
        // then the estimate of a < 10 will base on the origin statistics. the result will expand.
        for (auto & entry : result.second)
        {
            stats.updateSymbolStatistics(entry.first, entry.second);
        }
    }

    std::unordered_map<String, SymbolStatisticsPtr> and_symbol_statistics;
    for (auto & result : results)
    {
        std::unordered_map<String, SymbolStatisticsPtr> & symbol_statistics = result.second;
        for (auto & symbol_statistics_entry : symbol_statistics)
        {
            String symbol = symbol_statistics_entry.first;
            SymbolStatisticsPtr & statistics = symbol_statistics_entry.second;
            and_symbol_statistics[symbol] = statistics;
        }
    }

    return {selectivity, and_symbol_statistics};
}

FilterEstimateResult
FilterEstimator::estimateOrFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    std::vector<ConstASTPtr> disjuncts = PredicateUtils::extractDisjuncts(predicate);
    FilterEstimateResults results;
    double selectivity;
    double sum_selectivity = 0.0;
    double multiply_selectivity = 1.0;

    for (auto & disjunct : disjuncts)
    {
        // for each or predicate, use origin statistics to estimate.
        PlanNodeStatisticsPtr or_stats = stats.copy();
        FilterEstimateResult result = estimateFilter(*or_stats, disjunct, context);
        results.emplace_back(result);
        sum_selectivity = sum_selectivity + result.first;
        multiply_selectivity = multiply_selectivity * result.first;
    }
    selectivity = sum_selectivity - multiply_selectivity;

    std::unordered_map<String, std::vector<SymbolStatisticsPtr>> combined_symbol_statistics = combineSymbolStatistics(results);
    std::unordered_map<String, SymbolStatisticsPtr> symbol_statistics;
    for (auto & result : combined_symbol_statistics)
    {
        String symbol = result.first;
        auto & value = result.second;
        // for predicates like : a > 1, use the origin statistics.
        if (value.size() == 1)
        {
            symbol_statistics[symbol] = value[0];
        }
        else
        {
            // for predicates like a > 1 or a < 10, union the origin statistics of (a > 1) and (a < 10).
            auto & first_value = value[0];
            for (size_t i = 1; i < value.size(); ++i)
            {
                first_value = first_value->createUnion(value[i]);
            }
            symbol_statistics[symbol] = first_value;
        }
    }
    return {selectivity, symbol_statistics};
}

FilterEstimateResult
FilterEstimator::estimateNotFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto function = predicate->as<const ASTFunction &>();
    ConstASTPtr sub = function.arguments->getChildren()[0];
    FilterEstimateResult result = estimateFilter(stats, sub, context);

    std::unordered_map<String, SymbolStatisticsPtr> not_symbol_statistics;
    for (auto & symbol_statistics : result.second)
    {
        String symbol = symbol_statistics.first;
        SymbolStatisticsPtr origin = stats.getSymbolStatistics(symbol);
        not_symbol_statistics[symbol] = symbol_statistics.second->createNot(origin);
    }
    return {1.0 - result.first, not_symbol_statistics};
}

std::unordered_map<String, std::vector<SymbolStatisticsPtr>> FilterEstimator::combineSymbolStatistics(FilterEstimateResults & results)
{
    std::unordered_map<String, std::vector<SymbolStatisticsPtr>> combined_symbol_statistics;

    for (auto & result : results)
    {
        std::unordered_map<String, SymbolStatisticsPtr> & symbol_statistics = result.second;
        for (auto & symbol_statistics_entry : symbol_statistics)
        {
            String symbol = symbol_statistics_entry.first;
            SymbolStatisticsPtr & statistics = symbol_statistics_entry.second;
            if (combined_symbol_statistics.contains(symbol))
            {
                std::vector<SymbolStatisticsPtr> & value = combined_symbol_statistics[symbol];
                value.emplace_back(statistics);
            }
            else
            {
                combined_symbol_statistics[symbol] = std::vector<SymbolStatisticsPtr>{statistics};
            }
        }
    }
    return combined_symbol_statistics;
}

FilterEstimateResult
FilterEstimator::estimateSingleFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<const ASTFunction &>();
    if (function.name == "equals")
    {
        return estimateEqualityFilter(stats, predicate, context);
    }
    if (function.name == "notEquals")
    {
        return estimateNotEqualityFilter(stats, predicate, context);
    }
    if (function.name == "less")
    {
        return estimateRangeFilter(stats, predicate, context);
    }
    if (function.name == "lessOrEquals")
    {
        return estimateRangeFilter(stats, predicate, context);
    }
    if (function.name == "greater")
    {
        return estimateRangeFilter(stats, predicate, context);
    }
    if (function.name == "greaterOrEquals")
    {
        return estimateRangeFilter(stats, predicate, context);
    }
    if (function.name == "in" || function.name == "globalIn")
    {
        return estimateInFilter(stats, predicate, context);
    }
    if (function.name == "notIn" || function.name == "globalNotIn")
    {
        return estimateNotInFilter(stats, predicate, context);
    }
    if (function.name == "isNull")
    {
        return estimateNullFilter(stats, predicate, context);
    }
    if (function.name == "isNotNull")
    {
        return estimateNotNullFilter(stats, predicate, context);
    }
    if (function.name == "like")
    {
        return estimateLikeFilter(stats, predicate, context);
    }
    if (function.name == "notLike")
    {
        return estimateNotLikeFilter(stats, predicate, context);
    }

    if (function.name == InternalFunctionDynamicFilter::name)
    {
        return {1.0, {}};
    }
    // For not-supported condition, set filter selectivity to a conservative estimate 100%
    return {1.0, {}};
}

FilterEstimateResult
FilterEstimator::estimateEqualityFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<const ASTFunction &>();

    ConstASTPtr left = tryGetIdentifier(function.arguments->getChildren()[0]);
    std::optional<Field> field = context.calculateConstantExpression(function.arguments->getChildren()[1]);

    // only process, predicate with format : 'symbol = value', if predicate don't meet the format,
    // please modify rule std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression")
    if (!left->as<ASTIdentifier>() || !field.has_value())
    {
        return {1.0, {}};
    }

    auto & identifier = left->as<ASTIdentifier &>();
    String symbol = identifier.name();
    Field literal = *field;

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);

    // No statistics for symbol
    if (symbol_statistics.isUnknown())
    {
        return {1.0, {}};
    }

    // symbol == null, or symbol != null will be convert to NULL literal.
    double selectivity = 1.0;
    if (symbol_statistics.isNumber())
    {
        try
        {
            auto eval_res = castStringType(symbol_statistics, literal, context);
            if (eval_res.has_value() && !eval_res->isNull())
            {
                literal = *eval_res;
            }

            double value = symbol_statistics.toDouble(literal);
            // decide if the value is in [min, max] of the column.
            if (symbol_statistics.contains(value))
            {
                selectivity = symbol_statistics.estimateEqualFilter(value);
                std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
                    = {{symbol, symbol_statistics.createEqualFilter(value)}};
                return {selectivity, filtered_symbol_statistics};
            }
            else
            {
                selectivity = 0.0;
                std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
                    = {{symbol, symbol_statistics.createEmpty()}};
                return {selectivity, filtered_symbol_statistics};
            }
        }
        catch (...)
        {
            return {symbol_statistics.getNdv() == 0 ? selectivity : 1.0 / symbol_statistics.getNdv(), {}};
        }
    }
    else if (symbol_statistics.isString())
    {
        String str = symbol_statistics.toString(literal);
        double value = CityHash_v1_0_2::CityHash64(str.data(), str.size());
        selectivity = symbol_statistics.estimateEqualFilter(value);
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createEqualFilter(value)}};
        return {selectivity, filtered_symbol_statistics};
    }
    return {1.0, {}};
}

FilterEstimateResult
FilterEstimator::estimateNotEqualityFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<ASTFunction &>();

    ConstASTPtr left = tryGetIdentifier(function.arguments->getChildren()[0]);
    std::optional<Field> field = context.calculateConstantExpression(function.arguments->getChildren()[1]);

    // only process, predicate with format : 'symbol != value', if predicate don't meet the format,
    // please modify rule std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression")
    if (!left->as<ASTIdentifier>() || !field.has_value())
    {
        return {1.0, {}};
    }

    auto & identifier = left->as<ASTIdentifier &>();
    String symbol = identifier.name();
    Field literal = *field;

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);

    // No statistics for symbol
    if (symbol_statistics.isUnknown())
    {
        return {1.0, {}};
    }

    double selectivity = 1.0;
    if (symbol_statistics.isNumber())
    {
        try
        {
            auto eval_res = castStringType(symbol_statistics, literal, context);
            if (eval_res.has_value() && !eval_res->isNull())
            {
                literal = *eval_res;
            }

            double value = symbol_statistics.toDouble(literal);
            // decide if the value is in [min, max] of the column.
            if (symbol_statistics.contains(value))
            {
                selectivity = symbol_statistics.estimateNotEqualFilter(value);
                std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
                    = {{symbol, symbol_statistics.createNotEqualFilter(value)}};
                return {selectivity, filtered_symbol_statistics};
            }
            else
            {
                return {1.0, {}};
            }
        }
        catch (...)
        {
            return {symbol_statistics.getNdv() == 0 ? selectivity : 1 - 1.0 / symbol_statistics.getNdv(), {}};
        }
    }
    else if (symbol_statistics.isString())
    {
        String str = symbol_statistics.toString(literal);
        double value = CityHash_v1_0_2::CityHash64(str.data(), str.size());
        selectivity = symbol_statistics.estimateNotEqualFilter(value);
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createNotEqualFilter(value)}};
        return {selectivity, filtered_symbol_statistics};
    }
    return {1.0, {}};
}

FilterEstimateResult
FilterEstimator::estimateRangeFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<ASTFunction &>();

    ConstASTPtr left = tryGetIdentifier(function.arguments->getChildren()[0]);
    std::optional<Field> field = context.calculateConstantExpression(function.arguments->getChildren()[1]);

    // only process, predicate with format : 'symbol > | < | >= | <= value', if predicate don't meet the format,
    // please modify rule std::make_shared<IterativeRewriter>(Rules::normalizeExpressionRules(), "NormalizeExpression")
    if (!left->as<ASTIdentifier>() || !field.has_value())
    {
        return {1.0, {}};
    }

    auto & identifier = left->as<ASTIdentifier &>();
    String symbol = identifier.name();
    Field literal = *field;

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);
    if (symbol_statistics.isUnknown())
    {
        // No statistics for symbol
        return {1.0, {}};
    }

    double selectivity = 1.0;
    SymbolStatisticsPtr filtered_statistics;
    if (symbol_statistics.isNumber())
    {
        try
        {
            auto eval_res = castStringType(symbol_statistics, literal, context);
            if (eval_res.has_value() && !eval_res->isNull())
            {
                literal = *eval_res;
            }
            double value = symbol_statistics.toDouble(literal);
            double min = symbol_statistics.getMin();
            double max = symbol_statistics.getMax();

            if (function.name == "less")
            {
                selectivity = symbol_statistics.estimateLessThanOrLessThanEqualFilter(value, false, min, true);
                filtered_statistics = symbol_statistics.createLessThanOrLessThanEqualFilter(selectivity, min, value, false);
            }
            if (function.name == "lessOrEquals")
            {
                selectivity = symbol_statistics.estimateLessThanOrLessThanEqualFilter(value, true, min, true);
                filtered_statistics = symbol_statistics.createLessThanOrLessThanEqualFilter(selectivity, min, value, true);
            }
            if (function.name == "greater")
            {
                selectivity = symbol_statistics.estimateGreaterThanOrGreaterThanEqualFilter(max, true, value, false);
                filtered_statistics = symbol_statistics.createGreaterThanOrGreaterThanEqualFilter(selectivity, value, max, false);
            }
            if (function.name == "greaterOrEquals")
            {
                selectivity = symbol_statistics.estimateGreaterThanOrGreaterThanEqualFilter(max, true, value, true);
                filtered_statistics = symbol_statistics.createGreaterThanOrGreaterThanEqualFilter(selectivity, value, max, true);
            }
        }
        catch (...)
        {
            return {1.0, {}};
        }

        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics = {{symbol, filtered_statistics}};
        return {selectivity, filtered_symbol_statistics};
    }
    else if (symbol_statistics.isString())
    {
        // string type does not support range filter.
        return {1.0, {}};
    }
    return {1.0, {}};
}

FilterEstimateResult
FilterEstimator::estimateInFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<ASTFunction &>();
    bool match = function.arguments->getChildren()[0]->as<ASTIdentifier>() && function.arguments->getChildren()[1]->as<ASTFunction>();
    if (!match)
    {
        return {1.0, {}};
    }

    ASTIdentifier & identifier = function.arguments->getChildren()[0]->as<ASTIdentifier &>();
    ASTFunction & tuple = function.arguments->getChildren()[1]->as<ASTFunction &>();

    String symbol = identifier.name();

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);

    // No statistics for symbol
    if (symbol_statistics.isUnknown())
    {
        return {1.0, {}};
    }

    if (symbol_statistics.isNumber())
    {
        std::set<double> values;
        bool has_null_value = false;
        int can_not_eval_count = 0;
        for (auto & child : tuple.arguments->getChildren())
        {
            if (auto eval_res = context.calculateConstantExpression(child))
            {
                if (!eval_res->isNull())
                {
                    try
                    {
                        auto cast_result = castStringType(symbol_statistics, *eval_res, context);
                        if (cast_result.has_value() && !cast_result->isNull())
                        {
                            eval_res = cast_result;
                        }
                        if (eval_res)
                        {
                            double value = symbol_statistics.toDouble(*eval_res);
                            values.emplace(value);
                        }
                    }
                    catch (...)
                    {
                        // ignore value
                        can_not_eval_count += 1;
                    }
                }
                else
                {
                    has_null_value = true;
                }
            }
        }
        double in_values_selectivity = symbol_statistics.estimateInFilter(values, has_null_value, stats.getRowCount());
        if (can_not_eval_count > 0)
        {
            in_values_selectivity += 1.0 / can_not_eval_count;
        }
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createInFilter(values, has_null_value)}};
        return {in_values_selectivity, filtered_symbol_statistics};
    }
    else if (symbol_statistics.isString())
    {
        std::set<double> str_values;
        bool has_null_value = false;
        for (auto & child : tuple.arguments->getChildren())
        {
            if (auto eval_res = context.calculateConstantExpression(child))
            {
                String str = symbol_statistics.toString(*eval_res);
                double value = CityHash_v1_0_2::CityHash64(str.data(), str.size());
                str_values.insert(value);
            }
            else
            {
                has_null_value = true;
            }
        }
        double in_values_selectivity = symbol_statistics.estimateInFilter(str_values, has_null_value, stats.getRowCount());
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createInFilter(str_values, has_null_value)}};
        return {in_values_selectivity, filtered_symbol_statistics};
    }
    return {1.0, {}};
}

FilterEstimateResult
FilterEstimator::estimateNotInFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext & context)
{
    auto & function = predicate->as<ASTFunction &>();
    bool match = function.arguments->getChildren()[0]->as<ASTIdentifier>() && function.arguments->getChildren()[1]->as<ASTFunction>();
    if (!match)
    {
        return {1.0, {}};
    }

    ASTIdentifier & identifier = function.arguments->getChildren()[0]->as<ASTIdentifier &>();
    ASTFunction & tuple = function.arguments->getChildren()[1]->as<ASTFunction &>();

    String symbol = identifier.name();

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);
    // No statistics for symbol
    if (symbol_statistics.isUnknown())
    {
        return {1.0, {}};
    }

    if (symbol_statistics.isNumber())
    {
        std::set<double> values;
        bool has_null_value = false;
        int can_not_eval_count = 0;
        for (auto & child : tuple.arguments->getChildren())
        {
            if (auto eval_res = context.calculateConstantExpression(child))
            {
                if (!eval_res->isNull())
                {
                    try
                    {
                        auto cast_result = castStringType(symbol_statistics, *eval_res, context);
                        if (cast_result.has_value() && !cast_result->isNull())
                        {
                            eval_res = cast_result;
                        }
                        if (eval_res)
                        {
                            double value = symbol_statistics.toDouble(*eval_res);
                            values.emplace(value);
                        }
                    }
                    catch (...)
                    {
                        // ignore
                        can_not_eval_count += 1;
                    }
                }
                else
                {
                    has_null_value = true;
                }
            }
        }
        double not_in_values_selectivity = symbol_statistics.estimateNotInFilter(values, has_null_value, stats.getRowCount());
        if (can_not_eval_count > 0)
        {
            not_in_values_selectivity -= 1.0 / can_not_eval_count;
        }
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createNotInFilter(values, has_null_value)}};
        return {not_in_values_selectivity, filtered_symbol_statistics};
    }
    else if (symbol_statistics.isString())
    {
        std::set<double> str_values;
        bool has_null_value = false;
        for (auto & child : tuple.arguments->getChildren())
        {
            if (auto eval_res = context.calculateConstantExpression(child))
            {
                if (!eval_res->isNull())
                {
                    String str = symbol_statistics.toString(*eval_res);
                    double value = CityHash_v1_0_2::CityHash64(str.data(), str.size());
                    str_values.insert(value);
                }
                else
                {
                    has_null_value = true;
                }
            }
        }
        double not_in_values_selectivity = symbol_statistics.estimateNotInFilter(str_values, has_null_value, stats.getRowCount());
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics
            = {{symbol, symbol_statistics.createNotInFilter(str_values, has_null_value)}};
        return {not_in_values_selectivity, filtered_symbol_statistics};
    }
    return {1.0, {}};
}

FilterEstimateResult FilterEstimator::estimateNullFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext &)
{
    auto & function = predicate->as<ASTFunction &>();
    ConstASTPtr left = tryGetIdentifier(function.arguments->getChildren()[0]);
    bool match = left->as<ASTIdentifier>();

    if (!match)
    {
        return {1.0, {}};
    }

    auto & identifier = left->as<ASTIdentifier &>();
    String symbol = identifier.name();

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);
    if (symbol_statistics.isUnknown())
    {
        // No statistics for symbol
        return {1.0, {}};
    }

    double selectivity = 1.0;
    if (symbol_statistics.isNullable())
    {
        selectivity = symbol_statistics.estimateNullFilter(stats.getRowCount());
        std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics = {{symbol, symbol_statistics.createNullFilter()}};
        return {selectivity, filtered_symbol_statistics};
    }

    // if data type is not nullable, null filter will return empty.
    selectivity = 0.0;
    std::unordered_map<std::string, SymbolStatisticsPtr> symbol_stats = {{symbol, symbol_statistics.createEmpty()}};
    return {selectivity, symbol_stats};
}

FilterEstimateResult FilterEstimator::estimateNotNullFilter(PlanNodeStatistics & stats, ConstASTPtr & predicate, FilterEstimatorContext &)
{
    auto & function = predicate->as<ASTFunction &>();

    ConstASTPtr left = tryGetIdentifier(function.arguments->getChildren()[0]);
    bool match = left->as<ASTIdentifier>();

    if (!match)
    {
        return {1.0, {}};
    }

    auto & identifier = left->as<ASTIdentifier &>();
    String symbol = identifier.name();

    SymbolStatistics & symbol_statistics = *stats.getSymbolStatistics(symbol);
    if (symbol_statistics.isUnknown())
    {
        // No statistics for symbol
        return {1.0, {}};
    }

    double selectivity = 1.0;
    if (!symbol_statistics.isNullable())
    {
        return {1.0, {}};
    }

    selectivity = symbol_statistics.estimateNotNullFilter(stats.getRowCount());
    std::unordered_map<std::string, SymbolStatisticsPtr> filtered_symbol_statistics = {{symbol, symbol_statistics.createNotNullFilter()}};
    return {selectivity, filtered_symbol_statistics};
}

// TODO support dynamic sample for complex predicate @gouguiling
FilterEstimateResult FilterEstimator::estimateLikeFilter(PlanNodeStatistics &, ConstASTPtr &, FilterEstimatorContext &)
{
    return {DEFAULT_SELECTIVITY, {}};
}

// TODO support dynamic sample for complex predicate @gouguiling
FilterEstimateResult FilterEstimator::estimateNotLikeFilter(PlanNodeStatistics &, ConstASTPtr &, FilterEstimatorContext &)
{
    return {DEFAULT_SELECTIVITY, {}};
}

}
