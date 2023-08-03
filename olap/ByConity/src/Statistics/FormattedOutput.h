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
#include <Columns/ColumnString.h>
#include <Core/Block.h>
#include <Core/Types.h>
#include <DataTypes/DataTypeString.h>
#include <boost/lexical_cast.hpp>

namespace DB::Statistics
{
struct FormattedOutputData
{
    // TODO: optimize this
    std::unordered_map<String, String> attributes; // kv of attribute, can be min/max/etc

    template <typename T>
    void append(const String & key, const T & value)
    {
        if constexpr (std::is_same_v<T, String>)
        {
            attributes.insert_or_assign(key, value);
        }
        else
        {
            auto v_str = boost::lexical_cast<String>(value);
            attributes.insert_or_assign(key, v_str);
        }
    }
};

inline Block createSampleBlock(const std::vector<String> & ordered_attrs)
{
    Block block;
    ColumnWithTypeAndName col;
    col.column = ColumnString::create();
    col.type = std::make_shared<DataTypeString>();

    for (auto & attr : ordered_attrs)
    {
        col.name = attr;
        block.insert(col);
    }
    return block;
}

inline Block outputFormattedBlock(const std::vector<FormattedOutputData> & raw_data, const std::vector<String> & ordered_attrs)
{
    Block block;

    // attribute -> column

    std::vector<MutableColumnPtr> attrs_columns;

    for (size_t i = 0; i < ordered_attrs.size(); ++i)
    {
        attrs_columns.emplace_back(ColumnString::create());
    }

    auto insert_data = [&](size_t key_id, std::string_view str) {
        // key_id
        attrs_columns[key_id]->insertData(str.data(), str.size());
    };

    auto insert_null = [&](size_t key_id) { attrs_columns[key_id]->insertDefault(); };

    for (auto & data : raw_data)
    {
        // attrs_column["identifier"]->insertData(data.identifier.c_str(), data.identifier.size());
        for (size_t i = 0; i < ordered_attrs.size(); ++i)
        {
            auto attr = ordered_attrs[i];
            if (data.attributes.count(attr))
            {
                auto & v = data.attributes.at(attr);
                insert_data(i, v);
            }
            else
            {
                insert_null(i);
            }
        }
    }
    // block.setColumns(std::move(attrs_columns));
    auto sample = createSampleBlock(ordered_attrs);
    return sample.cloneWithColumns(std::move(attrs_columns));
}

} // namespace DB
