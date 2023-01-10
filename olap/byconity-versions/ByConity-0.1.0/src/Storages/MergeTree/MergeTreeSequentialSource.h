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
#include <Processors/Sources/SourceWithProgress.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/IMergeTreeReader.h>
#include <Storages/MergeTree/MarkRange.h>
#include <memory>

namespace DB
{

/// Lightweight (in terms of logic) stream for reading single part from MergeTree
class MergeTreeSequentialSource : public SourceWithProgress
{
public:
    /// NOTE: in case you want to read part with row id included, please add extra `_part_row_number` to
    /// the columns you want to read.
    MergeTreeSequentialSource(
        const MergeTreeMetaBase & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        MergeTreeMetaBase::DataPartPtr data_part_,
        Names columns_to_read_,
        bool read_with_direct_io_,
        bool take_column_types_from_storage,
        bool quiet = false);

    MergeTreeSequentialSource(
        const MergeTreeMetaBase & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        MergeTreeMetaBase::DataPartPtr data_part_,
        ImmutableDeleteBitmapPtr delete_bitmap_,
        Names columns_to_read_,
        bool read_with_direct_io_,
        bool take_column_types_from_storage,
        bool quiet = false);

    ~MergeTreeSequentialSource() override;

    String getName() const override { return "MergeTreeSequentialSource"; }

    size_t getCurrentMark() const { return current_mark; }

    size_t getCurrentRow() const { return current_row; }

protected:
    Chunk generate() override;

private:

    const MergeTreeMetaBase & storage;
    StorageMetadataPtr metadata_snapshot;

    /// Data part will not be removed if the pointer owns it
    MergeTreeMetaBase::DataPartPtr data_part;
    ImmutableDeleteBitmapPtr delete_bitmap;

    /// Columns we have to read (each Block from read will contain them)
    Names columns_to_read;
    bool continue_reading = false;

    /// Should read using direct IO
    bool read_with_direct_io;

    Poco::Logger * log = &Poco::Logger::get("MergeTreeSequentialSource");

    std::shared_ptr<MarkCache> mark_cache;
    using MergeTreeReaderPtr = std::unique_ptr<IMergeTreeReader>;
    MergeTreeReaderPtr reader;

    /// current mark at which we stop reading
    size_t current_mark = 0;

    /// current row at which we stop reading
    size_t current_row = 0;

private:
    /// Closes readers and unlock part locks
    void finish();
    size_t currentMarkStart() const { return data_part->index_granularity.getMarkStartingRow(current_mark); }
    size_t currentMarkEnd() const { return data_part->index_granularity.getMarkStartingRow(current_mark + 1); }
};

}
