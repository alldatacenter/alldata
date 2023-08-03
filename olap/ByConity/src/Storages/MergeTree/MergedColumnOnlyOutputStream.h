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
#include <Storages/MergeTree/MergeTreeIOSettings.h>

namespace DB
{

class MergeTreeDataPartWriterWide;

/// Writes only those columns that are in `header`
class MergedColumnOnlyOutputStream final : public IMergedBlockOutputStream
{
public:
    /// Pass empty 'already_written_offset_columns' first time then and pass the same object to subsequent instances of MergedColumnOnlyOutputStream
    ///  if you want to serialize elements of Nested data structure in different instances of MergedColumnOnlyOutputStream.
    MergedColumnOnlyOutputStream(
        const MergeTreeDataPartPtr & data_part,
        const StorageMetadataPtr & metadata_snapshot_,
        const Block & header_,
        CompressionCodecPtr default_codec_,
        const MergeTreeIndices & indices_to_recalc_,
        WrittenOffsetColumns * offset_columns_ = nullptr,
        const MergeTreeIndexGranularity & index_granularity = {},
        const MergeTreeIndexGranularityInfo * index_granularity_info_ = nullptr,
        bool is_merge = false);

    MergedColumnOnlyOutputStream(
        const MergeTreeDataPartPtr & data_part,
        const StorageMetadataPtr & metadata_snapshot_,
        const MergeTreeWriterSettings & write_settings,
        const Block & header_,
        CompressionCodecPtr default_codec_,
        const MergeTreeIndices & indices_to_recalc_,
        WrittenOffsetColumns * offset_columns_ = nullptr,
        const MergeTreeIndexGranularity & index_granularity = {},
        bool is_merge = false);

    Block getHeader() const override { return header; }
    void write(const Block & block) override;
    void writeSuffix() override;
    MergeTreeData::DataPart::Checksums
    writeSuffixAndGetChecksums(MergeTreeData::MutableDataPartPtr & new_part, MergeTreeData::DataPart::Checksums & all_checksums, bool sync = false);
    void updateWriterStream(const NameAndTypePair &pair) override;


private:
    Block header;
};


}
