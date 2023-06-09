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

#include <IO/VarInt.h>
#include <IO/WriteHelpers.h>

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/DataTypeArray.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnBitMap64.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnArray.h>
#include <AggregateFunctions/IAggregateFunction.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

struct AggregateFunctionBitMapLogicalData
{
    BitMap64 bitmap;
    bool has_value = false;

    void serialize(WriteBuffer & buf) const
    {
        size_t bytes_size = bitmap.getSizeInBytes();
        writeVarUInt(bytes_size, buf);
        PODArray<char> buffer(bytes_size);
        bitmap.write(buffer.data());
        writeString(buffer.data(), bytes_size, buf);
        writeVarUInt(has_value, buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        size_t bytes_size;
        readVarUInt(bytes_size, buf);
        PODArray<char> buffer(bytes_size);
        buf.readStrict(buffer.data(), bytes_size);
        bitmap = std::move(BitMap64::readSafe(buffer.data(), bytes_size));
        readVarUInt(has_value, buf);
    }
};

struct AggregateFunctionBitMapHasData
{
    bool has = false;
    UInt64 key;

    void serialize(WriteBuffer & buf) const
    {
        writeVarUInt(has, buf);
        writeVarUInt(key, buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        readVarUInt(has, buf);
        readVarUInt(key, buf);
    }
};

class AggregateFunctionBitMapOr final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapOr>
{
public:
    AggregateFunctionBitMapOr(const DataTypes & argument_types_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapOr>(argument_types_, {})
    {}

    String getName() const override { return "bitmapColumnOr"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeBitMap64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        this->data(place).bitmap |= bitmap;
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).bitmap |= this->data(rhs).bitmap;
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
        static_cast<ColumnBitMap64 &>(to).insert(this->data(place).bitmap);
    }

};

class AggregateFunctionBitMapXor final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapXor>
{
public:
    AggregateFunctionBitMapXor(const DataTypes & argument_types_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapXor>(argument_types_, {})
    {}

    String getName() const override { return "bitmapColumnXOr"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeBitMap64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        this->data(place).bitmap ^= bitmap;
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).bitmap ^= this->data(rhs).bitmap;
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
        static_cast<ColumnBitMap64 &>(to).insert(this->data(place).bitmap);
    }

};

class AggregateFunctionBitMapAnd final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapAnd>
{
public:
    AggregateFunctionBitMapAnd(const DataTypes & argument_types_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapAnd>(argument_types_, {})
    {}

    String getName() const override { return "bitmapColumnAnd"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeBitMap64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        if (!this->data(place).has_value)
        {
            this->data(place).bitmap = bitmap;
            this->data(place).has_value = true;
        }
        else
            this->data(place).bitmap &= bitmap;
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        if (this->data(rhs).has_value)
        {
            if (!this->data(place).has_value)
            {
                this->data(place).bitmap = this->data(rhs).bitmap;
                this->data(place).has_value = true;
            }
            else
                this->data(place).bitmap &= this->data(rhs).bitmap;
        }
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
        static_cast<ColumnBitMap64 &>(to).insert(this->data(place).bitmap);
    }
};

class AggregateFunctionBitMapCardinality final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapCardinality>
{
public:
    AggregateFunctionBitMapCardinality(const DataTypes & argument_types_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapLogicalData, AggregateFunctionBitMapCardinality>(argument_types_, {})
    {}

    String getName() const override { return "bitmapColumnCardinality"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeUInt64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        this->data(place).bitmap |= bitmap;
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).bitmap |= this->data(rhs).bitmap;
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
        static_cast<ColumnUInt64 &>(to).getData().push_back(this->data(place).bitmap.cardinality());
    }

};


class AggregateFunctionBitMapHas final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapHasData, AggregateFunctionBitMapHas>
{
public:
    AggregateFunctionBitMapHas(const DataTypes & argument_types_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapHasData, AggregateFunctionBitMapHas>(argument_types_, {})
    {}

    String getName() const override { return "bitmapColumnHas"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeUInt8>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);

        if (!columns[1])
            throw Exception("Second argument of " + getName() + " is missing", ErrorCodes::LOGICAL_ERROR);

        this->data(place).key = columns[1]->getUInt(row_num);

        bool & has = this->data(place).has;
        if (has)
            return;

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        this->data(place).has = bitmap.contains(this->data(place).key);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        bool & has = this->data(place).has;
        if (has)
            return;

        this->data(place).has |= this->data(rhs).has;
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
        static_cast<ColumnUInt8 &>(to).insert(this->data(place).has);
    }

};
}
