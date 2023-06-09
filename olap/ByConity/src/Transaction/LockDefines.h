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

#include <Core/Types.h>

namespace DB
{
enum class LockMode : UInt32
{
    NONE = 0,
    IS = 1,
    IX = 2,
    S = 3,
    X = 4,
    SIZE = 5,
};

template <typename E>
constexpr auto to_underlying(E e) noexcept
{
    return static_cast<std::underlying_type_t<E>>(e);
}

static constexpr auto LockModeSize = to_underlying(LockMode::SIZE);

bool conflicts(LockMode newMode, UInt32 currentModes);

inline UInt32 modeMask(LockMode mode)
{
    return 1 << to_underlying(mode);
}

constexpr auto toString(LockMode mode)
{
    switch (mode)
    {
        case LockMode::NONE:
            return "None";
        case LockMode::IS:
            return "IS";
        case LockMode::IX:
            return "IX";
        case LockMode::S:
            return "S";
        case LockMode::X:
            return "X";
        default:
            return "Unknown";
    }
}

String lockModesToDebugString(UInt32 modes);

enum class LockStatus : UInt32
{
    LOCK_INIT = 0,
    LOCK_OK,
    LOCK_WAITING,
    LOCK_TIMEOUT,
};

enum class LockLevel : UInt32
{
    TABLE = 0,
    BUCKET = 1,
    PARTITION = 2,
    SIZE = 3,
};

static constexpr auto LockLevelSize = to_underlying(LockLevel::SIZE);

constexpr auto toString(LockLevel level)
{
    switch (level)
    {
        case LockLevel::TABLE:
            return "Table";
        case LockLevel::BUCKET:
            return "Bucket";
        case LockLevel::PARTITION:
            return "Partition";
        default:
            return "Unknown";
    }
}
}
