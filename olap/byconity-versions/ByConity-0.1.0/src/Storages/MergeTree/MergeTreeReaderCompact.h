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

#include <Core/NamesAndTypes.h>
#include <Storages/MergeTree/IMergeTreeReader.h>
#include <IO/ReadBufferFromFileBase.h>


namespace DB
{

class MergeTreeDataPartCompact;
using DataPartCompactPtr = std::shared_ptr<const MergeTreeDataPartCompact>;

class IMergeTreeDataPart;
using DataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;

/// Reader for compact parts
class MergeTreeReaderCompact : public IMergeTreeReader
{
public:
    MergeTreeReaderCompact(
        DataPartCompactPtr data_part_,
        NamesAndTypesList columns_,
        const StorageMetadataPtr & metadata_snapshot_,
        UncompressedCache * uncompressed_cache_,
        MarkCache * mark_cache_,
        MarkRanges mark_ranges_,
        MergeTreeReaderSettings settings_,
        ValueSizeMap avg_value_size_hints_ = {},
        const ReadBufferFromFileBase::ProfileCallback & profile_callback_ = {},
        clockid_t clock_type_ = CLOCK_MONOTONIC_COARSE);

    /// Return the number of rows has been read or zero if there is no columns to read.
    /// If continue_reading is true, continue reading from last state, otherwise seek to from_mark
    size_t readRows(size_t from_mark, bool continue_reading, size_t max_rows_to_read, Columns & res_columns) override;

    bool canReadIncompleteGranules() const override { return false; }

private:
    bool isContinuousReading(size_t mark, size_t column_position);

    /// Positions of columns in part structure.
    using ColumnPositions = std::vector<ColumnPosition>;
    ColumnPositions column_positions;
    /// Should we read full column or only it's offsets
    std::vector<bool> read_only_offsets;

    size_t next_mark = 0;
    std::optional<std::pair<size_t, size_t>> last_read_granule;

    struct CompactDataReader
    {
        CompactDataReader(
            const MergeTreeData::DataPartPtr & data_part,
            UncompressedCache * uncompressed_cache,
            MarkCache * mark_cache,
            const MergeTreeReaderSettings & settings,
            const ColumnPositions & column_positions_,
            const MarkRanges & all_mark_ranges,
            const ReadBufferFromFileBase::ProfileCallback & profile_callback_,
            clockid_t clock_type_);

        void seekToMark(size_t row_index, size_t column_index);

        ReadBuffer * data_buffer;
        std::unique_ptr<CachedCompressedReadBuffer> cached_buffer;
        std::unique_ptr<CompressedReadBufferFromFile> non_cached_buffer;

        MergeTreeMarksLoader marks_loader;
    };
    using CompactDataReaderPtr = std::unique_ptr<CompactDataReader>;

    /// In compact part, all columns except for ByteMap columns will write into one file. These column will use this data reader.
    /// Each implicit column of ByteMap columns will use separate reader stream.
    CompactDataReaderPtr data_reader;

    void readCompactData(const NameAndTypePair & name_and_type, ColumnPtr & column, size_t from_mark,
        size_t column_position, size_t rows_to_read, bool only_offsets);

    ColumnPosition findColumnForOffsets(const String & column_name) const override;

    /// Returns maximal value of granule size in compressed file from @mark_ranges.
    /// This value is used as size of read buffer.
    static size_t getReadBufferSize(
        const DataPartPtr & part,
        MergeTreeMarksLoader & marks_loader,
        const ColumnPositions & column_positions,
        const MarkRanges & mark_ranges);

    /// Record map kv column to its origin map column in order to speed up read process.
    std::unordered_map<String, NameAndTypePair> map_kv_to_origin_col;
};

}
