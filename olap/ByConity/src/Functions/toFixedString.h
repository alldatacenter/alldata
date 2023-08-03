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
#include <Functions/IFunction.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeFixedString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnFixedString.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnNullable.h>
#include <IO/WriteHelpers.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_COLUMN;
    extern const int TOO_LARGE_STRING_SIZE;
    extern const int NOT_IMPLEMENTED;
}

enum class ConvertToFixedStringExceptionMode
{
    Throw,
    Zero,
    Null,
    FixedStringNull
};

/** Conversion to fixed string is implemented only for strings.
 * The function toFixedStringOrNull and toFixedStringOrZero is a special case,
 * when the source string is too long, we choose to truncate instead of filling the default value
  */
template<typename Name, ConvertToFixedStringExceptionMode exception_mode>
class FunctionToFixedStringImpl : public IFunction
{
public:
    static constexpr auto name = Name::name;
    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionToFixedStringImpl>(); }
    static FunctionPtr create() { return std::make_shared<FunctionToFixedStringImpl>(); }

    String getName() const override
    {
        return name;
    }

    size_t getNumberOfArguments() const override { return 2; }
    bool isInjective(const ColumnsWithTypeAndName &) const override { return true; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (!isUnsignedInteger(arguments[1].type))
            throw Exception("Second argument for function " + getName() + " must be unsigned integer", ErrorCodes::ILLEGAL_COLUMN);
        if (!arguments[1].column)
            throw Exception("Second argument for function " + getName() + " must be constant", ErrorCodes::ILLEGAL_COLUMN);
        if (!isStringOrFixedString(arguments[0].type))
            throw Exception(getName() + " is only implemented for types String and FixedString", ErrorCodes::NOT_IMPLEMENTED);

        const size_t n = arguments[1].column->getUInt(0);
        return std::make_shared<DataTypeFixedString>(n);
    }

    bool useDefaultImplementationForConstants() const override { return true; }
    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1}; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t /*input_rows_count*/) const override
    {
        const auto n = arguments[1].column->getUInt(0);
        return executeForN<exception_mode>(arguments, n);
    }

    template<ConvertToFixedStringExceptionMode exception_mode_for_n>
    static ColumnPtr executeForN(const ColumnsWithTypeAndName & arguments, const size_t n)
    {
        const auto & column = arguments[0].column;

        ColumnUInt8::MutablePtr col_null_map_to;
        ColumnUInt8::Container * vec_null_map_to [[maybe_unused]] = nullptr;
        if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Null)
        {
            col_null_map_to = ColumnUInt8::create(column->size(), false);
            vec_null_map_to = &col_null_map_to->getData();
        }

        if (const auto * column_string = checkAndGetColumn<ColumnString>(column.get()))
        {
            auto column_fixed = ColumnFixedString::create(n);

            auto & out_chars = column_fixed->getChars();
            const auto & in_chars = column_string->getChars();
            const auto & in_offsets = column_string->getOffsets();

            out_chars.resize_fill(in_offsets.size() * n);

            for (size_t i = 0; i < in_offsets.size(); ++i)
            {
                const size_t off = i ? in_offsets[i - 1] : 0;
                const size_t len = in_offsets[i] - off - 1;
                if (len > n)
                {
                    if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Throw)
                    {
                        throw Exception("String too long for type FixedString(" + toString(n) + ")",
                            ErrorCodes::TOO_LARGE_STRING_SIZE);
                    }
                    else if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Null)
                    {
                        (*vec_null_map_to)[i] = true;
                        continue;
                    }
                }
                memcpy(&out_chars[i * n], &in_chars[off], len);
            }

            if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Null)
                return ColumnNullable::create(std::move(column_fixed), std::move(col_null_map_to));
            else
                return column_fixed;
        }
        else if (const auto * column_fixed_string = checkAndGetColumn<ColumnFixedString>(column.get()))
        {
            const auto src_n = column_fixed_string->getN();
            if (src_n > n)
            {
                if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Throw)
                {
                    throw Exception{"String too long for type FixedString(" + toString(n) + ")", ErrorCodes::TOO_LARGE_STRING_SIZE};
                }
                else if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Null)
                {
                    auto column_fixed = ColumnFixedString::create(n);
                    std::fill(vec_null_map_to->begin(), vec_null_map_to->end(), true);
                    return ColumnNullable::create(column_fixed->cloneResized(column->size()), std::move(col_null_map_to));
                }
            }

            auto column_fixed = ColumnFixedString::create(n);

            auto & out_chars = column_fixed->getChars();
            const auto & in_chars = column_fixed_string->getChars();
            const auto size = column_fixed_string->size();
            out_chars.resize_fill(size * n);

            for (size_t i = 0; i < size; ++i)
                memcpy(&out_chars[i * n], &in_chars[i * src_n], src_n);

            return column_fixed;
        }
        else
        {
            if constexpr (exception_mode_for_n == ConvertToFixedStringExceptionMode::Null)
            {
                auto column_fixed = ColumnFixedString::create(n);
                std::fill(vec_null_map_to->begin(), vec_null_map_to->end(), true);
                return ColumnNullable::create(column_fixed->cloneResized(column->size()), std::move(col_null_map_to));
            }
            else
                throw Exception("Unexpected column: " + column->getName(), ErrorCodes::ILLEGAL_COLUMN);
        }
    }
};

struct NameToFixedString { static constexpr auto name = "toFixedString"; };
using FunctionToFixedString = FunctionToFixedStringImpl<NameToFixedString, ConvertToFixedStringExceptionMode::Throw>;

}

