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

#include "AggregateFunctionAttributionCorrelationMerge.h"
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/Helpers.h>
#include <DataTypes/DataTypeString.h>

namespace DB
{
namespace
{
    AggregateFunctionPtr createAggregateFunctionAttributionCorrelationMerge(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        if (argument_types.empty())
            throw Exception("Aggregate function " + name + " need arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const DataTypePtr & argument_type = argument_types[0];
        WhichDataType which(argument_type);
        if (which.idx != TypeIndex::Tuple)
            throw Exception("Aggregate function " + name + " need tuple arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (params.size() > 2)
            throw Exception("Aggregate function " + name + " need no more than two params", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 N = 0;
        if(params.size() > 0)
            N = params[0].safeGet<UInt64>();

        bool need_others = false;
        if (params.size() == 2)
            need_others = params[1].safeGet<UInt64>() > 0;

        return std::make_shared<AggregateFunctionAttributionCorrelationMerge>(N, need_others, argument_types, params);
    }
}

void registerAggregateFunctionAttributionCorrelationMerge(AggregateFunctionFactory & factory)
{
    factory.registerFunction("attributionCorrelationMerge", createAggregateFunctionAttributionCorrelationMerge,
                             AggregateFunctionFactory::CaseInsensitive);
}
}
