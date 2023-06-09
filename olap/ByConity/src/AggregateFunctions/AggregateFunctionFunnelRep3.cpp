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
#include <AggregateFunctions/AggregateFunctionFunnelRep3.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{

namespace
{

    AggregateFunctionPtr createAggregateFunctionFunnelRep3(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (argument_types.size() != 2)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto * array_type = checkAndGetDataType<DataTypeArray>(argument_types[0].get());
        const auto * interval_array_type = checkAndGetDataType<DataTypeArray>(argument_types[1].get());
        if (!array_type || !interval_array_type)
            throw Exception("Arguments for function " + name + " must be array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (parameters.size() != 2)
        {
            throw Exception("Aggregate funnction " + name + " requires (number_check, number_event).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
        UInt64 watch_numbers = parameters[0].safeGet<UInt64>();
        UInt64 event_numbers = parameters[1].safeGet<UInt64>();

        AggregateFunctionPtr res(createWithIntegerType<AggregateFunctionFunnelRep3>(*array_type->getNestedType(),
                                                                                    watch_numbers, event_numbers,
                                                                                    argument_types, parameters));
        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return res;
    }
}

void registerAggregateFunctionFunnelRep3(AggregateFunctionFactory & factory)
{
    factory.registerFunction("funnelRep3", createAggregateFunctionFunnelRep3, AggregateFunctionFactory::CaseInsensitive);
}

}
