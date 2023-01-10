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

#include <Storages/CnchPartitionInfo.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int METASTORE_EXCEPTION;
}

namespace Catalog
{

using DataPartPtr = std::shared_ptr<const MergeTreeDataPartCNCH>;
using DataPartsVector = std::vector<DataPartPtr>;

struct CommitItems
{
    DataPartsVector data_parts;
    DeleteBitmapMetaPtrVector delete_bitmaps;
    DataPartsVector staged_parts;

    bool empty() const
    {
        return data_parts.empty() && delete_bitmaps.empty() && staged_parts.empty();
    }
};

/// keep partitions sorted as bytekv manner;
struct partition_comparator
{
    bool operator() (const String & a, const String & b) const
    {
        String a_ = a + "_";
        String b_ = b + "_";
        return a_ < b_;
    }
};

using PartitionMap = std::map<String, PartitionInfoPtr, partition_comparator>;

inline String normalizePath(const String & path)
{
    if (path.empty()) return "";
    /// normalize directory format
    String normalized_path;
    /// change all ////// to /
    std::for_each(path.begin(), path.end(), [&normalized_path](char c)
    {
        if (c == '/' && !normalized_path.empty() && normalized_path.back() == '/')
            return;
        normalized_path.push_back(c);
    });
    /// remove trailing /
    if (normalized_path.size() > 1 && normalized_path.back() == '/')
        normalized_path.pop_back();
    return normalized_path;
}

inline String getNextKey(const String & start_key)
{
    String next_key = start_key;
    bool success = false;
    for (auto it = next_key.rbegin(); it != next_key.rend(); ++it)
    {
        if (reinterpret_cast<unsigned char&>(*it) < 0xFF)
        {
            (*it)++;
            success = true;
            break;
        }
        *it = 0;
    }
    if (unlikely(!success))
            throw Exception("Failed to get end key for " + start_key, ErrorCodes::METASTORE_EXCEPTION);
    return next_key;
}

struct BatchedCommitIndex
{
    size_t parts_begin;
    size_t parts_end;
    size_t bitmap_begin;
    size_t bitmap_end;
    size_t staged_begin;
    size_t staged_end;
    size_t expected_parts_begin;
    size_t expected_parts_end;
    size_t expected_bitmap_begin;
    size_t expected_bitmap_end;
    size_t expected_staged_begin;
    size_t expected_staged_end;
};

}
}
