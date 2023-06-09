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

#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnBitMap64.h>

#include <AggregateFunctions/IAggregateFunction.h>
#include <AggregateFunctions/AggregateBitMapExpressionCommon.h>


namespace DB
{

struct AggregateFunctionBitMapMaxLevelData
{
    AggregateFunctionBitMapData<Int64> level_data;

    void merge(AggregateFunctionBitMapMaxLevelData & rhs)
    {
        if (level_data.empty())
            level_data = std::move(rhs.level_data);
        else
            level_data.merge(std::move(rhs.level_data));
    }
};

class AggregateFunctionBitMapMaxLevel final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapMaxLevelData, AggregateFunctionBitMapMaxLevel>
{
private:
    UInt64 return_tpye;

public:
    explicit AggregateFunctionBitMapMaxLevel(const DataTypes & argument_types_, UInt64 return_tpye_)
        : IAggregateFunctionDataHelper<AggregateFunctionBitMapMaxLevelData, AggregateFunctionBitMapMaxLevel>(argument_types_, {}),
        return_tpye(return_tpye_)
    {}

    String getName() const override { return "bitmapMaxLevel"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        DataTypes types;
        if (return_tpye == 0)
        {
            types.emplace_back(std::make_shared<DataTypeInt64>()); // level
            types.emplace_back(std::make_shared<DataTypeUInt64>()); // result bitmap cardinality
        }
        else
        {
            types.emplace_back(std::make_shared<DataTypeInt64>()); // level
            types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>()));  // uid array
        }

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        Int64 key = columns[0]->getInt(row_num);

        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[1]);
        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        this->data(place).level_data.add(key, bitmap);
    }

    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs, Arena * ) const override
    {
        auto & lhs_data = this->data(place);
        auto & rhs_data = const_cast<AggregateFunctionBitMapMaxLevelData &>(this->data(rhs));

        lhs_data.merge(rhs_data);
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).level_data.serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).level_data.deserialize(buf);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & level_bitmaps = this->data(const_cast<AggregateDataPtr>(place)).level_data.bitmap_map;
        std::vector<Int64> keys;
        for (auto it = level_bitmaps.begin(); it != level_bitmaps.end(); ++it)
        {
            keys.emplace_back(it->first);
        }

        if (keys.size() > 1)
        {
            std::sort(keys.begin(), keys.end(), std::greater<Int64>());

            /// remove the duplicated id from all bitmaps, each id only exists in the max level where it really occurs.
            for (size_t i = 0; i < keys.size()-1; ++i)
            {
                for (size_t j = i+1; j < keys.size(); ++j)
                {
                    BitMap64 res = level_bitmaps.at(keys.at(i)) & level_bitmaps.at(keys.at(j));
                    if (!res.isEmpty())
                        level_bitmaps.at(keys.at(j)) ^= res;
                }
            }
        }

        // insert result
        auto & column_res = static_cast<ColumnArray &>(to);
        auto & res_offsets = column_res.getOffsets();
        ColumnArray::Offset prev_res_offset = res_offsets.empty() ? 0 : res_offsets.back();

        auto & tuple_in_array = static_cast<ColumnTuple &>(column_res.getData());

        if (return_tpye == 0) // summary only
        {
            auto & column_level = static_cast<ColumnUInt64 &>(tuple_in_array.getColumn(0));
            auto & column_summary = static_cast<ColumnUInt64 &>(tuple_in_array.getColumn(1));
            for (auto it = keys.rbegin(); it != keys.rend(); ++it)
            {
                column_level.insertValue(*it);

                auto & bitmap = level_bitmaps.at(*it);
                column_summary.insertValue(bitmap.cardinality());
            }
            prev_res_offset += keys.size();
            res_offsets.emplace_back(prev_res_offset);
            return;
        }

        auto & column_level = static_cast<ColumnUInt64 &>(tuple_in_array.getColumn(0));
        auto & column_array = static_cast<ColumnArray &>(tuple_in_array.getColumn(1));

        ColumnVector<UInt64> * column_id = typeid_cast<ColumnVector<UInt64> *>(&(column_array.getData()));
        ColumnVector<UInt64>::Container & res_container = column_id->getData();
        ColumnArray::Offsets & id_offsets = column_array.getOffsets();
        ColumnArray::Offset prev_id_offset = id_offsets.empty() ? 0 : id_offsets.back();

        if (return_tpye == 2) // summary & detail, summary here, detail logic is reused
        {
            for (auto it = keys.rbegin(); it != keys.rend(); ++it)
            {
                column_level.insertValue(*it);

                auto & bitmap = level_bitmaps.at(*it);
                res_container.emplace_back(bitmap.cardinality());

                prev_id_offset += 1;
                id_offsets.emplace_back(prev_id_offset);
            }
            prev_res_offset += keys.size();
        }

        /// return type = 1, detail logic
        for (auto it = keys.rbegin(); it != keys.rend(); ++it)
        {
            column_level.insertValue(*it);
            auto & bitmap = level_bitmaps.at(*it);
            size_t size = bitmap.cardinality();
            for (roaring::Roaring64MapSetBitForwardIterator iter = bitmap.begin(); iter != bitmap.end(); ++iter)
            {
                res_container.emplace_back(*iter);
            }
            prev_id_offset += size;
            id_offsets.emplace_back(prev_id_offset);
        }

        prev_res_offset += keys.size();
        res_offsets.emplace_back(prev_res_offset);
    }

};
}
