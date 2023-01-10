// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/IndexFile/Env.h>
#include <Common/Exception.h>
#include <Common/Slice.h>

#include <assert.h>
#include <limits>
#include <memory>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/types.h>

namespace DB::ErrorCodes
{
extern const int LOGICAL_ERROR;
}

namespace DB::IndexFile
{
namespace
{
    static const size_t kBufSize = 65536;

    static Status PosixError(const std::string & context, int err_number)
    {
        if (err_number == ENOENT)
        {
            return Status::NotFound(context, strerror(err_number));
        }
        else
        {
            return Status::IOError(context, strerror(err_number));
        }
    }

    // pread() based random-access
    class PosixRandomAccessFile : public RandomAccessFile
    {
    private:
        std::string filename_;
        int fd_;

    public:
        PosixRandomAccessFile(const std::string & fname, int fd) : filename_(fname), fd_(fd) { }

        virtual ~PosixRandomAccessFile() override { close(fd_); }

        virtual Status Read(uint64_t offset, size_t n, Slice * result, char * scratch) const override
        {
            int fd = fd_;
            Status s;
            ssize_t r = pread(fd, scratch, n, static_cast<off_t>(offset));
            *result = Slice(scratch, (r < 0) ? 0 : r);
            if (r < 0)
            {
                // An error: return a non-ok status
                s = PosixError(filename_, errno);
            }
            return s;
        }
    };

    class RemoteFileWithCache : public RandomAccessFile
    {
    public:
        RemoteFileWithCache(RemoteFileInfo file_, RemoteFileCachePtr cache_) : file(std::move(file_)), cache(std::move(cache_)) {}

        virtual Status Read(uint64_t offset, size_t n, Slice * result, char * scratch) const override
        {
            Status s;
            int fd = -1;
            if (cache)
                fd = cache->get(file);
            if (fd < 0)
            {
                /// fallback to remote read
                try
                {
                    ReadBufferFromByteHDFS buffer(
                        file.path,
                        /*pread=*/true,
                        file.hdfs_params,
                        /*buf_size=*/n,
                        /*existing_memory=*/scratch,
                        /*alignment=*/0,
                        /*read_all_once=*/true);

                    auto seek_off = static_cast<off_t>(file.start_offset + offset);
                    auto res_off = buffer.seek(seek_off);
                    if (res_off != seek_off)
                        throw Exception(
                            "Seek to " + file.path + " should return " + toString(seek_off) + " but got " + toString(res_off),
                            ErrorCodes::LOGICAL_ERROR);

                    auto is_eof = buffer.eof(); /// will trigger reading into scratch
                    if (is_eof)
                        throw Exception(
                            "Unexpected EOF when reading " + file.path + ", off=" + toString(seek_off) + ", size=" + toString(n),
                            ErrorCodes::LOGICAL_ERROR);
                    assert(buffer.buffer().size() == n);

                    *result = Slice(scratch, n);
                }
                catch (...)
                {
                    *result = Slice(scratch, 0);
                    s = Status::IOError(file.path, getCurrentExceptionMessage(false));
                }
            }
            else
            {
                /// read from local cached file
                ssize_t r = pread(fd, scratch, n, static_cast<off_t>(offset));
                *result = Slice(scratch, (r < 0) ? 0 : r);
                if (r < 0)
                {
                    /// An error: return a non-ok status
                    s = PosixError(cache->cacheFileName(file), errno);
                }
                cache->release(fd);
            }
            return s;
        }

    private:
        RemoteFileInfo file;
        RemoteFileCachePtr cache;
    };

    class PosixWritableFile : public WritableFile
    {
    private:
        // buf_[0, pos_-1] contains data to be written to fd_.
        std::string filename_;
        int fd_;
        char buf_[kBufSize];
        size_t pos_;

    public:
        PosixWritableFile(const std::string & fname, int fd) : WritableFile(), filename_(fname), fd_(fd), pos_(0) {}

        ~PosixWritableFile() override
        {
            if (fd_ >= 0)
            {
                // Ignoring any potential errors
                Close();
            }
        }

        virtual Status Append(const Slice & data) override
        {
            size_t n = data.size();
            const char * p = data.data();

            // Fit as much as possible into buffer.
            size_t copy = std::min(n, kBufSize - pos_);
            memcpy(buf_ + pos_, p, copy);
            p += copy;
            n -= copy;
            pos_ += copy;
            if (n == 0)
            {
                return Status::OK();
            }

            // Can't fit in buffer, so need to do at least one write.
            Status s = FlushBuffered();
            if (!s.ok())
            {
                return s;
            }

            // Small writes go to buffer, large writes are written directly.
            if (n < kBufSize)
            {
                memcpy(buf_, p, n);
                pos_ = n;
                return Status::OK();
            }
            return WriteRaw(p, n);
        }

        virtual Status Close() override
        {
            Status result = FlushBuffered();
            const int r = close(fd_);
            if (r < 0 && result.ok())
            {
                result = PosixError(filename_, errno);
            }
            fd_ = -1;
            return result;
        }

        virtual Status Flush() override { return FlushBuffered(); }

        Status SyncDirIfManifest()
        {
            const char * f = filename_.c_str();
            const char * sep = strrchr(f, '/');
            Slice basename;
            std::string dir;
            if (sep == nullptr)
            {
                dir = ".";
                basename = f;
            }
            else
            {
                dir = std::string(f, sep - f);
                basename = sep + 1;
            }
            Status s;
            if (basename.starts_with("MANIFEST"))
            {
                int fd = open(dir.c_str(), O_RDONLY);
                if (fd < 0)
                {
                    s = PosixError(dir, errno);
                }
                else
                {
                    if (fsync(fd) < 0)
                    {
                        s = PosixError(dir, errno);
                    }
                    close(fd);
                }
            }
            return s;
        }

        virtual Status Sync() override
        {
            // Ensure new files referred to by the manifest are in the filesystem.
            Status s = SyncDirIfManifest();
            if (!s.ok())
            {
                return s;
            }
            s = FlushBuffered();
#if defined(OS_LINUX)
            if (s.ok())
            {
                if (fdatasync(fd_) != 0)
                {
                    s = PosixError(filename_, errno);
                }
            }
#endif
            return s;
        }

    private:
        Status FlushBuffered()
        {
            Status s = WriteRaw(buf_, pos_);
            pos_ = 0;
            return s;
        }

        Status WriteRaw(const char * p, size_t n)
        {
            calculateHash(p, n);
            while (n > 0)
            {
                ssize_t r = write(fd_, p, n);
                if (r < 0)
                {
                    if (errno == EINTR)
                    {
                        continue; // Retry
                    }
                    return PosixError(filename_, errno);
                }
                p += r;
                n -= r;
            }
            return Status::OK();
        }
    };

    class PosixEnv : public Env
    {
    public:
        PosixEnv() = default;
        virtual ~PosixEnv() override = default;

        virtual Status NewRandomAccessFile(const std::string & fname, std::unique_ptr<RandomAccessFile> * result) override
        {
            Status s;
            int fd = ::open(fname.c_str(), O_RDONLY);
            if (fd < 0)
            {
                *result = nullptr;
                s = PosixError(fname, errno);
            }
            {
                *result = std::make_unique<PosixRandomAccessFile>(fname, fd);
            }
            return s;
        }

        virtual Status NewRandomAccessRemoteFileWithCache(
            const RemoteFileInfo & file, RemoteFileCachePtr cache, std::unique_ptr<RandomAccessFile> * result) override
        {
            *result = std::make_unique<RemoteFileWithCache>(file, cache);
            return Status::OK();
        }

        virtual Status NewWritableFile(const std::string & fname, std::unique_ptr<WritableFile> * result) override
        {
            Status s;
            int fd = ::open(fname.c_str(), O_TRUNC | O_WRONLY | O_CREAT, 0644);
            if (fd < 0)
            {
                *result = nullptr;
                s = PosixError(fname, errno);
            }
            else
            {
                *result = std::make_unique<PosixWritableFile>(fname, fd);
            }
            return s;
        }

        virtual bool FileExists(const std::string & fname) override { return ::access(fname.c_str(), F_OK) == 0; }

        virtual Status GetFileSize(const std::string & fname, uint64_t * size) override
        {
            Status s;
            struct ::stat sbuf;
            if (::stat(fname.c_str(), &sbuf) != 0)
            {
                *size = 0;
                s = PosixError(fname, errno);
            }
            else
            {
                *size = sbuf.st_size;
            }
            return s;
        }

        virtual Status RenameFile(const std::string & src, const std::string & target) override
        {
            Status result;
            if (::rename(src.c_str(), target.c_str()) != 0)
            {
                result = PosixError(src, errno);
            }
            return result;
        }

        virtual Status DeleteFile(const std::string & filename) override
        {
            if (::unlink(filename.c_str()) != 0)
            {
                return PosixError(filename, errno);
            }
            return Status::OK();
        }
    };

} // namespace

Env * Env::Default()
{
    static PosixEnv env;
    return &env;
}

}
