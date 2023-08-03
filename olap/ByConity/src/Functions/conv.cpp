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

#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionsConversion.h>
#include <DataTypes/IDataType.h>
#include <DataTypes/DataTypeString.h>
#include <Columns/ColumnString.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_COLUMN;
    extern const int BAD_ARGUMENTS;
}


class ExecutableFunctionConv : public IExecutableFunction
{
public:
    String getName() const override { return "conv"; }

    bool useDefaultImplementationForConstants() const override { return true; }
    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1, 2}; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t) const override
    {
        // XXX: optimize function if to_base = from_base ^ n or from_base = to_base ^ n.
        auto from_base = arguments[1].column->getInt(0);
        auto to_base = arguments[2].column->getInt(0);

        if (from_base < 2 || from_base > 36)
            throw Exception("from_base of function " + getName() + " should in [2, 36]", ErrorCodes::BAD_ARGUMENTS);
        if (to_base < 2 || to_base > 36)
            throw Exception("to_base of function " + getName() + " should in [2, 36]", ErrorCodes::BAD_ARGUMENTS);

        ColumnPtr data_column = arguments[0].column;
        MutableColumnPtr res = ColumnString::create();

        if (!isString(arguments[0].type))
        {
            data_column = ConvertImplGenericToString::execute(arguments);
        }

        for (size_t i = 0; i < data_column->size(); ++i)
        {
            res->insert(convert(data_column->getDataAt(i), from_base, to_base));
        }

        return res;
    }

private:
    static size_t getVerifiedIndex(const StringRef & str, UInt8 from_base)
    {
        if (!str.size)
            return 0;

        const char max_char = from_base <= 10 ? from_base + '0' - 1: from_base - 11 + 'A';
        const char * data = str.data;
        const char * end = str.data + str.size;

        // negative number
        if (*data == '-')
            ++data;

        while (data != end)
        {
            if (!isAlphaNumericASCII(*data))
                return data - str.data;

            if ((isLowerAlphaASCII(*data) && *data - 'a' + 'A' > max_char) || (*data > max_char))
                return data - str.data;

            ++data;
        }

        return data - str.data;
    }

    static UInt8 charToNum(char value)
    {
        return isNumericASCII(value) ? value - '0' : (isLowerAlphaASCII(value) ? value - 'a' + 10 : value - 'A' + 10);
    }

    /// data += value, data is reversed.
    static void add(String & data, char value, int to_base)
    {
        size_t i = 0;
        bool mark = false; // carry mark
        UInt8 num = charToNum(value);

        while (num || mark)
        {
            if (i < data.size())
                data[i] += num % to_base + mark;
            else
                data.push_back(num % to_base + mark);

            // reset carry mark.
            mark = false;

            if (data[i] >= to_base)
            {
                data[i] -= to_base;
                mark = true;
            }

            num /= to_base;
            ++i;
        }
    }

    /// data *= value, data is reversed.
    static void multiply(String & data, UInt8 num, UInt8 to_base)
    {
        if (data.empty())
            return;

        String res;
        size_t i = 0;

        while (num)
        {
            UInt16 curr_value = num % to_base;
            UInt16 mark = 0; // carry mark

            for (size_t j = 0, size = data.size(); j < size; ++j)
            {
                if (i + j >= res.size())
                    res.push_back(0);
                auto v = res[i + j] + data[j] * curr_value + mark;
                res[i + j] = v % to_base;
                mark = v / to_base;
            }

            if (mark)
                res.push_back(mark);

            num /= to_base;
            ++i;
        }

        data = std::move(res);
    }

    static String convert(const StringRef & value, UInt8 from_base, UInt8 to_base)
    {
        auto index = getVerifiedIndex(value, from_base);

        if (!index || (index == 1 && *value.data == '-'))
            return "0";

        if (from_base == to_base)
            return String(value.data, index);

        String res;

        bool negative = false;
        const char * curr = value.data;
        const char * end = value.data + index;

        while (curr != end)
        {
            if (*curr == '-')
            {
                negative = true;
                ++curr;
                continue;
            }

            multiply(res, from_base, to_base);
            add(res, *curr, to_base);
            ++curr;
        }

        for (auto & c: res)
            c += (c < 10 ? '0' : 'A' - 10);

        if (res.empty())
            res.push_back('0');
        else if (negative)
            res.push_back('-');

        std::reverse(res.begin(), res.end());
        return res;
    }
};

class FunctionBaseConv : public IFunctionBase
{
public:
    explicit FunctionBaseConv(DataTypePtr return_type_) : return_type(return_type_) {}

    String getName() const override { return "now"; }

    const DataTypes & getArgumentTypes() const override
    {
        static const DataTypes argument_types;
        return argument_types;
    }

    const DataTypePtr & getResultType() const override
    {
        return return_type;
    }

    ExecutableFunctionPtr prepare(const ColumnsWithTypeAndName &) const override
    {
        return std::make_unique<ExecutableFunctionConv>();
    }

private:
    DataTypePtr return_type;
};

class ConvOverloadResolver : public IFunctionOverloadResolver
{
public:
    static constexpr auto name = "conv";

    String getName() const override { return name; }

    bool isDeterministic() const override { return false; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }
    static FunctionOverloadResolverPtr create(ContextPtr) { return std::make_unique<ConvOverloadResolver>(); }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() != 3)
            throw Exception("Number of arguments for function " + getName() + " doesn't match: passed " + toString(arguments.size()) + ", should be 3.",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!isString(arguments[0].type) && !isInteger(arguments[0].type))
            throw Exception("First argument for function " + getName() + " must be String or Integer", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (!isInteger(arguments[1].type))
            throw Exception("Second argument for function " + getName() + " must be Integer", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (!isInteger(arguments[2].type))
            throw Exception("Third argument for function " + getName() + " must be Integer", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeString>();
    }

    FunctionBasePtr buildImpl(const ColumnsWithTypeAndName &, const DataTypePtr &) const override
    {
        return std::make_unique<FunctionBaseConv>(std::make_shared<DataTypeString>());
    }
};


void registerFunctionConv(FunctionFactory & factory)
{
    factory.registerFunction<ConvOverloadResolver>(FunctionFactory::CaseInsensitive);
}

}
