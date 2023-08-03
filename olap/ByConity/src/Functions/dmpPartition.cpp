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

#include <Common/typeid_cast.h>
#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnConst.h>
#include <Common/typeid_cast.h>
#include <iostream>

namespace DB
{

namespace ErrorCodes
{
    extern const int ARGUMENT_OUT_OF_BOUND;
    extern const int TOO_MANY_ARGUMENTS_FOR_FUNCTION;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int ILLEGAL_COLUMN;
    extern const int LOGICAL_ERROR;
}

class FunctionDmpPartition: public IFunction
{
public:
    static constexpr auto name = "dmpPartition";
    static FunctionPtr create(ContextPtr)
    {
        return std::make_shared<FunctionDmpPartition>();
    }

    String getName() const override { return name; }
    size_t getNumberOfArguments() const override { return 3; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        const DataTypePtr & type = arguments[0];

        if (!isInteger(type))
            throw Exception("Cannot partition " + type->getName(), ErrorCodes::LOGICAL_ERROR);

        return type;
    }

    ColumnPtr executeImplDryRun(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t ) const override
    {
        if (arguments.size() != 3)
            throw Exception("Function " + getName() + " need three arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto & num_column = arguments[0];
        return num_column.type->createColumn();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & , size_t ) const override
    {
        if (arguments.size() != 3)
            throw Exception("Function " + getName() + " need three arguments", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto & num_column = arguments[0];
        const auto & prefix_column = arguments[1];
        const auto & length_column = arguments[2];

        WhichDataType data_type(num_column.type);
        if (data_type.isUInt8())
            return executeInt<UInt8>(num_column, prefix_column, length_column);
        else if (data_type.isUInt16())
            return executeInt<UInt16>(num_column, prefix_column, length_column);
        else if (data_type.isUInt32())
            return executeInt<UInt32>(num_column, prefix_column, length_column);
        else if (data_type.isUInt64())
            return executeInt<UInt64>(num_column, prefix_column, length_column);
        else if (data_type.isInt8())
            return executeInt<Int8>(num_column, prefix_column, length_column);
        else if (data_type.isInt16())
            return executeInt<Int16>(num_column, prefix_column, length_column);
        else if (data_type.isInt32())
            return executeInt<Int32>(num_column, prefix_column, length_column);
        else if (data_type.isInt64())
            return executeInt<Int64>(num_column, prefix_column, length_column);
        else
            throw Exception("First argument for " + getName() + " should be numeric type", ErrorCodes::LOGICAL_ERROR);
    }

private:
    template <typename T, typename K>
    ColumnPtr executeIntImpl(const IColumn * column, const IColumn * prefix, const IColumn * length) const
    {
        const ColumnVector<T> * num_column = checkAndGetColumn<ColumnVector<T>>(column);
        const ColumnVector<T> * num_const = checkAndGetColumnConstData<ColumnVector<T>>(column);
        if (num_const)
            num_column = num_const;

        const ColumnVector<K> * prefix_column = checkAndGetColumnConstData<ColumnVector<K>>(prefix);
        const ColumnVector<UInt8> * length_column = checkAndGetColumnConstData<ColumnVector<UInt8>>(length);
        if (!prefix_column || !length_column)
            throw Exception("Function " + getName() + " need const number for last three arguments", ErrorCodes::ILLEGAL_COLUMN);

        T prefix_number = prefix_column->getElement(0);
        UInt8 length_number = length_column->getElement(0);

        if (length_number <= 0)
            throw Exception("Function " + getName() + " can only handle positive length for partition", ErrorCodes::LOGICAL_ERROR);

        T factor_number = std::pow(10, length_number);

        if (factor_number == 0)
            throw Exception("Function " + getName() + " cannot handler zero devide", ErrorCodes::LOGICAL_ERROR);

        T prefix_tailing_length = 0;
        T temp = prefix_number;
        while ((temp % 10) == 0 && temp != 0)
        {
            temp /= 10;
            prefix_tailing_length++;
        }

        if (num_column)
        {
            size_t rows_size = num_column->size();
            auto clone_column = num_column->cloneResized(rows_size);
            ColumnVector<T> * res_column = typeid_cast<ColumnVector<T> * >(clone_column.get());
            if (!res_column)
                throw Exception("Function " + getName() + " can only handle positive number", ErrorCodes::LOGICAL_ERROR);

            typename ColumnVector<T>::Container & vec_res = res_column->getData();

            if (prefix_tailing_length != 0)
            {
                T prefix_factor = std::pow(10, prefix_tailing_length);

                for (size_t i = 0; i < rows_size; ++i)
                {
                    if ((vec_res[i] / prefix_factor * prefix_factor) == prefix_number)
                        vec_res[i] = vec_res[i] / factor_number * factor_number;
                }
            }

            if (num_const)
                return ColumnConst::create(std::move(clone_column), rows_size);
            else
                return clone_column;
        }
        else
            throw Exception("Function " + getName() + " can only handle positive number", ErrorCodes::LOGICAL_ERROR);
    }


    template <typename T>
    ColumnPtr executeInt(const ColumnWithTypeAndName & partition, const ColumnWithTypeAndName & prefix, const ColumnWithTypeAndName & length) const
    {
        WhichDataType data_type(prefix.type);
        if (data_type.isUInt8())
            return executeIntImpl<T, UInt8>(partition.column.get(), prefix.column.get(), length.column.get());
        else if (data_type.isUInt16())
            return executeIntImpl<T, UInt16>(partition.column.get(), prefix.column.get(), length.column.get());
        else if (data_type.isUInt32())
            return executeIntImpl<T, UInt32>(partition.column.get(), prefix.column.get(), length.column.get());
        else if (data_type.isUInt64())
            return executeIntImpl<T, UInt64>(partition.column.get(), prefix.column.get(), length.column.get());
        else
            throw Exception("Function " + getName() + " need Unsigned number for second argument", ErrorCodes::LOGICAL_ERROR);
    }
};

void registerFunctionDmpPartition(FunctionFactory & factory)
{
    factory.registerFunction<FunctionDmpPartition>();
}


}
