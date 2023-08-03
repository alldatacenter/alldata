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

#include <Common/Exception.h>
#include <Functions/IFunction.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionsConversion.h>
#include <Functions/formatDateTime.cpp>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int UNSUPPORTED_PARAMETER;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}


class FunctionDateFormat : public FunctionFormatDateTime
{
    static constexpr std::string_view PATTERN_CHARS = "GyMdkHmsSEDFwWahKzZYuXL";
    static constexpr int TAG_ASCII_CHAR = 100;

    enum PATTERN {
        ERA = 0,                    // G
        YEAR,                       // y
        MONTH,                      // M
        DAY_OF_MONTH,               // d
        HOUR_OF_DAY1,               // k
        HOUR_OF_DAY0,               // H
        MINUTE,                     // m
        SECOND,                     // s
        MILLISECOND,                // S
        DAY_OF_WEEK,                // E
        DAY_OF_YEAR,                // D
        DAY_OF_WEEK_IN_MONTH,       // F
        WEEK_OF_YEAR,               // w
        WEEK_OF_MONTH,              // W
        AM_PM,                      // a
        HOUR1,                      // h
        HOUR0,                      // K
        ZONE_NAME,                  // z
        ZONE_VALUE,                 // Z
        WEEK_YEAR,                  // Y
        ISO_DAY_OF_WEEK,            // u
        ISO_ZONE,                   // X
        MONTH_STANDALONE            // L
    };

    void compilePattern(String & pattern, String & compiled_code) const
    {
        auto encode = [](int tag, int length, String & buffer)
        {
            if (length < 255)
            {
                buffer += static_cast<char>(tag);
                buffer += static_cast<char>(length);
            }
            else
                throw Exception("Illegal date format pattern. ", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        };

        size_t length = pattern.size();
        int count = 0;
        int last_tag = -1;

        for (size_t i = 0; i < length; i++)
        {
            char c = pattern[i];

            if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z'))
            {
                if (count != 0)
                {
                    encode(last_tag, count, compiled_code);
                    last_tag = -1;
                    count = 0;
                }

                size_t j;
                for (j = i + 1; j < length; j++)
                {
                    char d = pattern[j];
                    if ((d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z'))
                        break;
                }

                encode(TAG_ASCII_CHAR, j - i, compiled_code);

                for (; i < j; i++)
                {
                    compiled_code += (pattern[i]);
                }
                i--;

                continue;
            }

            auto found = PATTERN_CHARS.find(c);
            if (found == String::npos)
                throw Exception(String("Unknown pattern character '") + c + "'.", ErrorCodes::UNSUPPORTED_PARAMETER);

            int tag = found;

            if (last_tag == -1 || last_tag == tag)
            {
                last_tag = tag;
                count++;
                continue;
            }

            encode(last_tag, count, compiled_code);
            last_tag = tag;
            count = 1;
        }

        if (count != 0)
            encode(last_tag, count, compiled_code);
    }

    template <typename T>
    String parsePattern(String & pattern, std::vector<FunctionFormatDateTime::Action<T>> & instructions) const
    {
        String compiled;
        compilePattern(pattern, compiled);

        auto add_shift = [&](size_t amount)
        {
            if (instructions.empty())
                instructions.emplace_back(&FunctionFormatDateTime::Action<T>::noop);
            instructions.back().shift += amount;
        };

        auto add_instruction_or_shift = [&](typename FunctionFormatDateTime::Action<T>::Func func [[maybe_unused]], size_t shift)
        {
            if constexpr (std::is_same_v<T, UInt32>)
                instructions.emplace_back(func, shift);
            else
                add_shift(shift);
        };

        String result;

        size_t length = compiled.size();

        int tag;
        int count;

        for (size_t i = 0; i < length;)
        {
            if ((tag = compiled[i++]) == TAG_ASCII_CHAR)
            {
                count = compiled[i++];
                result.append(compiled, i, count);
                add_shift(count);
                i += count;
            }
            else
            {
                count = compiled[i++];
                switch (tag)
                {
                    case PATTERN::WEEK_YEAR:
                    case PATTERN::YEAR:
                        if (count != 2)
                        {
                            instructions.emplace_back(&FunctionFormatDateTime::Action<T>::year4, 4);
                            result.append("0000");
                        }
                        else
                        {
                            instructions.emplace_back(&FunctionFormatDateTime::Action<T>::year2, 2);
                            result.append("00");
                        }
                        break;

                    case PATTERN::MONTH:
                        instructions.emplace_back(&FunctionFormatDateTime::Action<T>::month, 2);
                        result.append("00");
                        break;

                    case PATTERN::DAY_OF_MONTH:
                        instructions.emplace_back(&FunctionFormatDateTime::Action<T>::dayOfMonth, 2);
                        result.append("00");
                        break;

                    case PATTERN::HOUR_OF_DAY0:
                        add_instruction_or_shift(&FunctionFormatDateTime::Action<T>::hour24, 2);
                        result.append("00");
                        break;

                    case PATTERN::HOUR0:
                        add_instruction_or_shift(&FunctionFormatDateTime::Action<T>::hour12, 2);
                        result.append("12");
                        break;

                    case PATTERN::MINUTE:
                        add_instruction_or_shift(&FunctionFormatDateTime::Action<T>::minute, 2);
                        result.append("00");
                        break;

                    case PATTERN::SECOND:
                        add_instruction_or_shift(&FunctionFormatDateTime::Action<T>::second, 2);
                        result.append("00");
                        break;

                    case PATTERN::DAY_OF_YEAR:
                        instructions.emplace_back(&FunctionFormatDateTime::Action<T>::dayOfYear, 3);
                        result.append("000");
                        break;

                    case PATTERN::AM_PM:
                        add_instruction_or_shift(&Action<T>::AMPM, 2);
                        result.append("AM");
                        break;

                    default:
                        throw Exception("Not supported pattern: " + std::to_string(tag), ErrorCodes::NOT_IMPLEMENTED);
                }
            }
        }

        add_shift(1);
        return result;
    }

public:
    static constexpr auto name = "date_format";

    static FunctionPtr create(const ContextPtr) { return std::make_shared<FunctionDateFormat>(); }

    explicit FunctionDateFormat() : FunctionFormatDateTime(false) {}

    String getName() const override { return name; }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1, 2}; }

    bool isVariadic() const override { return true; }
    size_t getNumberOfArguments() const override { return 0; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() != 2 && arguments.size() != 3)
            throw Exception("Number of arguments for function " + getName() + " doesn't match: passed "
                            + toString(arguments.size()) + ", should be 2 or 3",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!WhichDataType(arguments[0].type).isDateOrDateTime() && !WhichDataType(arguments[0].type).isUInt32() && !WhichDataType(arguments[0].type).isString())
            throw Exception("Illegal type " + arguments[0].type->getName() + " of 1 argument of function " + getName() +
                            ". Should be datetime or datetime format string", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (!WhichDataType(arguments[1].type).isString())
            throw Exception("Illegal type " + arguments[1].type->getName() + " of 2 argument of function " + getName() + ". Must be String.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (arguments.size() == 3)
        {
            if (!WhichDataType(arguments[2].type).isString())
                throw Exception("Illegal type " + arguments[2].type->getName() + " of 3 argument of function " + getName() + ". Must be String.",
                                ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        return std::make_shared<DataTypeString>();
    }


    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & , [[maybe_unused]] size_t input_rows_count) const override
    {
        ColumnsWithTypeAndName tmp_args{arguments[0]};
        if (arguments.size() == 3)
            tmp_args.emplace_back(arguments[2]);

        FunctionPtr convert = std::make_shared<FunctionToDateTime>();
        auto time_col = convert->executeImpl(tmp_args, std::make_shared<DataTypeDateTime>(), input_rows_count);

        if (const auto * times = checkAndGetColumn<ColumnVector<UInt32>>(time_col.get()))
        {
            const ColumnConst * pattern_column = checkAndGetColumnConst<ColumnString>(arguments[1].column.get());

            if (!pattern_column)
                throw Exception("Illegal column " + arguments[1].column->getName()
                                + " of second ('format') argument of function " + getName() + ". Must be constant string.",
                                ErrorCodes::ILLEGAL_COLUMN);

            auto pattern = pattern_column->getValue<String>();

            std::vector<FunctionFormatDateTime::Action<UInt32>> instructions;
            String pattern_to_fill = parsePattern(pattern, instructions);
            size_t result_size = pattern_to_fill.size();

            const auto & time_zone = extractTimeZoneFromFunctionArguments(arguments, 2, 0);

            const typename ColumnVector<UInt32>::Container & vec = times->getData();

            auto col_res = ColumnString::create();
            auto & dst_data = col_res->getChars();
            auto & dst_offsets = col_res->getOffsets();
            dst_data.resize(vec.size() * (result_size + 1));
            dst_offsets.resize(vec.size());

            /// Fill result with literals.
            {
                UInt8 * begin = dst_data.data();
                UInt8 * end = begin + dst_data.size();
                UInt8 * pos = begin;

                if (pos < end)
                {
                    memcpy(pos, pattern_to_fill.data(), result_size + 1);   /// With zero terminator.
                    pos += result_size + 1;
                }

                /// Fill by copying exponential growing ranges.
                while (pos < end)
                {
                    size_t bytes_to_copy = std::min(pos - begin, end - pos);
                    memcpy(pos, begin, bytes_to_copy);
                    pos += bytes_to_copy;
                }
            }

            auto *begin = reinterpret_cast<char *>(dst_data.data());
            auto *pos = begin;

            for (size_t i = 0; i < vec.size(); ++i)
            {
                for (auto & instruction : instructions)
                    instruction.perform(pos, vec[i], time_zone);

                dst_offsets[i] = pos - begin;
            }

            dst_data.resize(pos - begin);
            return col_res;
        }
        else
        {
            throw Exception("Illegal column " + arguments[0].column->getName()
                            + " of function " + getName() + ", must be Date or DateTime format", ErrorCodes::ILLEGAL_COLUMN);
        }
    }

};

void registerFunctionDateFormat(FunctionFactory & factory)
{
    factory.registerFunction<FunctionDateFormat>();
}

}
