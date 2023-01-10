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

#include <DataStreams/IBlockInputStream.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <Storages/MergeTree/CnchHiveReadPool.h>
namespace ProfileEvents
{
extern const Event SlowRead;
extern const Event ReadBackoff;
}
namespace ErrorCodes
{
extern const int LOGICAL_ERROR;
}

namespace DB
{
CnchHiveReadPool::CnchHiveReadPool(
    const size_t & threads_,
    const size_t & sum_row_groups_,
    RowGroupsInDataParts && parts_,
    const StorageCloudHive & data_,
    const StorageMetadataPtr & metadata_snapshot_,
    Names column_names_)
    : backoff_state{threads_}, parts{std::move(parts_)}, data{data_}, metadata_snapshot(metadata_snapshot_), column_names{column_names_}

{
    fillPerThreadInfo(threads_, sum_row_groups_, parts);
}

///
void CnchHiveReadPool::fillPerThreadInfo(const size_t & threads, const size_t & sum_row_groups, RowGroupsInDataParts data_parts)
{
    threads_tasks.resize(threads);
    const size_t min_row_groups_per_thread = (sum_row_groups - 1) / threads + 1;
    threads_row_groups_sum.resize(threads, 0);

    LOG_TRACE(
        &Poco::Logger::get("CnchHiveReadPool"),
        " threads: {} min_row_groups_per_thread: {} ",
        threads,
        min_row_groups_per_thread);

    size_t i = 0;
    size_t current_row_group_index = 0;
    size_t need_row_groups = min_row_groups_per_thread;
    size_t count_current_row_groups = 0;

    while (!data_parts.empty())
    {
        const auto part_idx = data_parts.size() - 1;
        const auto & part = data_parts.back().data_part;
        size_t row_groups_in_part = part->getTotalRowGroups();
        std::string path = part->getFullDataPartPath();

        LOG_TRACE(
        &Poco::Logger::get("CnchHiveReadPool"),
        " part_idx: {} row_groups_in_part: {}  path = {}",
        part_idx,
        row_groups_in_part,
        path);

        if (row_groups_in_part == 0)
        {
            current_row_group_index = 0;
            need_row_groups = min_row_groups_per_thread;
            data_parts.pop_back();
            continue;
        }

        bool available = false;
        for (size_t j = 0; j < threads; j++)
        {
            if (threads_row_groups_sum[(i + j) % threads] < min_row_groups_per_thread)
            {
                i = (i + j) % threads;
                available = true;
                break;
            }
        }
        if (!available)
            throw Exception("Unexpected lack of thread slots while spreading rowgroups among threads", ErrorCodes::LOGICAL_ERROR);

        while (need_row_groups > 0 && (current_row_group_index < row_groups_in_part))
        {
            threads_tasks[i].parts_and_groups.push_back({part_idx, current_row_group_index});
            threads_row_groups_sum[i]++;
            current_row_group_index++;
            need_row_groups--;
            count_current_row_groups++;
        }

        if (need_row_groups == 0)
        {
            i = (i + 1) % threads;
            if (i != threads - 1)
                need_row_groups = min_row_groups_per_thread;
            else
                need_row_groups = sum_row_groups - count_current_row_groups;
        }

        if (current_row_group_index >= row_groups_in_part)
        {
            current_row_group_index = 0;
            data_parts.pop_back();
        }
    }

    for (size_t cnt = 0; cnt < threads; cnt++)
    {
        auto thread_task = threads_tasks[cnt];
        auto parts_and_groups = thread_task.parts_and_groups;
        auto sum_row_groups_in_parts = threads_row_groups_sum[cnt];

        LOG_TRACE(
            &Poco::Logger::get("CnchHiveReadPool"),
            "current thread : {}  parts_and_groups size: {} sum_row_groups_in_parts size: {}",
            cnt,
            parts_and_groups.size(),
            sum_row_groups_in_parts);

        for (size_t j = 0; j < parts_and_groups.size(); j++)
        {
            auto part_idx = parts_and_groups[j].part_idx;
            auto need_read_row_group_index = parts_and_groups[j].need_read_row_group_index;

            LOG_TRACE(
                &Poco::Logger::get("CnchHiveReadPool"),
                " part_idx: {} need_read_row_group_index: {}",
                part_idx,
                need_read_row_group_index);

            auto path = parts[part_idx].data_part->getFullDataPartPath();

            LOG_TRACE(
                &Poco::Logger::get("CnchHiveReadPool"),
                " part_idx: {} path: {} need_read_row_group_index: {} ",
                part_idx,
                path,
                need_read_row_group_index);
        }
    }
}

Block CnchHiveReadPool::getHeader() const
{
    return metadata_snapshot->getSampleBlockForColumns(column_names, data.getVirtuals(), data.getStorageID());
}

CnchHiveReadTaskPtr CnchHiveReadPool::getTask(const size_t & thread)
{
    LOG_TRACE(&Poco::Logger::get("CnchHiveReadPool"), " getTask ");
    const std::lock_guard lock{mutex};

    if (thread >= backoff_state.current_threads)
        return nullptr;

    auto & thread_tasks = threads_tasks[thread];
    if (thread_tasks.parts_and_groups.empty())
        return nullptr;

    auto & thread_task = thread_tasks.parts_and_groups.back();
    size_t part_idx = thread_task.part_idx;
    size_t current_row_group = thread_task.need_read_row_group_index;
    auto part = parts[part_idx].data_part;
    size_t sum_row_group_in_part = threads_row_groups_sum.back();

    thread_tasks.parts_and_groups.pop_back();
    threads_row_groups_sum.pop_back();

    LOG_TRACE(
        &Poco::Logger::get("CnchHiveReadPool"),
        "current thread {} get new Task part_idx {} current_row_group: {}",
        thread,
        part_idx,
        current_row_group);

    return std::make_unique<CnchHiveReadTask>(part, current_row_group, sum_row_group_in_part, part_idx);
}

}
