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

#include <cassert>
#include <list>
#include <unordered_map>

namespace DB
{
template <
    typename Key,
    typename T,
    typename Compare = std::less<std::pair<Key, T>>,
    typename Hash = std::hash<Key>,
    typename KeyEqual = std::equal_to<Key>>
class OrderedCache
{
public:
    using key_type = Key;
    using mapped_type = T;
    using hasher = Hash;
    using key_equal = KeyEqual;
    using key_compare = Compare;

    using value_type = std::pair<const key_type, mapped_type>;
    using list_type = std::list<value_type>;
    using iterator = typename list_type::iterator;
    using const_iterator = typename list_type::const_iterator;

    using map_type = std::unordered_map<key_type, iterator, hasher, key_equal>;
    using map_iterator = typename map_type::iterator;
    using map_const_iterator = typename map_type::const_iterator;
    using size_type = typename map_type::size_type;

public:
    explicit OrderedCache(size_t max_size_) : max_size(std::max(static_cast<size_t>(1), max_size_)){ }

    virtual ~OrderedCache() { clear(); }

    void clear()
    {
        map.clear();
        list.clear();
    }

    size_t getMaxSize() const { return max_size; }

    void setMaxSize(size_t max_size_)
    {
        max_size = std::max(static_cast<size_t>(1), max_size_);
        removeOverFlow();
    }

    OrderedCache(const OrderedCache & x)
    {
        setMaxSize(x.getMaxSize());
        for (auto && kv : x)
            emplace_back(kv);
    }

    OrderedCache & operator=(const OrderedCache & x)
    {
        clear();
        setMaxSize(x.getMaxSize());
        for (auto && kv : x)
            emplace_back(kv);
        return *this;
    }

    OrderedCache(OrderedCache && other) noexcept
        : map{std::move(other.map)}, list{std::move(other.list)}, max_size{std::move(other.max_size)}
    {
    }

    OrderedCache & operator=(OrderedCache && other) noexcept
    {
        max_size = std::move(other.max_size);
        map = std::move(other.map);
        list = std::move(other.list);
        return *this;
    }

    iterator lower_bound(const value_type & value)
    {
        auto it = begin();
        for (; it != end() && comp(*it, value); ++it)
            ;
        return it;
    }

    const_iterator lower_bound(const value_type & value) const
    {
        auto it = begin();
        for (; it != end() && comp(*it, value); ++it)
            ;
        return it;
    }

    iterator upper_bound(const value_type & value)
    {
        auto it = begin();
        for (; it != end() && !comp(value, *it); ++it)
            ;
        return it;
    }

    const_iterator upper_bound(const value_type & value) const
    {
        auto it = begin();
        for (; it != end() && !comp(value, *it); ++it)
            ;
        return it;
    }

    template <typename... Args>
    std::pair<iterator, bool> emplace(Args &&... args)
    {
        value_type value{std::forward<Args>(args)...};

        auto map_it = map.find(value.first);
        if (map_it == map.end())
        {
            auto pos = lower_bound(value);
            auto it = list.insert(pos, value);
            map.emplace(value.first, it);
            removeOverFlow();
            return {it, true};
        }
        else
            return {map_it->second, false};
    }

    std::pair<iterator, bool> insert(const value_type & value)
    {
        auto map_it = map.find(value.first);
        if (map_it == map.end())
        {
            auto pos = lower_bound(value);
            auto it = list.insert(pos, value);
            map.emplace(value.first, it);
            removeOverFlow();
            return {it, true};
        }
        else
            return {map_it->second, false};
    }

    void erase(const key_type & key)
    {
        auto map_it = map.find(key);
        if (map_it == map.end())
            return;

        list.erase(map_it->second);
        map.erase(map_it);
    }

    void erase(iterator it)
    {
        auto map_it = map.find(it->first);
        assert(map_it != map.end());

        list.erase(map_it->second);
        map.erase(map_it);
    }

    iterator find(const key_type & key)
    {
        auto map_it = map.find(key);
        if (map_it == map.end())
            return end();

        return map_it->second;
    }

    const_iterator find(const key_type & key) const
    {
        auto map_it = map.find(key);
        if (map_it == map.end())
            return end();

        return map_it->second;
    }

    size_type count(const key_type & key) const { return map.count(key); }

    size_type size() const { return map.size(); }

    bool empty() const { return map.empty(); }

    const mapped_type & at(const key_type & key) const { return map.at(key)->second; }

    mapped_type & at(const key_type & key) { return map.at(key)->second; }

    mapped_type & operator[](const key_type & key)
    {
        auto it = find(key);
        if (it != end())
            return it->second;

        return emplace(key, mapped_type{}).first->second;
    }

    iterator begin() { return list.begin(); }

    iterator end() { return list.end(); }

    value_type & back() { return list.back(); }

    const value_type & back() const { return list.back(); }

    const_iterator begin() const { return list.begin(); }

    const_iterator end() const { return list.end(); }

protected:
    iterator insert(const_iterator pos, const value_type & value)
    {
        auto map_it = map.find(value.first);
        if (map_it == map.end())
        {
            auto it = list.insert(pos, value);
            map.emplace(value.first, it);
            removeOverFlow();
            return it;
        }
        else
            return map_it->second;
    }

    template <typename... Args>
    std::pair<iterator, bool> emplace_back(Args &&... args)
    {
        value_type value{std::forward<Args>(args)...};

        auto map_it = map.find(value.first);
        if (map_it == map.end())
        {
            key_type key = value.first;
            list.push_back(std::move(value));
            auto it = std::prev(list.end());
            map.emplace(key, it);
            removeOverFlow();
            return {it, true};
        }
        else
            return {map_it->second, false};
    }

private:
    size_t max_size;

    list_type list;
    map_type map;
    Compare comp;

    void removeOverFlow()
    {
        while (size() > max_size)
        {
            map.erase(list.back().first);
            list.pop_back();
        }
    }
};
}
