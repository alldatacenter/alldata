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
#include <unordered_map>
#include <unordered_set>
#include <Statistics/StatisticsBase.h>
#include <Statistics/StatsTableIdentifier.h>
#include <Statistics/TypeUtils.h>
#include <Storages/IStorage.h>

namespace DB::Statistics
{
class CatalogAdaptor
{
public:
    virtual bool hasStatsData(const StatsTableIdentifier & table) = 0;
    virtual StatsData readStatsData(const StatsTableIdentifier & table) = 0;
    virtual StatsCollection readSingleStats(const StatsTableIdentifier & table, const std::optional<String> & column_name) = 0;
    virtual void writeStatsData(const StatsTableIdentifier & table, const StatsData & stats_data) = 0;

    virtual void dropStatsColumnData(const StatsTableIdentifier & table, const ColumnDescVector & cols_desc) = 0;
    virtual void dropStatsData(const StatsTableIdentifier & table) = 0;
    virtual void dropStatsDataAll(const String & database) = 0;

    virtual void invalidateClusterStatsCache(const StatsTableIdentifier & table) = 0;
    // const because it should use ConstContext
    virtual void invalidateServerStatsCache(const StatsTableIdentifier & table) const = 0;

    virtual std::vector<StatsTableIdentifier> getAllTablesID(const String & database_name) const = 0;
    virtual std::optional<StatsTableIdentifier> getTableIdByName(const String & database_name, const String & table) const = 0;
    virtual StoragePtr getStorageByTableId(const StatsTableIdentifier & identifier) const = 0;
    virtual UInt64 getUpdateTime() = 0;
    virtual ColumnDescVector getCollectableColumns(const StatsTableIdentifier & identifier) const = 0;
    virtual const Settings & getSettingsRef() const = 0;

    virtual bool isTableCollectable(const StatsTableIdentifier & table) const
    {
        (void)table;
        return true;
    }

    virtual bool isTableAutoUpdated(const StatsTableIdentifier & table) const
    {
        (void)table;
        return false;
    }

    virtual void checkHealth(bool is_write) { (void)is_write; }
    virtual ~CatalogAdaptor() = default;
};

using CatalogAdaptorPtr = std::shared_ptr<CatalogAdaptor>;
using ConstCatalogAdaptorPtr = std::shared_ptr<const CatalogAdaptor>;
CatalogAdaptorPtr createCatalogAdaptor(ContextPtr context);
inline ConstCatalogAdaptorPtr createConstCatalogAdaptor(ContextPtr context)
{
    // only const function can be used in this adaptor
    return createCatalogAdaptor(context);
}

}
