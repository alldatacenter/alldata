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

#include <atomic>
#include <memory>
#include <shared_mutex>
#include <sstream>
#include <string>
#include <thread>
#include <type_traits>
#include <fcntl.h>
#include <Core/Types.h>
#include <boost/algorithm/string.hpp>
#include <Poco/Path.h>
#include <Poco/String.h>
#include <Poco/Timestamp.h>
#include <Poco/URI.h>
#include <Poco/Util/LayeredConfiguration.h>
#include <Common/Exception.h>
#include <Common/config.h>
#include "HDFSCommon.h"

namespace DB
{

namespace ErrorCodes
{
    extern const int PARAMETER_OUT_OF_BOUND;
}

class HDFSFileSystem
{
public:
   HDFSFileSystem(
        const HDFSConnectionParams & hdfs_params_, const int max_fd_num, const int skip_fd_num, const int io_error_num_to_reconnect);
    ~HDFSFileSystem() = default;
    HDFSFileSystem(const HDFSFileSystem &) = delete;
    HDFSFileSystem & operator=(const HDFSFileSystem &) = delete;

    int open(const std::string& path, int flags, mode_t mode);
    int close(const int fd);
    int flush(const int fd);
    ssize_t read(const int fd, void *buf, size_t count);
    ssize_t write(const int fd, const void *buf, size_t count);
    bool copyTo(const std::string& path, const std::string& rpath);
    bool moveTo(const std::string& path, const std::string& rpath);
    bool setSize(const std::string& path, uint64_t size);
    bool exists(const std::string& path) const;
    bool remove(const std::string& path, bool recursive = false) const;
    ssize_t getFileSize(const std::string& path) const;
    ssize_t getCapacity() const;
    void list(const std::string& path, std::vector<std::string>& files) const;
    int64_t getLastModifiedInSeconds(const std::string& path) const;
    bool renameTo(const std::string& path, const std::string& rpath) const;
    bool createFile(const std::string& path) const;
    bool createDirectory(const std::string& path) const;
    bool createDirectories(const std::string& path) const;
    bool isFile(const std::string& path) const;
    bool isDirectory(const std::string& path) const;
    bool setWriteable(const std::string& path, bool flag = true) const;
    bool canExecute(const std::string& path) const;
    bool setLastModifiedInSeconds(const std::string& path, uint64_t ts) const;

    HDFSFSPtr getFS() const;

    static inline String normalizePath(const String& path);

    inline hdfsFile getHDFSFileByFd(int fd) const
    {
        if (fd < SKIP_FD_NUM || fd >= MAX_FD_NUM + SKIP_FD_NUM)
        {
            throw Exception("Illegal HDFS fd", ErrorCodes::PARAMETER_OUT_OF_BOUND);
        }
        return fd_to_hdfs_file[fd];
    }

private:
    int getNextFd(const std::string & path);
    void reconnect() const;
    void reconnectIfNecessary() const;
    void handleError [[ noreturn ]] (const String & func) const;

    mutable std::shared_mutex hdfs_mutex;
    mutable HDFSFSPtr fs;

    HDFSConnectionParams hdfs_params;

    const int MAX_FD_NUM;
    const int SKIP_FD_NUM;
    mutable std::atomic<int> io_error_num = 0;
    const int io_error_num_to_reconnect;
    std::vector<hdfsFile> fd_to_hdfs_file;
    std::atomic_flag flag_ = ATOMIC_FLAG_INIT;
};

void registerDefaultHdfsFileSystem(
    const HDFSConnectionParams & hdfs_params, const int max_fd_num, const int skip_fd_num, const int io_error_num_to_reconnect);
DB::fs_ptr<DB::HDFSFileSystem> & getDefaultHdfsFileSystem();

namespace HDFSCommon
{

// here is the function used by ClickHouse
int open(const char *pathname, int flags, mode_t mode);
int close(int fd);
// off_t lseek(int fd, off_t offset, int whence);
int fsync(int fd);
ssize_t read(const int fd, void *buf, size_t count);
int write(int fd, const void* buf, size_t count);
bool exists(const std::string& path);
bool remove(const std::string& path, bool recursive = false);
int fcntl(int fd, int cmd, ... /* arg */ );
ssize_t getSize(const std::string& path);
ssize_t getCapacity();
bool renameTo(const std::string& path, const std::string& rpath);
bool copyTo(const std::string& path, const std::string& rpath);
bool moveTo(const std::string& path, const std::string& rpath);
bool createFile(const std::string& path);
bool createDirectory(const std::string& path);
bool createDirectories(const std::string& path);
Poco::Timestamp getLastModified(const std::string& path);
void setLastModified(const std::string& path, const Poco::Timestamp& ts);
void list(const std::string& path, std::vector<std::string>& files);
bool isFile(const std::string& path);
bool isDirectory(const std::string& path);
bool canExecute(const std::string& path);
void setReadOnly(const std::string& path);
void setWritable(const std::string& path);
void setSize(const std::string& path, uint64_t size);

// This class is for File abstraction.
class File {
public:
    File(const char* path) : path_(path)
    {
    }

    File(const std::string& path) : path_(path)
    {
    }

    File(const Poco::Path& path) : path_(path.toString())
    {
    }

    File() = default;
    ~File() = default;

    File& operator = (const Poco::Path& rpath) {
        path_ = rpath.toString();
        std::string::size_type n = path_.size();
        if (n > 1 && path_[n - 1] == '/')
            path_.resize(n - 1);
        return *this;
    }

    const std::string& getPath() const
    {
        return path_;
    }

    const std::string& path() const
    {
        return path_;
    }

    bool isFile() const
    {
        return HDFSCommon::isFile(path_);
    }

    bool canExecute() const
    {
        Poco::Path p(path_);
        return Poco::icompare(p.getExtension(), "exe") == 0;
    }

    File& setReadOnly()
    {
        HDFSCommon::setReadOnly(path_);
        return *this;
    }

    File& setWriteable()
    {
        HDFSCommon::setWritable(path_);
        return *this;
    }

    File& setExecutable(bool flag = true)
    {
        (void) flag;
        return *this;
    }

    bool isDirectory() const
    {
        return HDFSCommon::isDirectory(path_);
    }

    bool exists() const
    {
        return HDFSCommon::exists(path_);
    }

    bool remove(bool recursive = false)
    {
        return HDFSCommon::remove(path_, recursive);
    }

    uint64_t getSize() const
    {
        return HDFSCommon::getSize(path_);
    }


    void renameTo(const std::string& rpath)
    {
        HDFSCommon::renameTo(path_, rpath);
    }

    void copyTo(const std::string& rpath)
    {
        HDFSCommon::copyTo(path_, rpath);
    }

    bool createFile()
    {
        return HDFSCommon::createFile(path_);
    }

    bool createDirectory()
    {
        return HDFSCommon::createDirectory(path_);
    }

    void createDirectories()
    {
        HDFSCommon::createDirectories(path_);
    }

    Poco::Timestamp getLastModified()
    {
        return HDFSCommon::getLastModified(path_);
    }

    File& setLastModified(const Poco::Timestamp& ts)
    {
        HDFSCommon::setLastModified(path_, ts);
        return *this;
    }

    void list(std::vector<std::string>& files) const
    {
        files.clear();
        return HDFSCommon::list(path_, files);
    }

    void list(std::vector<HDFSCommon::File>& commonfiles) const
    {
        commonfiles.clear();
        std::vector<std::string> filenames;
        list(filenames);
        for (auto f : filenames)
        {
            commonfiles.push_back(HDFSCommon::File(f));
        }
    }

    void setSize(uint64_t size)
    {
        HDFSCommon::setSize(path_, size);
    }

private:
     std::string path_;
};

class DirectoryIterator
{
public:
    /// Creates the end iterator.
    DirectoryIterator();

    /// Creates a directory iterator for the given path.
    DirectoryIterator(const std::string &path);

    /// Creates a directory iterator for the given Common::File.
    DirectoryIterator(const HDFSCommon::File &file);

    /// Creates a directory iterator for the given path.
    DirectoryIterator(const Poco::Path &path);

    /// Destroys the DirectoryIterator.
    ~DirectoryIterator();

    /// Returns the current Common::Filename.
    const std::string &name() const;

    /// Returns the current path.
    const Poco::Path &path() const;

    const HDFSCommon::File &operator*() const;
    HDFSCommon::File &operator*();

    const HDFSCommon::File *operator->() const;
    HDFSCommon::File *operator->();

    bool operator==(const DirectoryIterator &iterator) const;
    bool operator!=(const DirectoryIterator &iterator) const;

    DirectoryIterator &operator ++();

    const std::string& Next();
    bool hasNext() const;

private:
    void OpenDir();
    void CloseDir();

    const std::string& GetCurrent() const;

protected:
    Poco::Path dir_path;
    HDFSCommon::File file;
    std::string current;

    std::vector<std::string> file_names;
    uint32_t current_idx;
};

inline const std::string &DirectoryIterator::name() const {
    return dir_path.getFileName();
}

inline const Poco::Path &DirectoryIterator::path() const {
    return dir_path;
}

inline const HDFSCommon::File &DirectoryIterator::operator*() const {
    return file;
}

inline HDFSCommon::File &DirectoryIterator::operator*() {
    return file;
}

inline const HDFSCommon::File *DirectoryIterator::operator->() const {
    return &file;
}

inline HDFSCommon::File *DirectoryIterator::operator->() {
    return &file;
}

inline bool DirectoryIterator::operator==(const DirectoryIterator &iterator) const {
    return name() == iterator.name();
}

inline bool DirectoryIterator::operator!=(const DirectoryIterator &iterator) const {
    return name() != iterator.name();
}

}


}
