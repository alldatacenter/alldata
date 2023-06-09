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
#include <Storages/IndexFile/IndexFileWriter.h>
#include <Storages/MergeTree/MergeTreeDataPartWriterOnDisk.h>

namespace rocksdb
{
    class DB;
}

namespace IndexFile
{
    struct IndexFileInfo;
}

namespace DB
{

/// Writes data part in wide format.
class MergeTreeDataPartWriterWide : public MergeTreeDataPartWriterOnDisk
{
public:
    MergeTreeDataPartWriterWide(
        const MergeTreeData::DataPartPtr & data_part,
        const NamesAndTypesList & columns_list,
        const StorageMetadataPtr & metadata_snapshot,
        const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
        const String & marks_file_extension,
        const CompressionCodecPtr & default_codec,
        const MergeTreeWriterSettings & settings,
        const MergeTreeIndexGranularity & index_granularity);

    ~MergeTreeDataPartWriterWide() override;

    void write(const Block & block, const IColumn::Permutation * permutation) override;

    void finish(IMergeTreeDataPart::Checksums & checksums, bool sync) final;

    void updateWriterStream(const NameAndTypePair &pair) override;

private:

    /// Finish serialization of data: write final mark if required and compute checksums
    /// Also validate written data in debug mode
    void finishDataSerialization(IMergeTreeDataPart::Checksums & checksums, bool sync);

    /// Method for self check (used in debug-build only). Checks that written
    /// data and corresponding marks are consistent. Otherwise throws logical
    /// errors.
    void validateColumnOfFixedSize(const String & name, const IDataType & type);

    void fillIndexGranularity(size_t index_granularity_for_block, size_t rows_in_block) override;

    /// Use information from just written granules to shift current mark
    /// in our index_granularity array.
    void shiftCurrentMark(const Granules & granules_written);

    /// Change rows in the last mark in index_granularity to new_rows_in_last_mark.
    /// Flush all marks from last_non_written_marks to disk and increment current mark if already written rows
    /// (rows_written_in_last_granule) equal to new_rows_in_last_mark.
    ///
    /// This function used when blocks change granularity drastically and we have unfinished mark.
    /// Also useful to have exact amount of rows in last (non-final) mark.
    void adjustLastMarkIfNeedAndFlushToDisk(size_t new_rows_in_last_mark);

    bool canGranuleNotComplete() override { return true; }

    size_t getRowsWrittenInLastMark() override { return rows_written_in_last_mark; }

    Poco::Logger * getLogger() override { return log; }

    /// How many rows we have already written in the current mark.
    /// More than zero when incoming blocks are smaller then their granularity.
    size_t rows_written_in_last_mark = 0;

    Poco::Logger * log;

    /** ------------------ Unique Table Only --------------------- **/
    void writeUniqueKeyIndex(Block & unique_key_block);
    void writeToTempUniqueKeyIndex(Block & block, size_t first_rid, rocksdb::DB & temp_index);
    void closeTempUniqueKeyIndex();
    void writeFinalUniqueKeyIndexFile(IndexFile::IndexFileInfo & file_info);

    size_t rows_count = 0;

    /// If the part contains only one block (normal insert case), we generate the key index file
    /// directly from the buffered block, avoiding the overhead of "temp_unique_key_index"
    Block buffered_unique_key_block;
    /// If the part contains more than one blocks (merge case), we first use "temp_unique_key_index"
    /// to sort and persist index entries, then generate the key index file from "temp_unique_key_index"
    String temp_unique_key_index_dir;
    rocksdb::DB * temp_unique_key_index = nullptr;
};

}
