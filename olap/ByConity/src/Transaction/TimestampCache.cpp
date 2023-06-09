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

#include "TimestampCache.h"

#include "Common/ProfileEvents.h"
#include <Common/Stopwatch.h>
#include <Common/ProfileEvents.h>

#include <algorithm>

namespace ProfileEvents
{
    extern const Event TsCacheCheckElapsedMilliseconds;
    extern const Event TsCacheUpdateElapsedMilliseconds;
}

namespace DB
{
TxnTimestamp TimestampCache::lookup(const String & part) const
{
    Stopwatch watch;
    auto it = find(part);
    ProfileEvents::increment(ProfileEvents::TsCacheCheckElapsedMilliseconds, watch.elapsedMilliseconds());
    if (it == end())
        return low_water();
    else
        return it->second;
}

TxnTimestamp TimestampCache::lookup(const Strings & parts) const
{
    Stopwatch watch;
    TxnTimestamp res = low_water();
    for (const auto & part : parts)
        res = std::max(res, lookup(part));

    ProfileEvents::increment(ProfileEvents::TsCacheCheckElapsedMilliseconds, watch.elapsedMilliseconds());
    return res;
}

void TimestampCache::insertOrAssign(const Strings & parts, const TxnTimestamp & timestamp)
{
    Stopwatch watch;
    for (const auto & part : parts)
        if (auto it = find(part); it != end())
            erase(it);

    auto it_before = lower_bound({"", timestamp});
    for (const auto & part : parts)
        insert(it_before, std::make_pair(part, timestamp));

    ProfileEvents::increment(ProfileEvents::TsCacheUpdateElapsedMilliseconds, watch.elapsedMicroseconds());
}

void TimestampCache::insertOrAssign(const String & part, const TxnTimestamp & timestamp)
{
    Stopwatch watch;
    auto res = emplace(part, timestamp);
    if (!res.second)
        res.first->second = timestamp;

    ProfileEvents::increment(ProfileEvents::TsCacheUpdateElapsedMilliseconds, watch.elapsedMicroseconds());
}

TxnTimestamp TimestampCache::low_water() const
{
    if (empty())
        return TxnTimestamp::minTS();
    else
        return back().second;
}

}
