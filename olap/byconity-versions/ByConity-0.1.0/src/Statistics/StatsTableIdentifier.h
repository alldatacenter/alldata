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
#include <Core/Types.h>

#include <utility>
#include <Interpreters/StorageID.h>

// this can be very different for cnch/stable_v2
namespace DB::Statistics
{

class StatsTableIdentifier
{
public:
    using UniqueKey = UUID;
    explicit StatsTableIdentifier(StorageID storage_id_) : storage_id(storage_id_) { }
    StatsTableIdentifier(const StatsTableIdentifier &) = default;
    StatsTableIdentifier(StatsTableIdentifier &&) = default;
    StatsTableIdentifier & operator=(const StatsTableIdentifier &) = default;
    StatsTableIdentifier & operator=(StatsTableIdentifier &&) = default;
    const String & getDatabaseName() const { return storage_id.database_name; }
    const String & getTableName() const { return storage_id.table_name; }

    String getDbTableName() const { return storage_id.getFullTableName(); }
    UniqueKey getUniqueKey() const;
    StorageID getStorageID() const { return storage_id; }
    UUID getUUID() const { return storage_id.uuid; }

private:
    StorageID storage_id;
    // useful only for adaptor
};

}
