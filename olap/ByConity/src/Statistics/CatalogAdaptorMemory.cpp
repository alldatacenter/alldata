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

#include <algorithm>
#include <memory>
#include <optional>
#include <string>
#include <Statistics/CacheManager.h>
#include <Statistics/CatalogAdaptor.h>
#include <Statistics/SerdeUtils.h>
#include <Statistics/StatisticsCollector.h>
#include <Statistics/StatisticsMemoryStore.h>
#include <Statistics/TypeUtils.h>
#include <boost/noncopyable.hpp>
#include <fmt/format.h>
#include <common/logger_useful.h>

namespace DB::Statistics
{

class CatalogAdaptorMemory : public CatalogAdaptor
{
public:
    CatalogAdaptorMemory(ContextPtr context_, std::shared_ptr<StatisticsMemoryStore> sms_ptr)
        : context(context_), statistics_memory_store(sms_ptr)
    {
    }

    bool hasStatsData(const StatsTableIdentifier & table) override
    {
        auto & sms = getStatisticsMemoryStore();
        std::shared_lock lck(sms.mtx);
        auto key = table.getUniqueKey();
        /// return whether table_stats of the corresponding table is non-empty
        return sms.entries.count(key);
    }

    StatsData readStatsData(const StatsTableIdentifier & table) override
    {
        auto & sms = getStatisticsMemoryStore();
        std::shared_lock lck(sms.mtx);
        auto key = table.getUniqueKey();

        if (!sms.entries.count(key))
        {
            return {};
        }

        return sms.entries.at(key)->data;
    }

    StatsCollection readSingleStats(const StatsTableIdentifier & table, const std::optional<String> & column_name_opt) override
    {
        auto & sms = getStatisticsMemoryStore();
        std::shared_lock lck(sms.mtx);
        auto key = table.getUniqueKey();

        if (!sms.entries.count(key))
        {
            return {};
        }

        auto & entry_data = sms.entries.at(key)->data;

        if (!column_name_opt.has_value())
        {
            return entry_data.table_stats;
        }
        else
        {
            auto column_name = *column_name_opt;
            auto it = entry_data.column_stats.find(column_name);
            if (it != entry_data.column_stats.end())
            {
                return it->second;
            }
            else
            {
                return {};
            }
        }
    }

    ColumnDescVector getCollectableColumns(const StatsTableIdentifier & identifier) const override
    {
        ColumnDescVector result;
        auto storage = getStorageByTableId(identifier);
        auto snapshot = storage->getInMemoryMetadataPtr();
        for (const auto & name_type_pr : snapshot->getColumns().getAll())
        {
            if (!Statistics::isCollectableType(name_type_pr.type))
            {
                continue;
            }
            result.emplace_back(name_type_pr);
        }
        return result;
    }

    // note: new
    void writeStatsData(const StatsTableIdentifier & table, const StatsData & stats_data) override
    {
        // meta.getEntry(table.getUniqueKey())->data = stats_data;

        auto & sms = getStatisticsMemoryStore();
        std::unique_lock lck(sms.mtx);
        auto key = table.getUniqueKey();
        if (!sms.entries.count(key))
        {
            // create new instance
            auto new_entry = std::make_shared<TableEntry>(TableEntry{table, {}});
            sms.entries.emplace(key, new_entry);
        }
        assert(sms.entries.count(key));
        auto & target = sms.entries.at(key)->data;

        if (!stats_data.table_stats.empty())
        {
            target.table_stats = stats_data.table_stats;
        }

        for (auto & [column_name, column_stats] : stats_data.column_stats)
        {
            if (!column_stats.empty())
            {
                target.column_stats[column_name] = column_stats;
            }
        }
    }

    void dropStatsColumnData(const StatsTableIdentifier & table, const ColumnDescVector & cols_desc) override
    {
        auto & sms = getStatisticsMemoryStore();
        std::unique_lock lck(sms.mtx);
        auto key = table.getUniqueKey();
        if (sms.entries.count(key))
        {
            auto & entry = sms.entries.at(key);
            if (!entry)
                return;
            for (auto & col_desc : cols_desc)
            {
                entry->data.column_stats.erase(col_desc.name);
            }
        }
    }

    void dropStatsData(const StatsTableIdentifier & table) override
    {
        auto & sms = getStatisticsMemoryStore();
        std::unique_lock lck(sms.mtx);
        auto key = table.getUniqueKey();
        sms.entries.erase(key);
    }

    void dropStatsDataAll(const String & database) override
    {
        auto tables = getAllTablesID(database);
        for (auto & table : tables)
        {
            dropStatsData(table);
        }
    }

    void invalidateClusterStatsCache(const StatsTableIdentifier & table) override
    {
        // for memory catalog, there should be only a single server
        Statistics::CacheManager::invalidate(context, table);
    }

    void invalidateServerStatsCache(const StatsTableIdentifier & table) const override
    {
        Statistics::CacheManager::invalidate(context, table);
    }


    std::vector<StatsTableIdentifier> getAllTablesID(const String & database_name) const override
    {
        std::vector<StatsTableIdentifier> results;
        auto db = DatabaseCatalog::instance().getDatabase(database_name);
        for (auto iter = db->getTablesIterator(context); iter->isValid(); iter->next())
        {
            auto table = iter->table();
            StatsTableIdentifier table_id(table->getStorageID());
            results.emplace_back(table_id);
        }
        return results;
    }

    std::optional<StatsTableIdentifier> getTableIdByName(const String & database_name, const String & table_name) const override
    {
        auto & ins = DatabaseCatalog::instance();
        auto db_storage = ins.getDatabase(database_name);
        auto table = db_storage->tryGetTable(table_name, context);
        if (!table)
        {
            return std::nullopt;
        }
        auto result = table->getStorageID();

        return StatsTableIdentifier(result);
    }

    StoragePtr getStorageByTableId(const StatsTableIdentifier & identifier) const override
    {
        auto & ins = DatabaseCatalog::instance();
        return ins.getTable(identifier.getStorageID(), context);
    }

    UInt64 getUpdateTime() override
    {
        // never use
        return 0;
    }

    const Settings & getSettingsRef() const override { return context->getSettingsRef(); }

private:
    StatisticsMemoryStore & getStatisticsMemoryStore() { return *statistics_memory_store; }
    ContextPtr context;
    std::shared_ptr<StatisticsMemoryStore> statistics_memory_store;
};

CatalogAdaptorPtr createCatalogAdaptorMemory(ContextPtr query_context)
{
    if (query_context->hasSessionContext())
    {
        auto session_context = query_context->getSessionContext();
        auto sms = session_context->getStatisticsMemoryStore();
        return std::make_shared<CatalogAdaptorMemory>(query_context, sms);
    }
    else
    {
        // for test environment
        static auto sms_static = std::make_shared<StatisticsMemoryStore>();
        return std::make_shared<CatalogAdaptorMemory>(query_context, sms_static);
    }
}
}
