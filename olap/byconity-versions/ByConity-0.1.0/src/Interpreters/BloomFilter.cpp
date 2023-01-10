/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Interpreters/BloomFilter.h>
#include <city.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnLowCardinality.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

static constexpr UInt64 SEED_GEN_A = 845897321;
static constexpr UInt64 SEED_GEN_B = 217728422;

static constexpr UInt64 MAX_BLOOM_FILTER_SIZE = 1 << 30;


BloomFilterParameters::BloomFilterParameters(size_t filter_size_, size_t filter_hashes_, size_t seed_)
    : filter_size(filter_size_), filter_hashes(filter_hashes_), seed(seed_)
{
    if (filter_size == 0)
        throw Exception("The size of bloom filter cannot be zero", ErrorCodes::BAD_ARGUMENTS);
    if (filter_hashes == 0)
        throw Exception("The number of hash functions for bloom filter cannot be zero", ErrorCodes::BAD_ARGUMENTS);
    if (filter_size > MAX_BLOOM_FILTER_SIZE)
        throw Exception(ErrorCodes::BAD_ARGUMENTS, "The size of bloom filter cannot be more than {}", MAX_BLOOM_FILTER_SIZE);
}


BloomFilter::BloomFilter(const BloomFilterParameters & params)
    : BloomFilter(params.filter_size, params.filter_hashes, params.seed)
{
}

BloomFilter::BloomFilter(size_t size_, size_t hashes_, size_t seed_)
    : size(size_), hashes(hashes_), seed(seed_), words((size + sizeof(UnderType) - 1) / sizeof(UnderType)), filter(words, 0)
{
    assert(size != 0);
    assert(hashes != 0);
}

bool BloomFilter::find(const char * data, size_t len)
{
    size_t hash1 = CityHash_v1_0_2::CityHash64WithSeed(data, len, seed);
    size_t hash2 = CityHash_v1_0_2::CityHash64WithSeed(data, len, SEED_GEN_A * seed + SEED_GEN_B);

    for (size_t i = 0; i < hashes; ++i)
    {
        size_t pos = (hash1 + i * hash2 + i * i) % (8 * size);
        if (!(filter[pos / (8 * sizeof(UnderType))] & (1ULL << (pos % (8 * sizeof(UnderType))))))
            return false;
    }
    return true;
}

void BloomFilter::add(const char * data, size_t len)
{
    size_t hash1 = CityHash_v1_0_2::CityHash64WithSeed(data, len, seed);
    size_t hash2 = CityHash_v1_0_2::CityHash64WithSeed(data, len, SEED_GEN_A * seed + SEED_GEN_B);

    for (size_t i = 0; i < hashes; ++i)
    {
        size_t pos = (hash1 + i * hash2 + i * i) % (8 * size);
        filter[pos / (8 * sizeof(UnderType))] |= (1ULL << (pos % (8 * sizeof(UnderType))));
    }
}

void BloomFilter::clear()
{
    filter.assign(words, 0);
}

bool BloomFilter::contains(const BloomFilter & bf)
{
    for (size_t i = 0; i < words; ++i)
    {
        if ((filter[i] & bf.filter[i]) != bf.filter[i])
            return false;
    }
    return true;
}

UInt64 BloomFilter::isEmpty() const
{
    for (size_t i = 0; i < words; ++i)
        if (filter[i] != 0)
            return false;
    return true;
}

bool operator== (const BloomFilter & a, const BloomFilter & b)
{
    for (size_t i = 0; i < a.words; ++i)
        if (a.filter[i] != b.filter[i])
            return false;
    return true;
}

void BloomFilter::addHashWithSeed(const UInt64 & hash, const UInt64 & hash_seed)
{
    size_t pos = CityHash_v1_0_2::Hash128to64(CityHash_v1_0_2::uint128(hash, hash_seed)) % (8 * size);
    filter[pos / (8 * sizeof(UnderType))] |= (1ULL << (pos % (8 * sizeof(UnderType))));
}

bool BloomFilter::findHashWithSeed(const UInt64 & hash, const UInt64 & hash_seed)
{
    size_t pos = CityHash_v1_0_2::Hash128to64(CityHash_v1_0_2::uint128(hash, hash_seed)) % (8 * size);
    return bool(filter[pos / (8 * sizeof(UnderType))] & (1ULL << (pos % (8 * sizeof(UnderType)))));
}

DataTypePtr BloomFilter::getPrimitiveType(const DataTypePtr & data_type)
{
    if (const auto * array_type = typeid_cast<const DataTypeArray *>(data_type.get()))
    {
        if (!typeid_cast<const DataTypeArray *>(array_type->getNestedType().get()))
            return getPrimitiveType(array_type->getNestedType());
        else
            throw Exception("Unexpected type " + data_type->getName() + " of bloom filter index.", ErrorCodes::BAD_ARGUMENTS);
    }

    if (const auto * nullable_type = typeid_cast<const DataTypeNullable *>(data_type.get()))
        return getPrimitiveType(nullable_type->getNestedType());

    if (const auto * low_cardinality_type = typeid_cast<const DataTypeLowCardinality *>(data_type.get()))
        return getPrimitiveType(low_cardinality_type->getDictionaryType());

    return data_type;
}

ColumnPtr BloomFilter::getPrimitiveColumn(const ColumnPtr & column)
{
    if (const auto * array_col = typeid_cast<const ColumnArray *>(column.get()))
        return getPrimitiveColumn(array_col->getDataPtr());

    if (const auto * nullable_col = typeid_cast<const ColumnNullable *>(column.get()))
        return getPrimitiveColumn(nullable_col->getNestedColumnPtr());

    if (const auto * low_cardinality_col = typeid_cast<const ColumnLowCardinality *>(column.get()))
        return getPrimitiveColumn(low_cardinality_col->convertToFullColumnIfLowCardinality());

    return column;
}

void BloomFilter::merge(const BloomFilter & bf)
{
    if (unlikely(size != bf.size || hashes != bf.hashes || seed != bf.seed || words != bf.words))
        throw Exception("Bloom filters must have the same size", ErrorCodes::LOGICAL_ERROR);
    for (size_t i = 0; i < words; ++i)
        filter[i] |= bf.filter[i];
}

void BloomFilter::serializeToBuffer(WriteBuffer & buf) const
{
    writeBinary(size, buf);
    writeBinary(hashes, buf);
    writeBinary(seed, buf);
    writeBinary(words, buf);

    for (size_t i = 0; i < words; ++i)
        writeBinary(filter[i], buf);
}

void BloomFilter::deserialize(ReadBuffer & buf)
{
    readBinary(size, buf);
    readBinary(hashes, buf);
    readBinary(seed, buf);
    readBinary(words, buf);

    filter.resize(words, 0);
    for (size_t i = 0; i < words; ++i)
        readBinary(filter[i], buf);
}

}
