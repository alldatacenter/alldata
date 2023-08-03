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
#include <Processors/Sources/SourceWithProgress.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/IMergeTreeReader.h>
#include <Storages/MergeTree/MarkRange.h>
#include <memory>

namespace DB
{
/// Similar to MergeTreeSequentialSource, the only differnence is that
/// deleted rows (determined by the delete bitmap) are filled with default values
/// rather than being filtered out as in MergeTreeSequentialSource.
/// Currently used by the FAST_DELETE mutation to remove sensitive data in deleted columns.
class MergeTreeFillDeleteWithDefaultValueSource : public SourceWithProgress
{
public:
    MergeTreeFillDeleteWithDefaultValueSource(
        const MergeTreeMetaBase & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        MergeTreeData::DataPartPtr data_part_,
        ImmutableDeleteBitmapPtr delete_bitmap_,
        Names columns_to_read_);

    ~MergeTreeFillDeleteWithDefaultValueSource() override;

    String getName() const override { return "MergeTreeFillDeleteWithDefaultValueSource"; }

protected:
    Chunk generate() override;

private:

    const MergeTreeMetaBase & storage;
    StorageMetadataPtr metadata_snapshot;

    /// Data part will not be removed if the pointer owns it
    MergeTreeData::DataPartPtr data_part;
    ImmutableDeleteBitmapPtr delete_bitmap;

    /// Columns we have to read (each Block from read will contain them)
    Names columns_to_read;
    /// whether the next read continues from last reading position and avoids seek
    bool continue_reading;

    Poco::Logger * log = &Poco::Logger::get("MergeTreeFillDeleteWithDefaultValueSource");

    std::shared_ptr<MarkCache> mark_cache;
    using MergeTreeReaderPtr = std::unique_ptr<IMergeTreeReader>;
    MergeTreeReaderPtr reader;

    /// current mark at which we stop reading
    size_t current_mark = 0;

    /// current row at which we stop reading
    size_t current_row = 0;

private:
    size_t currentMarkStart() const { return data_part->index_granularity.getMarkStartingRow(current_mark); }
    size_t currentMarkEnd() const { return data_part->index_granularity.getMarkStartingRow(current_mark + 1); }
    /// REQUIRES: columns should use header's schema
    Columns replaceDeletesWithDefaultValues(Columns columns, const PODArray<UInt8> & is_deleted);
    /// Closes readers and unlock part locks
    void finish();
};


} // namespace DB
