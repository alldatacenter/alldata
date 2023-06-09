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

#include <memory>
#include <Core/Names.h>
#include <Core/Block.h>
#include <Columns/IColumn.h>
#include <Common/PODArray.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Transaction/LockRequest.h>

namespace DB
{
class MergeTreeMetaBase;
class StorageCnchMergeTree;
}


namespace DB::CnchDedupHelper
{

class DedupScope
{
public:
    static DedupScope Table()
    {
        static DedupScope table_scope{true};
        return table_scope;
    }

    static DedupScope Partitions(const NameOrderedSet & partitions) { return {false, partitions}; }

    bool isTable() const { return is_table; }
    bool isPartitions() const { return !is_table; }

    const NameOrderedSet & getPartitions() const { return partitions; }

private:
    DedupScope(bool is_table_, const NameOrderedSet & partitions_ = {}) : is_table(is_table_), partitions(partitions_) { }

    bool is_table{false};
    NameOrderedSet partitions;
};

std::vector<LockInfoPtr>
getLocksToAcquire(const DedupScope & scope, TxnTimestamp txn_id, const MergeTreeMetaBase & storage, UInt64 timeout_ms);

MergeTreeDataPartsCNCHVector getStagedPartsToDedup(const DedupScope & scope, StorageCnchMergeTree & cnch_table, TxnTimestamp ts);

MergeTreeDataPartsCNCHVector getVisiblePartsToDedup(const DedupScope & scope, StorageCnchMergeTree & cnch_table, TxnTimestamp ts);

struct FilterInfo
{
    IColumn::Filter filter;
    size_t num_filtered{0};
};

Block filterBlock(const Block & block, const FilterInfo & filter_info);

}
