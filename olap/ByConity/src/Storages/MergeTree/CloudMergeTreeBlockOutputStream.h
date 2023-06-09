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

#include <CloudServices/CnchDedupHelper.h>
#include <CloudServices/commitCnchParts.h>
#include <DataStreams/IBlockOutputStream.h>
#include <Storages/MergeTree/MergeTreeCNCHDataDumper.h>
#include <Storages/MergeTree/MergeTreeDataWriter.h>
#include <common/logger_useful.h>
#include <Common/SimpleIncrement.h>
#include "WorkerTasks/ManipulationType.h"

namespace DB
{
class Block;
class Context;
class MergeTreeMetaBase;

class CloudMergeTreeBlockOutputStream : public IBlockOutputStream
{
public:
    CloudMergeTreeBlockOutputStream(
        MergeTreeMetaBase & storage_,
        StorageMetadataPtr metadata_snapshot_,
        ContextPtr context_,
        bool to_staging_area_ = false);

    Block getHeader() const override;

    void write(const Block & block) override;
    MergeTreeMutableDataPartsVector convertBlockIntoDataParts(const Block & block, bool use_inner_block_id = false);
    void writeSuffix() override;
    void writeSuffixImpl();

    void disableTransactionCommit() { disable_transaction_commit = true; }

private:
    using FilterInfo = CnchDedupHelper::FilterInfo;
    FilterInfo dedupWithUniqueKey(const Block & block);

    void writeSuffixForInsert();
    void writeSuffixForUpsert();

    MergeTreeMetaBase & storage;
    Poco::Logger * log;
    StorageMetadataPtr metadata_snapshot;
    ContextPtr context;
    bool to_staging_area;

    MergeTreeDataWriter writer;
    CnchDataWriter cnch_writer;

    // if we want to do batch preload indexing in write suffix
    MutableMergeTreeDataPartsCNCHVector preload_parts;

    bool disable_transaction_commit{false};
    SimpleIncrement increment;
};

}
