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
#include <common/strong_typedef.h>

namespace DB
{

#define MERGE_TREE_STORAGE_LEVEL_1_META_HEADER_SIZE 256
#define MERGE_TREE_STORAGE_LEVEL_1_DATA_HEADER_SIZE 256

STRONG_TYPEDEF(UInt32, MergeTreeDataFormatVersion)

const MergeTreeDataFormatVersion MERGE_TREE_DATA_MIN_FORMAT_VERSION_WITH_CUSTOM_PARTITIONING {1};
const MergeTreeDataFormatVersion MERGE_TREE_CHCH_DATA_STORAGTE_VERSION {1};

/// Remote storage version
const MergeTreeDataFormatVersion MERGE_TREE_DATA_STORAGTE_LEVEL_1_VERSION{100};

/// CNCH storage version
#define MERGE_TREE_STORAGE_CNCH_DATA_HEADER_SIZE 256
#define MERGE_TREE_STORAGE_CNCH_DATA_FOOTER_SIZE 256
}
