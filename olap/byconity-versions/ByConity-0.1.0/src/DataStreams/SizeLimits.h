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

#pragma once

#include <common/types.h>

namespace DB
{

/// What to do if the limit is exceeded.
enum class OverflowMode
{
    THROW     = 0,    /// Throw exception.
    BREAK     = 1,    /// Abort query execution, return what is.

    /** Only for GROUP BY: do not add new rows to the set,
      * but continue to aggregate for keys that are already in the set.
      */
    ANY       = 2,
};

class ReadBuffer;
class WriteBuffer;

struct SizeLimits
{
    /// If it is zero, corresponding limit check isn't performed.
    UInt64 max_rows = 0;
    UInt64 max_bytes = 0;
    OverflowMode overflow_mode = OverflowMode::THROW;

    SizeLimits() {}
    SizeLimits(UInt64 max_rows_, UInt64 max_bytes_, OverflowMode overflow_mode_)
        : max_rows(max_rows_), max_bytes(max_bytes_), overflow_mode(overflow_mode_) {}

    /// Check limits. If exceeded, return false or throw an exception, depending on overflow_mode.
    bool check(UInt64 rows, UInt64 bytes, const char * what, int too_many_rows_exception_code, int too_many_bytes_exception_code) const;
    bool check(UInt64 rows, UInt64 bytes, const char * what, int exception_code) const;

    /// Check limits. No exceptions.
    bool softCheck(UInt64 rows, UInt64 bytes) const;

    bool hasLimits() const { return max_rows || max_bytes; }

    void serialize(WriteBuffer & buffer) const;
    void deserialize(ReadBuffer & buffer);
};

}
