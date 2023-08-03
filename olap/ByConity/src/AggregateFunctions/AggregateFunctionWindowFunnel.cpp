/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/AggregateFunctionWindowFunnel.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>
#include <Core/Settings.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDate32.h>
#include <DataTypes/DataTypeDateTime.h>

#include <common/range.h>


namespace DB
{
struct Settings;

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

namespace
{

template <template <typename> class Data>
AggregateFunctionPtr
createAggregateFunctionWindowFunnel(const std::string & name, const DataTypes & arguments, const Array & params, const Settings *)
{
    if (params.empty())
        throw Exception{"Aggregate function " + name + " requires at least one parameter: <window>, [option, [option, ...]]", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH};

    if (arguments.size() < 2)
        throw Exception("Aggregate function " + name + " requires one timestamp argument and at least one event condition.", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (arguments.size() > max_events + 1)
        throw Exception("Too many event arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    for (const auto i : collections::range(1, arguments.size()))
    {
        const auto * cond_arg = arguments[i].get();
        if (!isUInt8(cond_arg))
            throw Exception{"Illegal type " + cond_arg->getName() + " of argument " + toString(i + 1) + " of aggregate function "
                    + name + ", must be UInt8", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT};
    }

    AggregateFunctionPtr res(createWithUnsignedIntegerType<AggregateFunctionWindowFunnel, Data>(*arguments[0], arguments, params));
    WhichDataType which(arguments.front().get());
    if (res)
        return res;
    else if (which.isDate())
        return std::make_shared<AggregateFunctionWindowFunnel<DataTypeDate::FieldType, Data<DataTypeDate::FieldType>>>(arguments, params);
    else if (which.isDateTime())
        return std::make_shared<AggregateFunctionWindowFunnel<DataTypeDateTime::FieldType, Data<DataTypeDateTime::FieldType>>>(arguments, params);

    throw Exception{"Illegal type " + arguments.front().get()->getName()
            + " of first argument of aggregate function " + name + ", must be Unsigned Number, Date, DateTime",
        ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT};
}

}

void registerAggregateFunctionWindowFunnel(AggregateFunctionFactory & factory)
{
    factory.registerFunction("windowFunnel", createAggregateFunctionWindowFunnel<AggregateFunctionWindowFunnelData>);
}

}
