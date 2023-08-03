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
#include <AggregateFunctions/AggregateFunnelCommon.h>

#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>

#include <Columns/ColumnVector.h>
#include <Common/ArenaAllocator.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>

#include <Columns/ColumnArray.h>
#include <Functions/FunctionHelpers.h>

namespace DB
{

/// Convert funnel output to TEA's format, it's aggregation semantics
template<typename T>
class AggregateFunctionFunnelRep final : public IAggregateFunctionDataHelper<AggregateFunctionFunnelRepData, AggregateFunctionFunnelRep<T>>
{
    UInt32 m_watch_numbers;
    UInt32 m_event_numbers;
    UInt32 m_total_size;
public:
    AggregateFunctionFunnelRep(UInt64 watchNumbers, UInt64 eventNumbers,
                               const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionFunnelRepData, AggregateFunctionFunnelRep<T>>(arguments, params),
          m_watch_numbers(watchNumbers), m_event_numbers(eventNumbers), m_total_size(eventNumbers * (watchNumbers + 1)){}

    String getName() const override { return "funnelRep"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionFunnelRepData;
        std::fill(d->value, d->value + m_total_size, 0);
    }

    size_t sizeOfData() const override
    {
        return sizeof(AggregateFunctionFunnelRepData) + m_total_size * sizeof(REPType);
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(
                 std::make_shared<DataTypeArray>(
                    std::make_shared<DataTypeNumber<REPType>>()));
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
          const ColumnArray &array_column = static_cast<const ColumnArray &>(*columns[0]);
          const IColumn::Offsets & offsets = array_column.getOffsets();
          auto & input_container = static_cast<const ColumnVector<T> &>(array_column.getData()).getData();
          const size_t input_vec_offset = (row_num == 0 ? 0 : offsets[row_num - 1]);
          const size_t input_vec_size = (offsets[row_num] - input_vec_offset);
          // Calculate TEA Count Ouput. e.g. [3, 2, 1, 0, 2], [3, 2, 1, 0, 1]
          // will be converted to:
          // [[2,2,2], [2,2,0], [2,0,0], [0,0,0], [2,1,0]]
          // Rule: Given input[watchIndex], output[watchIndex] is an array:
          // for (auto i : range(m_eventNumbers))
          // output[watchIndex][i] += input[watchIndex] > i ? 1 : 0;

          size_t max_level{};
          size_t output_offset{};

          for (size_t i = 0; i < input_vec_size; output_offset += m_event_numbers, i++)
          {
              max_level = size_t(input_container[input_vec_offset + i]);

              for (size_t e = 0; e < m_event_numbers; e++)
                  this->data(place).value[output_offset + e] += (max_level > e);
          }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        auto& cur_elems = this->data(place).value;
        auto& rhs_elems = this->data(rhs).value;
        for(size_t i = 0; i < m_total_size; i++)
            cur_elems[i] += rhs_elems[i];
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_total_size* sizeof(REPType));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        auto & value = this->data(place).value;
        buf.read(reinterpret_cast<char *>(&value[0]), m_total_size * sizeof(REPType));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        const auto & value = this->data(place).value;
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

	    offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_watch_numbers + 1);

        ColumnArray& nested_arr_to = static_cast<ColumnArray&>(arr_to.getData());
        ColumnArray::Offsets & nested_offsets_to = nested_arr_to.getOffsets();

        typename ColumnVector<REPType>::Container& data_to = static_cast<ColumnVector<REPType> &>(nested_arr_to.getData()).getData();

        const auto begin = std::begin(value);
        size_t cursor = 0;
        for (size_t i = 0; i <= m_watch_numbers; cursor += m_event_numbers, i++)
        {
            nested_offsets_to.push_back((nested_offsets_to.empty() ? 0 : nested_offsets_to.back()) + m_event_numbers);
            data_to.insert(std::next(begin, cursor), std::next(begin, cursor+m_event_numbers));
        }
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }

};

}
