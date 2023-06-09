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

#include <cstdint>
#include <map>
#include <mutex>
#include <Core/Types.h>
#include <Common/Exception.h>
#include <Common/PODArray.h>
#include <common/types.h>
#include <Common/ConsistentHashUtils/Hash.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>


namespace DB
{
/// CRC 64 hash
struct FIO_CRC64
{
    UInt64 operator()(const String & key) const { return fio_crc64(reinterpret_cast<const unsigned char *>(key.c_str()), key.length()); }

    template <class Numeric>
    UInt64 operator()(Numeric val) const
    {
        return fio_crc64(reinterpret_cast<const unsigned char *>(&val), sizeof(Numeric));
    }

    template <class Numeric>
    UInt64 operator()(const String & key, Numeric val) const
    {
        auto joint_key = key + std::to_string(val);
        return fio_crc64(reinterpret_cast<const unsigned char *>(joint_key.c_str()), joint_key.length());
    }
};

/// Standard C++ hash
struct STD_HASH
{
    UInt64 operator()(const String & key) const { return std::hash<String>{}(key); }

    template <class Numeric>
    UInt64 operator()(Numeric val) const
    {
        return std::hash<Numeric>{}(val);
    }

    template <class Numeric>
    UInt64 operator()(const String & key, Numeric val) const
    {
        auto joint_key = key + std::to_string(val);
        return std::hash<String>{}(joint_key);
    }
};

/// XXHASH
struct XX_HASH3
{
    UInt64 operator()(const String & key) const { return xxhash3(reinterpret_cast<const unsigned char *>(key.c_str()), key.length()); }

    template <class Numeric>
    UInt64 operator()(Numeric val) const
    {
        return xxhash3(reinterpret_cast<const unsigned char *>(&val), sizeof(Numeric));
    }

    template <class Numeric>
    UInt64 operator()(const String & key, Numeric val) const
    {
        auto joint_key = key + std::to_string(val);
        return xxhash3(reinterpret_cast<const unsigned char *>(joint_key.c_str()), joint_key.length());
    }
};

/// Interface for ring-based consistent hash map family
/// This implement three consistent hashing algorithms:
/// 1. Karger's algorithm (with 16 virtual nodes), ref: https://en.wikipedia.org/wiki/Consistent_hashing
/// 2. Multi-probe consistent hasing, ref: https://arxiv.org/abs/1505.00062
/// 3. Bounded-load consistent hashing, ref: https://arxiv.org/abs/1608.01350

class ConsistentHashRing
{
public:
    ConsistentHashRing() = default;
    ConsistentHashRing(size_t replicas_, size_t num_probes_, double load_factor_ = 1.25);
    virtual ~ConsistentHashRing() = default;

    /// Return number of assignment in the ring
    size_t size() const { return std::ceil(double(ring.size())/replicas); }
    bool empty() const { return ring.empty(); }

    /// Insert a node to ring, will insert virtual nodes if necessary
    void insert(const String & node);

    /// Delete a node from ring, will delete all virtual nodes if necessary
    void erase(const String & node);

    /// Calculate cap limit
    size_t getCapLimit(size_t num_parts, bool strict = false) const;

    /// Find the node to put the part on the ring
    virtual String find(const String & key) const;

    /// Find the node to put the part on the ring if cap limit is satisfied
    virtual String tryFind(const String & key, size_t cap_limit, std::unordered_map<String, UInt64> & stats) const;

    /// ToDo: Replace tryFind
    /// Find the node tu put the part on the ring if the cast if satisfied.
    virtual String tryFindByCost(const String & key, size_t cost, size_t cost_limit, std::unordered_map<String, UInt64> & stats) const;

    /// Find the node to put the key on the ring, and perform balancing if necessary
    virtual String findAndRebalance(const String & part, size_t cap_limit, std::unordered_map<String, UInt64> & stats) const;

    /// Clear all data, make the ring empty
    void clear()
    {
        ring.clear();
    }

    /// get the underlying ring, only for testing
    const auto & getUnderlyingMap() const { return ring; }

private:
    /// Number of replicas for each node, = 1 means no virtual node
    size_t replicas = 16;
    /// Number of probes for each node, = 1 means normal Karger's algorithm
    size_t num_probes = 21;
    /// Load factor - to compute capacity limit for each node
    /// = INF means no load restriction
    double load_factor = 1.15;
    /// Ring implemented as map
    std::map<UInt64, String> ring;

public:
    using RingIterator = typename decltype(ring)::iterator;
    using RingConstIterator = typename decltype(ring)::const_iterator;

private:

    void eraseImpl(const String & node, bool keep_physical = false);

    RingConstIterator findImpl(const String & key) const;


    /// Hash function
    inline static STD_HASH hasher = STD_HASH{};
};
}
