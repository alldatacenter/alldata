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

#include <Functions/IFunction.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnFixedString.h>
#include <Common/StringUtils/StringUtils.h>
#include <Functions/FunctionHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}

enum class OpType
{
    NOT_EQUAL,
    EQUAL,
    LESS,
    GREATER,
    LESS_EQUAL,
    GREATER_EQUAL
};

using VersionType = int;

struct VersionCompareBase
{
    explicit VersionCompareBase(size_t max_length_): max_length(max_length_) {}

    std::optional<size_t> next(bool init = false)
    {
        if (idx >= max_length)
            return std::nullopt;

        if (const_flag && !init)
        {
            if (idx < const_val.size())
                return const_val[idx++];
            else
                return std::nullopt;
        }
        else
        {
            ++idx;
            return nextImpl('.');
        }
    }

    void setConst(const StringRef & value)
    {
        reset(value);

        while (auto val = next(true))
        {
            const_val.push_back(val.value());
        }

        /// reset right idx, data and data_size after call function next().
        reset(value);
        const_flag = 1;
    }

    virtual void reset(const StringRef & rhs)
    {
        idx = 0;

        if (const_flag)
            return;

        data = rhs.data;
        data_size = rhs.size;
    }

    inline bool empty() const { return !data || !data_size; }

    virtual std::optional<VersionType> nextImpl(char separator) = 0;

    virtual bool checkDot(const VersionCompareBase &) { return false; }

    virtual ~VersionCompareBase() = default;

    friend struct AppVersionCompare;

protected:
    size_t max_length;

    size_t data_size = 0;
    const char * data = nullptr;

    size_t idx = 0;
    size_t const_flag = 0;
    std::vector<VersionType> const_val;
};

struct AppVersionCompare : public VersionCompareBase
{
    explicit AppVersionCompare(size_t max_length_): VersionCompareBase(max_length_) {}

    static constexpr auto name = "AppVersionCompare";
    /**
     * example 1: "6.4 vs 6.4.0"  =>  "6.4.-2 vs 6.4.0"
     * example 2: "6.4 vs 6.4."   =>  "6.4.-2 vs 6.4.-1"
     */
    static constexpr VersionType default_value = -2;

    std::optional<VersionType> nextImpl(char separator) override
    {
        if (!data_size && end_with_dot)
        {
            end_with_dot = false;
            return -1;
        }

        if (!data || !data_size)
            return std::nullopt;

        size_t i = 0;
        VersionType res = 0;

        for (; i < data_size && data[i] != separator; ++i)
        {
            if (isNumericASCII(data[i]))
                res = res * 10 + data[i] - '0';
            else if (data[i] == 0)  /// maybe fixed string.
                break;
            else
            {
                res = -1;
                break;
            }
        }

        while (i < data_size && data[i] != separator) ++i;

        data = data + std::min(i + 1, data_size);
        data_size -= std::min(i + 1, data_size);

        return res;
    }

    bool checkDot(const VersionCompareBase & rhs) override
    {
        /// if has empty string, compare two version directly
        if (empty() || rhs.empty())
            return false;

        bool lhs_has_dot = false;
        bool rhs_has_dot = false;

        for (size_t i = 0; i < data_size && !lhs_has_dot; ++i)
            if (data[i] == '.')
                lhs_has_dot = true;

        for (size_t i = 0; i < rhs.data_size && !rhs_has_dot; ++i)
            if (rhs.data[i] == '.')
                rhs_has_dot = true;

        return lhs_has_dot ^ rhs_has_dot;
    }

    void reset(const StringRef & rhs) override
    {
        VersionCompareBase::reset(rhs);
        end_with_dot = data_size && *(data + data_size - 1) == '.';
    }

private:
    bool end_with_dot;
};

struct VersionCompare: public VersionCompareBase
{
    explicit VersionCompare(size_t max_length_): VersionCompareBase(max_length_) {}

    static constexpr auto name = "versionCompare";
    static constexpr VersionType default_value = 0;

    std::optional<VersionType> nextImpl(char separator) override
    {
        if (!data || !data_size)
            return std::nullopt;

        size_t i = 0;
        VersionType res = 0;
        for (; i < data_size && isNumericASCII(data[i]) && data[i] != separator; ++i)
        {
            res = res * 10 + data[i] - '0';
        }

        /** move data to next separator. For example,
         *  if we have data = "123abc.1223abc" and size = 14.
         *  we will return 123 and reset data = "1223abc", size = 7.
         */
        while (i < data_size && data[i] != separator) ++i;
        data = data + std::min(i + 1, data_size);  /// separator or end data.
        data_size -= std::min(i + 1, data_size);

        return res;
    }
};

template<typename T>
class FunctionVersionCompareImpl : public IFunction
{
public:
    explicit FunctionVersionCompareImpl(ContextPtr) {}
    static FunctionPtr create(ContextPtr context) { return std::make_shared<FunctionVersionCompareImpl>(context); }

    static constexpr auto name = T::name;
    String getName() const override { return name; }
    bool isVariadic() const override { return true; }
    size_t getNumberOfArguments () const override { return 0; }

    DataTypePtr getReturnTypeImpl (const DataTypes & arguments) const override
    {
        if (arguments.size() != 3 && arguments.size() != 4)
            throw Exception(ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH,
                            "Number of arguments for function {} doesn't match: passed {}, should be 3 or 4",
                            getName(), arguments.size());

        for (size_t arg_idx = 0; arg_idx < 3; ++arg_idx)
        {
            const auto * arg = arguments[arg_idx].get();

            if (!WhichDataType(arg).isStringOrFixedString())
                throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
                                "The {}-th argument of function {} must be String or Fixed String, but got {}",
                                arg_idx + 1, getName(), arg->getName());
        }

        if (arguments.size() == 4 && !isUInt8(arguments[3]))
            throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT, "The forth argument for function {} must be UInt8", getName());

        return std::make_shared<DataTypeUInt8>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t input_rows_count) const override
    {
        auto left_column = arguments[0].column;
        auto right_column = arguments[1].column;
        const auto * left_const_column = checkAndGetColumnConstStringOrFixedString(left_column.get());
        const auto * right_const_column = checkAndGetColumnConstStringOrFixedString(right_column.get());
        const auto * op_const_column = checkAndGetColumnConstStringOrFixedString(arguments[2].column.get());

        if (!op_const_column)
            throw Exception("Operation of function " + getName() + " must be a const string", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        size_t max_length = std::numeric_limits<size_t>::max();

        if (arguments.size() == 4)
        {
            const auto * column = checkAndGetColumnConst<ColumnUInt8>(arguments[3].column.get());
            if (!column)
                throw Exception("The forth argument for function " + getName() + " must be Const UInt8", ErrorCodes::LOGICAL_ERROR);
            max_length = column->getUInt(0);
        }

        using CompFunc = std::function<bool(VersionType, VersionType)>;

        static const std::map<String, CompFunc> opMap =
        {
            {"", &compareValue<OpType::EQUAL>},
            {"=", &compareValue<OpType::EQUAL>},
            {"!=", &compareValue<OpType::NOT_EQUAL>},
            {"<", &compareValue<OpType::LESS>},
            {">", &compareValue<OpType::GREATER>},
            {"<=", &compareValue<OpType::LESS_EQUAL>},
            {">=", &compareValue<OpType::GREATER_EQUAL>},
        };

        auto op = op_const_column->getValue<String>();
        auto it = opMap.find(op);
        if (it == opMap.end())
            throw Exception("Illegal operation type '" + op + "', only support one of (=, !=, <, > <=, >=)", ErrorCodes::LOGICAL_ERROR);

        const auto & op_func = it->second;

        auto left_parse_helper = std::make_shared<T>(max_length);
        auto right_parse_helper = std::make_shared<T>(max_length);

        if (left_const_column)
            left_parse_helper->setConst(left_const_column->getDataAt(0));
        if (right_const_column)
            right_parse_helper->setConst(right_const_column->getDataAt(0));

        auto res_column = ColumnUInt8::create(input_rows_count);
        auto & res_data = res_column->getData();

        for (size_t i = 0; i < input_rows_count; ++i)
        {
            left_parse_helper->reset(left_column->getDataAt(i));
            right_parse_helper->reset(right_column->getDataAt(i));

            /// For function AppVersionCompare, if one of version has dot and another has not, return false directly
            if (left_parse_helper->checkDot(*right_parse_helper))
            {
                res_data[i] = 0;
                continue;
            }

            while (true)
            {
                auto left = left_parse_helper->next();
                auto right = right_parse_helper->next();
                auto left_value = left.value_or(T::default_value);
                auto right_value = right.value_or(T::default_value);

                if (!compareValue<OpType::EQUAL>(left_value, right_value))
                {
                    res_data[i] = op_func(left_value, right_value);
                    break;
                }

                if (!left && !right)
                {
                    res_data[i] = op_func(T::default_value, T::default_value);
                    break;
                }
            }
        }

        return res_column;
    }

private:
    template <OpType op_type>
    static bool compareValue(VersionType left, VersionType right)
    {
        switch (op_type)
        {
            case OpType::EQUAL: return left == right;
            case OpType::LESS:  return left < right;
            case OpType::GREATER: return left > right;
            case OpType::NOT_EQUAL: return left != right;
            case OpType::LESS_EQUAL: return left <= right;
            case OpType::GREATER_EQUAL: return left >= right;
        }
        return true;
    }
};

template class FunctionVersionCompareImpl<VersionCompare>;
template class FunctionVersionCompareImpl<AppVersionCompare>;

using FunctionVersionCompare = FunctionVersionCompareImpl<VersionCompare>;
using FunctionAppVersionCompare = FunctionVersionCompareImpl<AppVersionCompare>;

}
