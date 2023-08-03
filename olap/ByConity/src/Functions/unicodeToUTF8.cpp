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

#include <optional>
#include <Common/StringUtils/StringUtils.h>
#include <common/types.h>
#include <Columns/IColumn.h>
#include <Core/ColumnsWithTypeAndName.h>
#include <DataTypes/DataTypeString.h>
#include <Functions/FunctionFactory.h>
#include <Functions/IFunction.h>
#include <Interpreters/Context.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int ILLEGAL_COLUMN;
    extern const int BAD_ARGUMENTS;
}


template <typename Name, bool parse_all>
class FunctionUnicodeToUTF8: public IFunction
{
public:
    static constexpr auto name = Name::name;

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionUnicodeToUTF8>(); }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 1; }

    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() != 1)
            throw Exception("Number of arguments for function " + getName() + " doesn't match: passed " + toString(arguments.size()) + ", should be 1",
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!isString(arguments[0].type))
            throw Exception("First argument for function " + getName() + " must be String", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeString>();
    }

    /** if parse_all is false, we only try the prefix of the string that can be parsed into UTF-8.
     *  That is say, if we met an error when parsing, we will stop the parse work and append the left to the results,
     *  for example:
     *     select unicodeToUTF8('\\u4e2d\\u6587') => '中文'
     *     select unicodeToUTF8('\\u4e2d\\u6587test') => '中文test'
     *     select unicodeToUTF8('test\\u4e2d\\u6587') => 'test\\u4e2d\\u6587'
     *  If parse_all is true, we will skip the chars which can't be parsed, and try to parse all part of the string.
     *  for example:
     *     select unicodeToUTF8All('test\\u4e2d\\u6587') => 'test中文'
     */

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        auto res = result_type->createColumn();

        for (size_t i = 0; i < input_rows_count; ++i)
        {
            StringRef unicode_str = arguments[0].column->getDataAt(i);
            const char * curr = unicode_str.data;
            const char * end = unicode_str.data + unicode_str.size;

            String utf8_str;

            while (curr != end)
            {
                if (auto value = unicodeToUTF8(curr, end))
                {
                    utf8_str += *value;
                    curr += 6;
                }
                else
                {
                    if constexpr (parse_all)
                    {
                        utf8_str += *curr;
                        ++curr;
                    }
                    else
                    {
                        utf8_str += String(curr, end - curr);
                        break;
                    }
                }
            }

            res->insertData(utf8_str.data(), utf8_str.size());
        }

        return res;
    }

private:
    static inline std::optional<String> unicodeToUTF8(const char * curr, const char * end)
    {
        if (end - curr < 6)
            return std::nullopt;

        if (!startsWith(curr, "\\u"))
            return std::nullopt;

        /// skip "\\u"
        curr += 2;

        UInt16 u_num = 0;

        for(size_t i = 0; i < 4; ++i)
        {
            u_num <<= 4;  /// u_num * 16

            char c = *curr;
            if (c >= '0' && c <= '9')
                u_num += c - '0';
            else if (c >= 'A' && c <= 'F')
                u_num += c - 'A' + 10;
            else if (c >= 'a' && c <= 'f')
                u_num += c - 'a' + 10;
            else
                return std::nullopt;

            ++curr;
        }

        return String{char(0xE0 | (u_num >> 12)), char(0x80 | ((u_num >> 6) & 0x3F)), char(0x80 | (u_num & 0x3F))};
    }
};

struct UnicodeToUTF8Name
{
    static constexpr auto name = "unicodeToUTF8";
};

struct UnicodeToUTF8AllName
{
    static constexpr auto name = "unicodeToUTF8All";
};

void registerFunctionUnicodeToUTF8(FunctionFactory & factory)
{
    factory.registerFunction<FunctionUnicodeToUTF8<UnicodeToUTF8Name, false>>(FunctionFactory::CaseInsensitive);
    factory.registerFunction<FunctionUnicodeToUTF8<UnicodeToUTF8AllName, true>>(FunctionFactory::CaseInsensitive);
}

}
