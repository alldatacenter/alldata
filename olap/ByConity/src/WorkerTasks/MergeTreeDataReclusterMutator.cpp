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

#include <WorkerTasks/MergeTreeDataReclusterMutator.h>
#include <QueryPlan/QueryPlan.h>
#include <Storages/MergeTree/StorageFromMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeTreeSequentialSource.h>
#include <Storages/MergeTree/CloudMergeTreeBlockOutputStream.h>
#include <Processors/Executors/PipelineExecutingBlockInputStream.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ABORTED;
}

MergeTreeDataReclusterMutator::MergeTreeDataReclusterMutator(MergeTreeMetaBase & data_)
    : data(data_)
    , log(&Poco::Logger::get(data.getLogName() + " (CnchRecluster)"))
{
}

MergeTreeMutableDataPartsVector MergeTreeDataReclusterMutator::executeClusterTask(
    const ManipulationTaskParams & params,
    ManipulationListEntry & manipulation_entry,
    ContextPtr context)
{
    MergeTreeMutableDataPartsVector clustered_parts;
    for (auto & part : params.source_data_parts)
    {
        auto new_parts = executeOnSinglePart(part, params, manipulation_entry, context);
        clustered_parts.insert(clustered_parts.end(), new_parts.begin(), new_parts.end());
    }
    return clustered_parts;
}

MergeTreeMutableDataPartsVector MergeTreeDataReclusterMutator::executeOnSinglePart(
    const MergeTreeDataPartPtr & part,
    const ManipulationTaskParams & params,
    ManipulationListEntry & manipulation_entry,
    ContextPtr context)
{
    MergeTreeMutableDataPartsVector res;
    auto metadata_snapshot = data.getInMemoryMetadataPtr();
    auto column_names = metadata_snapshot->getColumns().getNamesOfPhysical();
    auto source = std::make_shared<MergeTreeSequentialSource>(data, metadata_snapshot, part, column_names, false, true);
    QueryPipeline pipeline;
    pipeline.init(Pipe(std::move(source)));
    pipeline.setMaxThreads(1);
    BlockInputStreamPtr input_stream = std::make_shared<PipelineExecutingBlockInputStream>(std::move(pipeline));
    BlockOutputStreamPtr output_stream = data.write(nullptr, data.getInMemoryMetadataPtr(), context);
    CloudMergeTreeBlockOutputStream * cloud_stream = static_cast<CloudMergeTreeBlockOutputStream *>(output_stream.get());
    Block block;
    while(checkOperationIsNotCanceled(manipulation_entry) && (block = input_stream->read()))
    {
        auto splitted_parts = cloud_stream->convertBlockIntoDataParts(block);
        for (auto & new_part : splitted_parts)
        {
            new_part->info.mutation = part->info.mutation;
            new_part->columns_commit_time = params.columns_commit_time;
            new_part->mutation_commit_time = params.mutation_commit_time;
        }
        res.insert(res.end(), splitted_parts.begin(), splitted_parts.end());
    }
    return res;
}


bool MergeTreeDataReclusterMutator::checkOperationIsNotCanceled(const ManipulationListEntry & manipulation_entry) const
{
    if (manipulation_entry->is_cancelled)
        throw Exception("Cancelled rescluster parts", ErrorCodes::ABORTED);

    return true;
}

}
