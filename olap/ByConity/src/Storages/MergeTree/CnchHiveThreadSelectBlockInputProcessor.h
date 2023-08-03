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

#pragma once

#include <DataStreams/IBlockInputStream.h>
#include <Processors/Formats/InputStreamFromInputFormat.h>
#include <Processors/Sources/SourceWithProgress.h>
#include <Storages/MergeTree/CnchHiveReadPool.h>
#include <Storages/StorageCloudHive.h>

namespace DB
{
class CnchHiveReadPool;

class CnchHiveThreadSelectBlockInputProcessor : public SourceWithProgress
{
public:
    CnchHiveThreadSelectBlockInputProcessor(
        const size_t & thread,
        const std::shared_ptr<CnchHiveReadPool> & pool,
        const StorageCloudHive & storage,
        const StorageMetadataPtr & metadata_snapshot_,
        ContextPtr & context,
        const UInt64 & max_block_size);

    String getName() const override { return "CnchHiveThread"; }

    ~CnchHiveThreadSelectBlockInputProcessor() override;

    Block getHeader() const;

private:
    Chunk generate() override;

    bool getNewTask();

    size_t thread;

    std::shared_ptr<CnchHiveReadPool> pool;

    // const StorageCloudHive & storage;
    StorageMetadataPtr metadata_snapshot;

    ContextPtr context;
    // const UInt64 max_block_size;

    std::unique_ptr<CnchHiveReadTask> task;

    std::unique_ptr<ReadBuffer> read_buf;

    BlockInputStreamPtr stream;
};

}
