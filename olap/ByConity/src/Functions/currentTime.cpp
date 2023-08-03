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

#include <Core/DecimalFunctions.h>
#include <Core/Field.h>
#include <DataTypes/DataTypeTime.h>
#include <Functions/FunctionFactory.h>
#include <Functions/IFunction.h>
#include <Functions/now64.h>
#include <Functions/FunctionsConversion.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

namespace
{

/// Get the current time. (It is a constant, it is evaluated once for the entire query.)
class ExecutableFunctionCurrentTime : public IExecutableFunction
{
public:
    explicit ExecutableFunctionCurrentTime(Decimal64::NativeType time_, UInt32 scale_) : time_value(time_), scale(scale_) {}

    String getName() const override { return "current_time"; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName &, const DataTypePtr &, size_t input_rows_count) const override
    {
        return DataTypeTime(scale).createColumnConst(
                input_rows_count,
                time_value);
    }

private:
    Decimal64::NativeType time_value;
    UInt32 scale;
};

class FunctionBaseCurrentTime : public IFunctionBase
{
public:
    explicit FunctionBaseCurrentTime(Decimal64::NativeType time_, UInt32 scale_, DataTypePtr return_type_) : time_value(time_), scale(scale_), return_type(return_type_) {}

    String getName() const override { return "current_time"; }

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
        return std::make_unique<ExecutableFunctionCurrentTime>(time_value, scale);
    }

    bool isDeterministic() const override { return false; }
    bool isDeterministicInScopeOfQuery() const override { return true; }

private:
    Decimal64::NativeType time_value;
    UInt32 scale;
    DataTypePtr return_type;
};

class CurrentTimeOverloadResolver : public IFunctionOverloadResolver
{
public:
    static constexpr auto name = "current_time";

    String getName() const override { return name; }

    bool isDeterministic() const override { return false; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }
    static FunctionOverloadResolverPtr create(ContextPtr) { return std::make_unique<CurrentTimeOverloadResolver>(); }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        UInt32 scale = DataTypeTime::default_scale;

        if (arguments.size() > 1)
        {
            throw Exception("Arguments size of function " + getName() + " should be 0, or 1", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
        if (!arguments.empty())
        {
            const auto & argument = arguments[0];
            if (!isInteger(argument.type) || !argument.column || !isColumnConst(*argument.column))
                throw Exception("Illegal type " + argument.type->getName() +
                                " of 0" +
                                " argument of function " + getName() +
                                ". Expected const integer.",
                                ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

            scale = argument.column->get64(0);
        }

        return std::make_shared<DataTypeTime>(scale);
    }

    FunctionBasePtr buildImpl(const ColumnsWithTypeAndName &, const DataTypePtr &result_type) const override
    {
        UInt32 scale = DataTypeTime::default_scale;
        auto res_type = removeNullable(result_type);
        if (const auto * type = typeid_cast<const DataTypeTime *>(res_type.get())) {
            scale = type->getScale();
        }
        DateTime64 dt64 = DB::nowSubsecondDt64(scale);
        ToTimeTransform transformer(scale, scale);
        Decimal64::NativeType t = transformer.execute(dt64, intExp10(scale), DateLUT::instance());
        return std::make_unique<FunctionBaseCurrentTime>(t, scale, std::make_shared<DataTypeTime>(scale));
    }
};

}

void registerFunctionCurrentTime(FunctionFactory & factory)
{
    factory.registerFunction<CurrentTimeOverloadResolver>(FunctionFactory::CaseInsensitive);
    factory.registerFunction<CurrentTimeOverloadResolver>("LOCALTIME", FunctionFactory::CaseInsensitive);
}

}
