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

#include <Storages/CnchStorageCache.h>
#include <Storages/IStorage.h>

namespace DB
{

void CnchStorageCache::insert(const String & db, const String & table, const UInt64 ts, const StoragePtr & storage_ptr)
{
    std::unique_lock<std::shared_mutex> lock(cache_mutex);

    TableName full_name = std::make_pair(db, table);
    auto cached = Base::get(full_name);
    if (cached && cached->first > ts)
        return;
    auto new_data = std::make_shared<TableData>(ts, storage_ptr);
    Base::set(full_name, new_data);
    if (inner_container)
        inner_container->insert(db, full_name);
}

StoragePtr CnchStorageCache::get(const String & db, const String & table)
{
    std::shared_lock<std::shared_mutex> lock(cache_mutex);
    auto table_data_ptr = Base::get(std::make_pair(db,table));
    if (table_data_ptr)
        return table_data_ptr->second;
    else
        return {};
}

void CnchStorageCache::remove(const String & db, const String & table)
{
    std::unique_lock<std::shared_mutex> lock(cache_mutex);
    Base::remove(std::make_pair(db, table));
}

void CnchStorageCache::remove(const String & db)
{
    std::unique_lock<std::shared_mutex> lock(cache_mutex);
    if (!inner_container)
        return;

    const auto & tables = inner_container->getKeys(db);
    for (const auto & table : tables)
        remove(table.first, table.second);
}

void CnchStorageCache::clear()
{
    std::unique_lock<std::shared_mutex> lock(cache_mutex);
    Base::reset();
}

}
