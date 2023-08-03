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

#include <memory>
#include <IO/ReadBuffer.h>
#include <IO/UncompressedCache.h>
#include <Storages/MarkCache.h>
#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/MergeTree/MarkRange.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeRangeReader.h>
#include <Storages/MergeTree/MergeTreeIndexGranularityInfo.h>
#include <Storages/MergeTree/MergeTreeIOSettings.h>
#include <Storages/MergeTree/MergeTreeMarksLoader.h>
#include <Storages/MergeTree/MergeTreeIndexGranularity.h>
#include <Storages/MergeTree/MergedReadBufferWithSegmentCache.h>
#include <Storages/MergeTree/IMergeTreeReaderStream.h>

namespace DB
{

class MergeTreeReaderStreamWithSegmentCache: public IMergeTreeReaderStream
{
public:
    MergeTreeReaderStreamWithSegmentCache(
        const StorageID& storage_id_, const String& part_name_,
        const String& stream_name_, DiskPtr disk_, size_t marks_count_,
        const String& data_path_, off_t data_offset_, size_t data_size_,
        const String& mark_path_, off_t mark_offset_, size_t mark_size_,
        const MarkRanges& all_mark_ranges_, const MergeTreeReaderSettings& settings_,
        MarkCache* mark_cache_, UncompressedCache* uncompressed_cache_,
        IDiskCache* segment_cache_, size_t cache_segment_size_,
        const MergeTreeIndexGranularityInfo* index_granularity_info_,
        const ReadBufferFromFileBase::ProfileCallback& profile_callback_,
        clockid_t clock_type_);

    virtual void seekToMark(size_t mark) override;

    virtual void seekToStart() override;

private:
    static void markRangeStatistics(const MarkRanges& all_mark_ranges,
        MergeTreeMarksLoader& marks_loader, size_t file_size,
        size_t* max_mark_range_bytes, size_t* sum_mark_range_bytes);

    MergeTreeMarksLoader marks_loader;

    std::unique_ptr<MergedReadBufferWithSegmentCache> read_buffer_holder;

};

}
