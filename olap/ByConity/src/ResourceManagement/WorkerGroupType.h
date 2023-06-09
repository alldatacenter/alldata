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

namespace DB::ResourceManagement
{
namespace WorkerGroupTypeImpl
{
    enum Type : uint8_t
    {
        Unknown = 0,
        Physical = 1,
        Shared = 2,
        Composite = 3,
    };
}
using WorkerGroupType = WorkerGroupTypeImpl::Type;

constexpr auto toString(WorkerGroupType type)
{
    switch (type)
    {
        case WorkerGroupType::Physical:
            return "Physical";
        case WorkerGroupType::Shared:
            return "Shared";
        case WorkerGroupType::Composite:
            return "Composite";
        default:
            return "Unknown";
    }
}

constexpr auto toWorkerGroupType(char * type_str)
{
    for (size_t i = 0; type_str[i]; ++i)
    {
        auto & c = type_str[i];
        if (i == 0)
            c = std::toupper(c);
        else
            c = std::tolower(c);
    }

    if (strcmp(type_str,  "Physical") == 0)
        return WorkerGroupType::Physical;
    else if (strcmp(type_str,  "Shared") == 0)
        return WorkerGroupType::Shared;
    else if (strcmp(type_str,  "Composite") == 0)
        return WorkerGroupType::Composite;
    else
        return WorkerGroupType::Unknown;

}

}
