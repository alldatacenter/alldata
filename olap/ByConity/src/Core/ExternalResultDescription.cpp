/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include "ExternalResultDescription.h"
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeDateTime64.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeFixedString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesDecimal.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeEnum.h>
#include <Common/typeid_cast.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int UNKNOWN_TYPE;
}

void ExternalResultDescription::init(const Block & sample_block_)
{
    sample_block = sample_block_;

    types.reserve(sample_block.columns());

    for (auto & elem : sample_block)
    {
        /// If default value for column was not provided, use default from data type.
        if (elem.column->empty())
            elem.column = elem.type->createColumnConstWithDefaultValue(1)->convertToFullColumnIfConst();

        bool is_nullable = elem.type->isNullable();
        DataTypePtr type_not_nullable = removeNullable(elem.type);
        const IDataType * type = type_not_nullable.get();

        WhichDataType which(type);

        if (which.isUInt8())
            types.emplace_back(ValueType::vtUInt8, is_nullable);
        else if (which.isUInt16())
            types.emplace_back(ValueType::vtUInt16, is_nullable);
        else if (which.isUInt32())
            types.emplace_back(ValueType::vtUInt32, is_nullable);
        else if (which.isUInt64())
            types.emplace_back(ValueType::vtUInt64, is_nullable);
        else if (which.isInt8())
            types.emplace_back(ValueType::vtInt8, is_nullable);
        else if (which.isInt16())
            types.emplace_back(ValueType::vtInt16, is_nullable);
        else if (which.isInt32())
            types.emplace_back(ValueType::vtInt32, is_nullable);
        else if (which.isInt64())
            types.emplace_back(ValueType::vtInt64, is_nullable);
        else if (which.isFloat32())
            types.emplace_back(ValueType::vtFloat32, is_nullable);
        else if (which.isFloat64())
            types.emplace_back(ValueType::vtFloat64, is_nullable);
        else if (which.isString())
            types.emplace_back(ValueType::vtString, is_nullable);
        else if (which.isDate())
            types.emplace_back(ValueType::vtDate, is_nullable);
        else if (which.isDate32())
            types.emplace_back(ValueType::vtDate32, is_nullable);
        else if (which.isDateTime())
            types.emplace_back(ValueType::vtDateTime, is_nullable);
        else if (which.isUUID())
            types.emplace_back(ValueType::vtUUID, is_nullable);
        else if (which.isEnum8())
            types.emplace_back(ValueType::vtEnum8, is_nullable);
        else if (which.isEnum16())
            types.emplace_back(ValueType::vtEnum16, is_nullable);
        else if (which.isDateTime64())
            types.emplace_back(ValueType::vtDateTime64, is_nullable);
        else if (which.isDecimal32())
            types.emplace_back(ValueType::vtDecimal32, is_nullable);
        else if (which.isDecimal64())
            types.emplace_back(ValueType::vtDecimal64, is_nullable);
        else if (which.isDecimal128())
            types.emplace_back(ValueType::vtDecimal128, is_nullable);
        else if (which.isDecimal256())
            types.emplace_back(ValueType::vtDecimal256, is_nullable);
        else if (which.isArray())
            types.emplace_back(ValueType::vtArray, is_nullable);
        else if (which.isFixedString())
            types.emplace_back(ValueType::vtFixedString, is_nullable);
        else
            throw Exception{"Unsupported type " + type->getName(), ErrorCodes::UNKNOWN_TYPE};
    }
}

}
