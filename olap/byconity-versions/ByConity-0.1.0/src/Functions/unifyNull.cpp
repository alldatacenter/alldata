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
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypeNullable.h>
#include <Columns/ColumnNullable.h>
#include <Core/ColumnNumbers.h>
#include <Common/typeid_cast.h>
#include <DataTypes/DataTypesNumber.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

/// If value is Nullable, return false, otherwise return its value
class FunctionUnifyNull : public IFunction
{
public:
    static constexpr auto name = "unifyNull";
    static FunctionPtr create(ContextPtr)
    {
        return std::make_shared<FunctionUnifyNull>();
    }

    std::string getName() const override
    {
        return name;
    }
    size_t getNumberOfArguments() const override { return 1; }
    bool useDefaultImplementationForNulls() const override { return false; }
    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        const auto *type = arguments[0].get();
        if (arguments[0]->isNullable())
        {
            type = static_cast<const DataTypeNullable &>(*arguments[0]).getNestedType().get();
        }

        if (!typeid_cast<const DataTypeUInt8 *>(type))
            throw Exception("UnifyNull only accept one UInt8 input", ErrorCodes::BAD_ARGUMENTS);

        return std::make_shared<DataTypeUInt8>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & , size_t ) const override
    {
        const ColumnPtr & col = arguments[0].column;

        if (col->onlyNull())
            return DataTypeUInt8().createColumnConst(col->size(), UInt64(0));
        else if (isColumnNullable(*col))
        {
            auto col_res = ColumnUInt8::create();
            typename ColumnUInt8::Container& vec_res = col_res->getData();
            vec_res.resize(col->size());

            const auto & nullable_col = static_cast<const ColumnNullable &>(*col);
            const auto & null_map = nullable_col.getNullMapColumn();
            const auto & col_src = nullable_col.getNestedColumn();
            const auto & null_data = static_cast<const ColumnUInt8 &>(null_map).getData();
            const auto & src_data = static_cast<const ColumnUInt8 &>(col_src).getData();
            for (size_t i = 0; i<col->size(); i++)
                vec_res[i] = (null_data[i] == 0 && src_data[i] != 0) ? 1 : 0;

            return col_res;
        }
        else
        {
            return col;
        }
    }
};


void registerFunctionUnifyNull(FunctionFactory & factory)
{
    factory.registerFunction<FunctionUnifyNull>();
}
}
