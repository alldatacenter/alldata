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

#include <Core/QueryProcessingStage.h>
#include <Storages/MergeTree/RowGroupsInDataPart.h>
#include <Storages/SelectQueryInfo.h>
#include <Storages/StorageCloudHive.h>

namespace DB
{
/** Executes SELECT queries on data from the hive.
  */
class HiveDataSelectExecutor
{
public:
    HiveDataSelectExecutor(const StorageCloudHive & data_);

    QueryPlanPtr read(
        const Names & column_names,
        const StorageMetadataPtr & metadata_snapshot,
        const SelectQueryInfo & query_info,
        ContextPtr & context,
        UInt64 max_block_size,
        size_t num_streams) const;

    // private:
    //     Pipe spreadRowGroupsAmongStreams(
    //         const Context & context,
    //         RowGroupsInDataParts && parts,
    //         size_t num_streams,
    //         const Names & column_names,
    //         const UInt64 max_block_size) const;

private:
    const StorageCloudHive & data;
    Poco::Logger * log;
};

}
