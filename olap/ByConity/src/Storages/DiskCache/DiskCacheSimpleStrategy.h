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

#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/DiskCache/IDiskCacheStrategy.h>

#include <mutex>
#include <unordered_map>

namespace DB
{
class DiskCacheSimpleStrategy : public IDiskCacheStrategy
{
public:
    explicit DiskCacheSimpleStrategy(const DiskCacheStrategySettings & settings_)
        : IDiskCacheStrategy(settings_)
        , cache_statistics(settings_.stats_bucket_size)
        , segment_hits_to_cache(settings_.hits_to_cache)
        , logger(&Poco::Logger::get("DiskCacheSimpleStrategy"))
    {
    }

    virtual IDiskCacheSegmentsVector getCacheSegments(const IDiskCacheSegmentsVector & segments) override;

private:
    struct AccessStatistics
    {
        std::mutex stats_mutex;
        std::unordered_map<String, UInt32> access_stats;
    };

    struct CacheStatistics
    {
        explicit CacheStatistics(size_t bucket_size) : buckets(bucket_size) { }
        AccessStatistics & getAccessStats(const String & key) { return buckets[hasher(key) % buckets.size()]; }

        std::hash<String> hasher;
        std::vector<AccessStatistics> buckets;
    };

    CacheStatistics cache_statistics;

    size_t segment_hits_to_cache;
    Poco::Logger * logger;
};

}
