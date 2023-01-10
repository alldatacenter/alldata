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

#include <Core/Types.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/HDFS/HDFSFileSystem.h>
#include <Common/config.h>
#include <common/logger_useful.h>
#include <Disks/IDisk.h>



namespace DB
{

#define DEFUALT_BUFFER_SIZE 1048576  // default buffer size 1M

/***
 * Helper class to upload and download files from hdfs.
 */
class HDFSDumper
{

public:
    HDFSDumper(const String & hdfs_user_, const String & hdfs_nnproxy, size_t buffer_size_ = DEFUALT_BUFFER_SIZE);

    // upload local parts to hdfs
    void uploadPartsToRemote(const String & local_path, const String & remote_path, std::vector<String> & parts_to_upload);

    // fetch remote part to multiple disks
    std::vector<std::pair<String, DiskPtr>> fetchPartsFromRemote(const Disks & disks,
        const String & remote_path, const String & relative_local_path);

    void uploadFileToRemote(const String & local_path, const String & remote_path);

    void getFileFromRemote(const String & remote_path, const String & local_path);

private:
    Poco::Logger * log = &Poco::Logger::get("HDFSDumper");
    size_t buffer_size;
    HDFSConnectionParams hdfs_params;
    std::unique_ptr<HDFSFileSystem> hdfs_filesystem = nullptr;
};

}
