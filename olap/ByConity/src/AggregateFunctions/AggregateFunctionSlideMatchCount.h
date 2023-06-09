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
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>

namespace DB
{

    namespace ErrorCodes
    {
        extern const int BAD_ARGUMENTS;
        extern const int LOGICAL_ERROR;
        extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    }

    namespace
    {
        using CountType = UInt64;
        const UInt8 ARRIVE = 0x01;
        const UInt8 NEWONE = 0x02;
        struct AggregateFunctionSlideMatchCountData
        {
            CountType slide_match_count[1];
        };

        template <typename StateType>
        class AggregateFunctionSlideMatchCount final
                : public IAggregateFunctionDataHelper<AggregateFunctionSlideMatchCountData,
                AggregateFunctionSlideMatchCount<StateType>>
        {
        private:
            UInt64 m_start_index;
            UInt64 m_num_slots;
            UInt64 m_include;

        public:
            AggregateFunctionSlideMatchCount(UInt64 start_index, UInt64 num_slots, UInt64 include,
                                             const DataTypes & arguments, const Array & params)
            :IAggregateFunctionDataHelper<AggregateFunctionSlideMatchCountData,
                                          AggregateFunctionSlideMatchCount<StateType> >(arguments, params),
             m_start_index(start_index), m_num_slots(num_slots), m_include(include) {}

            String getName() const override
            {
                return "slideMatchCount";
            }

            DataTypePtr getReturnType() const override
            {
                return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<CountType> >());
            }

            void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena *) const override
            {
                const auto & user_distribution = static_cast<const ColumnArray& >(*columns[0]);
                auto & user_distribution_container = static_cast<const ColumnVector<StateType> &>(user_distribution.getData()).getData();
                const auto & pattern = static_cast<const ColumnArray&>(*columns[1]);
                auto & pattern_container = static_cast<const ColumnVector<StateType> &>(pattern.getData()).getData();

                const auto & user_distribution_offsets = user_distribution.getOffsets();
                const auto & pattern_offsets = pattern.getOffsets();

                auto last_offsets = (row_num == 0 ? 0: user_distribution_offsets[row_num - 1]);
                auto user_distribution_size = (user_distribution_offsets[row_num] - last_offsets);
                auto pattern_size = pattern_offsets[0];

                if (unlikely(m_start_index + m_num_slots > user_distribution_size))
                    return;

                if (unlikely(m_start_index + 1 < pattern_size))
                    return;

                for (size_t i = 0; i < m_num_slots; i ++)
                {
                    bool hit = true;
                    for (size_t j = 0; j < pattern_size; j ++)
                    {
                        size_t ud_idx = (last_offsets + m_start_index + i) + 1 - pattern_size + j;
                        if ((!m_include) || pattern_container[j] == 0 || pattern_container[j] == (ARRIVE | NEWONE))
                        {
                            if (user_distribution_container[ud_idx] != pattern_container[j])
                            {
                                hit = false;
                                break;
                            }
                        }
                        else if (pattern_container[j] == ARRIVE)
                        {
                            if (!(user_distribution_container[ud_idx] & pattern_container[j]))
                            {
                                hit = false;
                                break;
                            }
                        }
                        else
                        {
                            hit = false;
                            break;
                        }
                    }

                    if (hit)
                        this->data(place).slide_match_count[i] += 1;
                }
            }

            void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
            {
                for (size_t i = 0; i < m_num_slots; i++)
                    this->data(place).slide_match_count[i] += this->data(rhs).slide_match_count[i];
            }

            void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
            {
                auto & slide_match_count = this->data(place).slide_match_count;
                buf.write(reinterpret_cast<const char*>(&slide_match_count[0]), m_num_slots * sizeof(slide_match_count[0]));
            }

            void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena *) const override
            {
                auto & slide_match_count = this->data(place).slide_match_count;
                buf.read(reinterpret_cast<char *>(&slide_match_count[0]), m_num_slots * sizeof(slide_match_count[0]));
            }

            void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
            {
                const auto &slide_match_count = this->data(place).slide_match_count;
                auto &array_to = static_cast<ColumnArray &>(to);

                auto &offsets_to = array_to.getOffsets();
                offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_num_slots);

                auto &data_to = static_cast<ColumnVector<CountType> &>(array_to.getData()).getData();
                data_to.insert(slide_match_count, slide_match_count + m_num_slots);
            }

            void create(const AggregateDataPtr place) const override
            {
                auto d = new (place) AggregateFunctionSlideMatchCountData;
                std::fill(d->slide_match_count, d->slide_match_count + m_num_slots, 0);
            }

            size_t sizeOfData() const override
            {
                return sizeof(AggregateFunctionSlideMatchCountData) + m_num_slots * sizeof(CountType);
            }

            bool allocatesMemoryInArena() const override { return false; }

        };


    }
}
