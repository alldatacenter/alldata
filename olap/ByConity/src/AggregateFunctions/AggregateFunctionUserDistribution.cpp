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
#include <AggregateFunctions/AggregateFunctionUserDistribution.h>
#include <AggregateFunctions/Helpers.h>

namespace DB {
namespace {

    AggregateFunctionPtr createAggregateFunctionUserDistributionHelper(const std::string &name, const DataTypes &argument_types, const Array &params)
    {
        if (params.size() != 3)
            throw Exception("Aggregate fucntion " + name + " requires (start_time, time_granularity, num_slots).", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (argument_types.size() < 2)
            throw Exception(
                "Incorrect number of arguments for aggregate function " + name + ", should be at least 2",
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!argument_types[0]->equals(*argument_types[1]))
            throw Exception("First two columns should be the same type for aggregate function " + name,
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 start_time = params[0].safeGet<UInt64>();
        UInt64 time_granularity = params[1].safeGet<UInt64>();
        UInt64 num_slots = params[2].safeGet<UInt64>();

        if (time_granularity == 0)
            throw Exception("The Parameter 'time_granularity' should not be zero!", ErrorCodes::LOGICAL_ERROR);

        AggregateFunctionPtr res = nullptr;

        res.reset(createWithNumericType<AggregateFunctionUserDistribution>(
            *argument_types[0], start_time, time_granularity, num_slots, argument_types, params
            ));

        if (!res)
            throw Exception("Illegal type for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;

    }

    AggregateFunctionPtr createAggregateFunctionUserDistribution(const std::string &name, const DataTypes &argument_types, const Array &params, const Settings * )
    {
        return createAggregateFunctionUserDistributionHelper(name, argument_types, params);
    }
}

void registerAggregateFunctionUserDistribution(AggregateFunctionFactory & factory)
{
    factory.registerFunction("userDistribution",
                             createAggregateFunctionUserDistribution,
                             AggregateFunctionFactory::CaseSensitive);
}
}
