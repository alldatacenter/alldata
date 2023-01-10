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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/AggregateFunctionBitMapJoin.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>
#include <algorithm>
#include <set>

namespace DB
{
    /// Expected format is 'P.I' or 'I', and P means the position,
    /// as well as the I means the index of argument
    PositionIndexPair parsePositionAndIndex(String & input)
    {
        auto first_dot = input.find_first_of('.');
        if (first_dot == std::string::npos)
        {
            UInt64 idx = std::stol(input); // If no dot, the value is index
            return {0xFF, idx};
        }

        auto last_dot = input.find_last_of('.');
        if (first_dot == 0u || first_dot == input.size()-1 || first_dot != last_dot)
            throw Exception("Illegal input type, 'P.I' or 'I' (UInt) is required", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        UInt64 pos = std::stol(input.substr(0, first_dot));
        UInt64 idx = 0;
        if (first_dot != std::string::npos)
            idx = std::stol(input.substr(first_dot+1));
        return {pos, idx};
    }

    // Array of UInt64 just contains argument index, like [3]
    // Array of String contains both argument index and pair of position and argument index, like ['3'] or ['1.3']
    void getParameterOfPositionAndIndex(Array & arr, const String & name, UInt64 union_num, UInt64 argument_num, std::vector<PositionIndexPair> & to)
    {
        if (arr.at(0).getType() == Field::TypeToEnum<String>::value)
        {
            for (size_t i = 0; i < arr.size(); ++i)
            {
                if (arr.at(i).safeGet<String>().empty())
                    throw Exception("AggregateFunction " + name + ": empty string in parameter is invalid", ErrorCodes::LOGICAL_ERROR);
                UInt64 pos = 0, idx = 0;
                std::tie(pos, idx) = parsePositionAndIndex(arr.at(i).safeGet<String>());
                if (pos == 0 || ((pos^0xFF) && pos > union_num+1))
                {
                    throw Exception("AggregateFunction " + name + ": wrong value of keys postion identifier, which starts from 1", ErrorCodes::LOGICAL_ERROR);
                }
                if (idx < 3 || idx > argument_num)
                    throw Exception("AggregateFunction " + name + ": wrong value of key index, which starts from 3", ErrorCodes::LOGICAL_ERROR);
                to.emplace_back(pos, idx);
            }
        }
        else if (arr.at(0).getType() == Field::TypeToEnum<UInt64>::value)
        {
            for (size_t i = 0; i < arr.size(); ++i)
            {
                UInt64 idx = arr.at(i).safeGet<UInt64>();
                if (idx < 3 || idx > argument_num)
                    throw Exception("AggregateFunction " + name + ": wrong value of key index", ErrorCodes::LOGICAL_ERROR);
                to.emplace_back(0xFF, idx);
            }
        }
        else
        {
            throw Exception("AggregateFunction " + name + ": Illegeal type of position and index", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
    }

namespace
{
    AggregateFunctionPtr
    createAggregateFunctionBitMapJoin(const String & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (argument_types.size() < 4)
            throw Exception("AggregateFunction " + name + " needs at least four arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        /// 3 params means default bitmap operation (AND) and default join type (INNER) is configured, and
        /// 5 params means default thread_numuber (32)
        /// 6 params are: (union_num, [join_keys], [group_by_keys], bitmap_op, join_type, thread_number, 0), the last 0 mean result is cardinality
        /// 7 params are: (union_num, [join_keys], [group_by_keys], bitmap_op, join_type, thread_number, result_type) result_type: 0->cardinality, 1->raw bitmap
        if (parameters.size() != 3 && parameters.size() != 5 && parameters.size() != 6 && parameters.size() != 7)
            throw Exception("AggregateFunction " + name + " needs 3, 5, 6 or 7 parameters", ErrorCodes::LOGICAL_ERROR);

        UInt64 union_num = parameters[0].safeGet<UInt64>();
        if (union_num != 1)
            throw Exception("AggregateFunction " + name + " can only support one JOIN now, set 1 please", ErrorCodes::LOGICAL_ERROR);

        Array join_arr = parameters[1].safeGet<Array>();
        Array group_by_arr = parameters[2].safeGet<Array>();
        std::vector<PositionIndexPair> join_keys_idx, group_by_keys_idx; // <position, idx> like a.name or b.id

        getParameterOfPositionAndIndex(join_arr, name, union_num, argument_types.size(), join_keys_idx);
        std::set<UInt64> keys_set;
        for (auto jk : join_keys_idx)
        {
            keys_set.emplace(jk.second);
        }
        if (keys_set.size() != join_keys_idx.size())
            throw Exception("AggregateFunction " + name + ": duplicated join key index, only one is ok", ErrorCodes::LOGICAL_ERROR);

        getParameterOfPositionAndIndex(group_by_arr, name, union_num, argument_types.size(), group_by_keys_idx);

        // judge duplicated group by keys
        for (size_t i = 0; i < group_by_keys_idx.size() - 1; ++i)
        {
            for (size_t j = i+1; j < group_by_keys_idx.size(); ++j)
            {
                // Two case:
                // 1. equal pair, like ['1.3', '1.3']
                // 2. same idx, but one of the postion is not specified(0xFF), like ['3', '1.3']
                if (group_by_keys_idx[i] == group_by_keys_idx[j] ||
                    (group_by_keys_idx[i].second == group_by_keys_idx[j].second
                        && (group_by_keys_idx[i].first == 0xFF || group_by_keys_idx[j].first == 0xFF)))
                    throw Exception("AggregateFunction " + name + ": duplicated group by index", ErrorCodes::LOGICAL_ERROR);
            }
        }

        String logic_str, join_str;
        if (parameters.size() == 5 || parameters.size() == 6)
        {
            logic_str = parameters[3].safeGet<String>();
            join_str = parameters[4].safeGet<String>();
        }

        LogicOperation logic_op(logic_str);
        if (!logic_op.isValid())
            throw Exception(
                "AggregateFunction " + name + " only support logic operation: AND, OR, XOR, besides empty string is also ok",
                DB::ErrorCodes::LOGICAL_ERROR);

        JoinOperation join_op(join_str);
        if (!join_op.isValid())
            throw Exception(
                "AggregateFunction " + name + " only support join type: INNER, LEFT. And empty string means INNER JOIN",
                DB::ErrorCodes::LOGICAL_ERROR);

        UInt64 thread_num = 32;
        if (parameters.size() == 6)
        {
            thread_num = parameters[5].safeGet<UInt64>();
        }

        UInt64 result_type = 0;
        if (parameters.size() == 7)
        {
            result_type = parameters[6].safeGet<UInt64>();
        }
        if (result_type != 0 && result_type != 1)
            throw Exception("AggregateFunction " + name + " only support result_type: 0, 1", ErrorCodes::LOGICAL_ERROR);

        if (!WhichDataType(argument_types[0]).isUInt8())
            throw Exception("AggregateFunction " + name + " needs Int type for its first argument", ErrorCodes::NOT_IMPLEMENTED);

        if (!isBitmap64(argument_types[1]))
            throw Exception(
                "AggregateFunction " + name + " needs BitMap64 type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

        for (size_t i = 2; i < argument_types.size(); ++i)
        {
            if (!isString(argument_types[i]))
                throw Exception("AggregateFunction " + name + " needs String type", ErrorCodes::NOT_IMPLEMENTED);
        }

        return std::make_shared<AggregateFunctionBitMapJoin>(argument_types, union_num, join_keys_idx, group_by_keys_idx, logic_op, join_op, thread_num, result_type);
    }
}

void registerAggregateFunctionsBitMapJoin(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapJoin", createAggregateFunctionBitMapJoin, AggregateFunctionFactory::CaseInsensitive);
}

}
