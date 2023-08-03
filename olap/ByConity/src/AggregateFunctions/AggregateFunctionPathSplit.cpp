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
#include <AggregateFunctions/AggregateFunctionPathSplit.h>

#include <common/constexpr_helpers.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int BAD_ARGUMENTS;
}

template <bool is_terminating_event = false, bool deduplicate = false>
AggregateFunctionPtr createAggregateFunctionPathSplit(const String & name, const DataTypes & argument_types, const Array & params, const Settings * )
{
    if (params.size() != 2)
        throw Exception("Aggregate function " + name + " requires 2 parameter", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (argument_types.size() != 3)
    {
        throw Exception(
            "Aggregate function " + name + " requires  3 arguments, but parsed " + std::to_string(argument_types.size()),
            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
    }

    UInt64 max_seesion_size = params[0].safeGet<UInt64>();
    UInt64 max_session_depth = params[1].safeGet<UInt64>();

    if (!max_seesion_size || !max_session_depth)
        throw Exception("Aggregate function " + name + "(>0, >0)(...).", ErrorCodes::BAD_ARGUMENTS);

    return std::make_shared<AggregateFunctionPathSplit<UInt16, is_terminating_event>>(
        max_seesion_size, max_session_depth, argument_types, params);
}

void registerAggregateFunctionPathSplit(AggregateFunctionFactory & factory)
{
    // factory.registerFunction("pathSplit", createAggregateFunctionPathSplit<false>, AggregateFunctionFactory::CaseSensitive);
    // factory.registerFunction("pathSplitR", createAggregateFunctionPathSplit<true>, AggregateFunctionFactory::CaseSensitive);

    static_for<0, 2 * 2>([&](auto ij) {
        String name = "pathSplit";
        constexpr bool i = ij & (1 << 0);
        constexpr bool j = ij & (1 << 1);
        name += (i ? "R" : "");
        name += (j ? "D" : "");
        factory.registerFunction(name, createAggregateFunctionPathSplit<i, j>, AggregateFunctionFactory::CaseSensitive);
    });
}

}
