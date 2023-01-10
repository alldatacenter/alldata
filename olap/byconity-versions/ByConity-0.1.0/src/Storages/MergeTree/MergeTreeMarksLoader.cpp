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

#include <Storages/MergeTree/MergeTreeMarksLoader.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <IO/ReadBufferFromFile.h>
#include <Storages/DiskCache/DiskCacheFactory.h>
#include <Storages/DiskCache/IDiskCacheSegment.h>
#include <Storages/DiskCache/IDiskCache.h>

#include <utility>

namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_ALL_DATA;
    extern const int CORRUPTED_DATA;
    extern const int LOGICAL_ERROR;
    extern const int CANNOT_SEEK_THROUGH_FILE;
}

MergeTreeMarksLoader::MergeTreeMarksLoader(
    DiskPtr disk_,
    MarkCache * mark_cache_,
    const String & mrk_path_,
    const String & stream_name_,
    size_t marks_count_,
    const MergeTreeIndexGranularityInfo & index_granularity_info_,
    bool save_marks_in_cache_,
    off_t mark_file_offset_,
    size_t mark_file_size_,
    size_t columns_in_mark_,
    IDiskCache * disk_cache_,
    UUID storage_uuid_,
    const String & part_name_)
    : disk(std::move(disk_))
    , mark_cache(mark_cache_)
    , mrk_path(mrk_path_)
    , stream_name(stream_name_)
    , marks_count(marks_count_)
    , mark_file_offset(mark_file_offset_)
    , mark_file_size(mark_file_size_)
    , index_granularity_info(index_granularity_info_)
    , save_marks_in_cache(save_marks_in_cache_)
    , columns_in_mark(columns_in_mark_)
    , disk_cache(disk_cache_)
    , storage_uuid {storage_uuid_}
    , part_name(part_name_) {}

const MarkInCompressedFile & MergeTreeMarksLoader::getMark(size_t row_index, size_t column_index)
{
    if (!marks)
        loadMarks();

    if (column_index >= columns_in_mark)
        throw Exception("Column index: " + toString(column_index)
            + " is out of range [0, " + toString(columns_in_mark) + ")", ErrorCodes::LOGICAL_ERROR);

    return (*marks)[row_index * columns_in_mark + column_index];
}

MarkCache::MappedPtr MergeTreeMarksLoader::loadMarksImpl()
{
    /// Memory for marks must not be accounted as memory usage for query, because they are stored in shared cache.
    MemoryTracker::BlockerInThread temporarily_disable_memory_tracker;

    size_t mark_size = index_granularity_info.getMarkSizeInBytes(columns_in_mark);
    size_t expected_file_size = mark_size * marks_count;

    if (expected_file_size != mark_file_size)
        throw Exception(
            "Bad size of marks file '" + fullPath(disk, mrk_path) + "' for stream '" + stream_name + "': " + std::to_string(mark_file_size) + ", must be: " + std::to_string(expected_file_size),
            ErrorCodes::CORRUPTED_DATA);

    auto res = std::make_shared<MarksInCompressedFile>(marks_count * columns_in_mark);

    if (!index_granularity_info.is_adaptive)
    {
        auto buffer = [&, this]() -> std::unique_ptr<ReadBufferFromFileBase> {
            if (index_granularity_info.has_disk_cache && disk_cache)
            {
                try
                {
                    String mrk_seg_key = IDiskCacheSegment::formatSegmentName(
                        UUIDHelpers::UUIDToString(storage_uuid), part_name, stream_name, 0, index_granularity_info.marks_file_extension);
                    auto [local_cache_disk, local_cache_path] = disk_cache->get(mrk_seg_key);
                    if (local_cache_disk && local_cache_disk->exists(local_cache_path))
                    {
                        LOG_TRACE(&Poco::Logger::get(__func__), "load from local disk {}, mrk_path {}", local_cache_disk->getPath(), local_cache_path);
                        size_t cached_mark_file_size = local_cache_disk->getFileSize(local_cache_path);
                        if (expected_file_size != cached_mark_file_size)
                            throw Exception(
                                "Bad size of marks file on disk cache'" + fullPath(local_cache_disk, local_cache_path) + "' for stream '"
                                    + stream_name + "': " + std::to_string(cached_mark_file_size)
                                    + ", must be: " + std::to_string(expected_file_size),
                                ErrorCodes::CORRUPTED_DATA);
                        return local_cache_disk->readFile(local_cache_path, {.buffer_size = cached_mark_file_size});
                    }
                }
                catch (...)
                {
                    tryLogCurrentException("Could not load marks from disk cache");
                }
            }

            auto buf = disk->readFile(mrk_path, {.buffer_size = mark_file_size});
            if (buf->seek(mark_file_offset) != mark_file_offset)
                throw Exception("Cannot seek to mark file  " + mrk_path + " for stream " + stream_name, ErrorCodes::CANNOT_SEEK_THROUGH_FILE);
            return buf;
        }();

        if (buffer->eof() || buffer->buffer().size() != mark_file_size)
            throw Exception("Cannot read all marks from file " + mrk_path + ", eof: " + std::to_string(buffer->eof())
            + ", buffer size: " + std::to_string(buffer->buffer().size()) + ", file size: " + std::to_string(mark_file_size), ErrorCodes::CANNOT_READ_ALL_DATA);

        buffer->readStrict(reinterpret_cast<char *>(res->data()), mark_file_size);
    }
    else
    {
        auto buffer = disk->readFile(mrk_path, {.buffer_size = mark_file_size});
        if (buffer->seek(mark_file_offset) != mark_file_offset)
            throw Exception("Cannot seek to mark file  " + mrk_path + " for stream " + stream_name, ErrorCodes::CANNOT_SEEK_THROUGH_FILE);

        size_t i = 0;
        off_t limit_offset_in_file = mark_file_offset + mark_file_size;
        while (buffer->getPosition() < limit_offset_in_file)
        {
            res->read(*buffer, i * columns_in_mark, columns_in_mark);
            buffer->seek(sizeof(size_t), SEEK_CUR);
            ++i;
        }

        if (i * mark_size != mark_file_size)
            throw Exception("Cannot read all marks from file " + mrk_path, ErrorCodes::CANNOT_READ_ALL_DATA);
    }
    res->protect();
    return res;
}

void MergeTreeMarksLoader::loadMarks()
{
    String mrk_name = index_granularity_info.getMarksFilePath(stream_name);
    if (mark_cache)
    {
        auto key = mark_cache->hash(mrk_path + mrk_name);
        if (save_marks_in_cache)
        {
            auto callback = [this]{ return loadMarksImpl(); };
            marks = mark_cache->getOrSet(key, callback);
        }
        else
        {
            marks = mark_cache->get(key);
            if (!marks)
                marks = loadMarksImpl();
        }
    }
    else
        marks = loadMarksImpl();

    if (!marks)
        throw Exception("Failed to load marks: " + mrk_name + " from path:" + mrk_path, ErrorCodes::LOGICAL_ERROR);
}

}
