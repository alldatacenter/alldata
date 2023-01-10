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

#include "DropRangeAction.h"

#include <Catalog/Catalog.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Interpreters/ServerPartLog.h>

namespace DB
{

namespace ErrorCodes
{
extern const int LOGICAL_ERROR;
}

void DropRangeAction::appendPart(MutableMergeTreeDataPartCNCHPtr part)
{
    parts.push_back(std::move(part));
}

void DropRangeAction::appendDeleteBitmap(DeleteBitmapMetaPtr delete_bitmap)
{
    delete_bitmaps.push_back(std::move(delete_bitmap));
}

void DropRangeAction::executeV1(TxnTimestamp commit_time)
{
    String log_table_name = table->getDatabaseName() + "." + table->getTableName();

    // Set commit time for parts and bitmap, otherwise they are invisible.
    for (MutableMergeTreeDataPartCNCHPtr & part : parts)
        part->commit_time = commit_time;
    for (auto & bitmap : delete_bitmaps)
        bitmap->updateCommitTime(commit_time);

    auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(table.get());
    if (!cnch_table)
        throw Exception("CNCH table ptr is null in DropRange Action", ErrorCodes::LOGICAL_ERROR);

    auto catalog = global_context.getCnchCatalog();
    catalog->finishCommit(table, txn_id, commit_time, {parts.begin(), parts.end()}, delete_bitmaps, false, /*preallocate_mode=*/ false);

    // ServerPartLog::addNewParts(global_context. ServerPartLogElement::DROP_RANGE, parts, txn_id, false);
}

void DropRangeAction::executeV2()
{
    auto * cnch_table = dynamic_cast<StorageCnchMergeTree *>(table.get());
    if (!cnch_table)
        throw Exception("Expected StorageCnchMergeTree, but got: " + table->getName(), ErrorCodes::LOGICAL_ERROR);

    auto catalog = global_context.getCnchCatalog();
    catalog->writeParts(table, txn_id, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps, /*staged_parts*/{}}, false, /*preallocate_mode=*/ false);
}

/// Post progressing
void DropRangeAction::postCommit(TxnTimestamp commit_time)
{
    /// set commit time for part
    global_context.getCnchCatalog()->setCommitTime(table, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps, /*staged_parts*/{}}, commit_time);

    // ServerPartLog::addNewParts(global_context. ServerPartLogElement::DROP_RANGE, parts, txn_id, false);
}

void DropRangeAction::abort()
{
    // clear parts in kv
    global_context.getCnchCatalog()->clearParts(table, Catalog::CommitItems{{parts.begin(), parts.end()}, delete_bitmaps,  /*staged_parts*/ {}}, true);

    // ServerPartLog::addNewParts(global_context. ServerPartLogElement::DROP_RANGE, parts, txn_id, true);
}

}
