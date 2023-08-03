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

#include "DiskCacheSimpleStrategy.h"

#include <Common/CurrentMetrics.h>
#include <Common/ProfileEvents.h>
#include <Common/Stopwatch.h>
#include <common/scope_guard.h>

namespace CurrentMetrics
{
extern const Metric DiskCacheRoughSingleStatsBucketSize;
}

namespace ProfileEvents
{
extern const Event DiskCacheAcquireStatsLock;
extern const Event DiskCacheUpdateStatsMicroSeconds;
}

namespace DB
{
IDiskCacheSegmentsVector DiskCacheSimpleStrategy::getCacheSegments(const IDiskCacheSegmentsVector & segments)
{
    Stopwatch update_stats_watch;
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::DiskCacheUpdateStatsMicroSeconds, update_stats_watch.elapsedMicroseconds()); });

    size_t accessed_bucket_num = 0;
    size_t accessed_bucket_size = 0;

    auto filter_cache_segments = [this, &accessed_bucket_num, &accessed_bucket_size](const auto & segment) {
        AccessStatistics & stats = cache_statistics.getAccessStats(segment->getSegmentName());
        ++accessed_bucket_num;
        Stopwatch watch;
        {
            std::lock_guard lock(stats.stats_mutex);
            accessed_bucket_size += stats.access_stats.size();
            ProfileEvents::increment(ProfileEvents::DiskCacheAcquireStatsLock, watch.elapsedMicroseconds());
            auto segment_hit_count = ++stats.access_stats[segment->getSegmentName()];
            if (segment_hit_count >= segment_hits_to_cache)
            {
                stats.access_stats.erase(segment->getSegmentName());
                return true;
            }
            else
                return false;
        }
    };

    if (accessed_bucket_num != 0)
    {
        CurrentMetrics::set(CurrentMetrics::DiskCacheRoughSingleStatsBucketSize, accessed_bucket_size / accessed_bucket_num);
    }

    // auto cache_segments_end = std::partition(segments.begin(), segments.end(), filter_cache_segments);
    IDiskCacheSegmentsVector res;
    std::copy_if(segments.begin(), segments.end(), std::back_inserter(res), filter_cache_segments);
    return res;
}

}
