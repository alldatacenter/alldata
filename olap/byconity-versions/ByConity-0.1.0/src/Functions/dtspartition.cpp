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
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeFixedString.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeTuple.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnFixedString.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <Functions/IFunction.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionFactory.h>
#include <Functions/hiveCityHash.h>
#include <Functions/hiveIntHash.h>
#include <typeinfo>

namespace DB
{

namespace ErrorCodes
{
extern const int ILLEGAL_COLUMN;
extern const int ILLEGAL_TYPE_OF_ARGUMENT;
extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

class FunctionDtsPartition : public IFunction
{
public:
    static constexpr auto name = "dtspartition";
    static FunctionPtr create(ContextPtr)
    {
        return std::make_shared<FunctionDtsPartition>();
    }

    String getName() const override
    {
        return name;
    }

    bool isVariadic() const override { return false; }
    size_t getNumberOfArguments() const override { return 2; }

    bool useDefaultImplementationForConstants() const override { return true; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        size_t number_of_arguments = arguments.size();

        if (number_of_arguments != 2)
            throw Exception("Number of arguments for function " + getName() + " doesn't match: passed "
                            + toString(number_of_arguments) + ", should be 2",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!isNumber(arguments[1]))
            throw Exception("Illegal type " + arguments[1]->getName()
                            + " of second argument of function "
                            + getName(),
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeUInt64>();
    }

private:
    using ToType = UInt64;

    template <typename FromType>
    void executeIntType(const IColumn * column, typename ColumnVector<ToType>::Container & vec_to, const Int64 & split_number) const
    {
        if (const ColumnVector<FromType> * col_from = checkAndGetColumn<ColumnVector<FromType>>(column))
        {
            const typename ColumnVector<FromType>::Container & vec_from = col_from->getData();
            size_t size = vec_from.size();
            for (size_t i = 0; i < size; ++i)
            {
                vec_to[i] = (HiveIntHash::intHash64(vec_from[i]).getMagnitude() % split_number).toUnsignedLong();
            }
        }
        else if (auto col_from_const = checkAndGetColumnConst<ColumnVector<FromType>>(column))
        {
            FromType value = col_from_const->template getValue<FromType>();
            UInt64 hash = (HiveIntHash::intHash64(value).getMagnitude() % split_number).toUnsignedLong();

            size_t size = vec_to.size();
            vec_to.assign(size, hash);
        }
        else
            throw Exception("Illegal column " + column->getName()
                            + " of argument of function " + getName(),
                            ErrorCodes::ILLEGAL_COLUMN);
    }

    void executeString(const IColumn * column, typename ColumnVector<ToType>::Container & vec_to, const Int64 & split_number) const
    {
        if (const ColumnString * col_from = checkAndGetColumn<ColumnString>(column))
        {
            const typename ColumnString::Chars & data = col_from->getChars();
            const typename ColumnString::Offsets & offsets = col_from->getOffsets();
            size_t size = offsets.size();

            ColumnString::Offset current_offset = 0;
            for (size_t i = 0; i < size; ++i)
            {
                const char *s = reinterpret_cast<const char *>(&data[current_offset]);
                auto length = offsets[i] - current_offset - 1;
                if (isDigit(s, length))
                    vec_to[i] = (HiveIntHash::intHash64(stringToBigInteger(s, length)).getMagnitude() % split_number).toUnsignedLong();
                else
                    vec_to[i] = HiveCityHash::cityHash64(s, 0, length) % split_number;
                current_offset = offsets[i];
            }
        }
        else if (const ColumnFixedString * col_from_fixed = checkAndGetColumn<ColumnFixedString>(column))
        {
            const typename ColumnString::Chars & data = col_from_fixed->getChars();
            size_t n = col_from_fixed->getN();
            size_t size = data.size() / n;

            for (size_t i = 0; i < size; ++i)
            {
                const char *s = reinterpret_cast<const char *>(&data[i * n]);
                if (isDigit(s, n))
                    vec_to[i] = (HiveIntHash::intHash64(stringToBigInteger(s, n)).getMagnitude() % split_number).toUnsignedLong();
                else
                    vec_to[i] = HiveCityHash::cityHash64(s, 0, n) % split_number;
            }
        }
        else if (const ColumnConst * col_from_const = checkAndGetColumnConstStringOrFixedString(column))
        {
            String value = col_from_const->getValue<String>().data();
            UInt64 hash;
            if (isDigit(value.c_str(), value.length()))
                hash = (HiveIntHash::intHash64(stringToBigInteger(value.c_str(), value.length())).getMagnitude() % split_number).toUnsignedLong();
            else
                hash = HiveCityHash::cityHash64(value.c_str(), 0, value.length()) % split_number;
            vec_to.assign(vec_to.size(), hash);
        }
        else
            throw Exception("Illegal column " + column->getName()
                            + " of first argument of function " + getName(),
                            ErrorCodes::ILLEGAL_COLUMN);
    }

    void executeAny(const IDataType * from_type, const IColumn * icolumn, typename ColumnVector<ToType>::Container & vec_to, const Int64 & split_number) const
    {
        WhichDataType which(from_type);

        if      (which.isUInt8()) executeIntType<UInt8>(icolumn, vec_to, split_number);
        else if (which.isUInt16()) executeIntType<UInt16>(icolumn, vec_to, split_number);
        else if (which.isUInt32()) executeIntType<UInt32>(icolumn, vec_to, split_number);
        else if (which.isUInt64()) executeIntType<UInt64>(icolumn, vec_to, split_number);
        else if (which.isInt8()) executeIntType<Int8>(icolumn, vec_to, split_number);
        else if (which.isInt16()) executeIntType<Int16>(icolumn, vec_to, split_number);
        else if (which.isInt32()) executeIntType<Int32>(icolumn, vec_to, split_number);
        else if (which.isInt64()) executeIntType<Int64>(icolumn, vec_to, split_number);
        else if (which.isEnum8()) executeIntType<Int8>(icolumn, vec_to, split_number);
        else if (which.isEnum16()) executeIntType<Int16>(icolumn, vec_to, split_number);
        else if (which.isDate()) executeIntType<UInt16>(icolumn, vec_to, split_number);
        else if (which.isDateTime()) executeIntType<UInt32>(icolumn, vec_to, split_number);
        else if (which.isString()) executeString(icolumn, vec_to, split_number);
        else if (which.isFixedString()) executeString(icolumn, vec_to, split_number);
        else
            throw Exception("Unexpected type " + from_type->getName() + " of argument of function " + getName(),
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }

    Int64 getSplitNumber(const ColumnPtr & column_split_number) const
    {
        Int64 split_number = 0;
        auto column_split_number_ptr = column_split_number.get();
        if (const ColumnConst * column_split_number_const = checkAndGetColumn<ColumnConst>(column_split_number_ptr))
        {
            split_number = column_split_number_const->getInt(0);
        }
        else if(const ColumnInt64 * column_split_number_int64 = checkAndGetColumn<ColumnInt64>(column_split_number_ptr))
        {
            split_number = column_split_number_int64->getInt(0);
        }
        else if(const ColumnInt32 * column_split_number_int32 = checkAndGetColumn<ColumnInt32>(column_split_number_ptr))
        {
            split_number = column_split_number_int32->getInt(0);
        }
        else if(const ColumnInt16 * column_split_number_int16 = checkAndGetColumn<ColumnInt16>(column_split_number_ptr))
        {
            split_number = column_split_number_int16->getInt(0);
        }
        else if(const ColumnInt8 * column_split_number_int8 = checkAndGetColumn<ColumnInt8>(column_split_number_ptr))
        {
            split_number = column_split_number_int8->getInt(0);
        }
        else if(const ColumnUInt64 * column_split_number_uint64 = checkAndGetColumn<ColumnUInt64>(column_split_number_ptr))
        {
            split_number = column_split_number_uint64->getInt(0);
        }
        else if(const ColumnUInt32 * column_split_number_uint32 = checkAndGetColumn<ColumnUInt32>(column_split_number_ptr))
        {
            split_number = column_split_number_uint32->getInt(0);
        }
        else if(const ColumnUInt16 * column_split_number_uint16 = checkAndGetColumn<ColumnUInt16>(column_split_number_ptr))
        {
            split_number = column_split_number_uint16->getInt(0);
        }
        else if(const ColumnUInt8 * column_split_number_uint8 = checkAndGetColumn<ColumnUInt8>(column_split_number_ptr))
        {
            split_number = column_split_number_uint8->getInt(0);
        }
        return split_number;
    }

public:
    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &,  size_t input_rows_count) const override
    {
        ColumnPtr column_split_number = arguments[1].column;

        Int64 split_number = getSplitNumber(column_split_number);
        if (split_number <= 0)
            throw Exception(
                "Illegal split_number of function " + getName() + ", should be greater than 0" ,
                ErrorCodes::ILLEGAL_COLUMN);

        auto col_to = ColumnVector<ToType>::create(input_rows_count);
        typename ColumnVector<ToType>::Container & vec_to = col_to->getData();
        const ColumnWithTypeAndName & col = arguments[0];

        executeAny(col.type.get(), col.column.get(), vec_to, split_number);

        return col_to;
    }
};

void registerFunctionDtsPartition(FunctionFactory & factory)
{
    factory.registerFunction<FunctionDtsPartition>();
}

}
