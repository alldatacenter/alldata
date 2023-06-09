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

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>

#include <Columns/ColumnArray.h>
#include <Columns/ColumnVector.h>
#include <DataTypes/DataTypesNumber.h>

#include <AggregateFunctions/IAggregateFunction.h>

#include <vector>
#include <tuple>
#include <map>
#include <math.h>

namespace DB {


struct AggregateFunctionFrequencyData
{
    UInt64 freq_value[10] {0};
};

template <typename T>
class AggregateFunctionFrequency final : public IAggregateFunctionDataHelper<AggregateFunctionFrequencyData, AggregateFunctionFrequency<T>> {
public:
    AggregateFunctionFrequency(const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionFrequencyData, AggregateFunctionFrequency<T>>(arguments, params){}

    String getName() const override { return "frequency"; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        auto v = static_cast<const ColumnVector<T> &>(*columns[0]).getData()[row_num];
        auto & freq_value = this->data(place).freq_value;

        // TODO: Realize with mapping
        if (v == 1)
            freq_value[0]++;
        else if (v == 2)
            freq_value[1]++;
        else if (v == 3)
            freq_value[2]++;
        else if (v == 4)
            freq_value[3]++;
        else if (v == 5)
            freq_value[4]++;
        else if (v >= 6 && v<= 10)
            freq_value[5]++;
        else if (v >= 11 && v <= 20)
            freq_value[6]++;
        else if (v >= 21 && v <= 50)
            freq_value[7]++;
        else if (v >= 50 && v <= 100)
            freq_value[8]++;
        else if (v > 100)
            freq_value[9]++;
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        for (int i = 0 ; i < 10 ; i++)
            this->data(place).freq_value[i] += this->data(rhs).freq_value[i];
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).freq_value;
        buf.write(reinterpret_cast<const char *>(&value[0]), 10 * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *) const override
    {
        auto & value = this->data(place).freq_value;
        buf.read(reinterpret_cast<char *>(&value[0]), 10 * sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        const auto & value = this->data(place).freq_value;
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + 10);

        typename ColumnVector<UInt64>::Container & data_to = static_cast<ColumnVector<UInt64> &>(arr_to.getData()).getData();
        data_to.insert(value, value + 10);
    }

    bool allocatesMemoryInArena() const override { return false; }

};

template <typename T>
struct AggregateFunctionDistributionData
{
    //the key is interval left side
    std::map<T, UInt64> freq_data;

    void add(const T key)
    {
        if (freq_data.find(key) == freq_data.end())
            freq_data[key] = 0;

        freq_data[key]++;
    }

    void merge(const std::map<T, UInt64> & rhs_map)
    {
        for (auto & rt : rhs_map)
        {
            if (freq_data.find(rt.first) == freq_data.end())
                freq_data.emplace(rt);
            else
                freq_data[rt.first] += rt.second;
        }
    }
};

// interval type, input type
template <typename ITV, typename IPT>
class AggregateFunctionDistribution final : public IAggregateFunctionDataHelper<AggregateFunctionDistributionData<ITV>, AggregateFunctionDistribution<ITV, IPT>>
{

public:
    AggregateFunctionDistribution(const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionDistributionData<ITV>, AggregateFunctionDistribution<ITV, IPT>>(arguments, params)
    {
        auto type = params[0].getType();
        if (type == Field::Types::Float64)
            start_val = static_cast<ITV>(params[0].safeGet<Float64>());
        else if (type == Field::Types::Int64)
            start_val = static_cast<ITV>(params[0].safeGet<Int64>());
        else if (type == Field::Types::UInt64)
            start_val = static_cast<ITV>(params[0].safeGet<UInt64>());

        type = params[1].getType();
        if (type == Field::Types::Float64)
            step = static_cast<ITV>(params[1].safeGet<Float64>());
        else if (type == Field::Types::UInt64)
            step = static_cast<ITV>(params[1].safeGet<UInt64>());

        if (step < ITV(0))
            throw Exception("Step must be a non-negative number.", ErrorCodes::LOGICAL_ERROR);

        group_num = params[2].safeGet<UInt64>();
        max_right_side = static_cast<ITV>(start_val + step*group_num);
    }

    String getName() const override { return "distribution"; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        ITV v = static_cast<ITV>(static_cast<const ColumnVector<IPT> &>(*columns[0]).getData()[row_num]);
        //ignore out-of-range values or nan values
        if (std::isnan(v)) return;
        // The effective precision of the Float32 type is only 7 bits at most,
        // The epsilon, 1e-6, for float comparison, is commonly enough.
        if (std::is_same<typename std::decay<IPT>::type, Float64>::value || std::is_same<typename std::decay<IPT>::type, Float32>::value)
        {
            if ((start_val - v) > 1e-6 || (v - max_right_side > 1e-6))
                return;
        }
        else if (v < start_val || v > max_right_side)
        {
            return;
        }

        this->data(place).add(getInterval(v));
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).merge(this->data(rhs).freq_data);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & freq_map = this->data(place).freq_data;
        size_t size = freq_map.size();
        writeVarUInt(size, buf);
        UInt8 type = std::is_same<typename std::decay<ITV>::type, Int64>::value ? 1
                    : std::is_same<typename std::decay<ITV>::type, UInt64>::value ? 2
                    : std::is_same<typename std::decay<ITV>::type, Float32>::value ? 4
                    : std::is_same<typename std::decay<ITV>::type, Float64>::value ? 8 : 0;
        writeVarUInt(type, buf);

        for (auto & mt: freq_map)
        {
            writeVarUInt(mt.second, buf);

            if (type == 1)
                writeVarInt(mt.first, buf);
            else if (type == 2)
                writeVarUInt(mt.first, buf);
            else if (type == 4 || type == 8)
                writeFloatBinary(mt.first, buf);
            else
                throw Exception("Illegal type of aggregate function distribution", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *) const override
    {
        auto & freq_map = this->data(place).freq_data;
        size_t size = 0;
        UInt8 type = 0;
        readVarUInt(size, buf);
        readVarUInt(type, buf);

        for (size_t i = 0; i < size; ++i)
        {
            UInt64 count = 0;
            readVarUInt(count, buf);

            if (type == 1)
            {
                Int64 left = 0;
                readVarInt(left, buf);
                freq_map.emplace(left, count);
            }
            else if (type == 2)
            {
                UInt64 left = 0;
                readVarUInt(left, buf);
                freq_map.emplace(left, count);
            }
            else if (type == 4)
            {
                Float32 left = 0.0;
                readFloatBinary(left, buf);
                freq_map.emplace(left, count);
            }
            else if (type == 8)
            {
                Float64 left = 0.0;
                readFloatBinary(left, buf);
                freq_map.emplace(left, count);
            }
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        const auto & freq_map = (this->data(place).freq_data);
        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        if (freq_map.size() > group_num)
            throw Exception("The interval number is more than expected.", ErrorCodes::LOGICAL_ERROR);
        if (freq_map.size() < group_num)
            padAllIntervals(const_cast<std::map<ITV, UInt64> &>(freq_map));

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + freq_map.size());

        ColumnVector<UInt64> & data_to = static_cast<ColumnVector<UInt64> &>(arr_to.getData());
        for (auto & mt : freq_map)
            data_to.insertValue(mt.second);
    }

    bool allocatesMemoryInArena() const override { return false; }

private:
    ITV start_val;
    ITV step;
    UInt64 group_num = 0;
    ITV max_right_side;

    //fill those empty intervals with 0
    void padAllIntervals(std::map<ITV, UInt64> & data) const
    {
        for (size_t i = 0; i < group_num; ++i)
        {
            ITV left_side = static_cast<ITV>(start_val + i*step);
            if (data.find(left_side) == data.end())
                data[left_side] = 0;
        }
    }

    //find the interval left side val locates in
    ITV getInterval(ITV val) const
    {
        if (std::is_same<typename std::decay<ITV>::type, Float64>::value || std::is_same<typename std::decay<ITV>::type, Float32>::value)
        {
            for (size_t i = 0; i < group_num; ++i)
            {
                ITV right_side = static_cast<ITV>(start_val + (i+1) * step);
                if (right_side - val > 1e-6)
                    return start_val + i*step;
            }
        }
        else
        {
            for (size_t i = 0; i < group_num; ++i)
            {
                ITV right_side = static_cast<ITV>(start_val + (i+1) * step);
                if (right_side > val)
                    return start_val + i*step;
            }
        }

        return static_cast<ITV>(start_val + (group_num-1) * step);
    }
};

}
