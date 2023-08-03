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

#include <Storages/MergeTree/MergeTreeMetaDataCommon.h>
#include <Storages/MergeTree/MergeTreePartInfo.h>
#include <Parsers/queryToString.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <Common/hex.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNKNOWN_DISK;
    extern const int BROKEN_META_DATA;
}

String getSerializedPartMeta(const DataPartPtr & part)
{
    Protos::DataPartModel part_data;
    part_data.set_type(part->getType().toString());
    part_data.set_disk_id(part->volume->getDisk()->getID());
    part_data.set_bytes_on_disk(part->getBytesOnDisk());
    part_data.set_rows_count(part->rows_count);

    {
        WriteBufferFromOwnString buffer;
        part->storePartitionAndMinMaxIndex(buffer);
        part_data.set_partition_and_minmax(buffer.str());
    }

    part_data.set_last_modify_time(part->modification_time);
    if (part->uuid != UUIDHelpers::Nil)
    {
        part_data.set_uuid(toString(part->uuid));
    }

    {
        WriteBufferFromOwnString buffer;
        part->getColumns().writeText(buffer);
        part_data.set_columns(buffer.str());
    }

    {
        WriteBufferFromOwnString buffer;
        part->ttl_infos.write(buffer);
        part_data.set_ttl_infos(buffer.str());
    }

    {
        WriteBufferFromOwnString buffer;
        part->versions->write(buffer);
        part_data.set_versions(buffer.str());
    }

    part_data.set_marks_count(part->getMarksCount());

    if (part->index_granularity_info.is_adaptive)
    {
        /// Set granunarity_marks if current part is using adaptive index granularity.
        for (auto & granularity : part->index_granularity.getIndexGranularities())
            part_data.add_index_granularities(granularity);
    }

    /// set parent part if current part is projection part.
    if (part->isProjectionPart())
        part_data.set_parent_part(part->getParentPart()->name);

    part_data.set_compress_codec(queryToString(part->default_codec->getFullCodecDesc()));

    return part_data.SerializeAsString();
}

MutableDataPartPtr buildPartFromMeta(const MergeTreeMetaBase & storage, const String & part_name, const Protos::DataPartModel & part_data)
{
    MergeTreePartInfo part_info;
    if (!MergeTreePartInfo::tryParsePartName(part_name, &part_info, storage.format_version))
        throw Exception("Cannot parse part info from name : " + part_name, ErrorCodes::BROKEN_META_DATA);

    DiskPtr disk_ptr = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByID(part_data.disk_id());
    if (!disk_ptr)
        throw Exception("Cannot get the disk with id '" + std::to_string(part_data.disk_id()) + "' for part : " + part_name, ErrorCodes::UNKNOWN_DISK);

    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part_name, disk_ptr, 0);
    MergeTreeDataPartType type;
    type.fromString(part_data.type());
    MutableDataPartPtr part = storage.createPart(part_name, type, part_info, single_disk_volume, part_name);

    deserializePartCommon(part_data, part);

    return part;
}

MutableDataPartPtr buildProjectionFromMeta(const MergeTreeMetaBase & storage, const String & projection_name, const Protos::DataPartModel & part_data, const IMergeTreeDataPart * parent)
{
    MergeTreePartInfo part_info("all", 0, 0, 0);

    MergeTreeDataPartType type;
    type.fromString(part_data.type());
    MutableDataPartPtr part = storage.createPart(projection_name, part_info, parent->volume,  projection_name + ".proj", parent);

    deserializePartCommon(part_data, part);

    return part;
}

void deserializePartCommon(const Protos::DataPartModel & part_data, MutableDataPartPtr & part)
{
    /// The order of deserialization matters.
    if (part_data.has_uuid())
    {
        UUID uuid;
        ReadBufferFromString buf(part_data.uuid());
        tryReadUUIDText(uuid, buf);
        part->uuid = uuid;
    }

    part->rows_count = part_data.rows_count();
    part->setBytesOnDisk(part_data.bytes_on_disk());

    if (part_data.has_columns())
    {
        ReadBufferFromString buf(part_data.columns());
        part->loadColumns(buf);
    }
    else
    {
        throw Exception("No columns info in MergeTree part meta", ErrorCodes::BROKEN_META_DATA);
    }

    {
        size_t marks_count = part_data.marks_count();
        std::vector<size_t> index_granularities {};
        for (auto & granularity : part_data.index_granularities())
            index_granularities.push_back(granularity);

        part->loadIndexGranularity(marks_count, index_granularities);
    }

    {
        ReadBufferFromString buf(part_data.partition_and_minmax());
        part->loadPartitionAndMinMaxIndex(buf);
    }

    if (part_data.has_ttl_infos())
    {
        ReadBufferFromString buf(part_data.ttl_infos());
        part->loadTTLInfos(buf);
    }

    if (part_data.has_versions())
    {
        ReadBufferFromString buf(part_data.versions());
        part->versions->read(buf);
    }

    part->loadDefaultCompressionCodec(part_data.compress_codec());
    part->modification_time = part_data.last_modify_time();
}


/** ----------------------- COMPATIBLE CODE BEGIN-------------------------- */
String unescapeForDiskName(const String & s)
{
    String res;
    const char * pos = s.data();
    const char * end = pos + s.size();

    while (pos != end)
    {
        if (!(*pos == '%' && pos + 2 < end))
        {
            res += *pos;
            ++pos;
        }
        else
        {
            ++pos;
            res += unhex2(pos);
            pos += 2;
        }
    }
    return res;
}

MutableDataPartPtr createPartFromRaw(const MergeTreeMetaBase & storage, const String & key, const String & meta)
{
    auto pos = key.rfind('_');
    String part_name = key.substr(0, pos);
    String disk_name = unescapeForDiskName(key.substr(pos + 1));
    DiskPtr disk_ptr = storage.getStoragePolicy(IStorage::StorageLocation::MAIN)->getDiskByName(disk_name);
    if (!disk_ptr)
        throw Exception("Cannot get the disk with name '" + disk_name + "' for part : " + part_name, ErrorCodes::UNKNOWN_DISK);

    MergeTreePartInfo part_info;
    if (!MergeTreePartInfo::tryParsePartName(part_name, &part_info, storage.format_version))
        throw Exception("Cannot parse part info from name : " + part_name, ErrorCodes::BROKEN_META_DATA);

    auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part_name, disk_ptr, 0);
    MutableDataPartPtr part = storage.createPart(part_name, MergeTreeDataPartType::WIDE, part_info, single_disk_volume, part_name);

    /// do not need checksums here.
    part->deserializeMetaInfo(meta);

    return part;
}
/*  -----------------------  COMPATIBLE CODE END -------------------------- */

}
