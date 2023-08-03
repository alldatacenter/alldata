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

#include "ConsistentHashUtils/ConsistentHashRing.h"
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>


namespace DB
{
ConsistentHashRing::ConsistentHashRing(size_t replicas_, size_t num_probes_, double load_factor_)
    : replicas(replicas_), num_probes(num_probes_), load_factor(load_factor_) {}

void ConsistentHashRing::erase(const String & node)
{
    eraseImpl(node, false);
}

void ConsistentHashRing::insert(const String & node)
{
    UInt64 init_hash_value = hasher(node);
    ring.emplace(init_hash_value, node);

    /// insert virtual nodes
    for (UInt32 i = 0; i < replicas - 1; ++i)
    {
        /// each virtual nodes will have name like "node0", "node1", ...
        UInt64 hash_value = hasher(node, i);
        ring.emplace(hash_value, node);
    }
}

size_t ConsistentHashRing::getCapLimit(size_t num_parts, bool strict) const
{
    size_t num_nodes = size();
    double load = load_factor;
    if (strict || num_parts <= num_nodes)
        load = 1;
    return std::ceil((num_parts * load) / num_nodes);
}


String ConsistentHashRing::find(const String & key) const
{
    auto it = findImpl(key);
    return it->second;
}

String ConsistentHashRing::tryFind(const String & key, size_t cap_limit, std::unordered_map<String, UInt64> & stats) const
{
    auto it = findImpl(key);

    auto it2 = stats.try_emplace(it->second, 0).first;

    if (it2->second < cap_limit)
    {
        it2->second++;
        return it->second;
    }
    return {};
}

String ConsistentHashRing::tryFindByCost(const String & key, size_t cost, size_t cost_limit, std::unordered_map<String, UInt64> & stats) const
{
    auto it = findImpl(key);

    if (stats[it->second] + cost < cost_limit)
    {
        stats[it->second] += cost;
        return it->second;
    }

    return {};
}

String ConsistentHashRing::findAndRebalance(const String & key, size_t cap_limit, std::unordered_map<String, UInt64> & stats) const
{
    auto it = findImpl(key);

    while (stats[it->second] >= cap_limit)
    {
        if (++it == ring.end())
            it = ring.begin();
    }
    stats[it->second]++;
    return it->second;
}

void ConsistentHashRing::eraseImpl(const String & node, bool keep_physical)
{
    for (UInt32 i = 0; i < replicas - 1; ++i)
    {
        UInt64 hash_value = hasher(node, i);
        auto it0 = ring.find(hash_value);
        if (it0 != ring.end())
            ring.erase(it0);
    }
    if (!keep_physical)
    {
        UInt64 hash_value = hasher(node);
        auto it0 = ring.find(hash_value);
        if (it0 != ring.end())
            ring.erase(it0);
    }
}

ConsistentHashRing::RingConstIterator ConsistentHashRing::findImpl(const String & key) const
{
    /// Multi-probe hasing assign a key to its closest lower-bound node on the ring.
    /// The key is assign num_probes times with a set of hash functions and the last assignment
    /// is returned. Yes, it's sound moron @@, but it works and backed by theory, pls read the
    /// paper for the formal proof.
    UInt64 min_distance = std::numeric_limits<UInt64>::max();
    auto ret = ring.begin();
    /// if num_probes == 1, then we it's normal Karger's algorithm
    for (UInt32 i = 0; i < num_probes; ++i)
    {
        /// Instead of using different hash functions in each probe, we use the same hash function
        /// but in each probe we add a incrementing number to the key.
        UInt64 hash_val = hasher(key, i);
        auto point = ring.lower_bound(hash_val);
        /// compute the distace of the hash_val to the point, if the distance is less than min_distance
        /// then assign the point to ret. If point is the ring.end(), then assign point to ring.begin()
        /// and compute the distance by UINT64_MAX - hash_val + point->first
        UInt64 d;
        if (point == ring.end())
        {
            point = ring.begin();
            d = std::numeric_limits<UInt64>::max() - hash_val + point->first;
        }
        else
        {
            d = point->first - hash_val;
        }
        if (d < min_distance)
        {
            min_distance = d;
            ret = point;
        }
    }

    return ret;
}

}
