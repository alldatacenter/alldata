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

#include <IO/ZstdDeflatingAppendableWriteBuffer.h>
#include <Common/MemoryTracker.h>
#include <Common/Exception.h>
#include <IO/ReadBufferFromFile.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ZSTD_ENCODER_FAILED;
}

ZstdDeflatingAppendableWriteBuffer::ZstdDeflatingAppendableWriteBuffer(
    WriteBufferFromFile & out_, int compression_level, bool append_to_existing_stream_,
    size_t buf_size, char * existing_memory, size_t alignment)
    : BufferWithOwnMemory<WriteBuffer>(buf_size, existing_memory, alignment)
    , out(out_)
    , append_to_existing_stream(append_to_existing_stream_)
{
    cctx = ZSTD_createCCtx();
    if (cctx == nullptr)
        throw Exception(ErrorCodes::ZSTD_ENCODER_FAILED, "zstd stream encoder init failed: zstd version: {}", ZSTD_VERSION_STRING);
    size_t ret = ZSTD_CCtx_setParameter(cctx, ZSTD_c_compressionLevel, compression_level);
    if (ZSTD_isError(ret))
        throw Exception(ErrorCodes::ZSTD_ENCODER_FAILED, "zstd stream encoder option setting failed: error code: {}; zstd version: {}", ret, ZSTD_VERSION_STRING);

    input = {nullptr, 0, 0};
    output = {nullptr, 0, 0};
}

void ZstdDeflatingAppendableWriteBuffer::nextImpl()
{
    if (!offset())
        return;

    input.src = reinterpret_cast<unsigned char *>(working_buffer.begin());
    input.size = offset();
    input.pos = 0;

    if (first_write && append_to_existing_stream && isNeedToAddEmptyBlock())
    {
        addEmptyBlock();
        first_write = false;
    }

    try
    {
        bool ended = false;
        do
        {
            out.nextIfAtEnd();

            output.dst = reinterpret_cast<unsigned char *>(out.buffer().begin());
            output.size = out.buffer().size();
            output.pos = out.offset();

            size_t compression_result = ZSTD_compressStream2(cctx, &output, &input, ZSTD_e_flush);
            if (ZSTD_isError(compression_result))
                throw Exception(
                    ErrorCodes::ZSTD_ENCODER_FAILED, "Zstd stream encoding failed: error code: {}; zstd version: {}", ZSTD_getErrorName(compression_result), ZSTD_VERSION_STRING);

            out.position() = out.buffer().begin() + output.pos;

            bool everything_was_compressed = (input.pos == input.size);
            bool everything_was_flushed = compression_result == 0;

            ended = everything_was_compressed && everything_was_flushed;
        } while (!ended);
    }
    catch (...)
    {
        /// Do not try to write next time after exception.
        out.position() = out.buffer().begin();
        throw;
    }
}

void ZstdDeflatingAppendableWriteBuffer::finish()
{
    if (finished || first_write)
    {
        /// Nothing was written or we have already finished
        return;
    }

    try
    {
        finishImpl();
        out.finalize();
        finished = true;
    }
    catch (...)
    {
        /// Do not try to flush next time after exception.
        out.position() = out.buffer().begin();
        finished = true;
        throw;
    }
}


ZstdDeflatingAppendableWriteBuffer::~ZstdDeflatingAppendableWriteBuffer()
{
    /// FIXME move final flush into the caller
    MemoryTracker::LockExceptionInThread lock(VariableContext::Global);

    finish();

    try
    {
        int err = ZSTD_freeCCtx(cctx);
        /// This is just in case, since it is impossible to get an error by using this wrapper.
        if (unlikely(err))
            throw Exception(ErrorCodes::ZSTD_ENCODER_FAILED, "ZSTD_freeCCtx failed: error: '{}'; zstd version: {}", ZSTD_getErrorName(err), ZSTD_VERSION_STRING);
    }
    catch (...)
    {
        /// It is OK not to terminate under an error from ZSTD_freeCCtx()
        /// since all data already written to the stream.
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

void ZstdDeflatingAppendableWriteBuffer::finishImpl()
{
    next();

    out.nextIfAtEnd();

    input.src = reinterpret_cast<unsigned char *>(working_buffer.begin());
    input.size = offset();
    input.pos = 0;

    output.dst = reinterpret_cast<unsigned char *>(out.buffer().begin());
    output.size = out.buffer().size();
    output.pos = out.offset();

    size_t remaining = ZSTD_compressStream2(cctx, &output, &input, ZSTD_e_end);
    while (remaining != 0)
    {
        if (ZSTD_isError(remaining))
            throw Exception(ErrorCodes::ZSTD_ENCODER_FAILED, "Zstd stream encoder end failed: error: '{}' zstd version: {}", ZSTD_getErrorName(remaining), ZSTD_VERSION_STRING);

        remaining = ZSTD_compressStream2(cctx, &output, &input, ZSTD_e_end);
    }
    out.position() = out.buffer().begin() + output.pos;
}

void ZstdDeflatingAppendableWriteBuffer::addEmptyBlock()
{
    /// HACK: https://github.com/facebook/zstd/issues/2090#issuecomment-620158967
    if (out.buffer().size() - out.offset() < ZSTD_CORRECT_TERMINATION_LAST_BLOCK.size())
        out.next();

    std::memcpy(out.buffer().begin() + out.offset(), ZSTD_CORRECT_TERMINATION_LAST_BLOCK.data(), ZSTD_CORRECT_TERMINATION_LAST_BLOCK.size());

    out.position() = out.buffer().begin() + out.offset() + ZSTD_CORRECT_TERMINATION_LAST_BLOCK.size();
}

bool ZstdDeflatingAppendableWriteBuffer::isNeedToAddEmptyBlock()
{
    ReadBufferFromFile reader(out.getFileName());
    auto fsize = reader.size();
    if (fsize > 3)
    {
        std::array<char, 3> result;
        reader.seek(fsize - 3, SEEK_SET);
        reader.readStrict(result.data(), 3);

        /// If we don't have correct block in the end, then we need to add it manually.
        /// NOTE: maybe we can have the same bytes in case of data corruption/unfinished write.
        /// But in this case file still corrupted and we have to remove it.
        return result != ZSTD_CORRECT_TERMINATION_LAST_BLOCK;
    }
    else if (fsize > 0)
    {
        throw Exception(
            ErrorCodes::ZSTD_ENCODER_FAILED,
            "Trying to write to non-empty file '{}' with tiny size {}. It can lead to data corruption",
            out.getFileName(), fsize);
    }
    return false;
}

}
