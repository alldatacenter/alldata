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

#include <Processors/Transforms/LimitsCheckingTransform.h>
#include <Access/EnabledQuota.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TOO_MANY_ROWS;
    extern const int TOO_MANY_ROWS_OR_BYTES;
}


void ProcessorProfileInfo::update(const Chunk & block)
{
    ++blocks;
    rows += block.getNumRows();
    bytes += block.bytes();
}

LimitsCheckingTransform::LimitsCheckingTransform(const Block & header_, StreamLocalLimits limits_)
    : ISimpleTransform(header_, header_, false)
    , limits(std::move(limits_))
{
}

void LimitsCheckingTransform::transform(Chunk & chunk)
{
    if (!info.started)
    {
        info.total_stopwatch.start();
        info.started = true;
    }

    if (!limits.speed_limits.checkTimeLimit(info.total_stopwatch, limits.timeout_overflow_mode))
    {
        stopReading();
        return;
    }

    if (chunk)
    {
        info.update(chunk);

        if (limits.mode == LimitsMode::LIMITS_CURRENT &&
            !limits.size_limits.check(info.rows, info.bytes, "result", ErrorCodes::TOO_MANY_ROWS_OR_BYTES))
        {
            stopReading();
        }

        if (quota)
            checkQuota(chunk);
    }
}

void LimitsCheckingTransform::checkQuota(Chunk & chunk)
{
    switch (limits.mode)
    {
        case LimitsMode::LIMITS_TOTAL:
            /// Checked in SourceWithProgress::progress method.
            break;

        case LimitsMode::LIMITS_CURRENT:
        {
            UInt64 total_elapsed = info.total_stopwatch.elapsedNanoseconds();
            quota->used(
                {Quota::RESULT_ROWS, chunk.getNumRows()},
                {Quota::RESULT_BYTES, chunk.bytes()},
                {Quota::EXECUTION_TIME, total_elapsed - prev_elapsed});
            prev_elapsed = total_elapsed;
            break;
        }
    }
}

}
