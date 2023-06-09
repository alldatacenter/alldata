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

#include <AggregateFunctions/AggregateRetentionCommon.h>
#include <AggregateFunctions/IAggregateFunction.h>

#include <Columns/ColumnArray.h>
#include <Columns/ColumnVector.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>

#include <Functions/FunctionHelpers.h>


namespace DB
{
template <typename T, typename CompressT>
class AggregateFunctionRetentionLoss final
    : public IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionRetentionLoss<T, CompressT>>
{
    UInt64 window; // retention window size
    UInt64 array_size;
    size_t word_size;

public:
    AggregateFunctionRetentionLoss(UInt64 window_, const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionRetentionLoss<T, CompressT>>(arguments, params)
        , window(window_)
        , array_size(window * window)
    {
        word_size = (window - 1) / (sizeof(T) * 8) + 1;
    }

    String getName() const override { return "retentionLoss"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionRetentionData;
        memset(d, 0, sizeOfData());
    }

    size_t sizeOfData() const override
    {
        // reserve additional space for retentions information.
        return array_size * sizeof(RType);
    }

    DataTypePtr getReturnType() const override { return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<RType>>()); }

    template <typename MT = CompressT>
    inline typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, bool>::type
    getBit(const T * container, size_t i) const
    {
        size_t ind_word = (i >> 3) / (sizeof(T));
        T g = 1 << (i - (ind_word << 3));
        return (container[ind_word] & g);
    }

    template <typename MT = CompressT>
    typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, void>::type
    addImpl(AggregateDataPtr place, const IColumn ** columns, size_t row_num, [[maybe_unused]] Arena * arena) const
    {
        const ColumnArray & start_array_col = static_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & start_offsets = start_array_col.getOffsets();
        auto & start_container = static_cast<const ColumnVector<T> &>(start_array_col.getData()).getData();
        const size_t start_offset = row_num == 0 ? 0 : start_offsets[row_num - 1];
        const T * p_start = &start_container[0] + start_offset;

        const ColumnArray & return_array_col = static_cast<const ColumnArray &>(*columns[1]);
        const IColumn::Offsets & return_offsets = return_array_col.getOffsets();
        auto & return_container = static_cast<const ColumnVector<T> &>(return_array_col.getData()).getData();
        const size_t return_offset = row_num == 0 ? 0 : return_offsets[row_num - 1];
        const T * p_return = &return_container[0] + return_offset;

        const size_t start_word_size = (start_offsets[row_num] - start_offset);
        const size_t return_word_size = (return_offsets[row_num] - return_offset);

        auto & retentions = this->data(place).retentions;

        constexpr size_t WORD_BIT_SIZE = sizeof(T) << 3;

        size_t start_max_size = std::min(static_cast<size_t>(window), start_word_size * WORD_BIT_SIZE);
        size_t return_max_size = std::min(static_cast<size_t>(window), return_word_size * WORD_BIT_SIZE);
        size_t current = 0;

        for (size_t i = 0; i < start_max_size; ++i)
        {
            if (getBit(p_start, i))
            {
                retentions[i * window + i] += 1;
                if (current == window) {
                    continue;
                }
                if (i + 1 > current)
                {
                    current = window;
                    for (size_t j = i + 1; j < return_max_size; ++j)
                    {
                        if (getBit(p_return, j))
                        {
                            current = j;
                            break;
                        }
                    }
                }
                for (size_t j = current; j < window; ++j)
                    retentions[i * window + j] += 1;
            }
        }
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        addImpl(place, columns, row_num, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, [[maybe_unused]] Arena * arena) const override
    {
        auto & cur_data = this->data(place).retentions;
        auto & rhs_data = this->data(rhs).retentions;

        for (size_t i = 0; i < window; ++i)
        {
            for (size_t j = i; j < window; ++j)
                cur_data[i * window + j] += rhs_data[i * window + j];
        }
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).retentions;
        buf.write(reinterpret_cast<const char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, [[maybe_unused]] Arena * arena) const override
    {
        auto & value = this->data(place).retentions;
        buf.read(reinterpret_cast<char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void calculation(ConstAggregateDataPtr place) const
    {
        RType * retentions = const_cast<RType *>(this->data(place).retentions);
        for (size_t i = 0; i < window; ++i)
        {
            for (size_t j = i + 1; j < window; ++j)
                retentions[i * window + j] = retentions[i * window + i] - retentions[i * window + j];
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        calculation(place);
        const auto & value = this->data(place).retentions;

        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + array_size);

        typename ColumnVector<RType>::Container & data_to = static_cast<ColumnVector<RType> &>(arr_to.getData()).getData();

        data_to.insert(value, value + array_size);
    }

    bool allocatesMemoryInArena() const override { return false; }

};

class AggregateFunctionAttrRetentionLoss final
    : public IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionAttrRetentionLoss>
{
    UInt64 window; // retention window size
    UInt64 array_size;

public:
    AggregateFunctionAttrRetentionLoss(UInt64 window_, const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionAttrRetentionLoss>(arguments, params)
        , window(window_)
        , array_size(window * window)
    {
    }

    String getName() const override { return "retentionLoss"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionRetentionData;
        memset(d, 0, sizeOfData());
    }

    size_t sizeOfData() const override
    {
        // reserve additional space for retentions information.
        return array_size * sizeof(RType);
    }

    DataTypePtr getReturnType() const override { return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<RType>>()); }

    void addImpl(AggregateDataPtr place, const IColumn ** columns, size_t row_num, [[maybe_unused]] Arena * arena) const
    {
        //Array(Array(String))
        const auto & start_arr_col = static_cast<const ColumnArray &>(*columns[0]);
        const auto & return_arr_col = static_cast<const ColumnArray &>(*columns[1]);
        //Array(Array(String))::offsets
        const auto start_arr_offset = start_arr_col.offsetAt(row_num);
        auto start_arr_size = start_arr_col.sizeAt(row_num);
        const auto return_arr_offset = return_arr_col.offsetAt(row_num);
        auto return_arr_size = return_arr_col.sizeAt(row_num);
        //Array(String)
        const auto & start_nested_arr_col = static_cast<const ColumnArray &>(start_arr_col.getData());
        const auto & return_nested_arr_col = static_cast<const ColumnArray &>(return_arr_col.getData());
        //String
        const auto & start_nested_arr_data = start_nested_arr_col.getData();
        const auto & return_nested_arr_data = return_nested_arr_col.getData();

        start_arr_size = std::min(start_arr_size, static_cast<size_t>(window));
        return_arr_size = std::min(return_arr_size, static_cast<size_t>(window));

        auto & retentions = this->data(place).retentions;

        for (size_t i = 0; i < start_arr_size; ++i)
        {
            auto start_nested_arr_offset = start_nested_arr_col.offsetAt(start_arr_offset + i);
            auto start_nested_arr_size = start_nested_arr_col.sizeAt(start_arr_offset + i);
            if (start_nested_arr_size)
                retentions[i * window + i] += 1;

            auto find_retention = [&]() {
                for (size_t j = i + 1; j < return_arr_size; ++j)
                {
                    auto return_nested_arr_offset = return_nested_arr_col.offsetAt(return_arr_offset + j);
                    auto return_nested_arr_size = return_nested_arr_col.sizeAt(return_arr_offset + j);
                    for (size_t jj = 0; jj < return_nested_arr_size; ++jj)
                    {
                        auto return_pro = return_nested_arr_data.getDataAt(return_nested_arr_offset + jj);
                        for (size_t ii = 0; ii < start_nested_arr_size; ++ii)
                        {
                            auto start_pro = start_nested_arr_data.getDataAt(start_nested_arr_offset + ii);
                            if (start_pro == return_pro)
                            {
                                for (size_t k = j; k < window; ++k)
                                    retentions[i * window + k] += 1;

                                return;
                            }
                        }
                    }
                }
            };
            find_retention();
        }
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        addImpl(place, columns, row_num, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, [[maybe_unused]] Arena * arena) const override
    {
        auto & cur_data = this->data(place).retentions;
        const auto & rhs_data = this->data(rhs).retentions;

        for (size_t i = 0; i < window; ++i)
        {
            for (size_t j = i; j < window; ++j)
                cur_data[i * window + j] += rhs_data[i * window + j];
        }
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).retentions;
        buf.write(reinterpret_cast<const char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, [[maybe_unused]] Arena * arena) const override
    {
        auto & value = this->data(place).retentions;
        buf.read(reinterpret_cast<char *>(&value[0]), array_size * sizeof(value[0]));
    }

    void calculation(ConstAggregateDataPtr place) const
    {
        RType * retentions = const_cast<RType *>(this->data(place).retentions);
        for (size_t i = 0; i < window; ++i)
        {
            for (size_t j = i + 1; j < window; ++j)
                retentions[i * window + j] = retentions[i * window + i] - retentions[i * window + j];
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        calculation(place);
        const auto & value = this->data(place).retentions;

        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + array_size);

        typename ColumnVector<RType>::Container & data_to = static_cast<ColumnVector<RType> &>(arr_to.getData()).getData();

        data_to.insert(value, value + array_size);
    }

    bool allocatesMemoryInArena() const override { return false; }
};
}
