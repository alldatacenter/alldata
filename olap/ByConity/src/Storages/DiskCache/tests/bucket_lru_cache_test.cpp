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

#include <chrono>
#include <memory>
#include <random>
#include <string>
#include <thread>
#include <gtest/gtest.h>
#include <Storages/DiskCache/BucketLRUCache.h>

using namespace DB;

class BucketLRUCacheTest: public ::testing::Test {
public:
    static void SetUpTestCase() {
    }

    static void TearDownTestCase() {
    }

    virtual void SetUp() override {
    }

    virtual void TearDown() override {
    }

    template<typename Key, typename Value, typename HashFunction>
    static void insertToCache(BucketLRUCache<Key, Value, HashFunction>& cache,
            int begin, int end) {
        for (int i = begin; i < end; i++) {
            cache.set(std::to_string(i), std::make_shared<String>(std::to_string(i)));
        }
    }

    template<typename Key, typename Value, typename HashFunction>
    static void verifyRangeExist(BucketLRUCache<Key, Value, HashFunction>& cache,
            int begin, int end) {
        for (int i = begin; i < end; i++) {
            ASSERT_EQ(*cache.get(std::to_string(i)), std::to_string(i));
        }
    }

    template<typename Key, typename Value, typename HashFunction>
    static void verifyRangeNotExist(BucketLRUCache<Key, Value, HashFunction>& cache,
            int begin, int end) {
        for (int i = begin; i < end; i++) {
            ASSERT_EQ(cache.get(std::to_string(i)), nullptr);
        }
    }

    template<typename Key, typename Value, typename HashFunction>
    static void verifyLRUOrder(BucketLRUCache<Key, Value, HashFunction>& cache,
            const std::vector<String>& order) {
        auto iter = cache.queue.begin();
        for (const String& key : order) {
            ASSERT_EQ(*iter, key);
            ++iter;
        }
    }

    template<typename Key, typename Value, typename HashFunction>
    static void verifyCacheWeight(BucketLRUCache<Key, Value, HashFunction>& cache,
            size_t cache_weight) {
        ASSERT_EQ(cache.current_size, cache_weight);
    }

    template<typename Key, typename Value, typename HashFunction>
    static void verifyCacheCount(BucketLRUCache<Key, Value, HashFunction>& cache,
            size_t cache_count) {
        ASSERT_EQ(cache.count(), cache_count);
    }

    template<typename Key, typename Value, typename HashFunction>
    static void removeFromCache(BucketLRUCache<Key, Value, HashFunction>& cache,
            const Key& key) {
        cache.remove(key);
    }

    static std::vector<String> generateRangeStr(int begin, int end) {
        std::vector<String> result;
        result.reserve(end - begin);
        for (int i = begin; i < end; i++) {
            result.push_back(std::to_string(i));
        }
        return result;
    }
};

#define VERIFY_CACHE_RANGE_EXIST(cache, begin, end) \
    ASSERT_NO_FATAL_FAILURE(verifyRangeExist(cache, begin, end))

#define VERIFY_CACHE_RANGE_NOT_EXIST(cache, begin, end) \
    ASSERT_NO_FATAL_FAILURE(verifyRangeNotExist(cache, begin, end))

#define VERIFY_CACHE_LRU_ORDER(cache, order) \
    ASSERT_NO_FATAL_FAILURE(verifyLRUOrder(cache, order))

#define VERIFY_CACHE_WEIGHT(cache, weight) \
    ASSERT_NO_FATAL_FAILURE(verifyCacheWeight(cache, weight))

#define VERIFY_CACHE_COUNT(cache, count) \
    ASSERT_NO_FATAL_FAILURE(verifyCacheCount(cache, count))

TEST_F(BucketLRUCacheTest, Simple) {
    int cache_size = 5;
    // Set lru refresh interval to 24h, so it shouldn't refresh lru list
    BucketLRUCache<String, String> cache(cache_size, 24 * 60 * 60, 2);

    insertToCache(cache, 0, cache_size);

    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, cache_size);

    int round = 10;
    VERIFY_CACHE_RANGE_NOT_EXIST(cache, cache_size, cache_size + round);

    for (int i = cache_size; i < cache_size + round; i++) {
        cache.set(std::to_string(i), std::make_shared<String>(std::to_string(i)));

        VERIFY_CACHE_COUNT(cache, cache_size);
        VERIFY_CACHE_WEIGHT(cache, cache_size);
        VERIFY_CACHE_RANGE_NOT_EXIST(cache, 0, i - cache_size);
        VERIFY_CACHE_RANGE_EXIST(cache, i - cache_size + 1, i + 1);
    }
}

TEST_F(BucketLRUCacheTest, Evict) {
    int cache_size = 5;
    BucketLRUCache<String, String> cache(cache_size, 0, 2);

    insertToCache(cache, 0, cache_size);
    std::this_thread::sleep_for(std::chrono::seconds(2));
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, 1);

    insertToCache(cache, cache_size, cache_size + 1);

    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_RANGE_NOT_EXIST(cache, 1, 2);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, 1);
    VERIFY_CACHE_RANGE_EXIST(cache, 2, cache_size + 1);
}

TEST_F(BucketLRUCacheTest, EvictWithSkipInterval) {
    int cache_size = 5;
    BucketLRUCache<String, String> cache(cache_size, 1, 2);

    insertToCache(cache, 0, cache_size);
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, cache_size));

    cache.get("4");
    cache.get("3");
    cache.get("2");
    cache.get("1");
    cache.get("0");
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, cache_size));
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);

    std::this_thread::sleep_for(std::chrono::seconds(2));
    cache.get("0");
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, std::vector<String>({"1", "2", "3", "4", "0"}));
}

TEST_F(BucketLRUCacheTest, InorderEvict) {
    int cache_size = 10;
    BucketLRUCache<String, String> cache(cache_size, 1, 2);

    int total_size = 10000;
    insertToCache(cache, 0, total_size);

    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(total_size - cache_size, total_size));
    VERIFY_CACHE_RANGE_NOT_EXIST(cache, 0, total_size - cache_size);
    VERIFY_CACHE_RANGE_EXIST(cache, total_size - cache_size, total_size);
}

TEST_F(BucketLRUCacheTest, SetInsert) {
    int cache_size = 5;
    BucketLRUCache<String, String> cache(cache_size, 1, 2);

    insertToCache(cache, 0, cache_size - 1);
    VERIFY_CACHE_COUNT(cache, cache_size - 1);
    VERIFY_CACHE_WEIGHT(cache, cache_size - 1);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, cache_size - 1);
    VERIFY_CACHE_RANGE_NOT_EXIST(cache, cache_size - 1, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, cache_size - 1));

    insertToCache(cache, cache_size - 1, cache_size);
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, cache_size));
}

TEST_F(BucketLRUCacheTest, SetUpdate) {
    int cache_size = 3;
    BucketLRUCache<String, String> cache(cache_size, 1, 2);

    insertToCache(cache, 0, cache_size);
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);

    VERIFY_CACHE_RANGE_EXIST(cache, 0, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, cache_size));

    cache.set("0", std::make_shared<String>("3"));
    ASSERT_EQ(*cache.get("0"), "3");
    VERIFY_CACHE_COUNT(cache, cache_size);
    VERIFY_CACHE_WEIGHT(cache, cache_size);
    VERIFY_CACHE_LRU_ORDER(cache, std::vector<String>({"1", "2", "0"}));
}

TEST_F(BucketLRUCacheTest, Remove) {
    BucketLRUCache<String, String> cache(5, 1, 1);

    insertToCache(cache, 0, 5);
    VERIFY_CACHE_COUNT(cache, 5);
    VERIFY_CACHE_WEIGHT(cache, 5);
    VERIFY_CACHE_RANGE_EXIST(cache, 0, 5);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(0, 5));

    removeFromCache(cache, std::string("0"));
    removeFromCache(cache, std::string("4"));

    VERIFY_CACHE_COUNT(cache, 3);
    VERIFY_CACHE_WEIGHT(cache, 3);
    VERIFY_CACHE_RANGE_EXIST(cache, 1, 4);
    VERIFY_CACHE_LRU_ORDER(cache, generateRangeStr(1, 4));

    removeFromCache(cache, std::string("2"));
    VERIFY_CACHE_COUNT(cache, 2);
    VERIFY_CACHE_WEIGHT(cache, 2);
    VERIFY_CACHE_RANGE_EXIST(cache, 1, 2);
    VERIFY_CACHE_RANGE_EXIST(cache, 3, 4);
    VERIFY_CACHE_LRU_ORDER(cache, std::vector<String>({"1", "3"}));

    insertToCache(cache, 5, 6);
    VERIFY_CACHE_COUNT(cache, 3);
    VERIFY_CACHE_WEIGHT(cache, 3);
    VERIFY_CACHE_RANGE_EXIST(cache, 1, 2);
    VERIFY_CACHE_RANGE_EXIST(cache, 3, 4);
    VERIFY_CACHE_RANGE_EXIST(cache, 5, 6);
    VERIFY_CACHE_LRU_ORDER(cache, std::vector<String>({"1", "3", "5"}));

    insertToCache(cache, 4, 5);
    VERIFY_CACHE_COUNT(cache, 4);
    VERIFY_CACHE_WEIGHT(cache, 4);
    VERIFY_CACHE_RANGE_EXIST(cache, 1, 2);
    VERIFY_CACHE_RANGE_EXIST(cache, 3, 6);
    VERIFY_CACHE_LRU_ORDER(cache, std::vector<String>({"1", "3", "5", "4"}));
}

TEST_F(BucketLRUCacheTest, Fuzzy) {
    int cache_size = 10;
    int mapping_bucket_size = 2;
    int lru_update_interval = 1;
    int worker_num = 10;
    int range_begin = 0;
    int range_end = 100;
    int per_thread_op_count = 100000;

    BucketLRUCache<String, String> cache(cache_size, lru_update_interval, mapping_bucket_size);
    auto worker = [=, &cache]() {
        std::default_random_engine re;
        std::uniform_int_distribution<int> num_dist(range_begin, range_end);
        std::uniform_int_distribution<int> op_dist(0, 1);
        for (int count = 0; count < per_thread_op_count; count++) {
            if (op_dist(re)) {
                // Read op
                String key = std::to_string(num_dist(re));
                auto res = cache.get(key);
                if (res != nullptr) {
                    ASSERT_EQ(*res, key);
                }
            } else {
                // Set op
                String key = std::to_string(num_dist(re));
                cache.set(key, std::make_shared<String>(key));
            }
        }
    };

    std::vector<std::thread> workers;
    for (int i = 0; i < worker_num; i++) {
        workers.emplace_back(worker);
    }
    for (auto& worker : workers) {
        worker.join();
    }
}

int main(int argc, char** argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
