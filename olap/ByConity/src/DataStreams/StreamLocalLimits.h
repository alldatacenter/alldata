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
#include <DataStreams/SizeLimits.h>
#include <DataStreams/ExecutionSpeedLimits.h>

namespace DB
{

/** What limitations and quotas should be checked.
  * LIMITS_CURRENT - checks amount of data returned by current stream only (BlockStreamProfileInfo is used for check).
  *  Currently it is used in root streams to check max_result_{rows,bytes} limits.
  * LIMITS_TOTAL - checks total amount of read data from leaf streams (i.e. data read from disk and remote servers).
  *  It is checks max_{rows,bytes}_to_read in progress handler and use info from ProcessListElement::progress_in for this.
  *  Currently this check is performed only in leaf streams.
  */
enum class LimitsMode
{
    LIMITS_CURRENT,
    LIMITS_TOTAL,
};

/// It is a subset of limitations from Limits.
struct StreamLocalLimits
{
    LimitsMode mode = LimitsMode::LIMITS_CURRENT;

    SizeLimits size_limits;

    ExecutionSpeedLimits speed_limits;

    OverflowMode timeout_overflow_mode = OverflowMode::THROW;

    void serialize(WriteBuffer & buf) const;
    void deserialize(ReadBuffer & buf);
};

}
