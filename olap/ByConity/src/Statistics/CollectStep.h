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
#include <memory>
#include <Statistics/CollectorSettings.h>
#include <Statistics/ParseUtils.h>
#include <Statistics/StatisticsCollectorObjects.h>
#include <Statistics/StatsTableIdentifier.h>

namespace DB::Statistics
{

class StatisticsCollector;


struct HandlerColumnData
{
    double nonnull_count = 0;
    // when scaleNdv output unreliable result, this is null
    std::optional<double> ndv_value_opt = std::nullopt;
    double min_as_double = 0;
    double max_as_double = 0;
    std::shared_ptr<BucketBounds> bucket_bounds; // RowCountHandler will write this
    std::optional<std::shared_ptr<StatsNdvBucketsResult>> ndv_buckets_result_opt;
};


struct HandlerContext : boost::noncopyable
{
    HandlerContext(const CollectorSettings & settings_) : settings(settings_) { }

    const CollectorSettings & settings;
    double full_count = 0;
    std::optional<double> query_row_count;
    std::unordered_map<String, HandlerColumnData> columns_data;
};
// count all row
class RowCountHandler : public ColumnHandlerBase
{
public:
    RowCountHandler(HandlerContext & handler_context_) : handler_context(handler_context_) { }

    std::vector<String> getSqls() override { return {"count(*)"}; }
    void parse(const Block & block, size_t index_offset) override
    {
        auto result = block.getByPosition(index_offset).column->getUInt(0);
        handler_context.query_row_count = result;
    }

    size_t size() override { return 1; }

private:
    HandlerContext & handler_context;
};

class CollectStep
{
public:
    using TableStats = StatisticsImpl::TableStats;
    using ColumnStats = StatisticsImpl::ColumnStats;
    using ColumnStatsMap = StatisticsImpl::ColumnStatsMap;

    explicit CollectStep(StatisticsCollector & core_);

    void collectTable();

    virtual void collect(const ColumnDescVector & col_names) = 0;
    virtual ~CollectStep() = default;

    void writeResult(TableStats & core_table_stats, ColumnStatsMap & core_columns_stats);

protected:
    StatisticsCollector & core;
    StatsTableIdentifier table_info;
    CatalogAdaptorPtr catalog;
    ContextPtr context;
    HandlerContext handler_context;
};

std::unique_ptr<CollectStep> createStatisticsCollectorStepSample(StatisticsCollector & core);
std::unique_ptr<CollectStep> createStatisticsCollectorStepFull(StatisticsCollector & core);
}
