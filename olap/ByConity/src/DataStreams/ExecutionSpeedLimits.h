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

#include <Poco/Timespan.h>
#include <common/types.h>
#include <DataStreams/SizeLimits.h>

class Stopwatch;

namespace DB
{

/// Limits for query execution speed.
class ExecutionSpeedLimits
{
public:
    /// For rows per second.
    size_t min_execution_rps = 0;
    size_t max_execution_rps = 0;
    /// For bytes per second.
    size_t min_execution_bps = 0;
    size_t max_execution_bps = 0;

    Poco::Timespan max_execution_time = 0;
    /// Verify that the speed is not too low after the specified time has elapsed.
    Poco::Timespan timeout_before_checking_execution_speed = 0;

    /// Pause execution in case if speed limits were exceeded.
    void throttle(size_t read_rows, size_t read_bytes, size_t total_rows_to_read, UInt64 total_elapsed_microseconds) const;

    bool checkTimeLimit(const Stopwatch & stopwatch, OverflowMode overflow_mode) const;

    void serialize(WriteBuffer & buf) const;
    void deserialize(ReadBuffer & buf);
};

}

