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

#include <Functions/now64.h>

#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <Functions/extractTimeZoneFromFunctionArguments.h>

#include <DataTypes/DataTypeDateTime64.h>
#include <DataTypes/DataTypeNullable.h>

#include <Common/assert_cast.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

namespace
{

Field nowSubsecond(UInt32 scale)
{
    DateTime64 dt64 = nowSubsecondDt64(scale);
    return DecimalField(dt64, scale);
}

/// Get the current time. (It is a constant, it is evaluated once for the entire query.)
class ExecutableFunctionNow64 : public IExecutableFunction
{
public:
    explicit ExecutableFunctionNow64(Field time_) : time_value(time_) {}

    String getName() const override { return "now64"; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName &, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        return result_type->createColumnConst(input_rows_count, time_value);
    }

private:
    Field time_value;
};

class FunctionBaseNow64 : public IFunctionBase
{
public:
    explicit FunctionBaseNow64(Field time_, DataTypePtr return_type_) : time_value(time_), return_type(return_type_) {}

    String getName() const override { return "now64"; }

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
        return std::make_unique<ExecutableFunctionNow64>(time_value);
    }

    bool isDeterministic() const override { return false; }
    bool isDeterministicInScopeOfQuery() const override { return true; }

private:
    Field time_value;
    DataTypePtr return_type;
};

class Now64OverloadResolver : public IFunctionOverloadResolver
{
public:
    static constexpr auto name = "now64";

    String getName() const override { return name; }

    bool isDeterministic() const override { return false; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }
    static FunctionOverloadResolverPtr create(ContextPtr) { return std::make_unique<Now64OverloadResolver>(); }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        UInt32 scale = DataTypeDateTime64::default_scale;
        String timezone_name;

        if (arguments.size() > 2)
        {
            throw Exception("Arguments size of function " + getName() + " should be 0, or 1, or 2", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
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
        if (arguments.size() == 2)
        {
            timezone_name = extractTimeZoneNameFromFunctionArguments(arguments, 1, 0);
        }

        return std::make_shared<DataTypeDateTime64>(scale, timezone_name);
    }

    FunctionBasePtr buildImpl(const ColumnsWithTypeAndName &, const DataTypePtr & result_type) const override
    {
        UInt32 scale = DataTypeDateTime64::default_scale;
        auto res_type = removeNullable(result_type);
        if (const auto * type = typeid_cast<const DataTypeDateTime64 *>(res_type.get()))
            scale = type->getScale();

        return std::make_unique<FunctionBaseNow64>(nowSubsecond(scale), result_type);
    }
};

}

void registerFunctionNow64(FunctionFactory & factory)
{
    factory.registerFunction<Now64OverloadResolver>(FunctionFactory::CaseInsensitive);
}

}
