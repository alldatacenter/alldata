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

#include <Storages/MergeTree/PartitionPruner.h>
#include <Catalog/DataModelPartWrapper.h>

namespace DB
{

bool PartitionPruner::canBePruned(const DataPart & part)
{
    if (part.isEmpty() && !part.isPartial())
        return true;
    const auto & partition_id = part.info.partition_id;
    bool is_valid;
    if (auto it = partition_filter_map.find(partition_id); it != partition_filter_map.end())
        is_valid = it->second;
    else
    {
        const auto & partition_value = part.partition.value;
        std::vector<FieldRef> index_value(partition_value.begin(), partition_value.end());
        is_valid = partition_condition.mayBeTrueInRange(
            partition_value.size(), index_value.data(), index_value.data(), partition_key.data_types);
        partition_filter_map.emplace(partition_id, is_valid);
    }
    return !is_valid;
}

bool PartitionPruner::canBePruned(const ServerDataPart & part)
{
    if (part.isEmpty())
        return true;
    const auto & partition_id = part.info().partition_id;
    bool is_valid;
    if (auto it = partition_filter_map.find(partition_id); it != partition_filter_map.end())
        is_valid = it->second;
    else
    {
        const auto & partition_value = part.partition().value;
        std::vector<FieldRef> index_value(partition_value.begin(), partition_value.end());
        is_valid = partition_condition.mayBeTrueInRange(
            partition_value.size(), index_value.data(), index_value.data(), partition_key.data_types);
        partition_filter_map.emplace(partition_id, is_valid);
    }
    return !is_valid;
}

}
