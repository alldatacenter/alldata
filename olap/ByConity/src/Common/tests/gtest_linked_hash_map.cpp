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

#include <memory>
#include <common/defines.h>
#include <fmt/format.h>
#include <gtest/gtest.h>
#include <Common/LinkedHashMap.h>
using namespace DB;


#if defined(ABORT_ON_LOGICAL_ERROR)
    // skip this test, since ASSERT_DEATH is not stable
    #define ASSERT_LOGICAL_ERROR(p)
#else
    #define ASSERT_LOGICAL_ERROR(p) ASSERT_ANY_THROW(p)
#endif

TEST(LinkedHashMap, String)
{
    using K = String;
    using V = String;
    auto genK = [](int x) { return fmt::format("K{:05d}", x); };
    auto genV = [](int x) { return fmt::format("V{:05d}", x); };
    int N = 10000;
    LinkedHashMap<K, V> mapping;
    ASSERT_TRUE(mapping.empty());
    for (int i = 0; i < N; ++i)
    {
        auto index = i * 3 % N;
        ASSERT_EQ(mapping.size(), i);
        mapping.emplace_back(genK(index), genV(index));
        ASSERT_EQ(mapping.size(), i + 1);
    }
    ASSERT_FALSE(mapping.empty());

    {
        auto i = 0;
        for (auto [k, v] : mapping)
        {
            auto index = i * 3 % N;
            ASSERT_EQ(k, genK(index));
            ASSERT_EQ(v, genV(index));
            ++i;
        }
    }
    ASSERT_EQ(mapping.size(), N);

    for (int i = 0; i < N; ++i)
    {
        auto k = genK(i);
        auto v = mapping.at(k);
        ASSERT_TRUE(mapping.count(k));
        ASSERT_EQ(v, genV(i));
    }

    ASSERT_FALSE(mapping.count(genK(N)));
    ASSERT_LOGICAL_ERROR(mapping.at(genK(N)));
}

TEST(LinkedHashMap, UniquePtr)
{
    using K = int;
    using V = std::unique_ptr<int>;
    auto genK = [](int x) { return x; };
    auto genV = [](int x) { return std::make_unique<int>(x); };
    int N = 10000;
    LinkedHashMap<K, V> mapping;
    ASSERT_TRUE(mapping.empty());
    for (int i = 0; i < N; ++i)
    {
        auto index = i * 3 % N;
        ASSERT_EQ(mapping.size(), i);
        mapping.emplace_back(genK(index), genV(index));
        ASSERT_EQ(mapping.size(), i + 1);
    }
    ASSERT_FALSE(mapping.empty());

    {
        auto i = 0;
        for (auto & [k, v] : mapping)
        {
            auto index = i * 3 % N;
            ASSERT_EQ(k, genK(index));
            ASSERT_EQ(*v, index);
            ++i;
        }
    }
    for (int i = 0; i < N; ++i)
    {
        auto k = genK(i);
        auto v = *mapping.at(k);
        ASSERT_TRUE(mapping.count(k));
        ASSERT_EQ(v, *genV(i));
    }
    ASSERT_FALSE(mapping.count(genK(N)));
    ASSERT_LOGICAL_ERROR(mapping.at(genK(N)));
}
