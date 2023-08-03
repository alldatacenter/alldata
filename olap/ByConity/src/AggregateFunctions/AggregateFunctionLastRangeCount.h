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

#include <AggregateFunctions/IAggregateFunction.h>
#include <AggregateFunctions/AggregateRetentionCommon.h>

#include <Columns/ColumnVector.h>
#include <Columns/ColumnArray.h>

#include <DataTypes/DataTypeArray.h>

#include <algorithm>

namespace DB {

    namespace
    {
        using CountType = UInt64;
        struct AggreateFunctionLastRangeCountData
        {
            CountType range_count[1];
        };

        template<typename StateType>
        class AggregateFunctionLastRangeCount final
                :public IAggregateFunctionDataHelper<AggreateFunctionLastRangeCountData,
                AggregateFunctionLastRangeCount<StateType> >
        {
        private:
            UInt64 m_duration;
            UInt64 m_start_index;
            UInt64 m_num_slots;

            bool inRange(const typename ColumnVector<StateType>::Container &container,size_t offset,
                    size_t start, size_t end) const
            {
                // The last n time intervals, including the current day, so it is a closed interval
                for (size_t i = start; i <= end; i++)
                    if (container[offset + i] & 0x01)
                        return true;

                return false;
            }

        public:

            AggregateFunctionLastRangeCount(UInt64 duration, UInt64 startIndex, UInt64 numSlots,
                                           const DataTypes & arguments, const Array & params) :
               IAggregateFunctionDataHelper<AggreateFunctionLastRangeCountData,
                                            AggregateFunctionLastRangeCount<StateType> >(arguments, params),
               m_duration(duration), m_start_index(startIndex), m_num_slots(numSlots) {}


            String getName() const override
            {
                return "lastRangeCount";
            }

            DataTypePtr getReturnType() const override
            {
                return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<CountType> >());
            }

            void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena *) const override
            {
                const auto & array_column = static_cast<const ColumnArray &>(*columns[0]);
                const auto & array_offsets = array_column.getOffsets();
                auto &input_container = static_cast<const ColumnVector<StateType> &>(array_column.getData()).getData();
                const size_t last_offset = (row_num == 0 ? 0 : array_offsets[row_num - 1]);
                const size_t array_size = (array_offsets[row_num] - last_offset);

                if (unlikely(m_start_index + m_num_slots > array_size))
                    return;

                if (unlikely(m_start_index + 1 < m_duration))
                    return;

                for (size_t i = 0; i < m_num_slots; i++) {
                    if (inRange(input_container, last_offset, i + m_start_index + 1 - m_duration, i + m_start_index))
                        this->data(place).range_count[i]++;
                }
            }

            void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
            {
                for (size_t i = 0; i < m_num_slots; i++)
                    this->data(place).range_count[i] += this->data(rhs).range_count[i];
            }

            void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
            {
                auto & range_count = this->data(place).range_count;
                buf.write(reinterpret_cast<const char *>(&range_count[0]), m_num_slots * sizeof(range_count[0]));
            }

            void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena *) const override
            {
                auto & range_count = this->data(place).range_count;
                buf.read(reinterpret_cast<char *>(&range_count[0]), m_num_slots * sizeof(range_count[0]));
            }

            void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
            {
                const auto & range_count = this->data(place).range_count;
                auto & array_to = static_cast<ColumnArray &>(to);

                auto & offsets_to = array_to.getOffsets();
                offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_num_slots);

                auto & data_to = static_cast<ColumnVector<CountType> &>(array_to.getData()).getData();
                data_to.insert(range_count, range_count + m_num_slots);
            }

            void create(const AggregateDataPtr place) const override
            {
                auto *d = new(place) AggreateFunctionLastRangeCountData;
                std::fill(d->range_count, d->range_count + m_num_slots, 0);
            }

            size_t sizeOfData() const override
            {
                return sizeof(AggreateFunctionLastRangeCountData) + m_num_slots * sizeof(CountType);
            }


            bool allocatesMemoryInArena() const override { return false; }
        };
    }
}

