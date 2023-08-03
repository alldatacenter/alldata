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

#include <optional>
#include <common/types.h>
#include "Storages/MergeTree/MergeTreePartition.h"
#include <Storages/MergeTree/MergeTreeDataPartType.h>
#include <Disks/IDisk.h>

namespace DB
{

class MergeTreeMetaBase;

/// Meta information about index granularity
struct MergeTreeIndexGranularityInfo
{
public:
    /// Marks file extension '.mrk' or '.mrk2'
    String marks_file_extension;

    /// Is stride in rows between marks non fixed?
    bool is_adaptive = false;

    /// Fixed size in rows of one granule if index_granularity_bytes is zero
    size_t fixed_index_granularity = 0;

    /// Approximate bytes size of one granule
    size_t index_granularity_bytes = 0;

    MergeTreeIndexGranularityInfo(const MergeTreeMetaBase & storage, MergeTreeDataPartType type_);

    void changeGranularityIfRequired(const DiskPtr & disk, const String & path_to_part);
    void changeGranularityIfRequired(const MergeTreeDataPartChecksums & checksums);

    String getMarksFilePath(const String & path_prefix) const
    {
        return path_prefix + marks_file_extension;
    }

    size_t getMarkSizeInBytes(size_t columns_num = 1) const;

    static std::optional<std::string> getMarksExtensionFromFilesystem(const DiskPtr & disk, const String & path_to_part);
    static std::optional<std::string> getMarksExtensionFromChecksums(const MergeTreeDataPartChecksums & checksums);

    void setAdaptive(size_t index_granularity_bytes_);
    void setNonAdaptive();

private:
    MergeTreeDataPartType type;
};

constexpr inline auto getNonAdaptiveMrkExtension() { return ".mrk"; }
constexpr inline auto getNonAdaptiveMrkSizeWide() { return sizeof(UInt64) * 2; }
constexpr inline auto getAdaptiveMrkSizeWide() { return sizeof(UInt64) * 3; }
inline size_t getAdaptiveMrkSizeCompact(size_t columns_num);
std::string getAdaptiveMrkExtension(MergeTreeDataPartType part_type);

}
