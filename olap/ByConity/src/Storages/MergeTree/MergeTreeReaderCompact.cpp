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

#include <Storages/MergeTree/MergeTreeReaderCompact.h>
#include <Storages/MergeTree/MergeTreeDataPartCompact.h>
#include <Columns/ColumnByteMap.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/MapHelpers.h>
#include <DataTypes/NestedUtils.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_ALL_DATA;
    extern const int ARGUMENT_OUT_OF_BOUND;
    extern const int MEMORY_LIMIT_EXCEEDED;
}

MergeTreeReaderCompact::CompactDataReader::CompactDataReader(
    const MergeTreeData::DataPartPtr & data_part,
    UncompressedCache * uncompressed_cache,
    MarkCache * mark_cache,
    const MergeTreeReaderSettings & settings,
    const ColumnPositions & column_positions_,
    const MarkRanges & all_mark_ranges,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback_,
    clockid_t clock_type_)
    : marks_loader(
        data_part->volume->getDisk(),
        mark_cache,
        data_part->index_granularity_info.getMarksFilePath(data_part->getFullRelativePath() + MergeTreeDataPartCompact::DATA_FILE_NAME),
        MergeTreeDataPartCompact::DATA_FILE_NAME,
        data_part->getMarksCount(),
        data_part->index_granularity_info,
        settings.save_marks_in_cache,
        data_part->getFileOffsetOrZero(data_part->index_granularity_info.getMarksFilePath(MergeTreeDataPartCompact::DATA_FILE_NAME)),
        data_part->getFileSizeOrZero(data_part->index_granularity_info.getMarksFilePath(MergeTreeDataPartCompact::DATA_FILE_NAME)),
        std::dynamic_pointer_cast<const MergeTreeDataPartCompact>(data_part)->getColumnsWithoutByteMapColSize())
{
    // Do not use max_read_buffer_size, but try to lower buffer size with maximal size of granule to avoid reading much data.
    auto buffer_size = getReadBufferSize(data_part, marks_loader, column_positions_, all_mark_ranges);
    if (!buffer_size || settings.max_read_buffer_size < buffer_size)
        buffer_size = settings.max_read_buffer_size;
    // auto buffer_size = settings.max_read_buffer_size;

    const String full_data_path = data_part->getFullRelativePath() + MergeTreeDataPartCompact::DATA_FILE_NAME_WITH_EXTENSION;
    if (uncompressed_cache)
    {
        auto buffer = std::make_unique<CachedCompressedReadBuffer>(
            fullPath(data_part->volume->getDisk(), full_data_path),
            [&, full_data_path, buffer_size]() {
                return data_part->volume->getDisk()->readFile(
                    full_data_path,
                    {
                        .buffer_size = buffer_size,
                        .estimated_size = 0,
                        .aio_threshold = settings.min_bytes_to_use_direct_io,
                        .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                        .mmap_cache = settings.mmap_cache.get()
                    }
                );
            },
            uncompressed_cache,
            /* allow_different_codecs = */ true);

        if (profile_callback_)
            buffer->setProfileCallback(profile_callback_, clock_type_);

        if (!settings.checksum_on_read)
            buffer->disableChecksumming();

        cached_buffer = std::move(buffer);
        data_buffer = cached_buffer.get();
    }
    else
    {
        auto buffer = std::make_unique<CompressedReadBufferFromFile>(
            data_part->volume->getDisk()->readFile(
                full_data_path,
                {
                    .buffer_size = buffer_size,
                    .estimated_size = 0,
                    .aio_threshold = settings.min_bytes_to_use_direct_io,
                    .mmap_threshold = settings.min_bytes_to_use_mmap_io,
                    .mmap_cache = settings.mmap_cache.get()
                }
            ),
            /* allow_different_codecs = */ true);

        if (profile_callback_)
            buffer->setProfileCallback(profile_callback_, clock_type_);

        if (!settings.checksum_on_read)
            buffer->disableChecksumming();

        non_cached_buffer = std::move(buffer);
        data_buffer = non_cached_buffer.get();
    }
}

void MergeTreeReaderCompact::CompactDataReader::seekToMark(size_t row_index, size_t column_index)
{
    MarkInCompressedFile mark = marks_loader.getMark(row_index, column_index);
    try
    {
        if (cached_buffer)
            cached_buffer->seek(mark.offset_in_compressed_file, mark.offset_in_decompressed_block);
        if (non_cached_buffer)
            non_cached_buffer->seek(mark.offset_in_compressed_file, mark.offset_in_decompressed_block);
    }
    catch (Exception & e)
    {
        /// Better diagnostics.
        if (e.code() == ErrorCodes::ARGUMENT_OUT_OF_BOUND)
            e.addMessage("(while seeking to mark (" + toString(row_index) + ", " + toString(column_index) + ")");

        throw;
    }
}

MergeTreeReaderCompact::MergeTreeReaderCompact(
    DataPartCompactPtr data_part_,
    NamesAndTypesList columns_,
    const StorageMetadataPtr & metadata_snapshot_,
    UncompressedCache * uncompressed_cache_,
    MarkCache * mark_cache_,
    MarkRanges mark_ranges_,
    MergeTreeReaderSettings settings_,
    ValueSizeMap avg_value_size_hints_,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback_,
    clockid_t clock_type_)
    : IMergeTreeReader(
        std::move(data_part_),
        std::move(columns_),
        metadata_snapshot_,
        uncompressed_cache_,
        mark_cache_,
        std::move(mark_ranges_),
        std::move(settings_),
        std::move(avg_value_size_hints_))
{
    try
    {
        size_t columns_num = columns.size();

        column_positions.resize(columns_num);
        read_only_offsets.resize(columns_num);
        auto name_and_type = columns.begin();
        auto compact_part = std::dynamic_pointer_cast<const MergeTreeDataPartCompact>(data_part);
        if (!compact_part)
            throw Exception("Can not convert part into MergeTreeDataPartCompact in MergeTreeReaderCompact.", ErrorCodes::LOGICAL_ERROR);
        for (size_t i = 0; i < columns_num; ++i, ++name_and_type)
        {
            if (name_and_type->name == "_part_row_number")
                continue;

            NameAndTypePair column_from_part;

            if (isMapKV(name_and_type->name))
            {
                auto & part_all_columns = data_part->getColumns();
                auto map_column = std::find_if(part_all_columns.begin(), part_all_columns.end(), [&](auto & val) {
                    return val.name + ".key" == name_and_type->name || val.name + ".value" == name_and_type->name;
                });
                if (map_column == part_all_columns.end())
                    throw Exception("Map column of map kv type doesn't exist.", ErrorCodes::LOGICAL_ERROR);
                map_kv_to_origin_col[name_and_type->name] = *map_column;
                column_from_part = getColumnFromPart(*map_column);
            }
            else
                column_from_part = getColumnFromPart(*name_and_type);

            auto position = compact_part->getColumnPositionWithoutMap(column_from_part.name);
            if (!position && typeid_cast<const DataTypeArray *>(column_from_part.type.get()))
            {
                /// If array of Nested column is missing in part,
                /// we have to read its offsets if they exist.
                position = findColumnForOffsets(column_from_part.name);
                read_only_offsets[i] = (position != std::nullopt);
            }

            column_positions[i] = std::move(position);
        }

        for (const NameAndTypePair & column : columns)
        {
            if (column.name == "_part_row_number")
                continue;

            auto column_from_part = getColumnFromPart(column);
            if (column_from_part.type->isMap() && !column_from_part.type->isMapKVStore())
            {
                // Scan the directory to get all implicit columns(stream) for the map type
                const DataTypeByteMap & type_map = typeid_cast<const DataTypeByteMap &>(*column_from_part.type);

                String key_name;
                String impl_key_name;
                {
                    for (auto & file : data_part->getChecksums()->files)
                    {
                        //Try to get keys, and form the stream, its bin file name looks like "NAME__xxxxx.bin"
                        const String & file_name = file.first;
                        if (isMapImplicitDataFileNameNotBaseOfSpecialMapName(file_name, column.name))
                        {
                            key_name = parseKeyNameFromImplicitFileName(file_name, column.name);
                            impl_key_name = getImplicitColNameForMapKey(column.name, key_name);
                            // Special handing if implicit key is referenced too
                            if (columns.contains(impl_key_name))
                            {
                                dup_implicit_keys.insert(impl_key_name);
                            }

                            addByteMapStreams(
                                {impl_key_name, type_map.getValueTypeForImplicitColumn()}, column.name, profile_callback_, clock_type_);
                            map_column_keys.insert({column.name, key_name});
                        }
                    }
                }
            }
            else if (isMapImplicitKeyNotKV(column.name)) // check if it's an implicit key and not KV
            {
                addByteMapStreams({column.name, column.type}, parseMapNameFromImplicitColName(column.name), profile_callback_, clock_type_);
            }
            else
            {
                if (!data_reader)
                {
                    data_reader = std::make_unique<CompactDataReader>(
                        data_part,
                        uncompressed_cache,
                        mark_cache,
                        settings,
                        column_positions,
                        all_mark_ranges,
                        profile_callback_,
                        clock_type_);
                }
            }
        }

        if (!dup_implicit_keys.empty()) names = columns.getNames();
    }
    catch (...)
    {
        storage.reportBrokenPart(data_part->name);
        throw;
    }
}

size_t MergeTreeReaderCompact::readRows(size_t from_mark, bool continue_reading, size_t max_rows_to_read, Columns & res_columns)
{
    if (continue_reading)
        from_mark = next_mark;

    size_t read_rows = 0;
    size_t num_columns = columns.size();
    checkNumberOfColumns(num_columns);

    MutableColumns mutable_columns(num_columns);
    std::unordered_map<String, size_t> res_col_to_idx;
    auto column_it = columns.begin();
    for (size_t i = 0; i < num_columns; ++i, ++column_it)
    {
        const auto & [name, type] = getColumnFromPart(*column_it);
        res_col_to_idx[name] = i;

        // Each implicit map column is stored in a seperate file whose column_postition is nullptr.
        if ((type->isMap() && !type->isMapKVStore()) || isMapImplicitKeyNotKV(name) || column_positions[i] || name == "_part_row_number")
        {
            if (res_columns[i] == nullptr)
                res_columns[i] = type->createColumn();
        }
    }

    auto sort_columns = columns;
    if (!dup_implicit_keys.empty())
        sort_columns.sort([](const auto & lhs, const auto & rhs) { return (!lhs.type->isMap()) && rhs.type->isMap(); });

    while (read_rows < max_rows_to_read)
    {
        size_t rows_to_read = data_part->index_granularity.getMarkRows(from_mark);

        std::unordered_map<String, ISerialization::SubstreamsCache> caches;
        int row_number_column_pos = -1;
        auto name_and_type = sort_columns.begin();
        for (size_t i = 0; i < num_columns; ++i, ++name_and_type)
        {
            auto column_from_part = getColumnFromPart(*name_and_type);

            const auto & [name, type] = column_from_part;
            size_t pos = res_col_to_idx[name];

            /// This case will happen when querying the column that is not included after adding new column.
            if (!res_columns[pos])
                continue;

            /// row number column will be populated at last after `read_rows` is set
            if (name == "_part_row_number")
            {
                row_number_column_pos = pos;
                continue;
            }

            try
            {
                auto & column = res_columns[pos];
                size_t column_size_before_reading = column->size();
                auto & cache = caches[column_from_part.getNameInStorage()];

                if (type->isMap() && !type->isMapKVStore())
                    readMapDataNotKV(
                        column_from_part, column, from_mark, continue_reading, rows_to_read, caches, res_col_to_idx, res_columns);
                else if (isMapImplicitKeyNotKV(name))
                    readData(column_from_part, column, from_mark, continue_reading, rows_to_read, cache);
                else
                {
                    if (!data_reader)
                        throw Exception("Compact data reader is not initialized but used.", ErrorCodes::LOGICAL_ERROR);
                    if (map_kv_to_origin_col.count(name_and_type->name))
                    {
                        auto map_col = map_kv_to_origin_col[name_and_type->name];
                        ColumnPtr map_column = map_col.type->createColumn();
                        readCompactData(map_col, map_column, from_mark, *column_positions[pos], rows_to_read, read_only_offsets[pos]);
                        const ColumnByteMap & column_map = typeid_cast<const ColumnByteMap &>(*map_column);
                        if (map_col.name + ".key" == name_and_type->name)
                        {
                            auto key_store_column = column_map.getKeyStorePtr();
                            if (column->empty())
                                column = key_store_column;
                            else
                                column->assumeMutable()->insertRangeFrom(*key_store_column, 0, key_store_column->size());
                        }
                        else /// handle .value
                        {
                            auto value_store_column = column_map.getValueStorePtr();
                            if (column->empty())
                                column = value_store_column;
                            else
                                column->assumeMutable()->insertRangeFrom(*value_store_column, 0, value_store_column->size());
                        }
                    }
                    else
                        readCompactData(column_from_part, column, from_mark, *column_positions[pos], rows_to_read, read_only_offsets[pos]);
                }

                if (column->empty())
                {
                    /// The case will happen in the following cases:
                    /// 1. When query a map implicit column that doesn't exist.
                    /// 2. When query a map column that doesn't have any key.
                    res_columns[pos] = nullptr;
                }
                else
                {
                    size_t read_rows_in_column = column->size() - column_size_before_reading;
                    if (read_rows_in_column < rows_to_read)
                        throw Exception(
                            ErrorCodes::CANNOT_READ_ALL_DATA,
                            "Cannot read all data in MergeTreeReaderCompact. Rows read: {}. Rows expected: {}.",
                            read_rows_in_column,
                            rows_to_read);
                }
            }
            catch (Exception & e)
            {
                if (e.code() != ErrorCodes::MEMORY_LIMIT_EXCEEDED)
                    storage.reportBrokenPart(data_part->name);

                /// Better diagnostics.
                e.addMessage("(while reading column " + column_from_part.name + ")");
                throw;
            }
            catch (...)
            {
                storage.reportBrokenPart(data_part->name);
                throw;
            }
        }

        /// Populate _part_row_number column if requested
        if (row_number_column_pos >= 0)
        {
            auto mutable_column = res_columns[row_number_column_pos]->assumeMutable();
            ColumnUInt64 & column = assert_cast<ColumnUInt64 &>(*mutable_column);
            size_t row_number = data_part->index_granularity.getMarkStartingRow(from_mark);
            for (size_t i = 0; i < rows_to_read; ++i)
                column.insertValue(row_number++);
            res_columns[row_number_column_pos] = std::move(mutable_column);
        }

        ++from_mark;
        read_rows += rows_to_read;
    }

    next_mark = from_mark;

    return read_rows;
}

void MergeTreeReaderCompact::readCompactData(
    const NameAndTypePair & name_and_type, ColumnPtr & column,
    size_t from_mark, size_t column_position, size_t rows_to_read, bool only_offsets)
{
    const auto & [name, type] = name_and_type;

    if (!isContinuousReading(from_mark, column_position))
        data_reader->seekToMark(from_mark, column_position);

    auto buffer_getter = [&](const ISerialization::SubstreamPath & substream_path) -> ReadBuffer *
    {
        if (only_offsets && (substream_path.size() != 1 || substream_path[0].type != ISerialization::Substream::ArraySizes))
            return nullptr;

        return data_reader->data_buffer;
    };

    ISerialization::DeserializeBinaryBulkStatePtr state;
    ISerialization::DeserializeBinaryBulkSettings deserialize_settings;
    deserialize_settings.getter = buffer_getter;
    deserialize_settings.avg_value_size_hint = avg_value_size_hints[name];

    if (name_and_type.isSubcolumn())
    {
        auto type_in_storage = name_and_type.getTypeInStorage();
        ColumnPtr temp_column = type_in_storage->createColumn();

        auto serialization = type_in_storage->getDefaultSerialization();
        serialization->deserializeBinaryBulkStatePrefix(deserialize_settings, state);
        serialization->deserializeBinaryBulkWithMultipleStreams(temp_column, rows_to_read, deserialize_settings, state, nullptr);

        auto subcolumn = type_in_storage->getSubcolumn(name_and_type.getSubcolumnName(), *temp_column);

        /// TODO: Avoid extra copying.
        if (column->empty())
            column = subcolumn;
        else
            column->assumeMutable()->insertRangeFrom(*subcolumn, 0, subcolumn->size());
    }
    else
    {
        auto serialization = type->getDefaultSerialization();
        serialization->deserializeBinaryBulkStatePrefix(deserialize_settings, state);
        serialization->deserializeBinaryBulkWithMultipleStreams(column, rows_to_read, deserialize_settings, state, nullptr);
    }

    /// The buffer is left in inconsistent state after reading single offsets
    if (only_offsets)
        last_read_granule.reset();
    else
        last_read_granule.emplace(from_mark, column_position);
}

IMergeTreeReader::ColumnPosition MergeTreeReaderCompact::findColumnForOffsets(const String & column_name) const
{
    String table_name = Nested::extractTableName(column_name);
    for (const auto & part_column : data_part->getColumns())
    {
        if (typeid_cast<const DataTypeArray *>(part_column.type.get()))
        {
            auto compact_part = std::dynamic_pointer_cast<const MergeTreeDataPartCompact>(data_part);
            if (!compact_part)
                throw Exception("Can not convert part into MergeTreeDataPartCompact in MergeTreeReaderCompact.", ErrorCodes::LOGICAL_ERROR);
            auto position = compact_part->getColumnPositionWithoutMap(part_column.name);
            if (position && Nested::extractTableName(part_column.name) == table_name)
                return position;
        }
    }

    return {};
}

bool MergeTreeReaderCompact::isContinuousReading(size_t mark, size_t column_position)
{
    if (!last_read_granule)
        return false;
    const auto & [last_mark, last_column] = *last_read_granule;
    return (mark == last_mark && column_position == last_column + 1)
        || (mark == last_mark + 1 && column_position == 0 && last_column == data_part->getColumns().size() - 1);
}

namespace
{

/// A simple class that helps to iterate over 2-dim marks of compact parts.
class MarksCounter
{
public:
    MarksCounter(size_t rows_num_, size_t columns_num_)
        : rows_num(rows_num_), columns_num(columns_num_) {}

    struct Iterator
    {
        size_t row;
        size_t column;
        MarksCounter * counter;

        Iterator(size_t row_, size_t column_, MarksCounter * counter_)
            : row(row_), column(column_), counter(counter_) {}

        Iterator operator++()
        {
            if (column + 1 == counter->columns_num)
            {
                ++row;
                column = 0;
            }
            else
            {
                ++column;
            }

            return *this;
        }

        bool operator==(const Iterator & other) const { return row == other.row && column == other.column; }
        bool operator!=(const Iterator & other) const { return !(*this == other); }
    };

    Iterator get(size_t row, size_t column) { return Iterator(row, column, this); }
    Iterator end() { return get(rows_num, 0); }

private:
    size_t rows_num;
    size_t columns_num;
};

}

size_t MergeTreeReaderCompact::getReadBufferSize(
    const DataPartPtr & part,
    MergeTreeMarksLoader & marks_loader,
    const ColumnPositions & column_positions,
    const MarkRanges & mark_ranges)
{
    size_t buffer_size = 0;
    size_t columns_num = column_positions.size();
    size_t file_size = part->getFileSizeOrZero(MergeTreeDataPartCompact::DATA_FILE_NAME_WITH_EXTENSION);

    auto compact_part = std::dynamic_pointer_cast<const MergeTreeDataPartCompact>(part);
    if (!compact_part)
        throw Exception("Can not convert part into MergeTreeDataPartCompact in MergeTreeReaderCompact.", ErrorCodes::LOGICAL_ERROR);
    MarksCounter counter(part->getMarksCount(), compact_part->getColumnsWithoutByteMapColSize());

    for (const auto & mark_range : mark_ranges)
    {
        for (size_t mark = mark_range.begin; mark < mark_range.end; ++mark)
        {
            for (size_t i = 0; i < columns_num; ++i)
            {
                if (!column_positions[i])
                    continue;

                auto it = counter.get(mark, *column_positions[i]);
                size_t cur_offset = marks_loader.getMark(it.row, it.column).offset_in_compressed_file;

                while (it != counter.end() && cur_offset == marks_loader.getMark(it.row, it.column).offset_in_compressed_file)
                    ++it;

                size_t next_offset = (it == counter.end() ? file_size : marks_loader.getMark(it.row, it.column).offset_in_compressed_file);
                buffer_size = std::max(buffer_size, next_offset - cur_offset);
            }
        }
    }

    return buffer_size;
}

}
