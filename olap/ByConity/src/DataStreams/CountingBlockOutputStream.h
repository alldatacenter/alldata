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
#include <DataStreams/IBlockOutputStream.h>
#include <DataStreams/IBlockInputStream.h>
#include <Interpreters/ProcessList.h>


namespace DB
{


/// Proxy class which counts number of written block, rows, bytes
class CountingBlockOutputStream : public IBlockOutputStream
{
public:
    CountingBlockOutputStream(const BlockOutputStreamPtr & stream_)
        : stream(stream_) {}

    void setProgressCallback(const ProgressCallback & callback)
    {
        progress_callback = callback;
    }

    void setProcessListElement(QueryStatus * elem)
    {
        process_elem = elem;
    }

    const Progress & getProgress() const
    {
        return progress;
    }

    Block getHeader() const override { return stream->getHeader(); }
    void write(const Block & block) override;

    void writePrefix() override;
    void writeSuffix() override;
    void flush() override                               { stream->flush(); }
    void onProgress(const Progress & current_progress) override { stream->onProgress(current_progress); }
    String getContentType() const override              { return stream->getContentType(); }

protected:
    BlockOutputStreamPtr stream;
    Progress progress;
    ProgressCallback progress_callback;
    QueryStatus * process_elem = nullptr;
    Stopwatch stopwatch {CLOCK_MONOTONIC_COARSE};
};

}
