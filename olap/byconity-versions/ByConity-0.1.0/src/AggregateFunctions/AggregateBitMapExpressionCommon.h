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

#include <array>
#include <stack>

#include <DataTypes/DataTypeBitMap64.h>

#include <Common/ArenaAllocator.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> || std::is_same_v<T, String> > >
struct AggregateFunctionBitMapData
{
    std::unordered_map<T, BitMap64> bitmap_map;
    bool is_finished = false;
    AggregateFunctionBitMapData() = default;

    void add(const T key, const BitMap64 & bitmap)
    {
        auto it = bitmap_map.find(key);
        if (it == bitmap_map.end()) {
            bitmap_map.emplace(key, bitmap);
        } else {
            it->second |= bitmap;
        }
    }

    void merge(AggregateFunctionBitMapData && rhs)
    {
        std::unordered_map<T, BitMap64> & rhs_map = rhs.bitmap_map;
        for (auto it = rhs_map.begin(); it != rhs_map.end(); ++it)
        {
            auto jt = bitmap_map.find(it->first);
            if (jt == bitmap_map.end()) {
                bitmap_map.emplace(it->first, it->second);
            }
            else {
                jt->second |= it->second;
            }
        }
        is_finished = false;
    }

    bool empty() { return bitmap_map.empty(); }

    UInt64 getCardinality(const T key)
    {
        auto it = bitmap_map.find(key);
        if (it != bitmap_map.end())
            return it->second.cardinality();
        return 0;
    }

    void serialize(WriteBuffer & buf) const
    {
        size_t key_size = bitmap_map.size();
        writeVarUInt(key_size, buf);

        for (auto it = bitmap_map.begin(); it != bitmap_map.end(); ++it)
        {
            writeBinary(it->first, buf);
            size_t bytes_size = it->second.getSizeInBytes();
            writeVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            it->second.write(buffer.data());
            writeString(buffer.data(), bytes_size, buf);
        }

        writeVarUInt(is_finished, buf);
    }

    void deserialize(ReadBuffer & buf)
    {
        size_t key_size;
        readVarUInt(key_size, buf);
        for (size_t i = 0; i < key_size; ++i)
        {
            T key;
            readBinary(key, buf);
            size_t bytes_size;
            readVarUInt(bytes_size, buf);
            PODArray<char> buffer(bytes_size);
            buf.readStrict(buffer.data(), bytes_size);
            BitMap64 bitmap = BitMap64::readSafe(buffer.data(), bytes_size);
            bitmap_map.emplace(key, std::move(bitmap));
        }

        readVarUInt(is_finished, buf);
    }
};

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> || std::is_same_v<T, String> > >
struct BitMapExpressionNode
{
    String left;
    String op;
    String right;
    T res;
    bool replicated = false;
    BitMapExpressionNode(const String & left_, const String & op_, const String & right_, const T res_, bool replicated_ = false)
    : left(left_), op(op_), right(right_), res(res_), replicated(replicated_) {}

    BitMapExpressionNode(String && left_, String && op_, String && right_, const T res_, bool replicated_ = false)
    : left(std::move(left_)), op(std::move(op_)), right(std::move(right_)), res(res_), replicated(replicated_) {}

    String toString()
    {
        std::ostringstream oss;
        oss << left << " " << op << " " << right << " = " << res << " REPLICATED: " << replicated << "\n";
        return oss.str();
    }
};

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> > >
struct BitMapExpressionAnalyzer
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<T>>;
    String original_expression;
    T final_key;
    BitMapExpressions expression_actions;
    std::unordered_map<String, size_t> replicated_keys;
    bool only_or = true;
    NameSet or_expressions;

    BitMapExpressionAnalyzer(const String & expression)
    : original_expression(expression)
    {
        analyze();
    }
    BitMapExpressionAnalyzer() = default;

    void subExpression(std::stack<String> & expression_stack, T & global_index, String & right)
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
                throw Exception("Invalid expression " + operation + " for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
            String left = expression_stack.top();
            expression_stack.pop();
            // Optimize the case which right is equal to left.
            // If the operation is "~", add a no-exists result to expression_stack so that we can get an empty bitmap
            if (right == left && operation == "~")
            {
                T res = global_index--;
                right = toString(res);
            }
            else if (right != left)
            {
                T res = global_index--;
                bool replicated = false;
                auto left_it = replicated_keys.find(left);
                auto right_it = replicated_keys.find(right);
                if (left_it != replicated_keys.end())
                {
                    if (left_it->second > 1)
                        replicated = true;
                }
                if (right_it != replicated_keys.end())
                {
                    if (right_it->second > 1)
                        replicated = true;
                }
                expression_actions.emplace_back(std::move(left), std::move(operation), std::move(right), res, replicated);
                right = toString(res);
            }
        }
    }

    void analyze()
    {
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
                    replicated_keys[number] += 1;
                    or_expressions.insert(number);
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

        if (only_or)
        {
            final_key = -1;
            return;
        }

        T global_index = -1;

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
                subExpression(expression_stack, global_index, number);
                expression_stack.push(number);
            }
            else
            {
                String right = expression_vector[i];
                // If there are replicated number, we cannot use some optimization strategy to execute expression
                subExpression(expression_stack, global_index, right);
                expression_stack.push(right);
            }
        }

        if (expression_stack.size() == 1) {
            const String & res = expression_stack.top();
            std::istringstream iss(res);
            iss >> final_key;
        } else {
            throw Exception("Invalid expression for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
        }
    }

    void executeExpressionImpl(String left, String operation, String right, T res, bool replicated, const AggregateFunctionBitMapData<T>& data) const
    {
        std::istringstream iss(left);
        T left_key;
        iss >> left_key;
        std::istringstream iss2(right);
        T right_key;
        iss2 >> right_key;

        auto& bitmap_map = const_cast<std::unordered_map<T, BitMap64>&>(data.bitmap_map);
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

    void executeExpressionOnlyOr(const AggregateFunctionBitMapData<T> & data) const
    {
        std::set<T> key_set;
        for (const auto & expression : or_expressions)
        {
            std::istringstream iss(expression);
            T key;
            iss >> key;
            key_set.insert(key);
        }

        auto& bitmap_map = const_cast<std::unordered_map<T, BitMap64>&>(data.bitmap_map);

        if (key_set.size() == 1)
        {
            T key = *key_set.begin();
            auto it = bitmap_map.find(key);
            if (it == bitmap_map.end()) {
                BitMap64 temp_bitmap;
                auto res_pair = bitmap_map.emplace(key, std::move(temp_bitmap));
                if (res_pair.second)
                    it = res_pair.first;
                else
                    throw Exception("Existing empty BitMap64 when inserting empty BitMap64", ErrorCodes::LOGICAL_ERROR);
            }
            auto it_final_item = bitmap_map.extract(it->first);
            it_final_item.key() = final_key;
            bitmap_map.insert(std::move(it_final_item));
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
            Roaring result = Roaring::fastunion(it->second.size(), static_cast<roaring::Roaring **>(&(*(it->second.begin()))));
            const_cast<std::map<UInt32, Roaring> &>(res_roaring.getRoarings()).emplace(it->first, std::move(result));
        }

        bitmap_map[final_key] = std::move(res_roaring);
    }

    void executeExpression(const AggregateFunctionBitMapData<T> & data) const
    {
        if (only_or)
        {
            executeExpressionOnlyOr(data);
        }
        else
        {
            for (const auto & action : expression_actions)
            {
                executeExpressionImpl(action.left, action.op, action.right, action.res, action.replicated, data);
            }
        }
    }
};

template<typename T, typename = std::enable_if_t< std::is_integral_v<T> || std::is_same_v<T, String> > >
struct BitMapExpressionMultiAnalyzer
{
    using BitMapExpressions = std::vector<BitMapExpressionNode<T>>;
    std::vector<String> original_expressions;
    T global_index = -1;
    std::vector<T> final_keys;
    std::vector<bool> expression_only_ors;
    std::vector<BitMapExpressions> expression_actions_vector;
    std::unordered_map<String, size_t> replicated_keys;
    std::vector<NameSet> or_expressions;

    BitMapExpressionMultiAnalyzer(const std::vector<String> & expressions)
            : original_expressions(expressions)
    {
        analyze();
    }

    BitMapExpressionMultiAnalyzer() = default;

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
                T res = global_index--;
                right = toString(res);
            }
            else if (right != left)
            {
                T res = global_index--;

                expression_actions_vector[index].emplace_back(std::move(left), std::move(operation), std::move(right), res, false);
                right = toString(res);
            }
        }
    }

    void analyze()
    {
        for (size_t i = 0; i < original_expressions.size(); i++)
            analyzeExpression(original_expressions[i], i);

        for (const auto only_or : expression_only_ors)
        {
            if (only_or)
            {
                for (const NameSet& or_expression: or_expressions)
                {
                    for (const String& or_expression_item: or_expression)
                    {
                        replicated_keys[or_expression_item] += 1;
                    }
                }

            }
            else
            {
                for (auto & expression_actions: expression_actions_vector)
                {
                    for (BitMapExpressionNode<T> & expression_action: expression_actions)
                    {
                        replicated_keys[expression_action.left] += 1;
                        replicated_keys[expression_action.right] += 1;
                    }
                }
            }
        }

        for (auto & expression_actions: expression_actions_vector)
        {
            for (BitMapExpressionNode<T> & expression_action: expression_actions)
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
                        number = toString(final_keys[res_index - 1]);
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
            T temp_final_key;
            const String & res = expression_stack.top();
            std::istringstream iss(res);
            iss >> temp_final_key;
            final_keys.emplace_back(temp_final_key);
        } else {
            throw Exception("Invalid expression for BitMap: " + original_expression, ErrorCodes::LOGICAL_ERROR);
        }
    }

    void executeExpressionImpl(String left, String operation, String right, T res, bool replicated, const AggregateFunctionBitMapData<T>& data) const
    {
        std::istringstream iss(left);
        T left_key;
        iss >> left_key;
        std::istringstream iss2(right);
        T right_key;
        iss2 >> right_key;

        auto& bitmap_map = const_cast<std::unordered_map<T, BitMap64>&>(data.bitmap_map);
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

    void executeExpressionOnlyOr(const AggregateFunctionBitMapData<T> & data, size_t index) const
    {
        std::set<T> key_set;
        for (const auto & expression : or_expressions[index])
        {
            std::istringstream iss(expression);
            T key;
            iss >> key;
            if (toString(key) != expression)
                throw Exception("expression is not fully parsed! parsed: " + toString(key) + ", but your input: " + expression
                    + ", please check function name and expression type", ErrorCodes::LOGICAL_ERROR);
            key_set.insert(key);
        }

        auto& bitmap_map = const_cast<std::unordered_map<T, BitMap64>&>(data.bitmap_map);

        if (key_set.size() == 1)
        {
            T key = *key_set.begin();
            auto it = bitmap_map.find(key);
            if (it == bitmap_map.end()) {
                BitMap64 temp_bitmap;
                auto res_pair = bitmap_map.emplace(key, std::move(temp_bitmap));
                if (res_pair.second)
                    it = res_pair.first;
                else
                    throw Exception("Existing empty BitMap64 when inserting empty BitMap64", ErrorCodes::LOGICAL_ERROR);
            }
            auto or_it = replicated_keys.find(toString(key));
            if (or_it == replicated_keys.end())
                return;
            else if (or_it->second > 1)
            {
                bitmap_map[final_keys[index]] = it->second;
            }
            else
            {
                auto it_final_item = bitmap_map.extract(it->first);
                it_final_item.key() = final_keys[index];
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
            Roaring result = Roaring::fastunion(it->second.size(), static_cast<roaring::Roaring **>(&(*(it->second.begin()))));
            const_cast<std::map<UInt32, Roaring> &>(res_roaring.getRoarings()).emplace(it->first, std::move(result));
        }

        bitmap_map[final_keys[index]] = std::move(res_roaring);
    }

    void executeExpression(const AggregateFunctionBitMapData<T> & data, size_t index) const
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

}
