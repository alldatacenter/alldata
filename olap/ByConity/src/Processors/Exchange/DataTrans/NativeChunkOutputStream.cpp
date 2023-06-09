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

#include "NativeChunkOutputStream.h"
#include <Compression/CompressedWriteBuffer.h>
#include <Core/Block.h>
#include <Core/ProtocolDefines.h>
#include <DataTypes/DataTypeLowCardinality.h>
#include <IO/VarInt.h>
#include <Common/typeid_cast.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}


NativeChunkOutputStream::NativeChunkOutputStream(
    WriteBuffer & ostr_, UInt64 client_revision_, const Block & header_, bool remove_low_cardinality_)
    : ostr(ostr_), client_revision(client_revision_), header(header_), remove_low_cardinality(remove_low_cardinality_)
{
}

static void writeData(const IDataType & type, const ColumnPtr & column, WriteBuffer & ostr, UInt64 offset, UInt64 limit)
{
    /** If there are columns-constants - then we materialize them.
      * (Since the data type does not know how to serialize / deserialize constants.)
      */
    ColumnPtr full_column = column->convertToFullColumnIfConst();

    ISerialization::SerializeBinaryBulkSettings settings;
    settings.getter = [&ostr](ISerialization::SubstreamPath) -> WriteBuffer * { return &ostr; };
    settings.position_independent_encoding = false;
    settings.low_cardinality_max_dictionary_size = 0; //-V1048

    auto serialization = type.getDefaultSerialization();

    ISerialization::SerializeBinaryBulkStatePtr state;
    serialization->serializeBinaryBulkStatePrefix(settings, state);
    serialization->serializeBinaryBulkWithMultipleStreams(*full_column, offset, limit, settings, state);
    serialization->serializeBinaryBulkStateSuffix(settings, state);
}


void NativeChunkOutputStream::write(const Chunk & chunk)
{
    /// chunk info
    auto chunk_info = chunk.getChunkInfo();
    if (chunk_info)
    {
        writeVarUInt(1, ostr);
        writeVarUInt(static_cast<UInt8>(chunk_info->getType()), ostr);
        chunk_info->write(ostr);
    }
    else
    {
        writeVarUInt(0, ostr);
    }
    /// Dimensions
    size_t columns = chunk.getNumColumns();
    size_t rows = chunk.getNumRows();

    writeVarUInt(columns, ostr);
    writeVarUInt(rows, ostr);

    for (size_t i = 0; i < columns; ++i)
    {
        DataTypePtr data_type = header.getDataTypes().at(i);
        ColumnPtr column_ptr = chunk.getColumns()[i];
        /// Send data to old clients without low cardinality type.
        if (remove_low_cardinality || (client_revision && client_revision < DBMS_MIN_REVISION_WITH_LOW_CARDINALITY_TYPE))
        {
            column_ptr = recursiveRemoveLowCardinality(column_ptr);
            data_type = recursiveRemoveLowCardinality(data_type);
        }

        /// Name/Type, we don't need write name/type here.
        /// Data
        if (rows) /// Zero items of data is always represented as zero number of bytes.
            writeData(*data_type, column_ptr, ostr, 0, 0);
    }
}
}
