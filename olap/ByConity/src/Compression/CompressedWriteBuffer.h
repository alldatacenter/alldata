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

#pragma once

#include <memory>

#include <Common/PODArray.h>

#include <IO/WriteBuffer.h>
#include <IO/BufferWithOwnMemory.h>
#include <Compression/ICompressionCodec.h>
#include <Compression/CompressionFactory.h>


namespace DB
{

class CompressedWriteBuffer : public BufferWithOwnMemory<WriteBuffer>
{
private:
    WriteBuffer & out;
    CompressionCodecPtr codec;

    PODArray<char> compressed_buffer;

    void nextImpl() override;

public:
    CompressedWriteBuffer(
        WriteBuffer & out_,
        CompressionCodecPtr codec_ = CompressionCodecFactory::instance().getDefaultCodec(),
        size_t buf_size = DBMS_DEFAULT_BUFFER_SIZE);

    /// The amount of compressed data
    size_t getCompressedBytes()
    {
        nextIfAtEnd();
        return out.count();
    }

    /// How many uncompressed bytes were written to the buffer
    size_t getUncompressedBytes()
    {
        return count();
    }

    /// How many bytes are in the buffer (not yet compressed)
    size_t getRemainingBytes()
    {
        nextIfAtEnd();
        return offset();
    }

    ~CompressedWriteBuffer() override;

    //@ByteMap
    virtual void deepCopyTo(/*CompressedWriteBuffer*/BufferBase& target) const override;
};

}
