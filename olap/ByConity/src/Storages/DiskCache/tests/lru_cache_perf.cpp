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

#include <limits>
#include <random>
#include <string>
#include <thread>
#include <vector>
#include <boost/program_options.hpp>
#include <Common/LRUCache.h>
#include <Common/Stopwatch.h>
#include <Storages/DiskCache/BucketLRUCache.h>

using namespace DB;

struct Statistics {
    Statistics(): total_count(0), total_op_time(0), min_op_time(std::numeric_limits<size_t>::max()),
        max_op_time(0) {}

    void update(size_t time_micro_sec) {
        ++total_count;
        total_op_time += time_micro_sec;

        min_op_time = std::min(min_op_time, time_micro_sec);
        max_op_time = std::max(max_op_time, time_micro_sec);
    }

    void aggregate(const Statistics& rhs) {
        total_count += rhs.total_count;
        total_op_time += rhs.total_op_time;
        min_op_time = std::min(min_op_time, rhs.min_op_time);
        max_op_time = std::max(max_op_time, rhs.max_op_time);
    }

    std::string str() {
        return "Count: " + std::to_string(total_count) + ", TotalTime: "
            + std::to_string(total_op_time) + ", MinOp: " + std::to_string(min_op_time)
            + ", MaxOp: " + std::to_string(max_op_time);
    }

    size_t total_count;
    size_t total_op_time;
    size_t min_op_time;
    size_t max_op_time;
};

template <typename Cache>
std::vector<Statistics> insertCacheMultiThread(Cache& cache, int cache_size,
        int thread_num) {
    std::vector<Statistics> worker_statistics(thread_num);
    auto write_worker = [&cache, &worker_statistics](size_t begin, size_t end, int idx) {
        Statistics& statistics = worker_statistics[idx];

        Stopwatch watch;

        for (size_t i = begin; i < end; i++) {
            watch.restart();
            cache.set(std::to_string(i), std::make_shared<String>(std::to_string(i)));
            statistics.update(watch.elapsedMicroseconds());
        }
    };

    std::vector<std::thread> workers;
    size_t offset = 0;
    size_t op_per_thread = cache_size / thread_num;
    for (int i = 0; i < thread_num; i++, offset += op_per_thread) {
        workers.emplace_back(write_worker, offset, offset + op_per_thread, i);
    }

    for (auto& worker : workers) {
        worker.join();
    }

    return worker_statistics;
}

template <typename Cache>
std::vector<Statistics> readCacheMultiThread(Cache& cache, size_t cache_size,
        int thread_num, size_t op_count) {
    std::vector<Statistics> worker_statistics(thread_num);
    auto read_worker = [&cache, &worker_statistics, op_count](int idx, double mean, double stddev) {
        Statistics& statistics = worker_statistics[idx];

        std::default_random_engine re;
        std::normal_distribution<double> dist(mean, stddev);

        Stopwatch watch;

        for (size_t i = 0; i < op_count; i++) {
            String key = std::to_string(static_cast<size_t>(dist(re)));
            watch.restart();
            cache.get(key);
            statistics.update(watch.elapsedMicroseconds());
        }
    };

    std::vector<std::thread> workers;
    for (int i = 0; i < thread_num; i++) {
        workers.emplace_back(read_worker, i, cache_size / 2, 500000);
    }

    for (auto& worker : workers) {
        worker.join();
    }

    return worker_statistics;
}

void perfRead(size_t cache_size, int thread_num, size_t op_count, bool use_blru) {
    std::vector<Statistics> worker_statistics;

    Stopwatch phase_watch;
    if (use_blru) {
        BucketLRUCache<String, String> cache(cache_size, 1, 10000);

        insertCacheMultiThread(cache, cache_size, thread_num);
        std::cout << "Insert cache takes " << phase_watch.elapsedMilliseconds() << " ms" << std::endl;

        phase_watch.restart();
        worker_statistics = readCacheMultiThread(cache, cache_size, thread_num, op_count);
        std::cout << "Read cache takes " << phase_watch.elapsedMilliseconds() << " ms" << std::endl;
    } else {
        LRUCache<String, String> cache(cache_size);

        insertCacheMultiThread(cache, cache_size, thread_num);
        std::cout << "Insert cache takes " << phase_watch.elapsedMilliseconds() << " ms" << std::endl;

        phase_watch.restart();
        worker_statistics = readCacheMultiThread(cache, cache_size, thread_num, op_count);
        std::cout << "Read cache takes " << phase_watch.elapsedMilliseconds() << " ms" << std::endl;
    }

    Statistics total_stats;
    for (auto& stats : worker_statistics) {
        total_stats.aggregate(stats);
    }
    std::cout << total_stats.str() << std::endl;
}

void perfWrite(size_t cache_size, int thread_num, bool use_blru) {
    std::vector<Statistics> worker_statistics;

    if (use_blru) {
        BucketLRUCache<String, String> cache(cache_size, 1, 10000);

        worker_statistics = insertCacheMultiThread(cache, cache_size, thread_num);
    } else {
        LRUCache<String, String> cache(cache_size);

        worker_statistics = insertCacheMultiThread(cache, cache_size, thread_num);
    }

    Statistics total_stats;
    for (auto& stats : worker_statistics) {
        total_stats.aggregate(stats);
    }
    std::cout << total_stats.str() << std::endl;
}

int main(int argc, char** argv) {
    namespace po = boost::program_options;
    po::options_description desc("opts");
    desc.add_options()
        ("cache_size", po::value<size_t>(), "number of cache size")
        ("threads", po::value<int>(), "number of read thread")
        ("op_count", po::value<size_t>(), "Read count per thread")
        ("type", po::value<String>(), "perf type")
        ("cache", po::value<String>(), "cache type");

    po::variables_map options;
    po::store(po::parse_command_line(argc, argv, desc), options);

    size_t cache_size = options["cache_size"].as<size_t>();
    int thread_num = options["threads"].as<int>();
    String perf_type = options["type"].as<String>();
    String cache_type = options["cache"].as<String>();

    if (perf_type != "get" && perf_type != "set") {
        throw Exception("type must be get or set", ErrorCodes::BAD_ARGUMENTS);
    }
    if (cache_type != "lru" && cache_type != "blru") {
        throw Exception("cache type must be lru or blru", ErrorCodes::BAD_ARGUMENTS);
    }

    bool use_bucket_lru = cache_type == "blru";
    if (perf_type == "get") {
        int op_count = options["op_count"].as<size_t>();
        perfRead(cache_size, thread_num, op_count, use_bucket_lru);
    } else {
        perfWrite(cache_size, thread_num, use_bucket_lru);
    }

    return 0;
}
