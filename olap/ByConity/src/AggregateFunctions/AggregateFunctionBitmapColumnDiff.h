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
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <Columns/ColumnBitMap64.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <Common/ArenaAllocator.h>

#include <AggregateFunctions/IAggregateFunction.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int BAD_ARGUMENTS;
    extern const int TOO_MANY_ARGUMENTS_FOR_FUNCTION;
    extern const int TOO_FEW_ARGUMENTS_FOR_FUNCTION;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}

using BitMapPtr = std::unique_ptr<BitMap64>;

template <typename T>
struct AggregateFunctionBitMapColumnDiffData
{
    using data_type = std::unordered_map<T, BitMapPtr>;
    using key_type = T;
    using value_type = typename data_type::value_type;

    data_type data;

    void add(const T key, const BitMap64 & bitmap)
    {
        auto [it, inserted] = data.try_emplace(key, std::make_unique<BitMap64>(std::move(const_cast<BitMap64 &>(bitmap))));
        if (!inserted) {
            *(it->second) |= bitmap;
        }
    }

    void merge(AggregateFunctionBitMapColumnDiffData & rhs)
    {
        data_type & rhs_map = rhs.data;
        for (auto rt = rhs_map.begin(); rt != rhs_map.end(); ++rt)
        {
            auto [it, inserted] = data.try_emplace(rt->first, std::move(const_cast<BitMapPtr &>(rt->second)));
            if (!inserted) {
                *(it->second) |= *(rt->second);
            }
        }
    }

    bool empty() { return data.empty(); }

    void serialize(WriteBuffer & buf) const
    {
        size_t key_size = data.size();
        writeVarUInt(key_size, buf);

        for (auto it = data.begin(); it != data.end(); ++it)
        {
            if constexpr (std::is_same_v<T, String>)
                writeStringBinary(it->first, buf);
            else
                writeVarUInt(it->first, buf);
            size_t bytes_size = it->second->getSizeInBytes();
            writeVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            it->second->write(buffer.data());
            writeString(buffer.data(), bytes_size, buf);
        }
    }

    void deserialize(ReadBuffer & buf)
    {
        size_t key_size;
        readVarUInt(key_size, buf);
        for (size_t i = 0; i < key_size; ++i)
        {
            T key;
            if constexpr (std::is_same_v<T, String>)
                readStringBinary(key, buf);
            else
            {
                UInt64 key_data;
                readVarUInt(key_data, buf);
                key = static_cast<T>(key_data);
            }

            size_t bytes_size;
            readVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            buf.readStrict(buffer.data(), bytes_size);
            BitMap64 bitmap = BitMap64::readSafe(buffer.data(), bytes_size);
            data.emplace(key, std::make_unique<BitMap64>(std::move(bitmap)));
        }
    }
};


enum DiffDirection
{
    FORWARD,
    BACKWARD,
    BIDIRECTION,
    INVALID
};

struct DiffDirectionOp
{
    DiffDirectionOp() : diff_direc(DiffDirection::FORWARD) {}
    DiffDirectionOp(String diff_dir_op)
    {
        std::transform(diff_dir_op.begin(), diff_dir_op.end(), diff_dir_op.begin(), ::tolower);
        if (diff_dir_op.empty() || diff_dir_op == "forward")
            diff_direc = DiffDirection::FORWARD;
        else if (diff_dir_op == "backward")
            diff_direc = DiffDirection::BACKWARD;
        else if (diff_dir_op == "bidirection" || diff_dir_op == "both")
            diff_direc = DiffDirection::BIDIRECTION;
        else
            diff_direc = DiffDirection::INVALID;
    }

    DiffDirection diff_direc;
};

template <typename T>
class AggregateFunctionBitMapColumnDiff final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapColumnDiffData<T>, AggregateFunctionBitMapColumnDiff<T>>
{
using DiffPair = typename std::pair<T, BitMapPtr>;

public:
    AggregateFunctionBitMapColumnDiff(const DataTypes & argument_types_, const UInt64 result_type_, const String & diff_direction_str_,
        const UInt64 diff_step_, bool is_date_ = false)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapColumnDiffData<T>, AggregateFunctionBitMapColumnDiff<T>>(argument_types_, {})
    , result_type(result_type_), diff_direction(diff_direction_str_), diff_step(diff_step_), is_date(is_date_)
    {}

    String getName() const override { return "bitmapColumnDiff"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        DataTypes types;

        if constexpr (std::is_same_v<T, String>)
            types.emplace_back(std::make_shared<DataTypeString>());
        else
            types.emplace_back(std::make_shared<DataTypeNumber<T>>());

        /// When input type is Data, the template T is set to UInt16 (for column of Date),
        /// so we have to distinguish this in runtime
        if (is_date)
        {
            types.clear();
            types.emplace_back(std::make_shared<DataTypeDate>());
        }

        if (result_type == 0ull) // sum count
            types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>()));
        else if (result_type == 1ull) // detail in bitmap
            types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeBitMap64>()));

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[1]);
        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);
        if constexpr (std::is_same_v<T, String>)
        {
            const auto & column_key = dynamic_cast<const ColumnString &>(*columns[0]);
            this->data(place).add(column_key.getDataAt(row_num).toString(), bitmap);
        }
        else
        {
            const auto & column_key = dynamic_cast<const ColumnVector<T> &>(*columns[0]);
            this->data(place).add(column_key.getElement(row_num), bitmap);
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).merge(this->data(const_cast<AggregateDataPtr>(rhs)));
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & input_data = const_cast<typename AggregateFunctionBitMapColumnDiffData<T>::data_type &>(this->data(place).data);
        if (input_data.empty())
            return;

        if (diff_step >= input_data.size())
            throw Exception(getName() + ": the step " + std::to_string(diff_step) + " is larger than data size", ErrorCodes::LOGICAL_ERROR);

        std::vector<DiffPair> all_data;
        std::unordered_map<T, std::vector<BitMapPtr>> intermediate_res;

        /// prepare data
        for (auto it = input_data.begin(); it != input_data.end(); ++it)
            all_data.emplace_back(std::move(it->first), std::move(it->second));

        if (diff_direction.diff_direc == DiffDirection::BACKWARD)
            std::sort(all_data.begin(), all_data.end(), std::greater<DiffPair>());
        else
            std::sort(all_data.begin(), all_data.end(), std::less<DiffPair>());

        /// computation
        /// In FORWARD/BIDIRECTION: data is sorted in ascending order, then compare one by one, only part FORWARD of BIDIRECTION
        /// In BACKWARD: data is sorted in descending order, then compare one by one
        {
            size_t i = 0;
            for (; i + diff_step < all_data.size(); ++i)
                intermediate_res[all_data[i].first].emplace_back(std::make_unique<BitMap64>(*(all_data[i].second) - *(all_data[i+diff_step].second)));

            for (; i < all_data.size(); ++i)
                intermediate_res[all_data[i].first].emplace_back(std::make_unique<BitMap64>(BitMap64()));
        }

        /// In BIDIRECTION: data is sorted in ascending order, and compare in reverse order for the part BACKWARD
        if (diff_direction.diff_direc == DiffDirection::BIDIRECTION)
        {
            size_t i = all_data.size() - 1;
            for (; i >= diff_step; --i)
                intermediate_res[all_data[i].first].emplace_back(std::make_unique<BitMap64>(*(all_data[i].second) - *(all_data[i-diff_step].second)));

            for (size_t cnt = 0; cnt < diff_step; --i, ++cnt)
                intermediate_res[all_data[i].first].emplace_back(std::make_unique<BitMap64>(BitMap64()));
        }

        /// fill result data
        auto & column_res = dynamic_cast<ColumnArray &>(to);
        auto & tuple_in_array = dynamic_cast<ColumnTuple &>(column_res.getData());
        auto & res_offsets = column_res.getOffsets();
        auto prev_res_offset = res_offsets.empty() ? 0 : res_offsets.back();

        if constexpr (std::is_same_v<T, String>)
        {
            auto & first_col_in_tuple = dynamic_cast<ColumnString &>(tuple_in_array.getColumn(0));
            for (auto it = intermediate_res.begin(); it != intermediate_res.end(); ++it)
                first_col_in_tuple.insertData(it->first.data(), it->first.size());
        }
        else
        {
            auto & first_col_in_tuple = dynamic_cast<ColumnVector<T> &>(tuple_in_array.getColumn(0));
            for (auto it = intermediate_res.begin(); it != intermediate_res.end(); ++it)
                first_col_in_tuple.insertValue(it->first);
        }

        auto & second_col_in_tuple = dynamic_cast<ColumnArray &>(tuple_in_array.getColumn(1));
        auto & offsets = second_col_in_tuple.getOffsets();
        auto prev_offset = offsets.empty() ? 0 : offsets.back();

        if (result_type == 0ull)
        {
            auto & column_in_tuple = dynamic_cast<ColumnUInt64 &>(second_col_in_tuple.getData());

            for (auto it = intermediate_res.begin(); it != intermediate_res.end(); ++it)
            {
                for (const auto & ptr : it->second)
                    column_in_tuple.insertValue(ptr->cardinality());

                prev_offset += it->second.size();
                offsets.push_back(prev_offset);
            }
        }
        else if (result_type == 1ull)
        {
            auto & column_in_tuple = dynamic_cast<ColumnBitMap64 &>(second_col_in_tuple.getData());

            for (auto it = intermediate_res.begin(); it != intermediate_res.end(); ++it)
            {
                for (const auto & ptr : it->second)
                {
                    column_in_tuple.insert(*ptr);
                }

                prev_offset += it->second.size();
                offsets.push_back(prev_offset);
            }
        }

        prev_res_offset += intermediate_res.size();
        res_offsets.push_back(prev_res_offset);
    }

private:
    /// sum count (0) or detail bitmap (1)
    UInt64 result_type;
    /***
    * the direction agg compute the difference of bitmaps
    * forward:     in ascending order of diff keys. eg. 2021-07-01 - 2021-07-02 for day '2021-07-01'
    * backward:    in descending order of diff keys. eg. 2021-07-01 - 2021-06-30 for day '2021-07-01'
    * bidirection: in both ascending and descending order. That's to say, both (2021-07-01 - 2021-07-02)
    *              and (2021-07-01 - 2021-06-30) will be computed for day '2021-07-01'
    * **/
    DiffDirectionOp diff_direction;
    /***
     * the step to decide the next key of operator Andnot.
     * eg. step = 2 means 2021-07-01 - 2021-07-03 for day '2021-07-01'
     * the value by user is an absolute UInt64 number
     ***/
    UInt64 diff_step;

    /***
     * wheather the true key type is Date, and internal CH stores it
     * in ColumnUInt16 and internal type is DayNum
     ***/
    bool is_date{false};
};
}
