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

namespace DB::Status
{
#define STATUS_DELETE 0x01ull
#define STATUS_INACTIVE 0x02ull
#define STATUS_DETACH 0x04ull

UInt64 setDelete(const UInt64 & status);
bool isDeleted(const UInt64 & status);

UInt64 setDetached(const UInt64 & status);
UInt64 setAttached(const UInt64 & status);
bool isDetached(const UInt64 & status);

UInt64 setInActive(const UInt64 & status, const bool is_active);
bool isInActive(const UInt64 & status);
}
