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
#include <common/logger_useful.h>

namespace DB
{
class MergeTreeMetaBase;

enum class ServerSelectPartsDecision
{
    SELECTED = 0,
    CANNOT_SELECT = 1,
    NOTHING_TO_MERGE = 2,
};

using ServerCanMergeCallback = std::function<bool(const ServerDataPartPtr &, const ServerDataPartPtr &)>;

ServerSelectPartsDecision selectPartsToMerge(
    const MergeTreeMetaBase & data,
    std::vector<ServerDataPartsVector> & res,
    const ServerDataPartsVector & data_parts,
    ServerCanMergeCallback can_merge_callback,
    size_t max_total_size_to_merge,
    bool aggressive,
    bool enable_batch_select,
    bool merge_with_ttl_allowed,
    Poco::Logger * log);

/**
* Group data parts by bucket number
*/
void groupPartsByBucketNumber(const MergeTreeMetaBase & data, std::unordered_map<Int64, ServerDataPartsVector> & grouped_buckets, const ServerDataPartsVector & parts);

}
