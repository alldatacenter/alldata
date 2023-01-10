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

#include <Interpreters/BloomFilterV2.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionHelpers.h> // for checkDataType
#include <IO/ReadBuffer.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBuffer.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/Set.h>
#include <Common/HashTable/Hash.h>
#include <Common/typeid_cast.h>
#include <common/StringRef.h>

namespace DB
{

#if __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Winstantiation-after-specialization"
#pragma clang diagnostic ignored "-Wmissing-noreturn"
#endif

#define NUMERIC_NO128_TYPE_DISPATCH(M)       \
        M(UInt8,  DataTypeUInt8, UInt8)      \
        M(UInt16, DataTypeUInt16, UInt16)    \
        M(UInt32, DataTypeUInt32, UInt32)    \
        M(UInt64, DataTypeUInt64, UInt64)    \
        M(Int8,   DataTypeInt8, Int8)        \
        M(Int16,  DataTypeInt16, Int16)      \
        M(Int32,  DataTypeInt32, Int32)      \
        M(Int64,  DataTypeInt64, Int64)      \
        M(Float32, DataTypeFloat32, Float32) \
        M(Float64, DataTypeFloat64, Float64) \
        M(StringRef, DataTypeString, String)

#define NUMERIC_TYPE_DISPATCH(M)       \
        NUMERIC_NO128_TYPE_DISPATCH(M) \
        M(UInt128, DataTypeUUID, UInt128)

BloomFilterV2::BloomFilterV2(size_t size) : BloomFilterV2(size, DEFAULT_BLOOM_HASH_NUM)
{
}

BloomFilterV2::BloomFilterV2(size_t size, size_t default_hashes)
{
    init(size, default_hashes);
}

bool isBloomFilterEnabled(const NameAndTypePair &)
{
    return false;
}

void interpreterSetAsIndexes(const Set & set, std::vector<std::vector<size_t>> & res, size_t numSlot, size_t numHash)
{
    auto & dataTypes = set.getDataTypes();
    if (dataTypes.size() != 1)
    {
        throw Exception("Set for BloomFilter size not correct", ErrorCodes::LOGICAL_ERROR);
    }
    // go through keys in set, and probe
    //auto& elems = const_cast<Set&>(set).getSetElements();
    auto elems = set.getSetElements();
    auto & probeColumn = *elems[0];
    auto numProbes = probeColumn.size();
    res.resize(numProbes);

    [[maybe_unused]] auto dataTypeRawPtr = dataTypes[0].get();

#define INDEX_DISPATCH(VALUETYPE, DATATYPE, GETTYPE) \
    else if (checkAndGetDataType<DATATYPE>(dataTypeRawPtr)) \
    { \
        size_t elem_index = 0; \
        for (size_t i = 0; i < numProbes; i++) \
        { \
            size_t h = DefaultHash<VALUETYPE>()(static_cast<VALUETYPE>(probeColumn[i].get<NearestFieldType<GETTYPE>>())); \
            const size_t delta = (h >> 34) | (h << 30); \
            for (UInt8 j = 0; j < numHash; j++) \
            { \
                res[elem_index].push_back(h % numSlot); \
                h += delta; \
            } \
            ++elem_index; \
        } \
    }

    if (false)
    {
    }
    NUMERIC_NO128_TYPE_DISPATCH(INDEX_DISPATCH)
    else
    {
        throw Exception("interpreterSetAsIndexes doesn't support this type yet!", ErrorCodes::LOGICAL_ERROR);
    }

#undef INDEX_DISPATCH
}

void BloomFilterV2::init(size_t size, size_t default_hashes)
{
    if ((size & (size - 1)) != 0)
    {
        throw Exception("size of BloomFilter should be 2^N, but it is " + toString(size), ErrorCodes::LOGICAL_ERROR);
    }
    numBytes = size;
    numBits = size << 3;
    setK(default_hashes);

    buffer.resize_fill(size);
}

void BloomFilterV2::setK(UInt8 k_)
{
    if (k_ > 4)
        k_ = 4;
    k = k_;
}

void BloomFilterV2::deserialize(ReadBuffer & istr)
{
    // deserialize BloomFilter from ReadBuffer
    readBinary(numBytes, istr);
    readBinary(k, istr);
    // resize it to correct bytes
    init(numBytes, k);

    istr.read(buffer.data(), numBytes);
}

void BloomFilterV2::serializeToBuffer(WriteBuffer & ostr)
{
    // serialize BloomFilter into WriteBuffer
    writeBinary(numBytes, ostr);
    writeBinary(k, ostr);
    ostr.write(buffer.data(), numBytes);
}

const char * BloomFilterV2::data() const
{
    return buffer.data();
}

template <typename KeyType>
void BloomFilterV2::addKey(const KeyType & key)
{
    // apply similar logic(double) as bloom filter in levelDB
    size_t h = DefaultHash<KeyType>()(key);
    const size_t delta = (h >> 34) | (h << 30); // Rotate right 34 bits
    for (UInt8 j = 0; j < k; j++)
    {
        // because numBits should be 2^N, % could be optimized as bit operation
        //const size_t bitpos = h % numBits;
        const size_t bitpos = (h & (numBits - 1));
        buffer[bitpos >> 3] |= (1 << (bitpos & 0x07ULL));
        h += delta;
    }
}

template <typename KeyType>
bool BloomFilterV2::probeKey(const KeyType & key) const
{
    size_t h = DefaultHash<KeyType>()(key);
    const size_t delta = (h >> 34) | (h << 30); // Rotate right 34 bits

    for (UInt8 j = 0; j < k; j++)
    {
        //const size_t bitpos = h % numBits;
        const size_t bitpos = (h & (numBits - 1));
        if ((buffer[bitpos >> 3] & (1 << (bitpos & 0x07ULL))) == 0)
            return false;
        h += delta;
    }
    return true;
}

// Below logic is just for compiler as Default<UInt128> is not implemented
template <>
void BloomFilterV2::addKey<UInt128>([[maybe_unused]] const UInt128 & key)
{
    throw Exception("BloomFilter on UInt128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

template <>
bool BloomFilterV2::probeKey<UInt128>([[maybe_unused]] const UInt128 & key) const
{
    throw Exception("BloomFilter on UInt128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

// Below logic is just for compiler as Default<UInt128> is not implemented
template <>
void BloomFilterV2::addKey<Int128>([[maybe_unused]] const Int128 & key)
{
    throw Exception("BloomFilter on Int128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

template <>
bool BloomFilterV2::probeKey<Int128>([[maybe_unused]] const Int128 & key) const
{
    throw Exception("BloomFilter on Int128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}


// return true if any value in set pass bloom filter check
bool BloomFilterV2::checkSet(const Set & set) const
{
    auto & dataTypes = set.getDataTypes();
    if (dataTypes.size() != 1)
    {
        throw Exception("Set for BloomFilter size not correct", ErrorCodes::LOGICAL_ERROR);
    }
    // go through keys in set, and probe
    //auto& elems = const_cast<Set&>(set).getSetElements();
    auto elems = set.getSetElements();
    auto & probeColumn = *elems[0];
    auto numProbes = probeColumn.size();

    [[maybe_unused]] auto dataTypeRawPtr = dataTypes[0].get();

#define DISPATCH(VALUETYPE, DATATYPE, GETTYPE) \
    else if (checkAndGetDataType<DATATYPE>(dataTypeRawPtr)) \
    { \
        for (size_t i = 0; i < numProbes; i++) \
        { \
            if (probeKey<VALUETYPE>(static_cast<VALUETYPE>(probeColumn[i].get<NearestFieldType<GETTYPE>>()))) \
                return true; \
        } \
    }

    if (false)
    {
    }
    NUMERIC_TYPE_DISPATCH(DISPATCH)
    else
    {
        throw Exception("BloomFilterV2::checkSet don't support this type yet", ErrorCodes::LOGICAL_ERROR);
    }

#undef DISPATCH
    return false;
}

bool BloomFilterV2::probeSlot(const std::vector<std::vector<size_t>> & indexes) const
{
    for (auto & v_index : indexes)
    {
        bool match = true;
        for (auto index : v_index)
        {
            if ((buffer[index >> 3] & (1 << (index & 0x07ULL))) == 0)
            {
                match = false;
                break;
            }
        }

        if (match)
            return true;
    }

    return false;
}

double BloomFilterV2::factor()
{
    UInt32 tmp;
    size_t numOnes(0);
    for (size_t i = 0; i < numBytes; i += 4)
    {
        tmp = *reinterpret_cast<const UInt32 *>(data() + i);
        numOnes += __builtin_popcount(tmp);
    }
    return double(numOnes) / numBits;
}

void BloomFilterV2::mergeInplace(BloomFilterV2 & bf)
{
    if (bf.size() == 0)
        return;
    if (this->size() != bf.size())
        throw Exception("Cannot merge bloom filters with different bit size", ErrorCodes::LOGICAL_ERROR);
    const char * other = bf.data();
    for (size_t i = 0; i < buffer.size(); i++)
    {
        buffer[i] |= other[i];
    }
}

//==========Implementation of RangedBloomFilter========================

template <typename KeyType>
void RangedBloomFilter::addKey(const KeyType & key)
{
    // apply similar logic(double) as bloom filter in levelDB
    size_t h = DefaultHash<KeyType>()(key);
    const size_t delta = (h >> 34) | (h << 30); // Rotate right 34 bits
    for (UInt8 j = 0; j < RangedBloomFilter::NUM_HASHS; j++)
    {
        //const size_t bitpos = h % RangedBloomFilter::BITS;
        const size_t bitpos = h & (RangedBloomFilter::BITS - 1);
        buffer[bitpos >> 6] |= (1ULL << (bitpos & 0x3FULL));
        h += delta;
    }
}

template <typename KeyType>
bool RangedBloomFilter::probeKey(const KeyType & key) const
{
    size_t h = DefaultHash<KeyType>()(key);
    const size_t delta = (h >> 34) | (h << 30); // Rotate right 34 bits

    for (UInt8 j = 0; j < RangedBloomFilter::NUM_HASHS; j++)
    {
        //const size_t bitpos = h % RangedBloomFilter::BITS;
        const size_t bitpos = h & (RangedBloomFilter::BITS - 1);
        if ((buffer[bitpos >> 6] & (1ULL << (bitpos & 0x3FULL))) == 0)
            return false;
        h += delta;
    }
    return true;
}

bool RangedBloomFilter::probeSlot(const std::vector<std::vector<size_t>> & indexes) const
{
    for (auto & v_index : indexes)
    {
        bool match = true;
        for (auto index : v_index)
        {
            if ((buffer[index >> 6] & (1ULL << (index & 0x3FULL))) == 0)
            {
                match = false;
                break;
            }
        }

        if (match)
            return true;
    }

    return false;
}

// Below logic is just for compiler as Default<UInt128> is not implemented
template <>
void RangedBloomFilter::addKey<UInt128>([[maybe_unused]] const UInt128 & key)
{
    throw Exception("RangedBloomFilter on UInt128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

template <>
bool RangedBloomFilter::probeKey<UInt128>([[maybe_unused]] const UInt128 & key) const
{
    throw Exception("RangedBloomFilter on UInt128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

template <>
void RangedBloomFilter::addKey<Int128>([[maybe_unused]] const Int128 & key)
{
    throw Exception("RangedBloomFilter on Int128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}

template <>
bool RangedBloomFilter::probeKey<Int128>([[maybe_unused]] const Int128 & key) const
{
    throw Exception("RangedBloomFilter on Int128 type not support yet", ErrorCodes::LOGICAL_ERROR);
}


void RangedBloomFilter::reset()
{
    std::fill(buffer, buffer + sizeof(buffer) / sizeof(buffer[0]), 0);
}

void RangedBloomFilter::deserialize(ReadBuffer & istr)
{
    istr.read(reinterpret_cast<char *>(&buffer[0]), RangedBloomFilter::BITS / 8);
}

void RangedBloomFilter::serializeToBuffer(WriteBuffer & ostr)
{
    ostr.write(reinterpret_cast<char *>(&buffer[0]), RangedBloomFilter::BITS / 8);
}

void RangedBloomFilter::deserializePrefix(ReadBuffer & istr)
{
    // Use one word to encode BITS & NUM_HASHS info
    size_t bits_hashs;
    readBinary(bits_hashs, istr);
}

void RangedBloomFilter::serializePrefixToBuffer(WriteBuffer & ostr)
{
    size_t bits_hashs = (RangedBloomFilter::BITS << 56 | RangedBloomFilter::NUM_HASHS);
    writeBinary(bits_hashs, ostr);
}

double RangedBloomFilter::factor()
{
    UInt32 tmp;
    size_t numOnes(0);
    for (size_t i = 0; i < RangedBloomFilter::BITS / 64; i++)
    {
        tmp = buffer[i] & 0x00000000FFFFFFFFull;
        numOnes += __builtin_popcount(tmp);
        tmp = buffer[i] >> 32;
        numOnes += __builtin_popcount(tmp);
    }

    return double(numOnes) / BITS;
}

// explicit instance functions
#define INST_ADDKEY(T, DUMMY, DUMMY2) \
    template void BloomFilterV2::addKey<T>(const T &); \
    template void RangedBloomFilter::addKey<T>(const T &);

#define INST_PROBEKEY(T, DUMMY, DUMMY2) \
    template bool BloomFilterV2::probeKey<T>(const T &) const; \
    template bool RangedBloomFilter::probeKey<T>(const T &) const;

NUMERIC_TYPE_DISPATCH(INST_ADDKEY)
NUMERIC_TYPE_DISPATCH(INST_PROBEKEY)

#undef INST_ADDKEY
#undef INST_PROBEKEY

#undef NUMERIC_TYPE_DISPATCH
#undef NUMERIC_NO_128_TYPE_DISPATCH


#if __clang__
#pragma clang diagnostic pop
#endif

}
