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

#include "DiskCacheSettings.h"

#include <fmt/format.h>

namespace DB
{
void DiskCacheSettings::loadFromConfig(const Poco::Util::AbstractConfiguration & config, const std::string & disk_cache_name)
{
    std::string config_prefix = fmt::format("{}.{}", prefix, disk_cache_name); // disk_cache.lru
    lru_max_size = config.getUInt64(config_prefix + ".lru_max_size", static_cast<uint64_t>(2) * 1024 * 1024 * 1024 * 1024);
    random_drop_threshold = config.getUInt64(config_prefix + ".random_drop_threshold", 50);
    mapping_bucket_size = config.getUInt64(config_prefix + ".mapping_bucket_size", 5000);
    lru_update_interval = config.getUInt64(config_prefix + ".lru_update_interval", 60);
    cache_shard_num = config.getUInt(config_prefix + ".cache_shard_num", 12);
    cache_dispatcher_per_disk = config.getBool(config_prefix + ".cache_dispatcher_per_disk", true);
    cache_loader_per_disk = config.getUInt(config_prefix + ".cache_loader_per_disk", 2);
    cache_load_dispatcher_drill_down_level = config.getInt(config_prefix + ".cache_load_dispatcher_drill_down_level", 1);
    cache_set_rate_limit = config.getUInt64(config_prefix + ".cache_set_rate_limit", 0);
}

void DiskCacheStrategySettings::loadFromConfig(const Poco::Util::AbstractConfiguration & config, const std::string & disk_cache_strategy_name)
{
    std::string config_prefix = fmt::format("{}.{}", prefix, disk_cache_strategy_name);
    segment_size = config.getUInt64(config_prefix + ".segment_size", 8192);
    hits_to_cache = config.getUInt64(config_prefix + ".hits_to_cache", 2);
    stats_bucket_size = config.getUInt64(config_prefix + ".stats_bucket_size", 10000);
}

}
