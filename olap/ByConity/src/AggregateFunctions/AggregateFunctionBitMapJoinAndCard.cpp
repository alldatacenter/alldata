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
#include <AggregateFunctions/AggregateFunctionBitMapJoinAndCard.h>
#include <AggregateFunctions/AggregateFunctionBitMapJoinAndCard2.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{

AggregateFunctionPtr createAggregateFunctionBitMapJoinAndCard(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() < 4)
        throw Exception("AggregateFunction " + name + " needs at least four arguments", ErrorCodes::NOT_IMPLEMENTED);

    Int32 union_num = 0;
    UInt64 thread_num = 0;
    UInt64 limit_bitmap_number = 0;
    if (parameters.size() == 0)
        throw Exception("AggregateFunction " + name + " needs two parameters (join_num, thread_num)", ErrorCodes::NOT_IMPLEMENTED);
    else
    {
        union_num = static_cast<Int32>(parameters[0].safeGet<UInt64>());
        thread_num = parameters[1].safeGet<UInt64>();

        if (parameters.size() == 3)
            limit_bitmap_number = parameters[2].safeGet<UInt64>();
    }

    if (union_num == 0 || union_num > 8) // a continuos 8 join is meaningless, 1 join is mostly used.
        throw Exception("AggregateFunction " + name + " join_number is in range [1,8]", ErrorCodes::NOT_IMPLEMENTED);
    if (thread_num == 0)
        thread_num = 16;
    if (thread_num > 48) // Several Storage-C machine only have 48 cores, besides 48 threads is large enough
        thread_num = 32; // A user input larger than 48 may means performance first, so a large thread number is set.
    if (limit_bitmap_number == 0 || limit_bitmap_number > 100000000)
        limit_bitmap_number = 100000000; // 100 million

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " needs BitMap64 type for its first argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!WhichDataType(argument_types[1]).isUInt8())
        throw Exception("AggregateFunction " + name + " needs Int type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!WhichDataType(argument_types[2]).isInt32())
        throw Exception("AggregateFunction " + name + " needs Int32 type for its third argument", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr attr_val_type = argument_types[3];

    if (!isString(*attr_val_type))
        throw Exception("AggregateFunction " + name + " needs String type for its fourth argument", ErrorCodes::NOT_IMPLEMENTED);

    for (size_t i = 4; i < argument_types.size(); ++i)
    {
        if (!isString(argument_types[i]))
            throw Exception("AggregateFunction " + name + " needs String type for args...", ErrorCodes::NOT_IMPLEMENTED);
    }

    return std::make_shared<AggregateFunctionBitMapJoinAndCard>(argument_types, union_num, thread_num, limit_bitmap_number);
}

AggregateFunctionPtr createAggregateFunctionBitMapJoinAndCard2(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() < 4)
        throw Exception("AggregateFunction " + name + " needs at least four arguments", ErrorCodes::NOT_IMPLEMENTED);

    Int32 union_num = 0;
    UInt64 thread_num = 0;
    UInt64 limit_bitmap_number = 0;
    if (parameters.size() == 0)
        throw Exception("AggregateFunction " + name + " needs two parameters (join_num, thread_num)", ErrorCodes::NOT_IMPLEMENTED);
    else
    {
        union_num = static_cast<Int32>(parameters[0].safeGet<UInt64>());
        thread_num = parameters[1].safeGet<UInt64>();

        if (parameters.size() == 3)
            limit_bitmap_number = parameters[2].safeGet<UInt64>();
    }

    if (union_num == 0 || union_num > 8) // a continuos 8 join is meaningless, 1 join is mostly used.
        throw Exception("AggregateFunction " + name + " join_number is in range [1,8]", ErrorCodes::NOT_IMPLEMENTED);
    if (thread_num == 0)
        thread_num = 16;
    if (thread_num > 48) // Several Storage-C machine only have 48 cores, and 48 threads is large enough
        thread_num = 32;
    if (limit_bitmap_number == 0 || limit_bitmap_number > 100000000)
        limit_bitmap_number = 100000000; // 100 million

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " needs BitMap64 type for its first argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!WhichDataType(argument_types[1]).isUInt8())
        throw Exception("AggregateFunction " + name + " needs Int type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!WhichDataType(argument_types[2]).isInt32())
        throw Exception("AggregateFunction " + name + " needs Int32 type for its third argument", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr attr_val_type = argument_types[3];

    if (!isString(*attr_val_type))
        throw Exception("AggregateFunction " + name + " needs String type for its fourth argument", ErrorCodes::NOT_IMPLEMENTED);

    for (size_t i = 4; i < argument_types.size(); ++i)
    {
        if (!isString(argument_types[i]))
            throw Exception("AggregateFunction " + name + " needs String type for args...", ErrorCodes::NOT_IMPLEMENTED);
    }

    return std::make_shared<AggregateFunctionBitMapJoinAndCard2>(argument_types, union_num, thread_num, limit_bitmap_number);
}
}

void registerAggregateFunctionsBitMapJoinAndCard(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapJoinAndCard", createAggregateFunctionBitMapJoinAndCard, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapJoinAndCard2", createAggregateFunctionBitMapJoinAndCard2, AggregateFunctionFactory::CaseInsensitive);
}

}
