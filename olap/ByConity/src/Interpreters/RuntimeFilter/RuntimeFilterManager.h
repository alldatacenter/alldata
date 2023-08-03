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

#include <Interpreters/Context.h>
#include <Interpreters/RuntimeFilter/RuntimeFilter.h>
#include <bthread/condition_variable.h>
#include <bthread/mutex.h>
#include <common/logger_useful.h>

#include <unordered_map>
#include <unordered_set>

namespace DB
{
using RuntimeFilterId = size_t;

template <typename Key, typename Value, typename Hash = std::hash<Key>>
class ConcurrentMapShard
{
public:
    void put(const Key & key, Value value)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        map_data.emplace(key, value);
    }

    size_t size()
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        return map_data.size();
    }

    Value get(const Key & key)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        auto it = map_data.find(key);
        if (it == map_data.end())
            throw Exception("Key not found", ErrorCodes::LOGICAL_ERROR);
        return map_data[key];
    }

    Value compute(const Key & key, std::function<Value(const Key &, Value)> func)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        auto it = map_data.find(key);
        if (it == map_data.end())
        {
            auto res = func(key, Value{});
            map_data.emplace(key, res);
            return res;
        }
        auto res = func(key, it->second);
        it->second = res;
        return res;
    }

    bool remove(const Key & key)
    {
        std::unique_lock<bthread::Mutex> lock(mutex);
        return map_data.erase(key);
    }

private:
    bthread::Mutex mutex;
    std::unordered_map<Key, Value, Hash> map_data;
};

template <typename Key, typename Value, typename Hash = std::hash<Key>>
class ConcurrentHashMap
{
public:
    explicit ConcurrentHashMap(size_t num_shard = 128, Hash const & hash_function_ = Hash())
        : hash_function(hash_function_), shards(num_shard), log(&Poco::Logger::get("ConcurrentShardMap"))
    {
        for (unsigned i = 0; i < num_shard; ++i)
            shards[i].reset(new ConcurrentMapShard<Key, Value, Hash>());
    }

    void put(const Key & key, Value value) { getShard(key).put(key, value); }

    Value get(const Key & key) { return getShard(key).get(key); }

    Value compute(const Key & key, std::function<Value(const Key &, Value)> func) { return getShard(key).compute(key, func); }

    bool remove(const Key & key) { return getShard(key).remove(key); }

    size_t size()
    {
        size_t total_size = 0;
        std::for_each(shards.begin(), shards.end(), [&](std::unique_ptr<ConcurrentMapShard<Key, Value>> & element) {
            total_size += element->size();
        });
        return total_size;
    }

private:
    ConcurrentMapShard<Key, Value> & getShard(const Key & key)
    {
        std::size_t const shard_index = hash_function(key) % shards.size();
        return *shards[shard_index];
    }

    Hash hash_function;
    std::vector<std::unique_ptr<ConcurrentMapShard<Key, Value, Hash>>> shards;
    Poco::Logger * log;
};

/// thread-safe
class PartialRuntimeFilter
{
public:
    PartialRuntimeFilter(size_t require_parallel_size_, std::vector<size_t> execute_segment_ids_)
        : require_parallel_size(require_parallel_size_), execute_segment_ids(std::move(execute_segment_ids_))
    {
    }

    size_t merge(RuntimeFilterPtr runtime_filter_, const String & address);
    RuntimeFilterPtr getRuntimeFilter() { return runtime_filter; }
    size_t getRequireParallelSize() const { return require_parallel_size; }
    const std::vector<size_t> & getExecuteSegmentIds() const { return execute_segment_ids; }

private:
    bthread::Mutex mutex;
    RuntimeFilterPtr runtime_filter;
    std::unordered_set<String> merged_address_set;

    const size_t require_parallel_size;
    const std::vector<size_t> execute_segment_ids;
};

class CompleteRuntimeFilter
{
public:
    CompleteRuntimeFilter() = default;

    RuntimeFilterPtr get(size_t timeout_ms)
    {
        std::unique_lock lock(mutex);
        if (timeout_ms != 0)
            cv.wait_for(lock, std::chrono::milliseconds(timeout_ms), [&]() { return runtime_filter.get(); });
        return runtime_filter;
    }

    void set(RuntimeFilterPtr runtime_filter_)
    {
        std::lock_guard lock(mutex);
        runtime_filter = std::move(runtime_filter_);
        cv.notify_all();
    }

private:
    bthread::Mutex mutex;
    bthread::ConditionVariable cv;
    RuntimeFilterPtr runtime_filter;
};

using PartialRuntimeFilterPtr = std::shared_ptr<PartialRuntimeFilter>;
using PartialRuntimeFilters = std::unordered_map<RuntimeFilterId, PartialRuntimeFilterPtr>;
using PartialRuntimeFiltersPtr = std::shared_ptr<PartialRuntimeFilters>;
using CompleteRuntimeFilterPtr = std::shared_ptr<CompleteRuntimeFilter>;

class RuntimeFilterManager final : private boost::noncopyable
{
public:
    static RuntimeFilterManager & getInstance();

    /// call before plan segment schedule in coordinator
    void registerQuery(const String & query_id, PlanSegmentTree & plan_segment_tree);

    /// call on query finish in coordinator
    void removeQuery(const String & query_id);

    /// merge partial runtime filters received from worker builders
    PartialRuntimeFilterPtr getPartialRuntimeFilter(const String & query_id, RuntimeFilterId filter_id);

    /// call in worker, store runtime filter dispatched from server dispatch
    void putRuntimeFilter(const String & query_id, size_t segment_id, RuntimeFilterId filter_id, RuntimeFilterPtr & runtime_filter);

    /// call in worker, clear after plan segment finish
    void removeRuntimeFilter(const String & query_id, size_t segment_id, RuntimeFilterId filter_id);

    /// call in worker, get runtime filter for execution
    RuntimeFilterPtr getRuntimeFilter(const String & query_id, size_t segment_id, RuntimeFilterId filter_id, size_t timeout_ms);

    static String makeKey(const String & query_id, size_t segment_id, RuntimeFilterId filter_id);

    /// call in worker, get runtime filter for execution
    RuntimeFilterPtr getRuntimeFilter(const String & key, size_t timeout_ms);

private:
    RuntimeFilterManager() : log(&Poco::Logger::get("RuntimeFilterManager")) {}

    /**
     * Coordinator: Query Id -> RuntimeFilters
     */
    ConcurrentHashMap<String, PartialRuntimeFiltersPtr> partial_runtime_filters;

    /**
     * Worker: Query Id, PlanSegment Id, Filter Id -> RuntimeFilters
     */
    ConcurrentHashMap<String, CompleteRuntimeFilterPtr> complete_runtime_filters;

    Poco::Logger * log;
};
}
