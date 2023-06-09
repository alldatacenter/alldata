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
#include <DataStreams/IBlockStream_fwd.h>
#include <Common/Stopwatch.h>
#include <cmath>

#include <vector>

namespace DB
{

class Block;
class ReadBuffer;
class WriteBuffer;

class Context;
using ContextPtr = std::shared_ptr<const Context>;

/// Information for profiling. See IBlockInputStream.h
struct BlockStreamProfileInfo
{
    /// Info about stream object this profile info refers to.
    IBlockInputStream * parent = nullptr;

    bool started = false;
    Stopwatch total_stopwatch {CLOCK_MONOTONIC_COARSE};    /// Time with waiting time
    Stopwatch cpu_thread_stopwatch {CLOCK_THREAD_CPUTIME_ID};   /// Time with cpu time over thread

    size_t rows = 0;
    size_t blocks = 0;
    size_t bytes = 0;
    /// time stats
    /// main threads
    UInt64 cpu_time_ns = 0;
    UInt64 wall_time_ns = 0;
    /// async threads
    // UInt64 extra_wall_time_ns = 0;
    // UInt64 extra_cpu_time_ns = 0;

    UInt64 wallMilliseconds() const { return wall_time_ns / 1000000UL; }
    UInt64 cpuMilliseconds() const { return cpu_time_ns / 1000000UL; }
    double wallSeconds() const { return wall_time_ns / static_cast<double>(1000000000UL); }
    double cpuSeconds() const { return cpu_time_ns / static_cast<double>(1000000000UL); }
    UInt64 rowsPerSecond() const
    {
        double ms = std::round(wall_time_ns / static_cast<double>(1000000UL));
        if (ms >= 1)
            return rows * 1000 / ms;
        else
            return rows;
    }
    UInt64 bytesPerSecond() const
    {
        double ms = std::round(wall_time_ns / static_cast<double>(1000000UL));
        if (ms >= 1)
            return bytes * 1000 / ms;
        else
            return bytes;
    }

    using BlockStreamProfileInfos = std::vector<const BlockStreamProfileInfo *>;

    /// Collect BlockStreamProfileInfo for the nearest sources in the tree named `name`. Example; collect all info for PartialSorting streams.
    void collectInfosForStreamsWithName(const char * name, BlockStreamProfileInfos & res) const;

    /** Get the number of rows if there were no LIMIT.
      * If there is no LIMIT, 0 is returned.
      * If the query does not contain ORDER BY, the number can be underestimated - return the number of rows in blocks that were read before LIMIT reached.
      * If the query contains an ORDER BY, then returns the exact number of rows as if LIMIT is removed from query.
      */
    size_t getRowsBeforeLimit() const;
    bool hasAppliedLimit() const;

    void update(Block & block);
    void update(size_t num_rows, size_t num_bytes);

    /// Binary serialization and deserialization of main fields.
    /// Writes only main fields i.e. fields that required by internal transmission protocol.
    void read(ReadBuffer & in);
    void write(WriteBuffer & out) const;

    /// Sets main fields from other object (see methods above).
    /// If skip_block_size_info if true, then rows, bytes and block fields are ignored.
    void setFrom(const BlockStreamProfileInfo & rhs, bool skip_block_size_info);

    /// Only for Processors.
    void setRowsBeforeLimit(size_t rows_before_limit_)
    {
        applied_limit = true;
        rows_before_limit = rows_before_limit_;
    }

private:
    void calculateRowsBeforeLimit() const;

    /// For these fields we make accessors, because they must be calculated beforehand.
    mutable bool applied_limit = false;                    /// Whether LIMIT was applied
    mutable size_t rows_before_limit = 0;
    mutable bool calculated_rows_before_limit = false;    /// Whether the field rows_before_limit was calculated
};

struct ExtendedProfileInfo
{
    size_t read_rows = 0;
    size_t read_bytes = 0;
    size_t read_cached_bytes = 0;

    size_t written_rows = 0;
    size_t written_bytes = 0;
    size_t written_duration = 0;   /// ms

    size_t runtime_latency = 0;  /// ms

    void read(ReadBuffer & in);
    void write(WriteBuffer & out) const;
};

}
