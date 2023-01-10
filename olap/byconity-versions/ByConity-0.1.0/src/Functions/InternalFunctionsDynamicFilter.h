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

#include <Columns/ColumnConst.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/IFunction.h>

namespace DB
{
/**
 * internal function for optimizer
 */
class InternalFunctionDynamicFilter : public IFunction
{
public:
    static constexpr auto name = "$dynamicFilter";

    static FunctionPtr create(ContextPtr /*context*/) { return std::make_shared<InternalFunctionDynamicFilter>(); }

    String getName() const override { return name; }

    ColumnPtr executeImpl(
        const ColumnsWithTypeAndName & /*arguments*/, const DataTypePtr & /*result_type*/, size_t /*input_rows_count*/) const override
    {
        throw Exception("Unexpected internal function: dynamic filter", ErrorCodes::NOT_IMPLEMENTED);
    }

    DataTypePtr getReturnTypeImpl(const DataTypes & /*arguments*/) const override { return std::make_shared<DataTypeUInt8>(); }

    size_t getNumberOfArguments() const override { return 4; }
};

void registerInternalFunctionDynamicFilter(FunctionFactory &);
}
