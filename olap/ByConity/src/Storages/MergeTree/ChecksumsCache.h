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

#include <Common/LRUCache.h>
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Common/ProfileEvents.h>
#include <sstream>

namespace ProfileEvents
{
    extern const Event ChecksumsCacheHits;
    extern const Event ChecksumsCacheMisses;
}

namespace DB
{

using ChecksumsName = std::string;

inline String getChecksumsCacheKey(const String & storage_unique_id, const IMergeTreeDataPart & part)
{
    /// for those table without uuid, directly use storage object address as unique key for checksum cache.
    return storage_unique_id + "_" + part.name;
}

struct ChecksumsWeightFunction
{
    size_t operator()(const MergeTreeDataPartChecksums & checksums) const
    {
        constexpr size_t kApproximatelyBytesPerElement = 128;
        return checksums.files.size() * kApproximatelyBytesPerElement;
    }
};

class ChecksumsCache : public LRUCache<ChecksumsName, MergeTreeDataPartChecksums, std::hash<ChecksumsName>, ChecksumsWeightFunction>
{
    using Base = LRUCache<ChecksumsName, MergeTreeDataPartChecksums, std::hash<ChecksumsName>, ChecksumsWeightFunction>;
public:
    using Base::Base;

    explicit ChecksumsCache(size_t max_size_in_bytes): Base(max_size_in_bytes)
    {
        inner_container = std::make_unique<CacheContainer<Key>>();
    }

    template <typename LoadFunc>
    std::pair<MappedPtr, bool> getOrSet(const String & name, const Key & key, LoadFunc && load)
    {
        auto result = Base::getOrSet(key, load);
        if (result.second)
        {
            ProfileEvents::increment(ProfileEvents::ChecksumsCacheMisses);
            if (inner_container)
                inner_container->insert(name, key);
        }
        else
            ProfileEvents::increment(ProfileEvents::ChecksumsCacheHits);
        return result;
    }

    void dropChecksumCache(const String & name)
    {
        if (!inner_container)
            return;

        const auto & keys = inner_container->getKeys(name);
        for (const auto & key : keys)
            remove(key);
    }
};

using ChecksumsCachePtr = std::shared_ptr<ChecksumsCache>;

}
