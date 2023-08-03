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

#include "ConcurrentShardElement.h"

#include <common/logger_useful.h>

namespace DB
{
template <typename KeyType, typename ElementType, typename Hash = std::hash<KeyType>>
class ConcurrentShardMap
{
  using ElementPtr = std::shared_ptr<ElementType>;

public:
    explicit ConcurrentShardMap(size_t num_shard = 128, Hash const & hash_function_ = Hash())
        : hash_function(hash_function_), shards(num_shard)
    {
        for (unsigned i = 0; i < num_shard; ++i)
        {
            shards[i].reset(new ConcurrentShardElement<KeyType, ElementPtr>());
        }
    }

    bool exist(const KeyType & key) { return getShard(key).exist(key); }

    void put(const KeyType & key, ElementPtr value) { getShard(key).put(key, value); }

    bool putIfNotExists(const KeyType & key, ElementPtr value) { return getShard(key).putIfNotExists(key, value); }

    ElementPtr get(const KeyType & key) { return getShard(key).get(key); }

    ElementPtr get(const KeyType & key, size_t timeout) { return getShard(key).get(key, timeout); }

    bool remove(const KeyType & key) { return getShard(key).remove(key); }

    size_t size()
    {
        size_t total_size = 0;
        std::for_each(shards.begin(), shards.end(), [&](std::unique_ptr<ConcurrentShardElement<KeyType, ElementPtr>> & element) {
            total_size += element->size();
        });
        return total_size;
    }

    String keys()
    {
        String ret;
        std::for_each(shards.begin(), shards.end(), [&](std::unique_ptr<ConcurrentShardElement<KeyType, ElementPtr>> & element) {
            ret += element->keys();
        });
        return ret;
    }

    void forEach(std::function<void(std::pair<KeyType, ElementPtr>)> func)
    {
        std::for_each(
            shards.begin(), shards.end(), [&](std::unique_ptr<ConcurrentShardElement<KeyType, ElementPtr>> & element) { func(element); });
    }

private:
    Poco::Logger * log = &Poco::Logger::get("ConcurrentShardMap");
    ConcurrentShardElement<KeyType, ElementPtr> & getShard(const KeyType & key)
    {
        std::size_t const shard_index = hash_function(key) % shards.size();
        return *shards[shard_index];
    }

    Hash hash_function;
    std::vector<std::unique_ptr<ConcurrentShardElement<KeyType, ElementPtr>>> shards;
};
}
