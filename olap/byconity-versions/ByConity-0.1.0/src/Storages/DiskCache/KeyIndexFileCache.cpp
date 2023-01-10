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

#include <Storages/DiskCache/KeyIndexFileCache.h>

#include <IO/LimitReadBuffer.h>
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromFile.h>
#include <IO/WriteBufferFromString.h>
#include <IO/copyData.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Poco/DirectoryIterator.h>
#include <Poco/File.h>
#include <Common/LRUCache.h>
#include <Common/Stopwatch.h>
#include <Common/escapeForFileName.h>

#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>


namespace DB
{
namespace
{
    String buildCacheFileName(const String & key, UInt64 file_size)
    {
        auto escaped_key = escapeForFileName(key);
        WriteBufferFromOwnString buf;
        writeIntText(file_size, buf);
        writeChar('_', buf);
        writeString(escaped_key, buf);
        writeString(".idx", buf);
        return buf.str();
    }

    bool tryParseCacheFileName(const String & file_name, String & key, UInt64 & file_size)
    {
        if (!endsWith(file_name, ".idx"))
            return false;
        String escaped_key;
        ReadBufferFromString buf(file_name);
        readIntText(file_size, buf);
        assertChar('_', buf);
        readStringUntilEOF(escaped_key, buf);
        escaped_key.resize(escaped_key.size() - 4); /// remove ".idx" tail
        key = unescapeForFileName(escaped_key);
        return true;
    }

    enum class CacheState
    {
        Caching,
        Cached,
    };

    struct CacheValue
    {
        CacheValue(CacheState state_, size_t file_size_) : state(state_), file_size(file_size_) { }

        static std::shared_ptr<CacheValue> Caching(size_t file_size)
        {
            return std::make_shared<CacheValue>(CacheState::Caching, file_size);
        }

        static std::shared_ptr<CacheValue> Cached(size_t file_size) { return std::make_shared<CacheValue>(CacheState::Cached, file_size); }

        CacheState state;
        size_t file_size;
    };

    struct FileSizeAsWeightFunction
    {
        size_t operator()(const CacheValue & value) const { return value.file_size; }
    };

    class InnerCache : public LRUCache<String, CacheValue, std::hash<String>, FileSizeAsWeightFunction>
    {
    private:
        using Base = LRUCache<String, CacheValue, std::hash<String>, FileSizeAsWeightFunction>;

    public:
        InnerCache(size_t max_size_, String & base_path_) : Base(max_size_), base_path(base_path_) { }
        using Base::remove;

    private:
        /// will only get called when key is removed due to eviction
        void removeExternal(const Key & key, const std::shared_ptr<CacheValue> &, size_t weight) override
        {
            auto filename = buildCacheFileName(key, weight);
            String path = base_path + filename;
            // TODO note that cache mutex is hold while calling this function,
            // we may want to remove the file in async way
            if (!Poco::File(path).exists())
                return;
            /// note that the file will remain in existence until the last fd referring to it is closed
            Poco::File(path).remove();
        }
        const String & base_path;
    };
} // namespace

struct KeyIndexFileCache::Rep
{
    Rep(Context & context_, size_t max_size)
        : context(context_)
        , base_path(context.getPath() + "unique_key_index_cache/")
        , loading_thread_pool(context.getLocalDiskCacheThreadPool())
        , cache(max_size, base_path)
    {
    }

    Context & context;
    String base_path;
    ThreadPool & loading_thread_pool; // TODO use a separate thread pool for index loading
    InnerCache cache;
};

KeyIndexFileCache::KeyIndexFileCache(Context & context, UInt64 max_size)
    : log(&Poco::Logger::get("KeyIndexFileCache")), rep(std::make_shared<Rep>(context, max_size))
{
    initCacheFromFileSystem();
}

KeyIndexFileCache::~KeyIndexFileCache() = default;

void KeyIndexFileCache::initCacheFromFileSystem()
{
    LOG_INFO(log, "Initializing KeyIndexFileCache...");
    Poco::File(rep->base_path).createDirectories();

    Stopwatch timer;
    int num_loaded = 0;
    Poco::DirectoryIterator end;
    for (auto it = Poco::DirectoryIterator(rep->base_path); it != end; ++it)
    {
        auto & filename = it.name();
        if (startsWith(filename, "TMP_"))
        {
            LOG_TRACE(log, "Removing temporary file {}", it->path());
            Poco::File(it->path()).remove();
        }
        else
        {
            String key;
            UInt64 size;
            if (tryParseCacheFileName(filename, key, size))
            {
                rep->cache.set(key, CacheValue::Cached(size));
                num_loaded++;
            }
            else
            {
                LOG_WARNING(log, "Unrecognized name for unique key index cache file: {}", filename);
            }
        }
    }
    LOG_INFO(log, "Initialized KeyIndexFileCache with {} entries in {} ms", num_loaded, timer.elapsedMilliseconds());
}

int KeyIndexFileCache::get(const IndexFile::RemoteFileInfo & file)
{
    auto [value, inserted] = rep->cache.getOrSet(file.cache_key, [&file]() { return CacheValue::Caching(file.size); });

    if (value->state == CacheState::Caching)
    {
        if (inserted)
        {
            std::weak_ptr<Rep> rep_wp = rep;
            /// start a bg task to cache the file to local disk
            auto cache_file_task = [rep_wp, file]() {
                std::shared_ptr<Rep> rep_inner = rep_wp.lock();
                if (!rep_inner)
                {
                    LOG_WARNING(&Poco::Logger::get("KeyIndexFileCache"), "KeyIndexFileCache has been destory.");
                    return;
                }
                try
                {
                    Context & context = rep_inner->context;
                    ReadBufferFromByteHDFS remote_buf(
                        file.path,
                        true,
                        context.getHdfsConnectionParams(),
                        DBMS_DEFAULT_BUFFER_SIZE,
                        nullptr,
                        0,
                        false,
                        context.getDiskCacheThrottler());
                    remote_buf.seek(file.start_offset);
                    LimitReadBuffer from(remote_buf, file.size, false);

                    /// download into tmp file
                    String tmp_file = rep_inner->base_path + "TMP_" + escapeForFileName(file.cache_key);
                    WriteBufferFromFile to(tmp_file);
                    copyData(from, to);
                    // TODO do we need to fsync the file?
                    to.close();

                    /// rename to cache file
                    String dst_path = rep_inner->base_path + buildCacheFileName(file.cache_key, file.size);
                    Poco::File(tmp_file).renameTo(dst_path);

                    rep_inner->cache.set(file.cache_key, CacheValue::Cached(file.size));
                }
                catch (...)
                {
                    LOG_ERROR(
                        &Poco::Logger::get("KeyIndexFileCache"),
                        "Failed to cache {} to local disks: {}",
                        file.path,
                        getCurrentExceptionMessage(false));
                    rep_inner->cache.remove(file.cache_key);
                }
            };
            rep->loading_thread_pool.scheduleOrThrowOnError(cache_file_task);
        }
        return -1; /// file is not in local disk cache, can't open directly
    }
    assert(value->state == CacheState::Cached);
    auto filename = cacheFileName(file);
    auto local_path = rep->base_path + filename;
    int fd = ::open(local_path.c_str(), O_RDONLY);
    if (fd < 0)
    {
        LOG_ERROR(log, "Failed to open {}: errno={}, errmsg={}", filename, errno, strerror(errno));
    }
    return fd;
}

void KeyIndexFileCache::release(int fd)
{
    // TODO cache open fd and delay close
    if (int res = ::close(fd); res < 0)
        LOG_ERROR(log, "Failed to close fd {}, errno={}, errmsg={}", fd, errno, strerror(errno));
}

String KeyIndexFileCache::cacheFileName(const IndexFile::RemoteFileInfo & file) const
{
    return buildCacheFileName(file.cache_key, file.size);
}

} // namespace DB
