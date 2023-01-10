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

#include <Disks/DiskHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INVALID_CONFIG_PARAMETER;
}

String getDiskNameForPathId(const VolumePtr& volume, UInt32 path_id)
{
    if (path_id == 0)
    {
        return volume->getDefaultDisk()->getName();
    }
    else
    {
        return "HDFS/" + toString(path_id);
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
    fmt::print(stderr, "Getting disk {}:{} from volumn {} with storage policy {}\n", disk_name, disk->getPath(), remote_volume->getName(), storage_policy->getName());
    if (disk == nullptr)
        throw Exception("Disk " + disk_name + " not found in " + storage_policy->getName(),
            ErrorCodes::INVALID_CONFIG_PARAMETER);
    return disk;
}

}
