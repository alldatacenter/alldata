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
#include <Interpreters/Context.h>
#include <Storages/IndexFile/RemoteFileCache.h>
#include <common/logger_useful.h>

namespace DB
{
class KeyIndexFileCache : public IndexFile::RemoteFileCache
{
public:
    KeyIndexFileCache(Context & context, UInt64 max_size);
    ~KeyIndexFileCache() override;

    /// If file is cached locally, return opened fd to it.
    /// Client must call `release(fd)` after reading the file.
    /// Otherwise return -1 and cache the file in the background.
    int get(const IndexFile::RemoteFileInfo & file) override;

    /// Release the fd returned by `get` so that the file could be deleted
    /// if the cache decides to evicts it in order to cache new files.
    void release(int fd) override;

    String cacheFileName(const IndexFile::RemoteFileInfo & file) const override;

private:
    void initCacheFromFileSystem();

    Poco::Logger * log;
    struct Rep;
    std::shared_ptr<Rep> rep;
};

}
