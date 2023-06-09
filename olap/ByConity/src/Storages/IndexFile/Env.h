/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

#pragma once

#include <memory>
#include <string>
#include <vector>
#include <stdarg.h>
#include <stdint.h>
#include <IO/HashingWriteBuffer.h>
#include <Storages/IndexFile/RemoteFileCache.h>
#include <Storages/IndexFile/Status.h>

namespace DB
{
class Slice;
}

namespace DB::IndexFile
{
class RandomAccessFile;
class WritableFile;

// An Env is an interface used by the leveldb implementation to access
// operating system functionality like the filesystem etc.  Callers
// may wish to provide a custom Env object when opening a database to
// get fine gain control; e.g., to rate limit file system operations.
//
// All Env implementations are safe for concurrent access from
// multiple threads without any external synchronization.
class Env
{
public:
    Env() = default;

    Env(const Env &) = delete;
    Env & operator=(const Env &) = delete;

    virtual ~Env() { }

    // Return a default environment suitable for the current operating
    // system.  Sophisticated users may wish to provide their own Env
    // implementation instead of relying on this default environment.
    //
    // The result of Default() belongs to leveldb and must never be deleted.
    static Env * Default();

    // Create a brand new random access read-only file with the
    // specified name.  On success, stores a pointer to the new file in
    // *result and returns OK.  On failure stores nullptr in *result and
    // returns non-OK.  If the file does not exist, returns a non-OK
    // status.  Implementations should return a NotFound status when the file does
    // not exist.
    //
    // The returned file may be concurrently accessed by multiple threads.
    virtual Status NewRandomAccessFile(const std::string & fname, std::unique_ptr<RandomAccessFile> * result) = 0;

    // Create a brand new random access read-only remote file with the
    // local disk caches.  On success, stores a pointer to the new file in
    // *result and returns OK.  On failure stores nullptr in *result and
    // returns non-OK.
    //
    // When cache is nullptr, data is always read from remote file.
    // The returned file may be concurrently accessed by multiple threads.
    virtual Status NewRandomAccessRemoteFileWithCache(const RemoteFileInfo & file,
                                                      RemoteFileCachePtr cache,
                                                      std::unique_ptr<RandomAccessFile> * result) = 0;

    // Create an object that writes to a new file with the specified
    // name.  Deletes any existing file with the same name and creates a
    // new file.  On success, stores a pointer to the new file in
    // *result and returns OK.  On failure stores nullptr in *result and
    // returns non-OK.
    //
    // The returned file will only be accessed by one thread at a time.
    virtual Status NewWritableFile(const std::string & fname, std::unique_ptr<WritableFile> * result) = 0;

    // Returns true iff the named file exists.
    virtual bool FileExists(const std::string & fname) = 0;

    // Store the size of fname in *file_size.
    virtual Status GetFileSize(const std::string & fname, uint64_t * file_size) = 0;

    // Rename file src to target.
    virtual Status RenameFile(const std::string & src, const std::string & target) = 0;

    // Delete the named file.
    virtual Status DeleteFile(const std::string& fname) = 0;
};

// A file abstraction for randomly reading the contents of a file.
class RandomAccessFile
{
public:
    RandomAccessFile() = default;

    RandomAccessFile(const RandomAccessFile &) = delete;
    RandomAccessFile & operator=(const RandomAccessFile &) = delete;

    virtual ~RandomAccessFile() { }

    // Read up to "n" bytes from the file starting at "offset".
    // "scratch[0..n-1]" may be written by this routine.  Sets "*result"
    // to the data that was read (including if fewer than "n" bytes were
    // successfully read).  May set "*result" to point at data in
    // "scratch[0..n-1]", so "scratch[0..n-1]" must be live when
    // "*result" is used.  If an error was encountered, returns a non-OK
    // status.
    //
    // Safe for concurrent use by multiple threads.
    virtual Status Read(uint64_t offset, size_t n, Slice * result, char * scratch) const = 0;
};

// A file abstraction for sequential writing.  The implementation
// must provide buffering since callers may append small fragments
// at a time to the file.
class WritableFile
{
public:
    using uint128 = std::pair<uint64_t, uint64_t>;

    WritableFile() : hashing_file(DBMS_DEFAULT_HASHING_BLOCK_SIZE) {}

    WritableFile(const WritableFile &) = delete;
    WritableFile & operator=(const WritableFile &) = delete;

    virtual ~WritableFile() { }

    virtual Status Append(const Slice & data) = 0;
    virtual Status Close() = 0;
    virtual Status Flush() = 0;
    virtual Status Sync() = 0;

    void calculateHash(const char * p, size_t n) { hashing_file.calculateHash(const_cast<char *>(p), n); }

    uint128 getHash() { return hashing_file.getHash(); }

private:
    class HashingFile : public IHashingBuffer<DB::WriteBuffer>
    {
    public:
        HashingFile(size_t block_size_ = DBMS_DEFAULT_HASHING_BLOCK_SIZE) : IHashingBuffer<DB::WriteBuffer>(block_size_)
        {
            state = uint128(0, 0);
        }
    };

    HashingFile hashing_file;
};

}
