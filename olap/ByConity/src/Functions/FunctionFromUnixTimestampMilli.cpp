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

#include <Functions/extractTimeZoneFromFunctionArguments.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypeDateTime.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int DECIMAL_OVERFLOW;
    extern const int ILLEGAL_COLUMN;
}

class FunctionFromUnixTimestampMilli : public IFunction
{
public:
    static constexpr auto name = "fromUnixTimestampMilli";
    static FunctionPtr create(ContextPtr context_) { return std::make_shared<FunctionFromUnixTimestampMilli>(context_); }

    explicit FunctionFromUnixTimestampMilli(ContextPtr context_) : context(context_) {}
    size_t getNumberOfArguments() const override { return 0; }
    bool isVariadic() const override { return true; }
    String getName() const override { return name; }
    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName &arguments) const override
    {
        if (arguments.size() < 1 || arguments.size() > 2)
            throw Exception(ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH, "Function fromUnixTimestampMilli takes one or two arguments");

        if (!isInteger(arguments[0].type))
            throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
                            "The first argument for function fromUnixTimestampMilli must be integer");

        std::string timezone;
        if (arguments.size() == 2)
            timezone = extractTimeZoneNameFromFunctionArguments(arguments, 1, 0);

        return std::make_shared<DataTypeDateTime>(timezone);
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName &arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        WhichDataType which(arguments[0].column->getDataType());
        if (!(which.isInt() || which.isUInt()))
            throw Exception(ErrorCodes::ILLEGAL_COLUMN,
                    "Illegal column {} of first argument of function {}",
                    arguments[0].column->getName(),
                    getName());

        auto to_second_ts = FunctionFactory::instance().get("intDiv", context);
        auto from_unix_ts = FunctionFactory::instance().get("FROM_UNIXTIME", context);

        const auto source_type = arguments[0].type;

        auto dividend = arguments[0].type->createColumnConst(input_rows_count, Field(1000));
        ColumnsWithTypeAndName div_input = {arguments[0], {dividend, source_type, "dividend"}};

        auto ts = to_second_ts->build(div_input)->execute(div_input, source_type, input_rows_count);
        ColumnsWithTypeAndName from_ts_input = {{ts, source_type, "timestamp"}};

        auto result = from_unix_ts->build(from_ts_input)->execute(from_ts_input, result_type, input_rows_count);

        return result;
    }

private:
    ContextPtr context;
};

void registerFromUnixTimestampMilli(FunctionFactory & factory)
{
    factory.registerFunction("fromUnixTimestampMilli",
        [](ContextPtr context){ return std::make_unique<FunctionToOverloadResolverAdaptor>(
            std::make_shared<FunctionFromUnixTimestampMilli>(context)); });
}

}
