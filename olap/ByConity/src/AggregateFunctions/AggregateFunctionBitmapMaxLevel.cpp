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
#include <AggregateFunctions/AggregateFunctionBitmapMaxLevel.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{
AggregateFunctionPtr createAggregateFunctionBitMapMaxLevel(const std::string & name, const DataTypes & argument_types, const Array & parameters,const Settings *)
{
    if (parameters.size() > 2)
        throw Exception("AggregateFunction " + name + " needs 0 or 1 parameters", ErrorCodes::NOT_IMPLEMENTED);

    UInt64 return_tpye{0}; /// 0: only summary; 1: only detail; 2: summay + detail
    if (parameters.size() == 1)
        return_tpye = safeGet<UInt64>(parameters[0]);

    if (argument_types.size() != 2 )
        throw Exception("AggregateFunction " + name + " need two arguments", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr data_type = argument_types[0];
    if (!WhichDataType(data_type).isInt())
        throw Exception("AggregateFunction " + name + " need signed numeric type for its first argument", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[1]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    return std::make_shared<AggregateFunctionBitMapMaxLevel>(argument_types, return_tpye);
}

}

void registerAggregateFunctionsBitmapMaxLevel(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapMaxLevel", createAggregateFunctionBitMapMaxLevel, AggregateFunctionFactory::CaseInsensitive);
}

}
