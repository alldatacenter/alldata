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

#include <DataStreams/IBlockOutputStream.h>
#include <DataTypes/IDataType.h>
#include <Processors/Chunk.h>
#include <common/types.h>

namespace DB
{
class WriteBuffer;
class CompressedWriteBuffer;


/** Serializes the stream of chunk in their native binary format.
  */
class NativeChunkOutputStream
{
public:
    NativeChunkOutputStream(WriteBuffer & ostr_, UInt64 client_revision_, const Block & header_, bool remove_low_cardinality_ = false);

    void write(const Chunk & chunk);

private:
    WriteBuffer & ostr;
    UInt64 client_revision;
    Block header;
    bool remove_low_cardinality;
};

}
