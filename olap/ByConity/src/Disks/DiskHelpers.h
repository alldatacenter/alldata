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

#include "IVolume.h"
#include "StoragePolicy.h"
namespace DB
{

String getDiskNameForPathId(const VolumePtr& volume, UInt32 path_id);
String getDiskNameForPathId(const StoragePolicyPtr& storage_policy, UInt32 path_id);
DiskPtr getDiskForPathId(const StoragePolicyPtr& storage_policy, UInt32 path_id);
UInt32 getPartFromDisk(const VolumePtr& volume, const DiskPtr& disk);

}
