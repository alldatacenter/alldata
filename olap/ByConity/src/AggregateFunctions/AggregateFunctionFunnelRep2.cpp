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
#include <AggregateFunctions/AggregateFunctionFunnelRep2.h>
#include <AggregateFunctions/AggregateFunctionFunnelRep2ByTimes.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{

namespace
{

    template <typename TYPE>
    void transformArrayIntoVector(std::vector<TYPE>& res, const Array& array)
    {
        for (Field field : array)
            res.push_back(field.get<TYPE>());
    }

    AggregateFunctionPtr createAggregateFunctionFunnelRep2(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (argument_types.size() != 2)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto * array_type = checkAndGetDataType<DataTypeArray>(argument_types[0].get());
        if (!array_type)
            throw Exception("Arguments for function " + name + " must be array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (parameters.size() != 4)
        {
            throw Exception("Aggregate function " + name + " requires (number_check, event_numbers, target_step, target_interval_group).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
        UInt64 watch_numbers = parameters[0].safeGet<UInt64>();
        UInt64 event_numbers = parameters[1].safeGet<UInt64>();
        UInt64 target_step = parameters[2].safeGet<UInt64>();

        std::vector<UInt64> target_interval_group;
        transformArrayIntoVector(target_interval_group, parameters[3].safeGet<Array>());

        if (target_interval_group.size() <= 1)
            throw Exception("Target interval group should be greate than 1", ErrorCodes::LOGICAL_ERROR);

        AggregateFunctionPtr res(createWithIntegerType<AggregateFunctionFunnelRep2>(*array_type->getNestedType(),
                                                                                    watch_numbers, event_numbers, target_step, target_interval_group,
                                                                                    argument_types, parameters));
        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return res;
    }

    AggregateFunctionPtr createAggregateFunctionFunnelRep2ByTimes(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (argument_types.size() != 2)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto * array_type = checkAndGetDataType<DataTypeArray>(argument_types[0].get());
        if (!array_type)
            throw Exception("Arguments for function " + name + " must be array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (parameters.size() != 3)
            throw Exception("Aggregate function " + name + " requires (number_check, event_numbers, target_interval_group).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 watch_numbers = parameters[0].safeGet<UInt64>();
        UInt64 event_numbers = parameters[1].safeGet<UInt64>();

        std::vector<UInt64> target_interval_group;
        transformArrayIntoVector(target_interval_group, parameters[2].safeGet<Array>());

        if (target_interval_group.size() <= 1)
            throw Exception("Target interval group should be greate than 1", ErrorCodes::LOGICAL_ERROR);

        AggregateFunctionPtr res(createWithIntegerType<AggregateFunctionFunnelRep2ByTimes>(*array_type->getNestedType(),
                                                                                           watch_numbers, event_numbers, target_interval_group,
                                                                                           argument_types, parameters));
        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return res;
    }

}

void registerAggregateFunctionFunnelRep2(AggregateFunctionFactory & factory)
{
    factory.registerFunction("funnelRep2", createAggregateFunctionFunnelRep2, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("funnelRep2ByTimes", createAggregateFunctionFunnelRep2ByTimes, AggregateFunctionFactory::CaseInsensitive);
}

}
