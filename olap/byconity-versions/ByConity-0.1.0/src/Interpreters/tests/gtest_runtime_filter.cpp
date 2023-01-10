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

#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromString.h>
#include <Interpreters/BloomFilter.h>
#include <Interpreters/BloomFilterV2.h>

#include <gtest/gtest.h>

using namespace DB;

constexpr size_t DEFAULT_BLOOM_FILTER_BYTES = 1024 * 256;
constexpr size_t DEFAULT_BLOOM_HASH_NUM = 4;
constexpr size_t DEFAULT_BLOOM_FILTER_BITS = DEFAULT_BLOOM_FILTER_BYTES << 3;

TEST(RuntimeFilterTest, BloomFilterSerDer)
{
    BloomFilter bloom_filter{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};

    StringRef key1("a");
    bloom_filter.add(key1.data, key1.size);

    StringRef key2("b");
    bloom_filter.add(key2.data, key2.size);

    /**
     * serialize to buffer
     */
    WriteBufferFromOwnString write_buffer;
    bloom_filter.serializeToBuffer(write_buffer);

    /**
     * deserialize from buffer
     */
    ReadBufferFromString read_buffer(write_buffer.str());
    BloomFilter new_bloom_filter{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};
    new_bloom_filter.deserialize(read_buffer);

    EXPECT_TRUE(new_bloom_filter.find(key1.data, key1.size));
    EXPECT_TRUE(new_bloom_filter.find(key2.data, key2.size));

    StringRef key3("c");
    EXPECT_FALSE(new_bloom_filter.find(key3.data, key3.size));
}

TEST(RuntimeFilterTest, BloomFilterMerge)
{
    BloomFilter bloom_filter{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};
    StringRef key1("a");
    bloom_filter.add(key1.data, key1.size);

    BloomFilter bloom_filter2{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};
    StringRef key2("b");
    bloom_filter2.add(key2.data, key2.size);

    EXPECT_FALSE(bloom_filter.find(key2.data, key2.size));
    bloom_filter.merge(bloom_filter2);
    EXPECT_TRUE(bloom_filter.find(key2.data, key2.size));
}

TEST(RuntimeFilterTest, BloomFilterMerge2)
{
    BloomFilter bloom_filter{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};

    auto col = ColumnVector<UInt64>::create();
    col->insert(Field{794873});
    col->insert(Field{1190443});
    auto key1 = col->getDataAt(0);
    auto key2 = col->getDataAt(1);
    bloom_filter.add(key1.data, key1.size);

    BloomFilter bloom_filter2{DEFAULT_BLOOM_FILTER_BYTES, DEFAULT_BLOOM_HASH_NUM, DEFAULT_BLOOM_FILTER_BITS};
    bloom_filter2.add(key2.data, key2.size);

    EXPECT_FALSE(bloom_filter.find(key2.data, key2.size));
    bloom_filter.merge(bloom_filter2);
    EXPECT_TRUE(bloom_filter.find(key2.data, key2.size));
}

TEST(RuntimeFilterTest, BloomFilterV2)
{
    BloomFilterV2 bloom_filter{DEFAULT_BLOOM_FILTER_BYTES};

    auto col = ColumnVector<UInt64>::create();
    col->insert(Field{794873});
    col->insert(Field{1190443});
    auto key1 = col->getDataAt(0);
    auto key2 = col->getDataAt(1);
    bloom_filter.addKey(key1);

    BloomFilterV2 bloom_filter2{DEFAULT_BLOOM_FILTER_BYTES};
    bloom_filter2.addKey(key2);

    EXPECT_FALSE(bloom_filter.probeKey(key2));
    bloom_filter.mergeInplace(bloom_filter2);
    EXPECT_TRUE(bloom_filter.probeKey(key2));

    /**
     * serialize to buffer
     */
    WriteBufferFromOwnString write_buffer;
    bloom_filter.serializeToBuffer(write_buffer);

    /**
     * deserialize from buffer
     */
    ReadBufferFromString read_buffer(write_buffer.str());
    BloomFilterV2 new_bloom_filter{DEFAULT_BLOOM_FILTER_BYTES};
    new_bloom_filter.deserialize(read_buffer);

    EXPECT_TRUE(new_bloom_filter.probeKey(key1));
    EXPECT_TRUE(new_bloom_filter.probeKey(key2));
}
