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
#include <Storages/IndexFile/Cache.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>

namespace DB
{
class DeleteBitmapCache
{
public:
    explicit DeleteBitmapCache(size_t max_size_in_bytes);
    ~DeleteBitmapCache();

    /// insert ("version", "bitmap") into the cache using "key".
    void insert(const String & key, UInt64 version, ImmutableDeleteBitmapPtr bitmap);

    /// if found key, return true and set "out_version" and "out_bitmap".
    /// otherwise, return false.
    bool lookup(const String & key, UInt64 & out_version, ImmutableDeleteBitmapPtr & out_bitmap);

    /// if found key, erase it from cache
    void erase(const String & key);

    static String buildKey(UUID storage_uuid, const String & partition_id, Int64 min_block, Int64 max_block);

private:
    std::shared_ptr<IndexFile::Cache> cache;
};

using DeleteBitmapCachePtr = std::shared_ptr<DeleteBitmapCache>;

}
