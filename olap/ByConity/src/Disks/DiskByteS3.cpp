/*
 * Copyright 2023 Bytedance Ltd. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <filesystem>
#include <Disks/DiskByteS3.h>
#include <common/logger_useful.h>
#include <Common/filesystemHelpers.h>
#include <Common/quoteString.h>
#include <Common/formatReadable.h>
#include <Disks/DiskFactory.h>
#include <Storages/S3/RAReadBufferFromS3.h>
#include <Storages/S3/WriteBufferFromByteS3.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int S3_ERROR;
    extern const int INCORRECT_DISK_INDEX;
    extern const int NOT_IMPLEMENTED;
    extern const int BAD_ARGUMENTS;
}

std::mutex DiskByteS3::reservation_mutex;

class DiskByteS3Reservation : public IReservation
{
public:
    DiskByteS3Reservation(const DiskByteS3Ptr & disk_, UInt64 size_)
        : disk(disk_), size(size_), metric_increment(CurrentMetrics::DiskSpaceReservedForMerge, size_)
    {
    }

    UInt64 getSize() const override { return size; }

    DiskPtr getDisk(size_t i) const override
    {
        if (i != 0)
        {
            throw Exception("Can't use i != 0 with single disk reservation", ErrorCodes::INCORRECT_DISK_INDEX);
        }
        return disk;
    }

    Disks getDisks() const override
    {
        return {disk};
    }

    void update(UInt64 new_size) override
    {
        size = new_size;
    }

private:
    DiskByteS3Ptr disk;
    UInt64 size;
    CurrentMetrics::Increment metric_increment;
};

ReservationPtr DiskByteS3::reserve(UInt64 bytes)
{
    return std::make_unique<DiskByteS3Reservation>(std::static_pointer_cast<DiskByteS3>(shared_from_this()), bytes);
}

UInt64 DiskByteS3::getID() const
{
    return static_cast<UInt64>(std::hash<String>{}(DiskType::toString(getType())) ^ std::hash<String>{}(getPath()));
}

bool DiskByteS3::exists(const String& path) const
{
    // exists may used for object or some prefix, so use list instead of head object
    auto [more, token, object_names] = s3_util.listObjectsWithPrefix(
        std::filesystem::path(root_prefix) / path, std::nullopt, 1);
    return !object_names.empty();
}

size_t DiskByteS3::getFileSize(const String& path) const
{
    return s3_util.getObjectSize(std::filesystem::path(root_prefix) / path);
}

void DiskByteS3::listFiles(const String& path, std::vector<String>& file_names)
{
    String prefix = std::filesystem::path(root_prefix) / path;

    bool more_objects = false;
    std::optional<String> next_token = std::nullopt;
    std::vector<String> object_names;

    do {
        std::tie(more_objects, next_token, object_names) = s3_util.listObjectsWithPrefix(
            prefix, next_token);
        file_names.reserve(file_names.size() + object_names.size());
        file_names.insert(file_names.end(), object_names.begin(), object_names.end());
    } while(more_objects);
}

std::unique_ptr<ReadBufferFromFileBase> DiskByteS3::readFile(const String& path,
    const ReadSettings& settings) const
{
    return std::make_unique<RAReadBufferFromS3>(s3_util.getClient(),
        s3_util.getBucket(), std::filesystem::path(root_prefix) / path, 3,
        settings.buffer_size);
}

std::unique_ptr<WriteBufferFromFileBase> DiskByteS3::writeFile(const String& path,
    const WriteSettings& settings)
{
    return std::make_unique<WriteBufferFromByteS3>(s3_util.getClient(), s3_util.getBucket(),
        std::filesystem::path(root_prefix) / path, 16 * 1024 * 1024,
        16 * 1024 * 1024, settings.file_meta, settings.buffer_size);
}

void DiskByteS3::removeFile(const String& path)
{
    s3_util.deleteObject(std::filesystem::path(root_prefix) / path);
}

void DiskByteS3::removeFileIfExists(const String& path)
{
    s3_util.deleteObject(std::filesystem::path(root_prefix) / path, false);
}

void DiskByteS3::removeRecursive(const String& path)
{
    String prefix = std::filesystem::path(root_prefix) / path;

    LOG_TRACE(&Poco::Logger::get("DiskByteS3"), "RemoveRecursive: {} - {}", prefix, path);

    s3_util.deleteObjectsWithPrefix(prefix, [](const S3::S3Util&, const String&){return true;});
}

void registerDiskByteS3(DiskFactory& factory)
{
    auto creator = [](const String& name,
        const Poco::Util::AbstractConfiguration& config, const String& config_prefix,
            ContextPtr) -> DiskPtr {
        S3::S3Config s3_cfg(config, config_prefix);
        std::shared_ptr<Aws::S3::S3Client> client = s3_cfg.create();

        return std::make_shared<DiskByteS3>(name, s3_cfg.root_prefix, s3_cfg.bucket,
            client);
    };
    factory.registerDiskType("bytes3", creator);
    factory.registerDiskType("s3", creator);
}

}
