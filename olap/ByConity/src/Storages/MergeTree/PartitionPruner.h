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

#include <unordered_map>

#include <Storages/KeyDescription.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/KeyCondition.h>
#include "Catalog/DataModelPartWrapper.h"

namespace DB
{

/// Pruning partitions in verbatim way using KeyCondition
class PartitionPruner
{
private:
    std::unordered_map<String, bool> partition_filter_map;

    /// partition_key is adjusted here (with substitution from modulo to moduloLegacy).
    KeyDescription partition_key;

    KeyCondition partition_condition;
    bool useless;
    using DataPart = IMergeTreeDataPart;
    using DataPartPtr = std::shared_ptr<const DataPart>;

public:
    PartitionPruner(const StorageMetadataPtr & metadata, const SelectQueryInfo & query_info, ContextPtr context, bool strict)
        : partition_key(MergeTreePartition::adjustPartitionKey(metadata, context))
        , partition_condition(
              query_info, context, partition_key.column_names, partition_key.expression, true /* single_point */, strict)
        , useless(strict ? partition_condition.anyUnknownOrAlwaysTrue() : partition_condition.alwaysUnknownOrTrue())
    {
    }

    bool canBePruned(const DataPart & part);

    bool canBePruned(const ServerDataPart & part);

    bool isUseless() const { return useless; }

    const KeyCondition & getKeyCondition() const { return partition_condition; }
};

}
