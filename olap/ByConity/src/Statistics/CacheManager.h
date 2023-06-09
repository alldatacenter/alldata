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

#include <DataTypes/DataTypeUUID.h>
#include <Interpreters/Context.h>
#include <Statistics/StatisticsBase.h>
#include <Statistics/StatsTableIdentifier.h>
#include <Poco/ExpireLRUCache.h>
#include <Common/HashTable/Hash.h>

namespace DB::Statistics
{

class CacheManager
{
public:
    struct KeyHash
    {
        auto operator()(const std::pair<UUID, String> & key) const
        {
            return std::hash<UUID>()(key.first) ^ std::hash<String>()(key.second);
        }
    };
    using CacheType = Poco::ExpireLRUCache<std::pair<UUID, String>, StatsCollection>;

    static void initialize(ContextPtr context);
    // for testing
    static void initialize(UInt64 entry_size, std::chrono::seconds expire_time);

    static CacheType & instance()
    {
        if (!cache)
        {
            throw Exception("cache has to be initialized", ErrorCodes::LOGICAL_ERROR);
        }
        return *cache;
    }

    // invalidate cache on current server
    static void invalidate(const ContextPtr context, const StatsTableIdentifier & table);

private:
    static std::unique_ptr<CacheType> cache;
};

}
