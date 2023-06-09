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

#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Transaction/TxnTimestamp.h>
#include <WorkerTasks/ManipulationType.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>

#include <cppkafka/cppkafka.h>
#include <cppkafka/topic_partition_list.h>

namespace DB
{

class MergeTreeMetaBase;

struct DumpedData
{
    MutableMergeTreeDataPartsCNCHVector parts;
    DeleteBitmapMetaPtrVector bitmaps;
    MutableMergeTreeDataPartsCNCHVector staged_parts;
};

// TODO: proper writer
class CnchDataWriter : private boost::noncopyable
{
public:
    CnchDataWriter(
        MergeTreeMetaBase & storage_,
        ContextPtr context_,
        ManipulationType type_,
        String task_id_ = {},
        String consumer_group_ = {},
        const cppkafka::TopicPartitionList & tpl_ = {});

    ~CnchDataWriter() = default;

    DumpedData dumpAndCommitCnchParts(
        const IMutableMergeTreeDataPartsVector & temp_parts,
        const LocalDeleteBitmaps & temp_bitmaps = {},
        const IMutableMergeTreeDataPartsVector & temp_staged_parts = {});

    // server side only
    TxnTimestamp commitPreparedCnchParts(const DumpedData & data);

    /// Convert staged parts to visible parts along with the given delete bitmaps.
    void publishStagedParts(
        const MergeTreeDataPartsCNCHVector & staged_parts,
        const LocalDeleteBitmaps & bitmaps_to_dump);

    DumpedData dumpCnchParts(
        const IMutableMergeTreeDataPartsVector & temp_parts,
        const LocalDeleteBitmaps & temp_bitmaps = {},
        const IMutableMergeTreeDataPartsVector & temp_staged_parts = {});

    void commitDumpedParts(const DumpedData & dumped_data);

    void preload(const MutableMergeTreeDataPartsCNCHVector & dumped_parts);

private:

    MergeTreeMetaBase & storage;
    ContextPtr context;
    ManipulationType type;
    String task_id;

    String consumer_group;
    cppkafka::TopicPartitionList tpl;

    UUID newPartID(const MergeTreePartInfo& part_info, UInt64 txn_timestamp);
};

}
