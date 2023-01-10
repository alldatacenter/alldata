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
#include <DataTypes/DataTypeLowCardinality.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnLowCardinality.h>
#include <Common/typeid_cast.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}


class FunctionLowCardinalityIsNoneEncoded: public IFunction
{
public:
    static constexpr auto name = "lowCardinalityIsNoneEncoded";
    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionLowCardinalityIsNoneEncoded>(); }

    String getName() const override { return name; }

    size_t getNumberOfArguments() const override { return 1; }

    bool useDefaultImplementationForNulls() const override { return false; }
    bool useDefaultImplementationForConstants() const override { return true; }
    bool useDefaultImplementationForLowCardinalityColumns() const override { return false; }

    DataTypePtr getReturnTypeImpl(const DataTypes &) const override
    {
        return std::make_shared<DataTypeUInt8>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & argument, const DataTypePtr &,  size_t) const override
    {
        const ColumnWithTypeAndName & elem = argument[0];
        if (auto const * low_lc = checkAndGetColumn<ColumnLowCardinality>(*elem.column))
        {
            if (low_lc->isFullState())
            {
                return  DataTypeUInt8().createColumnConst(elem.column->size(), 1u);
            }
        }

        return DataTypeUInt8().createColumnConst(elem.column->size(), 0u);
    }
};


void registerFunctionLowCardinalityIsNoneEncoded(FunctionFactory & factory)
{
    factory.registerFunction<FunctionLowCardinalityIsNoneEncoded>();
}

}
