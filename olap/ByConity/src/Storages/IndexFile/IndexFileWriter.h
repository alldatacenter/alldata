// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Core/Types.h>
#include <Storages/IndexFile/Options.h>
#include <Storages/IndexFile/Status.h>
#include <Common/Slice.h>

#include <memory>

namespace DB::IndexFile
{
struct IndexFileInfo
{
    using uint128 = std::pair<uint64_t, uint64_t>;
    String file_path;
    String smallest_key;        /// smallest user key in file
    String largest_key;         /// largest user key in file
    uint64_t file_size = 0;     /// file size in bytes
    uint128 file_hash{0, 0};    /// file hash
    uint64_t num_entries = 0;   /// number of entries in file
};

class IndexFileWriter
{
public:
    explicit IndexFileWriter(const Options & options);
    ~IndexFileWriter();

    /// Prepare IndexFileWriter to write into file located at "file_path".
    Status Open(const String & file_path);

    /// Add a key with value to currently opened file
    /// REQUIRES: key is after any previously added key according to comparator.
    Status Add(const Slice & key, const Slice & value);

    /// Finalize writing to index file and close file.
    /// An optional IndexFileInfo pointer can be passed to the function
    /// which will be populated with information about the created sst file.
    Status Finish(IndexFileInfo * file_info = nullptr);

private:
    struct Rep;
    std::unique_ptr<Rep> rep;
};

}
