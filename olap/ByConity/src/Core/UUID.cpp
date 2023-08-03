/*
 * Copyright 2016-2023 ClickHouse, Inc.
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


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Core/UUID.h>
#include <Core/Types.h>
#include <Common/thread_local_rng.h>
#include <common/wide_integer_impl.h>
#include <common/strong_typedef.h>
#include <IO/WriteBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>


namespace DB
{

namespace UUIDHelpers
{
    UUID generateV4()
    {
        UInt128 res{thread_local_rng(), thread_local_rng()};
        res.items[0] = (res.items[0] & 0xffffffffffff0fffull) | 0x0000000000004000ull; // low
        res.items[1] = (res.items[1] & 0x3fffffffffffffffull) | 0x8000000000000000ull; // high
        return UUID{res};
    }

    String UUIDToString(const UUID & uuid)
    {
        String uuid_str;
        WriteBufferFromString buff(uuid_str);
        writeUUIDText(uuid, buff);
        return uuid_str;
    }

    UUID toUUID(const String & uuid_str)
    {
        UUID uuid;
        ReadBufferFromString buff(uuid_str);
        readUUIDText(uuid, buff);
       return uuid;
    }

    PairInt64 UUIDToPairInt64(const UUID & uuid)
    {   
        return PairInt64(uuid.toUnderType().items[0], uuid.toUnderType().items[1]);
    }
}

}
