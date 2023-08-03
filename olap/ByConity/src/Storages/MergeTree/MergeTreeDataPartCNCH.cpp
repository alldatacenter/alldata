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

#include "MergeTreeDataPartCNCH.h"

#include <IO/LimitReadBuffer.h>
#include <Storages/DiskCache/DiskCacheFactory.h>
#include <Storages/DiskCache/MetaFileDiskCacheSegment.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/MergeTree/DeleteBitmapCache.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/MergeTreeDataPartWriterWide.h>
#include <Storages/MergeTree/MergeTreeReaderCNCH.h>
#include <Storages/UUIDAndPartName.h>
#include <Storages/UniqueKeyIndexCache.h>
#include <Common/Exception.h>
#include "DataTypes/DataTypeByteMap.h"
#include "Storages/DiskCache/DiskCacheSegment.h"

namespace ProfileEvents
{
}

namespace DB
{
namespace ErrorCodes
{
    extern const int NO_FILE_IN_DATA_PART;
    extern const int LOGICAL_ERROR;
    extern const int NOT_IMPLEMENTED;
    extern const int DISK_CACHE_NOT_USED;
}

static constexpr auto DATA_FILE = "data";

static std::unique_ptr<ReadBufferFromFileBase> openForReading(const DiskPtr & disk, const String & path, size_t file_size)
{
    return disk->readFile(path, {.buffer_size = std::min(file_size, static_cast<size_t>(DBMS_DEFAULT_BUFFER_SIZE))});
}

static LimitReadBuffer readPartFile(ReadBufferFromFileBase & in, off_t file_offset, size_t file_size)
{
    if (file_size == 0)
        throw Exception(ErrorCodes::NO_FILE_IN_DATA_PART, "The size of file is zero");

    in.seek(file_offset);
    return LimitReadBuffer(in, file_size, false);
}

static std::pair<off_t, size_t> getFileOffsetAndSize(const IMergeTreeDataPart & data_part, const String & file_name)
{
    auto checksums = data_part.getChecksums();
    if (auto it = checksums->files.find(file_name); it != checksums->files.end())
    {
        return {it->second.file_offset, it->second.file_size};
    }
    else
    {
        throw Exception(fmt::format("Cannot find file {} in part {}", file_name, data_part.name), ErrorCodes::NO_FILE_IN_DATA_PART);
    }
}

MergeTreeDataPartCNCH::MergeTreeDataPartCNCH(
    const MergeTreeMetaBase & storage_,
    const String & name_,
    const VolumePtr & volume_,
    const std::optional<String> & relative_path_,
    const IMergeTreeDataPart * parent_part_,
    const UUID& part_id_)
    : IMergeTreeDataPart(storage_, name_, volume_, relative_path_, Type::CNCH, parent_part_, IStorage::StorageLocation::MAIN, part_id_)
{
}

MergeTreeDataPartCNCH::MergeTreeDataPartCNCH(
    const MergeTreeMetaBase & storage_,
    const String & name_,
    const MergeTreePartInfo & info_,
    const VolumePtr & volume_,
    const std::optional<String> & relative_path_,
    const IMergeTreeDataPart * parent_part_,
    const UUID& part_id_)
    : IMergeTreeDataPart(storage_, name_, info_, volume_, relative_path_, Type::CNCH, parent_part_, IStorage::StorageLocation::MAIN, part_id_)
{
}

IMergeTreeDataPart::MergeTreeReaderPtr MergeTreeDataPartCNCH::getReader(
    const NamesAndTypesList & columns_to_read,
    const StorageMetadataPtr & metadata_snapshot,
    const MarkRanges & mark_ranges,
    UncompressedCache * uncompressed_cache,
    MarkCache * mark_cache,
    const MergeTreeReaderSettings & reader_settings_,
    const ValueSizeMap & avg_value_size_hints,
    const ReadBufferFromFileBase::ProfileCallback & profile_callback) const
{
    auto new_settings = reader_settings_;
    new_settings.convert_nested_to_subcolumns = true;

    auto ptr = std::static_pointer_cast<const MergeTreeDataPartCNCH>(shared_from_this());
    return std::make_unique<MergeTreeReaderCNCH>(
        ptr, columns_to_read, metadata_snapshot, uncompressed_cache,
        mark_cache, mark_ranges, new_settings,
        avg_value_size_hints, profile_callback);
}

IMergeTreeDataPart::MergeTreeWriterPtr MergeTreeDataPartCNCH::getWriter(
    [[maybe_unused]] const NamesAndTypesList & columns_list,
    [[maybe_unused]] const StorageMetadataPtr & metadata_snapshot,
    [[maybe_unused]] const std::vector<MergeTreeIndexPtr> & indices_to_recalc,
    [[maybe_unused]] const CompressionCodecPtr & default_codec_,
    [[maybe_unused]] const MergeTreeWriterSettings & writer_settings,
    [[maybe_unused]] const MergeTreeIndexGranularity & computed_index_granularity) const
{
    return {};
}

bool MergeTreeDataPartCNCH::operator < (const MergeTreeDataPartCNCH & r) const
{
    if (!info.mutation || !r.info.mutation)
        return name < r.name;

    if (name < r.name)
        return true;
    else if (name == r.name)
        return info.mutation < r.info.mutation;
    else
        return false;
}

bool MergeTreeDataPartCNCH::operator > (const MergeTreeDataPartCNCH & r) const
{
    if (!info.mutation || !r.info.mutation)
        return name > r.name;

    if (name > r.name)
        return true;
    else if (name == r.name)
        return info.mutation > r.info.mutation;
    else
        return false;
}

void MergeTreeDataPartCNCH::fromLocalPart(const IMergeTreeDataPart & local_part)
{
    partition.assign(local_part.partition);
    if (local_part.checksums_ptr)
    {
        checksums_ptr = std::make_shared<MergeTreeDataPartChecksums>(*local_part.checksums_ptr);
        checksums_ptr->storage_type = StorageType::ByteHDFS;
    }
    else
    {
        /// anywhy we need a checksums
        checksums_ptr = std::make_shared<MergeTreeDataPartChecksums>();
        checksums_ptr->storage_type = StorageType::ByteHDFS;
    }
    minmax_idx = local_part.minmax_idx;
    rows_count = local_part.rows_count;
    loadIndexGranularity(local_part.getMarksCount(), local_part.index_granularity.getIndexGranularities());
    setColumns(local_part.getColumns());
    index = local_part.index;
    has_bitmap = local_part.has_bitmap.load();
    deleted = local_part.deleted;
    bucket_number = local_part.bucket_number;
    table_definition_hash = storage.getTableHashForClusterBy();
    columns_commit_time = local_part.columns_commit_time;
    mutation_commit_time = local_part.mutation_commit_time;
    min_unique_key = local_part.min_unique_key;
    max_unique_key = local_part.max_unique_key;
    /// TODO:
    // setAesEncrypter(local_part.getAesEncrypter());
    secondary_txn_id = local_part.secondary_txn_id;
    covered_parts_count = local_part.covered_parts_count;
    covered_parts_size = local_part.covered_parts_size;
    covered_parts_rows = local_part.covered_parts_rows;
    delete_bitmap = local_part.delete_bitmap;
}

String MergeTreeDataPartCNCH::getFileNameForColumn(const NameAndTypePair & column) const
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

bool MergeTreeDataPartCNCH::hasColumnFiles(const NameAndTypePair &) const
{
    return true;
};

void MergeTreeDataPartCNCH::loadIndexGranularity(size_t marks_count, [[maybe_unused]] const std::vector<size_t> & index_granularities)
{
    /// init once
    if (index_granularity.isInitialized())
        return;

    if (isPartial() && isEmpty())
    {
        auto base_part = getBasePart();
        if (!base_part->index_granularity.isInitialized())
            throw Exception(ErrorCodes::LOGICAL_ERROR, "Index granularity of base part must be ready before loading partial part index granularity");
        index_granularity = base_part->index_granularity;
        return;
    }

    if (index_granularity_info.is_adaptive)
    {
        // load from disk
        // usually we don't need to load index granularity from disk because
        // kv keeps this kind of information
        if (unlikely(index_granularities.empty()))
        {
            loadIndexGranularity();
            if (marks_count != index_granularity.getMarksCount())
                throw Exception(ErrorCodes::CANNOT_READ_ALL_DATA, "Expected marks count {}, loaded marks count {} from disk", marks_count, index_granularity.getMarksCount());
        }
        else
        {
            for (const auto & granularity : index_granularities)
                index_granularity.appendMark(granularity);
        }
    }
    else
    {
        index_granularity.resizeWithFixedGranularity(marks_count, index_granularity_info.fixed_index_granularity);
    }

    index_granularity.setInitialized();
};

void MergeTreeDataPartCNCH::loadColumnsChecksumsIndexes([[maybe_unused]] bool require_columns_checksums, [[maybe_unused]] bool check_consistency)
{
    /// only load necessary stuff here
    assertOnDisk();
    MemoryTracker::BlockerInThread temporarily_disable_memory_tracker(VariableContext::Global);
    getChecksums();
    loadIndexGranularity();
    calculateEachColumnSizes(columns_sizes, total_columns_size);

    /// FIXME:
    default_codec = CompressionCodecFactory::instance().getDefaultCodec();
}

void MergeTreeDataPartCNCH::loadFromFileSystem(bool load_hint_mutation)
{
    auto footer = loadPartDataFooter();
    const auto & meta_info_pos = footer["metainfo.txt"];

    String data_rel_path = fs::path(getFullRelativePath()) / DATA_FILE;
    DiskPtr disk = volume->getDisk();
    auto reader = openForReading(disk, data_rel_path, meta_info_pos.file_size);
    LimitReadBuffer limit_reader = readPartFile(*reader, meta_info_pos.file_offset, meta_info_pos.file_size);
    loadMetaInfoFromBuffer(limit_reader, load_hint_mutation);
}

UniqueKeyIndexPtr MergeTreeDataPartCNCH::getUniqueKeyIndex() const
{
    if (!storage.getInMemoryMetadataPtr()->hasUniqueKey())
        throw Exception(
            "getUniqueKeyIndex of " + storage.getStorageID().getNameForLogs() + " which doesn't have unique key",
            ErrorCodes::LOGICAL_ERROR);
    if (rows_count == 0)
        return std::make_shared<UniqueKeyIndex>(); /// return empty index for empty part

    if (storage.unique_key_index_cache)
    {
        UUIDAndPartName key(storage.getStorageUUID(), info.getBlockName());
        auto load_func = [this] { return const_cast<MergeTreeDataPartCNCH *>(this)->loadUniqueKeyIndex(); };
        return storage.unique_key_index_cache->getOrSet(std::move(key), std::move(load_func)).first;
    }
    else
        return const_cast<MergeTreeDataPartCNCH *>(this)->loadUniqueKeyIndex();
}

const ImmutableDeleteBitmapPtr & MergeTreeDataPartCNCH::getDeleteBitmap(bool is_unique_new_part) const
{
    if (!storage.getInMemoryMetadataPtr()->hasUniqueKey() || deleted)
    {
        if (delete_bitmap != nullptr)
            throw Exception("Delete bitmap for part " + name + " is not null", ErrorCodes::LOGICAL_ERROR);
        return delete_bitmap;
    }

    if (!delete_bitmap)
    {
        /// bitmap hasn't been set, load it from cache and metas
        if (delete_bitmap_metas.empty())
        {
            /// for new part of unique table, it's valid if its delete_bitmap_metas is empty
            if (is_unique_new_part)
                return delete_bitmap;
            throw Exception("No metadata for delete bitmap of part " + name, ErrorCodes::LOGICAL_ERROR);
        }
        Stopwatch watch;
        auto cache = storage.getContext()->getDeleteBitmapCache();
        String cache_key = DeleteBitmapCache::buildKey(storage.getStorageUUID(), info.partition_id, info.min_block, info.max_block);
        ImmutableDeleteBitmapPtr cached_bitmap;
        UInt64 cached_version = 0; /// 0 is an invalid value and acts as a sentinel
        bool hit_cache = cache->lookup(cache_key, cached_version, cached_bitmap);

        UInt64 target_version = delete_bitmap_metas.front()->commit_time();
        UInt64 txn_id = delete_bitmap_metas.front()->txn_id();
        if (hit_cache && cached_version == target_version)
        {
            /// common case: got the exact version of bitmap from cache
            const_cast<MergeTreeDataPartCNCH *>(this)->delete_bitmap = std::move(cached_bitmap);
        }
        else
        {
            DeleteBitmapPtr bitmap = std::make_shared<Roaring>();
            std::forward_list<DataModelDeleteBitmapPtr> to_reads; /// store meta in ascending order of commit time

            if (cached_version > target_version)
            {
                /// case: querying an older version than the cached version
                /// then cached bitmap can't be used and we need to build the bitmap from all metas
                to_reads = delete_bitmap_metas;
                to_reads.reverse();
            }
            else
            {
                /// case: querying a newer version than the cached version
                /// if all metas > cached version, build the bitmap from all metas.
                /// otherwise build the bitmap from the cached bitmap and newer metas (whose version > cached version)
                for (auto & meta : delete_bitmap_metas)
                {
                    if (meta->commit_time() > cached_version)
                    {
                        to_reads.insert_after(to_reads.before_begin(), meta);
                    }
                    else if (meta->commit_time() == cached_version)
                    {
                        *bitmap = *cached_bitmap; /// copy the cached bitmap as the base
                        break;
                    }
                    else
                    {
                        throw Exception("Part " + name + " doesn't contain delete bitmap meta at " + toString(cached_version), ErrorCodes::LOGICAL_ERROR);
                    }
                }
            }

            /// union to_reads into bitmap
            for (auto & meta : to_reads)
                deserializeDeleteBitmapInfo(storage, meta, bitmap);

            const_cast<MergeTreeDataPartCNCH *>(this)->delete_bitmap = std::move(bitmap);
            if (target_version > cached_version)
            {
                cache->insert(cache_key, target_version, delete_bitmap);
            }
            LOG_DEBUG(
                storage.log,
                "Loaded delete bitmap at commit_time {} of {} in {} ms, bitmap cardinality: {}, it was generated in txn_id: {}",
                target_version,
                name,
                watch.elapsedMilliseconds(),
                delete_bitmap->cardinality(),
                txn_id);
        }
    }
    assert(delete_bitmap != nullptr);
    return delete_bitmap;
}

MergeTreeDataPartChecksums::FileChecksums MergeTreeDataPartCNCH::loadPartDataFooter() const
{
    const String data_file_path = fs::path(getFullRelativePath()) / DATA_FILE;
    size_t data_file_size = volume->getDisk()->getFileSize(data_file_path);
    if (!volume->getDisk()->exists(data_file_path))
        throw Exception(ErrorCodes::NO_FILE_IN_DATA_PART, "No data file of part {} under path {}", name, data_file_path);

    auto data_file = openForReading(volume->getDisk(), data_file_path, MERGE_TREE_STORAGE_CNCH_DATA_FOOTER_SIZE);
    data_file->seek(data_file_size - MERGE_TREE_STORAGE_CNCH_DATA_FOOTER_SIZE);

    MergeTreeDataPartChecksums::FileChecksums file_checksums;
    auto add_file_checksum = [this, &buf = *data_file, &file_checksums] (const String & file_name) {
        Checksum file_checksum;
        file_checksum.mutation = info.mutation;
        readIntBinary(file_checksum.file_offset, buf);
        readIntBinary(file_checksum.file_size, buf);
        readIntBinary(file_checksum.file_hash, buf);
        file_checksums[file_name] = std::move(file_checksum);
    };

    add_file_checksum("primary.idx");
    add_file_checksum("checksums.txt");
    add_file_checksum("metainfo.txt");
    add_file_checksum("unique_key.idx");

    return file_checksums;
}

bool MergeTreeDataPartCNCH::isDeleted() const
{
    return deleted;
}

void MergeTreeDataPartCNCH::checkConsistency([[maybe_unused]] bool require_part_metadata) const
{
}

MergeTreeDataPartCNCH::IndexPtr MergeTreeDataPartCNCH::loadIndex()
{
    /// load index from base part if it's a partial part
    if (isPartial())
    {
        /// Partial parts may not have index; primary columns never get altered, so getting index from base parts
        auto base_part = getBasePart();
        index = base_part->getIndex();
        return index;
    }

    /// It can be empty in case of mutations
    if (!index_granularity.isInitialized())
        throw Exception("Index granularity is not loaded before index loading", ErrorCodes::LOGICAL_ERROR);

    auto metadata_snapshot = storage.getInMemoryMetadataPtr();
    const auto & primary_key = metadata_snapshot->getPrimaryKey();
    size_t key_size = primary_key.column_names.size();

    if (!key_size)
        return index;

    if (enableDiskCache())
    {
        auto disk_cache = DiskCacheFactory::instance().getDefault().first;
        PrimaryIndexDiskCacheSegment segment(shared_from_this());
        auto [cache_disk, segment_path] = disk_cache->get(segment.getSegmentName());
        if (cache_disk && cache_disk->exists(segment_path))
        {
            try
            {
                LOG_DEBUG(storage.log, "has index disk cache {}", segment_path);
                auto cache_buf = openForReading(cache_disk, segment_path, cache_disk->getFileSize(segment_path));
                index = loadIndexFromBuffer(*cache_buf, primary_key);
                return index;
            }
            catch (...)
            {
                tryLogCurrentException("Could not load index from disk cache");
            }
        }
        else if (disk_cache_mode == DiskCacheMode::FORCE_CHECKSUMS_DISK_CACHE)
        {
            throw Exception(ErrorCodes::DISK_CACHE_NOT_USED, "Index {} of part has no disk cache {} and 'FORCE_DISK_CACHE' is set", name, segment_path);
        }
    }

    auto checksums = getChecksums();
    auto [file_offset, file_size] = getFileOffsetAndSize(*this, "primary.idx");
    String data_rel_path = fs::path(getFullRelativePath()) / DATA_FILE;
    auto data_file = openForReading(volume->getDisk(), data_rel_path, file_size);
    LimitReadBuffer buf = readPartFile(*data_file, file_offset, file_size);
    index = loadIndexFromBuffer(buf, primary_key);

    if (enableDiskCache())
    {
        auto index_seg = std::make_shared<PrimaryIndexDiskCacheSegment>(shared_from_this());
        auto disk_cache = DiskCacheFactory::instance().getDefault().first;
        disk_cache->cacheSegmentsToLocalDisk({std::move(index_seg)});
    }

    return index;
}

IMergeTreeDataPart::ChecksumsPtr MergeTreeDataPartCNCH::loadChecksums([[maybe_unused]] bool require)
{
    ChecksumsPtr checksums = std::make_shared<Checksums>();
    checksums->storage_type = StorageType::ByteHDFS;
    if (deleted)
        return checksums;

    if (enableDiskCache())
    {
        ChecksumsDiskCacheSegment checksums_segment(shared_from_this());
        auto disk_cache = DiskCacheFactory::instance().getDefault().first;
        auto [cache_disk, segment_path] = disk_cache->get(checksums_segment.getSegmentName());
        if (cache_disk && cache_disk->exists(segment_path))
        {
            try
            {
                auto cache_buf = openForReading(cache_disk, segment_path, cache_disk->getFileSize(segment_path));
                if (checksums->read(*cache_buf))
                    assertEOF(*cache_buf);
                return checksums;
            }
            catch (...)
            {
                tryLogCurrentException("Could not load checksums from disk");
            }
        }
        else if (disk_cache_mode == DiskCacheMode::FORCE_CHECKSUMS_DISK_CACHE)
        {
            throw Exception(ErrorCodes::DISK_CACHE_NOT_USED, "Checksums {} of part has no disk cache {} and 'FORCE_DISK_CACHE' is set", name, segment_path);
        }
    }

    String data_rel_path = fs::path(getFullRelativePath()) / DATA_FILE;
    auto data_footer = loadPartDataFooter();
    const auto & checksum_file = data_footer["checksums.txt"];

    if (checksum_file.file_size == 0/* && isDeleted() */)
        throw Exception(ErrorCodes::NO_FILE_IN_DATA_PART, "The size of checksums in part {} under path {} is zero", name, data_rel_path);

    auto data_file = openForReading(volume->getDisk(), data_rel_path, checksum_file.file_size);
    LimitReadBuffer buf = readPartFile(*data_file, checksum_file.file_offset, checksum_file.file_size);

    if (checksums->read(buf))
    {
        assertEOF(buf);
        /// bytes_on_disk += delta_checksums->getTotalSizeOnDisk();
    }
    else
    {
        /// bytes_on_disk += delta_checksums->getTotalSizeOnDisk();
    }

    // merge with data footer
    data_footer.merge(checksums->files);
    checksums->files.swap(data_footer);

    // Update checksums base on current part's mutation, the mutation in hdfs's file
    // is not reliable, since when attach, part will have new mutation, but the mutation
    // and hint_mutation within part's checksums is untouched, so update it here
    for (auto & file : checksums->files)
    {
        file.second.mutation = info.mutation;
    }

    if (isPartial())
    {
        /// merge with previous checksums with current checksums
        const auto & prev_part = getPreviousPart();
        auto prev_checksums = prev_part->getChecksums();

        /// insert checksum files from previous part if it's not in current checksums
        for (const auto & [name, file] : prev_checksums->files)
        {
            checksums->files.emplace(name, file);
        }
    }

    // remove deleted files in checksums
    for (auto it = checksums->files.begin(); it != checksums->files.end();)
    {
        const auto & file = it->second;
        if (file.is_deleted)
            it = checksums->files.erase(it);
        else
            ++it;
    }

    if (storage.getSettings()->enable_persistent_checksum || is_temp || isProjectionPart())
    {
        std::lock_guard lock(checksums_mutex);
        checksums_ptr = checksums;
    }

    /// store in disk cache
    if (enableDiskCache())
    {
        auto segment = std::make_shared<ChecksumsDiskCacheSegment>(shared_from_this());
        auto disk_cache = DiskCacheFactory::instance().getDefault().first;
        disk_cache->cacheSegmentsToLocalDisk({std::move(segment)});
    }

    return checksums;
}

UniqueKeyIndexPtr MergeTreeDataPartCNCH::loadUniqueKeyIndex()
{
    return std::make_shared<UniqueKeyIndex>(
        getRemoteFileInfo(), storage.getContext()->getUniqueKeyIndexFileCache(), storage.getContext()->getUniqueKeyIndexBlockCache());
}

IndexFile::RemoteFileInfo MergeTreeDataPartCNCH::getRemoteFileInfo()
{
    /// Get base part who contains unique key index
    IMergeTreeDataPartPtr base_part = getBasePart();

    String data_path = base_part->getFullPath() + "/data";
    off_t offset = 0;
    size_t size = 0;
    getUniqueKeyIndexFilePosAndSize(base_part, offset, size);

    IndexFile::RemoteFileInfo file;
    file.hdfs_params = storage.getContext()->getHdfsConnectionParams();
    file.path = data_path;
    file.start_offset = offset;
    file.size = size;
    file.cache_key = toString(storage.getStorageUUID()) + "_" + info.getBlockName();
    return file;
}

void MergeTreeDataPartCNCH::getUniqueKeyIndexFilePosAndSize(const IMergeTreeDataPartPtr part, off_t & off, size_t & size)
{
    String data_rel_path = fs::path(part->getFullRelativePath()) / "data";
    String data_full_path = fs::path(part->getFullPath()) / "data";
    const auto & hdfs_params = storage.getContext()->getHdfsConnectionParams();
    ReadBufferFromByteHDFS data_file = ReadBufferFromByteHDFS(data_full_path, true, hdfs_params);
    size_t data_file_size = volume->getDisk()->getFileSize(data_rel_path);
    data_file.seek(data_file_size - MERGE_TREE_STORAGE_CNCH_DATA_FOOTER_SIZE + 3 * (2 * sizeof(size_t) + sizeof(CityHash_v1_0_2::uint128)));
    readIntBinary(off, data_file);
    readIntBinary(size, data_file);
}

void MergeTreeDataPartCNCH::loadIndexGranularity()
{
    if (index_granularity.isInitialized())
        return;


    auto checksums = getChecksums();
    index_granularity_info.changeGranularityIfRequired(*checksums);


    String full_path = getFullRelativePath();
    if (columns_ptr->empty())
        throw Exception("No columns in part " + name, ErrorCodes::NO_FILE_IN_DATA_PART);

    /// We can use any column except for ByteMap column whose data file may not exist.
    std::string marks_file_name;
    for (auto & column: *columns_ptr)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
            continue;
        marks_file_name = index_granularity_info.getMarksFilePath(getFileNameForColumn(column));
        break;
    }
    size_t marks_file_size = checksums->files.at(marks_file_name).file_size;

    if (!index_granularity_info.is_adaptive)
    {
        size_t marks_count = marks_file_size / index_granularity_info.getMarkSizeInBytes();
        index_granularity.resizeWithFixedGranularity(marks_count, storage.getSettings()->index_granularity);
    }
    else
    {
        /// TODO: use cache
        auto [file_off, file_size] = getFileOffsetAndSize(*this, marks_file_name);
        String data_path = fs::path(getFullRelativePath()) / DATA_FILE;
        auto reader = openForReading(volume->getDisk(), data_path, file_size);
        LimitReadBuffer buffer = readPartFile(*reader, file_off, file_size);
        while (!buffer.eof())
        {
            size_t discard = 0;
            readIntBinary(discard, buffer); /// skip offset_in_compressed file
            readIntBinary(discard, buffer); /// offset_in_decompressed_block
            size_t granularity = 0;
            readIntBinary(granularity, buffer);
            index_granularity.appendMark(granularity);
        }

        if (index_granularity.getMarksCount() * index_granularity_info.getMarkSizeInBytes() != marks_file_size)
            throw Exception("Cannot read all marks from file " + fullPath(volume->getDisk(), data_path), ErrorCodes::CANNOT_READ_ALL_DATA);
    }

    index_granularity.setInitialized();
}

void MergeTreeDataPartCNCH::loadMetaInfoFromBuffer(ReadBuffer & buf, bool load_hint_mutation)
{
    assertString("CHPT", buf);
    UInt8 version {0};
    readIntBinary(version, buf);

    UInt8 is_deleted;
    readIntBinary(is_deleted, buf);
    deleted = is_deleted;

    readVarUInt(bytes_on_disk, buf);
    readVarUInt(rows_count, buf);
    size_t marks_count = 0;
    readVarUInt(marks_count, buf);

    Int64 hint_mutation = 0;
    readVarUInt(hint_mutation, buf);
    if (load_hint_mutation)
    {
        info.hint_mutation = hint_mutation;
    }

    columns_ptr->readText(buf);
    deserializePartitionAndMinMaxIndex(buf);
    readIntBinary(bucket_number, buf);
    readIntBinary(table_definition_hash, buf);

    loadIndexGranularity(marks_count, {});
}

void MergeTreeDataPartCNCH::calculateEachColumnSizes(
    [[maybe_unused]] ColumnSizeByName & each_columns_size, [[maybe_unused]] ColumnSize & total_size) const
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

ColumnSize MergeTreeDataPartCNCH::getColumnSizeImpl(const NameAndTypePair & column, std::unordered_set<String> * processed_substreams) const
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

void MergeTreeDataPartCNCH::removeImpl(bool keep_shared_data) const
{
    for (const auto & [_, projection_part] : projection_parts)
        projection_part->projectionRemove(relative_path, keep_shared_data);

    auto disk = volume->getDisk();
    auto path_on_disk = fs::path(storage.getRelativeDataPath(location)) / relative_path;
    try
    {
        disk->removeFile(path_on_disk / "data");
        disk->removeDirectory(path_on_disk);
    }
    catch(...)
    {
        /// Recursive directory removal does many excessive "stat" syscalls under the hood.
        LOG_ERROR(storage.log, "Cannot quickly remove directory {} by removing files; fallback to recursive removal. Reason: {}", fullPath(disk, path_on_disk), getCurrentExceptionMessage(false));
        disk->removeRecursive(path_on_disk);
    }
}

void MergeTreeDataPartCNCH::projectionRemove(const String & parent_to, bool) const
{
    auto projection_path_on_disk = fs::path(storage.getRelativeDataPath(location)) / relative_path / parent_to;
    auto disk = volume->getDisk();
    try
    {
        disk->removeFile(projection_path_on_disk / "data");
        disk->removeDirectory(projection_path_on_disk);
    }
    catch(...)
    {
        /// Recursive directory removal does many excessive "stat" syscalls under the hood.
        LOG_ERROR(storage.log, "Cannot quickly remove directory {} by removing files; fallback to recursive removal. Reason: {}", fullPath(disk, projection_path_on_disk), getCurrentExceptionMessage(false));
        disk->removeRecursive(projection_path_on_disk);

    }
}

void MergeTreeDataPartCNCH::preload(ThreadPool & pool) const
{
    if (isPartial())
        throw Exception("Preload partial parts in invalid", ErrorCodes::LOGICAL_ERROR);

    Stopwatch watch;
    auto [cache, cache_strategy] = DiskCacheFactory::instance().getDefault();
    MarkRanges all_mark_ranges{MarkRange(0, getMarksCount())};
    IDiskCacheSegmentsVector segments;

    auto addSegments = [&, this, strategy = cache_strategy](const NameAndTypePair & col) {
        ISerialization::StreamCallback callback = [&](const ISerialization::SubstreamPath & substream_path) {
            String stream_name = ISerialization::getFileNameForStream(col.name, substream_path);
            ChecksumsPtr checksums = getChecksums();
            if (!checksums->files.count(stream_name + DATA_FILE_EXTENSION))
                return;

            String mark_file_name = index_granularity_info.getMarksFilePath(stream_name);
            String data_file_name = stream_name + DATA_FILE_EXTENSION;

            auto seg = strategy->transferRangesToSegments<DiskCacheSegment>(
                all_mark_ranges,
                shared_from_this(),
                DiskCacheSegment::FileOffsetAndSize{getFileOffsetOrZero(mark_file_name), getFileSizeOrZero(mark_file_name)},
                getMarksCount(),
                stream_name,
                DATA_FILE_EXTENSION,
                DiskCacheSegment::FileOffsetAndSize{getFileOffsetOrZero(data_file_name), getFileSizeOrZero(data_file_name)});
            segments.insert(segments.end(), std::make_move_iterator(seg.begin()), std::make_move_iterator(seg.end()));
        };
        ISerialization::SubstreamPath substream_path;
        auto serialization = getSerializationForColumn(col);
        serialization->enumerateStreams(callback, substream_path);
    };

    for (const NameAndTypePair & column : *columns_ptr)
    {
        if (column.type->isMap() && !column.type->isMapKVStore())
        {
            /// TODO: map
        }
        else
        {
            addSegments(column);
        }
    }

    /// cache checksums & pk
    /// ChecksumsCache and PrimaryIndexCache will be set during caching to disk
    segments.emplace_back(std::make_shared<ChecksumsDiskCacheSegment>(shared_from_this()));
    segments.emplace_back(std::make_shared<PrimaryIndexDiskCacheSegment>(shared_from_this()));

    pool.scheduleOrThrow([segments = std::move(segments), cache=cache] {
        for (const auto & segment : segments)
        {
            try
            {
                if (cache->get(segment->getSegmentName()).second.empty())
                    segment->cacheToDisk(*cache);
            }
            catch (...)
            {
                /// no exception thrown
            }
        }
    });
}
}
