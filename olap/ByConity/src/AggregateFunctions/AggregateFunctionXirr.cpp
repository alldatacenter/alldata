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
#include <AggregateFunctions/Helpers.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/AggregateFunctionXirr.h>


namespace DB
{

namespace
{
    AggregateFunctionPtr createAggregateFunctionXirr(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings * )
    {
        AggregateFunctionPtr res;

        if (argument_types.size() < 2)
            throw Exception("Aggregate function " + name + " requires two arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        std::optional<Float64> guess = std::nullopt;
        if (!parameters.empty())
        {
            const auto & param = parameters[0];
            if (param.getType() == Field::Types::Float64)
                guess = param.get<Float64>();
            else if (isInt64OrUInt64FieldType(param.getType()))
                guess = param.get<Int64>();
        }

        DataTypePtr data_type_0 = argument_types[0], data_type_1 = argument_types[1];
        if (isNumber(data_type_0) && isNumber(data_type_1))
            return AggregateFunctionPtr(createWithTwoNumericTypes<AggregateFunctionXirr>(*data_type_0, *data_type_1, argument_types, parameters, guess));

        throw Exception("Aggregate function " + name + " requires two numeric arguments", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

}

void registerAggregateFunctionXirr(AggregateFunctionFactory & factory)
{
    factory.registerFunction("xirr", createAggregateFunctionXirr, AggregateFunctionFactory::CaseInsensitive);
}

}
