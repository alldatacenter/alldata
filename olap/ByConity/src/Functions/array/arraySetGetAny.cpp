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
#include <Columns/ColumnNullable.h>
#include <Common/typeid_cast.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Set.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeNullable.h>
#include <Columns/ColumnSet.h>
#include <Functions/FunctionHelpers.h>
#include <Interpreters/NullableUtils.h>
#include <Core/Block.h>

namespace DB
{

namespace ErrorCodes
{
extern const int ILLEGAL_COLUMN;
}

/**
 *  arraySetGetAny(col, (1,2,3)) -> 1  if 1 is in column.
 *  if there is muliple element in column, return the first element it found, e.g. arraySetGetAny([1,2,3,4], (1,2)) -> 1
 *  if there is no value can be found, return 0
 **/

class FunctionArraySetGetAny : public IFunction
{
public:
    using ColumnArrays = std::vector<const ColumnArray *>;
    using ColumnSets = std::vector<const ColumnSet *>;
    using Bools = std::vector<bool>;

    static constexpr auto name = "arraySetGetAny";

    static FunctionPtr create(ContextPtr context)
    {
        return std::make_shared<FunctionArraySetGetAny>(context);
    }

    FunctionArraySetGetAny(ContextPtr &)
    {
    }

    String getName() const override { return name; }

    bool isVariadic() const override { return false; }

    size_t getNumberOfArguments() const override { return 2; }

    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl([[maybe_unused]] const DataTypes & arguments) const override
    {
        const DataTypeArray * array_type = checkAndGetDataType<DataTypeArray>(arguments[0].get());
        if (!array_type)
            throw Exception("Argument for function " + getName() + " must be array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeNullable>(array_type->getNestedType());
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t /* input_rows_count */) const override
    {
        const ColumnArray * array = checkAndGetColumn<ColumnArray>(arguments[0].column.get());
        const ColumnArray * array_const = checkAndGetColumnConstData<ColumnArray>(arguments[0].column.get());

        if (array_const)
            array = array_const;

        if (!array)
            throw Exception("Illegal column " + arguments[0].column->getName() + " of first argument of function " + getName(),
                            ErrorCodes::ILLEGAL_COLUMN);

        const ColumnSet * set_column = checkAndGetColumn<ColumnSet>(arguments[1].column.get());
        if (!set_column)
            throw Exception("Illegal column " + arguments[1].column->getName() + " of second argument of function " + getName(),
                            ErrorCodes::ILLEGAL_COLUMN);

        auto res_column = array->getData().cloneEmpty();
        auto res_map = ColumnUInt8::create();

        const Set & set = *(set_column->getData());

        switch(set.data.type)
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
                    setGetImpl<std::decay_t<decltype(*set.data.NAME)>>(*set.data.NAME, *array, *res_column, *res_map, set); \
                    break;

            APPLY_FOR_SET_VARIANTS_WITHOUT_STRING(M)
#undef M
        }

        if (array_const)
            return ColumnConst::create(ColumnNullable::create(std::move(res_column), std::move(res_map)), arguments[0].column->size());
        else
            return ColumnNullable::create(std::move(res_column), std::move(res_map));
    }

    template <typename Method>
    inline void setGetImpl([[maybe_unused]] Method& method,  [[maybe_unused]] const ColumnArray& array,
                           [[maybe_unused]] IColumn & result, [[maybe_unused]] ColumnUInt8 & result_map, const Set & set) const
    {
        const DataTypes & data_types = set.getDataTypes();
        if (data_types.size() == 0)
            throw Exception("Cannot find a valid set type", ErrorCodes::LOGICAL_ERROR);

        const IDataType & data_type = *(data_types[0]);
        WhichDataType to_type(data_type);

        if constexpr (std::is_same<typename Method::Key, UInt8>::value)
        {
            if (to_type.isInt8())
                setGetNumberImpl<Method, Int8>(method, array, result, result_map);
            else if (to_type.isUInt8())
                setGetNumberImpl<Method, UInt8>(method, array, result, result_map);
            else
                throw Exception("Not implement type for function " + data_type.getName(), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else if constexpr (std::is_same<typename Method::Key, UInt16>::value)
        {
            if (to_type.isInt16())
                setGetNumberImpl<Method, Int16>(method, array, result, result_map);
            else if (to_type.isUInt16())
                setGetNumberImpl<Method, UInt16>(method, array, result, result_map);
            else
                throw Exception("Not implement type for function " + data_type.getName(), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else if constexpr (std::is_same<typename Method::Key, UInt32>::value)
        {
            if (to_type.isInt32())
                setGetNumberImpl<Method, Int32>(method, array, result, result_map);
            else if (to_type.isUInt32())
                setGetNumberImpl<Method, UInt32>(method, array, result, result_map);
            else
                throw Exception("Not implement type for function " + data_type.getName(), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else if constexpr (std::is_same<typename Method::Key, UInt64>::value)
        {
            if (to_type.isInt64())
                setGetNumberImpl<Method, Int64>(method, array, result, result_map);
            else if (to_type.isUInt64())
                setGetNumberImpl<Method, UInt64>(method, array, result, result_map);
            else
                throw Exception("Not implement type for function " + data_type.getName(), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        else
            throw Exception("Not implement type for function " + getName(), ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

    template <typename Method, typename Data_Type>
    inline void setGetNumberImpl(Method & method, const ColumnArray & array, IColumn & result, ColumnUInt8 & result_map) const
    {
        if (auto * result_column = typeid_cast<ColumnVector<Data_Type> *>(&result))
        {
            const typename ColumnVector<Data_Type>::Container & array_container = typeid_cast<const ColumnVector<Data_Type> *>(&(array.getData()))->getData();
            typename ColumnVector<Data_Type>::Container & result_container = result_column->getData();
            ColumnUInt8::Container & result_map_container = result_map.getData();
            size_t rsize = array.size();
            const auto & offsets = array.getOffsets();
            size_t pre_offset = 0, cur_offset = 0;
            for (size_t i = 0; i<rsize; ++i)
            {
                // get current elems in this array input
                cur_offset = offsets[i];
                bool get_element = false;
                for (size_t j = pre_offset; j < cur_offset; ++j)
                {
                    Data_Type key = array_container[j];
                    if (method.data.has(key))
                    {
                        result_container.push_back(key);
                        get_element = true;
                        break;
                    }
                }
                if (!get_element)
                {
                    result_column->insertDefault();
                    result_map_container.push_back(1);
                }
                else
                    result_map_container.push_back(0);
                pre_offset = offsets[i];
            }
        }
    }

};

void registerFunctionArraySetGetAny(FunctionFactory & factory)
{
    factory.registerFunction<FunctionArraySetGetAny>();
}

}
