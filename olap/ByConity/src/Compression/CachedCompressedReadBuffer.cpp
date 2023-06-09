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

#include "CachedCompressedReadBuffer.h"

#include <IO/WriteHelpers.h>
#include <Compression/LZ4_decompress_faster.h>

#include <utility>


namespace DB
{

namespace ErrorCodes
{
    extern const int SEEK_POSITION_OUT_OF_BOUND;
}


void CachedCompressedReadBuffer::initInput()
{
    if (!file_in)
    {
        file_in = file_in_creator();
        compressed_in = file_in.get();

        if (profile_callback)
            file_in->setProfileCallback(profile_callback, clock_type);
    }
}


bool CachedCompressedReadBuffer::nextImpl()
{

    /// It represents the end of file when the position exceeds the limit in hdfs shared storage or handling implicit column data in compact impl.
    /// TODO: handle hdfs case
    if (/*(storage_type == StorageType::Hdfs || */is_limit /*)*/ && file_pos >= limit_offset_in_file)
        return false;

    /// Let's check for the presence of a decompressed block in the cache, grab the ownership of this block, if it exists.
    UInt128 key = cache->hash(path, file_pos);

    owned_cell = cache->getOrSet(key, [&]()
    {
        initInput();
        file_in->seek(file_pos, SEEK_SET);

        auto cell = std::make_shared<UncompressedCacheCell>();

        size_t size_decompressed;
        size_t size_compressed_without_checksum;
        cell->compressed_size = readCompressedData(size_decompressed, size_compressed_without_checksum, false);

        if (cell->compressed_size)
        {
            cell->additional_bytes = codec->getAdditionalSizeAtTheEndOfBuffer();
            cell->data.resize(size_decompressed + cell->additional_bytes);
            decompressTo(cell->data.data(), size_decompressed, size_compressed_without_checksum);
        }

        return cell;
    });

    if (owned_cell->data.size() == 0)
        return false;

    working_buffer = Buffer(owned_cell->data.data(), owned_cell->data.data() + owned_cell->data.size() - owned_cell->additional_bytes);

    file_pos += owned_cell->compressed_size;

    return true;
}

CachedCompressedReadBuffer::CachedCompressedReadBuffer(
    const std::string & path_,
    std::function<std::unique_ptr<ReadBufferFromFileBase>()> file_in_creator_,
    UncompressedCache * cache_,
    bool allow_different_codecs_,
    off_t file_offset_,
    size_t file_size_,
    bool is_limit_)
    : ReadBuffer(nullptr, 0)
    , file_in_creator(std::move(file_in_creator_))
    , cache(cache_)
    , path(path_)
    , file_pos(file_offset_)
    , limit_offset_in_file(file_offset_ + file_size_)
    , is_limit(is_limit_)
{
    allow_different_codecs = allow_different_codecs_;
}

void CachedCompressedReadBuffer::seek(size_t offset_in_compressed_file, size_t offset_in_decompressed_block)
{
    if (owned_cell &&
        offset_in_compressed_file == file_pos - owned_cell->compressed_size &&
        offset_in_decompressed_block <= working_buffer.size())
    {
        bytes += offset();
        pos = working_buffer.begin() + offset_in_decompressed_block;
        bytes -= offset();
    }
    else
    {
        file_pos = offset_in_compressed_file;

        bytes += offset();

        resetWorkingBuffer();

        nextImpl();

        if (offset_in_decompressed_block > working_buffer.size())
            throw Exception("Seek position is beyond the decompressed block"
                " (pos: " + toString(offset_in_decompressed_block) + ", block size: " + toString(working_buffer.size()) + ")",
                ErrorCodes::SEEK_POSITION_OUT_OF_BOUND);

        pos = working_buffer.begin() + offset_in_decompressed_block;
        bytes -= offset();
    }
}

}
