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

#include <limits>
#include <Poco/Util/AbstractConfiguration.h>

namespace DB
{
struct DiskCacheSettings
{
    static constexpr auto prefix = "disk_cache";
    void loadFromConfig(const Poco::Util::AbstractConfiguration & conf, const std::string & disk_cache_name);

    size_t lru_max_size {std::numeric_limits<size_t>::max()};
    // When queue size exceed random drop ratio, start drop disk cache task, range from 0 - 100
    size_t random_drop_threshold {50};
    // Cache mapping bucket size
    size_t mapping_bucket_size {5000};
    // LRU queue update interval in seconds
    size_t lru_update_interval {60};

    size_t cache_shard_num {12};
    // If true every disk will have it's own dispatcher, otherwise only one dispatcher
    // is used
    bool cache_dispatcher_per_disk {true};

    // Number of cache loader per disk
    size_t cache_loader_per_disk {2};
    int cache_load_dispatcher_drill_down_level {1};
    size_t cache_set_rate_limit {0};
};

struct DiskCacheStrategySettings
{
    static constexpr auto prefix = "disk_cache_strategy";
    void loadFromConfig(const Poco::Util::AbstractConfiguration & conf, const std::string & disk_cache_strategy_name);

    size_t segment_size {8192};
    size_t hits_to_cache {2};
    // Size of disk cache statistics bucket size
    size_t stats_bucket_size {10000};
};

}
