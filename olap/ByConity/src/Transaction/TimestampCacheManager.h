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

#include <map>
#include <memory>
#include <mutex>

#include <Core/Types.h>
#include <Core/UUID.h>
#include <Transaction/TimestampCache.h>

namespace DB
{
class TimestampCacheTableGuard
{
public:
    // TODO: add currently waiting txn_ids for entry
    struct Entry
    {
        std::unique_ptr<std::mutex> mutex;
        UInt32 counter;
    };

    using TableMutexMap = std::map<UUID, Entry>;

    TimestampCacheTableGuard(TableMutexMap & map_, std::unique_lock<std::mutex> guard_lock_, const UUID & uuid);
    ~TimestampCacheTableGuard();

private:
    TableMutexMap & map;
    TableMutexMap::iterator it;
    std::unique_lock<std::mutex> guard_lock;
    std::unique_lock<std::mutex> table_lock;
};

class TimestampCacheManager
{
public:
    explicit TimestampCacheManager(size_t max_size_) : max_size(max_size_) {}
    ~TimestampCacheManager() = default;

    std::unique_ptr<TimestampCacheTableGuard> getTimestampCacheTableGuard(const UUID & uuid);

    TimestampCachePtr & getTimestampCacheUnlocked(const UUID & uuid);

private:
    mutable std::mutex mutex;
    TimestampCacheTableGuard::TableMutexMap table_guard;
    std::unordered_map<UUID, TimestampCachePtr> tsCaches;
    size_t max_size;
};

using TimestampCacheManagerPtr = std::unique_ptr<TimestampCacheManager>;

}
