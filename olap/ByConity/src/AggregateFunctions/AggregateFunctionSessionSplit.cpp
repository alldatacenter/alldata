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

#include <AggregateFunctions/AggregateFunctionSessionSplit.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <DataTypes/DataTypeNullable.h>


namespace DB
{

namespace
{

void checkArgumentTypes(const String & name, const DataTypes & argument_types)
{
    static constexpr size_t max_session_argument_size = 20;
    size_t argument_size = argument_types.size();

    if(argument_size > max_session_argument_size)
        throw Exception("Aggregate function " + name + "has to many parameter, max is 20.", ErrorCodes::TOO_MANY_ARGUMENTS_FOR_FUNCTION);

    auto on_error = [&](size_t idx, const String & type_name, const String & require_type) {
        throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
                        "{}-th argument for aggregate function {} not match, should {}, but got {}",
                        toString(idx), name, require_type, type_name);
    };

    if (!typeid_cast<const DataTypeUInt64 *>(argument_types[0].get()))
        on_error(1, argument_types[0]->getName(), "UInt64");

    if (!typeid_cast<const DataTypeString *>(argument_types[1].get()))
        on_error(2, argument_types[1]->getName(), "String");

    if (!typeid_cast<const DataTypeUInt64 *>(argument_types[2].get()))
        on_error(3, argument_types[2]->getName(), "UInt64");

    auto type_bit64 = [](const DataTypePtr & type) -> bool
    {
        return typeid_cast<const DataTypeUInt64 *>(type.get()) || typeid_cast<const DataTypeInt64 *>(type.get());
    };

    for (size_t i = 3; i < argument_size; ++i)
    {
        if (i < 5 && !type_bit64(argument_types[i]))
            on_error(i + 1, argument_types[i]->getName(), "Nullable(Int64/UInt64)");
        if (i >= 5 && !typeid_cast<const DataTypeString *>(argument_types[i].get()))
            on_error(i + 1, argument_types[i]->getName(), "Nullable(String)");
    }
}

AggregateFunctionPtr createAggregateFunctionSessionSplit(const String & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    if (params.size() != 4)
        throw Exception("Aggregate function " + name + " requires 4 parameter", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (argument_types.size() < 7)
        throw Exception("Aggregate function " + name + " requires not less than 7 arguments, but parsed " + toString(argument_types.size()), ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    checkArgumentTypes(name, argument_types);

    UInt64 watch_start = params[0].safeGet<UInt64>();
    UInt64 window_size = params[1].safeGet<UInt64>();
    UInt64 base_time = params[2].safeGet<UInt64>();
    UInt8 type = static_cast<UInt8 >(params[3].safeGet<UInt64>());

    if (type >= 2)
        throw Exception("Aggregate function " + name + " fourth parameter must be 0 or 1", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    if (!window_size)
        throw Exception("Aggregate function " + name + " second parameter should not 0. It should be day(86400), week(604800) or month.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    return std::make_shared<AggregateFunctionSessionSplit>(watch_start, window_size, base_time, type, argument_types, params);
}

AggregateFunctionPtr createAggregateFunctionSessionSplitR2(const String & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    if (params.size() != 4)
        throw Exception("Aggregate function " + name + " requires 4 parameter", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (argument_types.size() != 7)
        throw Exception("Aggregate function " + name + " requires " + std::to_string(7) + " arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    checkArgumentTypes(name, argument_types);

    UInt64 watch_start = params[0].safeGet<UInt64>();
    UInt64 window_size = params[1].safeGet<UInt64>();
    UInt64 base_time = params[2].safeGet<UInt64>();
    UInt8 type = static_cast<UInt8 >(params[3].safeGet<UInt64>());

    if (type > 2)
        throw Exception("Aggregate function " + name + " fourth parameter must be less than 3", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    if (!window_size)
        throw Exception("Aggregate function " + name + " second parameter should be day(86400), week(604800) or month.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    return std::make_shared<AggregateFunctionSessionSplitR2>(watch_start, window_size, base_time, type, argument_types, params);
}

AggregateFunctionPtr createAggregateFunctionPageTime(const String & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    if (params.size() != 3 && params.size() != 4)
        throw Exception("Aggregate function " + name + " requires 3 or 4 parameter", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (argument_types.size() != 7)
        throw Exception("Aggregate function " + name + " requires " + std::to_string(7) + " arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    checkArgumentTypes(name, argument_types);

    UInt64 watch_start = params[0].safeGet<UInt64>();
    UInt64 window_size = params[1].safeGet<UInt64>();
    UInt64 base_time = params[2].safeGet<UInt64>();
    String refer_url = "all";
    if (params.size() == 4)
        refer_url = params[3].safeGet<String>();

    if (!window_size)
        throw Exception("Aggregate function " + name + " second parameter should be day(86400), week(604800) or month.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    return std::make_shared<AggregateFunctionPageTime>(watch_start, window_size, base_time, refer_url, argument_types, params);
}

AggregateFunctionPtr createAggregateFunctionPageTime2(const String & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    if (params.size() != 3)
        throw Exception("Aggregate function " + name + " requires 3 parameter", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (argument_types.size() < 7)
        throw Exception("Aggregate function " + name + " requires not less than 7 arguments, but parsed " + toString(argument_types.size()), ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    checkArgumentTypes(name, argument_types);

    UInt64 watch_start = params[0].safeGet<UInt64>();
    UInt64 window_size = params[1].safeGet<UInt64>();
    UInt64 base_time = params[2].safeGet<UInt64>();

    if (!window_size)
        throw Exception("Aggregate function " + name + " second parameter should be day(86400), week(604800) or month.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    return std::make_shared<AggregateFunctionPageTime2>(watch_start, window_size, base_time, argument_types, params);
}

AggregateFunctionPtr createAggregateFunctionSumMetric(const String & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    if (argument_types.size() != 1)
        throw Exception("Aggregate function " + name + " requires 1 argument", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    // check input type
    if (!typeid_cast<const DataTypeTuple *>(argument_types[0].get()))
        throw Exception("Aggregate function " + name + " Tuple type not matched!", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    return std::make_shared<AggregateFunctionSumMetric>(argument_types, params);
}

}

void registerAggregateFunctionSessionSplit(AggregateFunctionFactory & factory)
{
    factory.registerFunction("sessionSplit", createAggregateFunctionSessionSplit, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("sessionSplitR2", createAggregateFunctionSessionSplitR2, AggregateFunctionFactory::CaseInsensitive);

    factory.registerFunction("sumMetric", createAggregateFunctionSumMetric, AggregateFunctionFactory::CaseInsensitive);

    factory.registerFunction("pageTime", createAggregateFunctionPageTime, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("pageTime2", createAggregateFunctionPageTime2, AggregateFunctionFactory::CaseInsensitive);
}

}

