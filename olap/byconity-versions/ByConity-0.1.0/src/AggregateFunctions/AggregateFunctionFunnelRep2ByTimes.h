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
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeTuple.h>
#include <Functions/FunctionHelpers.h>

namespace DB
{

using std::vector;
using REPType = UInt64;
const size_t ARITHMETIC_AMOUNT_SIZE = 6;

/// Convert funnel output to TEA's format, it's aggregation semantics
template<typename T>
class AggregateFunctionFunnelRep2ByTimes final : public IAggregateFunctionDataHelper<AggregateFunctionFunnelRep2Data, AggregateFunctionFunnelRep2ByTimes<T>>
{
    UInt32 m_watch_numbers;
    UInt32 m_event_numbers;
    // todo UInt64 -> get from the template
    vector<UInt64> target_interval_group;
    UInt32 group_size;
    UInt32 m_total_size;
    UInt32 ariths_size;
public:
    AggregateFunctionFunnelRep2ByTimes(UInt64 watch_numbers, UInt64 event_numbers, vector<UInt64> targetIntervalGroup,
                               const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionFunnelRep2Data, AggregateFunctionFunnelRep2ByTimes<T>>(arguments, params),
          m_watch_numbers(watch_numbers), m_event_numbers(event_numbers), target_interval_group(targetIntervalGroup),
        group_size(target_interval_group.size()-1), m_total_size(group_size * (watch_numbers + 1)), ariths_size(watch_numbers + 1){}

    String getName() const override { return "funnelRep2ByTimes"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionFunnelRep2Data;
        d->value.resize(m_total_size);
        d->ariths.resize(ariths_size);
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes types;
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<REPType>>())));
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<ArithmeticType>>())));
        return std::make_shared<DataTypeTuple>(types);
    }

    void addArith(ArithmeticType cur_interval, size_t index, vector<Arithmetics>& ariths) const
    {
        ariths[index].avg_count += 1; // count
        ariths[index].avg_sum += cur_interval; // sum
        if (cur_interval > ariths[index].max) ariths[index].max = cur_interval; // max
        if (cur_interval < ariths[index].min) ariths[index].min = cur_interval; // min
        ariths[index].quantileTDigest.add(cur_interval); //quantiles
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
        const ColumnArray &array_column = static_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & offsets = array_column.getOffsets();
        const size_t input_vec_offset = (row_num == 0 ? 0 : offsets[row_num - 1]);
        const size_t input_vec_size = (offsets[row_num] - input_vec_offset);

        const ColumnArray &interval_column = static_cast<const ColumnArray &>(*columns[1]);
        // Dont merge these into one step like: const auto& nest_ariths_arr = interval_column[row_num].safeGet<Array>();
        const auto& nest_ariths_field = interval_column[row_num];
        const auto& nest_ariths_arr = nest_ariths_field.safeGet<Array>();

        auto& ariths = this->data(place).ariths;
        size_t group_offset, output_offset = group_size;
        for (size_t i = 1; i < input_vec_size; output_offset += group_size, i++)
        {
            if (i >= ariths_size) break;
            const auto& array = nest_ariths_arr[i].safeGet<Array>();

            if (!array.empty())
            {
                for (const auto & interval : array)
                {
                    UInt64 cur_interval = interval.safeGet<UInt64>();
                    // the group is like [), [), [), []
                    if (unlikely(cur_interval == target_interval_group[target_interval_group.size()-1]))
                    {
                        group_offset = target_interval_group.size()-2;
                        this->data(place).value[output_offset + group_offset] += 1;
                        this->data(place).value[group_offset] += 1;
                    }
                    else
                    {
                        const auto& it = upper_bound(target_interval_group.begin(), target_interval_group.end(), cur_interval);
                        if (it != target_interval_group.begin() && it != target_interval_group.end())
                        {
                            group_offset = it-target_interval_group.begin()-1;
                            this->data(place).value[output_offset + group_offset] += 1;
                            this->data(place).value[group_offset] += 1;
                        }
                    }

                    addArith(cur_interval, i, ariths);
                    addArith(cur_interval, 0, ariths);
                }
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

        if (rhs_ariths.size() != cur_ariths.size())
            throw Exception("rhs aggregate data size is not equal to current data size, rhs: " + std::to_string(rhs_ariths.size()) + ", cur: " + std::to_string(cur_ariths.size()), ErrorCodes::LOGICAL_ERROR);

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
        //buf.write(reinterpret_cast<const char *>(&value[0]), m_total_size* sizeof(REPType));
        writeBinary(value.size(), buf);
        for (const auto & v : value)
            writeBinary(v, buf);

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
        //buf.read(reinterpret_cast<char *>(&value[0]), m_total_size * sizeof(REPType));
        size_t v_size;
        readBinary(v_size, buf);
        value.resize(v_size);
        for (size_t i = 0; i < v_size; i++)
            readBinary(value[i], buf);

        auto & ariths = this->data(place).ariths;
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
        for (size_t i = 0; i <= m_watch_numbers; cursor += group_size, i++)
        {
            nested_offsets_to.push_back((nested_offsets_to.empty() ? 0 : nested_offsets_to.back()) + group_size);
            data_to.insert(value.begin() + cursor, value.begin() + cursor + group_size);
        }

        const auto & ariths = this->data(place).ariths;
        ColumnArray& ariths_arr_to = static_cast<ColumnArray &>(tuple_to.getColumn(1));
        ColumnArray::Offsets & ariths_offsets_to = ariths_arr_to.getOffsets();
        ariths_offsets_to.push_back((ariths_offsets_to.empty() ? 0 : ariths_offsets_to.back()) + m_watch_numbers + 1);

        ColumnArray& nested_ariths_arr_to = static_cast<ColumnArray&>(ariths_arr_to.getData());
        ColumnArray::Offsets & nested_ariths_offsets_to = nested_ariths_arr_to.getOffsets();

        typename ColumnVector<ArithmeticType>::Container& ariths_data_to = static_cast<ColumnVector<ArithmeticType> &>(nested_ariths_arr_to.getData()).getData();
        ArithmeticType avg{};
        ArithmeticType levels[] {0.5, 0.75, 0.25};
        size_t permutation[] {2, 0, 1};
        ArithmeticType quantile_res[] {0, 0, 0};
        for (size_t i = 0; i <= m_watch_numbers; i++)
        {
            if (!ariths[i].avg_count)
            {
                nested_ariths_offsets_to.push_back(nested_ariths_offsets_to.empty() ? 0 : nested_ariths_offsets_to.back());
                continue;
            }
            nested_ariths_offsets_to.push_back((nested_ariths_offsets_to.empty() ? 0 : nested_ariths_offsets_to.back()) + ARITHMETIC_AMOUNT_SIZE);

            avg = ariths[i].avg_count ? ariths[i].avg_sum / ariths[i].avg_count : 0;
            const_cast<QuantileTDigest<ArithmeticType>&>(ariths[i].quantileTDigest).getMany(levels, permutation, 3, quantile_res);

            ariths_data_to.emplace_back(avg);
            ariths_data_to.emplace_back(ariths[i].max);
            ariths_data_to.emplace_back(ariths[i].min);
            ariths_data_to.insert(quantile_res, quantile_res + 3);
        }
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};

}
