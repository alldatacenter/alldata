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

#include <memory>
#include <Core/Types.h>
#include <Storages/HDFS/HDFSCommon.h>

namespace DB::IndexFile
{
struct RemoteFileInfo
{
    HDFSConnectionParams hdfs_params;
    String path;            /// full path to the remote file containing the logical file
    UInt64 start_offset;    /// offset to the beginning of the logical file
    size_t size;            /// size of the logical file
    String cache_key;       /// unique identifier for the file in the cache
};

class RemoteFileCache
{
public:
    virtual ~RemoteFileCache() = default;

    /// If file is cached locally, return opened fd to it.
    /// Client must call `release(fd)` after reading the file.
    /// Otherwise return -1 and cache the file in the background.
    virtual int get(const RemoteFileInfo & file) = 0;

    /// Release the fd returned by `get` so that the file could be deleted
    /// if the cache decides to evicts it in order to cache new files.
    virtual void release(int fd) = 0;

    virtual String cacheFileName(const RemoteFileInfo & file) const = 0;
};

using RemoteFileCachePtr = std::shared_ptr<RemoteFileCache>;

}

