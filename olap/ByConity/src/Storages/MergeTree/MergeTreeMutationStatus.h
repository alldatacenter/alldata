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

#include <common/types.h>
#include <Core/Names.h>
#include <optional>
#include <map>
#include <ctime>


namespace DB
{


struct MergeTreeMutationStatus
{
    String id;
    String query_id; /// Currently only supported by HaMergeTree
    String command;
    time_t create_time = 0;
    std::map<String, Int64> block_numbers;

    /// Parts that should be mutated/merged or otherwise moved to Obsolete state for this mutation to complete.
    Names parts_to_do_names;

    /// If the mutation is done. Note that in case of ReplicatedMergeTree parts_to_do == 0 doesn't imply is_done == true.
    bool is_done = false;
    /// time when is_done is set to true
    time_t finish_time = 0;

    String latest_failed_part;
    time_t latest_fail_time = 0;
    String latest_fail_reason;
};

/// Check mutation status and throw exception in case of error during mutation
/// (latest_fail_reason not empty) or if mutation was killed (status empty
/// optional). mutation_ids passed separately, because status may be empty and
/// we can execute multiple mutations at once
void checkMutationStatus(std::optional<MergeTreeMutationStatus> & status, const std::set<String> & mutation_ids);

}
