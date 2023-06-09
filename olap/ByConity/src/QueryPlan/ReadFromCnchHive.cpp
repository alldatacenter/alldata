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

#include <Processors/Sources/NullSource.h>
#include <QueryPlan/ReadFromCnchHive.h>
#include <Storages/Hive/HiveDataSelectExecutor.h>
#include <Storages/MergeTree/CnchHiveReadPool.h>
#include <Storages/MergeTree/CnchHiveThreadSelectBlockInputProcessor.h>
#include <common/scope_guard_safe.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int INDEX_NOT_USED;
    extern const int LOGICAL_ERROR;
}

ReadFromCnchHive::ReadFromCnchHive(
    HiveDataPartsCNCHVector parts_,
    Names real_column_names_,
    const StorageCloudHive & data_,
    const SelectQueryInfo & query_info_,
    StorageMetadataPtr metadata_snapshot_,
    ContextPtr context_,
    size_t max_block_size_,
    size_t num_streams_,
    Poco::Logger * log_)
    : ISourceStep(
        DataStream{.header = metadata_snapshot_->getSampleBlockForColumns(real_column_names_, data_.getVirtuals(), data_.getStorageID())})
    , data_parts(parts_)
    , real_column_names(std::move(real_column_names_))
    , data(data_)
    , query_info(query_info_)
    , metadata_snapshot(std::move(metadata_snapshot_))
    , context(std::move(context_))
    , max_block_size(max_block_size_)
    , num_streams(num_streams_)
    , log(log_)
{
}

void ReadFromCnchHive::initializePipeline(QueryPipeline & pipeline, const BuildQueryPipelineSettings &)
{
    if (data_parts.empty())
    {
        pipeline.init(Pipe(std::make_shared<NullSource>(getOutputStream().header)));
        return;
    }

    LOG_TRACE(log, " data_parts size = {} num_streams {}", data_parts.size(), num_streams);

    Pipe pipe;

    BlocksInDataParts parts_with_row_groups{data_parts.begin(), data_parts.end()};

    auto process = [&](BlocksInDataParts data_parts_with_row_groups, int part_index) {
        const auto & part = data_parts[part_index];
        data_parts_with_row_groups[part_index].total_blocks = part->getTotalBlockNumber();
    };

    size_t num_threads = std::min(size_t(num_streams), data_parts.size());
    if (num_threads <= 1)
    {
        for (size_t part_index = 0; part_index < data_parts.size(); ++part_index)
            process(parts_with_row_groups, part_index);
    }
    else
    {
        /// Parallel loading of data parts.
        ThreadPool pool(num_threads);

        for (size_t part_index = 0; part_index < data_parts.size(); ++part_index)
            pool.scheduleOrThrowOnError([&, part_index, thread_group = CurrentThread::getGroup()] {
                SCOPE_EXIT_SAFE(if (thread_group) CurrentThread::detachQueryIfNotDetached(););
                if (thread_group)
                    CurrentThread::attachTo(thread_group);

                process(parts_with_row_groups, part_index);
            });

        pool.wait();
    }

    pipe = spreadRowGroupsAmongStreams(context, std::move(parts_with_row_groups), num_streams, real_column_names, max_block_size);

    if (pipe.empty())
    {
        pipeline.init(Pipe(std::make_shared<NullSource>(getOutputStream().header)));
        return;
    }

    for (const auto & processor : pipe.getProcessors())
        processors.emplace_back(processor);

    pipeline.init(std::move(pipe));
}

Pipe ReadFromCnchHive::spreadRowGroupsAmongStreams(
    ContextPtr & /*context*/,
    BlocksInDataParts && parts,
    size_t /*num_streams*/,
    const Names & column_names,
    const UInt64 & /*max_block_size*/)
{
    size_t sum_row_groups = 0;
    std::vector<size_t> sum_row_groups_in_parts(parts.size());
    for (size_t i = 0; i < parts.size(); ++i)
    {
        sum_row_groups_in_parts[i] = parts[i].data_part->getTotalBlockNumber();
        sum_row_groups += sum_row_groups_in_parts[i];
    }

    Pipes res;
    size_t max_threads = data.settings.max_read_row_group_threads;

    LOG_DEBUG(log, " num_stream : {} max_threads {}", num_streams, max_threads);

    if (num_streams > max_threads)
        num_streams = max_threads;

    if (sum_row_groups > 0)
    {
        /// Reduce the number of num_streams if data is small.
        if (sum_row_groups < num_streams)
            num_streams = sum_row_groups;

        LOG_TRACE(log, " num_streams = {} sum_row_groups = {}", num_streams, sum_row_groups);

        // const size_t min_row_groups_per_stream = (sum_row_groups - 1) / num_streams + 1;

        // LOG_DEBUG(log, " num_streams is: " << num_streams << " sum_row_groups: " << sum_row_groups << " min_row_groups_per_stream: " << min_row_groups_per_stream);

        CnchHiveReadPoolPtr pool
            = std::make_shared<CnchHiveReadPool>(num_streams, sum_row_groups, std::move(parts), data, metadata_snapshot, column_names);

        for (size_t i = 0; i < num_streams; ++i)
        {
            res.emplace_back(
                std::make_shared<CnchHiveThreadSelectBlockInputProcessor>(i, pool, data, metadata_snapshot, context, max_block_size));
        }
    }

    return Pipe::unitePipes(std::move(res));
}

std::shared_ptr<IQueryPlanStep> ReadFromCnchHive::copy(ContextPtr) const
{
    throw Exception("ReadFromCnchHive can not copy", ErrorCodes::NOT_IMPLEMENTED);
}

}
