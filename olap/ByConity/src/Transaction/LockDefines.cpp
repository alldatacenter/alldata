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

#include <Transaction/LockDefines.h>

#include <IO/WriteHelpers.h>
#include <IO/Operators.h>

namespace DB
{
// 'conflictTable[mode1] & mode2 != 0' means mode1 conflicts with mode2
static constexpr UInt32 conflictTable[] = {
    // NONE
    0,

    // IS
    // conflicts with X
    (1 << to_underlying(LockMode::X)),

    // IX
    // conflicts with S, X
    (1 << to_underlying(LockMode::S)) | (1 << to_underlying(LockMode::X)),

    // S
    // conflicts with IX, X
    (1 << to_underlying(LockMode::IX)) | (1 << to_underlying(LockMode::X)),

    // X
    // conflicts with S, X, IS, IX
    (1 << to_underlying(LockMode::S)) | (1 << to_underlying(LockMode::X)) | (1 << to_underlying(LockMode::IS))
        | (1 << to_underlying(LockMode::IX)),
};

bool conflicts(LockMode newMode, UInt32 currentMode)
{
    return (conflictTable[to_underlying(newMode)] & currentMode) != 0;
}

String lockModesToDebugString(UInt32 modes)
{
    // check whether each mode bitmask is set
    WriteBufferFromOwnString wb;
    for (UInt32 i = 0; i < LockModeSize; i++)
    {
        LockMode m = static_cast<LockMode>(i);
        if (modes & modeMask(m))
        {
            wb << toString(m) << ",";
        }
    }
    return wb.str();
}
}
