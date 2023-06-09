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

#pragma once

#include <Disks/IDisk.h>
#include <IO/HashingWriteBuffer.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/HDFS/WriteBufferFromHDFS.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Disks/IDisk.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <MergeTreeCommon/CnchStorageCommon.h>
#include <Storages/MergeTree/MergeTreeDataFormatVersion.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>
#include <Storages/MergeTree/S3ObjectMetadata.h>
#include <Common/config.h>
// #include <Encryption/AesEncrypt.h>

namespace DB
{
class MergeTreeMetaBase;

/** Dump local part to cloud storage
  */
class MergeTreeCNCHDataDumper
{
public:
    using uint128 = CityHash_v1_0_2::uint128;

    explicit MergeTreeCNCHDataDumper(
        MergeTreeMetaBase & data_,
        const S3ObjectMetadata::PartGeneratorID& generator_id_,
        const String & magic_code_ = "CNCH",
        const MergeTreeDataFormatVersion version_ = MERGE_TREE_CHCH_DATA_STORAGTE_VERSION);

    /** Dump local part,
      * Returns cnch part with unique name starting with 'tmp_',
      * Yet not added to catalog service.
      *
      * There are meta & data information per part.
      *
      * Meta information will be stored in catalog service.
      *
      * --------meta information--------
      * rows_count
      * marks_count
      * columns
      * partition
      * minmax index
      * ttl
      * --------------------------------
      *
      * Data information will be stored in cloud storage(e.g. s3 hdfs) as one data file,
      * Which will not be updated once generated, only can be rewrited to new data file or be deleted.
      *
      * ------------data header---------
      * magic_code(4 bytes)
      * version(8 bytes)
      * deleted(1 bytes)
      * reserved size(256 - 12 bytes)
      * -----------data content---------
      * columns data files & idx files
      * primary_index
      * checksums
      * metainfo
	  * unique_key_index
      * -----------data footer----------
      * primary_index offset(8 bytes)
      * primary_index size(8 bytes)
      * primary_index checksum(16 bytes)
      * checksums offset(8 bytes)
      * checksums size(8 bytes)
      * checksums checksum(16 bytes)
      * metainfo offset(8 bytes)
      * metainfo size(8 bytes)
      * metainfo checksum(16 bytes)
      * unique_key_index offset(8 bytes)
      * unique_key_index size(8 bytes)
      * unique_key_index checksum(16 bytes)
      * metainfo key (32 bytes)
      * --------------------------------
      */
    MutableMergeTreeDataPartCNCHPtr
    dumpTempPart(const IMutableMergeTreeDataPartPtr & local_part, bool is_temp_prefix = false, const DiskPtr & remote_disk = nullptr) const;

private:
    struct CNCHDataMeta
    {
        off_t index_offset;
        size_t index_size;
        uint128 index_checksum;

        off_t checksums_offset;
        size_t checksums_size;
        uint128 checksums_checksum;

        off_t meta_info_offset;
        size_t meta_info_size;
        uint128 meta_info_checksum;

        off_t unique_key_index_offset;
        size_t unique_key_index_size;
        uint128 unique_key_index_checksum;

        // AesEncrypt::AesKeyByteArray key;
    };
    // static_assert(sizeof(CNCHDataMeta) == 160);
    static_assert(sizeof(CNCHDataMeta) <= MERGE_TREE_STORAGE_CNCH_DATA_FOOTER_SIZE);

    void writeDataFileHeader(WriteBuffer & to, MutableMergeTreeDataPartCNCHPtr & part) const;
    void writeDataFileFooter(WriteBuffer & to, const CNCHDataMeta & meta) const;
    static size_t check(MergeTreeDataPartCNCHPtr remote_part, const std::shared_ptr<MergeTreeDataPartChecksums> & checksums, const CNCHDataMeta & meta);

    NamesAndTypesList getKeyColumns() const;

    MergeTreeMetaBase & data;
    S3ObjectMetadata::PartGeneratorID generator_id;
    Poco::Logger * log;
    String magic_code{"CNCH"};
    MergeTreeDataFormatVersion version{MERGE_TREE_CHCH_DATA_STORAGTE_VERSION};
};

}
