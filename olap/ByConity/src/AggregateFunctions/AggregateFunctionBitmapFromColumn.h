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

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <Columns/ColumnBitMap64.h>
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

struct AggregateFunctionBitMapFromColumnData
{
    // Switch to ordinary Allocator after 4096 bytes to avoid fragmentation and trash in Arena
    using Allocator = MixedAlignedArenaAllocator<alignof(UInt64), 4096>;
    using Array = PODArray<UInt64, 4098, Allocator>;

    Array value;
    BitMap64 bitmap;
};

class AggregateFunctionBitMapFromColumn final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapFromColumnData, AggregateFunctionBitMapFromColumn>
{
public:
    explicit AggregateFunctionBitMapFromColumn(const DataTypes & argument_types_)
        : IAggregateFunctionDataHelper<AggregateFunctionBitMapFromColumnData, AggregateFunctionBitMapFromColumn>(argument_types_, {})
    {}

    String getName() const override { return "bitmapFromColumn"; }
    bool allocatesMemoryInArena() const override { return true; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeBitMap64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        auto & arr = this->data(place).value;
        auto & map = this->data(place).bitmap;
        arr.push_back(columns[0]->getUInt(row_num), arena);

        if (arr.size() == 4097)
        {
            map.addMany(4097, arr.data());
            arr.clear();
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * ) const override
    {
        auto & cur_value = this->data(place).value;
        auto & cur_bitmap = this->data(place).bitmap;
        auto & rhs_value = this->data(rhs).value;
        auto & rhs_bitmap = const_cast<BitMap64 &>(this->data(rhs).bitmap);

        if (!cur_value.empty())
        {
            cur_bitmap.addMany(cur_value.size(), cur_value.data());
        }
        if (!rhs_value.empty())
        {
            rhs_bitmap.addMany(rhs_value.size(), rhs_value.data());
        }
        cur_bitmap |= rhs_bitmap;
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        size_t size = value.size();
        writeVarUInt(size, buf);
        buf.write(reinterpret_cast<const char *>(value.data()), size * sizeof(value[0]));

        const auto & bitmap = this->data(place).bitmap;
        size_t bytes = bitmap.getSizeInBytes();
        writeVarUInt(bytes, buf);

        if (bytes != 0)
        {
            std::vector<char> bitmap_chars;
            bitmap_chars.reserve(bytes);
            bitmap.write(bitmap_chars.data());
            buf.write(bitmap_chars.data(), bytes);
        }
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena * arena) const override
    {
        size_t size = 0;
        readVarUInt(size, buf);
        auto & value = this->data(place).value;

        value.resize(size, arena);
        buf.read(reinterpret_cast<char *>(value.data()), size * sizeof(value[0]));

        size_t bytes = 0;
        readVarUInt(bytes, buf);

        if (bytes != 0)
        {
            std::vector<char> bitmap_chars;
            bitmap_chars.reserve(bytes);
            buf.read(bitmap_chars.data(), bytes);

            auto & bitmap = this->data(place).bitmap;
            bitmap.read(bitmap_chars.data(), bytes);
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & bitmap = const_cast<BitMap64 &>(this->data(place).bitmap);
        auto & arr = this->data(place).value;
        if (!arr.empty())
            bitmap.addMany(arr.size(), arr.data());

        dynamic_cast<ColumnBitMap64 &>(to).insert(bitmap);
    }

};

}
