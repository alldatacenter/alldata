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

#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/ExpressionActions.h>
#include <Interpreters/ExpressionAnalyzer.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/queryToString.h>
#include <Storages/Hive/HiveBucketFilter.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

#define COLUMN_BUCKET_NUMBER "_bucket_number_internal"
#define MAX_VALUE 2147483647

void createHiveBucketColumn(Block & block, const Block & bucket_columns, const Int64 & total_bucket_num, const ContextPtr & /*context*/)
{
    ColumnPtr bucket_number_column;

    bucket_number_column = createColumnWithHiveHash(block, bucket_columns, total_bucket_num);

    block.insert(ColumnWithTypeAndName{std::move(bucket_number_column), std::make_shared<DataTypeUInt64>(), COLUMN_BUCKET_NUMBER});
}

Int64 getHiveBucket(DataTypePtr & type, ColumnPtr & column, String & name, const Int64 & total_bucket_num)
{
    return (getBuckHashCode(type, column, name) & MAX_VALUE) % total_bucket_num;
}

Int64 hashBytes(const String & str, int start, int length)
{
    Int64 result = 0;
    for (int i = start; i < length; ++i)
    {
        result = result * 31 + str[i];
    }

    return result;
}

Int64 getBuckHashCode(DataTypePtr & type, ColumnPtr & column, String & name)
{
    DataTypePtr striped_type = type->isNullable() ? static_cast<const DataTypeNullable *>(type.get())->getNestedType() : type;
    Int64 hashcode = 0;
    LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), " bucket col type = {}", striped_type->getName());
    if (WhichDataType(striped_type).isString())
    {
        hashcode = hashBytes(name, 1, name.length() - 1);
    }
    else if (WhichDataType(striped_type).isInt8() || WhichDataType(striped_type).isInt16() || WhichDataType(striped_type).isInt32())
    {
        hashcode = column->getInt(0);
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), " int  bucket value = {}", hashcode);
    }
    else if (WhichDataType(striped_type).isUInt8() || WhichDataType(striped_type).isUInt16() || WhichDataType(striped_type).isUInt32())
    {
        hashcode = column->getUInt(0);
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), " UInt bucket value = {}", hashcode);
    }
    else if (WhichDataType(striped_type).isUInt64())
    {
        UInt64 bigint_value = column->getUInt(0);
        hashcode = ((bigint_value >> 32) ^ bigint_value);
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), "UInt64 bucket value = {}, hashcode = ", bigint_value, hashcode);
    }
    else if (WhichDataType(striped_type).isInt64())
    {
        Int64 bigint_value = column->getInt(0);
        UInt64 cast_bigint_value = static_cast<UInt64>(bigint_value);
        hashcode = ((cast_bigint_value >> 32) ^ cast_bigint_value);
        LOG_TRACE(
            &Poco::Logger::get("getBuckHashCode"),
            "bigint bucket value = bigintValue {}, cast_bigint_value = = {} hashcode = {}",
            bigint_value,
            cast_bigint_value,
            hashcode);
    }
    else if (WhichDataType(striped_type).isFloat32())
    {
        hashcode = column->getInt(0);
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), "Float32 bucket value = {}", hashcode);
    }
    else if (WhichDataType(striped_type).isDate())
    {
        hashcode = column->getInt(0);
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), "Date bucket value = {}", hashcode);
    }
    else
    {
        throw Exception(
            "Computation of Hive bucket hashCode is not supported for Hive primitive type: " + striped_type->getName() + ".",
            ErrorCodes::BAD_ARGUMENTS);
    }

    return hashcode;
}

ColumnPtr createColumnWithHiveHash(Block & block, const Block & bucket_columns, const Int64 & total_bucket_num)
{
    auto result_column = ColumnUInt64::create();
    auto bucket_column_type = bucket_columns.getByPosition(0).type;

    for (const auto & column : block.getColumnsWithTypeAndName())
    {
        auto col = column.column;
        auto name = column.name;

        LOG_TRACE(
            &Poco::Logger::get("createColumnWithHiveHash"),
            " createColumnWithHiveHash bucket_column_type type = {} col name = {}",
            bucket_column_type->getName(),
            name);
        Int64 hashcode = getHiveBucket(bucket_column_type, col, name, total_bucket_num);
        result_column->insertValue(hashcode);
    }

    return result_column;
}

ASTs extractBucketColumnExpression(const ASTs & conditions, Names bucket_columns)
{
    ASTs res;
    if (conditions.empty())
        return res;

    for (const auto & condition : conditions)
    {
        LOG_TRACE(&Poco::Logger::get("getBuckHashCode"), " condition: {}", queryToString(condition));

        const auto & ast_func = typeid_cast<const ASTFunction *>(condition.get());
        if (!ast_func)
            continue;

        if (ast_func->arguments->children.size() != 2 || ast_func->name != "equals")
            continue;

        const auto & iden = typeid_cast<const ASTIdentifier *>(ast_func->arguments->children[0].get());
        const auto & literal = typeid_cast<const ASTLiteral *>(ast_func->arguments->children[1].get());

        if (iden && literal)
        {
            if (std::find(bucket_columns.begin(), bucket_columns.end(), iden->name()) == bucket_columns.end())
                continue;

            res.push_back(condition);
        }
    }
    return res;
}

}
