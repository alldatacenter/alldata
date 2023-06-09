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

#include <Processors/Transforms/AssignUniqueIdTransform.h>

#include <Columns/ColumnsNumber.h>
#include <DataTypes/DataTypesNumber.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int INCORRECT_RESULT_OF_SCALAR_SUBQUERY;
}

Block AssignUniqueIdTransform::transformHeader(Block header, String symbol)
{
    auto type_int = std::make_shared<DataTypeUInt64>();
    MutableColumnPtr column = type_int->createColumn();
    ColumnWithTypeAndName unique{std::move(column), type_int, symbol};
    header.insert(unique);
    return header;
}

AssignUniqueIdTransform::AssignUniqueIdTransform(const Block & header_, String symbol_)
    : ISimpleTransform(header_, transformHeader(header_, symbol_), false), symbol(symbol_)
{
    unique_value_mask = ((static_cast<Int64>(2)) << 54) | ((static_cast<Int64>(1)) << 40);
    requestValues();
}

void AssignUniqueIdTransform::transform(Chunk & chunk)
{
    size_t num_rows = chunk.getNumRows();
    std::vector<UInt64> value;
    for (size_t i = 0; i < num_rows; ++i)
    {
        if (row_id_counter >= max_row_id_counter_value)
        {
            requestValues();
        }
        Int64 row_id = row_id_counter++;
        if ((row_id & unique_value_mask) != 0)
        {
            throw Exception("RowId and uniqueValue mask overlaps", ErrorCodes::LOGICAL_ERROR);
        }
        value.emplace_back(unique_value_mask | row_id);
    }

    auto type_int = std::make_shared<DataTypeUInt64>();
    MutableColumnPtr column = type_int->createColumn();
    dynamic_cast<ColumnUInt64 *>(&*column)->getData().assign(value.begin(), value.end());
    chunk.addColumn(std::move(column));
}

Int64 AssignUniqueIdTransform::ROW_IDS_PER_REQUEST = 1L << 20L;
Int64 AssignUniqueIdTransform::MAX_ROW_ID = 1L << 40L;

void AssignUniqueIdTransform::requestValues()
{
    row_id_counter = row_id_pool.fetch_add(ROW_IDS_PER_REQUEST);
    max_row_id_counter_value = fmin(row_id_counter + ROW_IDS_PER_REQUEST, MAX_ROW_ID);
    if (row_id_counter >= MAX_ROW_ID)
    {
        throw Exception("Unique row id exceeds a limit: " + std::to_string(MAX_ROW_ID), ErrorCodes::LOGICAL_ERROR);
    }
}

}
