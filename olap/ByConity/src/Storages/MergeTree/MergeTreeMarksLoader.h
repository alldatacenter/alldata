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
#include <Disks/IDisk.h>
#include <Storages/MarkCache.h>

namespace DB
{

struct MergeTreeIndexGranularityInfo;
class IDiskCache;

class MergeTreeMarksLoader
{
public:
    using MarksPtr = MarkCache::MappedPtr;

    MergeTreeMarksLoader(
        DiskPtr disk_,
        MarkCache * mark_cache_,
        const String & mrk_path,
        const String & stream_name_,
        size_t marks_count_,
        const MergeTreeIndexGranularityInfo & index_granularity_info_,
        bool save_marks_in_cache_,
        off_t mark_file_offset_,
        size_t mark_file_size_,
        size_t columns_in_mark_ = 1,
        IDiskCache * disk_cache_ = nullptr,
        UUID storage_uuid_ = {},
        const String & part_name_ = {});

    const MarkInCompressedFile & getMark(size_t row_index, size_t column_index = 0);

    bool initialized() const { return marks != nullptr; }

    size_t marksCount() const { return marks_count; }

private:
    DiskPtr disk;
    MarkCache * mark_cache = nullptr;
    String mrk_path;
    String stream_name; // for compacted map
    size_t marks_count;

    off_t mark_file_offset;
    size_t mark_file_size;

    const MergeTreeIndexGranularityInfo & index_granularity_info;
    bool save_marks_in_cache = false;
    size_t columns_in_mark;
    MarkCache::MappedPtr marks;

    IDiskCache * disk_cache;
    UUID storage_uuid;
    String part_name;

    void loadMarks();
    MarkCache::MappedPtr loadMarksImpl();
};

}
