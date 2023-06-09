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

#include <Storages/MergeTree/IMergeTreeDataPartWriter.h>
#include <IO/WriteBufferFromFile.h>
#include <IO/WriteBufferFromFileBase.h>
#include <Compression/CompressedWriteBuffer.h>
#include <IO/HashingWriteBuffer.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <DataStreams/IBlockOutputStream.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Disks/IDisk.h>


namespace DB
{

/// Single unit for writing data to disk. Contains information about
/// amount of rows to write and marks.
struct Granule
{
    /// Start row in block for granule
    size_t start_row;
    /// Amount of rows from block which have to be written to disk from start_row
    size_t rows_to_write;
    /// Global mark number in the list of all marks (index_granularity) for this part
    size_t mark_number;
    /// Should writer write mark for the first of this granule to disk.
    /// NOTE: Sometimes we don't write mark for the start row, because
    /// this granule can be continuation of the previous one.
    bool mark_on_start;
    /// if true: When this granule will be written to disk all rows for corresponding mark will
    /// be wrtten. It doesn't mean that rows_to_write == index_granularity.getMarkRows(mark_number),
    /// We may have a lot of small blocks between two marks and this may be the last one.
    bool is_complete;
};

struct StreamNameAndMark
{
    String stream_name;
    MarkInCompressedFile mark;
};

using StreamsWithMarks = std::vector<StreamNameAndMark>;
using ColumnNameToMark = std::unordered_map<String, StreamsWithMarks>;
using MapColumnsKeys = std::map<String, std::set<String> >;

/// Multiple granules to write for concrete block.
using Granules = std::vector<Granule>;

/// Writes data part to disk in different formats.
/// Calculates and serializes primary and skip indices if needed.
class MergeTreeDataPartWriterOnDisk : public IMergeTreeDataPartWriter
{
public:
    using WrittenOffsetColumns = std::set<std::string>;

    /// Helper class, which holds chain of buffers to write data file with marks.
    /// It is used to write: one column, skip index or all columns (in compact format).
    struct Stream
    {
        Stream(
            const String & escaped_column_name_,
            DiskPtr disk_,
            const String & data_path_,
            const std::string & data_file_extension_,
            const std::string & marks_path_,
            const std::string & marks_file_extension_,
            const CompressionCodecPtr & compression_codec_,
            size_t max_compress_block_size_,
            bool is_compact_map = false);

        String escaped_column_name;
        std::string data_file_extension;
        std::string marks_file_extension;

        /// compressed -> compressed_buf -> plain_hashing -> plain_file
        std::unique_ptr<WriteBufferFromFileBase> plain_file;
        HashingWriteBuffer plain_hashing;
        CompressedWriteBuffer compressed_buf;
        HashingWriteBuffer compressed;

        /// marks -> marks_file
        std::unique_ptr<WriteBufferFromFileBase> marks_file;
        HashingWriteBuffer marks;

        /// file offset, it's used to distinguish different implicit columns because all implicit column data store in the same file.
        off_t data_file_offset;
        off_t marks_file_offset;

        void finalize();

        void sync() const;

        void addToChecksums(IMergeTreeDataPart::Checksums & checksums);

        void deepCopyTo(Stream& target);
        [[noreturn]] void freeResource();
    };

    using StreamPtr = std::unique_ptr<Stream>;

    MergeTreeDataPartWriterOnDisk(
        const MergeTreeData::DataPartPtr & data_part_,
        const NamesAndTypesList & columns_list,
        const StorageMetadataPtr & metadata_snapshot_,
        const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
        const String & marks_file_extension,
        const CompressionCodecPtr & default_codec,
        const MergeTreeWriterSettings & settings,
        const MergeTreeIndexGranularity & index_granularity);

    void setWrittenOffsetColumns(WrittenOffsetColumns * written_offset_columns_)
    {
        written_offset_columns = written_offset_columns_;
    }

    void setMergeStatus(bool is_merge_)
    {
        is_merge = is_merge_;
    }

protected:
     /// Count index_granularity for block and store in `index_granularity`
    size_t computeIndexGranularity(const Block & block) const;

    /// Write primary index according to granules_to_write
    void calculateAndSerializePrimaryIndex(const Block & primary_index_block, const Granules & granules_to_write);
    /// Write skip indices according to granules_to_write. Skip indices also have their own marks
    /// and one skip index granule can contain multiple "normal" marks. So skip indices serialization
    /// require additional state: skip_indices_aggregators and skip_index_accumulated_marks
    void calculateAndSerializeSkipIndices(const Block & skip_indexes_block, const Granules & granules_to_write);

    /// Finishes primary index serialization: write final primary index row (if required) and compute checksums
    void finishPrimaryIndexSerialization(MergeTreeData::DataPart::Checksums & checksums, bool sync);
    /// Finishes skip indices serialization: write all accumulated data to disk and compute checksums
    void finishSkipIndicesSerialization(MergeTreeData::DataPart::Checksums & checksums, bool sync);

    /// Get global number of the current which we are writing (or going to start to write)
    size_t getCurrentMark() const { return current_mark; }

    void setCurrentMark(size_t mark) { current_mark = mark; }

    /// Get unique non ordered skip indices column.
    Names getSkipIndicesColumns() const;

    void addStreams(
        const NameAndTypePair & column,
        const ASTPtr & effective_codec_desc);

    /// construct an implicit stream for map column (not kv)
    void addByteMapStreams(
         const NameAndTypePair & column, // implicit_name
         const String & col_name,
         const ASTPtr & effective_codec_desc);

    ISerialization::StreamCallback finalizeStreams(const String & name);

    /// Write data of one column.
    /// Return how many marks were written and
    /// how many rows were written for last mark
    void writeColumn(
        const NameAndTypePair & name_and_type,
        const IColumn & column,
        WrittenOffsetColumns & offset_columns,
        const Granules & granules,
        bool need_finalize = false);

    virtual bool canGranuleNotComplete() { return false; }

    virtual size_t getRowsWrittenInLastMark() { return 0; }

    /// Write data of one bytemap column.
    /// Return how many marks were written and
    /// how many rows were written for last mark
    void writeCompactedByteMapColumn(
        const NameAndTypePair & name_and_type,
        const IColumn & column,
        WrittenOffsetColumns & offset_columns,
        const Granules & granules);

    void writeUncompactedByteMapColumn(
        const NameAndTypePair & name_and_type,
        const IColumn & column,
        WrittenOffsetColumns & offset_columns,
        const Granules & granules);

    void deepCopyAndAdd(const String & source_stream_name, const String & target_stream_name, const IDataType & type);

    ISerialization::OutputStreamGetter createStreamGetter(const NameAndTypePair & column, WrittenOffsetColumns & offset_columns) const;

    /// Write single granule of one column.
    void writeSingleGranule(
        const NameAndTypePair & name_and_type,
        const IColumn & column,
        WrittenOffsetColumns & offset_columns,
        ISerialization::SerializeBinaryBulkStatePtr & serialization_state,
        ISerialization::SerializeBinaryBulkSettings & serialize_settings,
        const Granule & granule);

    /// Take offsets from column and return as MarkInCompressed file with stream name
    StreamsWithMarks getCurrentMarksForColumn(
        const NameAndTypePair & column,
        WrittenOffsetColumns & offset_columns,
        ISerialization::SubstreamPath & path);

    /// When writing uncompact map data and handling new key case, except for copy stream, it's necessary to copy last_non_written mark info, just replace the stream name.
    StreamsWithMarks copyLastNonWrittenMarks(
        const NameAndTypePair & source_column,
        const StreamsWithMarks & source_marks,
        const NameAndTypePair & target_column,
        WrittenOffsetColumns & offset_columns,
        ISerialization::SubstreamPath & path);

    /// Write mark to disk using stream and rows count
    void flushMarkToFile(
        const StreamNameAndMark & stream_with_mark,
        size_t rows_in_mark);

    /// Write mark for column taking offsets from column stream
    void writeSingleMark(
        const NameAndTypePair & column,
        WrittenOffsetColumns & offset_columns,
        size_t number_of_rows,
        ISerialization::SubstreamPath & path);

    void writeFinalMark(
        const NameAndTypePair & column,
        WrittenOffsetColumns & offset_columns,
        ISerialization::SubstreamPath & path);

    virtual Poco::Logger * getLogger() = 0;

    const MergeTreeIndices skip_indices;

    const String part_path;
    const String marks_file_extension;
    const CompressionCodecPtr default_codec;

    const bool compute_granularity;

    std::vector<StreamPtr> skip_indices_streams;
    MergeTreeIndexAggregators skip_indices_aggregators;
    std::vector<size_t> skip_index_accumulated_marks;

    using SerializationsMap = std::unordered_map<String, SerializationPtr>;
    SerializationsMap serializations;

    std::unique_ptr<WriteBufferFromFileBase> index_file_stream;
    std::unique_ptr<HashingWriteBuffer> index_stream;
    DataTypes index_types;
    /// Index columns from the last block
    /// It's written to index file in the `writeSuffixAndFinalizePart` method
    Columns last_block_index_columns;

    bool data_written = false;

    /// To correctly write Nested elements column-by-column.
    WrittenOffsetColumns * written_offset_columns = nullptr;

    /// Data is already written up to this mark.
    size_t current_mark = 0;

    using ColumnStreams = std::map<String, StreamPtr>;
    ColumnStreams column_streams;

    using SerializationState = ISerialization::SerializeBinaryBulkStatePtr;
    using SerializationStates = std::unordered_map<String, SerializationState>;

    SerializationStates serialization_states;

    /// Non written marks to disk (for each column). Waiting until all rows for
    /// this marks will be written to disk.
    using MarksForColumns = std::unordered_map<String, StreamsWithMarks>;
    MarksForColumns last_non_written_marks;

    MapColumnsKeys exist_keys_names;
	NamesAndTypesList implicit_columns_list;

    bool optimize_map_column_serialization = false;

    /// This parameter is used in following cases:
    /// 1. write compact map
    /// 2. write row store for unique table
    /// In other cases, this parameter can not reflect the correct merge status.
    bool is_merge = false;

private:
    void initSkipIndices();
    void initPrimaryIndex();

    virtual void fillIndexGranularity(size_t index_granularity_for_block, size_t rows_in_block) = 0;
};

}
