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

#include <initializer_list>
#include <type_traits>
#include <unordered_map>
#include <Common/Exception.h>
#include <vector>
#include <list>
#include <string>
#include <string_view>
#include <Common/ErrorCodes.h>
namespace DB
{
namespace ErrorCodes
{
    extern const ErrorCode LOGICAL_ERROR;
}

// this append only
template<typename Key, typename Value>
class LinkedHashMap {
public:
    LinkedHashMap() = default;
    template<typename KeyArg, typename ValueArg>
    void emplace_back(KeyArg&& key_arg, ValueArg&& value_args)
    {
        auto index = ordered_storage.size();
        if (mapping.count(key_arg))
        {
            throw Exception("duplicated key is not allowed", ErrorCodes::LOGICAL_ERROR);
        }
        mapping[key_arg] = index;
        ordered_storage.emplace_back(std::forward<KeyArg>(key_arg), std::forward<ValueArg>(value_args));
    }

    LinkedHashMap(std::initializer_list<std::pair<Key, Value>>&& init_list): ordered_storage(std::move(init_list)) {
        size_t index = 0;
        for(auto& [k, v]: ordered_storage)
        {
            (void)v;
            mapping[k] = index++;
        }
    }

    template<typename Iter>
    LinkedHashMap(Iter beg, Iter end)
    {
        insert_back(beg, end);
    }

    void emplace_back(const std::pair<Key, Value> & assignment)
    {
        emplace_back(assignment.first, assignment.second);
    }

    void emplace_back(std::pair<Key, Value> && assignment)
    {
        emplace_back(std::move(assignment.first), std::move(assignment.second));
    }

    template<typename Iter>
    void insert_back(Iter beg, Iter end)
    {
        for(auto iter = beg; iter != end; ++iter)
        {
            this->emplace_back(iter->first, iter->second);
        }
    }

    // TODO: use user-defined key to avoid it
    // non-const iterate is not safe since
    // user may modify the value

    auto begin() {
        return ordered_storage.begin();
    }
    auto end() {
        return ordered_storage.end();
    }

    auto begin() const{
        return ordered_storage.cbegin();
    }
    auto end() const {
        return ordered_storage.cend();
    }

    size_t size() const {
        return ordered_storage.size();
    }

    size_t count(const Key& key) const
    {
        return mapping.count(key);
    }

    bool empty() const {
        return ordered_storage.empty();
    }

    Value& at(const Key& key) {
        auto iter = mapping.find(key);
        if(iter == mapping.end())
        {
            throw Exception("out of bounds", ErrorCodes::LOGICAL_ERROR);
        }
        auto index = iter->second;
        return ordered_storage.at(index).second;
    }

    const Value& at(const Key& key) const {
        return const_cast<LinkedHashMap<Key, Value>*>(this)->at(key);
    }

    const auto& front() const
    {
        return ordered_storage.front();
    }

    const auto& back() const
    {
        return ordered_storage.back();
    }

    LinkedHashMap(const LinkedHashMap &) = default;
    LinkedHashMap(LinkedHashMap &&) = default;

    LinkedHashMap& operator=(const LinkedHashMap &) = default;
    LinkedHashMap& operator=(LinkedHashMap &&) = default;
private:
    std::vector<std::pair<Key, Value>> ordered_storage;
    std::unordered_map<Key, size_t> mapping;
};

} // namespace DB
