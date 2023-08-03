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

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/Native.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnConst.h>
#include <Functions/IFunction.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionFactory.h>
#include <Common/Arena.h>

namespace DB
{

/// A triple function that TEA use for sharding strategy, its signature is:
/// hashRange(hash_id, number_slots, number_shards)
class FunctionHashRange : public IFunction
{
public:
    static constexpr auto name = "hashRange";
    static FunctionPtr create(ContextPtr context_) { return std::make_shared<FunctionHashRange>(context_); }
    FunctionHashRange(ContextPtr) {}

    String getName() const override
    {
        return name;
    }

    size_t getNumberOfArguments() const override { return 3; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        return arguments[0];
    }

    template<typename T0, typename T1, typename T2>
    bool executeType(const ColumnsWithTypeAndName& args, ColumnPtr& res) const
    {
        auto col_0 = checkAndGetColumn<ColumnVector<T0>>(args[0].column.get());
        auto col_const_0 = checkAndGetColumnConst<ColumnVector<T0>>(args[0].column.get());
        auto col_1 = checkAndGetColumnConst<ColumnVector<T1>>(args[1].column.get());
        auto col_2 = checkAndGetColumnConst<ColumnVector<T2>>(args[2].column.get());

        if ((!col_0 && !col_const_0) || !col_1 || !col_2)
            return false;

        T1 val_1 = col_1->template getValue<T1>();
        T2 val_2 = col_2->template getValue<T2>();
        if (val_2 == 0)
            throw Exception("input val incorrect", ErrorCodes::BAD_ARGUMENTS);

        T0 per_range = (static_cast<T0>(val_1) + static_cast<T0>(val_2) - 1)/ static_cast<T0>(val_2);
        if (per_range == 0)
            throw Exception("input val incorrect", ErrorCodes::BAD_ARGUMENTS);

        auto col_res = ColumnVector<T0>::create();
        typename ColumnVector<T0>::Container & vec_res = col_res->getData();
        if (col_const_0)
        {
            T0 val_0 = col_const_0->template getValue<T0>();
            size_t size = col_const_0->size();
            vec_res.assign(size, (val_0 % static_cast<T0>(val_1))/per_range);
            res = std::move(col_res);
            return true;
        }

        auto& col_0_con = col_0->getData();
        size_t size = col_0->getData().size();
        vec_res.resize(size);
        // main column logic
        for (size_t i = 0; i < size; ++i)
            vec_res[i] = (col_0_con[i] % static_cast<T0>(val_1)) / per_range;

        res = std::move(col_res);
        return true;
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & args, const DataTypePtr& , size_t ) const override
    {
        ColumnPtr res;
        if (!(executeType<UInt64, UInt8, UInt8>(args, res)
              || executeType<UInt64, UInt16, UInt8>(args, res)
              || executeType<UInt64, UInt16, UInt16>(args, res)
              || executeType<UInt64, UInt32, UInt8>(args, res)
              || executeType<UInt64, UInt32, UInt16>(args, res)))
            throw Exception("Illegal Inputs of function hashRange", ErrorCodes::BAD_ARGUMENTS);

        return res;
    }

};

void registerFunctionHashRange(FunctionFactory & factory)
{
    factory.registerFunction<FunctionHashRange>();
}

}
