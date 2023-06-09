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

/** In compact format all columns are stored in one file (`data.bin`).
  * Data is split in granules and columns are serialized sequentially in one granule.
  * Granules are written one by one in data file.
  * Marks are also stored in single file (`data.mrk3`).
  * In compact format one mark is an array of marks for every column and a number of rows in granule.
  * Format of other data part files is not changed.
  * It's considered to store only small parts in compact format (up to 10M).
  * NOTE: Compact parts aren't supported for tables with non-adaptive granularity.
  * NOTE: In compact part compressed and uncompressed size of single column is unknown.
  */
class MergeTreeDataPartCompact : public IMergeTreeDataPart
{
public:
    static constexpr auto DATA_FILE_NAME = "data";
    static constexpr auto DATA_FILE_NAME_WITH_EXTENSION = "data.bin";

    MergeTreeDataPartCompact(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_ = {},
        const IMergeTreeDataPart * parent_part_ = nullptr,
        IStorage::StorageLocation location_ = IStorage::StorageLocation::MAIN);

    MergeTreeDataPartCompact(
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

    bool isStoredOnDisk() const override { return true; }

    bool hasColumnFiles(const NameAndTypePair & column) const override;

    String getFileNameForColumn(const NameAndTypePair & /* column */) const override { return DATA_FILE_NAME; }

    void loadIndexGranularity(const size_t marks_count, const std::vector<size_t> & index_granularities) override;

    ~MergeTreeDataPartCompact() override;

    size_t getColumnsWithoutByteMapColSize() const { return columns_without_bytemap_col_size; }

    void setColumnsPtr(const NamesAndTypesListPtr & new_columns_ptr) override;

    /// Due to all columns except for ByteMap columns are written into one file. It's necessary to hold the column position without ByteMap column.
    std::optional<size_t> getColumnPositionWithoutMap(const String & column_name) const;

private:

    size_t columns_without_bytemap_col_size;

    /// In compact parts order of columns without map col is necessary, only valid for compact part.
    NameToNumber column_name_to_position_without_map;

    void checkConsistency(bool require_part_metadata) const override;

    /// Loads marks index granularity into memory
    void loadIndexGranularity() override;

    /// Compact parts doesn't support per column size, only total size
    void calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const override;
};

}
