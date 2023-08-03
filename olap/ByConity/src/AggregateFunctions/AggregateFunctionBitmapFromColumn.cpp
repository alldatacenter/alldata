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
#include <AggregateFunctions/AggregateFunctionBitmapFromColumn.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{
AggregateFunctionPtr createAggregateFunctionBitMapFromColumn(const std::string & name, const DataTypes & argument_types, const Array & /*parameters*/, const Settings *)
{
    if (argument_types.size() != 1)
        throw Exception("AggregateFunction " + name + " need only one arguments", ErrorCodes::NOT_IMPLEMENTED);

    DataTypePtr data_type = argument_types[0];
    if (!WhichDataType(data_type).isNativeUInt() && !WhichDataType(data_type).isNativeInt())
        throw Exception("AggregateFunction " + name + " need UInt[8-64]/Int[8-64] type for its argument", ErrorCodes::NOT_IMPLEMENTED);

    return std::make_shared<AggregateFunctionBitMapFromColumn>(argument_types);
}
}

void registerAggregateFunctionsBitmapFromColumn(AggregateFunctionFactory & factory)
{
    factory.registerFunction("BitMapFromColumn", createAggregateFunctionBitMapFromColumn, AggregateFunctionFactory::CaseInsensitive);
}
}
