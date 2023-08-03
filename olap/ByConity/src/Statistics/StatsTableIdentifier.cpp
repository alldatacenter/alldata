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

#include <Statistics/StatsTableIdentifier.h>
#include <Storages/IStorage.h>

namespace DB::Statistics
{
auto StatsTableIdentifier::getUniqueKey() const -> UUID
{
    auto uuid = getUUID();

    if (uuid == UUID{})
    {
        // some table may return empty uuid, use hash for temporary fix
        auto hash_db = std::hash<String>()(getDatabaseName());
        auto hash_tb = std::hash<String>()(getTableName());
        hash_db = (hash_db & 0xffffffffffff0fffull) | 0x0000000000004000ull;
        hash_tb = (hash_tb & 0x3fffffffffffffffull) | 0x8000000000000000ull;

        uuid.toUnderType().items[0] = hash_db;
        uuid.toUnderType().items[1] = hash_tb;
    }
    return uuid;
}
}
