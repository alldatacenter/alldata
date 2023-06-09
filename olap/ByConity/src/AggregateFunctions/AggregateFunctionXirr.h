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

#include <Interpreters/AggregationCommon.h>

#include <Common/MemoryTracker.h>
#include <DataTypes/DataTypesNumber.h>
#include <AggregateFunctions/IAggregateFunction.h>

#define MAX_LOOPS			50
#define MAX_EPSILON			1e-10
#define XIRR_DAYS_PER_YEAR	365.0

namespace DB
{

template <typename AmountType, typename TST>
struct XirrItemTemp
{
    AmountType amount;
    TST time;

    bool operator < (const XirrItemTemp & item) const
    {
        return time < item.time || (time == item.time && amount < item.amount);
    }
};

template <typename AmountType, typename TST>
struct AggregateFunctionXirrData
{
    using XirrItem = XirrItemTemp<AmountType, TST>;
    using XirrItems = std::vector<XirrItem>;

    XirrItems xirr_items;

    void add(AmountType amount, TST time, Arena *)
    {
        XirrItem xirr_item{amount, time};
        xirr_items.push_back(xirr_item);
    }

    void merge(const AggregateFunctionXirrData<AmountType, TST> & other, Arena *)
    {
        xirr_items.insert(xirr_items.end(), other.xirr_items.begin(), other.xirr_items.end());
    }

    void serialize(WriteBuffer & buf) const
    {
        writeBinary(xirr_items.size(), buf);
        for (const auto & xirr_item : xirr_items)
        {
            writeBinary(xirr_item.amount, buf);
            writeBinary(xirr_item.time, buf);
        }
    }

    void deserialize(ReadBuffer & buf, Arena *)
    {
        size_t size;
        readBinary(size, buf);

        xirr_items.clear();
        xirr_items.reserve(size);

        XirrItem  xirr_item;
        for (size_t i = 0; i < size; ++i)
        {
            readBinary(xirr_item.amount, buf);
            readBinary(xirr_item.time, buf);
            xirr_items.template emplace_back(xirr_item);
        }
    }
};

template <typename AmountType, typename TST>
class AggregateFunctionXirr : public IAggregateFunctionDataHelper<AggregateFunctionXirrData<AmountType, TST>, AggregateFunctionXirr<AmountType, TST>>
{
    using XirrItem = XirrItemTemp<AmountType, TST>;
    using XirrItems = std::vector<XirrItem>;

private:
    bool has_guess = false;
    Float64 guess_value = 0.0;

public:
    AggregateFunctionXirr(const DataTypes & arguments, const Array & params, const std::optional<Float64> guess)
        : IAggregateFunctionDataHelper<AggregateFunctionXirrData<AmountType, TST>, AggregateFunctionXirr<AmountType, TST>>(arguments, params)
    {
        if (guess != std::nullopt)
        {
            has_guess = true;
            guess_value = guess.value();
        }
    }

    String getName() const override
    {
        return "xirr";
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeFloat64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        if (columns[0] == nullptr || columns[1] == nullptr)
            throw Exception("Parameter is null", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        AmountType amount = typeid_cast<const ColumnVector<AmountType> &>(*columns[0]).getData()[row_num];
        TST time = typeid_cast<const ColumnVector<TST> &>(*columns[1]).getData()[row_num];

        this->data(place).add(amount, time, arena);
    }

    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & data = const_cast<AggregateFunctionXirrData<AmountType, TST> &>(this->data(place));
        auto & xirr_items = data.xirr_items;

        std::sort(xirr_items.begin(), xirr_items.end());

        Float64 guess = has_guess ? guess_value : calculateAnnualizedReturn(xirr_items);

        Float64 res = calculateXirr(xirr_items, guess);

        dynamic_cast<ColumnFloat64 &>(to).getData().push_back(res);
    }

    Float64 calculateAnnualizedReturn(const XirrItems & xirr_items) const
    {
        double debit = 0.0, end_value = 0.0, power = 0.0;
        TST min_time, max_time;

        // consume that items are ordered by time asc
        min_time = xirr_items[0].time;
        max_time = xirr_items[xirr_items.size()-1].time;

        for (const auto & item : xirr_items)
        {
            double val = item.amount;

            end_value += val;
            if (val < 0.0)
                debit -= val;

            if (item.time > max_time)
                max_time = item.time;
            else if (item.time < min_time)
                min_time = item.time;
        }

        power = XIRR_DAYS_PER_YEAR / (max_time - min_time);

        if (xirr_items[0].amount > 0)
            power = -power;

        return pow(1 + end_value/debit, power) - 1;
    }

    Float64 calculateXirr(const XirrItems & xirr_items, Float64 guess) const
    {
        TST time0 = xirr_items[0].time;

        for (size_t i = 0; i < MAX_LOOPS; i++)
        {
            double deriv = 0.0;
            double result = xirr_items[0].amount;
            double r = guess + 1.0;
            double epsilon, new_rate;

            for (size_t j = 1; j < xirr_items.size(); j++)
            {
                double years = (xirr_items[j].time - time0) / XIRR_DAYS_PER_YEAR;
                double val = xirr_items[j].amount;
                double exp = pow(r, years);

                result += val / exp;
                deriv -= years * val / (exp * r); /* (exp * r) = pow(r, years + 1) */
            }

            new_rate = guess - (result / deriv);
            epsilon = fabs(new_rate - guess);

            /* It's not getting any better by adding numbers to infinity */
            if (!isfinite(new_rate))
                return NAN;

            if (epsilon <= MAX_EPSILON || fabs(result) < MAX_EPSILON)
                return new_rate;

            /* Try another guess, hopefully we're closer now. */
            guess = new_rate;
        }

        /* Didn't converge */
        return NAN;
    }

    bool allocatesMemoryInArena() const override { return false; }
};

}
