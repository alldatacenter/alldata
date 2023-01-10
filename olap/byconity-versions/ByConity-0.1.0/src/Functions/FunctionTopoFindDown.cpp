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

#include <Columns/ColumnTuple.h>
#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeArray.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <DataTypes/DataTypesNumber.h>
#include <IO/WriteHelpers.h>
#include <Common/assert_cast.h>


namespace DB {
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

class FunctionTopoFindDown : public IFunction {
public:
    static constexpr auto name = "topoFindDown";

    static FunctionPtr create(ContextPtr) { return std::make_shared<FunctionTopoFindDown>(); }

    String getName() const override
    {
        return name;
    }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }

    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override {
        size_t num_arguments = arguments.size();

        if (num_arguments != 2)
            throw Exception("Function " + getName() + " must have 2 arguments", ErrorCodes::BAD_ARGUMENTS);

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt8>());
    }


    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t ) const override
    {
        ColumnPtr hit_col = arguments[0].column->convertToFullColumnIfConst();
        ColumnPtr level_col = arguments[1].column->convertToFullColumnIfConst();

        const ColumnArray * hit_col_array = checkAndGetColumn<ColumnArray>(hit_col.get());
        const ColumnArray * level_col_array = checkAndGetColumn<ColumnArray>(level_col.get());
        if (!hit_col_array || !level_col_array)
            throw Exception("Function " + getName() + " must have 2 Array arguments", ErrorCodes::BAD_ARGUMENTS);

        if (!hit_col_array->hasEqualOffsets(*level_col_array))
            throw Exception("Array arguments for function " + getName() + " must have equal sizes", ErrorCodes::BAD_ARGUMENTS);

        const ColumnVector<UInt8> * hit_col_nested = checkAndGetColumn<ColumnVector<UInt8>>(hit_col_array->getData());
        const ColumnVector<UInt8> * level_col_nested = checkAndGetColumn<ColumnVector<UInt8>>(level_col_array->getData());
        if (!hit_col_nested || !level_col_nested)
            throw Exception("Function " + getName() + " must have 2 Array(UInt8) arguments", ErrorCodes::BAD_ARGUMENTS);

        const PaddedPODArray<UInt8> & data1 = hit_col_nested->getData();
        const PaddedPODArray<UInt8> & data2 = level_col_nested->getData();
        const ColumnArray::Offsets &  offsets = hit_col_array->getOffsets();

        auto result_col = result_type->createColumn();
        ColumnArray & result_col_array = assert_cast<ColumnArray &>(*result_col);
        IColumn & result_data_col = result_col_array.getData();
        PaddedPODArray<UInt8> & result_data = typeid_cast<ColumnVector<UInt8> &>(result_data_col).getData();

        ColumnArray::Offsets & result_offsets = result_col_array.getOffsets();

        size_t offsets_size = offsets.size();
        for (size_t i = 0; i < offsets_size; ++i)
            result_offsets.emplace_back(offsets[i]);

        size_t current_offset = 0;
        size_t offsets_i = 0;
        for (size_t i = 0; i < offsets_size; ++i)
        {
            size_t j = current_offset;
            offsets_i = offsets[i];
            for (; j < offsets_i; )
            {
                UInt8 hit_level = 0;
                for (; j < offsets[i]; )
                {
                    UInt8 hit = data1[j];
                    UInt8 level = data2[j];
                    if (hit)
                    {
                        hit_level = level;
                        // take j
                        result_data.emplace_back(UInt8(1));
                        j++;
                        break;
                    }
                    else
                    {
                        result_data.emplace_back(UInt8(0));
                        j++;
                    }
                }
                for (; j < offsets_i; )
                {
                    UInt8 level = data2[j];
                    if (level <= hit_level)
                        break;
                    // take j
                    result_data.emplace_back(UInt8(1));
                    j++;
                }
            }
            current_offset = offsets_i;
        }
        return result_col;
    }
};

void registerFunctionTopoFindDown(FunctionFactory &factory)
{
    factory.registerFunction<FunctionTopoFindDown>();
}
}

