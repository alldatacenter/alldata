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

#include <Storages/MergeTree/MergeTreeReaderWide.h>

#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/MapHelpers.h>
#include <DataTypes/NestedUtils.h>
#include <Interpreters/inplaceBlockConversions.h>
#include <Storages/MergeTree/IMergeTreeReader.h>
#include <Storages/MergeTree/MergeTreeDataPartWide.h>
#include <Common/escapeForFileName.h>
#include <Common/typeid_cast.h>
#include <utility>

namespace DB
{

namespace
{
    using OffsetColumns = std::map<std::string, ColumnPtr>;
}

namespace ErrorCodes
{
    extern const int MEMORY_LIMIT_EXCEEDED;
}

MergeTreeReaderWide::MergeTreeReaderWide(
    DataPartWidePtr data_part_,
    NamesAndTypesList columns_,
    const StorageMetadataPtr & metadata_snapshot_,
    UncompressedCache * uncompressed_cache_,
    MarkCache * mark_cache_,
    MarkRanges mark_ranges_,
    MergeTreeReaderSettings settings_,
    IMergeTreeDataPart::ValueSizeMap avg_value_size_hints_,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback_,
    clockid_t clock_type_)
    : IMergeTreeReader(
        std::move(data_part_),
        std::move(columns_),
        metadata_snapshot_,
        uncompressed_cache_,
        std::move(mark_cache_),
        std::move(mark_ranges_),
        std::move(settings_),
        std::move(avg_value_size_hints_))
{
    try
    {
        for (const NameAndTypePair & column : columns)
        {
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
                        // Try to get keys, and form the stream, its bin file name looks like "NAME__xxxxx.bin"
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
            else if (column.name != "_part_row_number")
            {
                addStreams(column_from_part, profile_callback_, clock_type_);
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

size_t MergeTreeReaderWide::readRows(size_t from_mark, bool continue_reading, size_t max_rows_to_read, Columns & res_columns)
{
    if (!continue_reading)
        next_row_number_to_read = data_part->index_granularity.getMarkStartingRow(from_mark);

    size_t read_rows = 0;
    try
    {
        size_t num_columns = columns.size();
        checkNumberOfColumns(num_columns);

        std::unordered_map<String, size_t> res_col_to_idx;
        auto column_it = columns.begin();
        for (size_t i = 0; i < num_columns; ++i, ++column_it)
        {
            const auto & [name, type] = getColumnFromPart(*column_it);
            res_col_to_idx[name] = i;
        }

        auto sort_columns = columns;
        if (!dup_implicit_keys.empty())
            sort_columns.sort([](const auto & lhs, const auto & rhs) { return (!lhs.type->isMap()) && rhs.type->isMap(); });

        /// Pointers to offset columns that are common to the nested data structure columns.
        /// If append is true, then the value will be equal to nullptr and will be used only to
        /// check that the offsets column has been already read.
        OffsetColumns offset_columns;
        std::unordered_map<String, ISerialization::SubstreamsCache> caches;

        int row_number_column_pos = -1;
        auto name_and_type = sort_columns.begin();
        for (size_t i = 0; i < num_columns; ++i, ++name_and_type)
        {
            auto column_from_part = getColumnFromPart(*name_and_type);
            const auto & [name, type] = column_from_part;
            size_t pos = res_col_to_idx[name];

            /// The column is already present in the block so we will append the values to the end.
            bool append = res_columns[pos] != nullptr;
            if (!append)
                res_columns[pos] = type->createColumn();

            /// row number column will be populated at last after `read_rows` is set
            if (name == "_part_row_number")
            {
                row_number_column_pos = pos;
                continue;
            }

            auto & column = res_columns[pos];
            try
            {
                size_t column_size_before_reading = column->size();
                auto & cache = caches[column_from_part.getNameInStorage()];
                if (type->isMap() && !type->isMapKVStore())
                    readMapDataNotKV(
                        column_from_part, column, from_mark, continue_reading, max_rows_to_read, caches, res_col_to_idx, res_columns);
                else
                    readData(column_from_part, column, from_mark, continue_reading, max_rows_to_read, cache);

                /// For elements of Nested, column_size_before_reading may be greater than column size
                ///  if offsets are not empty and were already read, but elements are empty.
                if (!column->empty())
                    read_rows = std::max(read_rows, column->size() - column_size_before_reading);
            }
            catch (Exception & e)
            {
                /// Better diagnostics.
                e.addMessage("(while reading column " + name + ")");
                throw;
            }

            if (column->empty())
                res_columns[pos] = nullptr;
        }

        /// Populate _part_row_number column if requested
        if (row_number_column_pos >= 0)
        {
            /// update `read_rows` if no physical columns are read (only _part_row_number is requested)
            if (columns.size() == 1)
            {
                read_rows = std::min(max_rows_to_read, data_part->rows_count - next_row_number_to_read);
            }

            if (read_rows)
            {
                auto mutable_column = res_columns[row_number_column_pos]->assumeMutable();
                ColumnUInt64 & column = assert_cast<ColumnUInt64 &>(*mutable_column);
                for (size_t i = 0, row_number = next_row_number_to_read; i < read_rows; ++i)
                    column.insertValue(row_number++);
                res_columns[row_number_column_pos] = std::move(mutable_column);
            }
            else
            {
                res_columns[row_number_column_pos] = nullptr;
            }
        }

        /// NOTE: positions for all streams must be kept in sync.
        /// In particular, even if for some streams there are no rows to be read,
        /// you must ensure that no seeks are skipped and at this point they all point to to_mark.
    }
    catch (Exception & e)
    {
        if (e.code() != ErrorCodes::MEMORY_LIMIT_EXCEEDED)
            storage.reportBrokenPart(data_part->name);

        /// Better diagnostics.
        e.addMessage("(while reading from part " + data_part->getFullPath() + " "
                     "from mark " + toString(from_mark) + " "
                     "with max_rows_to_read = " + toString(max_rows_to_read) + ")");
        throw;
    }
    catch (...)
    {
        storage.reportBrokenPart(data_part->name);

        throw;
    }

    next_row_number_to_read += read_rows;
    return read_rows;
}

void MergeTreeReaderWide::addStreams(const NameAndTypePair & name_and_type,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback, clockid_t clock_type)
{
    ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath & substream_path) {
        String stream_name = ISerialization::getFileNameForStream(name_and_type, substream_path);

        if (streams.count(stream_name))
            return;

        bool data_file_exists = data_part->getChecksums()->files.count(stream_name + DATA_FILE_EXTENSION);

        /** If data file is missing then we will not try to open it.
          * It is necessary since it allows to add new column to structure of the table without creating new files for old parts.
          */
        if (!data_file_exists)
            return;

        streams.emplace(
            stream_name,
            std::make_unique<MergeTreeReaderStream>(
                data_part->volume->getDisk(),
                data_part->getFullRelativePath() + stream_name,
                stream_name,
                DATA_FILE_EXTENSION,
                data_part->getMarksCount(),
                all_mark_ranges,
                settings,
                mark_cache,
                uncompressed_cache,
                &data_part->index_granularity_info,
                profile_callback,
                clock_type,
                data_part->getFileOffsetOrZero(stream_name + DATA_FILE_EXTENSION),
                data_part->getFileSizeOrZero(stream_name + DATA_FILE_EXTENSION),
                data_part->getFileOffsetOrZero(data_part->index_granularity_info.getMarksFilePath(stream_name)),
                data_part->getFileSizeOrZero(data_part->index_granularity_info.getMarksFilePath(stream_name))));
    };

    auto serialization = data_part->getSerializationForColumn(name_and_type);
    serialization->enumerateStreams(callback);
    serializations.emplace(name_and_type.name, std::move(serialization));
}

}
