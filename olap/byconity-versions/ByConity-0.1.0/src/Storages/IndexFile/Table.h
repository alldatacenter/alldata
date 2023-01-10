/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

#pragma once

#include <cstdint>
#include <string>
#include <memory>

#include <Storages/IndexFile/Iterator.h>

namespace DB::IndexFile
{
class Block;
class BlockHandle;
class Footer;
struct Options;
class RandomAccessFile;
struct ReadOptions;

/// A Table is a sorted map from strings to strings.  Tables are
/// immutable and persistent.  A Table may be safely accessed from
/// multiple threads without external synchronization.
class Table
{
public:
    /// Attempt to open the table that is stored in bytes [0..file_size)
    /// of "file", and read the metadata entries necessary to allow
    /// retrieving data from the table.
    ///
    /// If successful, returns ok and sets "*table" to the newly opened
    /// table.  The client should delete "*table" when no longer needed.
    /// If there was an error while initializing the table, sets "*table"
    /// to nullptr and returns a non-ok status.
    static Status Open(const Options & options, std::unique_ptr<RandomAccessFile> && file, uint64_t file_size, std::unique_ptr<Table> * table);

    Table(const Table &) = delete;
    Table & operator=(const Table &) = delete;

    ~Table();

    /// Search `key' in this table.
    /// Return OK and set the corresponding value to *value when found.
    /// Return NotFound when the key is not present.
    /// Return other error status otherwise.
    Status Get(const ReadOptions &, const Slice & key, std::string * value);

    /// Total bytes of resident memory usage.
    /// This will exclude memory used by data in block cache.
    size_t ResidentMemoryUsage() const;

    // Returns a new iterator over the table contents.
    // The result of NewIterator() is initially invalid (caller must
    // call one of the Seek methods on the iterator before using it).
    Iterator * NewIterator(const ReadOptions &) const;

    // Given a key, return an approximate byte offset in the file where
    // the data for that key begins (or would begin if the key were
    // present in the file).  The returned value is in terms of file
    // bytes, and so includes effects like compression of the underlying data.
    // E.g., the approximate offset of the last key in the table will
    // be close to the file length.
    // uint64_t ApproximateOffsetOf(const Slice & key) const;

private:
    struct Rep;

    // Convert an index iterator value (i.e., an encoded BlockHandle)
    // into an iterator over the contents of the corresponding block.
    static Iterator * BlockReader(void * arg, const ReadOptions & options, const Slice & index_value);

    explicit Table(Rep * rep) : rep_(rep) { }

    void ReadMeta(const Footer & footer);
    void ReadFilter(const Slice & filter_handle_value);

    Rep * const rep_;
};

}
