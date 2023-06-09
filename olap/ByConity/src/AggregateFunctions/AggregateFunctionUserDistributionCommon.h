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
#include <AggregateFunctions/AggregateRetentionCommon.h>
#include <DataTypes/DataTypeArray.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnArray.h>

namespace DB
{

    using StateType = UInt8;
    const StateType ARRIVE = 0x01;
    const StateType NEWONE = 0x02;

    struct AggregateFunctionUserDistributionData
    {
        StateType user_distribution[1];
    };

    template<typename TimeStampType>
    class AggregateFunctionUserDistributionCommon
            : public IAggregateFunctionDataHelper<AggregateFunctionUserDistributionData,
                    AggregateFunctionUserDistributionCommon<TimeStampType> >
    {
    protected:
        UInt64 m_num_slots;

    public:
        AggregateFunctionUserDistributionCommon(const DataTypes & arguments, const Array & params) :
             IAggregateFunctionDataHelper<AggregateFunctionUserDistributionData,
                           AggregateFunctionUserDistributionCommon<TimeStampType> >(arguments, params){}

        DataTypePtr getReturnType() const override
        {
            return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt8> >());
        }

        void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena *) const override
        {
            UInt8 index = 0;
            StateType user_state = 0;

            bool pass = computeUserState(columns, row_num, index, user_state);
            if (!pass)
                return;

            auto & user_distribution = this->data(place).user_distribution;
            user_distribution[index] |= user_state;
        }

        void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
        {
            for (size_t i = 0; i < this->m_num_slots; i ++)
                this->data(place).user_distribution[i] |= this->data(rhs).user_distribution[i];
        }

        void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
        {
            auto & user_distribution = this->data(place).user_distribution;
            buf.write(reinterpret_cast<const char *>(&user_distribution[0]), m_num_slots * sizeof(user_distribution[0]));
        }

        void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena *) const override
        {
            auto & user_distribution = this->data(place).user_distribution;
            buf.read(reinterpret_cast<char *>(&user_distribution[0]), m_num_slots * sizeof(user_distribution[0]));
        }

        void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
        {
            const auto & user_distribution = this->data(place).user_distribution;

            ColumnArray & array_to = static_cast<ColumnArray &>(to);
            ColumnArray::Offsets & offsets_to = array_to.getOffsets();

            offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_num_slots);

            ColumnVector<UInt8>::Container  & data_to = static_cast<ColumnVector<UInt8> &>(array_to.getData()).getData();

            data_to.insert(user_distribution, user_distribution + m_num_slots);
        }

        void create(const AggregateDataPtr place) const override
        {
            auto* d =  new (place) AggregateFunctionUserDistributionData;
            std::fill(d->user_distribution, d->user_distribution + m_num_slots, 0);
        }

        size_t sizeOfData() const override
        {
            return sizeof(AggregateFunctionUserDistributionData) + m_num_slots * sizeof(UInt8);
        }

        virtual bool computeUserState(const IColumn **columns, size_t row_num, UInt8 & index, StateType & user_state) const = 0;

    };
}
