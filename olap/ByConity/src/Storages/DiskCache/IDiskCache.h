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

#include <Core/BackgroundSchedulePool.h>
#include <Disks/IDisk.h>
#include <Storages/DiskCache/DiskCacheSettings.h>
#include <Storages/DiskCache/IDiskCacheSegment.h>

#include <common/logger_useful.h>
#include <optional>

namespace DB
{
class Context;
class ReadBuffer;
class WriteBuffer;
class Throttler;
class IVolume;
class IDisk;

using ThrottlerPtr = std::shared_ptr<Throttler>;
using VolumePtr = std::shared_ptr<IVolume>;
using DiskPtr = std::shared_ptr<IDisk>;

class IDiskCache
{
public:
    explicit IDiskCache(Context & context_, VolumePtr volume_, const DiskCacheSettings & settings);
    virtual ~IDiskCache()
    {
        try
        {
            shutdown();
        }
        catch (...) {}
    }

    IDiskCache(const IDiskCache &) = delete;
    IDiskCache & operator=(const IDiskCache &) = delete;

    void shutdown();
    void asyncLoad();

    /// set segment name in cache and write value to disk cache
    virtual void set(const String & key, ReadBuffer & value, size_t weight_hint) = 0;

    /// get segment from cache and return local path if exists.
    virtual std::pair<DiskPtr, String> get(const String & key) = 0;

    /// initialize disk cache from local disk
    virtual void load() = 0;

    /// get number of keys
    virtual size_t getKeyCount() const = 0;

    /// get cached files size
    virtual size_t getCachedSize() const = 0;

    void cacheSegmentsToLocalDisk(IDiskCacheSegmentsVector hit_segments);

    VolumePtr getStorageVolume() const { return volume; }
    ThrottlerPtr getDiskCacheThrottler() const { return disk_cache_throttler; }
    Poco::Logger * getLogger() const { return log; }

protected:
    Context & context;
    VolumePtr volume;
    DiskCacheSettings settings;
    ThrottlerPtr disk_cache_throttler;
    std::atomic<bool> shutdown_called {false};
    BackgroundSchedulePool::TaskHolder sync_task;

private:
    bool scheduleCacheTask(const std::function<void()> & task);

    Poco::Logger * log = &Poco::Logger::get("DiskCache");
};

using IDiskCachePtr = std::shared_ptr<IDiskCache>;

}
