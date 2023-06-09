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

#include <Core/NamesAndTypes.h>
#include <Common/PODArray.h>

namespace DB
{
class IColumn;
class ReadBuffer;
class WriteBuffer;
class Set;

bool isBloomFilterEnabled(const NameAndTypePair & ntp);
void interpreterSetAsIndexes(const Set &, std::vector<std::vector<size_t>> &, size_t numSlot, size_t numHash);

class BloomFilterV2
{
public:
    static constexpr size_t DEFAULT_BLOOM_HASH_NUM = 4;
    static constexpr size_t DEFAULT_BLOOM_FILTER_BYTES = 1024 * 256;
    static constexpr size_t DEFAULT_BLOOM_FILTER_BITS = DEFAULT_BLOOM_FILTER_BYTES << 3;

    size_t numBytes;
    size_t numBits;
    UInt8 k; //# hashs to build BloomFilter
    //Memory buffer;
    PODArray<char> buffer;

    BloomFilterV2() : numBytes(0), numBits(0), k(1) { }

    explicit BloomFilterV2(size_t size);

    BloomFilterV2(size_t size, size_t defualt_hashes);

    ~BloomFilterV2() = default;

    // Initialization should be invoked explicitly based on build/probe scenarios
    void init(size_t size, size_t default_hashes);
    void setK(UInt8 k_);

    template <typename KeyType>
    void addKey(const KeyType & key);

    template <typename KeyType>
    bool probeKey(const KeyType & key) const;


    bool checkSet(const Set & set) const;

    bool probeSlot(const std::vector<std::vector<size_t>> & indexes) const;

    void deserialize(ReadBuffer & istr);
    void serializeToBuffer(WriteBuffer & ostr);

    size_t size() const { return numBytes; }
    // get fill factor
    double factor();
    const char * data() const;
    void mergeInplace(BloomFilterV2 & bf);
};

// BloomFilter per Mark Range, Hard Coded Size because it's very sensitive
class RangedBloomFilter
{
public:
    static const size_t BITS = 512; // must be 2^N
    static const UInt8 NUM_HASHS = 2;
    //static const UInt8 NUM_HASHS = 1;

    UInt64 buffer[RangedBloomFilter::BITS / 64]; // 256 bit per mark range, two hash functions
    RangedBloomFilter() : buffer{} { }

    template <typename KeyType>
    void addKey(const KeyType & key);

    template <typename KeyType>
    bool probeKey(const KeyType & key) const;

    bool probeSlot(const std::vector<std::vector<size_t>> & indexes) const;

    void reset();

    void deserialize(ReadBuffer & istr);
    void serializeToBuffer(WriteBuffer & ostr);

    // For compatible reason, serialize settings into disk in case
    // we will change this setting without rebuild it.
    void serializePrefixToBuffer(WriteBuffer & ostr);
    void deserializePrefix(ReadBuffer & istr);

    // get fill factor
    double factor();
};

inline bool operator==(const RangedBloomFilter & lhs, const RangedBloomFilter & rhs)
{
    size_t num_slot = RangedBloomFilter::BITS / 64;
    for (size_t i = 0; i < num_slot; i++)
    {
        if (lhs.buffer[i] != rhs.buffer[i])
            return false;
    }
    return true;
}

inline bool operator!=(const RangedBloomFilter & lhs, const RangedBloomFilter & rhs)
{
    return !(lhs == rhs);
}

inline bool operator==(const BloomFilterV2 & lhs, const BloomFilterV2 & rhs)
{
    if (lhs.numBytes != rhs.numBytes)
        return false;
    for (size_t i = 0; i < lhs.numBytes; i++)
    {
        if (lhs.buffer[i] != rhs.buffer[i])
            return false;
    }
    return true;
}

inline bool operator!=(const BloomFilterV2 & lhs, const BloomFilterV2 & rhs)
{
    return !(lhs == rhs);
}

}
