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

#include "NativeChunkInputStream.h"

#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <IO/ReadHelpers.h>
#include <Processors/Transforms/AggregatingTransform.h>
#include <Common/typeid_cast.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int INCORRECT_INDEX;
    extern const int LOGICAL_ERROR;
    extern const int CANNOT_READ_ALL_DATA;
}

NativeChunkInputStream::NativeChunkInputStream(ReadBuffer & istr_, const Block & header_) : istr(istr_), header(header_)
{
}

void NativeChunkInputStream::readData(
    const IDataType & type, ColumnPtr & column, ReadBuffer & istr, size_t rows, double avg_value_size_hint)
{
    ISerialization::DeserializeBinaryBulkSettings settings;
    settings.getter = [&](ISerialization::SubstreamPath) -> ReadBuffer * { return &istr; };
    settings.avg_value_size_hint = avg_value_size_hint;
    settings.position_independent_encoding = false;
    settings.native_format = true;

    ISerialization::DeserializeBinaryBulkStatePtr state;
    auto serialization = type.getDefaultSerialization();

    serialization->deserializeBinaryBulkStatePrefix(settings, state);
    serialization->deserializeBinaryBulkWithMultipleStreams(column, rows, settings, state, nullptr);

    if (column->size() != rows)
        throw Exception(
            "Cannot read all data in NativeChunkInputStream. Rows read: " + toString(column->size()) + ". Rows expected: " + toString(rows)
                + ".",
            ErrorCodes::CANNOT_READ_ALL_DATA);
}

Chunk NativeChunkInputStream::readImpl()
{
    Chunk res;
    if (istr.eof())
    {
        return res;
    }

    /// chunk info
    UInt8 has_chunk_info;
    readVarUInt(has_chunk_info, istr);
    if (has_chunk_info == 1)
    {
        UInt8 chunk_info_type;
        readVarUInt(chunk_info_type, istr);
        // todo:: current we only support AggregatedChunkInfo
        if (chunk_info_type == static_cast<UInt8>(ChunkInfo::Type::AggregatedChunkInfo))
        {
            auto chunk_info = std::make_shared<AggregatedChunkInfo>();
            chunk_info->read(istr);
            res.setChunkInfo(chunk_info);
        }
    }
    /// Dimensions
    size_t col_num = 0;
    size_t row_num = 0;

    readVarUInt(col_num, istr);
    readVarUInt(row_num, istr);

    Columns columns;
    for (size_t i = 0; i < col_num; ++i)
    {
        DataTypePtr data_type = header.getDataTypes().at(i);

        /// Data
        ColumnPtr read_column = data_type->createColumn();

        double avg_value_size_hint = avg_value_size_hints.empty() ? 0 : avg_value_size_hints[i];
        if (row_num) /// If no row_num, nothing to read.
            readData(*data_type, read_column, istr, row_num, avg_value_size_hint);

        columns.emplace_back(std::move(read_column));
    }
    res.setColumns(columns, row_num);
    return res;
}
}

