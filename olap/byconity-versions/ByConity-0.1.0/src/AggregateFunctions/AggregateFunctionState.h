/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <DataTypes/DataTypeAggregateFunction.h>
#include <AggregateFunctions/IAggregateFunction.h>
#include <Columns/ColumnAggregateFunction.h>
#include <Common/assert_cast.h>


namespace DB
{
struct Settings;


/** Not an aggregate function, but an adapter of aggregate functions,
  * Aggregate functions with the `State` suffix differ from the corresponding ones in that their states are not finalized.
  * Return type - DataTypeAggregateFunction.
  */

class AggregateFunctionState final : public IAggregateFunctionHelper<AggregateFunctionState>
{
private:
    AggregateFunctionPtr nested_func;
    DataTypes arguments;
    Array params;

public:
    AggregateFunctionState(AggregateFunctionPtr nested_, const DataTypes & arguments_, const Array & params_)
        : IAggregateFunctionHelper<AggregateFunctionState>(arguments_, params_)
        , nested_func(nested_), arguments(arguments_), params(params_) {}

    String getName() const override
    {
        return nested_func->getName() + "State";
    }

    DataTypePtr getReturnType() const override
    {
        return getStateType();
    }

    DataTypePtr getStateType() const override
    {
        return nested_func->getStateType();
    }

    void create(AggregateDataPtr __restrict place) const override
    {
        nested_func->create(place);
    }

    void destroy(AggregateDataPtr __restrict place) const noexcept override
    {
        nested_func->destroy(place);
    }

    bool hasTrivialDestructor() const override
    {
        return nested_func->hasTrivialDestructor();
    }

    size_t sizeOfData() const override
    {
        return nested_func->sizeOfData();
    }

    size_t alignOfData() const override
    {
        return nested_func->alignOfData();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        nested_func->add(place, columns, row_num, arena);
    }

    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        nested_func->merge(place, rhs, arena);
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        nested_func->serialize(place, buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena * arena) const override
    {
        nested_func->deserialize(place, buf, arena);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        assert_cast<ColumnAggregateFunction &>(to).getData().push_back(place);
    }

    /// Aggregate function or aggregate function state.
    bool isState() const override { return true; }

    bool allocatesMemoryInArena() const override
    {
        return nested_func->allocatesMemoryInArena();
    }

    bool handleNullItSelf() const override
    {
        return nested_func->handleNullItSelf();
    }

    AggregateFunctionPtr getNestedFunction() const override { return nested_func; }
};

}
