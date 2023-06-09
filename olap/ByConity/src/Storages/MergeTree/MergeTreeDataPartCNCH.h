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

#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include "common/types.h"

namespace DB
{
class MergeTreeMetaBase;
/// Mock cnch part for Catalog usage.
class MergeTreeDataPartCNCH : public IMergeTreeDataPart
{
public:
    MergeTreeDataPartCNCH(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_ = {},
        const IMergeTreeDataPart * parent_part_ = nullptr,
        const UUID& part_id = UUIDHelpers::Nil);

    MergeTreeDataPartCNCH(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_ = {},
        const IMergeTreeDataPart * parent_part_ = nullptr,
        const UUID& part_id = UUIDHelpers::Nil);

    MergeTreeReaderPtr getReader(
        const NamesAndTypesList & columns_to_read,
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

    bool operator < (const MergeTreeDataPartCNCH & r) const;
    bool operator > (const MergeTreeDataPartCNCH & r) const;

    /// for dump
    void fromLocalPart(const IMergeTreeDataPart & local_part);

    bool isStoredOnDisk() const override { return true; }

    bool supportsVerticalMerge() const override { return true; }

    String getFileNameForColumn(const NameAndTypePair & column) const override;

    bool hasColumnFiles(const NameAndTypePair & column) const override;

    void loadIndexGranularity(size_t marks_count, const std::vector<size_t> & index_granularities) override;

    void loadColumnsChecksumsIndexes(bool require_columns_checksums, bool check_consistency) override;

    void loadFromFileSystem(bool load_hint_mutation = true);

    UniqueKeyIndexPtr getUniqueKeyIndex() const override;

    /// @param is_unique_new_part whether it's a part of unique table which has not been deduplicated
    /// For unique table, in normal case, the new part doesn't have delete_bitmap until it executes dedup action.
    /// But when the part has delete_flag info, delete_bitmap represent the delete_flag info which leads to that new part has delete_bitmap.
    const ImmutableDeleteBitmapPtr & getDeleteBitmap(bool is_unique_new_part = false) const override;

    virtual void projectionRemove(const String & parent_to, bool keep_shared_data) const override;

    void preload(ThreadPool & pool) const;

private:

    bool isDeleted() const;

    void checkConsistency(bool require_part_metadata) const override;

    IndexPtr loadIndex() override;

    MergeTreeDataPartChecksums::FileChecksums loadPartDataFooter() const;

    ChecksumsPtr loadChecksums(bool require) override;

    UniqueKeyIndexPtr loadUniqueKeyIndex() override;

    IndexFile::RemoteFileInfo getRemoteFileInfo();

    void getUniqueKeyIndexFilePosAndSize(const IMergeTreeDataPartPtr part, off_t & off, size_t & size);

    /// Loads marks index granularity into memory
    void loadIndexGranularity() override;

    void loadMetaInfoFromBuffer(ReadBuffer & buffer, bool load_hint_mutation);

    void calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const override;
    ColumnSize getColumnSizeImpl(const NameAndTypePair & column, std::unordered_set<String> * processed_substreams) const;

    void removeImpl(bool keep_shared_data) const override;
};

}
