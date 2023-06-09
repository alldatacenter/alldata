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

#include <city.h>
#include <string.h>

#include <common/unaligned.h>
#include <common/types.h>

#include "CompressedWriteBuffer.h"
#include <Compression/CompressionFactory.h>

#include <Common/MemorySanitizer.h>
#include <Common/MemoryTracker.h>


namespace DB
{

namespace ErrorCodes
{
}

static constexpr auto CHECKSUM_SIZE{sizeof(CityHash_v1_0_2::uint128)};

void CompressedWriteBuffer::nextImpl()
{
    if (!offset())
        return;

    size_t decompressed_size = offset();
    UInt32 compressed_reserve_size = codec->getCompressedReserveSize(decompressed_size);
    compressed_buffer.resize(compressed_reserve_size);
    UInt32 compressed_size = codec->compress(working_buffer.begin(), decompressed_size, compressed_buffer.data());

    // FIXME remove this after fixing msan report in lz4.
    // Almost always reproduces on stateless tests, the exact test unknown.
    __msan_unpoison(compressed_buffer.data(), compressed_size);

    CityHash_v1_0_2::uint128 checksum = CityHash_v1_0_2::CityHash128(compressed_buffer.data(), compressed_size);
    out.write(reinterpret_cast<const char *>(&checksum), CHECKSUM_SIZE);
    out.write(compressed_buffer.data(), compressed_size);
}


CompressedWriteBuffer::CompressedWriteBuffer(
    WriteBuffer & out_,
    CompressionCodecPtr codec_,
    size_t buf_size)
    : BufferWithOwnMemory<WriteBuffer>(buf_size), out(out_), codec(std::move(codec_))
{
}

CompressedWriteBuffer::~CompressedWriteBuffer()
{
    /// FIXME move final flush into the caller
    MemoryTracker::LockExceptionInThread lock;
    next();
}

void CompressedWriteBuffer::deepCopyTo(/*CompressedWriteBuffer*/BufferBase& target) const
{
    // call base class
    CompressedWriteBuffer &explicitTarget = dynamic_cast<CompressedWriteBuffer&>(target);
    BufferWithOwnMemory<WriteBuffer>::deepCopyTo(explicitTarget);
    out.deepCopyTo(explicitTarget.out);

    // ignore compression setting, TODO, sanity check they should be the same

    //Copy compressed_buffer
    explicitTarget.compressed_buffer.assign(compressed_buffer.begin(), compressed_buffer.end());
}

}
