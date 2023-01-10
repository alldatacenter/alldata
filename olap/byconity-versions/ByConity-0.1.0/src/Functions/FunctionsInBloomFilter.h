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
#include <Functions/FunctionHelpers.h>
#include <Functions/IFunction.h>
#include <Interpreters/BloomFilter.h>
#include <Interpreters/RuntimeFilter/RuntimeFilterManager.h>
#include <Common/typeid_cast.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
}

class FunctionInBloomFilter : public IFunction
{
public:
    static constexpr auto name = "bloomFilterExist";

    static FunctionPtr create(ContextPtr context) { return std::make_shared<FunctionInBloomFilter>(std::move(context)); }

    explicit FunctionInBloomFilter(ContextPtr context_) : context(std::move(context_)), log(&Poco::Logger::get("FunctionInBloomFilter")) { }

    String getName() const override { return name; }

    bool isVariadic() const override { return false; }

    size_t getNumberOfArguments() const override { return 3; }

    DataTypePtr getReturnTypeImpl(const DataTypes & arguments) const override
    {
        if (!checkAndGetDataType<DataTypeString>(arguments[0].get()))
            throw Exception("Function " + getName() + " first argument string is required", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        if (!checkAndGetDataType<DataTypeString>(arguments[1].get()))
            throw Exception("Function " + getName() + " first argument string is required", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return std::make_shared<DataTypeUInt8>();
    }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr &, size_t input_rows_count) const override
    {
        const ColumnWithTypeAndName & join_key = arguments[2];
        String rf_key = arguments[0].column->size() == 0 ? "" : arguments[0].column->getDataAt(0).toString();
        String column_key = arguments[1].column->size() == 0 ? "" : arguments[1].column->getDataAt(0).toString();

        /// When dynamic mode is enabled each time execute this function should get runtime filter otherwise get runtime filter only once
        if ((!context->getSettingsRef().runtime_filter_dynamic_mode && !runtime_filter)
            || context->getSettingsRef().runtime_filter_dynamic_mode)
        {
            runtime_filter = RuntimeFilterManager::getInstance().getRuntimeFilter(rf_key, 0);
            if (runtime_filter)
                LOG_DEBUG(log, "Runtime filter with key " + rf_key + " success");
        }

        if (!runtime_filter)
        {
            LOG_DEBUG(log, "RuntimeFilter can not find runtime filter with key-" + rf_key);
            Field field = UInt8{1};
            return DataTypeUInt8().createColumnConst(input_rows_count, field);
        }

        auto bloom_filter = runtime_filter->getBloomFilterByColumn(column_key);
        if (!bloom_filter)
        {
            LOG_DEBUG(log, "RuntimeFilter Can not find bloom filter with key-" + column_key);
            Field field = UInt8{1};
            return DataTypeUInt8().createColumnConst(input_rows_count, field);
        }

        auto col_to = ColumnVector<UInt8>::create();
        auto & vec_to = col_to->getData();
        vec_to.resize(input_rows_count);
        for (size_t i = 0; i < input_rows_count; ++i)
        {
            if (const auto * nullable = checkAndGetColumn<ColumnNullable>(join_key.column.get()))
            {
                StringRef key = nullable->isNullAt(i) ? "NULL" : nullable->getNestedColumn().getDataAt(i);
                vec_to[i] = bloom_filter->probeKey(key);
            }
            else
            {
                StringRef key = join_key.column->getDataAt(i);
                vec_to[i] = bloom_filter->probeKey(key);
            }
        }
        return col_to;
    }

private:
    ContextPtr context;
    mutable RuntimeFilterPtr runtime_filter;
    Poco::Logger * log;
};
}
