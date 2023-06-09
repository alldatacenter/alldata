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
#include <memory>
#include <vector>
#include <unordered_map>
#include <shared_mutex>

namespace DB
{
struct SDCacheKey {
   std::string psm;
   std::string vw_name;
   bool operator <(const SDCacheKey &rhs) const {return ( psm < rhs.psm && vw_name < rhs.vw_name );}
   bool operator ==(const SDCacheKey &rhs) const {return ( psm == rhs.psm && vw_name == rhs.vw_name );}
};

// specialized hash function for unordered_map keys
struct hash_fn
{
    std::size_t operator() (const SDCacheKey &key) const
    {
        std::size_t h1 = std::hash<std::string>{}(key.psm);
        std::size_t h2 = std::hash<std::string>{}(key.vw_name);
        return h1 ^ (h2 << 1);
    }
};

template<typename Tendpoint>
struct SDCacheValue {
   std::vector<Tendpoint> endpoints;
   time_t last_update;
};

template<typename Tendpoint>
class ServiceDiscoveryCache
{
public:
    bool get(const SDCacheKey & key, SDCacheValue<Tendpoint> & value)
    {
        std::shared_lock<std::shared_mutex> lock(mutex);
        if (cache.find(key) == cache.end())
            return false;
        value = cache[key];
        return true;
    }
    void put(const SDCacheKey & key, const SDCacheValue<Tendpoint> & value)
    {
        std::lock_guard<std::shared_mutex> lock(mutex);
        cache[key] = value;
    }
    void clear()
    {
        std::lock_guard<std::shared_mutex> lock(mutex);
        cache.clear();
    }
private:
    using Map = std::unordered_map<SDCacheKey, SDCacheValue<Tendpoint>, hash_fn>;
    Map cache;
    std::shared_mutex mutex;
};
}
