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

#include <QueryPlan/ISourceStep.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Storages/MergeTree/RowGroupsInDataPart.h>
#include <Storages/StorageCloudHive.h>

namespace DB
{
class Pipe;

class ReadFromCnchHive final : public ISourceStep
{
public:
    ReadFromCnchHive(
        HiveDataPartsCNCHVector parts_,
        Names real_column_names_,
        const StorageCloudHive & data_,
        const SelectQueryInfo & query_info_,
        StorageMetadataPtr metadata_snapshot_,
        ContextPtr context_,
        size_t max_block_size_,
        size_t num_streams_,
        Poco::Logger * log_);

    String getName() const override { return "ReadFromCnchHive"; }

    Type getType() const override { return Type::ReadFromCnchHive; }

    void initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &) override;

    std::shared_ptr<IQueryPlanStep> copy(ContextPtr ptr) const override;

private:
    HiveDataPartsCNCHVector data_parts;
    Names real_column_names;

    const StorageCloudHive & data;
    SelectQueryInfo query_info;

    StorageMetadataPtr metadata_snapshot;

    ContextPtr context;

    const size_t max_block_size;
    size_t num_streams;

    Poco::Logger * log;

    Pipe spreadRowGroupsAmongStreams(
        ContextPtr & context, BlocksInDataParts && parts, size_t num_streams, const Names & column_names, const UInt64 & max_block_size);
};
}
