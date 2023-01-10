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

#include "MetaFileDiskCacheSegment.h"

#include <Core/UUID.h>
#include <IO/MemoryReadWriteBuffer.h>
#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>

namespace DB
{
ChecksumsDiskCacheSegment::ChecksumsDiskCacheSegment(IMergeTreeDataPartPtr data_part_)
    : IDiskCacheSegment(0, 0)
    , data_part(std::move(data_part_))
    , storage(data_part->storage.shared_from_this())
    , segment_name(formatSegmentName(
          UUIDHelpers::UUIDToString(data_part->storage.getStorageUUID()), data_part->name, "", segment_number, "checksums.txt"))
{
}

String ChecksumsDiskCacheSegment::getSegmentName() const
{
    return segment_name;
}

void ChecksumsDiskCacheSegment::cacheToDisk(IDiskCache & disk_cache)
{
    auto checksums = data_part->getChecksums();
    if (!checksums)
        return;

    MemoryWriteBuffer write_buffer;
    checksums->write(write_buffer);
    size_t file_size = write_buffer.count();
    if (auto read_buffer = write_buffer.tryGetReadBuffer())
    {
        disk_cache.set(getSegmentName(), *read_buffer, file_size);
    }
}

PrimaryIndexDiskCacheSegment::PrimaryIndexDiskCacheSegment(IMergeTreeDataPartPtr data_part_)
    : IDiskCacheSegment(0, 0)
    , data_part(std::move(data_part_))
    , storage(data_part->storage.shared_from_this())
    , segment_name(formatSegmentName(
          UUIDHelpers::UUIDToString(data_part->storage.getStorageUUID()), data_part->name, "", segment_number, "primary.idx"))
{
}

String PrimaryIndexDiskCacheSegment::getSegmentName() const
{
    return segment_name;
}

void PrimaryIndexDiskCacheSegment::cacheToDisk(IDiskCache & disk_cache)
{
    auto metadata_snapshot = data_part->storage.getInMemoryMetadataPtr();
    const auto & primary_key = metadata_snapshot->getPrimaryKey();
    size_t key_size = primary_key.column_names.size();

    if (key_size == 0)
        return;

    auto index = data_part->getIndex();
    size_t marks_count = data_part->getMarksCount();
    MemoryWriteBuffer write_buffer;

    Serializations serializations(key_size);
    for (size_t j = 0; j < key_size; ++j)
        serializations[j] = primary_key.data_types[j]->getDefaultSerialization();

    for (size_t i = 0; i < marks_count; ++i)
    {
        for (size_t j = 0; j < index->size(); ++j)
            serializations[j]->serializeBinary(*index->at(j), i, write_buffer);
    }

    size_t file_size = write_buffer.count();
    if (auto read_buffer = write_buffer.tryGetReadBuffer())
    {
        disk_cache.set(getSegmentName(), *read_buffer, file_size);
    }
}

}
