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

#include <assert.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <Storages/MergeTree/DeleteBitmapCache.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

namespace
{
    struct CachedBitmap
    {
        CachedBitmap(UInt64 version_, ImmutableDeleteBitmapPtr bitmap_) : version(version_), bitmap(std::move(bitmap_)) { }
        UInt64 version;
        ImmutableDeleteBitmapPtr bitmap;
    };

    void DeleteCachedBitmap([[maybe_unused]] const Slice & key, void * value)
    {
        auto * cached_bitmap = reinterpret_cast<CachedBitmap *>(value);
        delete cached_bitmap;
    }
} /// anonymous namespace

DeleteBitmapCache::DeleteBitmapCache(size_t max_size_in_bytes) : cache(IndexFile::NewLRUCache(max_size_in_bytes))
{
}

DeleteBitmapCache::~DeleteBitmapCache() = default;

void DeleteBitmapCache::insert(const String & key, UInt64 version, ImmutableDeleteBitmapPtr bitmap)
{
    if (bitmap == nullptr)
        throw Exception("bitmap is null", ErrorCodes::BAD_ARGUMENTS);

    /// TODO improve accuracy of memory usage estimation
    /// Currently we estimate the memory usage of the bitmap to be 2 bytes per elements,
    /// which may be underestimated for sparse bitmap and overestimated for dense bitmap.
    size_t charge = sizeof(CachedBitmap) + 2 * (bitmap->cardinality());
    CachedBitmap * value = new CachedBitmap(version, std::move(bitmap));
    auto * handle = cache->Insert(key, value, charge, &DeleteCachedBitmap);
    if (handle)
        cache->Release(handle);
    else
        delete value; /// insert failed
}

bool DeleteBitmapCache::lookup(const String & key, UInt64 & out_version, ImmutableDeleteBitmapPtr & out_bitmap)
{
    auto * handle = cache->Lookup(key);
    if (!handle)
    {
        // ProfileEvents::increment(ProfileEvents::DeleteBitmapCacheMiss);
        return false;
    }

    // ProfileEvents::increment(ProfileEvents::DeleteBitmapCacheHit);
    auto * value = reinterpret_cast<CachedBitmap *>(cache->Value(handle));
    out_version = value->version;
    out_bitmap = value->bitmap;
    cache->Release(handle);
    return true;
}

void DeleteBitmapCache::erase(const String & key)
{
    cache->Erase(key);
}

String DeleteBitmapCache::buildKey(UUID storage_uuid, const String & partition_id, Int64 min_block, Int64 max_block)
{
    WriteBufferFromOwnString buf;
    writeBinary(storage_uuid, buf);
    writeString(partition_id, buf);
    writeBinary(min_block, buf);
    writeBinary(max_block, buf);
    return std::move(buf.str());
}

}
