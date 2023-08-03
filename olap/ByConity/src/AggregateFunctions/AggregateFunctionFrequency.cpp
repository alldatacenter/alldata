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
#include <AggregateFunctions/AggregateFunctionFrequency.h>
#include <AggregateFunctions/Helpers.h>
#include <AggregateFunctions/FactoryHelpers.h>

namespace DB
{
namespace
{
    AggregateFunctionPtr createAggregateFunctionFrequency(const std::string &name, const DataTypes &argument_types, const Array &parameters, const Settings *)
    {
        assertNoParameters(name, parameters);
        assertUnary(name, argument_types);

        AggregateFunctionPtr res(createWithNumericType<AggregateFunctionFrequency>(*argument_types[0], argument_types, parameters));

        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;
    }

    AggregateFunctionPtr createAggregateFunctionDistribution(const std::string &name, const DataTypes &argument_types, const Array &parameters, const Settings *)
    {
        assertUnary(name, argument_types);

        if (parameters.size() != 3)
            throw Exception("AggregateFunction " + name + " needs three parameters: (start_val, step, group_num).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        auto first_type = parameters[0].getType();
        if (first_type != Field::Types::Float64 && first_type != Field::Types::Int64 && first_type != Field::Types::UInt64)
        {
            throw Exception("Aggregate function " + name + " needs integer or float for the first parameter.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        // Param step is a non-negative number. And under the aggregate function engine,
        // it can only be float or uint literal.
        auto second_type = parameters[1].getType();
        if (second_type != Field::Types::Float64 && second_type != Field::Types::UInt64)
        {
            throw Exception("The second parameter of aggregate function " + name + " needs greater or equal to 0.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        auto third_type = parameters[2].getType();
        if (third_type != Field::Types::UInt64)
            throw Exception("The third parameter of aggregate function " + name + "needs greater than 0.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        UInt32 is_int = WhichDataType(argument_types[0]).isInt() ? 1u : 0;
        UInt32 is_uint = WhichDataType(argument_types[0]).isUInt() ? 2u : 0 ;
        UInt32 is_float = WhichDataType(argument_types[0]).isFloat() ? 4u : 0;

        UInt32 input_type = is_int | is_uint | is_float;
        if (!input_type)
            throw Exception("Aggregate function " + name + " needs Float32, Float64, Int or UInt type for input value.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (is_uint && first_type == Field::Types::UInt64 && second_type == Field::Types::UInt64)
        {
            AggregateFunctionPtr res(createWithTwoBasicNumericTypes<AggregateFunctionDistribution>(DataTypeUInt64(), *argument_types[0], argument_types, parameters));
            return res;
        }
        else if ((is_int || is_uint) && (first_type == Field::Types::Float64 || second_type == Field::Types::Float64))
        {
            AggregateFunctionPtr res(createWithTwoBasicNumericTypes<AggregateFunctionDistribution>(DataTypeFloat64(), *argument_types[0], argument_types, parameters));
            return res;
        }
        else if (is_float)
        {
            AggregateFunctionPtr res(createWithTwoBasicNumericTypes<AggregateFunctionDistribution>(*argument_types[0], *argument_types[0], argument_types, parameters));
            return res;
        }
        else
        {
            AggregateFunctionPtr res(createWithTwoBasicNumericTypes<AggregateFunctionDistribution>(DataTypeInt64(), *argument_types[0], argument_types, parameters));
            return res;
        }

    }
}

void registerAggregateFunctionFrequency(AggregateFunctionFactory & factory)
{
    factory.registerFunction("frequency", createAggregateFunctionFrequency, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("distribution", createAggregateFunctionDistribution, AggregateFunctionFactory::CaseInsensitive);
}


}
