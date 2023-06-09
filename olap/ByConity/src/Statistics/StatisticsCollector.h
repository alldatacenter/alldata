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
#include <utility>
#include <Core/Types.h>
#include <Interpreters/Context.h>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/Base64.h>
#include <Statistics/CatalogAdaptor.h>
#include <Statistics/CollectorSettings.h>
#include <Statistics/StatisticsCollectorObjects.h>
#include <common/logger_useful.h>

namespace DB::Statistics
{

// uuid level
class StatisticsCollector
{
public:
    friend class CollectStep;

    using TableStats = StatisticsImpl::TableStats;
    using ColumnStats = StatisticsImpl::ColumnStats;
    using ColumnStatsMap = StatisticsImpl::ColumnStatsMap;

    StatisticsCollector(
        ContextPtr context_, CatalogAdaptorPtr catalog_, const StatsTableIdentifier & table_info_, const CollectorSettings & settings_)
        : context(context_), catalog(catalog_), table_info(table_info_), settings(settings_)
    {
        logger = &Poco::Logger::get("StatisticsLogger" + table_info.getDbTableName());
    }

    // use default settings
    StatisticsCollector(ContextPtr context_, CatalogAdaptorPtr catalog_, const StatsTableIdentifier & table_info_)
        : StatisticsCollector(context_, catalog_, table_info_, CollectorSettings(context_->getSettingsRef()))
    {
    }

    void collect(const ColumnDescVector & col_names);

    void writeToCatalog();
    void readAllFromCatalog();
    void readFromCatalog(const std::vector<String> & cols_name);
    void readFromCatalogImpl(const ColumnDescVector & cols_desc);

    std::optional<PlanNodeStatisticsPtr> toPlanNodeStatistics() const;

    const auto & getTableStats() const { return table_stats; }
    const auto & getColumnsStats() const { return columns_stats; }
    void setTableStats(TableStats && stats) { table_stats = std::move(stats); }
    void setColumnStats(String col_name, ColumnStats && col_stats) { columns_stats[col_name] = std::move(col_stats); }

private:
    ContextPtr context;
    Poco::Logger * logger;
    CatalogAdaptorPtr catalog;
    StatsTableIdentifier table_info;

    // table stats
    TableStats table_stats;

    // column stats
    ColumnStatsMap columns_stats;
    CollectorSettings settings;
};
}
