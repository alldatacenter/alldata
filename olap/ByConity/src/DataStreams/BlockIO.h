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

#include <Client/Connection.h>
#include <DataStreams/IBlockStream_fwd.h>
#include <functional>
#include <Processors/QueryPipeline.h>


namespace DB
{

class ProcessListEntry;
class PlanSegmentProcessListEntry;

struct BlockIO
{
    BlockIO() = default;
    BlockIO(BlockIO &&) = default;

    BlockIO & operator= (BlockIO && rhs);
    ~BlockIO();

    BlockIO(const BlockIO &) = delete;
    BlockIO & operator= (const BlockIO & rhs) = delete;

    std::shared_ptr<ProcessListEntry> process_list_entry;
    std::shared_ptr<PlanSegmentProcessListEntry> plan_segment_process_entry;

    BlockOutputStreamPtr out;
    BlockInputStreamPtr in;

    QueryPipeline pipeline;

    /// Callbacks for query logging could be set here.
    std::function<void(IBlockInputStream *, IBlockOutputStream *, QueryPipeline *, UInt64)>    finish_callback;
    std::function<void(UInt64)>                                                                exception_callback;

    /// When it is true, don't bother sending any non-empty blocks to the out stream
    bool null_format = false;

    Stopwatch watch;

    ConnectionPtr remote_execution_conn;

    /// Call these functions if you want to log the request.
    void onFinish()
    {
        watch.stop();
        if (finish_callback)
        {
            QueryPipeline * pipeline_ptr = nullptr;
            if (pipeline.initialized())
                pipeline_ptr = &pipeline;

            finish_callback(in.get(), out.get(), pipeline_ptr, watch.elapsedMilliseconds());
        }
    }

    void onException()
    {
        watch.stop();
        if (exception_callback)
            exception_callback(watch.elapsedMilliseconds());
    }

    /// Returns in or converts pipeline to stream. Throws if out is not empty.
    BlockInputStreamPtr getInputStream();

private:
    void reset();
};

}
