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
#include <utility>

#include <Core/Types.h>
#include <Transaction/OrderedCache.h>
#include <Transaction/TxnTimestamp.h>

namespace DB
{
struct TimestampCacheComparator
{
    using TimestampCacheValue = std::pair<String, TxnTimestamp>;
    bool operator()(const TimestampCacheValue & lhs, const TimestampCacheValue & rhs) { return lhs.second > rhs.second; }
};

/*
Timestamp cache maintains 'last updated timestamp' for each part. It's used to solve transaction conflicts.
Timestamp cache has a capacity. If the size exceeds its capacity, entries will be evicted from cache based on FIFO.
Timestamp cache maintains a low-water mark, which is the oldest 'last updated timestamp' among all parts in the cache.
*/
class TimestampCache : public OrderedCache<String, TxnTimestamp, TimestampCacheComparator>
{
    using Base = OrderedCache<String, TxnTimestamp, TimestampCacheComparator>;

public:
    explicit TimestampCache(size_t max_size) : Base(max_size) { }

    TxnTimestamp lookup(const String & part) const;

    // return the latest 'last updated timestamp' for parts. If all parts are not found in cache, low_water mark will be returned.
    TxnTimestamp lookup(const Strings & parts) const;

    // If the part exists in cache, update last updated timestamp. If not exist, create an entry for this part
    void insertOrAssign(const Strings & parts, const TxnTimestamp & timestamp);
    void insertOrAssign(const String & part, const TxnTimestamp & timestamp);

    TxnTimestamp low_water() const;
};

using TimestampCachePtr = std::unique_ptr<TimestampCache>;
}
