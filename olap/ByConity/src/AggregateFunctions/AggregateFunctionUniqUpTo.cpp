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
#include <AggregateFunctions/Helpers.h>
#include <AggregateFunctions/AggregateFunctionUniqUpTo.h>
#include <Common/FieldVisitorConvertToNumber.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDate32.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeFixedString.h>


namespace DB
{

struct Settings;

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ARGUMENT_OUT_OF_BOUND;
}


namespace
{

constexpr UInt8 uniq_upto_max_threshold = 100;


AggregateFunctionPtr createAggregateFunctionUniqUpTo(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
{
    UInt8 threshold = 5;    /// default value

    if (!params.empty())
    {
        if (params.size() != 1)
            throw Exception("Aggregate function " + name + " requires one parameter or less.", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 threshold_param = applyVisitor(FieldVisitorConvertToNumber<UInt64>(), params[0]);

        if (threshold_param > uniq_upto_max_threshold)
            throw Exception("Too large parameter for aggregate function " + name + ". Maximum: " + toString(uniq_upto_max_threshold),
                ErrorCodes::ARGUMENT_OUT_OF_BOUND);

        threshold = threshold_param;
    }

    if (argument_types.empty())
        throw Exception("Incorrect number of arguments for aggregate function " + name,
            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    bool use_exact_hash_function = !isAllArgumentsContiguousInMemory(argument_types);

    if (argument_types.size() == 1)
    {
        const IDataType & argument_type = *argument_types[0];

        AggregateFunctionPtr res(createWithNumericType<AggregateFunctionUniqUpTo>(*argument_types[0], threshold, argument_types, params));

        WhichDataType which(argument_type);
        if (res)
            return res;
        else if (which.isDate())
            return std::make_shared<AggregateFunctionUniqUpTo<DataTypeDate::FieldType>>(threshold, argument_types, params);
        else if (which.isDate32())
            return std::make_shared<AggregateFunctionUniqUpTo<DataTypeDate32::FieldType>>(threshold, argument_types, params);
        else if (which.isDateTime())
            return std::make_shared<AggregateFunctionUniqUpTo<DataTypeDateTime::FieldType>>(threshold, argument_types, params);
        else if (which.isStringOrFixedString())
            return std::make_shared<AggregateFunctionUniqUpTo<String>>(threshold, argument_types, params);
        else if (which.isUUID())
            return std::make_shared<AggregateFunctionUniqUpTo<DataTypeUUID::FieldType>>(threshold, argument_types, params);
        else if (which.isTuple())
        {
            if (use_exact_hash_function)
                return std::make_shared<AggregateFunctionUniqUpToVariadic<true, true>>(argument_types, params, threshold);
            else
                return std::make_shared<AggregateFunctionUniqUpToVariadic<false, true>>(argument_types, params, threshold);
        }
    }

    /// "Variadic" method also works as a fallback generic case for single argument.
    if (use_exact_hash_function)
        return std::make_shared<AggregateFunctionUniqUpToVariadic<true, false>>(argument_types, params, threshold);
    else
        return std::make_shared<AggregateFunctionUniqUpToVariadic<false, false>>(argument_types, params, threshold);
}

}

void registerAggregateFunctionUniqUpTo(AggregateFunctionFactory & factory)
{
    factory.registerFunction("uniqUpTo", {createAggregateFunctionUniqUpTo, {true}});
}

}
