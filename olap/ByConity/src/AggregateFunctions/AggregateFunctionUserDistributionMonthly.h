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
#include <string>
#include <common/DateLUT.h>


namespace DB
{
    namespace
    {
        template <typename TimestampType>
        class AggregateFunctionUserDistributionMonthly final
                :public AggregateFunctionUserDistributionCommon<TimestampType>
        {
        private:
            UInt64 m_start_time;
            const std::string & m_timezone;
            TimestampType* m_time_duration = nullptr;
            const DateLUTImpl & date_lut;

            bool convertTimeToIndex(const TimestampType timestamp, UInt8 & index) const
            {
                if (timestamp < m_time_duration[0] || timestamp > m_time_duration[2 * this->m_num_slots - 1])
                    return false;

                for (size_t i = 0; i < this->m_num_slots; i++)
                {
                    if (timestamp >= m_time_duration [2 * i] && timestamp <= m_time_duration[2 * i + 1])
                    {
                        index = i;
                        return true;
                    }
                }

                return false;
            }

        public:
            AggregateFunctionUserDistributionMonthly(const std::string& time_zone,
                    UInt64 start_time, UInt64 num_slots,
                    const DataTypes & arguments, const Array & params)
                    : AggregateFunctionUserDistributionCommon<TimestampType>(arguments, params),
                      m_start_time(start_time), m_timezone(time_zone), date_lut(DateLUT::instance(time_zone))
            {
                this->m_num_slots = num_slots;
                this->m_time_duration = new TimestampType[2 * num_slots];

                const auto init_year = date_lut.toYear(m_start_time);
                const auto init_month = date_lut.toMonth(m_start_time);

                for (size_t i = 0; i < this->m_num_slots; i ++)
                {
                    auto current_month = (init_month - 1 + i) % 12 + 1;
                    auto current_year = init_year +  (init_month - 1 + i) / 12;
                    this->m_time_duration[2 * i] = date_lut.makeDateTime(current_year, current_month, 1, 0, 0, 0);

                    auto next_month = current_month % 12 + 1;
                    auto next_year = current_year +  (current_month) / 12;
                    this->m_time_duration[2 * i + 1] = date_lut.makeDateTime(next_year, next_month, 1, 0, 0, 0) - 1;
                }
            }

            virtual ~AggregateFunctionUserDistributionMonthly() override
            {
                if (m_time_duration)
                    delete[] m_time_duration;
            }

            bool computeUserState(const IColumn **columns, size_t row_num, UInt8 & index, StateType & user_state) const override
            {
                TimestampType server_time = static_cast<const ColumnVector<TimestampType> *>(columns[0])->getData()[row_num];
                TimestampType user_register_ts = static_cast<const ColumnVector<TimestampType> *>(columns[1])->getData()[row_num];

                UInt8 arrive_index = 0;
                bool arrive_success = convertTimeToIndex(server_time, arrive_index);
                if (!arrive_success)
                    return false;
                user_state |= ARRIVE;

                UInt8 newone_index = 0;
                bool newone_success = convertTimeToIndex(user_register_ts, newone_index);
                if (newone_success && newone_index == arrive_index)
                    user_state |= NEWONE;

                index = arrive_index;
                return true;
            }

            String getName() const override
            {
                return "userDistributionMonthly";
            }

            bool allocatesMemoryInArena() const override { return false; }

        };
    }
}
