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

#include <Disks/IDiskRemote.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Core/UUID.h>
#include <memory>


namespace DB
{

struct DiskHDFSSettings
{
    size_t min_bytes_for_seek;
    int thread_pool_size;
    int objects_chunk_size_to_delete;

    DiskHDFSSettings(
            int min_bytes_for_seek_,
            int thread_pool_size_,
            int objects_chunk_size_to_delete_)
        : min_bytes_for_seek(min_bytes_for_seek_)
        , thread_pool_size(thread_pool_size_)
        , objects_chunk_size_to_delete(objects_chunk_size_to_delete_) {}
};


/**
 * Storage for persisting data in HDFS and metadata on the local disk.
 * Files are represented by file in local filesystem (clickhouse_root/disks/disk_name/path/to/file)
 * that contains HDFS object key with actual data.
 */
class DiskHDFS final : public IDiskRemote
{
public:
    using SettingsPtr = std::unique_ptr<DiskHDFSSettings>;

    DiskHDFS(
        const String & disk_name_,
        const String & hdfs_root_path_,
        SettingsPtr settings_,
        const String & metadata_path_,
        const Poco::Util::AbstractConfiguration & config_);

    DiskType::Type getType() const override { return DiskType::Type::HDFS; }

    std::unique_ptr<ReadBufferFromFileBase> readFile(
        const String & path,
        const ReadSettings& rd_settings) const override;

    std::unique_ptr<WriteBufferFromFileBase> writeFile(const String & path, const WriteSettings& wr_settings) override;

    void removeFromRemoteFS(RemoteFSPathKeeperPtr fs_paths_keeper) override;

    RemoteFSPathKeeperPtr createFSPathKeeper() const override;

private:
    String getRandomName() { return toString(UUIDHelpers::generateV4()); }

    const Poco::Util::AbstractConfiguration & config;

    HDFSBuilderWrapper hdfs_builder;
    HDFSFSPtr hdfs_fs;

    SettingsPtr settings;
};

}
