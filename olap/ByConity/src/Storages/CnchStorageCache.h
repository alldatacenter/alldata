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

#include <Common/LRUCache.h>
#include <shared_mutex>
#include <city.h>

namespace DB
{

class IStorage;
using StoragePtr = std::shared_ptr<IStorage>;

using TableName = std::pair<String, String>;  // database and table pair
using TableData = std::pair<UInt64, StoragePtr>; //identify table with commit time

struct TableNameHash
{
    size_t operator()(const TableName & key) const
    {
        return CityHash_v1_0_2::CityHash64(key.first.data(), key.first.length()) ^ CityHash_v1_0_2::CityHash64(key.second.data(), key.second.length());
    }
};


class CnchStorageCache : public LRUCache<TableName, TableData, TableNameHash>
{
private:
    using Base = LRUCache<TableName, TableData, TableNameHash>;
    std::shared_mutex cache_mutex;

public:
    CnchStorageCache(size_t cache_size)
        : Base(cache_size)
    {
        inner_container = std::make_unique<CacheContainer<Key>>();
    }

    /***
     * Insert storage into cache.
     * @param db databasename
     * @param table tablename
     * @param ts timestamp
     * @param storage_ptr storageptr
     */
    void insert(const String & db, const String & table, const UInt64 ts, const StoragePtr & storage_ptr);

    /***
     * Get storage from cache.
     * @param db databasename
     * @param table tablename
     * @return nullptr if the storage is not cached.
     */

    StoragePtr get(const String & db, const String & table);

    /***
     * Remove the storage cache of the table.
     * @param db databasename
     * @param table tablename
     */
    void remove(const String & db, const String & table);

    /***
     * Remove all storages cache in the database;
     * @param db databasename
     */
    void remove(const String & db);

    /***
     * Clear all cached storage.
     */
    void clear();
};

using CnchStorageCachePtr = std::shared_ptr<CnchStorageCache>;

}
