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
#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage.h>
#include <Common/Stopwatch.h>

namespace Poco
{
class Logger;
};

namespace DB
{

class ReplicatedMergeTreeBlockOutputStream;

/** Writes data to the specified table and to all dependent materialized views.
  */
class PushingToViewsBlockOutputStream : public IBlockOutputStream, WithContext
{
public:
    PushingToViewsBlockOutputStream(
        const StoragePtr & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        ContextPtr context_,
        const ASTPtr & query_ptr_,
        bool no_destination = false);

    Block getHeader() const override;
    void write(const Block & block) override;

    void flush() override;
    void writePrefix() override;
    void writeSuffix() override;

private:
    StoragePtr storage;
    StorageMetadataPtr metadata_snapshot;
    BlockOutputStreamPtr output;
    ReplicatedMergeTreeBlockOutputStream * replicated_output = nullptr;
    Poco::Logger * log;

    ASTPtr query_ptr;
    Stopwatch main_watch;

    struct ViewInfo
    {
        ASTPtr query;
        StorageID table_id;
        BlockOutputStreamPtr out;
        std::exception_ptr exception;
        UInt64 elapsed_ms = 0;
        String implicit_column;
    };

    std::vector<ViewInfo> views;
    ContextMutablePtr select_context;
    ContextMutablePtr insert_context;

    void process(const Block & block, ViewInfo & view);
};


}
