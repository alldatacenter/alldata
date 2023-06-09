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

#include <Storages/UUIDAndPartName.h>
#include <Catalog/DataModelPartWrapper.h>
#include <Common/LRUCache.h>
#include <Core/UUID.h>
#include <Protos/data_models.pb.h>
#include <Protos/DataModelHelpers.h>

namespace ProfileEvents
{
    extern const Event CnchDataPartCacheHits;
    extern const Event CnchDataPartCacheMisses;
}

namespace DB
{

using TableWithPartition = std::pair<UUID, String>;
using DataPartModelsMap = std::map<String, DataModelPartWrapperPtr>;

struct DataPartsWeightFunction
{
    size_t operator()(const DataPartModelsMap & parts_map) const
    {
        return parts_map.size();
    }
};

class CnchDataPartCache : public LRUCache<TableWithPartition, DataPartModelsMap, TableWithPartitionHash, DataPartsWeightFunction>
{
private:
    using Base = LRUCache<TableWithPartition, DataPartModelsMap, TableWithPartitionHash, DataPartsWeightFunction>;

public:
    CnchDataPartCache(size_t max_parts_to_cache)
        : Base(max_parts_to_cache) {
        inner_container = std::make_unique<CacheContainer<Key>>();
    }

    template <typename LoadFunc>
    MappedPtr getOrSet(const Key & key, LoadFunc && load)
    {
        auto result = Base::getOrSet(key, load);

        if (result.second)
        {
            ProfileEvents::increment(ProfileEvents::CnchDataPartCacheHits);
            if (inner_container)
            {
                String uuid_str = UUIDHelpers::UUIDToString(key.first);
                inner_container->insert(uuid_str, key);
            }
        }
        else
            ProfileEvents::increment(ProfileEvents::CnchDataPartCacheMisses);

        return result.first;
    }

    void insert(const Key & key, const String & part_name, const DataModelPartWrapperPtr & partPtr)
    {
        /// insert part into cache only if key has already exits.
        auto value = Base::get(key);
        if (value)
        {
            (*value)[part_name] = partPtr;
            // call Base::set here to update cache status in LRU cache.
            Base::set(key, value);
        }
        else
        {
            auto new_value = std::make_shared<DataPartModelsMap>();
            new_value->emplace(part_name, partPtr);
            Base::set(key, new_value);
            if (inner_container)
            {
                String uuid_str = UUIDHelpers::UUIDToString(key.first);
                inner_container->insert(uuid_str, key);
            }
        }
    }

    void insert(const Key & key, const MappedPtr & value)
    {
        Base::set(key, value);
        if (inner_container)
        {
            String uuid_str = UUIDHelpers::UUIDToString(key.first);
            inner_container->insert(uuid_str, key);
        }
    }

    void dropCache(const UUID & uuid)
    {
        if (!inner_container)
            return;

        String uuid_str = UUIDHelpers::UUIDToString(uuid);
        const auto & keys = inner_container->getKeys(uuid_str);
        for (const auto & key : keys)
            remove(key);
    }

    std::unordered_map<String, std::pair<size_t, size_t>> getTableCacheInfo()
    {
        auto keys = inner_container->getAllKeys();

        std::unordered_map<String, std::pair<size_t, size_t>> res;

        for (const auto & key : keys)
        {
            auto cached = get(key);
            if (cached)
            {
                String uuid_str = UUIDHelpers::UUIDToString(key.first);
                auto it = res.find(uuid_str);
                if (it == res.end())
                {
                    res.emplace(uuid_str, std::make_pair(1, cached->size()));
                }
                else
                {
                    it->second.first++;
                    it->second.second += cached->size();
                }
            }
        }

        return res;
    }

    void remove(const Key & key) { Base::remove(key); }

};

using CnchDataPartCachePtr = std::shared_ptr<CnchDataPartCache>;

}
