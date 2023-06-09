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
#include <Columns/IColumn.h>
#include <Core/NamesAndTypes.h>
#include <DataTypes/IDataType.h>
#include <Interpreters/Context.h>
#include <Storages/SelectQueryInfo.h>

namespace DB
{
void createHiveBucketColumn(Block & block, const Block & bucket_columns, const Int64 & total_bucket_num, const ContextPtr & context);
Int64 getHiveBucket(DataTypePtr & type, ColumnPtr & column, String & name, const Int64 & total_bucket_num);
Int64 hashBytes(const String & str, int start, int length);
Int64 getBuckHashCode(DataTypePtr & type, ColumnPtr & column, String & name);
ColumnPtr createColumnWithHiveHash(Block & block, const Block & bucket_columns, const Int64 & total_bucket_num);
ASTs extractBucketColumnExpression(const ASTs & conditions, Names bucket_columns);

}
