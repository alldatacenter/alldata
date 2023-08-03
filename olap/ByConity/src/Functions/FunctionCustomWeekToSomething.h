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
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDate32.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeDateTime64.h>
#include <Functions/CustomWeekTransforms.h>
#include <Functions/IFunction.h>
#include <Functions/TransformDateTime64.h>
#include <IO/WriteHelpers.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}


/// See CustomWeekTransforms.h
template <typename ToDataType, typename Transform>
class FunctionCustomWeekToSomething : public IFunction
{
public:
    static constexpr auto name = Transform::name;
    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionCustomWeekToSomething>(); }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }
    size_t getNumberOfArguments() const override { return 0; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() == 1)
        {
            if (!isDate(arguments[0].type) && !isDate32(arguments[0].type)
                && !isDateTime(arguments[0].type) && !isDateTime64(arguments[0].type))
                throw Exception(
                    "Illegal type " + arguments[0].type->getName() + " of argument of function " + getName()
                        + ". Must be Date, Date32, DateTime or DateTime64.",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else if (arguments.size() == 2)
        {
            if (!isDate(arguments[0].type) && !isDate32(arguments[0].type)
                && !isDateTime(arguments[0].type) && !isDateTime64(arguments[0].type))
                throw Exception(
                    "Illegal type " + arguments[0].type->getName() + " of 1st argument of function " + getName()
                        + ". Must be Date, Date32, DateTime or DateTime64.",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            if (!isUInt8(arguments[1].type))
                throw Exception(
                        "Illegal type of 2nd (optional) argument of function " + getName()
                        + ". Must be constant UInt8 (week mode).",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else if (arguments.size() == 3)
        {
            if (!isDate(arguments[0].type) && !isDate32(arguments[0].type)
                && !isDateTime(arguments[0].type) && !isDateTime64(arguments[0].type))
                throw Exception(
                    "Illegal type " + arguments[0].type->getName() + " of argument of function " + getName()
                        + ". Must be Date, Date32, DateTime or DateTime64.",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            if (!isUInt8(arguments[1].type))
                throw Exception(
                        "Illegal type of 2nd (optional) argument of function " + getName()
                        + ". Must be constant UInt8 (week mode).",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            if (!isString(arguments[2].type))
                throw Exception(
                        "Illegal type of 3rd (optional) argument of function " + getName()
                        + ". Must be constant string (timezone name).",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            if ((isDate(arguments[0].type) || isDate32(arguments[0].type))
                && (std::is_same_v<ToDataType, DataTypeDate> || std::is_same_v<ToDataType, DataTypeDate32>))                throw Exception(
                    "The timezone argument of function " + getName() + " is allowed only when the 1st argument has the type DateTime or DateTime64.",
                    ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else
            throw Exception(
                "Number of arguments for function " + getName() + " doesn't match: passed " + toString(arguments.size())
                    + ", expected 1, 2 or 3.",
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        return std::make_shared<ToDataType>();
    }

    bool useDefaultImplementationForConstants() const override { return true; }
    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1, 2}; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        const IDataType * from_type = arguments[0].type.get();
        WhichDataType which(from_type);

        if (which.isDate())
            return CustomWeekTransformImpl<DataTypeDate, ToDataType>::execute(
                arguments, result_type, input_rows_count, Transform{});
        else if (which.isDate32())
            return CustomWeekTransformImpl<DataTypeDate32, ToDataType>::execute(
                arguments, result_type, input_rows_count, Transform{});
        else if (which.isDateTime())
            return CustomWeekTransformImpl<DataTypeDateTime, ToDataType>::execute(
                arguments, result_type, input_rows_count, Transform{});
        else if (which.isDateTime64())
        {
            return CustomWeekTransformImpl<DataTypeDateTime64, ToDataType>::execute(
                arguments, result_type, input_rows_count,
                TransformDateTime64<Transform>{assert_cast<const DataTypeDateTime64 *>(from_type)->getScale()});
        }
        else
            throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
                "Illegal type {} of argument of function {}",
                arguments[0].type->getName(), this->getName());
    }


    bool hasInformationAboutMonotonicity() const override { return true; }

    Monotonicity getMonotonicityForRange(const IDataType & type, const Field & left, const Field & right) const override
    {
        IFunction::Monotonicity is_monotonic{true};
        IFunction::Monotonicity is_not_monotonic;

        if (std::is_same_v<typename Transform::FactorTransform, ZeroTransform>)
        {
            is_monotonic.is_always_monotonic = true;
            return is_monotonic;
        }

        /// This method is called only if the function has one argument. Therefore, we do not care about the non-local time zone.
        const DateLUTImpl & date_lut = DateLUT::instance();

        if (left.isNull() || right.isNull())
            return is_not_monotonic;

        /// The function is monotonous on the [left, right] segment, if the factor transformation returns the same values for them.

        if (checkAndGetDataType<DataTypeDate>(&type))
        {
            return Transform::FactorTransform::execute(UInt16(left.get<UInt64>()), date_lut)
                    == Transform::FactorTransform::execute(UInt16(right.get<UInt64>()), date_lut)
                ? is_monotonic
                : is_not_monotonic;
        }
        else if (checkAndGetDataType<DataTypeDate32>(&type))
        {
            return Transform::FactorTransform::execute(Int32(left.get<UInt64>()), date_lut)
                   == Transform::FactorTransform::execute(Int32(right.get<UInt64>()), date_lut)
                   ? is_monotonic : is_not_monotonic;
        }
        else
        {
            return Transform::FactorTransform::execute(UInt32(left.get<UInt64>()), date_lut)
                    == Transform::FactorTransform::execute(UInt32(right.get<UInt64>()), date_lut)
                ? is_monotonic
                : is_not_monotonic;
        }
    }
};

}
