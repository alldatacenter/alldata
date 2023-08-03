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

#pragma once

#include <unordered_map>
#include <list>
#include <set>
#include <memory>
#include <chrono>
#include <mutex>
#include <atomic>

#include <Core/Types.h>
#include <common/logger_useful.h>
#include <boost/multi_index_container.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index/hashed_index.hpp>
#include <boost/multi_index/member.hpp>


namespace DB
{

template <typename T>
struct TrivialWeightFunction
{
    size_t operator()(const T &) const
    {
        return 1;
    }
};

// CacheContainer is a key-value structure. It used as the first level key-value mapping of two-level LRU cache.
// Ths container has name-Key pair, where name is the first level key, Key is the first level value and second level key.
// A name may has multiple Keys, and a Key belongs to multiple names.
template<typename Key>
class CacheContainer
{
    struct name_tag{};
    struct key_tag{};

    using CacheIndex = std::pair<String, Key>;

    using CacheIndexes = boost::multi_index_container<
        CacheIndex,
        boost::multi_index::indexed_by<boost::multi_index::ordered_non_unique<boost::multi_index::tag<name_tag>,
                                                                              boost::multi_index::member<CacheIndex, String, &CacheIndex::first>>,
                                       boost::multi_index::ordered_non_unique<boost::multi_index::tag<key_tag>,
                                                                              boost::multi_index::member<CacheIndex, Key, &CacheIndex::second>>>>;

private:
    CacheIndexes indexes;
    std::mutex mutex;

public:
    // name-Key pairs in CacheContainer
    // Get the name contained in CacheContainer by a key
    // If there is no such key, return empty set
    std::set<String> getNames(const Key & key)
    {
        std::lock_guard<std::mutex> lock(mutex);

        auto & key_index = indexes.template get<key_tag>();
        auto it = key_index.equal_range(key);
        if (it.first == it.second)
            return std::set<String>();

        std::set<String> ret;
        for (auto pos = it.first; pos != it.second; pos++)
            ret.insert((*pos).first);
        return ret;
    }

    // name-Key pairs in CacheContainer
    // Get keys contained in CacheContainer by a name
    // If there is no such a key, return empty set.
    std::set<Key> getKeys(const String & name)
    {
        std::lock_guard<std::mutex> lock(mutex);

        auto & name_index = indexes.template get<name_tag>();
        auto it = name_index.equal_range(name);
        if (it.first == it.second)
            return std::set<Key>();

        std::set<Key> ret;
        for (auto pos = it.first; pos != it.second; pos++)
            ret.insert((*pos).second);
        return ret;
    }

    std::set<Key> getAllKeys()
    {
        std::lock_guard<std::mutex> lock(mutex);

        std::set<Key> ret;
        const auto & key_index = indexes.template get<key_tag>();
        for (auto it = key_index.begin(); it != key_index.end(); ++it)
        {
            ret.insert(it->second);
        }
        return ret;
    }

    // name-Key pairs in CacheContainer
    // Insert name-Key pairs into CacheContainer
    // A name may has multiple Keys, and a Key belongs to multiple names
    void insert(const String & name, const Key & key)
    {
        std::lock_guard<std::mutex> lock(mutex);
        indexes.insert({name, key});
    }

    // remove all name-key pairs in CacheContainer by Key
    void remove(const Key & key)
    {
        std::lock_guard<std::mutex> lock(mutex);

        auto & key_index = indexes.template get<key_tag>();
        auto it = key_index.equal_range(key);
        if (it.first == it.second)
            return;

        key_index.erase(it.first, it.second);
    }

    // remove all name-key pairs in CacheContainer by name
    void drop(const String & name)
    {
        std::lock_guard<std::mutex> lock(mutex);

        auto & name_index = indexes.template get<name_tag>();
        auto it = name_index.equal_range(name);
        if (it.frist == it.second)
            return;

        name_index.erase(it.first, it.second);
    }

    void drop()
    {
        std::lock_guard<std::mutex> lock(mutex);

        indexes.template get<key_tag>().clear();
    }
};


/// Thread-safe cache that evicts entries which are not used for a long time.
/// WeightFunction is a functor that takes Mapped as a parameter and returns "weight" (approximate size)
/// of that value.
/// Cache starts to evict entries when their total weight exceeds max_size.
/// Value weight should not change after insertion.
template <typename TKey, typename TMapped, typename HashFunction = std::hash<TKey>, typename WeightFunction = TrivialWeightFunction<TMapped>>
class LRUCache
{
public:
    using Key = TKey;
    using Mapped = TMapped;
    using MappedPtr = std::shared_ptr<Mapped>;

private:
    using Clock = std::chrono::steady_clock;

public:
    LRUCache(size_t max_size_)
        : max_size(std::max(static_cast<size_t>(1), max_size_)) {}

    MappedPtr get(const Key & key)
    {
        std::lock_guard lock(mutex);

        auto res = getImpl(key, lock);
        if (res)
            ++hits;
        else
            ++misses;

        return res;
    }

    void set(const Key & key, const MappedPtr & mapped)
    {
        std::lock_guard lock(mutex);

        setImpl(key, mapped, lock);
    }

    /// If the value for the key is in the cache, returns it. If it is not, calls load_func() to
    /// produce it, saves the result in the cache and returns it.
    /// Only one of several concurrent threads calling getOrSet() will call load_func(),
    /// others will wait for that call to complete and will use its result (this helps prevent cache stampede).
    /// Exceptions occurring in load_func will be propagated to the caller. Another thread from the
    /// set of concurrent threads will then try to call its load_func etc.
    ///
    /// Returns std::pair of the cached value and a bool indicating whether the value was produced during this call.
    template <typename LoadFunc>
    std::pair<MappedPtr, bool> getOrSet(const Key & key, LoadFunc && load_func)
    {
        InsertTokenHolder token_holder;
        {
            std::lock_guard cache_lock(mutex);

            auto val = getImpl(key, cache_lock);
            if (val)
            {
                ++hits;
                return std::make_pair(val, false);
            }

            auto & token = insert_tokens[key];
            if (!token)
                token = std::make_shared<InsertToken>(*this);

            token_holder.acquire(&key, token, cache_lock);
        }

        InsertToken * token = token_holder.token.get();

        std::lock_guard token_lock(token->mutex);

        token_holder.cleaned_up = token->cleaned_up;

        if (token->value)
        {
            /// Another thread already produced the value while we waited for token->mutex.
            ++hits;
            return std::make_pair(token->value, false);
        }

        ++misses;
        token->value = load_func();

        std::lock_guard cache_lock(mutex);

        /// Insert the new value only if the token is still in present in insert_tokens.
        /// (The token may be absent because of a concurrent reset() call).
        bool result = false;
        auto token_it = insert_tokens.find(key);
        if (token_it != insert_tokens.end() && token_it->second.get() == token)
        {
            setImpl(key, token->value, cache_lock);
            result = true;
        }

        if (!token->cleaned_up)
            token_holder.cleanup(token_lock, cache_lock);

        return std::make_pair(token->value, result);
    }

    void getStats(size_t & out_hits, size_t & out_misses) const
    {
        std::lock_guard lock(mutex);
        out_hits = hits;
        out_misses = misses;
    }

    void remove(const Key & key)
    {
        std::lock_guard<std::mutex> lock(mutex);

        auto it = cells.find(key);
        if (it == cells.end())
            return;

        const auto & cell = it->second;
        current_size -= cell.size;
        queue.erase(cell.queue_iterator);
        cells.erase(it);

        //sync inner_container if it exists
        if (inner_container)
            inner_container->remove(key);
    }

    size_t weight() const
    {
        std::lock_guard lock(mutex);
        return current_size;
    }

    size_t count() const
    {
        std::lock_guard lock(mutex);
        return cells.size();
    }

    size_t maxSize() const
    {
        return max_size;
    }

    void reset()
    {
        std::lock_guard lock(mutex);
        queue.clear();
        cells.clear();
        insert_tokens.clear();
        current_size = 0;
        hits = 0;
        misses = 0;
    }

    virtual ~LRUCache() {}

protected:
    using LRUQueue = std::list<Key>;
    using LRUQueueIterator = typename LRUQueue::iterator;

    // A inner container, which can be used as the first level key-value mapping if
    // we construct a two-level LRU cache.
    // For example, in queryCache, inner_container contains (database:table, query_key) pairs.
    // In the meantime, LRUCache itself contains a (query_key, query_result) pairs.
    std::unique_ptr<CacheContainer<Key>> inner_container = nullptr;

    struct Cell
    {
        MappedPtr value;
        size_t size;
        LRUQueueIterator queue_iterator;
    };

    using Cells = std::unordered_map<Key, Cell, HashFunction>;

    Cells cells;

    mutable std::mutex mutex;
private:

    /// Represents pending insertion attempt.
    struct InsertToken
    {
        explicit InsertToken(LRUCache & cache_) : cache(cache_) {}

        std::mutex mutex;
        bool cleaned_up = false; /// Protected by the token mutex
        MappedPtr value; /// Protected by the token mutex

        LRUCache & cache;
        size_t refcount = 0; /// Protected by the cache mutex
    };

    using InsertTokenById = std::unordered_map<Key, std::shared_ptr<InsertToken>, HashFunction>;

    /// This class is responsible for removing used insert tokens from the insert_tokens map.
    /// Among several concurrent threads the first successful one is responsible for removal. But if they all
    /// fail, then the last one is responsible.
    struct InsertTokenHolder
    {
        const Key * key = nullptr;
        std::shared_ptr<InsertToken> token;
        bool cleaned_up = false;

        InsertTokenHolder() = default;

        void acquire(const Key * key_, const std::shared_ptr<InsertToken> & token_, [[maybe_unused]] std::lock_guard<std::mutex> & cache_lock)
        {
            key = key_;
            token = token_;
            ++token->refcount;
        }

        void cleanup([[maybe_unused]] std::lock_guard<std::mutex> & token_lock, [[maybe_unused]] std::lock_guard<std::mutex> & cache_lock)
        {
            token->cache.insert_tokens.erase(*key);
            token->cleaned_up = true;
            cleaned_up = true;
        }

        ~InsertTokenHolder()
        {
            if (!token)
                return;

            if (cleaned_up)
                return;

            std::lock_guard token_lock(token->mutex);

            if (token->cleaned_up)
                return;

            std::lock_guard cache_lock(token->cache.mutex);

            --token->refcount;
            if (token->refcount == 0)
                cleanup(token_lock, cache_lock);
        }
    };

    friend struct InsertTokenHolder;


    InsertTokenById insert_tokens;

    LRUQueue queue;

    /// Total weight of values.
    size_t current_size = 0;
    const size_t max_size;

    std::atomic<size_t> hits {0};
    std::atomic<size_t> misses {0};

    WeightFunction weight_function;

    MappedPtr getImpl(const Key & key, [[maybe_unused]] std::lock_guard<std::mutex> & cache_lock)
    {
        auto it = cells.find(key);
        if (it == cells.end())
        {
            return MappedPtr();
        }

        Cell & cell = it->second;

        /// Move the key to the end of the queue. The iterator remains valid.
        queue.splice(queue.end(), queue, cell.queue_iterator);

        return cell.value;
    }

    void setImpl(const Key & key, const MappedPtr & mapped, [[maybe_unused]] std::lock_guard<std::mutex> & cache_lock)
    {
        auto [it, inserted] = cells.emplace(std::piecewise_construct,
            std::forward_as_tuple(key),
            std::forward_as_tuple());

        Cell & cell = it->second;

        if (inserted)
        {
            try
            {
                cell.queue_iterator = queue.insert(queue.end(), key);
            }
            catch (...)
            {
                cells.erase(it);
                throw;
            }
        }
        else
        {
            current_size -= cell.size;
            queue.splice(queue.end(), queue, cell.queue_iterator);
        }

        cell.value = mapped;
        cell.size = cell.value ? weight_function(*cell.value) : 0;
        current_size += cell.size;

        removeOverflow();
    }

    void removeOverflow()
    {
        size_t current_weight_lost = 0;
        size_t queue_size = cells.size();
        while ((current_size > max_size) && (queue_size > 1))
        {
            const Key & key = queue.front();

            auto it = cells.find(key);
            if (it == cells.end())
            {
                LOG_ERROR(&Poco::Logger::get("LRUCache"), "LRUCache became inconsistent. There must be a bug in it.");
                abort();
            }

            const auto & cell = it->second;

            current_size -= cell.size;
            current_weight_lost += cell.size;

            removeExternal(key, cell.value, cell.size);
            cells.erase(it);
            queue.pop_front();
            --queue_size;
        }

        onRemoveOverflowWeightLoss(current_weight_lost);

        if (current_size > (1ull << 63))
        {
            LOG_ERROR(&Poco::Logger::get("LRUCache"), "LRUCache became inconsistent. There must be a bug in it.");
            abort();
        }
    }

    /// Override this method if you want to track how much weight was lost in removeOverflow method.
    virtual void onRemoveOverflowWeightLoss(size_t /*weight_loss*/) {}
    virtual void removeExternal(const Key & /*key*/, const MappedPtr &/*value*/, size_t /*weight*/) {}
};


}
