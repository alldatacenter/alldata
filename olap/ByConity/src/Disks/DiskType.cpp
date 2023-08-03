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

#include <Disks/DiskType.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

int DiskType::toInt(Type disk_type)
{
    return static_cast<int>(disk_type);
}

DiskType::Type DiskType::toType(int disk_type_id)
{   
    if (disk_type_id > TYPE_NUM_IN_DISK_TYPE)
    {
        throw Exception("Unknown disk type id " + std::to_string(disk_type_id),
            ErrorCodes::BAD_ARGUMENTS);
    }
    return static_cast<Type>(disk_type_id);
}

}
