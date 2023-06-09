// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <functional>
#include <memory>
#include <stddef.h>

#include <Storages/IndexFile/Cache.h>
#include <Storages/IndexFile/Comparator.h>
#include <Storages/IndexFile/Env.h>
#include <Storages/IndexFile/RemoteFileCache.h>

namespace DB::IndexFile
{
class FilterPolicy;

// DB contents are stored in a set of blocks, each of which holds a
// sequence of key,value pairs.  Each block may be compressed before
// being stored in a file.  The following enum describes which
// compression method (if any) is used to compress a block.
enum CompressionType
{
    // NOTE: do not change the values of existing entries, as these are
    // part of the persistent format on disk.
    kNoCompression = 0x0,
    kSnappyCompression = 0x1
};

struct Options
{
    // -------------------
    // Parameters that affect behavior

    // Comparator used to define the order of keys in the table.
    // Default: a comparator that uses lexicographic byte-wise ordering
    //
    // REQUIRES: The client must ensure that the comparator supplied
    // here has the same name and orders keys *exactly* the same as the
    // comparator provided to previous open calls on the same DB.
    const Comparator * comparator = BytewiseComparator();

    // If true, the implementation will do aggressive checking of the
    // data it is processing and will stop early if it detects any
    // errors.  This may have unforeseen ramifications: for example, a
    // corruption of one DB entry may cause a large number of entries to
    // become unreadable or for the entire DB to become unopenable.
    // Default: false
    bool paranoid_checks = false;

    // Use the specified object to interact with the environment,
    // e.g. to read/write files, schedule background work, etc.
    // Default: Env::Default()
    Env * env = Env::Default();

    // -------------------
    // Parameters that affect performance

    // Control over files.
    // If non-null, use the specified cache to cache remote file in local disks.
    RemoteFileCachePtr remote_file_cache = nullptr;

    // Control over blocks (user data is stored in a set of blocks, and
    // a block is the unit of reading from disk).
    // If non-null, use the specified cache for blocks.
    std::shared_ptr<Cache> block_cache = nullptr;

    // Approximate size of user data packed per block.  Note that the
    // block size specified here corresponds to uncompressed data.  The
    // actual size of the unit read from disk may be smaller if
    // compression is enabled.  This parameter can be changed dynamically.
    //
    // Default: 4K
    size_t block_size = 4 * 1024;

    // Number of keys between restart points for delta encoding of keys.
    // This parameter can be changed dynamically.  Most clients should
    // leave this parameter alone.
    //
    // Default: 16
    int block_restart_interval = 16;

    // Compress blocks using the specified compression algorithm.  This
    // parameter can be changed dynamically.
    //
    // Default: kSnappyCompression, which gives lightweight but fast
    // compression.
    //
    // Typical speeds of kSnappyCompression on an Intel(R) Core(TM)2 2.4GHz:
    //    ~200-500MB/s compression
    //    ~400-800MB/s decompression
    // Note that these speeds are significantly faster than most
    // persistent storage speeds, and therefore it is typically never
    // worth switching to kNoCompression.  Even if the input data is
    // incompressible, the kSnappyCompression implementation will
    // efficiently detect that and will switch to uncompressed mode.
    CompressionType compression = kSnappyCompression;

    // If non-null, use the specified filter policy to reduce disk reads.
    // Many applications will benefit from passing the result of
    // NewBloomFilterPolicy() here.
    //
    // Default: nullptr
    std::shared_ptr<const FilterPolicy> filter_policy = nullptr;
};

using Predicate = std::function<bool(const Slice & key, const Slice & val)>;

// Options that control read operations
struct ReadOptions
{
    // If true, all data read from underlying storage will be
    // verified against corresponding checksums.
    // Default: false
    bool verify_checksums = false;

    // Should the data read for this iteration be cached in memory?
    // Callers may wish to set this field to false for bulk scans.
    // Default: true
    bool fill_cache = true;

    // If not empty, all the KVs not satisfying the predicate will be skipped in this iteration.
    // Default: empty
    Predicate select_predicate;
};

}
