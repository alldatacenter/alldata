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

#include <Storages/MergeTree/MergeTreeIndexGranularityInfo.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include "Storages/MergeTree/MergeTreeDataPartType.h"
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>

namespace fs = std::filesystem;

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int UNKNOWN_PART_TYPE;
}

std::optional<std::string> MergeTreeIndexGranularityInfo::getMarksExtensionFromFilesystem(const DiskPtr & disk, const String & path_to_part)
{
    if (disk->exists(path_to_part))
    {
        for (DiskDirectoryIteratorPtr it = disk->iterateDirectory(path_to_part); it->isValid(); it->next())
        {
            const auto & ext = fs::path(it->path()).extension();
            if (ext == getNonAdaptiveMrkExtension()
                || ext == getAdaptiveMrkExtension(MergeTreeDataPartType::WIDE)
                || ext == getAdaptiveMrkExtension(MergeTreeDataPartType::COMPACT))
                return ext;
        }
    }
    return {};
}

std::optional<std::string> MergeTreeIndexGranularityInfo::getMarksExtensionFromChecksums(const MergeTreeDataPartChecksums & checksums)
{
    for (const auto & checksum : checksums.files)
    {
        const auto & ext = fs::path(checksum.first).extension();
        if (ext == getNonAdaptiveMrkExtension()
            || ext == getAdaptiveMrkExtension(MergeTreeDataPartType::WIDE)
            || ext == getAdaptiveMrkExtension(MergeTreeDataPartType::COMPACT)
            || ext == getAdaptiveMrkExtension(MergeTreeDataPartType::CNCH)) // duplicate
            return ext;
    }
    return {};
}

MergeTreeIndexGranularityInfo::MergeTreeIndexGranularityInfo(const MergeTreeMetaBase & storage, MergeTreeDataPartType type_)
    : type(type_)
{
    const auto storage_settings = storage.getSettings();
    fixed_index_granularity = storage_settings->index_granularity;

    /// Granularity is fixed
    if (!storage.canUseAdaptiveGranularity())
    {
        if (type != MergeTreeDataPartType::WIDE && type != MergeTreeDataPartType::CNCH)
            throw Exception("Only WIDE or CNCH parts can be used with non-adaptive granularity, current type: " + type.toString(), ErrorCodes::NOT_IMPLEMENTED);
        setNonAdaptive();
    }
    else
        setAdaptive(storage_settings->index_granularity_bytes);

    has_disk_cache = (type == MergeTreeDataPartType::CNCH) && storage_settings->enable_local_disk_cache;
}

void MergeTreeIndexGranularityInfo::changeGranularityIfRequired(const DiskPtr & disk, const String & path_to_part)
{
    auto mrk_ext = getMarksExtensionFromFilesystem(disk, path_to_part);
    if (mrk_ext && *mrk_ext == getNonAdaptiveMrkExtension())
        setNonAdaptive();
}

void MergeTreeIndexGranularityInfo::changeGranularityIfRequired(const MergeTreeDataPartChecksums & checksums)
{
    auto mrk_ext = getMarksExtensionFromChecksums(checksums);
    if (mrk_ext && *mrk_ext == getNonAdaptiveMrkExtension())
        setNonAdaptive();
}

void MergeTreeIndexGranularityInfo::setAdaptive(size_t index_granularity_bytes_)
{
    is_adaptive = true;
    marks_file_extension = getAdaptiveMrkExtension(type);
    index_granularity_bytes = index_granularity_bytes_;
}

void MergeTreeIndexGranularityInfo::setNonAdaptive()
{
    is_adaptive = false;
    marks_file_extension = getNonAdaptiveMrkExtension();
    index_granularity_bytes = 0;
}

size_t MergeTreeIndexGranularityInfo::getMarkSizeInBytes(size_t columns_num) const
{
    if (type == MergeTreeDataPartType::WIDE || type == MergeTreeDataPartType::CNCH)
        return is_adaptive ? getAdaptiveMrkSizeWide() : getNonAdaptiveMrkSizeWide();
    else if (type == MergeTreeDataPartType::COMPACT)
        return getAdaptiveMrkSizeCompact(columns_num);
    else if (type == MergeTreeDataPartType::IN_MEMORY)
        return 0;
    else
        throw Exception("Unknown part type", ErrorCodes::UNKNOWN_PART_TYPE);
}

size_t getAdaptiveMrkSizeCompact(size_t columns_num)
{
    /// Each mark contains number of rows in granule and two offsets for every column.
    return sizeof(UInt64) * (columns_num * 2 + 1);
}

std::string getAdaptiveMrkExtension(MergeTreeDataPartType part_type)
{
    if (part_type == MergeTreeDataPartType::WIDE || part_type == MergeTreeDataPartType::CNCH)
        return ".mrk2";
    else if (part_type == MergeTreeDataPartType::COMPACT)
        return ".mrk3";
    else if (part_type == MergeTreeDataPartType::IN_MEMORY)
        return "";
    else
        throw Exception("Unknown part type", ErrorCodes::UNKNOWN_PART_TYPE);
}

}
