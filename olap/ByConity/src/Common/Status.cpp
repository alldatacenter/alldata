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

#include <Common/Status.h>

namespace DB::Status
{

UInt64 setDelete(const UInt64 & status) { return status | STATUS_DELETE; }

bool isDeleted(const UInt64 & status) { return status & STATUS_DELETE; }


UInt64 setDetached(const UInt64 & status)
{
    return status | STATUS_DETACH;
}

UInt64 setAttached(const UInt64 & status)
{
    return (~STATUS_DETACH) & status;
}


bool isDetached(const UInt64 & status) { return status & STATUS_DETACH; }

UInt64 setInActive(const UInt64 & status, const bool is_active)
{
    if (is_active)
        return (~STATUS_INACTIVE) & status;
    else
        return status | STATUS_INACTIVE;
}

bool isInActive(const UInt64 & status) { return status & STATUS_INACTIVE; }

}
