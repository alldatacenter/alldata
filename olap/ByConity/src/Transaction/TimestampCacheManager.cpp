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

#include "TimestampCacheManager.h"
#include <common/logger_useful.h>

namespace DB
{
TimestampCacheTableGuard::TimestampCacheTableGuard(TableMutexMap & map_, std::unique_lock<std::mutex> guard_lock_, const UUID & uuid)
    : map(map_), guard_lock(std::move(guard_lock_))
{
    it = map.emplace(uuid, Entry{std::make_unique<std::mutex>(), 0}).first;
    ++it->second.counter;
    guard_lock.unlock();
    table_lock = std::unique_lock<std::mutex>(*it->second.mutex);
}

TimestampCacheTableGuard::~TimestampCacheTableGuard()
{
    guard_lock.lock();
    --it->second.counter;
    if (!it->second.counter)
    {
        table_lock.unlock();
        map.erase(it);
    }
}

std::unique_ptr<TimestampCacheTableGuard> TimestampCacheManager::getTimestampCacheTableGuard(const UUID & uuid)
{
    std::unique_lock<std::mutex> lock(mutex);
    return std::make_unique<TimestampCacheTableGuard>(table_guard, std::move(lock), uuid);
}

TimestampCachePtr & TimestampCacheManager::getTimestampCacheUnlocked(const UUID & uuid)
{
    if (tsCaches.find(uuid) == tsCaches.end())
        tsCaches.emplace(uuid, std::make_unique<TimestampCache>(max_size));

    return tsCaches[uuid];
}

}
