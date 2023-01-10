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
#include <Columns/ColumnArray.h>
#include <Columns/ColumnsNumber.h>
#include <Common/typeid_cast.h>
#include <Functions/FunctionFactory.h>
#include <Interpreters/Set.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/IDataType.h>
#include <Columns/ColumnSet.h>
#include <Functions/FunctionHelpers.h>
#include <Interpreters/NullableUtils.h>


namespace DB
{

namespace ErrorCodes
{
extern const int ILLEGAL_COLUMN;
}

/**
***  arraySetCheck can have variadic arguments. For example:
***  1. arraySetCheck(column, set)  # it return true if column has at least one value in set
***  2. arraySetCheck(column1, set1, column2, set2) # it can be regarded as arraySetCheck(column1, set1) and arraySetCheck(column2, set2)
**/

class FunctionArraySetCheck : public IFunction
{
public:
    using ColumnArrays = std::vector<const ColumnArray *>;
    using ColumnSets = std::vector<const ColumnSet *>;
    using Bools = std::vector<bool>;

    static constexpr auto name = "arraySetCheck";
    static FunctionPtr create(ContextPtr context)
    {
        return std::make_shared<FunctionArraySetCheck>(context);
    }

    FunctionArraySetCheck(ContextPtr)
    {
    }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }

    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        if (arguments.size() == 2)
        {
            if (isArray(arguments[0]))
            {
                if (const auto * data_type_array = typeid_cast<const DataTypeArray*>(arguments[0].get()))
                {
                    if (data_type_array->getNestedType()->getTypeId() == TypeIndex::String)
                        throw Exception{
                            "Second argument for function " + getName() + " can not be string, will be support later",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT};
                }

            }
        }
        return std::make_shared<DataTypeNumber<UInt8>>();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t /* input_rows_count */) const override
    {
        ColumnArrays arrays;
        ColumnSets sets;
        Bools is_nullables;

        bool is_column = true;
        bool all_array_const = true;

        for (size_t i = 0; i < arguments.size(); ++i)
        {
            if (is_column)
            {
                const ColumnArray * array = checkAndGetColumn<ColumnArray>(arguments[i].column.get());
                const ColumnArray * array_const = checkAndGetColumnConstData<ColumnArray>(arguments[i].column.get());

                bool is_nullable = false;
                if (array)
                {
                    is_nullable = isColumnNullable(array->getData());
                    all_array_const = false;
                }
                else if (array_const)
                    array = array_const;

                if (!array)
                    throw Exception("Illegal first argument of function " + getName(), ErrorCodes::ILLEGAL_COLUMN);

                arrays.push_back(array);
                is_nullables.push_back(is_nullable);
                is_column = false;
            }
            else
            {
                bool isArgNullable = isColumnNullable(*arguments[i].column);

                if (isArgNullable)
                    throw Exception("Nullable set is not support in function " + getName(), ErrorCodes::NOT_IMPLEMENTED);

                const ColumnSet * set_arg = checkAndGetColumn<ColumnSet>(arguments[i].column.get());

                if (!set_arg)
                    throw Exception{"Illegal set argument of function " + getName(), ErrorCodes::ILLEGAL_COLUMN};

                sets.push_back(set_arg);
                is_column = true;
            }
        }

        size_t array_size = arrays.size();
        if (array_size == 0 || array_size != sets.size() || array_size != is_nullables.size())
            throw Exception("Mismatch arguments of columns and sets", ErrorCodes::ILLEGAL_COLUMN);

        auto result_column = ColumnUInt8::create();
        ColumnUInt8::Container& vec_res = result_column->getData();
        size_t column_size = arrays.front()->size();
        vec_res.resize_fill(column_size);

        for (size_t col_idx = 0; col_idx < array_size; ++col_idx)
        {
            const ColumnArray * array = arrays[col_idx];
            const Set & st = *(sets[col_idx]->getData());
            bool is_nullable = is_nullables[col_idx];
            bool first = col_idx == 0 ? true : false;

            switch(st.data.type)
            {
                case SetVariants::Type::EMPTY:
                    break;
                    /* TODO dongyifeng support it later
                     */
                case SetVariants::Type::key_string:
                case SetVariants::Type::key_fixed_string:
                    break;
                    /* TODO dongyifeng add it later
                    case SetVariants::Type::bitmap64:
                        break;
                         */
#define M(NAME)                                                          \
                case SetVariants::Type::NAME:                                \
                    if (is_nullable)                                         \
                    setCheckImpl<std::decay_t<decltype(*st.data.NAME)>, true>(*st.data.NAME, *array, vec_res, first);  \
                    else                                                                                          \
                    setCheckImpl<std::decay_t<decltype(*st.data.NAME)>, false>(*st.data.NAME, *array, vec_res, first); \
                    break;

                APPLY_FOR_SET_VARIANTS_WITHOUT_STRING(M)
#undef M
            }
        }

        if (all_array_const)
            return ColumnConst::create(std::move(result_column), arguments[0].column->size());
        else
            return result_column;
    }

    template <typename Method, bool Nullable>
    inline void setCheckImpl(Method& method,
                             const ColumnArray& keys,
                             ColumnUInt8::Container& result,
                             bool first) const
    {
        size_t rsize = result.size();
        ConstNullMapPtr null_map{};

        ColumnRawPtrs key_cols;
        key_cols.push_back(&keys.getData());

        if(Nullable) extractNestedColumnsAndNullMap(key_cols, null_map);

        typename Method::State state(key_cols, {}, nullptr);
        size_t pre_offset = 0, cur_offset = 0;
        auto& offsets = keys.getOffsets();
        Arena arena(0);
        for (size_t i = 0; i<rsize; ++i)
        {
            // get current elems in this array input
            cur_offset = offsets[i];

            if (first || result[i])
            {
                bool has_key = false;
                for (size_t j = pre_offset; (!has_key) && j < cur_offset; ++j)
                {
                    if (Nullable && (*null_map)[j]) continue; // skip null input
                    typename Method::Key key = state.getKeyHolder(j, arena);
                    has_key = method.data.has(key);
                }
                result[i] = has_key;
            }

            pre_offset = offsets[i];
        }
    }

};

void registerFunctionArraySetCheck(FunctionFactory & factory)
{
    factory.registerFunction<FunctionArraySetCheck>();
}

}
