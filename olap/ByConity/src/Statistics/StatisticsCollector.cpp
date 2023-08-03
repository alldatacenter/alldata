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

#include <set>
#include <tuple>
#include <vector>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Statistics/CacheManager.h>
#include <Statistics/CachedStatsProxy.h>
#include <Statistics/CollectStep.h>
#include <Statistics/StatisticsCollector.h>
#include <Statistics/StatsNdvBucketsImpl.h>
#include <Statistics/SubqueryHelper.h>
#include <Statistics/TypeUtils.h>
#include <boost/algorithm/string/join.hpp>
#include <common/logger_useful.h>

namespace DB::Statistics
{

void StatisticsCollector::collect(const ColumnDescVector & cols_desc)
{
    auto step = [&] {
        if (settings.enable_sample)
        {
            return createStatisticsCollectorStepSample(*this);
        }
        else
        {
            return createStatisticsCollectorStepFull(*this);
        }
    }();

    step->collect(cols_desc);
    step->writeResult(table_stats, columns_stats);
}

void StatisticsCollector::writeToCatalog()
{
    if (!catalog)
    {
        throw Exception("catalog is NULL", ErrorCodes::LOGICAL_ERROR);
    }
    StatsData data;
    data.table_stats = table_stats.writeToCollection();
    for (auto & [name, stats] : columns_stats)
    {
        data.column_stats[name] = stats.writeToCollection();
    }
    auto proxy = createCachedStatsProxy(catalog);
    proxy->put(table_info, std::move(data));
    catalog->invalidateClusterStatsCache(table_info);
}

void StatisticsCollector::readAllFromCatalog()
{
    auto proxy = createCachedStatsProxy(catalog);
    auto data = proxy->get(table_info);
    table_stats.readFromCollection(data.table_stats);
    for (auto & [name, stats] : data.column_stats)
    {
        columns_stats[name].readFromCollection(stats);
    }
}

void StatisticsCollector::readFromCatalog(const std::vector<String> & cols_name)
{
    auto cols_desc = filterCollectableColumns(catalog->getCollectableColumns(table_info), cols_name);
    this->readFromCatalogImpl(cols_desc);
}

void StatisticsCollector::readFromCatalogImpl(const ColumnDescVector & cols_desc)
{
    auto proxy = createCachedStatsProxy(catalog);
    auto data = proxy->get(table_info, true, cols_desc);
    if (data.table_stats.empty() && data.column_stats.empty())
    {
        // no stats collected, do nothing
        return;
    }

    table_stats.readFromCollection(data.table_stats);
    for (auto & [name, type] : cols_desc)
    {
        if (!data.column_stats.count(name))
        {
            // TODO: give a warning
            continue;
        }
        (void)type;
        auto & stats = data.column_stats.at(name);
        columns_stats[name].readFromCollection(stats);
    }
}

std::optional<PlanNodeStatisticsPtr> StatisticsCollector::toPlanNodeStatistics() const
{
    if (!table_stats.basic)
    {
        // return empty
        return std::nullopt;
    }

    auto result = std::make_shared<PlanNodeStatistics>();
    auto table_row_count = table_stats.basic->getRowCount();
    result->updateRowCount(table_row_count);
    // whether to construct single bucket histogram from min/max if there is no histogram
    for (const auto & [col, stats] : columns_stats)
    {
        auto symbol = std::make_shared<SymbolStatistics>();

        if (stats.basic)
        {
            auto nonnull_count = stats.basic->getProto().nonnull_count();
            symbol->null_counts = table_row_count - nonnull_count;
            symbol->min = stats.basic->getProto().min_as_double();
            symbol->max = stats.basic->getProto().max_as_double();
            symbol->ndv = AdjustNdvWithCount(stats.basic->getProto().ndv_value(), nonnull_count);
            symbol->unknown = false;
            auto construct_single_bucket_histogram = symbol->ndv == 1;

            if (stats.ndv_buckets_result)
            {
                stats.ndv_buckets_result->writeSymbolStatistics(*symbol);
            }
            else if (construct_single_bucket_histogram)
            {
                auto bucket = Bucket(symbol->min, symbol->max, symbol->ndv, nonnull_count, true, true);
                symbol->histogram.emplaceBackBucket(std::move(bucket));
            }
            result->updateSymbolStatistics(col, symbol);
        }
    }
    return result;
}

}
