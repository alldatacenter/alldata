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
#include <IO/ReadHelpers.h>

#include <Common/ThreadPool.h>
#include <Common/setThreadName.h>
#include <Common/CurrentThread.h>
#include <common/logger_useful.h>
#include <shared_mutex>
#include <sys/time.h>

#include <array>
#include <vector>
#include <tuple>
#include <string>
#include <functional>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeBitMap64.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnBitMap64.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <AggregateFunctions/IAggregateFunction.h>
#include "AggregateFunctions/AggregateFunctionBitMapJoinAndCard.h"

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NUMBER_OF_ARGUMENTS_DOES_NOT_MATCH;
}

struct PositionTuples;

struct AggregateFunctionBitMapJoinAndCard2Data
{
    std::vector<JoinTuple> input_tuples;

    AggregateFunctionBitMapJoinAndCard2Data() = default;

    void add(const BitMapPtr & bitmap_ptr, const Int32 & pos, const JoinKey & join_key, const String & attr_val, const Strings & args, Int32 union_num)
    {
        if (pos <= 0 || pos > union_num+1)
            throw Exception("AggregateFunction BitMapJoinAndCard2: Wrong position value. Position starts from 1 and ends with join_num+1, please check", DB::ErrorCodes::LOGICAL_ERROR);

        Strings attr_vals(union_num+1);
        attr_vals[pos-1] = attr_val;

        input_tuples.emplace_back(std::make_tuple(bitmap_ptr, pos, join_key, std::move(attr_vals), std::move(args)));
    }

    void merge(const AggregateFunctionBitMapJoinAndCard2Data & rhs)
    {
        const std::vector<JoinTuple> & rhs_input_tuples = rhs.input_tuples;
        input_tuples.insert(input_tuples.end(), make_move_iterator(rhs_input_tuples.begin()), make_move_iterator(rhs_input_tuples.end()));
    }

    void serialize(WriteBuffer & buf) const
    {

        size_t input_tuples_size = input_tuples.size();
        writeVarUInt(input_tuples_size, buf);
        for (auto it = input_tuples.begin(); it != input_tuples.end(); ++it)
        {
            BitMapPtr bitmap_ptr;
            Int32 pos;
            JoinKey joinkey;
            Strings attr_vals;
            Strings args;
            std::tie(bitmap_ptr, pos, joinkey, attr_vals, args) = *it;

            size_t bytes_size = (*bitmap_ptr).getSizeInBytes();
            writeVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            (*bitmap_ptr).write(buffer.data());
            writeString(buffer.data(), bytes_size, buf);

            writeVarInt(pos, buf);
            writeVarInt(joinkey, buf);

            writeVarUInt(attr_vals.size(), buf);
            for (auto str: attr_vals)
            {
                writeString(str, buf);
            }

            writeVarUInt((args).size(), buf);
            for (auto a: args)
            {
                writeString(a, buf);
            }
        }
    }

    void deserialize(ReadBuffer & buf)
    {

        size_t input_tuples_size = 0;
        readVarUInt(input_tuples_size, buf);

        for (size_t i = 0; i < input_tuples_size; ++i)
        {
            size_t bytes_size;
            readVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            buf.readStrict(buffer.data(), bytes_size);
            BitMap64 bitmap = BitMap64::readSafe(buffer.data(), bytes_size);

            Int32 pos;
            readVarInt(pos, buf);

            JoinKey joinkey;
            readVarInt(joinkey, buf);

            size_t attrs_size = 0;
            readVarUInt(attrs_size, buf);
            Strings attr_vals;
            for (size_t j = 0; j < attrs_size; ++j)
            {
                String attr_val;
                readString(attr_val, buf);
                attr_vals.emplace_back(std::move(attr_val));
            }

            size_t args_size = 0;
            readVarUInt(args_size, buf);
            Strings args;
            for (size_t j = 0; j < args_size; ++j)
            {
                String arg;
                readString(arg, buf);
                args.emplace_back(std::move(arg));
            }

            JoinTuple tup = std::make_tuple(std::make_shared<BitMap64>(std::move(bitmap)), pos, joinkey, std::move(attr_vals), std::move(args));
            input_tuples.emplace_back(std::move(tup));
        }
    }

};

class AggregateFunctionBitMapJoinAndCard2 final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapJoinAndCard2Data, AggregateFunctionBitMapJoinAndCard2>
{
public:
    AggregateFunctionBitMapJoinAndCard2(const DataTypes & argument_types_, const Int32 & union_num_, const UInt64 & thread_num_, const UInt64 limit_bitmap_number_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapJoinAndCard2Data, AggregateFunctionBitMapJoinAndCard2>(argument_types_, {})
    , union_num(union_num_), thread_num(thread_num_), limit_bitmap_number(limit_bitmap_number_)
    {
        arguments_num = argument_types_.size();
    }

    String getName() const override { return "BitMapJoinAndCard2";}
    bool allocatesMemoryInArena() const override { return false; }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & col_bitmap = static_cast<const ColumnBitMap64 &>(*columns[0]);
        const BitMap64 & bitmap = col_bitmap.getBitMapAtImpl(row_num);
        auto bitmap_ptr = std::make_shared<BitMap64>(std::move(const_cast<BitMap64 &>(bitmap)));

        const auto & col_position = static_cast<const ColumnInt8 &>(*columns[1]);
        const Int32 & positionInUnion = static_cast<Int32>(col_position.getElement(row_num));

        const auto & col_joinkey = static_cast<const ColumnInt32 &>(*columns[2]);
        const JoinKey & join_key = col_joinkey.getElement(row_num);

        const auto & col_attr_val = static_cast<const ColumnString &>(*columns[3]);
        String attr_val = col_attr_val.getDataAt(row_num).toString();

        Strings args;
        for (size_t i = 4; i < arguments_num; ++i)
        {
            const auto & col_arg = static_cast<const ColumnString &>(*columns[i]); //args start from columns[4]
            args.emplace_back(col_arg.getDataAt(row_num).toString());
        }

        this->data(place).add(bitmap_ptr, positionInUnion, join_key, attr_val, args, union_num);
    }

    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs, Arena *) const override
    {
        this->data(place).merge(this->data(rhs));
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    DataTypePtr getReturnType() const override
    {
        DataTypes type;
        type.emplace_back(std::make_shared<DataTypeUInt64>());
        type.emplace_back(std::make_shared<DataTypeInt32>());
        type.insert(type.end(), union_num+1, std::make_shared<DataTypeString>());
        size_t args_num = arguments_num - 4;
        type.insert(type.end(), args_num, std::make_shared<DataTypeString>());

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(type));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & input_tuples = this->data(place).input_tuples;

        std::vector<PositionTuples> tuplesByPosition;
        for (size_t i = 0; i < union_num + 1; ++i)
        {
            tuplesByPosition.emplace_back(i, JoinTuplePtrs());
        }

        //partition all input tuples by position
        for (auto & p : input_tuples)
        {
            Int32 pos = std::get<1>(p);
            tuplesByPosition.at(pos-1).addTuple(p);
        }

        const auto res = calcJoin(tuplesByPosition);

        auto & col = static_cast<ColumnArray &>(to);
        auto &col_offsets = static_cast<ColumnArray::ColumnOffsets &>(col.getOffsetsColumn());

        ColumnTuple &tup_nested = static_cast<ColumnTuple &>(col.getData());

        auto & col_bitmap_card = static_cast<ColumnUInt64 &>(tup_nested.getColumn(0));
        auto & col_joinkey = static_cast<ColumnInt32 &>(tup_nested.getColumn(1));

        size_t args_num = arguments_num - 4;

        for (auto & p : res)
        {
            for (auto rt = p.begin(); rt != p.end(); ++rt)
            {
                UInt64 bitmap_cardinality;
                JoinKey joinkey;
                Strings attr_vals;
                Strings args;

                std::tie(bitmap_cardinality, std::ignore, joinkey, attr_vals, args) = std::move(*rt);
                col_bitmap_card.insert(bitmap_cardinality);
                col_joinkey.insert(joinkey);

                for (size_t i = 0; i < union_num+1; i++)
                {
                    (static_cast<ColumnString &>(tup_nested.getColumn(2+i))).insert(attr_vals.at(i));
                }

                for (size_t i = 0; i < args_num; i++)
                {
                    (static_cast<ColumnString &>(tup_nested.getColumn(2 + union_num+1 + i))).insert(args.at(i));
                }
            }
        }

        col_offsets.getData().push_back(col.getData().size());
    }

private:
    std::vector<std::vector<ResultTuple>>
    calcJoinMultiThreads(std::shared_ptr<std::vector<JoinTuplePtrs>> & res_ptr, const std::shared_ptr<PositionTuples> & rhs, size_t thread_num_, const bool is_last_join) const
    {
        std::vector<JoinTuplePtrs> intermediate_tuples_bucktes(thread_num_, JoinTuplePtrs()); // It store the intermediate JOIN result, and it's used for next JOIN
        std::vector<std::vector<ResultTuple>> res_tuples_buckets(thread_num_, std::vector<ResultTuple>());  // It store the final result of the last JOIN
        ThreadGroupStatusPtr thread_group = CurrentThread::getGroup();

        auto runJoinAndCard = [&] (size_t index)
        {
            setThreadName("bitmapJoinAndCard");
            CurrentThread::attachToIfDetached(thread_group);
            JoinTuplePtrs tuples_tmp;
            std::vector<ResultTuple> res_tuples_in_a_thread;

            auto & left = res_ptr->at(index);
            for (auto rt = rhs->tuples.begin(); rt != rhs->tuples.end(); ++rt)
            {
                for (auto lt = left.begin(); lt != left.end(); ++lt)
                {
                    BitMapPtr bitmap_ptr, rt_bitmap_ptr;
                    Int32 pos, rt_pos;
                    JoinKey joinkey;
                    Strings attr_vals, rt_attr_vals;
                    Strings args, rt_args;

                    std::tie(bitmap_ptr, pos, joinkey, attr_vals, args) = *(*lt);
                    std::tie(rt_bitmap_ptr, rt_pos, std::ignore, rt_attr_vals, rt_args) = *(*rt);

                    BitMap64 bitmap(*bitmap_ptr);
                    bitmap &= *rt_bitmap_ptr;

                    attr_vals[rt_pos-1] = rt_attr_vals[rt_pos-1];

                    for (size_t i = 0; i < args.size(); ++i)
                    {
                        if (args[i] == "#-1#" && rt_args[i] != "#-1#")
                            args[i] = rt_args[i];
                    }

                    if (!is_last_join)
                    {
                        JoinTuple tmp = std::make_tuple(std::make_shared<BitMap64>(std::move(bitmap)), pos, joinkey, std::move(attr_vals), std::move(args));
                        tuples_tmp.emplace_back(std::make_shared<JoinTuple>(std::move(tmp)));
                    }
                    else {
                        ResultTuple tmp(std::make_tuple(std::move(bitmap.cardinality()), pos, joinkey, std::move(attr_vals), std::move(args)));
                        res_tuples_in_a_thread.emplace_back(tmp);
                    }
                }
            }
            left.clear();

            if (!is_last_join)
                intermediate_tuples_bucktes[index] = std::move(tuples_tmp);
            else
                res_tuples_buckets[index] = std::move(res_tuples_in_a_thread);
        };

        std::unique_ptr<ThreadPool> threadPool = std::make_unique<ThreadPool>(thread_num_);

        for (size_t i = 0; i < thread_num; ++i)
        {
            auto joinAndCardFunc = std::bind(runJoinAndCard, i);
            threadPool->scheduleOrThrowOnError(joinAndCardFunc);
        }

        threadPool->wait();

        res_ptr = std::make_shared<std::vector<JoinTuplePtrs>>(std::move(intermediate_tuples_bucktes));
        // For intermediate JOIN, a empty object returned,
        // the true result is returned after the last JOIN finished.
        return res_tuples_buckets;
    }

    std::vector<std::vector<ResultTuple>> calcJoin(std::vector<PositionTuples> & position_tuples) const
    {
        //partition the entire position tuples into several parts
        if (position_tuples.empty())
            throw Exception("BitMapJoinAndCard::calcJoin: empty input data!", DB::ErrorCodes::LOGICAL_ERROR);

        //look up for the largest parts
        size_t max_size = 0;
        size_t max_size_pos = 0;
        size_t total_rows = 0;
        for (auto it = position_tuples.cbegin(); it != position_tuples.cend(); ++it)
        {
            if (it->tuples.size() > limit_bitmap_number)
                throw Exception("AggregateFunction " + getName() +
                    ": receives too many bitmaps in the " + std::to_string(it->position) + "-th subquery", ErrorCodes::TOO_MANY_ROWS);

            total_rows *= it->tuples.size();

            if (it != position_tuples.end()-1 && total_rows > limit_bitmap_number)
                throw Exception("AggregateFunction " + getName() + ": The memory is out of limit to contain any bitmap after several JOINs," +
                    " and the remaining JOINs can't go on", ErrorCodes::TOO_MANY_ROWS);

            if (it->tuples.size() > max_size)
            {
                max_size_pos = it->position;
                max_size = it->tuples.size();
            }
        }

        if (total_rows > limit_bitmap_number)
            throw Exception("AggregateFunction " + getName() + ": The memory is out of limit to contain the whole result,", ErrorCodes::TOO_MANY_ROWS);

        //partition the largest parts for parallel computation
        std::shared_ptr<PositionTuples> first_ptr = std::make_shared<PositionTuples>(std::move(position_tuples[max_size_pos]));
        std::vector<JoinTuplePtrs> tups_parts(thread_num, std::vector<std::shared_ptr<JoinTuple>>());
        for (size_t i = 0; i < max_size; ++i)
        {
            tups_parts[i%thread_num].emplace_back(std::move(first_ptr->tuples.at(i)));
        }
        std::shared_ptr<std::vector<JoinTuplePtrs>> tups_parts_ptr = std::make_shared<std::vector<JoinTuplePtrs>>(std::move(tups_parts));

        std::vector<std::vector<ResultTuple>> result_buckets;
        for (size_t i = 1; i < union_num+1; ++i)
        {
            auto next_index = (max_size_pos + i) % (union_num+1);
            if (i != union_num)
                result_buckets = calcJoinMultiThreads(tups_parts_ptr, std::make_shared<PositionTuples>(position_tuples[next_index]), thread_num, false);
            else
                result_buckets = calcJoinMultiThreads(tups_parts_ptr, std::make_shared<PositionTuples>(position_tuples[next_index]), thread_num, true);
        }

        return result_buckets;
    }

    UInt32 union_num;
    size_t arguments_num;
    size_t thread_num;
    size_t limit_bitmap_number;
};

}
