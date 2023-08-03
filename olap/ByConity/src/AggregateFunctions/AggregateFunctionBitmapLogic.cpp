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
#include <AggregateFunctions/AggregateFunctionBitmapLogic.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>
#include <common/DayNum.h>


namespace DB
{

namespace
{

AggregateFunctionPtr createAggregateFunctionBitMapOr(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 1 )
        throw Exception("AggregateFunction " + name + " need one arguments", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

   return std::make_shared<AggregateFunctionBitMapOr>(argument_types);
}

AggregateFunctionPtr createAggregateFunctionBitMapXor(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 1 )
        throw Exception("AggregateFunction " + name + " need one arguments", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

   return std::make_shared<AggregateFunctionBitMapXor>(argument_types);
}

AggregateFunctionPtr createAggregateFunctionBitMapAnd(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 1 )
        throw Exception("AggregateFunction " + name + " need one arguments", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

   return std::make_shared<AggregateFunctionBitMapAnd>(argument_types);
}

AggregateFunctionPtr createAggregateFunctionBitMapCardinality(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 1 )
        throw Exception("AggregateFunction " + name + " need one arguments", ErrorCodes::NOT_IMPLEMENTED);

    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

   return std::make_shared<AggregateFunctionBitMapCardinality>(argument_types);
}

AggregateFunctionPtr createAggregateFunctionBitMapHas(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 2 )
        throw Exception("AggregateFunction " + name + " need two arguments", ErrorCodes::NOT_IMPLEMENTED);


    if (!isBitmap64(argument_types[0]))
        throw Exception("AggregateFunction " + name + " need BitMap type for its second argument", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr data_type = argument_types[1];

    if (!WhichDataType(data_type).isNativeInt() && !WhichDataType(data_type).isNativeUInt())
        throw Exception("AggregateFunction " + name + " need numeric type for its first argument", ErrorCodes::NOT_IMPLEMENTED);

    return std::make_shared<AggregateFunctionBitMapHas>(argument_types);
}

}

void registerAggregateFunctionsBitmapLogic(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapColumnOr", createAggregateFunctionBitMapOr, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapColumnXor", createAggregateFunctionBitMapXor, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapColumnAnd", createAggregateFunctionBitMapAnd, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapColumnCardinality", createAggregateFunctionBitMapCardinality, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("BitMapColumnHas", createAggregateFunctionBitMapHas, AggregateFunctionFactory::CaseInsensitive);
}

}
