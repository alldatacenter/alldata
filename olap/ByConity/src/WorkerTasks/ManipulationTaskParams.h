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

#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Core/Types.h>
#include <Storages/IStorage_fwd.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MutationCommands.h>
#include <Transaction/TxnTimestamp.h>
#include <WorkerTasks/ManipulationType.h>

namespace DB
{

class MutationCommands;

struct ManipulationTaskParams
{
    using Type = ManipulationType;

    Type type{Type::Empty};
    UInt16 rpc_port{0};

    String task_id;
    Int64 txn_id{0};

    String create_table_query; /// non-empty if the task needs to create temporary table
    StoragePtr storage; /// On which storage this manipulate task should run
    bool is_bucket_table{false}; /// mark if the table is treated as bucket table when create this manipulation task

    ServerDataPartsVector source_parts;         /// Used by server
    MergeTreeDataPartsVector source_data_parts; /// Used by worker
    MergeTreeDataPartsVector all_parts;         /// Used by callee
    Strings new_part_names;

    TxnTimestamp columns_commit_time;
    TxnTimestamp mutation_commit_time;

    std::shared_ptr<MutationCommands> mutation_commands;

    explicit ManipulationTaskParams(StoragePtr s) : storage(std::move(s)) {}
    ManipulationTaskParams(const ManipulationTaskParams &) = default;
    ManipulationTaskParams & operator=(const ManipulationTaskParams &) = default;
    ManipulationTaskParams(ManipulationTaskParams &&) = default;
    ManipulationTaskParams & operator=(ManipulationTaskParams &&) = default;

    String toDebugString() const;

    void assignSourceParts(ServerDataPartsVector parts);
    void assignSourceParts(MergeTreeDataPartsVector parts);

    void assignParts(MergeTreeMutableDataPartsVector parts);

private:
    template <class Vec>
    void assignSourcePartsImpl(const Vec & parts);
};

}
