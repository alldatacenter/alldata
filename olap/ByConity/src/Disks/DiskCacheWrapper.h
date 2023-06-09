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

#include <unordered_map>
#include <common/logger_useful.h>
#include "DiskDecorator.h"
#include "DiskLocal.h"

namespace DB
{
struct FileDownloadMetadata;

/**
 * Simple cache wrapper.
 * Tries to cache files matched by predicate to given local disk (cache disk).
 *
 * When writeFile() is invoked wrapper firstly writes file to cache.
 * After write buffer is finalized actual file is stored to underlying disk.
 *
 * When readFile() is invoked and file exists in cache wrapper reads this file from cache.
 * If file doesn't exist wrapper downloads this file from underlying disk to cache.
 * readFile() invocation is thread-safe.
 */
class DiskCacheWrapper : public DiskDecorator
{
public:
    DiskCacheWrapper(
        std::shared_ptr<IDisk> delegate_,
        std::shared_ptr<DiskLocal> cache_disk_,
        std::function<bool(const String &)> cache_file_predicate_);
    void createDirectory(const String & path) override;
    void createDirectories(const String & path) override;
    void clearDirectory(const String & path) override;
    void moveDirectory(const String & from_path, const String & to_path) override;
    void moveFile(const String & from_path, const String & to_path) override;
    void replaceFile(const String & from_path, const String & to_path) override;

    std::unique_ptr<ReadBufferFromFileBase> readFile(
        const String & path,
        const ReadSettings& settings) const override;

    std::unique_ptr<WriteBufferFromFileBase> writeFile(
        const String & path,
        const WriteSettings& settings) override;

    void removeFile(const String & path) override;
    void removeFileIfExists(const String & path) override;
    void removeDirectory(const String & path) override;
    void removeRecursive(const String & path) override;
    void removeSharedFile(const String & path, bool keep_s3) override;
    void removeSharedRecursive(const String & path, bool keep_s3) override;
    void createHardLink(const String & src_path, const String & dst_path) override;
    ReservationPtr reserve(UInt64 bytes) override;

private:
    std::shared_ptr<FileDownloadMetadata> acquireDownloadMetadata(const String & path) const;

    /// Disk to cache files.
    std::shared_ptr<DiskLocal> cache_disk;
    /// Cache only files satisfies predicate.
    const std::function<bool(const String &)> cache_file_predicate;
    /// Contains information about currently running file downloads to cache.
    mutable std::unordered_map<String, std::weak_ptr<FileDownloadMetadata>> file_downloads;
    /// Protects concurrent downloading files to cache.
    mutable std::mutex mutex;

    Poco::Logger * log = &Poco::Logger::get("DiskCache");
};

}
