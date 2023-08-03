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
#include <Processors/Formats/IOutputFormat.h>
#include <Common/ConcurrentBoundedQueue.h>
#include <DataStreams/BlockStreamProfileInfo.h>
#include <IO/WriteBuffer.h>

namespace DB
{

/// LazyOutputFormat is used to retrieve ready data from executing pipeline.
/// You can periodically call `getChunk` from separate thread.
/// Used in PullingAsyncPipelineExecutor.
class LazyOutputFormat : public IOutputFormat
{

public:
    explicit LazyOutputFormat(const Block & header)
        : IOutputFormat(header, out), queue(2) {}

    String getName() const override { return "LazyOutputFormat"; }

    Chunk getChunk(UInt64 milliseconds = 0);
    Chunk getTotals();
    Chunk getExtremes();

    bool isFinished() { return queue.isFinishedAndEmpty(); }

    BlockStreamProfileInfo & getProfileInfo() { return info; }

    void setRowsBeforeLimit(size_t rows_before_limit) override;

    void onCancel() override
    {
        queue.clearAndFinish();
    }

    void finalize() override
    {
        queue.finish();
    }

protected:
    void consume(Chunk chunk) override
    {
        (void)(queue.emplace(std::move(chunk)));
    }

    void consumeTotals(Chunk chunk) override { totals = std::move(chunk); }
    void consumeExtremes(Chunk chunk) override { extremes = std::move(chunk); }

private:
    ConcurrentBoundedQueue<Chunk> queue;
    Chunk totals;
    Chunk extremes;

    /// Is not used.
    static WriteBuffer out;

    BlockStreamProfileInfo info;
};

}
