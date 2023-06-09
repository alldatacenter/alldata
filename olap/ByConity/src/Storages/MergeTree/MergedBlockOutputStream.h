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

#include <Storages/MergeTree/IMergedBlockOutputStream.h>
#include <Columns/ColumnArray.h>

namespace DB
{
class MergeTreeDataPartWriterWide;

/** To write one part.
  * The data refers to one partition, and is written in one part.
  */
class MergedBlockOutputStream final : public IMergedBlockOutputStream
{
public:
    struct WriteSettings
    {
        bool only_recode = false;
    };

    MergedBlockOutputStream(
        const MergeTreeDataPartPtr & data_part,
        const StorageMetadataPtr & metadata_snapshot_,
        const NamesAndTypesList & columns_list_,
        const MergeTreeIndices & skip_indices,
        CompressionCodecPtr default_codec_,
        bool blocks_are_granules_size = false,
        bool optimize_map_column_serialization_ = false);

    Block getHeader() const override { return metadata_snapshot->getSampleBlock(); }

    /// If the data is pre-sorted.
    void write(const Block & block) override;
    void write(const Block & block, const WriteSettings & write_settings);

    /** If the data is not sorted, but we have previously calculated the permutation, that will sort it.
      * This method is used to save RAM, since you do not need to keep two blocks at once - the original one and the sorted one.
      */
    void writeWithPermutation(const Block & block, const IColumn::Permutation * permutation);

    void writeSuffix() override;

    /// Finilize writing part and fill inner structures
    void writeSuffixAndFinalizePart(
            MergeTreeData::MutableDataPartPtr & new_part,
            bool sync = false,
            const NamesAndTypesList * total_columns_list = nullptr,
            MergeTreeData::DataPart::Checksums * additional_column_checksums = nullptr);

    size_t getRowsCount() const { return rows_count; }
    void updateWriterStream(const NameAndTypePair &pair) override;

private:
    /** If `permutation` is given, it rearranges the values in the columns when writing.
      * This is necessary to not keep the whole block in the RAM to sort it.
      */
    void writeImpl(const Block & block, const IColumn::Permutation * permutation);

    void finalizePartOnDisk(
            const MergeTreeData::MutableDataPartPtr & new_part,
            NamesAndTypesList & part_columns,
            MergeTreeData::DataPart::Checksums & checksums,
            bool sync);

private:
    NamesAndTypesList columns_list;
    IMergeTreeDataPart::MinMaxIndex minmax_idx;
    size_t rows_count = 0;
    CompressionCodecPtr default_codec;
};

}
