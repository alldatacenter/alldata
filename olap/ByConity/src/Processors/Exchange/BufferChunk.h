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

#include <vector>
#include <Columns/IColumn.h>
#include <Core/Block.h>
#include <Processors/Chunk.h>
#include <Poco/Logger.h>


namespace DB
{
class BufferChunk
{
public:
    BufferChunk(const Block & header, UInt64 threshold_in_bytes, UInt64 threshold_in_row_num);
    Chunk add(Chunk chunk);
    Chunk flush(bool force);
    void resetBuffer();

private:
    const Block & header;
    size_t column_num;
    UInt64 threshold_in_bytes;
    UInt64 threshold_in_row_num;
    MutableColumns buffer_columns;
    ChunkInfoPtr current_chunk_info;
    Poco::Logger * logger;
    inline size_t bufferBytes() const;
    inline bool compareBufferChunkInfo(const ChunkInfoPtr & chunk_info) const;
};

}
