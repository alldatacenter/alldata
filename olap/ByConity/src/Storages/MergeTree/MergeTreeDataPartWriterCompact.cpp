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

#include <Storages/MergeTree/MergeTreeDataPartWriterCompact.h>
#include <Storages/MergeTree/MergeTreeDataPartCompact.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Interpreters/Context.h>
#include <Compression/CompressionFactory.h>
#include <Compression/CompressedReadBufferFromFile.h>
#include <DataTypes/Serializations/ISerialization.h>
#include <DataTypes/Serializations/SerializationNullable.h>
#include <DataTypes/MapHelpers.h>
#include <Common/escapeForFileName.h>
#include <Columns/ColumnByteMap.h>
#include <DataTypes/DataTypeNullable.h>
#include <Common/FieldVisitorToString.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

MergeTreeDataPartWriterCompact::CompactDataWriter::CompactDataWriter(
    const DiskPtr & disk,
    const String & part_path,
    const String & marks_file_extension_,
    const MergeTreeWriterSettings & settings,
    const CompressionCodecPtr default_codec_)
    : default_codec(default_codec_)
    , marks_file_extension(marks_file_extension_)
    , plain_file(disk->writeFile(
          part_path + MergeTreeDataPartCompact::DATA_FILE_NAME_WITH_EXTENSION, {.buffer_size = settings.max_compress_block_size, .mode = WriteMode::Rewrite}))
    , plain_hashing(*plain_file)
    , marks_file(disk->writeFile(part_path + MergeTreeDataPartCompact::DATA_FILE_NAME + marks_file_extension, {.buffer_size = 4096, .mode = WriteMode::Rewrite}))
    , marks(*marks_file)
{
}

void MergeTreeDataPartWriterCompact::CompactDataWriter::next()
{
    plain_hashing.next();
    marks.next();
}

void MergeTreeDataPartWriterCompact::CompactDataWriter::finalize()
{
    plain_file->finalize();
    marks_file->finalize();
}

void MergeTreeDataPartWriterCompact::CompactDataWriter::sync() const
{
    plain_file->sync();
    marks_file->sync();
}

void MergeTreeDataPartWriterCompact::CompactDataWriter::addToChecksums(IMergeTreeDataPart::Checksums & checksums)
{
    String data_file_name = MergeTreeDataPartCompact::DATA_FILE_NAME_WITH_EXTENSION;
    String marks_file_name = MergeTreeDataPartCompact::DATA_FILE_NAME +  marks_file_extension;

    size_t uncompressed_size = 0;
    CityHash_v1_0_2::uint128 uncompressed_hash{0, 0};

    for (const auto & [_, stream] : streams_by_codec)
    {
        uncompressed_size += stream->hashing_buf.count();
        auto stream_hash = stream->hashing_buf.getHash();
        uncompressed_hash = CityHash_v1_0_2::CityHash128WithSeed(
            reinterpret_cast<char *>(&stream_hash), sizeof(stream_hash), uncompressed_hash);
    }

    checksums.files[data_file_name].is_compressed = true;
    checksums.files[data_file_name].uncompressed_size = uncompressed_size;
    checksums.files[data_file_name].uncompressed_hash = uncompressed_hash;
    checksums.files[data_file_name].file_size = plain_hashing.count();
    checksums.files[data_file_name].file_hash = plain_hashing.getHash();

    checksums.files[marks_file_name].file_size = marks.count();
    checksums.files[marks_file_name].file_hash = marks.getHash();
}

void MergeTreeDataPartWriterCompact::CompactDataWriter::addDataStreams(
    const NameAndTypePair & column, const ASTPtr & effective_codec_desc, SerializationsMap & serializations)
{
    IDataType::StreamCallbackWithType callback
        = [&](const ISerialization::SubstreamPath & substream_path, const IDataType & substream_type) {
              String stream_name = ISerialization::getFileNameForStream(column, substream_path);

              /// Shared offsets for Nested type.
              if (compressed_streams.count(stream_name))
                  return;

              CompressionCodecPtr compression_codec;

              /// If we can use special codec than just get it
              if (ISerialization::isSpecialCompressionAllowed(substream_path))
                  compression_codec = CompressionCodecFactory::instance().get(effective_codec_desc, &substream_type, default_codec);
              else /// otherwise return only generic codecs and don't use info about data_type
                  compression_codec = CompressionCodecFactory::instance().get(effective_codec_desc, nullptr, default_codec, true);

              UInt64 codec_id = compression_codec->getHash();
              auto & stream = streams_by_codec[codec_id];
              if (!stream)
                  stream = std::make_shared<CompressedStream>(plain_hashing, compression_codec);

              compressed_streams.emplace(stream_name, stream);
          };

    column.type->enumerateStreams(serializations[column.name], callback);
}

MergeTreeDataPartWriterCompact::MergeTreeDataPartWriterCompact(
    const MergeTreeData::DataPartPtr & data_part_,
    const NamesAndTypesList & columns_list_,
    const StorageMetadataPtr & metadata_snapshot_,
    const std::vector<MergeTreeIndexPtr> & indices_to_recalc_,
    const String & marks_file_extension_,
    const CompressionCodecPtr & default_codec_,
    const MergeTreeWriterSettings & settings_,
    const MergeTreeIndexGranularity & index_granularity_)
    : MergeTreeDataPartWriterOnDisk(data_part_, columns_list_, metadata_snapshot_,
        indices_to_recalc_, marks_file_extension_,
        default_codec_, settings_, index_granularity_)
	, log(&Poco::Logger::get(storage.getLogName() + " (WriterCompact)"))
{
    const auto & storage_columns = metadata_snapshot->getColumns();
    for (const auto & column : columns_list)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
            continue;
        else if (isMapImplicitKeyNotKV(column.name))
            addByteMapStreams({column}, parseMapNameFromImplicitColName(column.name), default_codec->getFullCodecDesc());
        else
        {
            if (!data_writer)
            {
                data_writer = std::make_unique<CompactDataWriter>(data_part->volume->getDisk(), part_path, marks_file_extension, settings, default_codec);
            }
            data_writer->addDataStreams(column, storage_columns.getCodecDescOrDefault(column.name, default_codec), serializations);
        }
    }
}

namespace
{

/// Get granules for block using index_granularity
Granules getGranulesToWrite(const MergeTreeIndexGranularity & index_granularity, size_t block_rows, size_t current_mark, bool last_block)
{
    if (current_mark >= index_granularity.getMarksCount())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Request to get granules from mark {} but index granularity size is {}", current_mark, index_granularity.getMarksCount());

    Granules result;
    size_t current_row = 0;
    while (current_row < block_rows)
    {
        size_t expected_rows_in_mark = index_granularity.getMarkRows(current_mark);
        size_t rows_left_in_block = block_rows - current_row;
        if (rows_left_in_block < expected_rows_in_mark && !last_block)
        {
            /// Invariant: we always have equal amount of rows for block in compact parts because we accumulate them in buffer.
            /// The only exclusion is the last block, when we cannot accumulate more rows.
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Required to write {} rows, but only {} rows was written for the non last granule", expected_rows_in_mark, rows_left_in_block);
        }

        result.emplace_back(Granule{
            .start_row = current_row,
            .rows_to_write = std::min(rows_left_in_block, expected_rows_in_mark),
            .mark_number = current_mark,
            .mark_on_start = true,
            .is_complete = (rows_left_in_block >= expected_rows_in_mark)
        });
        current_row += result.back().rows_to_write;
        current_mark++;
    }

    return result;
}

/// Write single granule of one column (rows between 2 marks)
void writeColumnSingleGranule(
    const ColumnWithTypeAndName & column,
    const SerializationPtr & serialization,
    ISerialization::OutputStreamGetter stream_getter,
    size_t from_row,
    size_t number_of_rows)
{
    ISerialization::SerializeBinaryBulkStatePtr state;
    ISerialization::SerializeBinaryBulkSettings serialize_settings;

    serialize_settings.getter = stream_getter;
    serialize_settings.position_independent_encoding = true; //-V1048
    serialize_settings.low_cardinality_max_dictionary_size = 0; //-V1048

    serialization->serializeBinaryBulkStatePrefix(serialize_settings, state);
    serialization->serializeBinaryBulkWithMultipleStreams(*column.column, from_row, number_of_rows, serialize_settings, state);
    serialization->serializeBinaryBulkStateSuffix(serialize_settings, state);
}

/**
 * @note Just using for merge when handling map implicit column and enable compact map data.
 * Due to ByteMap data should stored in separate files, thus we must use Vertical merge algorithm rather than Horizonal merge algorithm when enable_compact_map_date is true.
 * In that way, all columns except for map column will belong to merged columns, others will belong to gathering columns.
 * The block size read by these two type of stream reader may not be the same, and we need to make sure that all columns in one part have the same index granules, thus we need to handle the case.
 * There are two ways to handle the case:
 * 1. truncate the block to satisfy the condition just like the action in wide format writer.
 * 2. accumulate the data until satisfy the condition.
 * Due to compact format parts are usually small, thus picking up the second solution is also fine.
 */
bool checkGranuleValidityForImplicitColumn(
    const NamesAndTypesList & columns, const MergeTreeIndexGranularity & index_granularity, size_t rows_in_buffer, size_t current_mark)
{
    if (columns.size() > 1 || !isMapImplicitKeyNotKV(columns.front().name))
        return true;

    if (current_mark >= index_granularity.getMarksCount())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Request to get granules from mark {} but index granularity size is {}", current_mark, index_granularity.getMarksCount());

    size_t current_row = 0;
    while (current_row < rows_in_buffer)
    {
        size_t expected_rows_in_mark = index_granularity.getMarkRows(current_mark);
        size_t rows_left_in_block = rows_in_buffer - current_row;
        if (rows_left_in_block < expected_rows_in_mark)
            return false;

        current_row += expected_rows_in_mark;
        current_mark++;
    }

    return true;
}
}

void MergeTreeDataPartWriterCompact::write(const Block & block, const IColumn::Permutation * permutation)
{
    /// Fill index granularity for this block
    /// if it's unknown (in case of insert data or horizontal merge,
    /// but not in case of vertical merge)
    if (compute_granularity)
    {
        size_t index_granularity_for_block = computeIndexGranularity(block);
        fillIndexGranularity(index_granularity_for_block, block.rows());
    }

    Block result_block = permuteBlockIfNeeded(block, permutation);

    if (!header)
        header = result_block.cloneEmpty();

    columns_buffer.add(result_block.mutateColumns());
    size_t current_mark_rows = index_granularity.getMarkRows(getCurrentMark());
    size_t rows_in_buffer = columns_buffer.size();

    if (rows_in_buffer >= current_mark_rows && checkGranuleValidityForImplicitColumn(columns_list, index_granularity, rows_in_buffer, getCurrentMark()))
    {
        Block flushed_block = header.cloneWithColumns(columns_buffer.releaseColumns());
        auto granules_to_write = getGranulesToWrite(index_granularity, flushed_block.rows(), getCurrentMark(), /* last_block = */ false);
        writeDataBlockPrimaryIndexAndSkipIndices(flushed_block, granules_to_write);
        setCurrentMark(getCurrentMark() + granules_to_write.size());
    }
}

void MergeTreeDataPartWriterCompact::writeDataBlockPrimaryIndexAndSkipIndices(const Block & block, const Granules & granules_to_write)
{
    writeDataBlock(block, granules_to_write);

    if (settings.rewrite_primary_key)
    {
        Block primary_key_block = getBlockAndPermute(block, metadata_snapshot->getPrimaryKeyColumns(), nullptr);
        calculateAndSerializePrimaryIndex(primary_key_block, granules_to_write);
    }

    Block skip_indices_block = getBlockAndPermute(block, getSkipIndicesColumns(), nullptr);
    calculateAndSerializeSkipIndices(skip_indices_block, granules_to_write);
}

void MergeTreeDataPartWriterCompact::writeDataBlock(const Block & block, const Granules & granules)
{
    for (const auto & granule : granules)
    {

        auto name_and_type = columns_list.begin();
        for (size_t i = 0; i < columns_list.size(); ++i, ++name_and_type)
        {
            if ((name_and_type->type->isMap() && !name_and_type->type->isMapKVStore()) || isMapImplicitKeyNotKV(name_and_type->name))
                continue;
            if (!data_writer)
                throw Exception("Compact data writer is not initialized but used.", ErrorCodes::LOGICAL_ERROR);
            data_written = true;

            /// Tricky part, because we share compressed streams between different columns substreams.
            /// Compressed streams write data to the single file, but with different compression codecs.
            /// So we flush each stream (using next()) before using new one, because otherwise we will override
            /// data in result file.
            CompressedStreamPtr prev_stream;
            auto stream_getter = [&, this](const ISerialization::SubstreamPath & substream_path) -> WriteBuffer *
            {
                String stream_name = ISerialization::getFileNameForStream(*name_and_type, substream_path);

                auto & result_stream = data_writer->compressed_streams[stream_name];
                /// Write one compressed block per column in granule for more optimal reading.
                if (prev_stream && prev_stream != result_stream)
                {
                    /// Offset should be 0, because compressed block is written for every granule.
                    assert(result_stream->hashing_buf.offset() == 0);
                    prev_stream->hashing_buf.next();
                }

                prev_stream = result_stream;

                return &result_stream->hashing_buf;
            };


            writeIntBinary(data_writer->plain_hashing.count(), data_writer->marks);
            writeIntBinary(UInt64(0), data_writer->marks);

            writeColumnSingleGranule(
                block.getByName(name_and_type->name), serializations[name_and_type->name],
                stream_getter, granule.start_row, granule.rows_to_write);

            /// Each type always have at least one substream
            prev_stream->hashing_buf.next(); //-V522
        }

        if (data_writer)
            writeIntBinary(granule.rows_to_write, data_writer->marks);
    }

    /// write all implicit map column
    writeAllImplicitColumnBlock(block, granules);
}

void MergeTreeDataPartWriterCompact::writeAllImplicitColumnBlock(const Block & block, const Granules & granules)
{
    auto name_and_type = columns_list.begin();
    auto offset_columns = WrittenOffsetColumns{}; /// Nested type will not be the map KV type, so don't need to handle offset column
    for (size_t i = 0; i < columns_list.size(); ++i, ++name_and_type)
    {
        if ((!name_and_type->type->isMap() || name_and_type->type->isMapKVStore()) && !isMapImplicitKeyNotKV(name_and_type->name))
            continue;

        const ColumnWithTypeAndName & column = block.getByName(name_and_type->name);
        data_written = true;

        if (name_and_type->type->isMap() && !name_and_type->type->isMapKVStore())
        {
            if (data_part->versions->enable_compact_map_data)
                writeCompactedByteMapColumn(*name_and_type, *column.column, offset_columns, granules);
            else
                writeUncompactedByteMapColumn(*name_and_type, *column.column, offset_columns, granules);
        }
        else /// handle implcit column directly
            writeColumn(*name_and_type, *column.column, offset_columns, granules);
    }
}

void MergeTreeDataPartWriterCompact::finishDataSerialization(IMergeTreeDataPart::Checksums & checksums, bool sync)
{
    if (columns_buffer.size() != 0)
    {
        auto block = header.cloneWithColumns(columns_buffer.releaseColumns());
        auto granules_to_write = getGranulesToWrite(index_granularity, block.rows(), getCurrentMark(), /* last_block = */ true);
        if (!granules_to_write.back().is_complete)
        {
            /// Correct last mark as it should contain exact amount of rows.
            index_granularity.popMark();
            index_granularity.appendMark(granules_to_write.back().rows_to_write);
            granules_to_write.back().is_complete = true;
        }
        writeDataBlockPrimaryIndexAndSkipIndices(block, granules_to_write);
    }

#ifndef NDEBUG
    if (data_writer)
    {
        /// Offsets should be 0, because compressed block is written for every granule.
        for (const auto & [_, stream] : data_writer->streams_by_codec)
            assert(stream->hashing_buf.offset() == 0);
    }
#endif

    if (with_final_mark && data_written)
    {
        /// Here `serialize_settings` just works for implicit column, which should be consistent with the settings in MergeTreeDataPartWriterOnDisk::writeColumn.
        const auto & global_settings = storage.getContext()->getSettingsRef();
        ISerialization::SerializeBinaryBulkSettings serialize_settings;
        serialize_settings.low_cardinality_max_dictionary_size = global_settings.low_cardinality_max_dictionary_size;
        serialize_settings.low_cardinality_use_single_dictionary_for_part
            = global_settings.low_cardinality_use_single_dictionary_for_part != 0;

        WrittenOffsetColumns offset_columns; /// Nested type will not be the map KV type, so don't need to handle offset column

        auto name_and_type = columns_list.begin();
        for (size_t i = 0; i < columns_list.size(); ++i, ++name_and_type)
        {
            if (name_and_type->type->isMap() && !name_and_type->type->isMapKVStore())
                continue;

            if (isMapImplicitKeyNotKV(name_and_type->name))
            {
                if (!serialization_states.empty())
                {
                    serialize_settings.getter = createStreamGetter(*name_and_type, offset_columns);
                    serializations[name_and_type->name]->serializeBinaryBulkStateSuffix(serialize_settings, serialization_states[name_and_type->name]);
                }

                writeFinalMark(*name_and_type, offset_columns, serialize_settings.path);
            }
            else
            {
                if (!data_writer)
                    throw Exception("Compact data writer is not initialized but used.", ErrorCodes::LOGICAL_ERROR);
                writeIntBinary(data_writer->plain_hashing.count(), data_writer->marks);
                writeIntBinary(UInt64(0), data_writer->marks);
            }
        }
        if (data_writer)
            writeIntBinary(UInt64(0), data_writer->marks);

        // @ByteMap, process implicit columns deduced by ByteMap
        for (auto & cl : implicit_columns_list)
        {
             if (!serialization_states.empty())
            {
                serialize_settings.getter = createStreamGetter(cl, offset_columns);
                serializations[cl.name]->serializeBinaryBulkStateSuffix(serialize_settings, serialization_states[cl.name]);
            }

            writeFinalMark(cl, offset_columns, serialize_settings.path);
        }
    }

    if (data_writer)
    {
        data_writer->next();
        data_writer->addToChecksums(checksums);
        data_writer->finalize();

        if (sync)
            data_writer->sync();
    }

    /// handle implicit column
    for (auto & stream : column_streams)
    {
        stream.second->finalize();
        stream.second->addToChecksums(checksums);
        if (sync)
            stream.second->sync();
    }

    column_streams.clear();
    serialization_states.clear();
}

static void fillIndexGranularityImpl(
    MergeTreeIndexGranularity & index_granularity,
    size_t index_offset,
    size_t index_granularity_for_block,
    size_t rows_in_block)
{
    for (size_t current_row = index_offset; current_row < rows_in_block; current_row += index_granularity_for_block)
    {
        size_t rows_left_in_block = rows_in_block - current_row;

        /// Try to extend last granule if block is large enough
        ///  or it isn't first in granule (index_offset != 0).
        if (rows_left_in_block < index_granularity_for_block &&
            (rows_in_block >= index_granularity_for_block || index_offset != 0))
        {
            // If enough rows are left, create a new granule. Otherwise, extend previous granule.
            // So, real size of granule differs from index_granularity_for_block not more than 50%.
            if (rows_left_in_block * 2 >= index_granularity_for_block)
                index_granularity.appendMark(rows_left_in_block);
            else
                index_granularity.addRowsToLastMark(rows_left_in_block);
        }
        else
        {
            index_granularity.appendMark(index_granularity_for_block);
        }
    }
    index_granularity.setInitialized();
}

void MergeTreeDataPartWriterCompact::fillIndexGranularity(size_t index_granularity_for_block, size_t rows_in_block)
{
    size_t index_offset = 0;
    if (index_granularity.getMarksCount() > getCurrentMark())
        index_offset = index_granularity.getMarkRows(getCurrentMark()) - columns_buffer.size();

    fillIndexGranularityImpl(
        index_granularity,
        index_offset,
        index_granularity_for_block,
        rows_in_block);
}

void MergeTreeDataPartWriterCompact::ColumnsBuffer::add(MutableColumns && columns)
{
    if (accumulated_columns.empty())
        accumulated_columns = std::move(columns);
    else
    {
        for (size_t i = 0; i < columns.size(); ++i)
            accumulated_columns[i]->insertRangeFrom(*columns[i], 0, columns[i]->size());
    }
}

Columns MergeTreeDataPartWriterCompact::ColumnsBuffer::releaseColumns()
{
    Columns res(std::make_move_iterator(accumulated_columns.begin()),
        std::make_move_iterator(accumulated_columns.end()));
    accumulated_columns.clear();
    return res;
}

size_t MergeTreeDataPartWriterCompact::ColumnsBuffer::size() const
{
    if (accumulated_columns.empty())
        return 0;
    return accumulated_columns.at(0)->size();
}

void MergeTreeDataPartWriterCompact::finish(IMergeTreeDataPart::Checksums & checksums, bool sync)
{
    // If we don't have anything to write, skip finalization.
    if (!columns_list.empty())
        finishDataSerialization(checksums, sync);

    if (settings.rewrite_primary_key)
        finishPrimaryIndexSerialization(checksums, sync);

    finishSkipIndicesSerialization(checksums, sync);
}
}
