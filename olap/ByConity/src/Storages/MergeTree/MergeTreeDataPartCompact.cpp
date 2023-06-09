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

#include "MergeTreeDataPartCompact.h"
#include <DataTypes/NestedUtils.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/MapHelpers.h>
#include <Storages/MergeTree/MergeTreeReaderCompact.h>
#include <Storages/MergeTree/MergeTreeDataPartWriterCompact.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int CANNOT_READ_ALL_DATA;
    extern const int NOT_IMPLEMENTED;
    extern const int NO_FILE_IN_DATA_PART;
    extern const int BAD_SIZE_OF_FILE_IN_DATA_PART;
}


MergeTreeDataPartCompact::MergeTreeDataPartCompact(
       MergeTreeMetaBase & storage_,
        const String & name_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_)
    : IMergeTreeDataPart(storage_, name_, volume_, relative_path_, Type::COMPACT, parent_part_, location_)
{
}

MergeTreeDataPartCompact::MergeTreeDataPartCompact(
        const MergeTreeMetaBase & storage_,
        const String & name_,
        const MergeTreePartInfo & info_,
        const VolumePtr & volume_,
        const std::optional<String> & relative_path_,
        const IMergeTreeDataPart * parent_part_,
        IStorage::StorageLocation location_)
    : IMergeTreeDataPart(storage_, name_, info_, volume_, relative_path_, Type::COMPACT, parent_part_, location_)
{
}

IMergeTreeDataPart::MergeTreeReaderPtr MergeTreeDataPartCompact::getReader(
    const NamesAndTypesList & columns_to_read,
    const StorageMetadataPtr & metadata_snapshot,
    const MarkRanges & mark_ranges,
    UncompressedCache * uncompressed_cache,
    MarkCache * mark_cache,
    const MergeTreeReaderSettings & reader_settings,
    const ValueSizeMap & avg_value_size_hints,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback) const
{
    auto ptr = std::static_pointer_cast<const MergeTreeDataPartCompact>(shared_from_this());
    return std::make_unique<MergeTreeReaderCompact>(
        ptr, columns_to_read, metadata_snapshot, uncompressed_cache,
        mark_cache, mark_ranges, reader_settings,
        avg_value_size_hints, profile_callback);
}

IMergeTreeDataPart::MergeTreeWriterPtr MergeTreeDataPartCompact::getWriter(
    const NamesAndTypesList & columns_list,
    const StorageMetadataPtr & metadata_snapshot,
    const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
    const CompressionCodecPtr & default_codec_,
    const MergeTreeWriterSettings & writer_settings,
    const MergeTreeIndexGranularity & computed_index_granularity) const
{
    /// Handle implicit col when merging. Because it will use Vertical algorithm when there has map column and all map columns will in gathering column, each implicit map column will be handled one by one.
    if (columns_list.size() == 1 && isMapImplicitKeyNotKV(columns_list.front().name))
        return std::make_unique<MergeTreeDataPartWriterCompact>(
            shared_from_this(),
            columns_list,
            metadata_snapshot,
            indices_to_recalc,
            index_granularity_info.marks_file_extension,
            default_codec_,
            writer_settings,
            computed_index_granularity);
    else
    {
        NamesAndTypesList ordered_columns_list;
        std::copy_if(columns_list.begin(), columns_list.end(), std::back_inserter(ordered_columns_list), [this](const auto & column) {
            return getColumnPosition(column.name) != std::nullopt;
        });

        /// Order of writing is important in compact format
        ordered_columns_list.sort(
            [this](const auto & lhs, const auto & rhs) { return *getColumnPosition(lhs.name) < *getColumnPosition(rhs.name); });

        return std::make_unique<MergeTreeDataPartWriterCompact>(
            shared_from_this(),
            ordered_columns_list,
            metadata_snapshot,
            indices_to_recalc,
            index_granularity_info.marks_file_extension,
            default_codec_,
            writer_settings,
            computed_index_granularity);
    }
}

void MergeTreeDataPartCompact::calculateEachColumnSizes(ColumnSizeByName & each_columns_size, ColumnSize & total_size) const
{
    auto checksums = getChecksums();
    auto bin_checksum = checksums->files.find(DATA_FILE_NAME_WITH_EXTENSION);
    if (bin_checksum != checksums->files.end())
    {
        total_size.data_compressed += bin_checksum->second.file_size;
        total_size.data_uncompressed += bin_checksum->second.uncompressed_size;
    }

    auto mrk_checksum = checksums->files.find(DATA_FILE_NAME + index_granularity_info.marks_file_extension);
    if (mrk_checksum != checksums->files.end())
        total_size.marks += mrk_checksum->second.file_size;

    // Special handling flattened map type
    for (const NameAndTypePair & column : *columns_ptr)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
        {
            ColumnSize size = getMapColumnSizeNotKV(checksums, column);
            each_columns_size[column.name] = size;
            total_size.add(size);
        }
    }
}

void MergeTreeDataPartCompact::loadIndexGranularity()
{
    String full_path = getFullRelativePath();

    if (columns_ptr->empty())
        throw Exception("No columns in part " + name, ErrorCodes::NO_FILE_IN_DATA_PART);

    if (!index_granularity_info.is_adaptive)
        throw Exception("MergeTreeDataPartCompact cannot be created with non-adaptive granularity.", ErrorCodes::NOT_IMPLEMENTED);

    auto marks_file_path = index_granularity_info.getMarksFilePath(full_path + "data");
    if (!volume->getDisk()->exists(marks_file_path))
        throw Exception("Marks file '" + fullPath(volume->getDisk(), marks_file_path) + "' doesn't exist", ErrorCodes::NO_FILE_IN_DATA_PART);

    size_t marks_file_size = volume->getDisk()->getFileSize(marks_file_path);

    auto buffer = volume->getDisk()->readFile(marks_file_path, {.buffer_size = marks_file_size});
    while (!buffer->eof())
    {
        /// Skip offsets for columns
        buffer->seek(columns_without_bytemap_col_size * sizeof(MarkInCompressedFile), SEEK_CUR);
        size_t granularity;
        readIntBinary(granularity, *buffer);
        index_granularity.appendMark(granularity);
    }

    if (index_granularity.getMarksCount() * index_granularity_info.getMarkSizeInBytes(columns_without_bytemap_col_size) != marks_file_size)
        throw Exception("Cannot read all marks from file " + marks_file_path, ErrorCodes::CANNOT_READ_ALL_DATA);

    index_granularity.setInitialized();
}

void MergeTreeDataPartCompact::loadIndexGranularity(const size_t /*marks_count*/, const std::vector<size_t> & index_granularities)
{
    if (index_granularities.empty())
        throw Exception("MergeTreeDataPartCompact cannot be created with non-adaptive granularity.", ErrorCodes::NOT_IMPLEMENTED);

    for (const auto & granularity : index_granularities)
        index_granularity.appendMark(granularity);

    index_granularity.setInitialized();
}

bool MergeTreeDataPartCompact::hasColumnFiles(const NameAndTypePair & column) const
{
    if (!getColumnPosition(column.name))
        return false;

    /// Handle the special case that there has only one compact map column because it will has no data file if just insert {}
    if (hasOnlyOneCompactedMapColumnNotKV())
        return true;

    auto check_stream_exists = [&](const String & bin_file, const String & mrk_file)
    {
        auto checksums = getChecksums();
        auto bin_checksum = checksums->files.find(bin_file);
        auto mrk_checksum = checksums->files.find(mrk_file);

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
        return check_stream_exists(DATA_FILE_NAME_WITH_EXTENSION, DATA_FILE_NAME + index_granularity_info.marks_file_extension);
}

void MergeTreeDataPartCompact::setColumnsPtr(const NamesAndTypesListPtr & new_columns_ptr)
{
    IMergeTreeDataPart::setColumnsPtr(new_columns_ptr);
    columns_without_bytemap_col_size = 0;
    column_name_to_position_without_map.clear();
    column_name_to_position_without_map.reserve(new_columns_ptr->size());
    size_t pos_without_map = 0;
    for (const auto & column : *new_columns_ptr)
    {
        if (!column.type->isMap() || column.type->isMapKVStore())
        {
            columns_without_bytemap_col_size++;
            column_name_to_position_without_map.emplace(column.name, pos_without_map);
            for (const auto & subcolumn : column.type->getSubcolumnNames())
                column_name_to_position_without_map.emplace(Nested::concatenateName(column.name, subcolumn), pos_without_map);
            ++pos_without_map;
        }
    }
}

std::optional<size_t> MergeTreeDataPartCompact::getColumnPositionWithoutMap(const String & column_name) const
{
    auto it = column_name_to_position_without_map.find(column_name);
    if (it == column_name_to_position_without_map.end())
        return {};
    return it->second;
}

void MergeTreeDataPartCompact::checkConsistency(bool require_part_metadata) const
{
    checkConsistencyBase();
    String path = getFullRelativePath();
    String mrk_file_name = DATA_FILE_NAME + index_granularity_info.marks_file_extension;
    auto checksums = getChecksums();

    if (!checksums->empty())
    {
        /// count.txt should be present even in non custom-partitioned parts
        if (!checksums->files.count("count.txt"))
            throw Exception("No checksum for count.txt", ErrorCodes::NO_FILE_IN_DATA_PART);

        if (require_part_metadata)
        {
            if (!checksums->files.count(mrk_file_name))
                throw Exception("No marks file checksum for column in part " + fullPath(volume->getDisk(), path), ErrorCodes::NO_FILE_IN_DATA_PART);
            if (!checksums->files.count(DATA_FILE_NAME_WITH_EXTENSION))
                throw Exception("No data file checksum for in part " + fullPath(volume->getDisk(), path), ErrorCodes::NO_FILE_IN_DATA_PART);
        }
    }
    else
    {
        {
            /// count.txt should be present even in non custom-partitioned parts
            auto file_path = path + "count.txt";
            if (!volume->getDisk()->exists(file_path) || volume->getDisk()->getFileSize(file_path) == 0)
                throw Exception("Part " + path + " is broken: " + fullPath(volume->getDisk(), file_path) + " is empty", ErrorCodes::BAD_SIZE_OF_FILE_IN_DATA_PART);
        }

        /// Check that marks are nonempty and have the consistent size with columns number.
        auto mrk_file_path = path + mrk_file_name;

        if (volume->getDisk()->exists(mrk_file_name))
        {
            UInt64 file_size = volume->getDisk()->getFileSize(mrk_file_name);
             if (!file_size)
                throw Exception("Part " + path + " is broken: " + fullPath(volume->getDisk(), mrk_file_name) + " is empty.",
                    ErrorCodes::BAD_SIZE_OF_FILE_IN_DATA_PART);

            UInt64 expected_file_size = index_granularity_info.getMarkSizeInBytes(columns_ptr->size()) * index_granularity.getMarksCount();
            if (expected_file_size != file_size)
                throw Exception(
                    "Part " + path + " is broken: bad size of marks file '" + fullPath(volume->getDisk(), mrk_file_name) + "': " + std::to_string(file_size) + ", must be: " + std::to_string(expected_file_size),
                    ErrorCodes::BAD_SIZE_OF_FILE_IN_DATA_PART);
        }
    }
}

MergeTreeDataPartCompact::~MergeTreeDataPartCompact()
{
    removeIfNeeded();
}

}
