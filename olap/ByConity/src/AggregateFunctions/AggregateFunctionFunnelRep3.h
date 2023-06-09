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
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeTuple.h>
#include <Functions/FunctionHelpers.h>

namespace DB
{

using REPType = UInt64;
const size_t ARITHMETIC_AMOUNT = 6;

class AggregateFunctionFunnelRep3Data
{
public:
    // using Array = PODArray<REPType, 32>;
    // Array value;
    REPType* value;
    // its first elem stores the total amount of rows currently read
    std::vector<Arithmetics> ariths;
};

/// Convert funnel output to TEA's format, it's aggregation semantics
template<typename T>
class AggregateFunctionFunnelRep3 final : public IAggregateFunctionDataHelper<AggregateFunctionFunnelRep3Data, AggregateFunctionFunnelRep3<T>>
{
    UInt32 m_watch_numbers;
    UInt32 m_event_numbers;
    UInt32 m_total_size;
public:
    AggregateFunctionFunnelRep3(UInt64 watchNumbers, UInt64 eventNumbers,
                                const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionFunnelRep3Data, AggregateFunctionFunnelRep3<T>>(arguments, params),
          m_watch_numbers(watchNumbers), m_event_numbers(eventNumbers), m_total_size(eventNumbers * (watchNumbers + 1)){}

    String getName() const override { return "funnelRep3"; }

    void create(const AggregateDataPtr place) const override
    {
        auto* d = new (place) AggregateFunctionFunnelRep3Data;
        d->value = new REPType[m_total_size]();
        d->ariths.resize(m_total_size);
    }

    void freeArea(ConstAggregateDataPtr place) const
    {
        delete[] this->data(place).value;
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes types;
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<REPType>>())));
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<ArithmeticType>>()))));
        return std::make_shared<DataTypeTuple>(types);
    }

    void addArith(IntervalType cur_interval, size_t i, size_t j, std::vector<Arithmetics>& ariths) const
    {
        size_t index = i * m_event_numbers + j;
        if (index >= ariths.size()) return;
        ariths[index].avg_count += 1; // count
        ariths[index].avg_sum += cur_interval; // sum
        if (cur_interval > ariths[index].max) ariths[index].max = cur_interval; //max
        if (cur_interval < ariths[index].min) ariths[index].min = cur_interval; //min
        ariths[index].quantileTDigest.add(cur_interval); //quantiles
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
        const ColumnArray &array_column = static_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & offsets = array_column.getOffsets();
        auto & input_container = static_cast<const ColumnVector<T> &>(array_column.getData()).getData();
        const size_t input_vec_offset = (row_num == 0 ? 0 : offsets[row_num - 1]);
        const size_t input_vec_size = (offsets[row_num] - input_vec_offset);

        size_t max_level{};
        size_t output_offset{};
        for (size_t i = 0; i < input_vec_size; output_offset += m_event_numbers, i++)
        {
            max_level = size_t(input_container[input_vec_offset + i]);

            for (size_t e = 0; e < m_event_numbers; e++)
                this->data(place).value[output_offset + e] += (max_level > e);
        }

        const ColumnArray &ariths_array_column = static_cast<const ColumnArray &>(*columns[1]);
        const auto& nest_ariths_field = ariths_array_column[row_num];
        const auto& nest_ariths_arr = nest_ariths_field.safeGet<Array>();

        auto & ariths = this->data(place).ariths;
        IntervalType cur_interval, total_interval;
        for (size_t i = 1; i < input_vec_size; i++)
        {
            if (i > m_watch_numbers) break;
            const auto& array = nest_ariths_arr[i].safeGet<Array>();
            total_interval = 0;
            for (size_t j = 1; j < array.size(); j++)
            {
                cur_interval = array[j].safeGet<IntervalType>();
                // Arithmetic statistics of the conversion interval in the current layer (from step j to step j+1)
                addArith(cur_interval, i, j, ariths);
                // Arithmetic statistics of conversion intervals in all observation units
                addArith(cur_interval, 0, j, ariths);

                total_interval += cur_interval;
            }

            // Arithmetic statistics of conversion interval in the current observation unit (from step 1 to step m_event_numbers)
            if (array.size() == m_event_numbers)
            {
                addArith(total_interval, i, 0, ariths);
                addArith(total_interval, 0, 0, ariths);
            }
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        auto& cur_elems = this->data(place).value;
        auto& rhs_elems = this->data(rhs).value;
        for(size_t i = 0; i < m_total_size; i++)
            cur_elems[i] += rhs_elems[i];

        auto& cur_ariths = this->data(place).ariths;
        auto& rhs_ariths = this->data(rhs).ariths;
        for(size_t i = 0; i < rhs_ariths.size(); i++)
        {
            cur_ariths[i].avg_count += rhs_ariths[i].avg_count;
            cur_ariths[i].avg_sum += rhs_ariths[i].avg_sum;
            if (rhs_ariths[i].max > cur_ariths[i].max) cur_ariths[i].max = rhs_ariths[i].max;
            if (rhs_ariths[i].min < cur_ariths[i].min) cur_ariths[i].min = rhs_ariths[i].min;
            cur_ariths[i].quantileTDigest.merge(rhs_ariths[i].quantileTDigest);
        }
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        const auto & ariths = this->data(place).ariths;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_total_size* sizeof(REPType));

        size_t size = ariths.size();
        writeBinary(size, buf);
        for(const Arithmetics& ari : ariths)
        {
            writeBinary(ari.avg_count, buf);
            writeBinary(ari.avg_sum, buf);
            writeBinary(ari.max, buf);
            writeBinary(ari.min, buf);
            const_cast<QuantileTDigest<ArithmeticType>&>(ari.quantileTDigest).serialize(buf);
        }
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        auto & value = this->data(place).value;
        auto & ariths = this->data(place).ariths;
        buf.read(reinterpret_cast<char *>(&value[0]), m_total_size * sizeof(REPType));

        size_t size;
        readBinary(size, buf);
        ariths.resize(size);
        for(size_t i = 0; i < size; i++)
        {
            Arithmetics ari;
            readBinary(ari.avg_count, buf);
            readBinary(ari.avg_sum, buf);
            readBinary(ari.max, buf);
            readBinary(ari.min, buf);
            ari.quantileTDigest.deserialize(buf);
            ariths[i] = std::move(ari);
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        ColumnTuple & tuple_to = static_cast<ColumnTuple &>(to);

        const auto & value = this->data(place).value;
        ColumnArray& arr_to = static_cast<ColumnArray &>(tuple_to.getColumn(0));
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();
        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_watch_numbers + 1);

        ColumnArray& nested_arr_to = static_cast<ColumnArray&>(arr_to.getData());
        ColumnArray::Offsets & nested_offsets_to = nested_arr_to.getOffsets();

        typename ColumnVector<REPType>::Container& data_to = static_cast<ColumnVector<REPType> &>(nested_arr_to.getData()).getData();
        size_t cursor = 0;
        for (size_t i = 0; i <= m_watch_numbers; cursor += m_event_numbers, i++)
        {
            nested_offsets_to.push_back((nested_offsets_to.empty() ? 0 : nested_offsets_to.back()) + m_event_numbers);
            data_to.insert(value + cursor, value + cursor + m_event_numbers);
        }

        ColumnArray& ariths_arr_to = static_cast<ColumnArray &>(tuple_to.getColumn(1));
        ColumnArray::Offsets & ariths_arr_offsets_to = ariths_arr_to.getOffsets();
        ariths_arr_offsets_to.push_back((ariths_arr_offsets_to.empty() ? 0 : ariths_arr_offsets_to.back()) + m_watch_numbers + 1);

        ColumnArray& nested_ariths_arr_to = static_cast<ColumnArray&>(ariths_arr_to.getData());
        ColumnArray::Offsets & nested_ariths_arr_offsets_to = nested_ariths_arr_to.getOffsets();
        ColumnArray& inner_ariths_arr_to = static_cast<ColumnArray&>(nested_ariths_arr_to.getData());
        ColumnArray::Offsets & inner_ariths_arr_offsets_to = inner_ariths_arr_to.getOffsets();

        const auto & ariths = this->data(place).ariths;
        size_t index;
        ArithmeticType avg{};
        ArithmeticType levels[] {0.5, 0.75, 0.25};
        size_t permutation[] {2, 0, 1};
        ArithmeticType quantile_res[] {0, 0, 0};
        typename ColumnVector<ArithmeticType>::Container& inner_data_to = static_cast<ColumnVector<ArithmeticType> &>(inner_ariths_arr_to.getData()).getData();
        for (size_t i = 0; i <= m_watch_numbers; i++)
        {
            for (size_t j = 0; j < m_event_numbers; j++)
            {
                index = i * m_event_numbers + j;
                if (!ariths[index].avg_count)
                {
                    inner_ariths_arr_offsets_to.push_back(inner_ariths_arr_offsets_to.empty() ? 0 : inner_ariths_arr_offsets_to.back());
                    continue;
                }
                inner_ariths_arr_offsets_to.push_back((inner_ariths_arr_offsets_to.empty() ? 0 : inner_ariths_arr_offsets_to.back()) + ARITHMETIC_AMOUNT);

                avg = ariths[index].avg_count ? ariths[index].avg_sum / ariths[index].avg_count : 0;
                const_cast<QuantileTDigest<ArithmeticType>&>(ariths[index].quantileTDigest).getMany(levels, permutation, 3, quantile_res);

                inner_data_to.emplace_back(avg);
                inner_data_to.emplace_back(ariths[index].max);
                inner_data_to.emplace_back(ariths[index].min);
                inner_data_to.insert(quantile_res, quantile_res + 3);
            }
            nested_ariths_arr_offsets_to.push_back((nested_ariths_arr_offsets_to.empty() ? 0 : nested_ariths_arr_offsets_to.back()) + m_event_numbers);
        }

        freeArea(place);
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }

};

}
