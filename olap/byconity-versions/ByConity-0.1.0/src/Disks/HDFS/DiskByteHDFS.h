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

#include <limits>
#include <Disks/IDisk.h>
#include <Disks/DiskType.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/HDFS/HDFSFileSystem.h>

namespace DB
{

class DiskByteHDFSReservation;

class DiskByteHDFS final : public IDisk
{
public:
    DiskByteHDFS(const String& disk_name_, const String& hdfs_base_path_,
        const HDFSConnectionParams& hdfs_params_);

    virtual const String& getName() const override;

    virtual ReservationPtr reserve(UInt64 bytes) override;

    virtual UInt64 getID() const override;

    virtual const String& getPath() const override { return disk_path; }

    virtual UInt64 getTotalSpace() const override { return std::numeric_limits<UInt64>::max(); }

    virtual UInt64 getAvailableSpace() const override { return std::numeric_limits<UInt64>::max(); }

    virtual UInt64 getUnreservedSpace() const override { return std::numeric_limits<UInt64>::max(); }

    virtual bool exists(const String& path) const override;

    virtual bool isFile(const String& path) const override;

    virtual bool isDirectory(const String & path) const override;

    virtual size_t getFileSize(const String & path) const override;

    virtual void createDirectory(const String & path) override;

    virtual void createDirectories(const String & path) override;

    virtual void clearDirectory(const String & path) override;

    virtual void moveDirectory(const String& from_path, const String& to_path) override;

    virtual DiskDirectoryIteratorPtr iterateDirectory(const String& path) override;

    virtual void createFile(const String& path) override;

    virtual void moveFile(const String& from_path, const String& to_path) override;

    virtual void replaceFile(const String& from_path, const String& to_path) override;

    virtual void copy(const String & from_path, const std::shared_ptr<IDisk> & to_disk, const String & to_path) override;

    virtual void listFiles(const String & path, std::vector<String> & file_names) override;

    virtual std::unique_ptr<ReadBufferFromFileBase> readFile(
        const String & path,
        const ReadSettings& settings) const override;

    virtual std::unique_ptr<WriteBufferFromFileBase> writeFile(
        const String & path,
        const WriteSettings& setting) override;

    virtual void removeFile(const String & path) override;

    virtual void removeFileIfExists(const String & path) override;

    virtual void removeDirectory(const String & path) override;

    virtual void removeRecursive(const String & path) override;

    virtual void setLastModified(const String & path, const Poco::Timestamp & timestamp) override;

    virtual Poco::Timestamp getLastModified(const String & path) override;

    virtual void setReadOnly(const String & path) override;

    virtual void createHardLink(const String & src_path, const String & dst_path) override;

    virtual DiskType::Type getType() const override;

private:
    inline String absolutePath(const String& relative_path) const;

    const String disk_name;
    const String disk_path;

    HDFSConnectionParams hdfs_params;

    HDFSFileSystem hdfs_fs;
};

class DiskByteHDFSReservation: public IReservation
{
public:
    DiskByteHDFSReservation(std::shared_ptr<DiskByteHDFS> disk_, UInt64 size_): disk(disk_), size(size_) {}

    virtual UInt64 getSize() const override
    {
        return size;
    }

    virtual DiskPtr getDisk(size_t i) const override;

    virtual Disks getDisks() const override
    {
        return {disk};
    }

    virtual void update(UInt64 new_size) override
    {
        size = new_size;
    }

private:
    DiskPtr disk;
    UInt64 size;
};

}
