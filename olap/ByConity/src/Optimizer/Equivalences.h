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

#include <unordered_map>
#include <unordered_set>
#include <vector>
#include <memory>

namespace DB
{
// forward declaration
template <typename T, typename Hash, typename Equal>
class Equivalences;

template <typename T, typename Hash = std::hash<T>, typename Equal = std::equal_to<T>>
struct UnionFind
{
    mutable std::unordered_map<T, T, Hash, Equal> parent;

    UnionFind() = default;

    UnionFind(const UnionFind & left, const UnionFind & right)
    {
        parent.insert(left.parent.begin(), left.parent.end());
        parent.insert(right.parent.begin(), right.parent.end());
    }

    T find(const T & v) const
    {
        if (!parent.contains(v))
            parent[v] = v;
        if (v == parent[v])
            return v;
        return parent[v] = find(parent[v]);
    }

    void add(T a, T b)
    {
        a = find(a);
        b = find(b);
        if (a != b)
            parent[b] = a;
    }

    bool isConnected(T a, T b) const
    {
        return find(a) == find(b);
    }

    std::vector<std::unordered_set<T>> getSets()
    {
        std::vector<std::unordered_set<T, Hash, Equal>> result;
        std::unordered_map<T, size_t, Hash, Equal> parent_to_index;

        for (auto & item : parent)
        {
            auto p = find(item.first);
            if (!parent_to_index.contains(p))
            {
                parent_to_index[p] = result.size();
                result.emplace_back();
            }
            result[parent_to_index[p]].insert(item.first);
        }

        return result;
    }
};

template <typename T, typename Hash = std::hash<T>, typename Equal = std::equal_to<T>>
class Equivalences
{
    using EquivalencesType = Equivalences<T, Hash, Equal>;
    using Ptr = std::shared_ptr<EquivalencesType>;
    using Map = std::unordered_map<T, T, Hash, Equal>;
public:
    Equivalences() = default;
    Equivalences(const EquivalencesType & left, const EquivalencesType & right) : union_find(left.union_find, right.union_find) { }

    Equivalences(const Equivalences&) = delete;
    Equivalences& operator=(const Equivalences&) = delete;
    Equivalences(Equivalences &&) noexcept = default;
    Equivalences& operator=(Equivalences &&)  noexcept = default;

    void add(T first, T second)
    {
        map.reset();
        union_find.add(std::move(first), std::move(second));
    }

    bool isEqual(T first, T second) const
    {
        return union_find.isConnected(first, second);
    }

    Ptr translate(std::unordered_map<T, T> & identities) const
    {
        auto result = std::make_shared<EquivalencesType>();
        std::unordered_map<T, std::unordered_set<T>> str_to_set;
        for (auto & item : union_find.parent)
        {
            if (identities.contains(item.first))
            {
                str_to_set[item.second].insert(identities[item.first]);
            }
        }

        for (auto & item : str_to_set)
        {
            auto & set = item.second;
            if (set.size() > 1)
            {
                auto first = *set.begin();
                for (auto iter = set.begin()++; iter != set.end(); iter++)
                {
                    result->add(first, *iter);
                }
            }
        }
        return result;
    }

    Ptr translate(std::unordered_set<T> & identities) const
    {
        auto result = std::make_shared<EquivalencesType>();
        std::unordered_map<T, std::unordered_set<T>> str_to_set;
        for (auto & item : union_find.parent)
        {
            if (identities.contains(item.first))
            {
                str_to_set[item.second].insert(item.first);
            }
        }

        for (auto & item : str_to_set)
        {
            auto & set = item.second;
            if (set.size() > 1)
            {
                auto first = *set.begin();
                for (auto iter = set.begin()++; iter != set.end(); iter++)
                {
                    result->add(first, *iter);
                }
            }
        }
        return result;
    }

    const Map & representMap() const
    {
        if (map)
            return *map;

        std::unordered_map<T, std::unordered_set<T>, Hash, Equal> str_to_set;
        for (auto & item : union_find.parent)
        {
            str_to_set[item.second].insert(item.first);
        }

        map = std::make_unique<Map>();
        for (auto & item : str_to_set)
        {
            auto & set = item.second;
            auto min = *std::min_element(set.begin(), set.end());
            for (auto & str : set)
            {
                (*map)[str] = min;
            }
        }
        return *map;
    }

private:
    UnionFind<T, Hash, Equal> union_find;
    mutable std::unique_ptr<Map> map {}; // cache
};
}
