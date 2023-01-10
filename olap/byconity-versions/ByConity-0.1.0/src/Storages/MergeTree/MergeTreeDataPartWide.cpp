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

#include "MergeTreeDataPartWide.h"
#include <Storages/MergeTree/MergeTreeReaderWide.h>
#include <Storages/MergeTree/MergeTreeDataPartWriterWide.h>
#include <Storages/MergeTree/IMergeTreeDataPartWriter.h>
#include <DataTypes/NestedUtils.h>
#include <DataTypes/MapHelpers.h>
#include <Core/NamesAndTypes.h>
#include <Common/StringUtils/StringUtils.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_ALL_DATA;
    extern const int NO_FILE_IN_DATA_PART;
    extern const int BAD_SIZE_OF_FILE_IN_DATA_PART;
    extern const int LOGICAL_ERROR;
}


MergeTreeDataPartWide::MergeTreeDataPartWide(
       MergeTreeMetaBase & storage_,
        const String & name_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_)
    : IMergeTreeDataPart(storage_, name_, volume_, relative_path_, Type::WIDE, parent_part_, location_)
{
}

MergeTreeDataPartWide::MergeTreeDataPartWide(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_)
    : IMergeTreeDataPart(storage_, name_, info_, volume_, relative_path_, Type::WIDE, parent_part_, location_)
{
}

IMergeTreeDataPart::MergeTreeReaderPtr MergeTreeDataPartWide::getReader(
    const NamesAndTypesList & columns_to_read,
    const StorageMetadataPtr & metadata_snapshot,
    const MarkRanges & mark_ranges,
    UncompressedCache * uncompressed_cache,
    MarkCache * mark_cache,
    const MergeTreeReaderSettings & reader_settings,
    const ValueSizeMap & avg_value_size_hints,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback) const
{
    auto new_settings = reader_settings;
    new_settings.convert_nested_to_subcolumns = true;

    auto ptr = std::static_pointer_cast<const MergeTreeDataPartWide>(shared_from_this());
    return std::make_unique<MergeTreeReaderWide>(
        ptr, columns_to_read, metadata_snapshot, uncompressed_cache,
        mark_cache, mark_ranges, new_settings,
        avg_value_size_hints, profile_callback);
}

IMergeTreeDataPart::MergeTreeWriterPtr MergeTreeDataPartWide::getWriter(
    const NamesAndTypesList & columns_list,
    const StorageMetadataPtr & metadata_snapshot,
    const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
    const CompressionCodecPtr & default_codec_,
    const MergeTreeWriterSettings & writer_settings,
    const MergeTreeIndexGranularity & computed_index_granularity) const
{
    return std::make_unique<MergeTreeDataPartWriterWide>(
        shared_from_this(), columns_list, metadata_snapshot, indices_to_recalc,
        index_granularity_info.marks_file_extension,
        default_codec_, writer_settings, computed_index_granularity);
}


/// Takes into account the fact that several columns can e.g. share their .size substreams.
/// When calculating totals these should be counted only once.
ColumnSize MergeTreeDataPartWide::getColumnSizeImpl(
    const NameAndTypePair & column, std::unordered_set<String> * processed_substreams) const
{
    ColumnSize size;
    auto checksums = getChecksums();
    if (checksums->empty())
        return size;

    // Special handling flattened map type
    if (column.type->isMap() && !column.type->isMapKVStore())
        return getMapColumnSizeNotKV(checksums, column);

    auto serialization = getSerializationForColumn(column);
    serialization->enumerateStreams([&](const ISerialization::SubstreamPath & substream_path)
    {
        String file_name = ISerialization::getFileNameForStream(column, substream_path);

        if (processed_substreams && !processed_substreams->insert(file_name).second)
            return;

        auto bin_checksum = checksums->files.find(file_name + DATA_FILE_EXTENSION);
        if (bin_checksum != checksums->files.end())
        {
            size.data_compressed += bin_checksum->second.file_size;
            size.data_uncompressed += bin_checksum->second.uncompressed_size;
        }

        auto mrk_checksum = checksums->files.find(file_name + index_granularity_info.marks_file_extension);
        if (mrk_checksum != checksums->files.end())
            size.marks += mrk_checksum->second.file_size;
    }, {});

    return size;
}

void MergeTreeDataPartWide::loadIndexGranularity()
{
    if (index_granularity.isInitialized())
        return;

    String full_path = getFullRelativePath();
    index_granularity_info.changeGranularityIfRequired(volume->getDisk(), full_path);

    if (columns_ptr->empty())
        throw Exception("No columns in part " + name, ErrorCodes::NO_FILE_IN_DATA_PART);

    /// We can use any column except for ByteMap column whose data file may not exist.
    std::string marks_file_path;
    for (auto & column: *columns_ptr)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
            continue;
        marks_file_path = index_granularity_info.getMarksFilePath(full_path + getFileNameForColumn(column));
        break;
    }
    if (!volume->getDisk()->exists(marks_file_path))
        throw Exception("Marks file '" + fullPath(volume->getDisk(), marks_file_path) + "' doesn't exist", ErrorCodes::NO_FILE_IN_DATA_PART);

    size_t marks_file_size = volume->getDisk()->getFileSize(marks_file_path);

    if (!index_granularity_info.is_adaptive)
    {
        size_t marks_count = marks_file_size / index_granularity_info.getMarkSizeInBytes();
        index_granularity.resizeWithFixedGranularity(marks_count, index_granularity_info.fixed_index_granularity); /// all the same
    }
    else
    {
        auto buffer = volume->getDisk()->readFile(marks_file_path, {.buffer_size = marks_file_size});
        while (!buffer->eof())
        {
            buffer->seek(sizeof(size_t) * 2, SEEK_CUR); /// skip offset_in_compressed file and offset_in_decompressed_block
            size_t granularity;
            readIntBinary(granularity, *buffer);
            index_granularity.appendMark(granularity);
        }

        if (index_granularity.getMarksCount() * index_granularity_info.getMarkSizeInBytes() != marks_file_size)
            throw Exception("Cannot read all marks from file " + fullPath(volume->getDisk(), marks_file_path), ErrorCodes::CANNOT_READ_ALL_DATA);
    }

    index_granularity.setInitialized();
}

void MergeTreeDataPartWide::loadIndexGranularity(size_t marks_count, const std::vector<size_t> & index_granularities)
{
    /// The empty marks means using fixed index granularity
    if (index_granularities.empty())
        index_granularity_info.setNonAdaptive();

    if (!index_granularity_info.is_adaptive)
    {
        index_granularity.resizeWithFixedGranularity(marks_count, index_granularity_info.fixed_index_granularity);
    }
    else
    {
        for (const auto & granularity : index_granularities)
            index_granularity.appendMark(granularity);
    }
    index_granularity.setInitialized();
}

MergeTreeDataPartWide::~MergeTreeDataPartWide()
{
    removeIfNeeded();
}

void MergeTreeDataPartWide::checkConsistency(bool require_part_metadata) const
{
    checkConsistencyBase();
    String path = getFullRelativePath();
    auto checksums = getChecksums();
    if (!checksums->empty())
    {
        if (require_part_metadata)
        {
            for (const NameAndTypePair & name_type : *columns_ptr)
            {
                //@ByteMap
                if (name_type.type->isMap() && !name_type.type->isMapKVStore())
                    continue;

                auto serialization = getSerializationForColumn(name_type);
                serialization->enumerateStreams([&](const ISerialization::SubstreamPath & substream_path)
                {
                    String file_name = ISerialization::getFileNameForStream(name_type, substream_path);
                    String mrk_file_name = file_name + index_granularity_info.marks_file_extension;
                    String bin_file_name = file_name + DATA_FILE_EXTENSION;
                    if (!checksums->files.count(mrk_file_name))
                        throw Exception("No " + mrk_file_name + " file checksum for column " + name_type.name + " in part " + fullPath(volume->getDisk(), path),
                            ErrorCodes::NO_FILE_IN_DATA_PART);
                    if (!checksums->files.count(bin_file_name))
                        throw Exception("No " + bin_file_name + " file checksum for column " + name_type.name + " in part " + fullPath(volume->getDisk(), path),
                            ErrorCodes::NO_FILE_IN_DATA_PART);
                });
            }
        }

    }
    else
    {
        /// Check that all marks are nonempty and have the same size.
        std::optional<UInt64> marks_size;
        for (const NameAndTypePair & name_type : *columns_ptr)
        {
            //@ByteMap
            if (name_type.type->isMap() && !name_type.type->isMapKVStore())
                continue;

            auto serialization = IDataType::getSerialization(name_type,
                [&](const String & stream_name)
                {
                    return volume->getDisk()->exists(stream_name + DATA_FILE_EXTENSION);
                });

            serialization->enumerateStreams([&](const ISerialization::SubstreamPath & substream_path)
            {
                auto file_path = path + ISerialization::getFileNameForStream(name_type, substream_path) + index_granularity_info.marks_file_extension;

                /// Missing file is Ok for case when new column was added.
                if (volume->getDisk()->exists(file_path))
                {
                    UInt64 file_size = volume->getDisk()->getFileSize(file_path);

                    if (!file_size)
                        throw Exception("Part " + path + " is broken: " + fullPath(volume->getDisk(), file_path) + " is empty.",
                            ErrorCodes::BAD_SIZE_OF_FILE_IN_DATA_PART);

                    if (!marks_size)
                        marks_size = file_size;
                    else if (file_size != *marks_size)
                        throw Exception("Part " + path + " is broken: marks have different sizes.",
                            ErrorCodes::BAD_SIZE_OF_FILE_IN_DATA_PART);
                }
            });
        }
    }
}

bool MergeTreeDataPartWide::hasColumnFiles(const NameAndTypePair & column) const
{
    /// Handle the special case that there has only one compact map column because it will has no data file if just insert {}
    if (hasOnlyOneCompactedMapColumnNotKV())
        return true;
    auto check_stream_exists = [this](const String & stream_name)
    {
        auto checksums = getChecksums();
        auto bin_checksum = checksums->files.find(stream_name + DATA_FILE_EXTENSION);
        auto mrk_checksum = checksums->files.find(stream_name + index_granularity_info.marks_file_extension);

        return bin_checksum != checksums->files.end() && mrk_checksum != checksums->files.end();
    };

    if (column.type->isMap() && !column.type->isMapKVStore())
    {
        for (auto & [file, _] : getChecksums()->files)
        {
            if (versions->enable_compact_map_data)
            {
                if (isMapCompactFileNameOfSpecialMapName(file, column.name))
                    return true;
            }
            else
            {
                if (isMapImplicitFileNameOfSpecialMapName(file, column.name))
                    return true;
            }
        }
        return false;
    }
    else
    {
        bool res = true;
        auto serialization = IDataType::getSerialization(column, check_stream_exists);
        serialization->enumerateStreams([&](const ISerialization::SubstreamPath & substream_path)
        {
            String file_name = ISerialization::getFileNameForStream(column, substream_path);
            if (!check_stream_exists(file_name))
                res = false;
        });

        return res;
    }
}

String MergeTreeDataPartWide::getFileNameForColumn(const NameAndTypePair & column) const
{
    String filename;
    auto serialization = column.type->getDefaultSerialization();
    serialization->enumerateStreams([&](const ISerialization::SubstreamPath & substream_path)
    {
        if (filename.empty())
            filename = ISerialization::getFileNameForStream(column, substream_path);
    });
    return filename;
}

void MergeTreeDataPartWide::calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const
{
    std::unordered_set<String> processed_substreams;
    for (const NameAndTypePair & column : *columns_ptr)
    {
        ColumnSize size = getColumnSizeImpl(column, &processed_substreams);
        each_columns_size[column.name] = size;
        total_size.add(size);

#ifndef NDEBUG
        /// Most trivial types
        if (rows_count != 0 && column.type->isValueRepresentedByNumber() && !column.type->haveSubtypes())
        {
            size_t rows_in_column = size.data_uncompressed / column.type->getSizeOfValueInMemory();
            if (rows_in_column != rows_count)
            {
                throw Exception(
                    ErrorCodes::LOGICAL_ERROR,
                    "Column {} has rows count {} according to size in memory "
                    "and size of single value, but data part {} has {} rows", backQuote(column.name), rows_in_column, name, rows_count);
            }
        }
#endif
    }
}

}
