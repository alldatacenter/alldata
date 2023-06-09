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

#include <Catalog/Catalog.h>
#include <CloudServices/CnchDedupHelper.h>
#include <CloudServices/CnchServerClient.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/StorageCnchMergeTree.h>

namespace DB::ErrorCodes
{
extern const int LOGICAL_ERROR;
}

namespace DB::CnchDedupHelper
{

static void checkDedupScope(const DedupScope & scope, const MergeTreeMetaBase & storage)
{
    if (!storage.getSettings()->partition_level_unique_keys && !scope.isTable())
        throw Exception("Expect TABLE scope for table level uniqueness", ErrorCodes::LOGICAL_ERROR);
}

std::vector<LockInfoPtr>
getLocksToAcquire(const DedupScope & scope, TxnTimestamp txn_id, const MergeTreeMetaBase & storage, UInt64 timeout_ms)
{
    checkDedupScope(scope, storage);

    std::vector<LockInfoPtr> res;
    if (scope.isPartitions())
    {
        for (auto & partition : scope.getPartitions())
        {
            auto lock_info = std::make_shared<LockInfo>(txn_id);
            lock_info->setMode(LockMode::X);
            lock_info->setTimeout(timeout_ms);
            lock_info->setUUID(storage.getStorageUUID());
            lock_info->setPartition(partition);
            res.push_back(std::move(lock_info));
        }
    }
    else
    {
        auto lock_info = std::make_shared<LockInfo>(txn_id);
        lock_info->setMode(LockMode::X);
        lock_info->setTimeout(timeout_ms);
        lock_info->setUUID(storage.getStorageUUID());
        res.push_back(std::move(lock_info));
    }
    return res;
}

MergeTreeDataPartsCNCHVector getStagedPartsToDedup(const DedupScope & scope, StorageCnchMergeTree & cnch_table, TxnTimestamp ts)
{
    checkDedupScope(scope, cnch_table);

    MergeTreeDataPartsCNCHVector staged_parts;
    if (scope.isPartitions())
    {
        NameSet partitions_filter{scope.getPartitions().begin(), scope.getPartitions().end()};
        return cnch_table.getStagedParts(ts, &partitions_filter);
    }
    else
    {
        /// For table-level unique key, row may be updated from one partition to another,
        /// therefore we must dedup with staged parts from all partitions to ensure correctness.
        /// Example: foo(p String, k Int32) partition by p unique key k
        ///  Time   Action
        ///   t1    insert into foo ('P1', 1);
        ///   t2    dedup worker found staged part in P1 and waited for lock
        ///   t3    insert into foo ('P2', 1);
        ///   t4    insert into foo ('P1', 1);
        ///   t5    dedup worker acquired lock and began to dedup
        /// If dedup worker only processes staged parts from P1 at t5, the final data of foo would be ('P2', 1), which is wrong.
        return cnch_table.getStagedParts(ts);
    }
}

MergeTreeDataPartsCNCHVector getVisiblePartsToDedup(const DedupScope & scope, StorageCnchMergeTree & cnch_table, TxnTimestamp ts)
{
    checkDedupScope(scope, cnch_table);

    if (scope.isPartitions())
    {
        Names partitions_filter{scope.getPartitions().begin(), scope.getPartitions().end()};
        return cnch_table.getUniqueTableMeta(ts, partitions_filter);
    }
    else
    {
        return cnch_table.getUniqueTableMeta(ts);
    }
}

Block filterBlock(const Block & block, const FilterInfo & filter_info)
{
    if (filter_info.num_filtered == 0)
        return block;

    Block res = block.cloneEmpty();
    ssize_t new_size_hint = res.rows() - filter_info.num_filtered;
    for (size_t i = 0; i < res.columns(); ++i)
    {
        ColumnWithTypeAndName & dst_col = res.getByPosition(i);
        const ColumnWithTypeAndName & src_col = block.getByPosition(i);
        dst_col.column = src_col.column->filter(filter_info.filter, new_size_hint);
    }
    return res;
}

}
