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

#include <Storages/MergeTree/IMergeTreeDataPart.h>

namespace DB
{

class UncompressedCache;

class MergeTreeDataPartInMemory : public IMergeTreeDataPart
{
public:
    MergeTreeDataPartInMemory(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_ = {},
        const IMergeTreeDataPart * parent_part_ = nullptr,
        IStorage::StorageLocation location_ = IStorage::StorageLocation::MAIN);

    MergeTreeDataPartInMemory(
        MergeTreeMetaBase & storage_,
        const String & name_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_ = {},
        const IMergeTreeDataPart * parent_part_ = nullptr,
        IStorage::StorageLocation location_ = IStorage::StorageLocation::MAIN);

    MergeTreeReaderPtr getReader(
        const NamesAndTypesList & columns,
        const StorageMetadataPtr & metadata_snapshot,
        const MarkRanges & mark_ranges,
        UncompressedCache * uncompressed_cache,
        MarkCache * mark_cache,
        const MergeTreeReaderSettings & reader_settings_,
        const ValueSizeMap & avg_value_size_hints,
        const ReadBufferFromFileBase::ProfileCallback & profile_callback) const override;

    MergeTreeWriterPtr getWriter(
        const NamesAndTypesList & columns_list,
        const StorageMetadataPtr & metadata_snapshot,
        const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
        const CompressionCodecPtr & default_codec_,
        const MergeTreeWriterSettings & writer_settings,
        const MergeTreeIndexGranularity & computed_index_granularity) const override;

    bool isStoredOnDisk() const override { return false; }
    bool hasColumnFiles(const NameAndTypePair & column) const override { return !!getColumnPosition(column.name); }
    String getFileNameForColumn(const NameAndTypePair & /* column */) const override { return ""; }
    void renameTo(const String & new_relative_path, bool remove_new_dir_if_exists) const override;
    void makeCloneInDetached(const String & prefix, const StorageMetadataPtr & metadata_snapshot) const override;

    void flushToDisk(const String & base_path, const String & new_relative_path, const StorageMetadataPtr & metadata_snapshot) const;

    /// Returns hash of parts's block
    Checksum calculateBlockChecksum() const;

    mutable Block block;

private:
    mutable std::condition_variable is_merged;

    /// Calculates uncompressed sizes in memory.
    void calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const override;
};

using DataPartInMemoryPtr = std::shared_ptr<const MergeTreeDataPartInMemory>;
DataPartInMemoryPtr asInMemoryPart(const MergeTreeDataPartPtr & part);

}
