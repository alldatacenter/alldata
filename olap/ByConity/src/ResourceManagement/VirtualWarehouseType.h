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

#include <cctype>
#include <cstdint>
#include <cstring>
#include <vector>
#include <Core/Types.h>

namespace DB::ResourceManagement
{
namespace VirtualWarehouseTypeImpl
{
    enum Type : uint8_t
    {
        Unknown = 0,
        Read = 1,
        Write = 2,
        Task = 3,
        Default = 4,
        Num = 5,
    };
}
using VirtualWarehouseType = VirtualWarehouseTypeImpl::Type;
using VirtualWarehouseTypes = std::vector<VirtualWarehouseType>;

constexpr auto toString(VirtualWarehouseType type)
{
    switch (type)
    {
        case VirtualWarehouseType::Read:
            return "Read";
        case VirtualWarehouseType::Write:
            return "Write";
        case VirtualWarehouseType::Task:
            return "Task";
        case VirtualWarehouseType::Default:
            return "Default";
        default:
            return "Unknown";
    }
}

constexpr VirtualWarehouseType toVirtualWarehouseType(char * type)
{
    for (size_t i = 0; type[i]; ++i)
    {
        auto & c = type[i];
        if (i == 0)
            c = std::toupper(c);
        else
            c = std::tolower(c);
    }

    if (strcmp(type, "Read") == 0)
        return VirtualWarehouseType::Read;
    else if (strcmp(type,"Write") == 0)
        return VirtualWarehouseType::Write;
    else if (strcmp(type,"Task") == 0)
        return VirtualWarehouseType::Task;
    else if (strcmp(type,"Default") == 0)
        return VirtualWarehouseType::Default;
    else
        return VirtualWarehouseType::Unknown;
}

//TODO Remove when system VW logic is deprecated
constexpr auto toSystemVWName(VirtualWarehouseType t)
{
    switch (t)
    {
        case VirtualWarehouseType::Read:
            return "vw_read";
        case VirtualWarehouseType::Write:
            return "vw_write";
        case VirtualWarehouseType::Task:
            return "vw_task";
        case VirtualWarehouseType::Default:
            return "vw_default";
        default:
            return "";
    }

}

bool isSystemVW(const String & virtual_warehouse);

}
