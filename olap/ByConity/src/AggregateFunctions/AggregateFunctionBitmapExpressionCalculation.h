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
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnBitMap64.h>

#include <AggregateFunctions/IAggregateFunction.h>
#include <AggregateFunctions/AggregateBitMapExpressionCommon.h>
#include <Common/typeid_cast.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}


template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
struct AggregateFunctionBitMapCountData
{
    AggregateFunctionBitMapData<T> bitmap_data;
    UInt64 count = 0;

    void merge(AggregateFunctionBitMapCountData<T> & rhs)
    {
        if (bitmap_data.empty())
        {
            bitmap_data = std::move(rhs.bitmap_data);
            count = rhs.count;
        }
        else
        {
            bitmap_data.merge(std::move(rhs.bitmap_data));
        }
    }

    void serialize(WriteBuffer & buf)
    {
        bitmap_data.serialize(buf);
        writeVarUInt(count, buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        bitmap_data.deserialize(buf);
        readVarUInt(count, buf);
    }
};

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> || std::is_same_v<T, String> > >
struct AggregateFunctionBitMapMultiCountData
{
    AggregateFunctionBitMapData<T> bitmap_data;
    std::vector<UInt64> count_vector;

    void merge(AggregateFunctionBitMapMultiCountData<T> & rhs)
    {
        if (bitmap_data.empty())
        {
            bitmap_data = std::move(rhs.bitmap_data);
            count_vector = std::move(rhs.count_vector);
        }
        else
        {
            bitmap_data.merge(std::move(rhs.bitmap_data));
        }
    }

    void serialize(WriteBuffer & buf)
    {
        bitmap_data.serialize(buf);
        writeVarUInt(count_vector.size(), buf);
        for (size_t i = 0; i < count_vector.size(); i++)
            writeVarUInt(count_vector[i], buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        bitmap_data.deserialize(buf);
        size_t vector_size;
        readVarUInt(vector_size, buf);
        for (size_t i = 0; i < vector_size; ++i)
        {
            UInt64 temp_count;
            readVarUInt(temp_count, buf);
            count_vector.emplace_back(temp_count);
        }
    }
};



template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
 struct AggregateFunctionBitMapExtractData
 {
     AggregateFunctionBitMapData<T> bitmap_data;

     void merge(AggregateFunctionBitMapExtractData<T> & rhs)
     {
         if (bitmap_data.empty())
         {
             bitmap_data = std::move(rhs.bitmap_data);
         }
         else
         {
             bitmap_data.merge(std::move(rhs.bitmap_data));
         }
    }

    void serialize(WriteBuffer & buf)
    {
        bitmap_data.serialize(buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        bitmap_data.deserialize(buf);
    }
};





/// Simply count number of calls.
template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
class AggregateFunctionBitMapCount final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapCountData<T>, AggregateFunctionBitMapCount<T>>
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<T>>;
private:
    BitMapExpressionAnalyzer<T> analyzer;
    T final_key;
    UInt64 is_bitmap_execute = false;
public:
    AggregateFunctionBitMapCount(const DataTypes & argument_types_, String expression_, UInt64 is_bitmap_execute_)
     : IAggregateFunctionDataHelper<AggregateFunctionBitMapCountData<T>, AggregateFunctionBitMapCount<T>>(argument_types_, {})
     , analyzer(expression_), is_bitmap_execute(is_bitmap_execute_)
    {
        final_key = analyzer.final_key;
    }

    String getName() const override { return "bitmapCount"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeUInt64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_key = static_cast<const ColumnVector<T> &>(*columns[0]);
        T key = column_key.getElement(row_num);

        // use's key in this range may get the intermediate cached results
        if (key >= final_key && key < 0)
            throw Exception("The tag (or bitmap key): " + std::to_string(key) + " affects the computation, " +
                "please change another number, maybe positive number is better", ErrorCodes::LOGICAL_ERROR);

        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[1]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        auto & bitmap_data = this->data(place).bitmap_data;

        bitmap_data.add(key, bitmap);
    }

    bool mergeSingleStream(AggregateFunctionBitMapCountData<T> & bitmap_count_data) const
    {
        auto & bitmap_data = bitmap_count_data.bitmap_data;
        auto & count = bitmap_count_data.count;

        if (bitmap_data.is_finished)
            return false;

        analyzer.executeExpression(bitmap_data);
        count = bitmap_data.getCardinality(final_key);
        bitmap_data.is_finished = true;

        return true;
    }

    void mergeTwoStreams(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs) const
    {
        auto & lhs_bitmap_data = this->data(place).bitmap_data;
        const auto & rhs_bitmap_data = this->data(rhs).bitmap_data;

        auto lhs_bitmap_it = lhs_bitmap_data.bitmap_map.find(final_key);
        auto rhs_bitmap_it = rhs_bitmap_data.bitmap_map.find(final_key);
        bool lhs_bitmap_exists = lhs_bitmap_it != lhs_bitmap_data.bitmap_map.end();
        bool rhs_bitmap_exists = rhs_bitmap_it != rhs_bitmap_data.bitmap_map.end();

        if (lhs_bitmap_exists && rhs_bitmap_exists)
        {
            lhs_bitmap_it->second |= rhs_bitmap_it->second;
        }
        else
            throw Exception("Cannot find final key when merge two streams in " + getName(), ErrorCodes::LOGICAL_ERROR);
    }

    /*
    * The merge function is based on the fact different stream can calculate its result independently.
    * If we use BITMAPEXECUTE, ParallelBitMapBlockInputStream will produce many streams and invoke this merge
    * function for each stream. And only the ParallelBitMapBlockInputStream can invoke merge with only one stream, so
    * we can calculate expression for this single stream directly and set is_final as true.
    * When there are two streams, it may be invoked by ParallelBitMapBlockInputStream or normal aggregating stream. But,
    * if is_finished is true for both two streams, we can ensure these two streams can calculate directly.
    * Otherwise, handle them as normal aggregate functions, that is, use bitmap or to get all bitmap and calculate expression
    * in insertResultInto functions
    *
    * For distributed queries, we also have two merge model:
    * 1. distributed_perfect_shard: each node performs the merge as single node, the final result is merged by sum or bitmapOr function.
    * 2. distributed table: each node performs the aggregation at local and return a intermediate status, then at the coordinator node, it performs a
    * second aggregation. There exists the case the second aggregation has multiple merges - the first merge has single stream, and then merge the following
    * streams.
    *
    * The second case will obstruct the determine of BITMAPEXECUTE model, since for a merge with only one stream, we cannot distinguish whether it is a BITMAPEXECUTE model
    * or distributed table. The only thing we can do is assuming this stream can be used to execute expression.
    * This assumption is correct as follows.
    * 1. If it is a distributed table and it has only one stream, execute expression in advanced will not affect the correctness of the result.
    *    If it has more than one stream, we lost some performance to execute first stream's expression. The first stream will merge other streams again and set is_finished as false.
    *    Then calculate the result in the insertResultInto function.
    * 2. If it is a BITMAPEXECUTE model, each stream will be passed to merge function independently in ParallelBitMapInputStream so that each stream will set is_finished as true.
    *
    */
    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        auto & lhs_data = this->data(place);
        auto & rhs_data = const_cast<AggregateFunctionBitMapCountData<T> &>(this->data(rhs));

        if (is_bitmap_execute && lhs_data.bitmap_data.empty())
        {
            mergeSingleStream(rhs_data);
            lhs_data.merge(rhs_data);
        }
        else if (lhs_data.bitmap_data.is_finished && rhs_data.bitmap_data.is_finished)
        {
            lhs_data.count += rhs_data.count;
        }
        // If two stream are both false on is_finished, it must not a BITMAPEXECUTE model, so merge them and delay expression execution on insertResultInto
        else if (!lhs_data.bitmap_data.is_finished && !rhs_data.bitmap_data.is_finished)
        {
            lhs_data.merge(rhs_data);
        }
        // If one of stream's is_finished is false, it means
        // 1. it is a BITMAPEXECUTE model but not a distributed_perfect_shard model.
        // 2. it may be execution on distributed table
        // So it is better to merge them to guarantee the correctness of result
        else
        {
            lhs_data.merge(rhs_data);
        }
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    // If is_finished is false, aggregate functions need to caculate expression.
    // It means the aggregate function is a normal aggregate (not a BITMAPEXECUTE), so the final
    // result is computed in insertResultInto
    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & local_data = const_cast<AggregateFunctionBitMapCountData<T> &>(this->data(place));

        if (!local_data.bitmap_data.is_finished)
        {
            mergeSingleStream(local_data);
        }

        static_cast<ColumnUInt64 &>(to).getData().push_back(local_data.count);
    }

};

/// Simply count number of calls.
template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
class AggregateFunctionBitMapMultiCount final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapMultiCountData<T>, AggregateFunctionBitMapMultiCount<T>>
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<T>>;
private:
    BitMapExpressionMultiAnalyzer<T> analyzer;
    std::vector<T> final_keys;
public:
    AggregateFunctionBitMapMultiCount(const DataTypes & argument_types_, std::vector<String> expression_)
            : IAggregateFunctionDataHelper<AggregateFunctionBitMapMultiCountData<T>, AggregateFunctionBitMapMultiCount<T>>(argument_types_, {}), analyzer(expression_)
    {
        final_keys = analyzer.final_keys;
    }

    String getName() const override { return "bitmapMultiCount"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_key = static_cast<const ColumnVector<T> &>(*columns[0]);
        T key = column_key.getElement(row_num);

        // use's key in this range may get the intermediate cached results
        if (std::any_of(final_keys.begin(), final_keys.end(), [&](T final_key) {
                return key >= final_key && key < 0; }))
            throw Exception("The tag (or bitmap key): " + std::to_string(key) + " affects the computation, " +
                "please change another number, maybe positive number is better", ErrorCodes::LOGICAL_ERROR);

        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[1]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        auto & bitmap_data = this->data(place).bitmap_data;

        bitmap_data.add(key, bitmap);
    }

    bool mergeSingleStream(AggregateFunctionBitMapMultiCountData<T> & bitmap_count_data) const
    {
        auto & bitmap_data = bitmap_count_data.bitmap_data;
        auto & count_vector = bitmap_count_data.count_vector;

        if (bitmap_data.is_finished)
            return false;
        count_vector.clear();
        for (size_t i = 0; i < final_keys.size(); i++)
        {
            analyzer.executeExpression(bitmap_data, i);
            count_vector.push_back(bitmap_data.getCardinality(final_keys[i]));
        }
        bitmap_data.is_finished = true;

        return true;
    }

    void mergeTwoStreams(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs) const
    {
        auto & lhs_bitmap_data = this->data(place).bitmap_data;
        const auto & rhs_bitmap_data = this->data(rhs).bitmap_data;
        for (auto& final_key: final_keys)
        {
            auto lhs_bitmap_it = lhs_bitmap_data.bitmap_map.find(final_key);
            auto rhs_bitmap_it = rhs_bitmap_data.bitmap_map.find(final_key);
            bool lhs_bitmap_exists = lhs_bitmap_it != lhs_bitmap_data.bitmap_map.end();
            bool rhs_bitmap_exists = rhs_bitmap_it != rhs_bitmap_data.bitmap_map.end();
            if (lhs_bitmap_exists && rhs_bitmap_exists)
            {
                lhs_bitmap_it->second |= rhs_bitmap_it->second;
            }
            else
                throw Exception("Cannot find final key when merge two streams in " + getName(), ErrorCodes::LOGICAL_ERROR);
        }
    }

    /*
    * The merge function is based on the fact different stream can caculate its result independently.
    * If we use BITMAPEXECUTE, ParallelBitMapBlockInputStream will produce many streams and invoke this merge
    * function for each stream. And only the ParallelBitMapBlockInputStream can invoke merge with only one stream, so
    * we can caculate expression for this single stream directly and set is_final as true.
    * When there are two streams, it may be invoked by ParallelBitMapBlockInputStream or normal aggregating stream. But,
    * if is_finished is true for both two streams, we can ensure these two streams can caculate directly.
    * Otherwise, handle them as normal aggregate functions, that is, use bitmap or to get all bitmap and caculate expression
    * in insertResultInto functions
    *
    * For distributed queries, we also have two merge model:
    * 1. distributed_perfect_shard: each node performs the merge as single node, the final result is merged by sum or bitmapOr function.
    * 2. distributed table: each node performs the aggregation at local and return a intermediate status, then at the coordinator node, it performs a
    * second aggregation. There exists the case the second aggregation has multiple merges - the first merge has single stream, and then merge the following
    * streams.
    *
    * The second case will obstruct the determine of BITMAPEXECUTE model, since for a merge with only one stream, we cannot distinguish whether it is a BITMAPEXECUTE model
    * or distributed table. The only thing we can do is assuming this stream can be used to execute expression.
    * This assumption is correct as follows.
    * 1. If it is a distributed table and it has only one stream, execute expression in advanced will not affect the correctness of the result.
    *    If it has more than one stream, we lost some performance to execute first stream's expression. The first stream will merge other streams again and set is_finished as false.
    *    Then caculate the result in the insertResultInto function.
    * 2. If it is a BITMAPEXECUTE model, each stream will be passed to merge function independently in ParallelBitMapInputStream so that each stream will set is_finished as true.
    *
    */
    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs, Arena *) const override
    {
        auto & lhs_data = this->data(place);
        auto & rhs_data = const_cast<AggregateFunctionBitMapMultiCountData<T> &>(this->data(rhs));

        if (lhs_data.bitmap_data.empty())
        {
            mergeSingleStream(rhs_data);
            lhs_data.merge(rhs_data);
        }
        else if (lhs_data.bitmap_data.is_finished && rhs_data.bitmap_data.is_finished)
        {
            for (size_t i = 0; i < final_keys.size(); i++)
                lhs_data.count_vector[i] += rhs_data.count_vector[i];
        }
            // If two stream are both false on is_finished, it must not a BITMAPEXECUTE model, so merge them and delay expression execution on insertResultInto
        else if (!lhs_data.bitmap_data.is_finished && !rhs_data.bitmap_data.is_finished)
        {
            lhs_data.merge(rhs_data);
        }
            // If one of stream's is_finished is false, it means
            // 1. it is a BITMAPEXECUTE model but not a distributed_perfect_shard model.
            // 2. it may be execution on distributed table
            // So it is better to merge them to guarantee the correctness of result
        else
        {
            lhs_data.merge(rhs_data);
        }
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    // If is_finished is false, aggregate functions need to caculate expression.
    // It means the aggregate function is a normal aggregate (not a BITMAPEXECUTE), so the final
    // result is computed in insertResultInto
    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & local_data = const_cast<AggregateFunctionBitMapMultiCountData<T> &>(this->data(place));

        if (!local_data.bitmap_data.is_finished)
        {
            mergeSingleStream(local_data);
        }
        Array res;
        for (auto count: local_data.count_vector)
            res.push_back(count);
        static_cast<ColumnArray &>(to).insert(res);
    }

};

struct BitMapExpressionWithDateMultiAnalyzer
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<String>>;
    std::vector<String> original_expressions;
    Int64 global_index = -1;
    std::unordered_set<Int64> keys_without_date;
    std::vector<Int64> final_keys;
    std::vector<bool> expression_only_ors;
    std::vector<BitMapExpressions> expression_actions_vector;
    std::unordered_map<String, size_t> replicated_keys;
    std::vector<NameSet> or_expressions;

    BitMapExpressionWithDateMultiAnalyzer(const std::vector<String> & expressions)
            : original_expressions(expressions)
    {
        analyze();
    }

    BitMapExpressionWithDateMultiAnalyzer() = default;

    void subExpression(std::stack<String> & expression_stack, String & right, size_t index)
    {
        while (!expression_stack.empty() &&
               (expression_stack.top() == "&" || expression_stack.top() == "|"
                || expression_stack.top() == "," || expression_stack.top() == "~" || right == "#"))
        {
            if (right == "#")
            {
                right = expression_stack.top();
                expression_stack.pop();
            }
            if (expression_stack.empty())
                break;
            String operation = expression_stack.top();
            expression_stack.pop();
            if (expression_stack.empty())
                throw Exception("Invalid expression " + operation + " for BitMap: " + original_expressions[index], ErrorCodes::LOGICAL_ERROR);
            String left = expression_stack.top();
            expression_stack.pop();
            // Optimize the case which right is equal to left.
            // If the operation is "~", add a no-exists result to expression_stack so that we can get an empty bitmap
            if (right == left && operation == "~")
            {
                Int64 res = global_index--;
                right = std::to_string(res);
            }
            else
            {
                Int64 res = global_index--;

                expression_actions_vector[index].emplace_back(std::move(left), std::move(operation), std::move(right), std::to_string(res), false);
                right = std::to_string(res);
            }
        }
    }

    void analyze()
    {
        for (size_t i = 0; i < original_expressions.size(); i++)
            analyzeExpression(original_expressions[i], i);
        for (auto & expression_actions: expression_actions_vector)
        {
            for (BitMapExpressionNode<String> & expression_action: expression_actions)
            {
                replicated_keys[expression_action.left] += 1;
                replicated_keys[expression_action.right] += 1;
            }
        }
        for (const NameSet& or_expression: or_expressions)
        {
            for (const String& or_expression_item: or_expression)
            {
                replicated_keys[or_expression_item] += 1;
            }
        }

        for (auto & expression_actions: expression_actions_vector)
        {
            for (BitMapExpressionNode<String> & expression_action: expression_actions)
            {
                auto left_it = replicated_keys.find(expression_action.left);
                do {
                    if (left_it == replicated_keys.end())
                        break;
                    if (left_it->second <= 1)
                        break;

                    expression_action.replicated = true;
                } while(false);
            }
        }
    }
    void analyzeExpression(String& original_expression, size_t index)
    {
        bool only_or = true;
        NameSet or_expression;
        String expression = original_expression + "#";
        std::stack<String> expression_stack;
        size_t expression_size = expression.size();
        std::vector<String> expression_vector;
        size_t number_index = expression_size;
        for (size_t i = 0; i < expression_size; i++)
        {
            if (expression[i] == '(' || expression[i] == '&' || expression[i] == '|' || expression[i] == ','
                || expression[i] == ')' || expression[i] == '#' || expression[i] == '~' || expression[i] == ' ') {
                if (number_index != expression_size) {
                    String number = expression.substr(number_index, (i - number_index));
                    // replace number with final key
                    if (number.size() > 1 && number[0] == '_')
                    {
                        auto res_index = std::stoi(number.substr(1));
                        if (res_index <= 0 || res_index > static_cast<Int32>(index))
                        {
                            throw Exception("Invalid expression " + number + " for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
                        }
                        number = std::to_string(final_keys[res_index - 1]);
                    }
                    else {
                        if (number.size() > 1 && (number.find('_') == std::string::npos))
                        {
                            auto key_without_date = std::stoi(number);
                            keys_without_date.emplace(key_without_date);
                        }
                    }

                    or_expression.insert(number);
                    expression_vector.push_back(std::move(number));
                    number_index = expression_size;
                }
                switch (expression[i]) {
                    case '(':
                        expression_vector.push_back("(");
                        break;
                    case '&':
                    {
                        expression_vector.push_back("&");
                        only_or = false;
                        break;
                    }
                    case '|':
                        expression_vector.push_back("|");
                        break;
                    case ')':
                        expression_vector.push_back(")");
                        break;
                    case ',':
                        expression_vector.push_back(",");
                        break;
                    case '~':
                    {
                        expression_vector.push_back("~");
                        only_or = false;
                        break;
                    }
                    case '#':
                        expression_vector.push_back("#");
                        break;
                }
            } else {
                if (number_index == expression_size) {
                    number_index = i;
                }
            }
        }
        BitMapExpressions expressions;
        expression_actions_vector.emplace_back(std::move(expressions));
        or_expressions.emplace_back(std::move(or_expression));
        expression_only_ors.emplace_back(only_or);
        if (only_or)
        {
            final_keys.emplace_back(global_index--);
            return;
        }

        for (size_t i = 0; i < expression_vector.size(); i++)
        {
            if (expression_vector[i] == "(" || expression_vector[i] == "&"
                || expression_vector[i] == "|" || expression_vector[i] == ","
                || expression_vector[i] == "~")
            {
                expression_stack.push(expression_vector[i]);
            }
            else if (expression_vector[i] == ")")
            {
                if (expression_stack.empty())
                    throw Exception("Invalid expression " + expression_vector[i] + " for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
                String number = expression_stack.top();
                expression_stack.pop();
                if (expression_stack.empty())
                    throw Exception("Invalid expression " + number + " for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
                expression_stack.pop();
                subExpression(expression_stack, number, index);
                expression_stack.push(number);
            }
            else
            {
                String right = expression_vector[i];
                // If there are replicated number, we cannot use some optimization strategy to execute expression
                subExpression(expression_stack, right, index);
                expression_stack.push(right);
            }
        }

        if (expression_stack.size() == 1) {
            Int64 temp_final_key;
            const String & res = expression_stack.top();
            std::istringstream iss(res);
            iss >> temp_final_key;
            final_keys.emplace_back(temp_final_key);
        } else {
            throw Exception("Invalid expression for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
        }
    }

    void executeExpressionImpl(String left_key, String operation, String right_key, String res, bool replicated, const AggregateFunctionBitMapData<String>& data) const
    {
        auto& bitmap_map = const_cast<std::unordered_map<String, BitMap64>&>(data.bitmap_map);
        auto left_iter = bitmap_map.find(left_key);
        auto right_iter = bitmap_map.find(right_key);

        if (left_iter == bitmap_map.end()) {
            BitMap64 temp_bitmap;
            auto res_pair = bitmap_map.emplace(left_key, std::move(temp_bitmap));
            if (res_pair.second)
                left_iter = res_pair.first;
            else
                throw Exception("Existing empty BitMap64 when inserting empty BitMap64", ErrorCodes::LOGICAL_ERROR);
        }
        if (right_iter == bitmap_map.end()) {
            BitMap64 temp_bitmap;
            auto res_pair = bitmap_map.emplace(right_key, std::move(temp_bitmap));
            if (res_pair.second)
                right_iter = res_pair.first;
            else
                throw Exception("Existing empty BitMap64 when inserting empty BitMap64", ErrorCodes::LOGICAL_ERROR);
        }
        if (!replicated)
        {
            if (operation == "|" || operation == ",") {
                left_iter->second |= right_iter->second;
                auto left_item = bitmap_map.extract(left_iter->first);
                left_item.key() = res;
                bitmap_map.insert(std::move(left_item));
            }
            else if (operation == "&") {
                left_iter->second &= right_iter->second;
                auto left_item = bitmap_map.extract(left_iter->first);
                left_item.key() = res;
                bitmap_map.insert(std::move(left_item));
            }
            else if (operation == "~") {
                left_iter->second -= right_iter->second;
                auto left_item = bitmap_map.extract(left_iter->first);
                left_item.key() = res;
                bitmap_map.insert(std::move(left_item));
            }
        }
        else
        {
            if (operation == "|" || operation == ",") {
                bitmap_map[res] = left_iter->second;
                bitmap_map[res] |= right_iter->second;
            }
            else if (operation == "&") {
                bitmap_map[res] = left_iter->second;
                bitmap_map[res] &= right_iter->second;
            }
            else if (operation == "~") {
                bitmap_map[res] = left_iter->second;
                bitmap_map[res] -= right_iter->second;
            }
        }
    }

    void executeExpressionOnlyOr(const AggregateFunctionBitMapData<String> & data, size_t index) const
    {
        std::set<String> key_set;
        for (const auto & expression : or_expressions[index])
        {
            key_set.insert(expression);
        }

        auto& bitmap_map = const_cast<std::unordered_map<String, BitMap64>&>(data.bitmap_map);

        if (key_set.size() == 1)
        {
            String key = *key_set.begin();
            auto it = bitmap_map.find(key);
            if (it == bitmap_map.end()) {
                BitMap64 temp_bitmap;
                auto res_pair = bitmap_map.emplace(key, std::move(temp_bitmap));
                if (res_pair.second)
                    it = res_pair.first;
                else
                    throw Exception("Existing empty BitMap64 when inserting empty BitMap64", ErrorCodes::LOGICAL_ERROR);
            }
            auto or_it = replicated_keys.find(key);
            if (or_it == replicated_keys.end())
                return;
            else if (or_it->second > 1)
            {
                bitmap_map[std::to_string(final_keys[index])] = it->second;
            }
            else
            {
                auto it_final_item = bitmap_map.extract(it->first);
                it_final_item.key() = std::to_string(final_keys[index]);
                bitmap_map.insert(std::move(it_final_item));
            }
            return;
        }

        std::map<UInt32, std::vector<Roaring*>> roaring_map;
        for (const auto & key: key_set)
        {
            auto it = bitmap_map.find(key);
            if (it == bitmap_map.end())
                continue;
            std::map<UInt32, Roaring> & inner_roaring = const_cast<std::map<UInt32, Roaring> &>(it->second.getRoarings());
            for (auto jt = inner_roaring.begin(); jt != inner_roaring.end(); ++jt)
            {
                if (roaring_map.find(jt->first) == roaring_map.end())
                    roaring_map.emplace(jt->first, std::vector<Roaring *>());
                roaring_map[jt->first].emplace_back(&jt->second);
            }
        }

        BitMap64 res_roaring;

        for (auto it = roaring_map.begin(); it != roaring_map.end(); ++it)
        {
            Roaring result = Roaring::fastunion(it->second.size(), &(*(it->second.begin())));
            const_cast<std::map<UInt32, Roaring> &>(res_roaring.getRoarings()).emplace(it->first, std::move(result));
        }

        bitmap_map[std::to_string(final_keys[index])] = std::move(res_roaring);
    }

    void executeExpression(const AggregateFunctionBitMapData<String> & data, size_t index) const
    {
        if (expression_only_ors[index])
        {
            executeExpressionOnlyOr(data, index);
        }
        else
        {
            for (const auto & action : expression_actions_vector[index])
            {
                executeExpressionImpl(action.left, action.op, action.right, action.res, action.replicated, data);
            }
        }
    }
};
/// Simply count number of calls.
class AggregateFunctionBitMapMultiCountWithDate final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapMultiCountData<String>, AggregateFunctionBitMapMultiCountWithDate>
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<String>>;
private:
    BitMapExpressionWithDateMultiAnalyzer analyzer;
    std::vector<Int64> final_keys;
    std::unordered_set<Int64> keys_without_date;
public:
    AggregateFunctionBitMapMultiCountWithDate(const DataTypes & argument_types_, std::vector<String> expression_)
            : IAggregateFunctionDataHelper<AggregateFunctionBitMapMultiCountData<String>, AggregateFunctionBitMapMultiCountWithDate>(argument_types_, {}), analyzer(expression_)
    {
        final_keys = analyzer.final_keys;
        keys_without_date = analyzer.keys_without_date;
    }

    String getName() const override { return "bitmapMultiCountWithDate"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_date = dynamic_cast<const ColumnVector<Int64> &>(*columns[0]);
        String date = std::to_string(column_date.getElement(row_num));

        const auto & column_key = dynamic_cast<const ColumnVector<Int64 > &>(*columns[1]);
        String key = std::to_string(column_key.getElement(row_num));

        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[2]);

        auto & bitmap_data_map = this->data(place).bitmap_data.bitmap_map;

        String date_with_key = date + "_" + key;
        auto it = bitmap_data_map.find(date_with_key);
        if (it == bitmap_data_map.end()) {
            bitmap_data_map.emplace(std::make_pair(date_with_key, column_bitmap.getBitMapAtImpl(row_num)));
        } else {
            it->second |= column_bitmap.getBitMapAtImpl(row_num);
        }

        if (keys_without_date.find(column_key.getElement(row_num)) != keys_without_date.end())
            bitmap_data_map.emplace(std::make_pair(key, column_bitmap.getBitMapAtImpl(row_num)));
    }

    bool mergeSingleStream(AggregateFunctionBitMapMultiCountData<String> & bitmap_count_data) const
    {
        auto & bitmap_data = bitmap_count_data.bitmap_data;
        auto & count_vector = bitmap_count_data.count_vector;

        if (bitmap_data.is_finished)
            return false;
        count_vector.clear();
        for (size_t i = 0; i < final_keys.size(); i++)
        {
            analyzer.executeExpression(bitmap_data, i);
            count_vector.push_back(bitmap_data.getCardinality(std::to_string(final_keys[i])));
        }
        bitmap_data.is_finished = true;

        return true;
    }

    void mergeTwoStreams(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs) const
    {
        auto & lhs_bitmap_data = this->data(place).bitmap_data;
        const auto & rhs_bitmap_data = this->data(rhs).bitmap_data;
        for (auto& final_key: final_keys)
        {
            auto lhs_bitmap_it = lhs_bitmap_data.bitmap_map.find(std::to_string(final_key));
            auto rhs_bitmap_it = rhs_bitmap_data.bitmap_map.find(std::to_string(final_key));
            bool lhs_bitmap_exists = lhs_bitmap_it != lhs_bitmap_data.bitmap_map.end();
            bool rhs_bitmap_exists = rhs_bitmap_it != rhs_bitmap_data.bitmap_map.end();
            if (lhs_bitmap_exists && rhs_bitmap_exists)
            {
                lhs_bitmap_it->second |= rhs_bitmap_it->second;
            }
            else
                throw Exception("Cannot find final key when merge two streams in " + getName(), ErrorCodes::LOGICAL_ERROR);
        }
    }

    /*
    * The merge function is based on the fact different stream can caculate its result independently.
    * If we use BITMAPEXECUTE, ParallelBitMapBlockInputStream will produce many streams and invoke this merge
    * function for each stream. And only the ParallelBitMapBlockInputStream can invoke merge with only one stream, so
    * we can caculate expression for this single stream directly and set is_final as true.
    * When there are two streams, it may be invoked by ParallelBitMapBlockInputStream or normal aggregating stream. But,
    * if is_finished is true for both two streams, we can ensure these two streams can caculate directly.
    * Otherwise, handle them as normal aggregate functions, that is, use bitmap or to get all bitmap and caculate expression
    * in insertResultInto functions
    *
    * For distributed queries, we also have two merge model:
    * 1. distributed_perfect_shard: each node performs the merge as single node, the final result is merged by sum or bitmapOr function.
    * 2. distributed table: each node performs the aggregation at local and return a intermediate status, then at the coordinator node, it performs a
    * second aggregation. There exists the case the second aggregation has multiple merges - the first merge has single stream, and then merge the following
    * streams.
    *
    * The second case will obstruct the determine of BITMAPEXECUTE model, since for a merge with only one stream, we cannot distinguish whether it is a BITMAPEXECUTE model
    * or distributed table. The only thing we can do is assuming this stream can be used to execute expression.
    * This assumption is correct as follows.
    * 1. If it is a distributed table and it has only one stream, execute expression in advanced will not affect the correctness of the result.
    *    If it has more than one stream, we lost some performance to execute first stream's expression. The first stream will merge other streams again and set is_finished as false.
    *    Then caculate the result in the insertResultInto function.
    * 2. If it is a BITMAPEXECUTE model, each stream will be passed to merge function independently in ParallelBitMapInputStream so that each stream will set is_finished as true.
    *
    */
    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs, Arena *) const override
    {
        auto & lhs_data = this->data(place);
        auto & rhs_data = const_cast<AggregateFunctionBitMapMultiCountData<String> &>(this->data(rhs));

        if (lhs_data.bitmap_data.empty())
        {
            mergeSingleStream(rhs_data);
            lhs_data.merge(rhs_data);
        }
        else if (lhs_data.bitmap_data.is_finished && rhs_data.bitmap_data.is_finished)
        {
            for (size_t i = 0; i < final_keys.size(); i++)
                lhs_data.count_vector[i] += rhs_data.count_vector[i];
        }
            // If two stream are both false on is_finished, it must not a BITMAPEXECUTE model, so merge them and delay expression execution on insertResultInto
        else if (!lhs_data.bitmap_data.is_finished && !rhs_data.bitmap_data.is_finished)
        {
            lhs_data.merge(rhs_data);
        }
            // If one of stream's is_finished is false, it means
            // 1. it is a BITMAPEXECUTE model but not a distributed_perfect_shard model.
            // 2. it may be execution on distributed table
            // So it is better to merge them to guarantee the correctness of result
        else
        {
            lhs_data.merge(rhs_data);
        }
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    // If is_finished is false, aggregate functions need to caculate expression.
    // It means the aggregate function is a normal aggregate (not a BITMAPEXECUTE), so the final
    // result is computed in insertResultInto
    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & local_data = const_cast<AggregateFunctionBitMapMultiCountData<String> &>(this->data(place));

        if (!local_data.bitmap_data.is_finished)
        {
            mergeSingleStream(local_data);
        }
        Array res;
        for (auto count: local_data.count_vector)
            res.push_back(count);
        static_cast<ColumnArray &>(to).insert(res);
    }

};

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
class AggregateFunctionBitMapExtract final : public IAggregateFunctionDataHelper<AggregateFunctionBitMapExtractData<T>, AggregateFunctionBitMapExtract<T>>
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<T>>;
private:
    BitMapExpressionAnalyzer<T> analyzer;
    T final_key;
    UInt64 is_bitmap_execute = 0;
public:
    AggregateFunctionBitMapExtract(const DataTypes & argument_types_, String expression_, UInt64 is_bitmap_execute_)
    : IAggregateFunctionDataHelper<AggregateFunctionBitMapExtractData<T>, AggregateFunctionBitMapExtract<T>>(argument_types_, {})
    , analyzer(expression_), is_bitmap_execute(is_bitmap_execute_)
    {
        final_key = analyzer.final_key;
    }

    String getName() const override { return "bitmapExtract"; }
    bool allocatesMemoryInArena() const override { return false; }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeBitMap64>();
    }

    void add(AggregateDataPtr __restrict place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        const auto & column_key = static_cast<const ColumnVector<T> &>(*columns[0]);
        T key = column_key.getElement(row_num);

        // use's key in this range may get the intermediate cached results
        if (key >= final_key && key < 0)
            throw Exception("The tag (or bitmap key): " + std::to_string(key) + " affects the computation, " +
                "please change another number, maybe positive number is better", ErrorCodes::LOGICAL_ERROR);

        const auto & column_bitmap = static_cast<const ColumnBitMap64 &>(*columns[1]);

        const BitMap64 & bitmap = column_bitmap.getBitMapAt(row_num);

        auto & bitmap_data = this->data(place).bitmap_data;

        bitmap_data.add(key, bitmap);
    }

    bool mergeSingleStream(AggregateFunctionBitMapExtractData<T> & bitmap_extract_data) const
    {
        auto & bitmap_data = bitmap_extract_data.bitmap_data;

        if (bitmap_data.is_finished)
            return false;

        analyzer.executeExpression(bitmap_data);
        bitmap_data.is_finished = true;

        return true;
    }

    void mergeTwoStreams(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs) const
    {
        auto & lhs_bitmap_data = this->data(place).bitmap_data;
        const auto & rhs_bitmap_data = this->data(rhs).bitmap_data;

        auto lhs_bitmap_it = lhs_bitmap_data.bitmap_map.find(final_key);
        auto rhs_bitmap_it = rhs_bitmap_data.bitmap_map.find(final_key);
        bool lhs_bitmap_exists = lhs_bitmap_it != lhs_bitmap_data.bitmap_map.end();
        bool rhs_bitmap_exists = rhs_bitmap_it != rhs_bitmap_data.bitmap_map.end();

        if (lhs_bitmap_exists && rhs_bitmap_exists)
        {
            lhs_bitmap_it->second |= rhs_bitmap_it->second;
        }
        else
            throw Exception("Cannot find final key when merge two streams in " + getName(), ErrorCodes::LOGICAL_ERROR);
    }

    /*
    * The merge function is based on the fact different stream can caculate its result independently.
    * If we use BITMAPEXECUTE, ParallelBitMapBlockInputStream will produce many streams and invoke this merge
    * function for each stream. And only the ParallelBitMapBlockInputStream can invoke merge with only one stream, so
    * we can caculate expression for this single stream directly and set is_final as true.
    * When there are two streams, it may be invoked by ParallelBitMapBlockInputStream or normal aggregating stream. But,
    * if is_finished is true for both two streams, we can ensure these two streams can be merged by mergeTwoStreams.
    * Otherwise, handle them as normal aggregate functions.
    *
    * For distributed queries, we also have two merge model:
    * 1. distributed_perfect_shard: each node performs the merge as single node, the final result is merged by sum or bitmapOr function.
    * 2. distributed table: each node performs the aggregation at local and return a intermediate status, then at the coordinator node, it performs a
    * second aggregation. There exists the case the second aggregation has multiple merges - the first merge has single stream, and then merge the following
    * streams.
    *
    * The second case will obstruct the determine of BITMAPEXECUTE model, since for a merge with only one stream, we cannot distinguish whether it is a BITMAPEXECUTE model
    * or distributed table. The only thing we can do is assuming this stream can be used to execute expression.
    * This assumption is correct as follows.
    * 1. If it is a distributed table and it has only one stream, execute expression in advanced will not affect the correctness of the result.
    *    If it has more than one stream, we lost some performance to execute first stream's expression. The first stream will merge other streams again and set is_finished as false.
    *    Then caculate the result in the insertResultInto function.
    * 2. If it is a BITMAPEXECUTE model, each stream will be passed to merge function independently in ParallelBitMapInputStream so that each stream will set is_finished as true.
    *
    */
    void merge(AggregateDataPtr __restrict place, ConstAggregateDataPtr __restrict rhs, Arena *) const override
    {
        auto & lhs_data = this->data(place);
        auto & rhs_data = const_cast<AggregateFunctionBitMapExtractData<T> &>(this->data(rhs));

        if (is_bitmap_execute && lhs_data.bitmap_data.empty())
        {
            mergeSingleStream(rhs_data);
            lhs_data.merge(rhs_data);
        }
        else if (lhs_data.bitmap_data.is_finished && rhs_data.bitmap_data.is_finished)
        {
            mergeTwoStreams(place, rhs);
        }
        else if (!lhs_data.bitmap_data.is_finished && !rhs_data.bitmap_data.is_finished)
        {
            lhs_data.merge(rhs_data);
        }
        // If one of stream's is_finished is false, it means
        // 1. it is a BITMAPEXECUTE model but not a distributed_perfect_shard model.
        // 2. it may be execution on distributed table
        // So it is better to merge them to guarantee the correctness of result
        else
        {
            lhs_data.merge(rhs_data);
        }
    }

    void serialize(ConstAggregateDataPtr __restrict place, WriteBuffer & buf) const override
    {
        this->data(const_cast<AggregateDataPtr>(place)).serialize(buf);
    }

    void deserialize(AggregateDataPtr __restrict place, ReadBuffer & buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    // If is_finished is false, aggregate functions need to caculate expression.
    // It means the aggregate function is a normal aggregate (not a BITMAPEXECUTE), so the final
    // result is computed in insertResultInto
    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & local_data = const_cast<AggregateFunctionBitMapExtractData<T> &>(this->data(place));

        if (!local_data.bitmap_data.is_finished)
        {
            mergeSingleStream(local_data);
        }

        auto it = local_data.bitmap_data.bitmap_map.find(final_key);
        BitMap64 bitmap;
        if (it != local_data.bitmap_data.bitmap_map.end())
            bitmap = it->second;

        static_cast<ColumnBitMap64 &>(to).insert(std::move(bitmap));
    }

};

}
