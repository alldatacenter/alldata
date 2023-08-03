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

#include <Core/Types.h>
#include <Interpreters/Context_fwd.h>
#include <Interpreters/StorageID.h>
#include <Storages/IStorage_fwd.h>
#include <Databases/IDatabase.h>

#include <unordered_map>
#include <unordered_set>
#include <mutex>


namespace DB
{

class CnchWorkerServiceImpl;

class CnchWorkerResource
{
public:
    void executeCreateQuery(ContextMutablePtr context, const String & create_query, bool skip_if_exists = false);
    StoragePtr getTable(const StorageID & table_id) const;
    DatabasePtr getDatabase(const String & database_name) const;
    bool isCnchTableInWorker(const StorageID & table_id) const;

    void clearResource();

    friend class CnchWorkerServiceImpl;

private:
    auto getLock() const { return std::lock_guard(mutex); }

    using DatabaseAndTableName = std::pair<String, String>;
    struct DatabaseAndTableNameHash
    {
        size_t operator()(const DatabaseAndTableName & key) const
        {
            SipHash hash;
            hash.update(key.first);
            hash.update(key.second);
            return hash.get64();
        }
    };

    mutable std::mutex mutex;

    std::unordered_map<DatabaseAndTableName, StoragePtr, DatabaseAndTableNameHash> cloud_tables;
    std::unordered_map<String, DatabasePtr> memory_databases;

    /// for offloading query
    std::unordered_set<DatabaseAndTableName, DatabaseAndTableNameHash> cnch_tables;
    std::map<UUID, String> worker_table_names;
};

}
