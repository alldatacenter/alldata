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
#include <AggregateFunctions/AggregateFunctionFunnelRep.h>
#include <AggregateFunctions/AggregateFunctionFunnelRepByTimes.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{

namespace
{

    AggregateFunctionPtr createAggregateFunctionFunnelRep(const std::string & name, const DataTypes & argument_types,
                                                          const Array & parameters, bool is_by_times = false)
    {
        if (argument_types.size() != 1)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto * array_type = checkAndGetDataType<DataTypeArray>(argument_types[0].get());
        if (!array_type)
            throw Exception("First argument for function " + name + " must be an array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (parameters.size() != 2)
        {
            throw Exception("Aggregate funnction " + name + " requires (number_check, number_event).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
        UInt64 watch_numbers = parameters[0].safeGet<UInt64>();
        UInt64 event_numbers = parameters[1].safeGet<UInt64>();
        AggregateFunctionPtr res;
        if (is_by_times)
            res.reset(createWithIntegerType<AggregateFunctionFunnelRepByTimes>(*array_type->getNestedType(),
                                                                               watch_numbers, event_numbers,
                                                                               argument_types, parameters));
        else
            res.reset(createWithIntegerType<AggregateFunctionFunnelRep>(*array_type->getNestedType(),
                                                                        watch_numbers, event_numbers,
                                                                        argument_types, parameters));
        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return res;
    }

    AggregateFunctionPtr createAggregateFunctionFunnelRepByTimes(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFunnelRep(name, argument_types, params, true);
    }

    AggregateFunctionPtr createAggregateFunctionFunnelRepHelper(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFunnelRep(name, argument_types, params);
    }
}

void registerAggregateFunctionFunnelRep(AggregateFunctionFactory & factory)
{
    factory.registerFunction("funnelRep", createAggregateFunctionFunnelRepHelper, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("funnelRepByTimes", createAggregateFunctionFunnelRepByTimes, AggregateFunctionFactory::CaseInsensitive);
}

}
