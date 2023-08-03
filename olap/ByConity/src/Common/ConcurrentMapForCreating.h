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

#include <memory>
#include <mutex>
#include <unordered_map>

namespace DB
{
template <class K, class V, class H = std::hash<K>>
class ConcurrentMapForCreating
{
public:
    using Key = K;
    using Value = V;
    using ValuePtr = std::shared_ptr<V>;
    using Hash = H;

    ValuePtr tryGetImpl(const Key & key, std::lock_guard<std::mutex> &) const
    {
        if (auto it = cells.find(key); it == cells.end())
            return nullptr;
        else
            return it->second;
    }

    ValuePtr tryGet(const Key & key) const
    {
        std::lock_guard lock(cells_mutex);
        return tryGetImpl(key, lock);
    }

    /// If the value for the key is in the map, returns it. If it is not, calls load_func() to
    /// produce it, saves the result in the map and returns it.
    /// Only one of several concurrent threads calling getOrSet() will call load_func(),
    /// others will wait for that call to complete and will use its result (this helps prevent map stampede).
    /// Exceptions occurring in load_func will be propagated to the caller. Another thread from the
    /// set of concurrent threads will then try to call its load_func etc.
    ///
    /// Returns std::pair of the mapped value and a bool indicating whether the value was produced during this call.
    template <typename LoadFunc>
    std::pair<ValuePtr, bool> getOrCreate(const Key & key, LoadFunc && load_func)
    {
        InsertTokenHolder token_holder;

        {
            std::lock_guard lock(cells_mutex);

            if (auto value = tryGetImpl(key, lock))
                return std::make_pair(value, false);

            auto & token = insert_tokens[key];
            if (!token)
                token = std::make_shared<InsertToken>(*this);

            token_holder.acquire(&key, token, lock);
        }

        InsertToken * token = token_holder.token.get();

        std::lock_guard token_lock(token->mutex);
        token_holder.cleaned_up = token->cleaned_up;

        /// Another thread already produced the value while we waited for token->mutex.
        if (token->value)
            return std::make_pair(token->value, false);

        token->value = load_func();

        std::lock_guard lock(cells_mutex);
        /// NOTE: check insert_tokens if impl reset()
        cells.try_emplace(key, token->value);

        if (!token->cleaned_up)
            token_holder.cleanup(token_lock, lock);

        return std::make_pair(token->value, true);
    }

    void set(const Key & key, ValuePtr value)
    {
        std::lock_guard<std::mutex> lock(cells_mutex);
        cells[key] = std::move(value);
    }

    size_t erase(const Key & key)
    {
        std::lock_guard<std::mutex> lock(cells_mutex);
        return cells.erase(key);
    }

    size_t size() const
    {
        std::lock_guard lock(cells_mutex);
        return cells.size();
    }

    auto getAll() const
    {
        std::lock_guard lock(cells_mutex);
        return cells;
    }

    template <class F>
    void withAll(F && f) const
    {
        std::lock_guard lock(cells_mutex);
        for (auto & [_, t] : cells)
            f(t);
    }

    template <class F>
    void withAll(F && f)
    {
        std::lock_guard lock(cells_mutex);
        for (auto & [_, t] : cells)
            f(t);
    }

private:
    /// Represents pending insertion attempt.
    struct InsertToken
    {
        explicit InsertToken(ConcurrentMapForCreating & parent_) : parent(parent_) {}

        std::mutex mutex;
        bool cleaned_up = false; /// Protected by the token mutex
        ValuePtr value; /// Protected by the token mutex

        ConcurrentMapForCreating & parent;
        size_t refcount = 0; /// Protected by the parent's mutex
    };

    using InsertTokenById = std::unordered_map<Key, std::shared_ptr<InsertToken>, Hash>;

    /// This class is responsible for removing used insert tokens from the insert_tokens map.
    /// Among several concurrent threads the first successful one is responsible for removal. But if they all
    /// fail, then the last one is responsible.
    struct InsertTokenHolder
    {
        const Key * key = nullptr;
        std::shared_ptr<InsertToken> token;
        bool cleaned_up = false;

        void acquire(const Key * key_, const std::shared_ptr<InsertToken> & token_, std::lock_guard<std::mutex> &)
        {
            key = key_;
            token = token_;
            ++token->refcount;
        }

        void cleanup(std::lock_guard<std::mutex> &, std::lock_guard<std::mutex> &)
        {
            token->parent.insert_tokens.erase(*key);
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

            std::lock_guard parent_lock(token->parent.cells_mutex);
            if (--token->refcount == 0)
                cleanup(token_lock, parent_lock);
        }
    };

    friend struct InsertTokenHolder;


protected:
    mutable std::mutex cells_mutex;
    std::unordered_map<Key, ValuePtr, Hash> cells;

private:
    InsertTokenById insert_tokens;

};


}
