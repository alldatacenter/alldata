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

#include <memory>
#include <optional>
#include <Storages/MergeTree/MergeTreeReaderStreamWithSegmentCache.h>
#include <Storages/DiskCache/DiskCacheSegment.h>
#include <DataStreams/MarkInCompressedFile.h>
#include <fmt/core.h>
#include <Common/Exception.h>
#include <Compression/CachedCompressedReadBuffer.h>
#include <Compression/CompressedReadBuffer.h>
#include <Compression/CompressedReadBufferFromFile.h>
#include <Disks/IDisk.h>
#include <IO/ReadBuffer.h>
#include <IO/ReadBufferFromFileBase.h>
#include <IO/createReadBufferFromFileBase.h>
#include <Storages/DiskCache/DiskCacheSegment.h>
#include <Storages/MergeTree/MergeTreeReaderStream.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>

namespace DB
{
MergeTreeReaderStreamWithSegmentCache::MergeTreeReaderStreamWithSegmentCache(
    const StorageID& storage_id_, const String& part_name_,
    const String& stream_name_, DiskPtr disk_, size_t marks_count_,
    const String& data_path_, off_t data_offset_, size_t data_size_,
    const String& mark_path_, off_t mark_offset_, size_t mark_size_,
    const MarkRanges& all_mark_ranges_, const MergeTreeReaderSettings& settings_,
    MarkCache* mark_cache_, UncompressedCache* uncompressed_cache_,
    IDiskCache* segment_cache_, size_t cache_segment_size_,
    const MergeTreeIndexGranularityInfo* index_granularity_info_,
    const ReadBufferFromFileBase::ProfileCallback& profile_callback_,
    clockid_t clock_type_):
        marks_loader(disk_, mark_cache_, mark_path_,
            stream_name_, marks_count_, *index_granularity_info_,
            settings_.save_marks_in_cache, mark_offset_, mark_size_, 1,
            segment_cache_, storage_id_.uuid, part_name_)
{
    size_t max_mark_range_bytes = 0;
    size_t sum_mark_range_bytes = 0;

    markRangeStatistics(all_mark_ranges_, marks_loader, data_size_,
        &max_mark_range_bytes, &sum_mark_range_bytes);

    if (max_mark_range_bytes == 0)
        max_mark_range_bytes = settings_.max_read_buffer_size;

    size_t buffer_size = std::min(settings_.max_read_buffer_size,
        max_mark_range_bytes);

    size_t total_segment_count = (marks_count_ + cache_segment_size_ - 1) / cache_segment_size_;

    read_buffer_holder = std::make_unique<MergedReadBufferWithSegmentCache>(
        storage_id_, part_name_, stream_name_, disk_, data_path_, data_offset_,
        data_size_, cache_segment_size_, segment_cache_, sum_mark_range_bytes,
        buffer_size, settings_, total_segment_count, marks_loader, uncompressed_cache_,
        profile_callback_, clock_type_
    );
    data_buffer = read_buffer_holder.get();
}

void MergeTreeReaderStreamWithSegmentCache::seekToStart()
{
    read_buffer_holder->seekToStart();
}

void MergeTreeReaderStreamWithSegmentCache::seekToMark(size_t mark)
{
    read_buffer_holder->seekToMark(mark);
}

void MergeTreeReaderStreamWithSegmentCache::markRangeStatistics(const MarkRanges& all_mark_ranges,
    MergeTreeMarksLoader& marks_loader, size_t file_size, size_t* max_mark_range_bytes,
    size_t* sum_mark_range_bytes)
{
    size_t marks_count = marks_loader.marksCount();
    for (const auto & mark_range : all_mark_ranges)
    {
        size_t left_mark = mark_range.begin;
        size_t right_mark = mark_range.end;

        /// NOTE: if we are reading the whole file, then right_mark == marks_count
        /// and we will use max_read_buffer_size for buffer size, thus avoiding the need to load marks.

        /// If the end of range is inside the block, we will need to read it too.
        if (right_mark < marks_count && marks_loader.getMark(right_mark).offset_in_decompressed_block > 0)
        {
            auto indices = collections::range(right_mark, marks_count);
            auto it = std::upper_bound(indices.begin(), indices.end(), right_mark, [&marks_loader](size_t i, size_t j)
            {
                return marks_loader.getMark(i).offset_in_compressed_file < marks_loader.getMark(j).offset_in_compressed_file;
            });

            right_mark = (it == indices.end() ? marks_count : *it);
        }

        size_t mark_range_bytes;

        /// If there are no marks after the end of range, just use file size
        if (right_mark >= marks_count
            || (right_mark + 1 == marks_count
                && marks_loader.getMark(right_mark).offset_in_compressed_file == marks_loader.getMark(mark_range.end).offset_in_compressed_file))
        {
            mark_range_bytes = file_size - (left_mark < marks_count ? marks_loader.getMark(left_mark).offset_in_compressed_file : 0);
        }
        else
        {
            mark_range_bytes = marks_loader.getMark(right_mark).offset_in_compressed_file - marks_loader.getMark(left_mark).offset_in_compressed_file;
        }

        *max_mark_range_bytes = std::max(*max_mark_range_bytes, mark_range_bytes);
        *sum_mark_range_bytes += mark_range_bytes;
    }
}

}
