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

#include <Storages/UniqueKeyIndex.h>
#include <Common/Coding.h>
#include <Common/Exception.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int UNKNOWN_EXCEPTION;
}

UniqueKeyIndex::UniqueKeyIndex(const String & file_path, UniqueKeyIndexBlockCachePtr block_cache)
{
    IndexFile::Options options;
    options.block_cache = std::move(block_cache);
    auto local_reader = std::make_unique<IndexFile::IndexFileReader>(options);
    auto status = local_reader->Open(file_path);
    if (!status.ok())
        throw Exception("Failed to open index file " + file_path + ": " + status.ToString(), ErrorCodes::UNKNOWN_EXCEPTION);
    index_reader = std::move(local_reader);
}

UniqueKeyIndex::UniqueKeyIndex(
    const IndexFile::RemoteFileInfo & remote_file, UniqueKeyIndexFileCachePtr file_cache, UniqueKeyIndexBlockCachePtr block_cache)
{
    IndexFile::Options options;
    options.remote_file_cache = std::move(file_cache);
    options.block_cache = std::move(block_cache);
    auto local_reader = std::make_unique<IndexFile::IndexFileReader>(options);
    auto status = local_reader->Open(remote_file);
    if (!status.ok())
        throw Exception("Failed to open index file " + remote_file.path + ": " + status.ToString(), ErrorCodes::UNKNOWN_EXCEPTION);
    index_reader = std::move(local_reader);
}

bool UniqueKeyIndex::lookup(const String & key, UInt32 & rowid)
{
    if (!index_reader)
        return false;

    String value;
    auto status = index_reader->Get(IndexFile::ReadOptions(), key, &value);
    if (status.ok())
    {
        Slice input(value);
        if (GetVarint32(&input, &rowid))
            return true;
        throw Exception("Failed to decode rowid", ErrorCodes::UNKNOWN_EXCEPTION);
    }
    else if (status.IsNotFound())
    {
        return false;
    }
    else
    {
        throw Exception("Failed to lookup key: " + status.ToString(), ErrorCodes::UNKNOWN_EXCEPTION);
    }
}

std::unique_ptr<IndexFile::Iterator> UniqueKeyIndex::new_iterator(const IndexFile::ReadOptions & options)
{
    if (!index_reader)
        return std::unique_ptr<IndexFile::Iterator>(IndexFile::NewEmptyIterator());
    std::unique_ptr<IndexFile::Iterator> res;
    auto st = index_reader->NewIterator(options, &res);
    if (!st.ok())
        throw Exception("Failed to get iterator: " + st.ToString(), ErrorCodes::UNKNOWN_EXCEPTION);
    return res;
}

size_t UniqueKeyIndex::residentMemoryUsage() const
{
    return index_reader ? index_reader->ResidentMemoryUsage() : sizeof(UniqueKeyIndex);
}

}
