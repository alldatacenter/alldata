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

#include "DiskCacheFactory.h"

#include <Disks/IStoragePolicy.h>
#include <Interpreters/Context.h>
#include <Storages/DiskCache/DiskCacheLRU.h>
#include <Storages/DiskCache/DiskCacheSettings.h>
#include <Storages/DiskCache/DiskCacheSimpleStrategy.h>

namespace DB
{
void DiskCacheFactory::init(Context & context)
{
    const auto & config = context.getConfigRef();
    DiskCacheSettings cache_settings;
    DiskCacheStrategySettings strategy_settings;
    cache_settings.loadFromConfig(config, "lru");
    strategy_settings.loadFromConfig(config, "simple");

    // TODO: volume
    VolumePtr disk_cache_volume = context.getStoragePolicy("default")->getVolume(0);
    auto disk_cache = std::make_shared<DiskCacheLRU>(context, disk_cache_volume, cache_settings);
    disk_cache->asyncLoad();

    auto cache_strategy = std::make_shared<DiskCacheSimpleStrategy>(strategy_settings);
    default_cache = std::make_pair(std::move(disk_cache), std::move(cache_strategy));
}

}
