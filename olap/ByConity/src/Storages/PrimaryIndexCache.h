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

#include <Storages/PrimaryIndex.h>
#include <Storages/UUIDAndPartName.h>
#include <Common/LRUCache.h>
#include <Common/ProfileEvents.h>

namespace ProfileEvents
{
extern const Event PrimaryIndexCacheHits;
extern const Event PrimaryIndexCacheMisses;
}

namespace DB
{
struct PrimaryIndexWeightFunction
{
    size_t operator()(const PrimaryIndex & index) const
    {
        size_t sum_bytes = 0;
        for (auto & column : index)
            sum_bytes += column->allocatedBytes();
        return sum_bytes;
    }
};


class PrimaryIndexCache
    : public LRUCache<UUIDAndPartName, PrimaryIndex, UUIDAndPartNameHash, PrimaryIndexWeightFunction>
{
    using Base = LRUCache<UUIDAndPartName, PrimaryIndex, UUIDAndPartNameHash, PrimaryIndexWeightFunction>;

public:
    using Base::Base;

    template <typename LoadFunc>
    std::pair<MappedPtr, bool> getOrSet(const Key & key, LoadFunc && load)
    {
        auto result = Base::getOrSet(key, load);
        if (result.second)
            ProfileEvents::increment(ProfileEvents::PrimaryIndexCacheMisses);
        else
            ProfileEvents::increment(ProfileEvents::PrimaryIndexCacheHits);
        return result;
    }
};

using PrimaryIndexCachePtr = std::shared_ptr<PrimaryIndexCache>;

} /// EOF
