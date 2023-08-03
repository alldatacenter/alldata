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

#include <Disks/DiskType.h>
#include <Disks/DiskHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INVALID_CONFIG_PARAMETER;
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
}

String getDiskNameForPathId(const VolumePtr& volume, UInt32 path_id)
{
    if (path_id == 0)
    {
        return volume->getDefaultDisk()->getName();
    }
    switch (volume->getDisk()->getType())
    {   
        case DiskType::Type::ByteHDFS: return "HDFS/" + std::to_string(path_id);
        case DiskType::Type::ByteS3: return "S3/" + std::to_string(path_id);
        default:
            throw Exception(fmt::format("Invalid volume type {}",
                DiskType::toString(volume->getDisk()->getType())), ErrorCodes::BAD_ARGUMENTS);
    }
}

String getDiskNameForPathId(const StoragePolicyPtr& storage_policy, UInt32 path_id)
{
    return getDiskNameForPathId(storage_policy->getVolume(0), path_id);
}

DiskPtr getDiskForPathId(const StoragePolicyPtr& storage_policy, UInt32 path_id)
{
    VolumePtr remote_volume = storage_policy->getVolume(0);
    String disk_name = getDiskNameForPathId(remote_volume, path_id);
    DiskPtr disk = storage_policy->getDiskByName(disk_name);
    if (disk == nullptr)
        throw Exception("Disk " + disk_name + " not found in " + storage_policy->getName(),
            ErrorCodes::INVALID_CONFIG_PARAMETER);
    return disk;
}

UInt32 getPartFromDisk(const VolumePtr& volume, const DiskPtr& disk)
{
    if (volume->getDefaultDisk()->getName() == disk->getName())
        return 0;

    const String& disk_name = disk->getName();
    auto pos = disk_name.find('/');
    if (pos == std::string::npos || pos == disk_name.size() - 1)
        throw Exception("Invalid disk name for remote disk: " + disk->getName(),
            ErrorCodes::LOGICAL_ERROR);

    return std::stoul(disk_name.substr(pos + 1)); 
}

}
