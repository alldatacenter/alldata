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
#include <AggregateFunctions/AggregateFunctionBitmapColumnDiff.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{
template<template <typename> class Function>
AggregateFunctionPtr createAggregateFunctionBitmapColumnDiff(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    if (argument_types.size() != 2)
        throw Exception("AggregateFunction " + name + " need only two arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    UInt64 return_type_{0}, diff_step_{1};
    String diff_direction_str{"forward"};
    if (!parameters.empty() && parameters.size() != 3)
        throw Exception("AggregateFunction " + name + " need three parameters", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (!parameters.empty())
    {
        parameters[0].tryGet<UInt64>(return_type_);
        parameters[1].tryGet<String>(diff_direction_str);
        parameters[2].tryGet<UInt64>(diff_step_);
    }

    if (!isBitmap64(argument_types[1]))
        throw Exception("AggregateFunction " + name + " need BitMap64 type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr data_type_0 = argument_types[0];
    if (!WhichDataType(data_type_0).isDate() && !WhichDataType(data_type_0).isUInt()
        && !WhichDataType(data_type_0).isInt() && !WhichDataType(data_type_0).isString())
        throw Exception("AggregateFunction " + name + " need Date/Int/UInt/String type for its first argument, for order sorting.", ErrorCodes::NOT_IMPLEMENTED);

    if (WhichDataType(data_type_0).isDate())
        return std::make_shared<AggregateFunctionBitMapColumnDiff<UInt16>>(argument_types, return_type_, diff_direction_str, diff_step_, true);
    else if (WhichDataType(data_type_0).isString())
        return std::make_shared<AggregateFunctionBitMapColumnDiff<String>>(argument_types, return_type_, diff_direction_str, diff_step_);
    else {
        AggregateFunctionPtr res;
        res.reset(createWithNumericType<Function>(*data_type_0, argument_types, return_type_, diff_direction_str, diff_step_));
        return res;
    }
}
}

void registerAggregateFunctionsBitmapColumnDiff(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitmapColumnDiff", createAggregateFunctionBitmapColumnDiff<AggregateFunctionBitMapColumnDiff>, AggregateFunctionFactory::CaseInsensitive);
}
}
