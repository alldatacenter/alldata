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

#include "AggregateFunctionAttributionAnalysis.h"
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/Helpers.h>
#include <DataTypes/DataTypeString.h>

namespace DB
{
namespace
{
    template <typename TYPE>
    std::vector<TYPE> transformArrayIntoVector(const Array& array)
    {
        std::vector<TYPE> res;
        for (Field field : array)
            res.push_back(field.get<TYPE>());
        return res;
    }

    AggregateFunctionPtr createAggregateFunctionAttributionAnalysis(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        if (params.size() < 7 ||  params.size() > 12)
            throw Exception("Aggregate function " + name + " requires more than 7 arguments and less then 11 arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        for (const DataTypePtr& argument_type : argument_types)
        {
            if (argument_type == nullptr)
                throw Exception("Parameter is null", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        String target_event = params[0].get<String>();
        std::vector<String> touch_events = transformArrayIntoVector<String>(params[1].get<Array>());
        std::vector<String> procedure_events = transformArrayIntoVector<String>(params[2].get<Array>());

        UInt64 back_time = params[3].get<UInt64>();
        UInt64 attribution_mode = params[4].get<UInt64>();

        UInt64 transformVal = params[5].safeGet<UInt64>();
        bool other_transform = transformVal > 0;

        std::vector<UInt16> relation_matrix = transformArrayIntoVector<UInt16>(params[6].get<Array>());
        relation_matrix[0] = argument_types.size()-4;

        String time_zone = "Asia/Shanghai";
        if (params.size() > 7)
        {
            time_zone = params[7].get<String>();
        }

        if (attribution_mode < 3 && params.size() != 7 && params.size() != 8)
            throw Exception("Aggregate function " + name + " require (7,8) parameters under such attribution mode", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        double o = 0.4, p = 0.2, q = 0.4;
        if (attribution_mode == 3)
        {
            o = params[8].get<Float64>();
            p = params[9].get<Float64>();
            q = params[10].get<Float64>();

            if(o + p + q != 1.0)
                throw Exception("Total attribution ratios needs to be 1", ErrorCodes::BAD_ARGUMENTS);
        }
        UInt64 t = 1;
        if (attribution_mode == 4)
        {
            t = params[8].get<UInt64>();

            if(t == 0)
                throw Exception("Half-life-time needs to be an integer greater than 0", ErrorCodes::BAD_ARGUMENTS);
        }

        DataTypePtr data_type = argument_types[2];
        if (isNumber(data_type))
        {
            return AggregateFunctionPtr(createWithNumericType<AggregateFunctionAttributionAnalysis>(*argument_types[2], target_event, touch_events, procedure_events,
                                                                                                    back_time, attribution_mode, other_transform, relation_matrix, time_zone, t, o, p, q, argument_types, params));
        }
        throw Exception("AggregateFunction " + name + " need int or float type for its third argument", ErrorCodes::BAD_ARGUMENTS);
    }
}

void registerAggregateFunctionAttributionAnalysis(AggregateFunctionFactory & factory)
{
    factory.registerFunction("attributionAnalysis", createAggregateFunctionAttributionAnalysis,AggregateFunctionFactory::CaseInsensitive);
}
}
