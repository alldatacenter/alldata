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

#pragma once

#include <AggregateFunctions/AggregateFunctionUserDistributionCommon.h>

namespace DB
{
namespace
{
    template<typename TimestampType>
    class AggregateFunctionUserDistribution final
        : public AggregateFunctionUserDistributionCommon<TimestampType>
    {
    private:
        UInt64 m_start_time;
        UInt64 m_time_granularity;

    public:
        AggregateFunctionUserDistribution(UInt64 start_time, UInt64 time_granularity, UInt64 num_slots,
                                          const DataTypes & arguments, const Array & params)
            : AggregateFunctionUserDistributionCommon<TimestampType>(arguments, params),
            m_start_time(start_time)
        {
            this->m_time_granularity = time_granularity;
            this->m_num_slots = num_slots;
        }

        String getName() const override
        {
            return "userDistribution";
        }

        bool computeUserState(const IColumn **columns, size_t row_num, UInt8 & index, StateType & user_state) const override
        {
            TimestampType server_time = static_cast<const ColumnVector<TimestampType> *>(columns[0])->getData()[row_num];
            TimestampType user_register_ts = static_cast<const ColumnVector<TimestampType> *>(columns[1])->getData()[row_num];

            if (unlikely(static_cast<UInt64>(server_time) < m_start_time))
                return false;

            UInt8 arrive_index = (server_time - m_start_time) / (this->m_time_granularity);
            if (arrive_index < this->m_num_slots)
                user_state |= ARRIVE;

            if (static_cast<UInt64>(user_register_ts) >= m_start_time)
            {
                UInt8 newone_index = (user_register_ts - m_start_time) / (this->m_time_granularity);
                if (newone_index < this->m_num_slots && newone_index == arrive_index)
                    user_state |= NEWONE;
            }

            index = arrive_index;
            return true;
        }

        bool allocatesMemoryInArena() const override { return false; }

    };
}
}
