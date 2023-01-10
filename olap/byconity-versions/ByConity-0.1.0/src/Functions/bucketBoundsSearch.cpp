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

#include <Columns/ColumnArray.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnsNumber.h>
#include <Core/ColumnNumbers.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Functions/FunctionFactory.h>
#include <Functions/IFunction.h>
#include <Statistics/BucketBoundsImpl.h>
#include <Statistics/TypeMacros.h>
#include <Statistics/TypeUtils.h>
#include <Common/typeid_cast.h>


namespace DB
{

using namespace Statistics;

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ILLEGAL_COLUMN;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int ZERO_ARRAY_OR_TUPLE_INDEX;
    extern const int NOT_IMPLEMENTED;
}

template <typename T>
class PreparedFunctionBucketBoundsSearch final : public IExecutableFunction
{
public:
    explicit PreparedFunctionBucketBoundsSearch(BucketBoundsImpl<T> && bucket_bounds_) : bucket_bounds(std::move(bucket_bounds_)) { }

    String getName() const override { return "bucket_bounds_search"; }
    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        (void)result_type;
        assert(arguments.size() == 2);
        auto raw_data_column = arguments[1].column;
        auto data_column = checkAndGetColumn<ColumnVector<T>>(raw_data_column.get());
        if (!data_column)
        {
            throw Exception(fmt::format(FMT_STRING("Unsupported Column: {}"), arguments[1].name), ErrorCodes::NOT_IMPLEMENTED);
        }
        auto & data_container = data_column->getData();

        auto out_column = ColumnUInt16::create(input_rows_count);
        auto & out_container = out_column->getData();
        for (size_t i = 0; i < input_rows_count; ++i)
        {
            auto data = data_container[i];
            auto res = bucket_bounds.binarySearchBucket(data);
            out_container[i] = res;
        }
        return out_column;
    }

private:
    BucketBoundsImpl<T> bucket_bounds;
};

template <typename T>
class FunctionBucketBoundsSearch final : public IFunctionBase
{
public:
    FunctionBucketBoundsSearch(const DB::ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type_)
    {
        for (const auto & arg : arguments)
        {
            arguments_types.push_back(arg.type);
        }
        result_type = result_type_;
    }

    String getName() const override { return "bucket_bounds_search"; }
    const DataTypes & getArgumentTypes() const override { return arguments_types; }
    const DataTypePtr & getResultType() const override { return result_type; }

    ExecutableFunctionPtr prepare(const ColumnsWithTypeAndName & sample_arguments) const override
    {
        BucketBoundsImpl<T> bucket_bounds;
        if (sample_arguments.size() != 2)
        {
            throw Exception("unexpected sample block size", ErrorCodes::LOGICAL_ERROR);
        }
        auto b64 = sample_arguments[0].column->getDataAt(0);
        bucket_bounds.deserialize(base64Decode(b64.operator std::string_view()));
        return std::make_shared<PreparedFunctionBucketBoundsSearch<T>>(std::move(bucket_bounds));
    }
    bool hasInformationAboutMonotonicity() const override { return true; }
    Monotonicity getMonotonicityForRange(const IDataType & /*type*/, const Field & /*left*/, const Field & /*right*/) const override
    {
        return Monotonicity(true, true, true);
    }


private:
    DataTypes arguments_types;
    DataTypePtr result_type;
};

class FunctionBucketBoundsSearchBuilder final : public IFunctionOverloadResolver
{
public:
    static constexpr auto name = "bucket_bounds_search";
    String getName() const override { return name; }

    static FunctionOverloadResolverPtr create(ContextPtr) { return std::make_shared<FunctionBucketBoundsSearchBuilder>(); }
    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override
    {
        return {
            0,
        };
    }

    FunctionBasePtr buildImpl(const DB::ColumnsWithTypeAndName & arguments, const DB::DataTypePtr & result_type) const override
    {
        if (arguments.size() != 2)
        {
            throw Exception("unmatched size of arguments: must be 2", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
        if (arguments[0].type->getTypeId() != TypeIndex::String)
        {
            throw Exception("the first arguments must be base64 format of bucket bounds", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }

        auto b64 = arguments[0].column->getDataAt(0).operator std::string_view();
        auto blob_header = base64Decode(b64.substr(0, 8));
        auto serde_data_type = BucketBounds::getSerdeDataTypeFromBlob(blob_header);
        switch (serde_data_type)
        {
#define HANDLE_CASE(TYPE) \
    case SerdeDataType::TYPE: { \
        return std::make_shared<FunctionBucketBoundsSearch<TYPE>>(arguments, result_type); \
    }
            FIXED_TYPE_ITERATE(HANDLE_CASE)
#undef HANDLE_CASE
            default:
                throw Exception("unimplemented data type", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        }
    }

    size_t getNumberOfArguments() const override { return 2; }

    DataTypePtr getReturnTypeImpl(const DB::ColumnsWithTypeAndName & arguments) const override
    {
        (void)arguments;
        return std::make_shared<DataTypeUInt16>();
    }
};

void registerFunctionBucketBoundsSearch(FunctionFactory & factory)
{
    factory.registerFunction<FunctionBucketBoundsSearchBuilder>();
}

}
