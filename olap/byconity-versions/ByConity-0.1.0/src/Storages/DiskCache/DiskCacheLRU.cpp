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

#include <Storages/DiskCache/DiskCacheLRU.h>
#include <sys/stat.h>
#include <atomic>
#include <filesystem>
#include <memory>
#include <string>
#include <string_view>
#include "Common/hex.h"
#include <Common/Throttler.h>
#include <Common/setThreadName.h>
#include "Interpreters/Context.h"
#include <common/errnoToString.h>
#include <Disks/IVolume.h>
#include <IO/WriteBufferFromString.h>
#include <IO/copyData.h>

namespace fs = std::filesystem;

namespace CurrentMetrics
{
    extern const Metric DiskCacheEvictQueueLength;
}

namespace ProfileEvents
{
    extern const Event DiskCacheGetMetaMicroSeconds;
    extern const Event DiskCacheGetTotalOps;
    extern const Event DiskCacheSetTotalOps;
}

namespace DB
{

namespace ErrorCodes
{
    extern const int SYSTEM_ERROR;
}

static constexpr auto DISK_CACHE_TEMP_FILE_SUFFIX = ".temp";
static constexpr auto TMP_SUFFIX_LEN = std::char_traits<char>::length(DISK_CACHE_TEMP_FILE_SUFFIX);
/// backward compatibility
/// background load thread to migrate from old to new format
static constexpr auto PREV_DISK_CACHE_DIR_NAME = "disk_cache";
static constexpr auto DISK_CACHE_DIR_NAME = "disk_cache_v1";

namespace
{
    constexpr size_t HEX_KEY_LEN = sizeof(DiskCacheLRU::KeyType) * 2;

    UInt64 unhex16(const char * data)
    {
        UInt64 res = 0;
        for (size_t i = 0; i < sizeof(UInt64) * 2; ++i, ++data)
        {
            res <<= 4;
            res += static_cast<UInt64>(unhex(*data));
        }
        return res;
    }

    bool isHexKey(const String & hex_key)
    {
        if (hex_key.size() != HEX_KEY_LEN)
            return false;

        for (char c : hex_key)
        {
            if (!(isNumericASCII(c) || (c >= 'a' && c <= 'f')))
                return false;
        }

        return true;
    }
}

DiskCacheLRU::DiskCacheLRU(
    Context & context_, const VolumePtr & volume_, const DiskCacheSettings & settings_)
    : IDiskCache(context_, volume_, settings_)
    , set_rate_throttler(settings_.cache_set_rate_limit == 0 ? nullptr : std::make_shared<Throttler>(settings_.cache_set_rate_limit))
    , containers(
          settings.cache_shard_num,
          BucketLRUCache<KeyType, DiskCacheMeta, UInt128Hash, DiskCacheWeightFunction>::Options{
              .lru_update_interval = static_cast<UInt32>(settings.lru_update_interval),
              .mapping_bucket_size = static_cast<UInt32>(std::max(1UL, settings.mapping_bucket_size / settings.cache_shard_num)),
              .max_size = std::max(static_cast<size_t>(1), settings.lru_max_size / settings.cache_shard_num),
              .enable_customize_evict_handler = true,
              .customize_evict_handler
              = [this](
                    const KeyType & key, const std::shared_ptr<DiskCacheMeta> & meta, size_t sz) { return onEvictSegment(key, meta, sz); },
              .customize_post_evict_handler =
                  [this](
                      const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>> & removed_elements,
                      const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>> & updated_elements) {
                      afterEvictSegment(removed_elements, updated_elements);
                  },
          })
{
    if (settings.cache_load_dispatcher_drill_down_level < -1)
    {
        throw Exception(fmt::format("Load dispatcher's drill down level {} invalid, "
            "must be positive or -1", settings.cache_load_dispatcher_drill_down_level),
            ErrorCodes::BAD_ARGUMENTS);
    }
}

DiskCacheLRU::KeyType DiskCacheLRU::hash(const String & seg_key)
{
    size_t stream_name_pos = seg_key.find_last_of('/');
    if (stream_name_pos == std::string::npos)
        throw Exception("Invalid seg key: " + seg_key, ErrorCodes::LOGICAL_ERROR);

    stream_name_pos += 1;

    /// hash of stream name
    auto low = sipHash64(seg_key.data() + stream_name_pos, seg_key.size() - stream_name_pos);

    /// hash of (table_uuid + part_name)
    auto high = sipHash64(seg_key.data(), stream_name_pos - 1);

    return {high, low};
}

String DiskCacheLRU::hexKey(const KeyType & key)
{
    std::string res(HEX_KEY_LEN, '\0');
    /// Use little endian
    writeHexUIntLowercase(key, res.data());
    return res;
}

std::optional<DiskCacheLRU::KeyType> DiskCacheLRU::unhexKey(const String & hex_key)
{
    if (!isHexKey(hex_key))
        return {};

    auto low = unhex16(hex_key.data());
    auto high = unhex16(hex_key.data() + HEX_KEY_LEN / 2);

    return UInt128{high, low};
}

// disk_cache_v1/uuid_part_name[:3]/uuid_part_name/stream_name
// e.g. disk_cache_v1/752/752573bf0b591cd/de3e88c72ced6c3d
// files belongs to the same part will be placed under the same path
fs::path DiskCacheLRU::getRelativePath(const DiskCacheLRU::KeyType & hash_key)
{
    String hex_key = hexKey(hash_key);
    std::string_view view(hex_key);
    std::string_view hex_key_low = view.substr(0, HEX_KEY_LEN / 2);
    std::string_view hex_key_high = view.substr(HEX_KEY_LEN / 2, HEX_KEY_LEN);

    return fs::path(DISK_CACHE_DIR_NAME) / hex_key_high.substr(0, 3) / hex_key_high / hex_key_low;
}

void DiskCacheLRU::set(const String& seg_name, ReadBuffer& value, size_t weight_hint)
{
    // Limit set rate to avoid too high write iops or lock contention
    if (set_rate_throttler)
    {
        set_rate_throttler->add(1);
    }

    ProfileEvents::increment(ProfileEvents::DiskCacheSetTotalOps);

    auto key = hash(seg_name);
    auto& shard = containers.shard(key);
    // Insert cache meta first, if there is a entry already there, skip this insert
    bool inserted = shard.emplace(key, std::make_shared<DiskCacheMeta>(
        DiskCacheMeta::State::Caching, nullptr, 0
    ));
    if (!inserted)
    {
        return;
    }

    ReservationPtr reserved_space = nullptr;
    try
    {
        // Avoid reserve as much as possible, since it will acquire a disk level
        // exclusive lock
        reserved_space = volume->reserve(weight_hint);
        if (reserved_space == nullptr) {
            throw Exception("Failed to reserve space", ErrorCodes::BAD_ARGUMENTS);
        }

        // Write data to local
        size_t weight = writeSegment(seg_name, value, reserved_space);

        // Update meta in lru cache, it must still there, since it should get evicted
        // since it have 0 weight
        shard.update(key, std::make_shared<DiskCacheMeta>(
            DiskCacheMeta::State::Cached, reserved_space->getDisk(), weight
        ));
    }
    catch(...)
    {
        String local_disk_path = reserved_space == nullptr ? "" : reserved_space->getDisk()->getPath();
        tryLogCurrentException(log, fmt::format("Failed to key {} "
            "to local, disk path: {}, weight: {}", seg_name, local_disk_path, weight_hint));
        shard.erase(key);
    }
}

std::pair<DiskPtr, String> DiskCacheLRU::get(const String & seg_name)
{
    ProfileEvents::increment(ProfileEvents::DiskCacheGetTotalOps);
    Stopwatch watch;
    SCOPE_EXIT({ProfileEvents::increment(ProfileEvents::DiskCacheGetMetaMicroSeconds,
        watch.elapsedMicroseconds());});

    auto key = hash(seg_name);
    auto& shard = containers.shard(key);
    std::shared_ptr<DiskCacheMeta> cache_meta = shard.get(key);
    if (cache_meta == nullptr || cache_meta->state != DiskCacheMeta::State::Cached)
    {
        return {};
    }

    if (unlikely(cache_meta->disk == nullptr))
    {
        // Disk is removed, erase it, should be rare
        shard.erase(key);
        return {};
    }

    return {cache_meta->disk, getRelativePath(key)};
}

size_t DiskCacheLRU::writeSegment(const String& seg_key, ReadBuffer& buffer, ReservationPtr& reservation)
{
    DiskPtr disk = reservation->getDisk();
    String cache_rel_path = getRelativePath(hash(seg_key));
    String temp_cache_rel_path = cache_rel_path + ".temp";

    try
    {
        // Create parent directory
        disk->createDirectories(fs::path(cache_rel_path).parent_path());

        // Write into temporary file, by default it will truncate this file
        size_t written_size = 0;
        {
            WriteBufferFromFile to(fs::path(disk->getPath()) / temp_cache_rel_path);
            copyData(buffer, to);

            written_size = to.count();
        }

        // Finish file writing, rename it, there shouldn't be any threads trying
        // to modify these files now
        disk->replaceFile(temp_cache_rel_path, cache_rel_path);

        return written_size;
    }
    catch (...)
    {
        disk->removeFileIfExists(temp_cache_rel_path);
        disk->removeFileIfExists(cache_rel_path);
        throw;
    }
}

std::pair<bool, std::shared_ptr<DiskCacheMeta>> DiskCacheLRU::onEvictSegment(
    [[maybe_unused]]const KeyType & key, const std::shared_ptr<DiskCacheMeta>& meta, size_t)
{
    if (meta->state != DiskCacheMeta::State::Cached)
    {
        return {false, nullptr};
    }

    return {false, std::make_shared<DiskCacheMeta>(DiskCacheMeta::State::Deleting,
        meta->disk, 0)};
}

// NOTE(wsy) This is called outside lru's lock, maybe we can remove evict thread pool
void DiskCacheLRU::afterEvictSegment([[maybe_unused]]const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>>& removed_elements,
    const std::vector<std::pair<KeyType, std::shared_ptr<DiskCacheMeta>>>& updated_elements)
{
    if (shutdown_called)
        return;

    auto& thread_pool = context.getLocalDiskCacheEvictThreadPool();
    for (auto iter = updated_elements.begin(); iter != updated_elements.end(); ++iter)
    {
        DiskPtr disk = iter->second->disk;
        if (unlikely(disk == nullptr))
        {
            continue;
        }

        thread_pool.scheduleOrThrowOnError([this, key = iter->first, dsk = std::move(disk), &thread_pool]() {
            try
            {
                auto rel_path = getRelativePath(key);
                dsk->removeRecursive(rel_path);

                // Since we are holding locks of lru when calling onEvictSegment,
                // this erase must happen after state is update to Deleting
                auto& shard = containers.shard(key);
                shard.erase(key);
            }
            catch (...)
            {
                tryLogCurrentException(log, fmt::format("Failed to remove cache {}, "
                    "disk path {}", String(getRelativePath(key)), dsk->getPath()));
            }

            /// remove parent path if it's empty
            try
            {
                auto rel_path = getRelativePath(key);
                if (dsk->isDirectoryEmpty(rel_path.parent_path()))
                {
                    dsk->removeDirectory(rel_path.parent_path());
                }
            }
            catch (...)
            {
            }

            CurrentMetrics::set(CurrentMetrics::DiskCacheEvictQueueLength, thread_pool.active() - 1);
        });
    }
}

void DiskCacheLRU::load()
{
    Stopwatch watch;
    SCOPE_EXIT({ LOG_INFO(log, fmt::format("load thread takes {} ms", watch.elapsedMilliseconds())); });
    Disks disks = volume->getDisks();
    if (settings.cache_dispatcher_per_disk)
    {
        ThreadPool dispatcher_pool(disks.size());
        ExceptionHandler except_handler;
        for (const DiskPtr & disk : disks)
        {
            dispatcher_pool.scheduleOrThrowOnError(createExceptionHandledJob(
                [this, disk] {
                    auto loader = std::make_unique<DiskCacheLoader>(
                        *this, disk, settings.cache_loader_per_disk, 1, 1);
                    loader->exec(fs::path(DISK_CACHE_DIR_NAME) / "");
                },
                except_handler));

            dispatcher_pool.scheduleOrThrowOnError(createExceptionHandledJob(
                [this, disk] {
                    auto migrator = std::make_unique<DiskCacheMigrator>(
                        *this, disk, settings.cache_loader_per_disk, 1, 1);
                    migrator->exec(fs::path(PREV_DISK_CACHE_DIR_NAME) / "");
                },
                except_handler));
        }
        dispatcher_pool.wait();
        except_handler.throwIfException();
    }
    else
    {
        setThreadName("DCDispatcher");

        for (const DiskPtr & disk : disks)
        {
            auto loader = std::make_unique<DiskCacheLoader>(*this, disk, settings.cache_loader_per_disk, 1, 1);
            loader->exec(fs::path(DISK_CACHE_DIR_NAME) / "");
        }

        for (const DiskPtr & disk : disks)
        {
            auto migrator = std::make_unique<DiskCacheMigrator>(*this, disk, settings.cache_loader_per_disk, 1, 1);
            migrator->exec(fs::path(PREV_DISK_CACHE_DIR_NAME) / "");
        }
    }
}

DiskCacheLRU::DiskIterator::DiskIterator(
    DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk_, int min_depth_parallel_, int max_depth_parallel_)
    : disk_cache(cache_)
    , disk(std::move(disk_))
    , worker_per_disk(worker_per_disk_)
    , min_depth_parallel(min_depth_parallel_)
    , max_depth_parallel(max_depth_parallel_)
{
}

void DiskCacheLRU::DiskIterator::exec(fs::path entry_path)
{
    String full_path = fs::path(disk->getPath()) / entry_path;
    struct stat obj_stat;
    if (stat(full_path.c_str(), &obj_stat) != 0)
        return;  // not exists

    if (!S_ISDIR(obj_stat.st_mode))
        throw Exception(fmt::format("disk path {} {} is not a directory", disk->getPath(), entry_path.string()), ErrorCodes::LOGICAL_ERROR);

    pool = worker_per_disk <= 1 ? nullptr : std::make_unique<ThreadPool>(worker_per_disk);
    iterateDirectory(entry_path / "", 0);
    if (pool)
        pool->wait();
    handler.throwIfException();
}

void DiskCacheLRU::DiskIterator::iterateDirectory(fs::path rel_path, size_t depth)
{
    auto iterate_dir_impl = [this, current_depth = depth] (fs::path current_rel_path) {
        for (auto iter = disk->iterateDirectory(current_rel_path); iter->isValid(); iter->next())
        {
            try
            {
                String abs_obj_path = fs::path(disk->getPath()) / current_rel_path / iter->name();
                struct stat obj_stat;
                if (stat(abs_obj_path.c_str(), &obj_stat) != 0)
                {
                    LOG_WARNING(log, fmt::format("Failed to stat file {}, error {}", abs_obj_path, errnoToString(errno)));
                    continue;
                }

                if (S_ISREG(obj_stat.st_mode))
                {
                    iterateFile(current_rel_path / iter->name(), obj_stat.st_size);
                }
                else if (S_ISDIR(obj_stat.st_mode))
                {
                    iterateDirectory(current_rel_path / iter->name() / "", current_depth + 1);
                }
            }
            catch (...)
            {
                tryLogCurrentException(log);
            }
        }

        // if (disk->isDirectoryEmpty(current_rel_path))
        //     disk->remove(current_rel_path); // rmdir
    };

    if (pool && static_cast<int>(depth) >= min_depth_parallel && static_cast<int>(depth) <= max_depth_parallel)
    {
        pool->scheduleOrThrowOnError(createExceptionHandledJob(
            [iterate_dir_impl, rel_path] {
                setThreadName("DCWorker");
                iterate_dir_impl(std::move(rel_path));
            },
            handler));
    }
    else
    {
        try
        {
            iterate_dir_impl(std::move(rel_path));
        }
        catch (...)
        {
            handler.setException(std::current_exception());
        }
    }
}

DiskCacheLRU::DiskCacheLoader::DiskCacheLoader(
    DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk_, int min_depth_parallel_, int max_depth_parallel_)
    : DiskIterator(cache_, std::move(disk_), worker_per_disk_, min_depth_parallel_, max_depth_parallel_)
{
}

DiskCacheLRU::DiskCacheLoader::~DiskCacheLoader()
{
    try
    {
        LOG_INFO(&Poco::Logger::get("DiskCacheLoader"), fmt::format("Loaded {} segs from disk {}", total_loaded, disk->getPath()));
    }
    catch (...) {}
}

void DiskCacheLRU::DiskCacheLoader::iterateFile(fs::path file_path, size_t file_size)
{
    bool is_temp_segment = endsWith(file_path, DISK_CACHE_TEMP_FILE_SUFFIX);
    String segment_hex = file_path.filename().string();

    /// remove temp prefix
    if (is_temp_segment)
    {
        segment_hex.resize(segment_hex.size() - TMP_SUFFIX_LEN);
    }

    /// Use little endian
    String hex_key = segment_hex + file_path.parent_path().filename().string();
    auto unhexed = unhexKey(hex_key);
    if (!unhexed)
    {
        LOG_ERROR(log, "Invalid disk cache file path {} with hex key {}", (fs::path(disk->getPath()) / file_path).string(), hex_key);
        disk->removeFileIfExists(file_path);
        return;
    }

    const KeyType & key = unhexed.value();

    if (is_temp_segment)
    {
        std::shared_ptr<DiskCacheMeta> meta = nullptr;
        {
            ProfileEvents::increment(ProfileEvents::DiskCacheGetTotalOps);
            Stopwatch watch;
            SCOPE_EXIT({ProfileEvents::increment(ProfileEvents::DiskCacheGetMetaMicroSeconds,
                watch.elapsedMicroseconds());});

            meta = disk_cache.containers.shard(key).get(key);
        }

        if (meta == nullptr)
        {
            // There is a temporary cache file and no corresponding entry in disk cache
            try
            {
                disk->removeFileIfExists(file_path);
            }
            catch(...)
            {
                tryLogCurrentException(log, fmt::format("Failed to remove temporary "
                    "cache file {}, disk path: {}, should be rare", file_path.string(),
                    disk->getPath()));
            }
        }
    }
    else
    {
        ProfileEvents::increment(ProfileEvents::DiskCacheSetTotalOps);
        if (disk_cache.containers.shard(key).emplace(key, std::make_shared<DiskCacheMeta>(DiskCacheMeta::State::Cached, disk, file_size)))
            ++total_loaded;
    }
}

DiskCacheLRU::DiskCacheMigrator::DiskCacheMigrator(
    DiskCacheLRU & cache_, DiskPtr disk_, size_t worker_per_disk_, int min_depth_parallel_, int max_depth_parallel_)
    : DiskIterator(cache_, std::move(disk_), worker_per_disk_, min_depth_parallel_, max_depth_parallel_)
{
}

DiskCacheLRU::DiskCacheMigrator::~DiskCacheMigrator()
{
    try
    {
        LOG_INFO(&Poco::Logger::get("DiskCacheMigrator"), fmt::format("Migrated {} segs from disk {}", total_migrated, disk->getPath()));
    }
    catch (...) {}
}

void DiskCacheLRU::DiskCacheMigrator::iterateFile(fs::path rel_path, size_t file_size)
{
    if (endsWith(rel_path, DISK_CACHE_TEMP_FILE_SUFFIX))
    {
        disk->removeFileIfExists(rel_path);
        return;
    }

    String seg_name;
    {
        WriteBufferFromString buf(seg_name);

        /// skip base path disk_cache/
        for (auto it = std::next(rel_path.begin()); it != rel_path.end(); ++it)
        {
            if (it->empty())
                continue;
            if (buf.count() > 0)
                writeChar('/', buf);

            writeString(it->string(), buf);
        }
    }

    auto key = hash(seg_name);
    auto& shard = disk_cache.containers.shard(key);
    // Insert cache meta first, if there is a entry already there, skip this insert

    auto target_path = disk_cache.getRelativePath(key);
    try
    {
        bool inserted = shard.emplace(key, std::make_shared<DiskCacheMeta>(DiskCacheMeta::State::Caching, nullptr, 0));
        if (inserted)
        {
            disk->createDirectories(target_path.parent_path());
            disk->moveFile(rel_path, target_path);
            shard.update(key, std::make_shared<DiskCacheMeta>(DiskCacheMeta::State::Cached, disk, file_size));
            ++total_migrated;
        }
        else
        {
            disk->removeFileIfExists(rel_path);
        }
    }
    catch (...)
    {
        disk->removeFileIfExists(rel_path);
        // disk->removeIfExists(target_path);
        shard.erase(key);
        throw;
    }
}
}
