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

#include <Functions/FunctionFactory.h>
#include <Functions/FunctionDateOrDateTimeAddInterval.h>

namespace DB
{

class FunctionNextDay: public FunctionDateOrDateTimeAddInterval<NextDayImp>
{
public:
    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionNextDay>(); }
    using Base = FunctionDateOrDateTimeAddInterval<NextDayImp>;

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        ColumnsWithTypeAndName new_arguments;
        for (size_t i = 0; i < arguments.size(); ++i)
        {
            if (i == 1 && isString(arguments[i].type))
                new_arguments.emplace_back(nullptr, std::make_shared<DataTypeUInt8>(), arguments[i].name);
            else
                new_arguments.emplace_back(nullptr, arguments[i].type, arguments[i].name);
        }

        return Base::getReturnTypeImpl(new_arguments);
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        auto new_arguments = arguments;
        new_arguments[1].column = convertColumn(arguments[1]);
        return Base::executeImpl(new_arguments, result_type, input_rows_count);
    }

private:
    static ColumnPtr convertColumn(const ColumnWithTypeAndName & column)
    {
        if (!isString(column.type))
            return column.column;

        constexpr static auto week = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };

        auto res = ColumnUInt8::create(column.column->size());
        auto & data = res->getData();

        for (size_t i = 0; i < column.column->size(); ++i)
        {
            auto value = std::string(column.column->getDataAt(i));
            if (value.size() < 2)
                throw Exception(value + " can not convert to day of week", ErrorCodes::BAD_ARGUMENTS);

            for (auto & c : value)
                if (isUpperAlphaASCII(c))
                    c = c - 'A' + 'a';

            auto it = std::find_if(week.begin(), week.end(), [&](auto & c) { return startsWith(c, value); });
            if (it == week.end())
                throw Exception(value + " can not convert to day of week", ErrorCodes::BAD_ARGUMENTS);

            data[i] = std::distance(week.begin(), it) + 1;
        }

        return res;
    }
};

void registerFunctionNextDay(FunctionFactory & factory)
{
    factory.registerFunction<FunctionNextDay>();
    factory.registerAlias("next_day", NextDayImp::name);
}

}
