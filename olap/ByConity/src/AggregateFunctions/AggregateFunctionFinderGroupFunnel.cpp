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
#include <AggregateFunctions/AggregateFunctionFinderGroupFunnel.h>
#include <AggregateFunctions/AggregateFunctionFinderGroupFunnelByTimes.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{
namespace
{
    AggregateFunctionPtr createAggregateFunctionFinderGroupFunnel(const std::string & name, const DataTypes & argument_types, const Array & params, bool is_by_times = false)
    {
        // The fuction signature is vvfunnelOpt(_,_,_,_,UIDX)(T, CT, U, _, ....)
        if (params.size() < 5 || params.size() > 10)
            throw Exception("Aggregate function " + name + " requires (windows_in_seconds, start_time, check_granularity, number_check, [related_num], [relative_window_type, time_zone], [time_interval]), "
                                + "whose optional parameters will be used for attribute association and current day window", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (argument_types.size() < 4)
            throw Exception("Incorrect number of arguments for aggregate function " + name + ", should be at least four.",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!argument_types[0]->equals(*argument_types[1]))
            throw Exception("First two columns should be the same type for aggregate function " + name,
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!typeid_cast<const DataTypeUInt64 *>(argument_types[0].get()))
            throw Exception("First two columns should be the same type as UInt64 " + name,
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        UInt64 attr_related = 0;
        if (params.size() == 6 || params.size() == 8 || params.size() == 9 || params.size() == 10)
            attr_related = params[5].safeGet<UInt64>();

        size_t v = attr_related ?  __builtin_popcount(attr_related)+3 : 3;
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
        UInt64 user_pro_idx = params[4].safeGet<UInt64>();

        if (!watch_step)
            throw Exception("WatchStep should be greater than 0", ErrorCodes::BAD_ARGUMENTS);

        UInt64 window_type = 0;
        String time_zone;
        if (params.size() == 7)
        {
            window_type = params[5].safeGet<UInt64>();
            // wind_type = 0: not a relative procedure (default)
            // wind_type = 1: relative window of the same day of the first event to be checked
            if (!(window_type == 0 || window_type == 1))
                throw Exception("Illegal window_type value", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            time_zone = params[6].safeGet<String>();
        }

        if (params.size() == 8 || params.size() == 9 || params.size() == 10)
        {
            window_type = params[6].safeGet<UInt64>();
            if (!(window_type == 0 || window_type == 1))
                throw Exception("Illegal window_type value", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            time_zone = params[7].safeGet<String>();
        }

        bool time_interval = false;
        if (params.size() == 9 || params.size() == 10)
            time_interval = params[8].safeGet<UInt64>() > 0;

        UInt64 num_virts = argument_types.size() - v;
        // Limitation right now, we only support up to 64 events.
        if (num_virts > NUMBER_STEPS)
            throw Exception("Too many events checked in " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (user_pro_idx > num_virts || user_pro_idx < 1)
            throw Exception("Wrong input for user property index, 1-based.",ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        AggregateFunctionPtr res = nullptr;
        DataTypePtr user_pro_type = argument_types[2];
        if (user_pro_type->isNullable())
            user_pro_type = static_cast<const DataTypeNullable *>(user_pro_type.get())->getNestedType();

        DataTypePtr attr_type = std::make_shared<DataTypeUInt8>();
        if (attr_related)
        {
            attr_type = argument_types[3];
            for (int i = 1; i < __builtin_popcount(attr_related); i++)
            {
                if (argument_types[i+3]->getTypeId() != attr_type->getTypeId())
                    throw Exception("Inconsistent types of associated attributes", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            }
        }

        if (is_by_times)
        {
            if (time_interval && params.size() != 10)
                throw Exception("Need set target calculate interval step on by times funnel " + name,
                                ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

            UInt64 target_step = NUMBER_STEPS + 1;
            if (time_interval) {
                target_step = params[9].safeGet<UInt64>();
            }

            if (typeid_cast<const DataTypeString*>(user_pro_type.get()) ||
                typeid_cast<const DataTypeFixedString*>(user_pro_type.get()))
            {
                res.reset(createWithSingleTypeLastKnown<AggregateFunctionFinderGroupFunnelByTimes>(
                    *attr_type, window, watch_start, watch_step,
                    watch_numbers, user_pro_idx, window_type, time_zone, num_virts, attr_related, time_interval, target_step, argument_types, params));
            }
            else
            {
                res.reset(createWithTypesAndIntegerType<AggregateFunctionFinderGroupNumFunnelByTimes>(
                    *user_pro_type, *attr_type, window, watch_start, watch_step,
                    watch_numbers, user_pro_idx, window_type, time_zone, num_virts, attr_related, time_interval, target_step, argument_types, params));
            }
        }
        else
        {
            if (typeid_cast<const DataTypeString*>(user_pro_type.get()) ||
                typeid_cast<const DataTypeFixedString*>(user_pro_type.get()))
            {
                res.reset(createWithSingleTypeLastKnown<AggregateFunctionFinderGroupFunnel>(
                    *attr_type, window, watch_start, watch_step,
                    watch_numbers, user_pro_idx, window_type, time_zone, num_virts, attr_related, time_interval, argument_types, params));
            }
            else
            {
                res.reset(createWithTypesAndIntegerType<AggregateFunctionFinderGroupNumFunnel>(
                    *user_pro_type, *attr_type, window, watch_start, watch_step,
                    watch_numbers, user_pro_idx, window_type, time_zone, num_virts, attr_related, time_interval, argument_types, params));
            }
        }

        if (!res)
            throw Exception("Illegal types for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;
    }

    AggregateFunctionPtr createAggregateFunctionFinderGroupFunnelHelper(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFinderGroupFunnel(name, argument_types, params);
    }

    AggregateFunctionPtr createAggregateFunctionFinderGroupFunnelByTimesHelper(const std::string & name, const DataTypes & argument_types, const Array & params, const Settings *)
    {
        return createAggregateFunctionFinderGroupFunnel(name, argument_types, params, true);
    }

}

void registerAggregateFunctionFinderGroupFunnel(AggregateFunctionFactory & factory)
{
    factory.registerFunction("finderGroupFunnel", createAggregateFunctionFinderGroupFunnelHelper, AggregateFunctionFactory::CaseInsensitive);
    factory.registerFunction("finderGroupFunnelByTimes", createAggregateFunctionFinderGroupFunnelByTimesHelper, AggregateFunctionFactory::CaseInsensitive);
}
}
