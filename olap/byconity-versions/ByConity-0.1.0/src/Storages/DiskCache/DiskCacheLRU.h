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

#include <atomic>
#include <filesystem>
#include <Storages/DiskCache/IDiskCache.h>
#include <Storages/DiskCache/BucketLRUCache.h>
#include <Storages/DiskCache/ShardCache.h>
#include <sys/types.h>
#include <Poco/Logger.h>

namespace DB
{

class DiskCacheMeta
{
public:
    enum class State
    {
        Caching,
        Cached,
        Deleting,
    };

    DiskCacheMeta(State state_, const DiskPtr & disk_, size_t size_):
        state(state_), disk(disk_), size(size_) {}

    State state;
    DiskPtr disk;
    size_t size;
};

struct DiskCacheWeightFunction
{
    size_t operator()(const DiskCacheMeta& meta) const
    {
        if (meta.state == DiskCacheMeta::State::Cached)
        {
            return meta.size;
        }
        return 0;
    }
};

//            ┌───────┐            ┌──────┐        ┌────────┐               ┌────┐
// CacheState │Caching├───────────►│Cached├───────►│Deleting├──────────────►│Gone│
//            └──┬────┘            └──────┘        └───┬────┘               └────┘
//               │                     ▲               │                       ▲
// FileState     └───►Temp────►Final───┘               └───►Final────►Deleted──┘
class DiskCacheLRU: public IDiskCache
{
public:
    using KeyType = UInt128;

    DiskCacheLRU(Context & context_, const VolumePtr & volume, const DiskCacheSettings & settings);

    void set(const String& seg_name, ReadBuffer& value, size_t weight_hint) override;
    std::pair<DiskPtr, String> get(const String& seg_name) override;
    void load() override;

    size_t getKeyCount() const override { return containers.count(); }
    size_t getCachedSize() const override { return containers.weight(); }

    /// for test
    static KeyType hash(const String & seg_name);
    static String hexKey(const KeyType & key);
    static std::optional<KeyType> unhexKey(const String & hex);
    static std::filesystem::path getRelativePath(const KeyType & key);

private:
    size_t writeSegment(const String& seg_name, ReadBuffer& buffer, ReservationPtr& reservation);

    std::pair<bool, std::shared_ptr<DiskCacheMeta>> onEvictSegment(const KeyType & key,
        const std::shared_ptr<DiskCacheMeta>& meta, size_t);
    void afterEvictSegment(const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>>& removed_elements,
        const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>>& updated_elements);

    struct DiskIterator : private boost::noncopyable
    {
        explicit DiskIterator(
            DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk_, int min_depth_parallel_, int max_depth_parallel_);
        virtual ~DiskIterator() = default;

        void exec(std::filesystem::path entry_path);
        virtual void iterateDirectory(std::filesystem::path rel_path, size_t depth);
        virtual void iterateFile(std::filesystem::path file_path, size_t file_size) = 0;

        // lifecycle shorter than disk cache
        DiskCacheLRU & disk_cache;
        DiskPtr disk;

        /// worker_per_disk > 1 to enable parallel iterate
        size_t worker_per_disk{1};

        /// min iterate depth to use thread pool
        int min_depth_parallel{-1};

        /// max iterate depth to use thread pool
        int max_depth_parallel{-1};

        std::unique_ptr<ThreadPool> pool;
        ExceptionHandler handler;
        Poco::Logger * log {&Poco::Logger::get("DiskIterator")};
    };

    /// Load from disk when Disk cache starts up
    struct DiskCacheLoader : DiskIterator
    {
        explicit DiskCacheLoader(
            DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk, int min_depth_parallel, int max_depth_parallel);
        ~DiskCacheLoader() override;
        void iterateFile(std::filesystem::path file_path, size_t file_size) override;

        std::atomic_size_t total_loaded = 0;
    };

    /// Migrate from old format to new format
    struct DiskCacheMigrator : DiskIterator
    {
        explicit DiskCacheMigrator(
            DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk, int min_depth_parallel, int max_depth_parallel);
        ~DiskCacheMigrator() override;
        void iterateFile(std::filesystem::path file_path, size_t file_size) override;

        std::atomic_size_t total_migrated = 0;
    };

    ThrottlerPtr set_rate_throttler;
    Poco::Logger * log {&Poco::Logger::get("DiskCacheLRU")};
    ShardCache<KeyType, UInt128Hash, BucketLRUCache<KeyType, DiskCacheMeta, UInt128Hash, DiskCacheWeightFunction>> containers;
};

}
