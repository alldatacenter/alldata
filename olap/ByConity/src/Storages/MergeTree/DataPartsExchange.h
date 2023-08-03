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

#pragma once

#include <Interpreters/InterserverIOHandler.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/IStorage_fwd.h>
#include <IO/HashingWriteBuffer.h>
#include <IO/copyData.h>
#include <IO/ConnectionTimeouts.h>
#include <IO/ReadWriteBufferFromHTTP.h>
#include <Common/Throttler.h>


namespace zkutil
{
    class ZooKeeper;
    using ZooKeeperPtr = std::shared_ptr<ZooKeeper>;
}

namespace DB
{

class StorageReplicatedMergeTree;

namespace DataPartsExchange
{

/** Service for sending parts from the table *ReplicatedMergeTree.
  */
class Service final : public InterserverIOEndpoint
{
public:
    explicit Service(MergeTreeData & data_, const StoragePtr & storage_);

    Service(const Service &) = delete;
    Service & operator=(const Service &) = delete;

    std::string getId(const std::string & node_id) const override;
    void processQuery(const HTMLForm & params, ReadBuffer & body, WriteBuffer & out, HTTPServerResponse & response) override;

    void processQueryPart(const HTMLForm & params, ReadBuffer & body, WriteBuffer & out, HTTPServerResponse & response, bool incrementally);
    void processQueryPartList(const HTMLForm & params, ReadBuffer & body, WriteBuffer & out, HTTPServerResponse & response);
    void processQueryExist(const HTMLForm & params, ReadBuffer & body, WriteBuffer & out, HTTPServerResponse & response);

private:
    MergeTreeData::DataPartPtr findPart(const String & name);
    void sendPartFromMemory(
        const MergeTreeData::DataPartPtr & part,
        WriteBuffer & out,
        const std::map<String, std::shared_ptr<IMergeTreeDataPart>> & projections = {});

    MergeTreeData::DataPart::Checksums sendPartFromDisk(
        const MergeTreeData::DataPartPtr & part,
        ReadBuffer & body,
        WriteBuffer & out,
        int client_protocol_version,
        bool incrementally,
        const std::map<String, std::shared_ptr<IMergeTreeDataPart>> & projections = {});

    void sendPartS3Metadata(const MergeTreeData::DataPartPtr & part, WriteBuffer & out);

    /// StorageReplicatedMergeTree::shutdown() waits for all parts exchange handlers to finish,
    /// so Service will never access dangling reference to storage
    MergeTreeData & data;
    std::weak_ptr<IStorage> storage;
    Poco::Logger * log;
};

/** Client for getting the parts from the table *MergeTree.
  */
class Fetcher final : private boost::noncopyable
{
public:
    explicit Fetcher(MergeTreeData & data_) : data(data_), log(&Poco::Logger::get("Fetcher")) {}

    /// Downloads a part to tmp_directory. If to_detached - downloads to the `detached` directory.
    MergeTreeData::MutableDataPartPtr fetchPart(
        const StorageMetadataPtr & metadata_snapshot,
        ContextPtr context,
        const String & part_name,
        const String & replica_path,
        const String & host,
        int port,
        const ConnectionTimeouts & timeouts,
        const String & user,
        const String & password,
        const String & interserver_scheme,
        ThrottlerPtr throttler,
        bool to_detached = false,
        const String & tmp_prefix_ = "",
        std::optional<CurrentlySubmergingEmergingTagger> * tagger_ptr = nullptr,
        bool try_use_s3_copy = true,
        const DiskPtr disk_s3 = nullptr,
        bool incrementally = false);

    Strings fetchPartList(
        const String & partition_id,
        const String & filter,
        const String & endpoint_str,
        const String & host,
        int port,
        const ConnectionTimeouts & timeouts,
        const String & user,
        const String & password,
        const String & interserver_scheme);

    /// You need to stop the data transfer.
    ActionBlocker blocker;

private:
    void downloadBaseOrProjectionPartToDisk(
            const String & replica_path,
            const String & part_download_path,
            bool sync,
            DiskPtr disk,
            PooledReadWriteBufferFromHTTP & in,
            MergeTreeData::DataPart::Checksums & checksums,
            ThrottlerPtr throttler,
            const MergeTreeData::DataPartPtr & old_version_part = nullptr) const;


    MergeTreeData::MutableDataPartPtr downloadPartToDisk(
            const String & part_name,
            const String & replica_path,
            bool to_detached,
            const String & tmp_prefix_,
            bool sync,
            DiskPtr disk,
            PooledReadWriteBufferFromHTTP & in,
            size_t projections,
            MergeTreeData::DataPart::Checksums & checksums,
            ThrottlerPtr throttler,
            const MergeTreeData::DataPartPtr & old_version_part = nullptr);

    MergeTreeData::MutableDataPartPtr downloadPartToMemory(
            const String & part_name,
            const UUID & part_uuid,
            const StorageMetadataPtr & metadata_snapshot,
            ContextPtr context,
            ReservationPtr reservation,
            PooledReadWriteBufferFromHTTP & in,
            size_t projections,
            ThrottlerPtr throttler);

    MergeTreeData::MutableDataPartPtr downloadPartToS3(
            const String & part_name,
            const String & replica_path,
            bool to_detached,
            const String & tmp_prefix_,
            const Disks & disks_s3,
            PooledReadWriteBufferFromHTTP & in,
            ThrottlerPtr throttler);

    MergeTreeData & data;
    Poco::Logger * log;
};

}

}
