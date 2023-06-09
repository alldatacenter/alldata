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

#include <IO/ZstdInflatingReadBuffer.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int ZSTD_DECODER_FAILED;
}

ZstdInflatingReadBuffer::ZstdInflatingReadBuffer(std::unique_ptr<ReadBuffer> in_, size_t buf_size, char * existing_memory, size_t alignment)
    : BufferWithOwnMemory<ReadBuffer>(buf_size, existing_memory, alignment), in(std::move(in_))
{
    dctx = ZSTD_createDCtx();
    input = {nullptr, 0, 0};
    output = {nullptr, 0, 0};

    if (dctx == nullptr)
    {
        throw Exception(ErrorCodes::ZSTD_DECODER_FAILED, "zstd_stream_decoder init failed: zstd version: {}", ZSTD_VERSION_STRING);
    }
}

ZstdInflatingReadBuffer::~ZstdInflatingReadBuffer()
{
    ZSTD_freeDCtx(dctx);
}

bool ZstdInflatingReadBuffer::nextImpl()
{
    if (eof)
        return false;

    if (input.pos >= input.size)
    {
        in->nextIfAtEnd();
        input.src = reinterpret_cast<unsigned char *>(in->position());
        input.pos = 0;
        input.size = in->buffer().end() - in->position();
    }

    output.dst = reinterpret_cast<unsigned char *>(internal_buffer.begin());
    output.size = internal_buffer.size();
    output.pos = 0;

    size_t ret = ZSTD_decompressStream(dctx, &output, &input);
    if (ZSTD_isError(ret))
        throw Exception(
            ErrorCodes::ZSTD_DECODER_FAILED, "Zstd stream encoding failed: error code: {}; zstd version: {}", ZSTD_getErrorName(ret), ZSTD_VERSION_STRING);

    /// Check that something has changed after decompress (input or output position)
    assert(in->eof() || output.pos > 0 || in->position() < in->buffer().begin() + input.pos);

    in->position() = in->buffer().begin() + input.pos;
    working_buffer.resize(output.pos);

    if (in->eof())
    {
        eof = true;
        return !working_buffer.empty();
    }
    else if (output.pos == 0)
    {
        /// It is possible, that input buffer is not at eof yet, but nothing was decompressed in current iteration.
        /// But there are cases, when such behaviour is not allowed - i.e. if input buffer is not eof, then
        /// it has to be guaranteed that working_buffer is not empty. So if it is empty, continue.
        return nextImpl();
    }

    return true;
}

}
