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
#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypesDecimal.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeTuple.h>
#include <Common/Arena.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnsNumber.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>

#include <common/logger_useful.h>
#include <Poco/Logger.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int ARGUMENT_OUT_OF_BOUND;
    extern const int BAD_ARGUMENTS;
    extern const int UNKNOWN_TYPE;
    extern const int TYPE_MISMATCH;
    extern const int LOGICAL_ERROR;
    extern const int SIZES_OF_ARRAYS_DOESNT_MATCH;
}

/**
 *
 * Divide data by groups, and then separately aggregates the data in those groups.
 * In the end, Merge data one by one.
 *
 * Parameters
 *      start — Starting value of the whole required interval for the values of resampling_key.
 *      stop — Ending value of the whole required interval for the values of resampling_key. The whole interval doesn't include the stop value [start, stop).
 *      step — Step for separating the whole interval by subintervals. The aggFunction is executed over each of those subintervals independently.
 *      total - element size
 *      aod - align of data
 *      sod - size of data
 */

template <typename Key>
class AggregateFunctionStack final : public IAggregateFunctionHelper<AggregateFunctionStack<Key>>
{
private:
    constexpr static size_t max_elements = 4096;

    AggregateFunctionPtr nested_function;

    size_t last_col;

    Key begin;
    Key end;
    size_t step;

    size_t total;
    size_t aod;
    size_t sod;

public:

    AggregateFunctionStack(
        AggregateFunctionPtr nested_function_,
        Key begin_,
        Key end_,
        size_t step_,
        const DataTypes & arguments,
        const Array & params
    ) :
        IAggregateFunctionHelper<AggregateFunctionStack<Key>> {arguments, params},
        nested_function {nested_function_},
        last_col {arguments.size() - 1},
        begin {begin_},
        end {end_},
        step {step_},
        total {0},
        aod {nested_function->alignOfData()},
        sod {(nested_function->sizeOfData() + aod - 1) / aod * aod}
    {
        // notice: argument types has been checked before
        if (step == 0)
            throw Exception("The step given in function "+ getName() + " should not be zero", ErrorCodes::ARGUMENT_OUT_OF_BOUND);

        if (end < begin)
            total = 0;
        else
            total = (end - begin + step - 1) / step;

        if (total > max_elements)
            throw Exception("The range given in function " + getName() + " contains too many elements", ErrorCodes::ARGUMENT_OUT_OF_BOUND);
    }

    String getName() const override
    {
        return nested_function->getName() + "Stack";
    }

    bool isState() const override
    {
        return nested_function->isState();
    }

    bool allocatesMemoryInArena() const override
    {
        return nested_function->allocatesMemoryInArena();
    }

    bool hasTrivialDestructor() const override
    {
        return nested_function->hasTrivialDestructor();
    }

    size_t sizeOfData() const override
    {
        return total * sod;
    }

    size_t alignOfData() const override
    {
        return aod;
    }

    void create(AggregateDataPtr place) const override
    {
        for (size_t i = 0; i < total; ++i)
            nested_function->create(place + i * sod);
    }

    void destroy(AggregateDataPtr place) const noexcept override
    {
        for (size_t i = 0; i < total; ++i)
            nested_function->destroy(place + i * sod);
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        Key key;

        if constexpr (static_cast<Key>(-1) < 0)
            key = columns[last_col]->getInt(row_num);
        else
            key = columns[last_col]->getUInt(row_num);

        if (key < begin || key >= end)
            return;

        size_t pos = (key - begin) / step;

        nested_function->add(place + pos * sod, columns, row_num, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        for (size_t i = 0; i < total; ++i)
            nested_function->merge(place + i * sod, rhs + i * sod, arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        for (size_t i = 0; i < total; ++i)
            nested_function->serialize(place + i * sod, buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena * arena) const override
    {
        for (size_t i = 0; i < total; ++i)
            nested_function->deserialize(place + i * sod, buf, arena);
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes type;
        type.emplace_back(std::make_shared<DataTypeNumber<Key>>());
        type.emplace_back(nested_function->getReturnType());
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(type));
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena * arena) const override
    {
        auto & col = assert_cast<ColumnArray &>(to);
        auto & col_offsets = assert_cast<ColumnArray::ColumnOffsets &>(col.getOffsetsColumn());

        ColumnTuple & array_to_nest = assert_cast<ColumnTuple &>(col.getData());
        auto & timestamp = assert_cast<ColumnVector<Key> &>(array_to_nest.getColumn(0)).getData();

        for (size_t i = 0; i < total; ++i)
        {
            if(i)
                nested_function->merge(const_cast<AggregateDataPtr>(place), place + i * sod, nullptr);

            nested_function->insertResultInto(const_cast<AggregateDataPtr>(place), array_to_nest.getColumn(1), arena);

            timestamp.push_back(begin + i * step);
        }

        col_offsets.getData().push_back(col.getData().size());
    }
};

template <typename KeyType, typename ValueType>
struct MergeSteamStackData
{
    std::vector<KeyType> key;
    std::vector<ValueType> value;

    void add(const PaddedPODArray<KeyType> & other_key, const PaddedPODArray<ValueType> & other_value, size_t start, size_t end)
    {
        if (!key.size())
        {
            key.insert(key.begin(), other_key.begin() + start, other_key.begin() + end);
            value.insert(value.begin(), other_value.begin() + start, other_value.begin() + end);
        }
        else
        {
            if (key.size() != end - start)
                throw Exception("Function MergeSteamStack key size mismatch", ErrorCodes::SIZES_OF_ARRAYS_DOESNT_MATCH);

            for (size_t i = start, j = 0; i < end; ++i, ++j)
            {
                if(key[j] != static_cast<KeyType>(other_key[i]))
                    throw Exception("Function MergeSteamStack need same key value when merge stream", ErrorCodes::BAD_ARGUMENTS);
                value[j] += static_cast<ValueType>(other_value[i]);
            }
        }
    }

    void merge(const MergeSteamStackData<KeyType, ValueType> & other)
    {
        if (other.key.empty())
            return;

        if (key.empty())
        {
            key = other.key;
            value = other.value;
            return;
        }

        if (key.size() != other.key.size())
            throw Exception("Function MergeSteamStack key size mismatch", ErrorCodes::SIZES_OF_ARRAYS_DOESNT_MATCH);

        for (size_t i = 0; i < value.size(); ++i)
        {
            if(key[i] != other.key[i])
                throw Exception("Function MergeSteamStack need same key value when merge stream.", ErrorCodes::BAD_ARGUMENTS);
            this->value[i] += other.value[i];
        }
    }

    void serialize(WriteBuffer & buf) const
    {
        size_t size = key.size();
        writeBinary(key.size(), buf);
        buf.write(reinterpret_cast<const char *>(&key[0]), size * sizeof(key[0]));
        buf.write(reinterpret_cast<const char *>(&value[0]), size * sizeof(value[0]));
    }

    void deserialize(ReadBuffer & buf)
    {
        size_t size;
        readBinary(size, buf);
        key.resize(size);
        value.resize(size);
        buf.read(reinterpret_cast<char *>(&key[0]), size * sizeof(key[0]));
        buf.read(reinterpret_cast<char *>(&value[0]), size * sizeof(value[0]));
    }
};


template <typename KeyType, typename ValueType>
class AggregateFunctionMergeStreamStack final : public IAggregateFunctionDataHelper<MergeSteamStackData<KeyType, ValueType>, AggregateFunctionMergeStreamStack<KeyType, ValueType>>
{
public:
    using KeyDataType = DataTypeNumber<KeyType>;
    using ValueDataType = DataTypeNumber<ValueType>;

    String getName() const override { return "MergeStreamStack"; }

    explicit AggregateFunctionMergeStreamStack(const DataTypes & argument_types_)
        : IAggregateFunctionDataHelper<MergeSteamStackData<KeyType, ValueType>,
        AggregateFunctionMergeStreamStack<KeyType, ValueType>>(argument_types_, {}) {}


    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column = assert_cast<const ColumnArray &>(*columns[0]);
        const auto & col_offsets = assert_cast<const ColumnArray::ColumnOffsets &>(column.getOffsetsColumn());

        size_t offset = col_offsets.getUInt(row_num);
        size_t prev_offset = row_num ? col_offsets.getUInt(row_num - 1) : 0;

        const ColumnTuple &array_to_nest = assert_cast<const ColumnTuple &>(column.getData());
        const auto & key_column = assert_cast<const ColumnVector<KeyType> &>(array_to_nest.getColumn(0));
        const auto & value_column = assert_cast<const ColumnVector<ValueType> &>(array_to_nest.getColumn(1));

        this->data(place).add(key_column.getData(), value_column.getData(), prev_offset, offset);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).merge(this->data(rhs));
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes type;
        type.emplace_back(std::make_shared<KeyDataType>());
        type.emplace_back(std::make_shared<ValueDataType>());
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(type));
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        auto & col = assert_cast<ColumnArray &>(to);
        auto & col_offsets = assert_cast<ColumnArray::ColumnOffsets &>(col.getOffsetsColumn());

        ColumnTuple & array_to_nest = assert_cast<ColumnTuple &>(col.getData());
        auto & key_column = assert_cast<ColumnVector<KeyType> &>(array_to_nest.getColumn(0)).getData();
        auto & value_column = assert_cast<ColumnVector<ValueType> &>(array_to_nest.getColumn(1)).getData();

        for (size_t i = 0, size = this->data(place).key.size(); i < size; ++i)
        {
            key_column.push_back(this->data(place).key[i]);
            value_column.push_back(this->data(place).value[i]);
        }

        col_offsets.getData().push_back(col.getData().size());
    }

    bool allocatesMemoryInArena() const override
    {
        return false;
    }
};

}
