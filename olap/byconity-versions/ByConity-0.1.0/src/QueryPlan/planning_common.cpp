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

#include <QueryPlan/planning_common.h>

namespace DB
{

void putIdentities(const NamesAndTypes & columns, Assignments & assignments, NameToType & types)
{
    for (const auto & col: columns)
    {
        assignments.emplace_back(col.name, toSymbolRef(col.name));
        types[col.name] = col.type;
    }
}

FieldSymbolInfos mapSymbols(const FieldSymbolInfos & field_symbol_infos, const std::unordered_map<String, String> & old_to_new)
{
    FieldSymbolInfos result;

    auto find_new_symbol = [&](const auto & old)
    {
        if (auto it = old_to_new.find(old); it != old_to_new.end())
            return it->second;

        throw Exception("Symbol not found", ErrorCodes::LOGICAL_ERROR);
    };

    for (const auto & old_info : field_symbol_infos)
    {
        const auto & sub_column_symbols = old_info.sub_column_symbols;
        FieldSymbolInfo::SubColumnToSymbol new_sub_column_symbols;

        for (const auto & sub_col_item: sub_column_symbols)
            new_sub_column_symbols.emplace(sub_col_item.first, find_new_symbol(sub_col_item.second));

        result.emplace_back(find_new_symbol(old_info.primary_symbol), new_sub_column_symbols);
    }

    return result;
}

}
