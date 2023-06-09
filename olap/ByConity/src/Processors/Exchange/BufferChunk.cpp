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

#include <common/types.h>

#include <Columns/IColumn.h>
#include <Processors/Exchange/BufferChunk.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <Processors/Chunk.h>

namespace DB
{
BufferChunk::BufferChunk(
    const Block & header_, UInt64 threshold_in_bytes_, UInt64 threshold_in_row_num_)
    : header(header_)
    , column_num(header_.getColumns().size())
    , threshold_in_bytes(threshold_in_bytes_)
    , threshold_in_row_num(threshold_in_row_num_)
    , logger(&Poco::Logger::get("BufferChunk"))
{
    resetBuffer();
}

Chunk BufferChunk::flush(bool force)
{
    size_t rows = buffer_columns[0]->size();
    Chunk empty;
    if (rows == 0)
        return empty;

    if (!force)
    {
        if (bufferBytes() < threshold_in_bytes && rows < threshold_in_row_num)
            return empty;
    }

    LOG_TRACE(logger, "flush buffer, force: {}, row: {}", force, rows);

    Chunk chunk(std::move(buffer_columns), rows, std::move(current_chunk_info));
    current_chunk_info = ChunkInfoPtr();
    resetBuffer();

    return chunk;
}

bool BufferChunk::compareBufferChunkInfo(const ChunkInfoPtr & chunk_info) const
{
    return ((current_chunk_info && chunk_info && *current_chunk_info == *chunk_info) || (!current_chunk_info && !chunk_info));
}


void BufferChunk::resetBuffer()
{
    buffer_columns = header.cloneEmptyColumns();
}


Chunk BufferChunk::add(Chunk chunk)
{
    if (chunk.getNumRows() > threshold_in_row_num || chunk.bytes() > threshold_in_bytes || !compareBufferChunkInfo(chunk.getChunkInfo()))
    {
        auto res = flush(true);
        current_chunk_info = std::move(chunk.getChunkInfo());
        buffer_columns = chunk.mutateColumns();
        return res;
    }

    auto res = flush(false);
    if (res)
    {
        current_chunk_info = std::move(chunk.getChunkInfo());
        buffer_columns = chunk.mutateColumns();
        return res;
    }

    MutableColumns columns = chunk.mutateColumns();
    for (size_t i = 0; i < buffer_columns.size(); i++)
    {
        buffer_columns[i]->insertRangeFrom(*columns[i], 0, columns[i]->size());
    }
    return Chunk();
}

size_t BufferChunk::bufferBytes() const
{
    size_t total = 0;
    for (size_t i = 0; i < column_num; ++i)
    {
        total += buffer_columns[i]->byteSize();
    }
    return total;
}

}
