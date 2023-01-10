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

#include "DiskCacheSegment.h"

#include <IO/LimitReadBuffer.h>
#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/DiskCache/IDiskCacheSegment.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeReaderStream.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>

namespace DB
{
DiskCacheSegment::DiskCacheSegment(
    UInt32 segment_number_,
    UInt32 segment_size_,
    const IMergeTreeDataPartPtr & data_part_,
    const FileOffsetAndSize & mrk_file_pos_,
    size_t marks_count_,
    const String & stream_name_,
    const String & extension_,
    const FileOffsetAndSize & stream_file_pos_)
    : IDiskCacheSegment(segment_number_, segment_size_)
    , data_part(data_part_)
    , storage(data_part_->storage.shared_from_this()) /// Need to extend the lifetime of storage because disk cache can run async
    , mrk_file_pos(mrk_file_pos_)
    , marks_count(marks_count_)
    , stream_name(stream_name_)
    , extension(extension_)
    , stream_file_pos(stream_file_pos_)
    , marks_loader(
          data_part->volume->getDisk(),
          nullptr,
          data_part->getFullRelativePath() + "data",
          stream_name,
          marks_count,
          data_part->index_granularity_info,
          false,
          mrk_file_pos.file_offset,
          mrk_file_pos.file_size)
{
}

String DiskCacheSegment::getSegmentKey(
    const StorageID & storage_id, const String & part_name, const String & stream_name, UInt32 segment_index, const String & extension)
{
    return IDiskCacheSegment::formatSegmentName(
        UUIDHelpers::UUIDToString(storage_id.uuid), part_name, stream_name, segment_index, extension);
}

String DiskCacheSegment::getSegmentName() const
{
    return formatSegmentName(
        UUIDHelpers::UUIDToString(storage->getStorageUUID()), data_part->name, stream_name, segment_number, extension);
}

void DiskCacheSegment::cacheToDisk(IDiskCache & disk_cache)
{
    Poco::Logger * log = disk_cache.getLogger();

    try
    {
        size_t left_mark = std::min(segment_size * segment_number, marks_count);
        size_t right_mark = std::min(segment_size * (segment_number + 1), marks_count);
        if (left_mark >= right_mark)
            return;

        size_t cache_data_left_offset = marks_loader.getMark(left_mark).offset_in_compressed_file;
        size_t cache_data_right_offset = 0;
        if (right_mark >= marks_count)
        {
            cache_data_right_offset = stream_file_pos.file_size;
        }
        else if (marks_loader.getMark(right_mark).offset_in_decompressed_block == 0)
        {
            cache_data_right_offset = marks_loader.getMark(right_mark).offset_in_compressed_file;
        }
        else
        {
            /// if right_mark is inside the block, we will need to read the whole block;
            String data_path = data_part->getFullRelativePath() + "data";
            auto disk = data_part->volume->getDisk();
            auto source_buffer = std::make_unique<CompressedReadBufferFromFile>(
                disk->readFile(data_path), stream_file_pos.file_offset, stream_file_pos.file_size, true);

            source_buffer->seek(stream_file_pos.file_offset + marks_loader.getMark(right_mark).offset_in_compressed_file, 0);
            cache_data_right_offset = marks_loader.getMark(right_mark).offset_in_compressed_file + source_buffer->getSizeCompressed();
        }

        size_t cache_data_bytes = cache_data_right_offset - cache_data_left_offset;
        LOG_DEBUG(
            log,
            "cache data file: `{}` mark range [{}, {}), offset: {}, bytes: {}",
            stream_name + extension,
            left_mark,
            right_mark,
            cache_data_left_offset,
            cache_data_bytes);

        String data_path = data_part->getFullRelativePath() + "data";
        auto disk = data_part->volume->getDisk();
        auto data_file = disk->readFile(data_path);

        /// cache data segment
        data_file->seek(stream_file_pos.file_offset + cache_data_left_offset);
        LimitReadBuffer segment_value(*data_file, cache_data_bytes, false);
        disk_cache.set(getSegmentName(), segment_value, cache_data_bytes);

        /// cache mark segment
        data_file->seek(mrk_file_pos.file_offset);
        LimitReadBuffer marks_value(*data_file, mrk_file_pos.file_size, false);
        String marks_key = formatSegmentName(UUIDHelpers::UUIDToString(storage->getStorageUUID()), data_part->name, stream_name, 0, MARKS_FILE_EXTENSION);
        disk_cache.set(marks_key, marks_value, mrk_file_pos.file_size);
    }
    catch (...)
    {
        LOG_ERROR(log, "Failed to cache segment to local disk.");
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}
}
