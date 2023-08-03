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

#include <string>
#include <utility>

#include <city.h>
#include <Core/UUID.h>

namespace DB
{
using UUIDAndPartName = std::pair<UUID, std::string>;

struct UUIDAndPartNameHash
{
    size_t operator()(const UUIDAndPartName & key) const
    {
        return UInt128Hash{}(key.first) ^ CityHash_v1_0_2::CityHash64(key.second.data(), key.second.length());
    }
};

using TableWithPartitionHash = UUIDAndPartNameHash;

}
