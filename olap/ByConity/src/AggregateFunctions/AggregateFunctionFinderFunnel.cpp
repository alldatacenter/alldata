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
#include <AggregateFunctions/AggregateFunctionFinderFunnel.h>
#include <AggregateFunctions/AggregateFunctionFinderFunnelByTimes.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{
    AggregateFunctionPtr createAggregateFunctionFinderFunnelHelper(const std::string & name, const DataTypes & argument_types, const Array & params, bool is_by_times = false)
    {
        if (argument_types.size() < 3 || argument_types.size() > 9)
            throw Exception("Incorrect number of arguments for aggregate function " + name + ", requires " +
                                "(windows_in_seconds, start_time, check_granularity, number_check, [related_num], [relative_window_type, time_zone], [time_interval [count_group]] should be at least 3",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!argument_types[0]->equals(*argument_types[1]))
            throw Exception("First two columns should be the same type for aggregate function " + name,
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!typeid_cast<const DataTypeUInt64 *>(argument_types[0].get()))
            throw Exception("First two columns should be the same type as UInt64 " + name,
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 attr_related = 0;
        if (params.size() == 5 || params.size() == 7 || params.size() == 8)
            attr_related = params[4].safeGet<UInt64>();

        size_t v = attr_related ?  __builtin_popcount(attr_related) + 2 : 2;
        bool event_check = v < argument_types.size();
        for (size_t i = v; i < argument_types.size(); i++)
        {
            if (!typeid_cast<const DataTypeUInt8 *>(argument_types[i].get()))
            {
                event_check = false;
                break;
            }
        }
        if (!event_check)
            throw Exception("Illegal types for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        UInt64 window = params[0].safeGet<UInt64>();
        UInt64 watch_start = params[1].safeGet<UInt64>();
        UInt64 watch_step = params[2].safeGet<UInt64>();
        UInt64 watch_numbers = params[3].safeGet<UInt64>();

        if (!watch_numbers || !watch_step)
            throw Exception("WatchNumber/WatchStep should be greater than 0", ErrorCodes::BAD_ARGUMENTS);

        UInt64 window_type = 0;
        String time_zone;
        if (params.size() == 6)
        {
            window_type = params[4].safeGet<UInt64>();
            // wind_type = 0: not a relative procedure (default)
            // wind_type = 1: relative window of the same day of the first event to be checked
            if (!(window_type == 0 || window_type == 1))
                throw Exception("Illegal window_type value", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            time_zone = params[5].safeGet<String>();
        }

        if (params.size() == 7 || params.size() == 8)
        {
            window_type = params[5].safeGet<UInt64>();
            if (!(window_type == 0 || window_type == 1))
                throw Exception("Illegal window_type value", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            time_zone = params[6].safeGet<String>();
        }

        bool time_interval = false;
        if (params.size() == 8 || params.size() == 9)
            time_interval = params[7].safeGet<UInt64>() > 0;

        UInt64 num_virts = argument_types.size() - v;

        AggregateFunctionPtr res = nullptr;

        if (num_virts > NUMBER_STEPS)
            throw Exception("Too many events checked in " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        DataTypePtr attr_type = std::make_shared<DataTypeUInt8>();
        if (attr_related)
        {
            attr_type = argument_types[2];
            for (int i = 1; i < __builtin_popcount(attr_related); i++)
            {
                if (argument_types[i+2]->getTypeId() != attr_type->getTypeId())
                    throw Exception("Inconsistent types of associated attributes", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            }
        }

        if (is_by_times)
        {
            if (time_interval && params.size() != 9)
                throw Exception("Need set target calculate interval step on by times funnel" + name,
                                ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

            UInt64 target_step = NUMBER_STEPS + 1;
            if (time_interval) {
                target_step = params[8].safeGet<UInt64>();
            }

            res.reset(createWithSingleTypeLastKnown<AggregateFunctionFinderFunnelByTimes>(
                *attr_type, window, watch_start, watch_step,
                watch_numbers, window_type, time_zone, num_virts, attr_related, time_interval, target_step, argument_types, params));
        }
        else
            res.reset(createWithSingleTypeLastKnown<AggregateFunctionFinderFunnel>(
                *attr_type, window, watch_start, watch_step,
                watch_numbers, window_type, time_zone, num_virts, attr_related, time_interval, argument_types, params));

        if (!res)
            throw Exception("Illegal type for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;
    }

    AggregateFunctionPtr createAggregateFunctionFinderFunnel(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFinderFunnelHelper(name, argument_types, params);
    }

    AggregateFunctionPtr createAggregateFunctionFinderFunnelByTimes(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFinderFunnelHelper(name, argument_types, params, true);
    }

}

void registerAggregateFunctionFinderFunnel(AggregateFunctionFactory & factory)
{
    factory.registerFunction("finderFunnel", createAggregateFunctionFinderFunnel, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("finderFunnelByTimes", createAggregateFunctionFinderFunnelByTimes, AggregateFunctionFactory::CaseInsensitive);
}
}
