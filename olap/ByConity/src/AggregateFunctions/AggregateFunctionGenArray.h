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
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>

#include <Columns/ColumnVector.h>
#include <Common/ArenaAllocator.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>

#include <Columns/ColumnArray.h>

#include <AggregateFunctions/AggregateRetentionCommon.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeTuple.h>
#include <Functions/FunctionHelpers.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

using GAType = UInt8; // genArray out type
using ResultItem = std::set<String>;
using ResultType = std::vector<ResultItem>;

/* compressed version */
class AggregateFunctionAttrGenArrayData
{
public:
    ResultType attrs;

    //template<bool value>
    inline void set(size_t i, String attr)
    {
        attrs[i].insert(attr);
    }
};

/* compressed version */
class AggregateFunctionGenArrayData
{
public:
    GAType value[1];

    //template<bool value>
    inline void set(size_t i)
    {
        size_t ind_word = (i >> 3) / (sizeof(GAType));
        GAType g = 1 << (i - (ind_word << 3));

        value[ind_word] |= g;
    }

    inline bool get(size_t i)
    {
        size_t ind_word = (i >> 3) / (sizeof(GAType));
        GAType g = 1 << (i- (ind_word << 3));
        return (value[ind_word] & g);
    }
};

/* uncompressed version, sparse friendly */
class AggregateFunctionGenArrayData2
{
public:
    using Array = std::set<GAType>;
    Array value;

    inline bool empty() const
    {
        return value.empty();
    }

    //template<bool value>
    inline void set(size_t i)
    {
        value.insert(static_cast<GAType>(i));
    }

    inline bool get(size_t i) const
    {
        return value.count(static_cast<GAType>(i));
    }
};

/* uncompressed version, one byte per mark */
class AggregateFunctionGenArrayData3
{
public:
    GAType value[1];

    inline void set(size_t i)
    {
        value[i] = 1;
    }

    inline bool get(size_t i)
    {
        return value[i];
    }
};

template <typename T, typename AttrType>
class AggregateFunctionAttrGenArray final : public IAggregateFunctionDataHelper<AggregateFunctionAttrGenArrayData, AggregateFunctionAttrGenArray<T, AttrType>>
{
    T m_start_time;
    T m_ret_step;
    UInt32 m_number_steps;

public:
    AggregateFunctionAttrGenArray(UInt64 numberSteps, UInt64 startTime, UInt64 retStep,
                              const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionAttrGenArrayData, AggregateFunctionAttrGenArray<T, AttrType>>(arguments, params),
        m_start_time(startTime), m_ret_step(retStep), m_number_steps(numberSteps)
    {
        if (m_ret_step == 0)
            throw Exception("Input paramter is illegal.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

    String getName() const override { return "genArray"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionAttrGenArrayData;
        d->attrs.insert(d->attrs.begin(), m_number_steps, {});
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>()));
    }

    String getStringAttr(const IColumn ** columns, size_t row_num) const
    {
        if constexpr (std::is_same<AttrType, String>::value)
            return (dynamic_cast<const ColumnString *>(columns[1]))->getDataAt(row_num).toString();
        else
            return toString(static_cast<const ColumnVector<AttrType> *>(columns[1])->getData()[row_num]);
    }

    void add(AggregateDataPtr place, const IColumn**  columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
        auto c_time = static_cast<const ColumnVector<T> &>(*columns[0]).getData()[row_num];

        String attr = getStringAttr(columns, row_num);

        if (c_time < m_start_time)
            return;

        size_t ind = (c_time - m_start_time) / m_ret_step;
        // ignore those out of timeframe
        if (ind >= m_number_steps)
            return;

        this->data(place).set(ind, attr);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        for (size_t i = 0; i < m_number_steps; i++)
        {
            auto & attrs1 = this->data(place).attrs[i];
            const auto & attrs2 = this->data(rhs).attrs[i];
            attrs1.insert(attrs2.begin(), attrs2.end());
        }
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & attrs = this->data(place).attrs;
        writeVarUInt(attrs.size(), buf);
        for (const auto & attr : attrs)
        {
            size_t size = attr.size();
            writeVarUInt(size, buf);
            for (auto iter : attr)
                writeBinary(iter, buf);
        }
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        size_t size = 0;
        readVarUInt(size, buf);

        auto & attrs = this->data(place).attrs;
        attrs.clear();
        attrs.resize(size);

        for (size_t i = 0; i < size; ++i)
        {
            size_t size_i = 0;
            readVarUInt(size_i, buf);
            for (size_t j = 0; j < size_i; ++j)
            {
                String s;
                readBinary(s, buf);
                attrs[i].insert(s);
            }
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const ResultType & attrs = this->data(place).attrs;

        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();
        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_number_steps);

        ColumnArray& nested_arr_to = static_cast<ColumnArray &>(arr_to.getData());
        ColumnArray::Offsets & nested_offsets_to = nested_arr_to.getOffsets();
        [[maybe_unused]]auto & data_to = static_cast<ColumnString &>(nested_arr_to.getData());

        for (const auto& attr : attrs)
        {
            for (const auto& s : attr)
                data_to.insertData(s.data(), s.size());

            nested_offsets_to.push_back((nested_offsets_to.empty() ? 0 : nested_offsets_to.back()) + attr.size());
        }
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }

};

/// Generate the mask array whether even happens at specified timeframe.
//  Given parameter(m_numSteps, m_startTime, m_retStep), the output corresponding output array will be
//  allocated as Array(UInt8) and its size is (m_retStep+7)/8.
//  Given timestamp column v, the bit in (v-m_startTime) / m_retStep will be set.

template <typename T>
class AggregateFunctionGenArray final : public IAggregateFunctionDataHelper<AggregateFunctionGenArrayData, AggregateFunctionGenArray<T>>
{
    T m_start_time;
    T m_ret_step;
    UInt32 m_number_steps;
    UInt32 m_words;

public:
    AggregateFunctionGenArray(UInt64 numberSteps, UInt64 startTime, UInt64 retStep,
                              const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionGenArrayData, AggregateFunctionGenArray<T>>(arguments, params),
        m_start_time(startTime), m_ret_step(retStep), m_number_steps(numberSteps)
    {
        m_words = (m_number_steps + sizeof(GAType) * 8 -1) / (sizeof(GAType) * 8 );

        if (m_ret_step == 0)
            throw Exception("Input paramter is illegal.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

    String getName() const override { return "genArray"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionGenArrayData;
        std::fill(d->value, d->value+m_words, 0);
    }

    size_t sizeOfData() const override
    {
        return sizeof(AggregateFunctionGenArrayData) + m_words * sizeof(GAType);
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<GAType>>());
    }

    void add(AggregateDataPtr place, const IColumn**  columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
          auto c_time = static_cast<const ColumnVector<T> &>(*columns[0]).getData()[row_num];
          if (c_time < m_start_time) return;
          size_t ind = (c_time - m_start_time) / m_ret_step;
          if (ind >= m_number_steps) return; // ignore those out of timeframe
          this->data(place).set(ind);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        for (size_t i = 0; i<m_words; i++)
            this->data(place).value[i] |= this->data(rhs).value[i];
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_words* sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        auto & value = this->data(place).value;
        buf.read(reinterpret_cast<char *>(&value[0]), m_words* sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const auto & value = this->data(place).value;
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets& offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_words);
        typename ColumnVector<GAType>::Container& data_to = static_cast<ColumnVector<GAType> &>(arr_to.getData()).getData();

        data_to.insert(value, value+m_words);
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};

template <typename T>
class AggregateFunctionGenArray2 final : public IAggregateFunctionDataHelper<AggregateFunctionGenArrayData2, AggregateFunctionGenArray2<T>>
{
    T m_start_time;
    T m_ret_step;
    UInt32 m_number_steps;

public:
    AggregateFunctionGenArray2(UInt64 numberSteps, UInt64 startTime, UInt64 retStep,
                              const DataTypes & arguments, const Array & params)
        :IAggregateFunctionDataHelper<AggregateFunctionGenArrayData2, AggregateFunctionGenArray2<T>>(arguments, params),
         m_start_time(startTime), m_ret_step(retStep), m_number_steps(numberSteps)
    {
        if (m_ret_step == 0)
            throw Exception("Input paramter is illegal.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

    String getName() const override { return "genArray"; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<GAType>>());
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
          auto c_time = static_cast<const ColumnVector<T> &>(*columns[0]).getData()[row_num];
          if (c_time < m_start_time) return;
          size_t ind = (c_time - m_start_time) / m_ret_step;
          if (ind >= m_number_steps) return; // ignore those out of timeframe
          this->data(place).set(ind);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        this->data(place).value.insert(std::begin(this->data(rhs).value),
                    std::end(this->data(rhs).value));
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        size_t size = value.size();
        writeVarUInt(size, buf);
        for (const auto& elem : value)
            buf.write(reinterpret_cast<const char *>(&elem), sizeof(GAType));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        size_t size = 0;
        readVarUInt(size, buf);
        auto & value = this->data(place).value;
        GAType tmp;

        for (size_t i = 0; i<size; i++)
        {
            buf.read(reinterpret_cast<char *>(&tmp), sizeof(GAType));
            value.insert(tmp);
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const auto & value = this->data(place).value;
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + value.size());
        typename ColumnVector<GAType>::Container & data_to = static_cast<ColumnVector<GAType> &>(arr_to.getData()).getData();
        for (const auto& elem : value)
            data_to.push_back(elem);
        //data_to.insert(value.begin(), value.end());
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};

template <typename T>
class AggregateFunctionGenArray3 final : public IAggregateFunctionDataHelper<AggregateFunctionGenArrayData3, AggregateFunctionGenArray3<T>>
{
    T m_start_time;
    T m_ret_step;
    UInt32 m_number_steps;

public:
    AggregateFunctionGenArray3(UInt64 numberSteps, UInt64 startTime, UInt64 retStep,
                              const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionGenArrayData3, AggregateFunctionGenArray3<T>>(arguments, params),
          m_start_time(startTime), m_ret_step(retStep), m_number_steps(numberSteps)
    {
        if (m_ret_step == 0)
            throw Exception("Input paramter is illegal.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

    }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionGenArrayData;
        std::fill(d->value, d->value+m_number_steps, 0);
    }

    size_t sizeOfData() const override
    {
        return sizeof(AggregateFunctionGenArrayData3) + m_number_steps * sizeof(GAType);
    }

    String getName() const override { return "genArray"; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<GAType>>());
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena __attribute__((unused))) const override
    {
          auto c_time = static_cast<const ColumnVector<T> &>(*columns[0]).getData()[row_num];
          if (c_time < m_start_time) return;
          size_t ind = (c_time - m_start_time) / m_ret_step;
          if (ind >= m_number_steps) return; // ignore those out of timeframe
          this->data(place).set(ind);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        auto& value = this->data(place).value;
        auto& rhs_value = this->data(rhs).value;
        for (size_t i = 0; i<m_number_steps; i++)
           if (rhs_value[i]) value[i] = 1;
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).value;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_number_steps* sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena __attribute__((unused))) const override
    {
        auto & value = this->data(place).value;
        buf.read(reinterpret_cast<char *>(&value[0]), m_number_steps* sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const auto & value = this->data(place).value;
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_number_steps);
        typename ColumnVector<GAType>::Container & data_to = static_cast<ColumnVector<GAType> &>(arr_to.getData()).getData();
        data_to.insert(value, value+m_number_steps);
    }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};

}
