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

#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionHelpers.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>

namespace DB
{

using RType = UInt64;
struct AggregateFunctionRetention4Data
{
    RType retentions[1];
};

template <typename T>
class AggregateFunctionRetention4 final : public IAggregateFunctionDataHelper<AggregateFunctionRetention4Data, AggregateFunctionRetention4<T>>
{
    UInt64 window;
    DayNum start_date;
    DayNum end_date;
    UInt64 array_size;

public:
    AggregateFunctionRetention4(UInt64 window_, DayNum start_date_, DayNum end_date_, const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionRetention4Data, AggregateFunctionRetention4<T>>(arguments, params),
        window(window_),
        start_date(start_date_),
        end_date(end_date_ + 1),
        array_size(window * (end_date - start_date))
    {}

    String getName() const override { return "retention4"; }

    void create(AggregateDataPtr place) const override
    {
        auto * d = new (place) AggregateFunctionRetention4Data;
        std::fill(d->retentions, d->retentions + array_size, 0);
    }

    size_t sizeOfData() const override
    {
        // reserve additional space for retentions information.
        return sizeof(AggregateFunctionRetention4Data) + array_size * sizeof(RType);
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes types {
            std::make_shared<DataTypeDate>(),
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>())
        };
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena *) const override
    {
        const ColumnArray & first_column = assert_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & first_offsets = first_column.getOffsets();
        const auto & first_container = assert_cast<const ColumnVector<T> &>(first_column.getData()).getData();
        const size_t first_vec_offset = row_num == 0 ? 0 : first_offsets[row_num - 1];
        const T* cur_first_container = &first_container[0] + first_vec_offset;
        const size_t first_vec_size = (first_offsets[row_num] - first_vec_offset);

        const ColumnArray & retention_column = assert_cast<const ColumnArray &>(*columns[1]);
        const IColumn::Offsets & retention_offsets = retention_column.getOffsets();
        const auto & retention_container = assert_cast<const ColumnVector<T> &>(retention_column.getData()).getData();
        const size_t retention_vec_offset = row_num == 0 ? 0 : retention_offsets[row_num - 1];
        const T * cur_retention_container = &retention_container[0] + retention_vec_offset;
        const size_t retention_vec_size = (retention_offsets[row_num] - retention_vec_offset);

        const size_t step = sizeof(T) << 3;
        auto & values = this->data(place).retentions;
        T first_word, retention_word;
        size_t first_event = -1;
        size_t iw = 0, jw = 0, iw_offset = 0, jw_offset = 0;

        /*
         * example:
         *      if m_retWindow == 2, m_end_date - m_start_date = 4,
         *      it means that the retWindow limit is 2, the date limit is 4.
         *
         * the m_retArray is:
         *      0 0 | 0 0 | 0 0 | 0 0
         */

        /// use first_events calculate base_user
        for (; iw < first_vec_size; ++iw, iw_offset += step)
        {
            first_word = cur_first_container[iw];
            if (first_word == 0)
                continue;

            for (size_t iiw = 0; iiw < step; ++iiw)
            {
                auto pos = iw_offset + iiw;

                /// Date limit exceeded, early break
                if (pos >= static_cast<size_t>(end_date - start_date))
                {
                    first_event = pos;
                    break;
                }

                if (first_word & (1u << iiw))
                {
                    first_event = pos;
                    values[first_event * window] += 1;
                    break;
                }
            }
            if (first_event != static_cast<size_t>(-1))
                break;
        }

        if (first_event == static_cast<size_t>(-1) || first_event >= static_cast<size_t>(end_date - start_date))
            return;

        /// use retention_events calculate retention
        for (jw = iw, jw_offset = iw_offset; jw < retention_vec_size; ++jw, jw_offset += step)
        {
            retention_word = cur_retention_container[jw];
            if (!retention_word)
                continue;

            /// retWindow limit exceeded, early break
            if (jw_offset > first_event && jw_offset - first_event >= window)
                break;

            for (size_t jjw = 0; jjw < step; ++jjw)
            {
                auto pos = jw_offset + jjw;

                if (pos <= first_event)
                    continue;

                /// retWindow limit exceeded, early break
                if (pos - first_event >= window)
                    break;

                if (pos > first_event && pos < array_size && (retention_word & (1u << jjw)))
                    values[first_event * window + pos - first_event] += 1;
            }
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        auto & cur_data = this->data(place).retentions;
        auto & rhs_data = this->data(rhs).retentions;

        for (size_t i = 0; i < array_size; i++)
        {
            cur_data[i] += rhs_data[i];
        }
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).retentions;
        buf.write(reinterpret_cast<const char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *) const override
    {
        auto & value = this->data(place).retentions;
        buf.read(reinterpret_cast<char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        const auto & value = this->data(place).retentions;

        ColumnArray & arr_to = assert_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        ColumnTuple & tuple_to = assert_cast<ColumnTuple &>(arr_to.getData());
        auto & date_tp = assert_cast<ColumnUInt16 &>(tuple_to.getColumn(0)).getData();
        ColumnArray & arr_in = assert_cast<ColumnArray &>(tuple_to.getColumn(1));
        ColumnArray::Offsets  & offsets_in = arr_in.getOffsets();

        int ind = 0;
        for (auto date = start_date; date < end_date; ++date, ++ind)
        {
            date_tp.push_back(date);
            auto & date_to = assert_cast<ColumnUInt64 &>(arr_in.getData()).getData();
            date_to.insert(value + (window *ind), value+(window * ind + window));
            offsets_in.push_back((offsets_in.empty() ? 0: offsets_in.back()) + window);
        }

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + (end_date - start_date));
    }

    bool allocatesMemoryInArena() const override
    {
        return false;
    }
};

}
