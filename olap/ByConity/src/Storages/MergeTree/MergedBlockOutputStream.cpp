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

#include <Interpreters/Context.h>
#include <Parsers/queryToString.h>
#include <Storages/MergeTree/MergeTreeDataPartWriterWide.h>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Storages/MergeTree/MergedBlockOutputStream.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int LOGICAL_ERROR;
}

MergedBlockOutputStream::MergedBlockOutputStream(
    const MergeTreeDataPartPtr & data_part,
    const StorageMetadataPtr & metadata_snapshot_,
    const NamesAndTypesList & columns_list_,
    const MergeTreeIndices & skip_indices,
    CompressionCodecPtr default_codec_,
    bool blocks_are_granules_size,
    bool optimize_map_column_serialization)
    : IMergedBlockOutputStream(data_part, metadata_snapshot_)
    , columns_list(columns_list_)
    , default_codec(default_codec_)
{
    MergeTreeWriterSettings writer_settings(
        storage.getContext()->getSettings(),
        storage.getSettings(),
        data_part->index_granularity_info.is_adaptive,
        /* rewrite_primary_key = */ true,
        blocks_are_granules_size,
        optimize_map_column_serialization,
        /* enable_disk_based_key_index = */ metadata_snapshot->hasUniqueKey());

    if (!part_path.empty())
        volume->getDisk()->createDirectories(part_path);

    writer = data_part->getWriter(columns_list, metadata_snapshot, skip_indices, default_codec, writer_settings);

    if (metadata_snapshot->hasUniqueKey())
    {
        auto writer_wide = dynamic_cast<MergeTreeDataPartWriterWide *>(writer.get());
        if (!writer_wide)
            throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Unique table only supports wide format part right now.");
    }
}

/// If data is pre-sorted.
void MergedBlockOutputStream::write(const Block & block)
{
    writeImpl(block, nullptr);
}

void MergedBlockOutputStream::write(const Block & /*block*/, const WriteSettings & /*write_settings*/)
{

}


/** If the data is not sorted, but we pre-calculated the permutation, after which they will be sorted.
    * This method is used to save RAM, since you do not need to keep two blocks at once - the source and the sorted.
    */
void MergedBlockOutputStream::writeWithPermutation(const Block & block, const IColumn::Permutation * permutation)
{
    writeImpl(block, permutation);
}

void MergedBlockOutputStream::writeSuffix()
{
    throw Exception("Method writeSuffix is not supported by MergedBlockOutputStream", ErrorCodes::NOT_IMPLEMENTED);
}

void MergedBlockOutputStream::writeSuffixAndFinalizePart(
        MergeTreeData::MutableDataPartPtr & new_part,
        bool sync,
        const NamesAndTypesList * total_columns_list,
        MergeTreeData::DataPart::Checksums * additional_column_checksums)
{
    /// Finish write and get checksums.
    MergeTreeData::DataPart::ChecksumsPtr checksums_ptr = std::make_shared<MergeTreeData::DataPart::Checksums>();

    if (additional_column_checksums)
        *checksums_ptr = std::move(*additional_column_checksums);

    /// Finish columns serialization.
    writer->finish(*checksums_ptr, sync);

    for (const auto & [projection_name, projection_part] : new_part->getProjectionParts())
        checksums_ptr->addFile(
            projection_name + ".proj",
            projection_part->getChecksums()->getTotalSizeOnDisk(),
            projection_part->getChecksums()->getTotalChecksumUInt128());

    NamesAndTypesList part_columns;
    if (!total_columns_list)
        part_columns = columns_list;
    else
        part_columns = *total_columns_list;

    if (new_part->isStoredOnDisk())
        finalizePartOnDisk(new_part, part_columns, *checksums_ptr, sync);

    new_part->setColumns(part_columns);
    new_part->rows_count = rows_count;
    new_part->modification_time = time(nullptr);
    *(new_part->index) = writer->releaseIndexColumns();
    new_part->checksums_ptr = checksums_ptr;
    new_part->setBytesOnDisk(checksums_ptr->getTotalSizeOnDisk());
    new_part->index_granularity = writer->getIndexGranularity();
    new_part->calculateColumnsSizesOnDisk();
    if (default_codec != nullptr)
        new_part->default_codec = default_codec;
    new_part->storage.lockSharedData(*new_part);
}

void MergedBlockOutputStream::finalizePartOnDisk(
    const MergeTreeData::MutableDataPartPtr & new_part,
    NamesAndTypesList & part_columns,
    MergeTreeData::DataPart::Checksums & checksums,
    bool sync)
{

    if (new_part->isProjectionPart())
    {
        if (storage.format_version >= MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING || isCompactPart(new_part))
        {
            auto count_out = volume->getDisk()->writeFile(part_path + "count.txt", {.buffer_size = 4096});
            HashingWriteBuffer count_out_hashing(*count_out);
            writeIntText(rows_count, count_out_hashing);
            count_out_hashing.next();
            checksums.files["count.txt"].file_size = count_out_hashing.count();
            checksums.files["count.txt"].file_hash = count_out_hashing.getHash();
        }
    }
    else
    {
        if (new_part->uuid != UUIDHelpers::Nil)
        {
            auto out = volume->getDisk()->writeFile(fs::path(part_path) / IMergeTreeDataPart::UUID_FILE_NAME, {.buffer_size = 4096});
            HashingWriteBuffer out_hashing(*out);
            writeUUIDText(new_part->uuid, out_hashing);
            checksums.files[IMergeTreeDataPart::UUID_FILE_NAME].file_size = out_hashing.count();
            checksums.files[IMergeTreeDataPart::UUID_FILE_NAME].file_hash = out_hashing.getHash();
            out->finalize();
            if (sync)
                out->sync();
        }

        if (storage.format_version >= MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING || isCompactPart(new_part))
        {
            new_part->partition.store(storage, volume->getDisk(), part_path, checksums);
            if (new_part->minmax_idx.initialized)
                new_part->minmax_idx.store(storage, volume->getDisk(), part_path, checksums);
            else if (rows_count)
                throw Exception("MinMax index was not initialized for new non-empty part " + new_part->name
                        + ". It is a bug.", ErrorCodes::LOGICAL_ERROR);

            auto count_out = volume->getDisk()->writeFile(fs::path(part_path) / "count.txt", {.buffer_size = 4096});
            HashingWriteBuffer count_out_hashing(*count_out);
            writeIntText(rows_count, count_out_hashing);
            count_out_hashing.next();
            checksums.files["count.txt"].file_size = count_out_hashing.count();
            checksums.files["count.txt"].file_hash = count_out_hashing.getHash();
            count_out->finalize();
            if (sync)
                count_out->sync();
        }
    }

    {
        /// Write a file with versions
        auto out = volume->getDisk()->writeFile(fs::path(part_path) / "versions.txt", {.buffer_size = 4096});
        HashingWriteBuffer out_hashing(*out);
        new_part->versions->write(out_hashing);
        checksums.files["versions.txt"].file_size = out_hashing.count();
        checksums.files["versions.txt"].file_hash = out_hashing.getHash();
        out->finalize();
        if (sync)
            out->sync();
    }

    if (!new_part->ttl_infos.empty())
    {
        /// Write a file with ttl infos in json format.
        auto out = volume->getDisk()->writeFile(fs::path(part_path) / "ttl.txt", {.buffer_size = 4096});
        HashingWriteBuffer out_hashing(*out);
        new_part->ttl_infos.write(out_hashing);
        checksums.files["ttl.txt"].file_size = out_hashing.count();
        checksums.files["ttl.txt"].file_hash = out_hashing.getHash();
        out->finalize();
        if (sync)
            out->sync();
    }

    removeEmptyColumnsFromPart(new_part, part_columns, checksums);

    {
        /// Write a file with a description of columns.
        auto out = volume->getDisk()->writeFile(fs::path(part_path) / "columns.txt", {.buffer_size = 4096});
        part_columns.writeText(*out);
        out->finalize();
        if (sync)
            out->sync();
    }

    if (default_codec != nullptr)
    {
        auto out = volume->getDisk()->writeFile(part_path + IMergeTreeDataPart::DEFAULT_COMPRESSION_CODEC_FILE_NAME, {.buffer_size = 4096});
        DB::writeText(queryToString(default_codec->getFullCodecDesc()), *out);
        out->finalize();
    }
    else
    {
        throw Exception("Compression codec have to be specified for part on disk, empty for" + new_part->name
                + ". It is a bug.", ErrorCodes::LOGICAL_ERROR);
    }

    {
        /// Write file with checksums.
        auto out = volume->getDisk()->writeFile(fs::path(part_path) / "checksums.txt", {.buffer_size = 4096});
        checksums.versions = new_part->versions;
        checksums.write(*out);
        out->finalize();
        if (sync)
            out->sync();
    }
}

void MergedBlockOutputStream::writeImpl(const Block & block, const IColumn::Permutation * permutation)
{
    block.checkNumberOfRows();
    size_t rows = block.rows();
    if (!rows)
        return;

    writer->write(block, permutation);
    rows_count += rows;
}

void  MergedBlockOutputStream::updateWriterStream(const NameAndTypePair &pair)
{
    writer->updateWriterStream(pair);
}

}
