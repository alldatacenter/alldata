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
#include <Core/NamesAndTypes.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypeNullable.h>
#include <Statistics/CommonErrorCodes.h>
#include <fmt/format.h>

// use namespace to avoid name pollution
namespace DB::Statistics
{
using ColumnDescVector = NamesAndTypes;

struct DecayVerboseResult
{
    DataTypePtr type;
    bool is_nullable = false;
    bool is_low_cardinality = false;
};

namespace impl
{
    template <class>
    inline constexpr bool always_false_v = false;
}


inline DecayVerboseResult decayDataTypeVerbose(DataTypePtr type)
{
    DecayVerboseResult result;

    while (true)
    {
        if (type->lowCardinality())
        {
            type = dynamic_cast<const DataTypeLowCardinality *>(type.get())->getDictionaryType();
            result.is_low_cardinality = true;
        }

        else if (type->isNullable())
        {
            type = dynamic_cast<const DataTypeNullable *>(type.get())->getNestedType();
            result.is_nullable = true;
        }
        else
        {
            break;
        }
    }
    result.type = type;
    return result;
}

inline DataTypePtr decayDataType(const DataTypePtr & type)
{
    return decayDataTypeVerbose(type).type;
}

inline bool isCollectableType(const DataTypePtr & raw_type)
{
    auto res = decayDataTypeVerbose(raw_type);
    auto type = res.type;

    if (isColumnedAsNumber(type))
    {
        return true;
    }
    else if (isStringOrFixedString(type))
    {
        return true;
    }
    else if (isDecimal(type))
    {
        return true;
    }
    else
    {
        return false;
    }
}

inline ColumnDescVector filterCollectableColumns(
    const ColumnDescVector & collectable, const std::vector<String> & target_columns, bool exception_on_unsupported = false)
{
    std::unordered_map<String, DataTypePtr> name_to_type;
    ColumnDescVector result;
    std::vector<String> unsupported_columns;

    for (const auto & elem : collectable)
        name_to_type.emplace(elem.name, elem.type);

    for (const auto & col_name : target_columns)
    {
        if (name_to_type.count(col_name))
        {
            auto type = name_to_type[col_name];
            result.emplace_back(col_name, type);
        }
        else
        {
            unsupported_columns.emplace_back(col_name);
        }
    }

    if (exception_on_unsupported && !unsupported_columns.empty())
    {
        auto err_msg = fmt::format(FMT_STRING("columns ({}) not exist or is not collectable"), fmt::join(unsupported_columns, ", "));
        throw Exception(err_msg, ErrorCodes::BAD_ARGUMENTS);
    }

    return result;
}

}
