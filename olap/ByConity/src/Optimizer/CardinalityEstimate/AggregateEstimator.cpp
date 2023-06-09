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

#include <Optimizer/CardinalityEstimate/AggregateEstimator.h>

namespace DB
{
static double estimateGroupBy(
    std::unordered_map<String, SymbolStatisticsPtr> & symbol_statistics, PlanNodeStatisticsPtr & child_stats, const Names & group_keys)
{
    double row_count = 1;
    bool all_unknown = true;
    for (const auto & key : group_keys)
    {
        if (child_stats->getSymbolStatistics().contains(key) && !child_stats->getSymbolStatistics(key)->isUnknown())
        {
            auto key_stats = child_stats->getSymbolStatistics(key)->copy();
            int null_rows
                = child_stats->getRowCount() == 0 || (double(key_stats->getNullsCount()) / child_stats->getRowCount() == 0.0) ? 0 : 1;
            if (key_stats->getNdv() > 0)
            {
                row_count *= static_cast<double>(key_stats->getNdv()) + null_rows;
            }
            symbol_statistics[key] = key_stats;
            all_unknown = false;
        }
    }

    if (!group_keys.empty() && all_unknown)
    {
        row_count = child_stats->getRowCount();
    }

    row_count = std::min(row_count, double(child_stats->getRowCount()));

    for (auto & item : symbol_statistics)
    {
        auto & stat = item.second;
        double adjust_row_count = double(row_count) / child_stats->getRowCount();
        stat = stat->applySelectivity(adjust_row_count, 1.0);
    }

    return row_count;
}

PlanNodeStatisticsPtr AggregateEstimator::estimate(PlanNodeStatisticsPtr & child_stats, const AggregatingStep & step)
{
    if (!child_stats)
    {
        return nullptr;
    }
    std::unordered_map<String, SymbolStatisticsPtr> symbol_statistics;
    const Names & group_keys = step.getKeys();

    double row_count = estimateGroupBy(symbol_statistics, child_stats, group_keys);

    std::unordered_map<String, DataTypePtr> name_to_type;
    for (const auto & item : step.getOutputStream().header)
    {
        name_to_type[item.name] = item.type;
    }
    const AggregateDescriptions & agg_descs = step.getAggregates();
    for (const auto & agg_desc : agg_descs)
    {
        symbol_statistics[agg_desc.column_name]
            = AggregateEstimator::estimateAggFun(agg_desc.function, row_count, name_to_type[agg_desc.column_name]);
    }

    return std::make_shared<PlanNodeStatistics>(row_count, std::move(symbol_statistics));
}

PlanNodeStatisticsPtr AggregateEstimator::estimate(PlanNodeStatisticsPtr & child_stats, const MergingAggregatedStep & step)
{
    if (!child_stats)
    {
        return nullptr;
    }

    std::unordered_map<String, SymbolStatisticsPtr> symbol_statistics;
    const Names & group_keys = step.getKeys();
    double row_count = estimateGroupBy(symbol_statistics, child_stats, group_keys);

    const AggregateDescriptions & agg_descs = step.getAggregates();
    std::unordered_map<String, DataTypePtr> name_to_type;
    for (const auto & item : step.getOutputStream().header)
    {
        name_to_type[item.name] = item.type;
    }
    for (const auto & agg_desc : agg_descs)
    {
        symbol_statistics[agg_desc.column_name]
            = AggregateEstimator::estimateAggFun(agg_desc.function, row_count, name_to_type[agg_desc.column_name]);
    }

    return std::make_shared<PlanNodeStatistics>(row_count, std::move(symbol_statistics));
}

PlanNodeStatisticsPtr AggregateEstimator::estimate(PlanNodeStatisticsPtr & child_stats, const DistinctStep & step)
{
    if (!child_stats)
    {
        return nullptr;
    }

    std::unordered_map<String, SymbolStatisticsPtr> symbol_statistics;
    const auto & columns = step.getColumns();
    UInt64 limit = step.getSetSizeLimits().max_rows;
    double row_count = estimateGroupBy(symbol_statistics, child_stats, columns);

    // distinct limit
    if (limit != 0)
    {
        row_count = std::min(row_count, double(limit));
    }

    return std::make_shared<PlanNodeStatistics>(row_count, std::move(symbol_statistics));
}

SymbolStatisticsPtr AggregateEstimator::estimateAggFun(AggregateFunctionPtr fun, UInt64 row_count, DataTypePtr data_type)
{
    SymbolStatistics statistics{row_count, 0, 0, 0, 0, {}, data_type, fun->getName(), false};
    return std::make_shared<SymbolStatistics>(statistics);
}

}
