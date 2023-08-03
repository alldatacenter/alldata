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

#include <Transaction/Actions/MergeMutateAction.h>
#include <Catalog/Catalog.h>
#include <CloudServices/commitCnchParts.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int DIRECTORY_ALREADY_EXISTS;
}

void MergeMutateAction::appendPart(MutableMergeTreeDataPartCNCHPtr part)
{
    parts.emplace_back(std::move(part));
}

void MergeMutateAction::executeV1(TxnTimestamp commit_time)
{
    auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(table.get());
    if (!cnch_table)
        throw Exception("Expected StorageCnchMergeTree, but got: " + table->getName(), ErrorCodes::LOGICAL_ERROR);

    // Set commit time for parts and bitmap, otherwise they are invisible.
    for (MutableMergeTreeDataPartCNCHPtr & part : parts)
    {
        updatePartData(part, commit_time);
    }

    for (auto & bitmap : delete_bitmaps)
        bitmap->updateCommitTime(commit_time);

    global_context.getCnchCatalog()->finishCommit(table, txn_id, commit_time, {parts.begin(), parts.end()}, delete_bitmaps, true, /*preallocate_mode=*/ false);
}

void MergeMutateAction::executeV2()
{
    auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(table.get());
    if (!cnch_table)
        throw Exception("Expected StorageCnchMergeTree, but got: " + table->getName(), ErrorCodes::LOGICAL_ERROR);

    global_context.getCnchCatalog()->writeParts(table, txn_id, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps, /*staged_parts*/{}}, true,  /*preallocate_mode=*/ false);
}

/// Post processing
void MergeMutateAction::postCommit(TxnTimestamp commit_time)
{
    /// set commit time for part
    global_context.getCnchCatalog()->setCommitTime(table, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps, /*staged_parts*/{}}, commit_time);
    for (auto & part : parts)
        part->commit_time = commit_time;
}

void MergeMutateAction::abort()
{
    // clear parts in kv
    // skip part cache to avoid blocking by write lock of part cache for long time
    global_context.getCnchCatalog()->clearParts(table, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps, /*staged_parts*/{}}, true);
}

void MergeMutateAction::updatePartData(MutableMergeTreeDataPartCNCHPtr part, [[maybe_unused]] TxnTimestamp commit_time)
{
    part->name = part->info.getPartName();
    part->commit_time = commit_time;
}

}
