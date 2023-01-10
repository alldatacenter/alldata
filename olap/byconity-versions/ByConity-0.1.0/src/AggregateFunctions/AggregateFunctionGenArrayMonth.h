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

#include <AggregateFunctions/AggregateFunctionGenArray.h>

namespace DB
{

template <typename T, typename AttrType>
class AggregateFunctionAttrGenArrayMonth final : public IAggregateFunctionDataHelper<AggregateFunctionAttrGenArrayData, AggregateFunctionAttrGenArrayMonth<T, AttrType>>
{
    T m_start_time;
    UInt32 start_month;
    UInt32 m_number_steps;
    const DateLUTImpl & lut = DateLUT::instance();

public:
    AggregateFunctionAttrGenArrayMonth(UInt64 numberSteps, String startDate,
                                          const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionAttrGenArrayData, AggregateFunctionAttrGenArrayMonth<T, AttrType>>(arguments, params),
        m_number_steps(numberSteps)
    {
        m_start_time = time_t(LocalDate(startDate));
        start_month = lut.toRelativeMonthNum(m_start_time);
    }

    String getName() const override { return "genArrayMonth"; }

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

        size_t ind = lut.toRelativeMonthNum(c_time) - start_month;
        if (ind >= m_number_steps) return; // ignore those out of timeframe
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
        for(const auto & attr : attrs)
        {
            size_t size = attr.size();
            writeVarUInt(size, buf);
            for (const auto & iter : attr)
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

        for(size_t i = 0; i < size; ++i)
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

template <typename T>
class AggregateFunctionGenArrayMonth : public IAggregateFunctionDataHelper<AggregateFunctionGenArrayData, AggregateFunctionGenArrayMonth<T>>
{
    T m_start_time;
    UInt32 start_month;
    UInt32 m_number_steps;
    UInt32 m_words;
    const DateLUTImpl & lut = DateLUT::instance();

public:
    AggregateFunctionGenArrayMonth(UInt64 numberSteps, String startDate,
                                   const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionGenArrayData, AggregateFunctionGenArrayMonth>(arguments, params),
        m_number_steps(numberSteps)
    {
        m_start_time = time_t(LocalDate(startDate));
        start_month = lut.toRelativeMonthNum(m_start_time);
        m_words = (m_number_steps + sizeof(GAType) * 8 -1) / (sizeof(GAType) * 8 );
    }

    String getName() const override { return "genArrayMonth"; }

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

        size_t ind = lut.toRelativeMonthNum(c_time) - start_month;
        if (ind >= m_number_steps) return; // ignore those out of timeframe
        this->data(place).set(ind);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena __attribute__((unused))) const override
    {
        for (size_t i = 0; i < m_words; i++)
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

}
