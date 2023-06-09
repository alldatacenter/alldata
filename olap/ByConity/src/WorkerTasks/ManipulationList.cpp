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

#include <WorkerTasks/ManipulationList.h>

#include <Catalog/DataModelPartWrapper.h>
#include <Common/CurrentThread.h>
#include <common/getThreadId.h>
#include <Storages/IStorage.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <WorkerTasks/ManipulationTaskParams.h>


namespace CurrentMetrics
{
    extern const Metric MemoryTrackingForMerges;
}

namespace DB
{

ManipulationListElement::ManipulationListElement(const ManipulationTaskParams & params, bool disable_memory_tracker)
    : type(params.type)
    , task_id(params.task_id)
    , last_touch_time(time(nullptr))
    , storage_id(params.storage->getStorageID())
    , result_part_names(params.new_part_names)
    , thread_id{getThreadId()}
{
    if (!params.source_data_parts.empty())
    {
        partition_id = params.source_data_parts.front()->info.partition_id;
        num_parts = params.source_data_parts.size();
        source_data_version = params.source_data_parts.front()->info.mutation;
        for (const auto & source_part : params.source_data_parts)
        {
            source_part_names.push_back(source_part->name);

            total_size_bytes_compressed += source_part->getBytesOnDisk();
            total_size_marks += source_part->getMarksCount();
            total_rows_count += source_part->index_granularity.getTotalRows();
        }
    }
    else
    {
        partition_id = params.source_parts.front()->info().partition_id;
        num_parts = params.source_parts.size();
        source_data_version = params.source_parts.front()->info().mutation;
        for (const auto & source_part : params.source_parts)
        {
            source_part_names.push_back(source_part->name());

            total_size_bytes_compressed += source_part->part_model().size();
            total_size_marks += source_part->part_model().marks_count();
            total_rows_count += source_part->part_model().rows_count();
        }
    }

    memory_tracker.setDescription("Manipulation");

    /// Let's try to copy memory related settings from the query,
    /// since settings that we have here is not from query, but global, from the table.
    ///
    /// NOTE: Remember, that Thread level MemoryTracker does not have any settings,
    /// so it's parent is required.
    MemoryTracker * query_memory_tracker = CurrentThread::getMemoryTracker();
    MemoryTracker * parent_query_memory_tracker;
    if (query_memory_tracker->level == VariableContext::Thread && (parent_query_memory_tracker = query_memory_tracker->getParent())
        && parent_query_memory_tracker != &total_memory_tracker)
    {
        memory_tracker.setOrRaiseHardLimit(parent_query_memory_tracker->getHardLimit());
    }

    /// Each merge is executed into separate background processing pool thread.
    /// We disable memory_tracker in CnchMergeMutateThread, since it may cause coredump.
    if (!disable_memory_tracker)
        background_thread_memory_tracker = CurrentThread::getMemoryTracker();

    if (background_thread_memory_tracker)
    {
        memory_tracker.setMetric(CurrentMetrics::MemoryTrackingForMerges);
        background_thread_memory_tracker_prev_parent = background_thread_memory_tracker->getParent();
        background_thread_memory_tracker->setParent(&memory_tracker);
    }
}

ManipulationInfo ManipulationListElement::getInfo() const
{
    ManipulationInfo res(storage_id);
    res.type = type;
    res.task_id = task_id;
    res.related_node = related_node;
    res.partition_id = partition_id;
    res.elapsed = watch.elapsedSeconds();
    res.progress = progress.load(std::memory_order_relaxed);
    res.num_parts = num_parts;
    res.total_size_bytes_compressed = total_size_bytes_compressed;
    res.total_size_marks = total_size_marks;
    res.total_rows_count = total_rows_count;
    res.bytes_read_uncompressed = bytes_read_uncompressed.load(std::memory_order_relaxed);
    res.bytes_written_uncompressed = bytes_written_uncompressed.load(std::memory_order_relaxed);
    res.rows_read = rows_read.load(std::memory_order_relaxed);
    res.rows_written = rows_written.load(std::memory_order_relaxed);
    res.columns_written = columns_written.load(std::memory_order_relaxed);
    res.memory_usage = memory_tracker.get();
    res.thread_id = thread_id;

    for (const auto & source_part_name : source_part_names)
        res.source_part_names.emplace_back(source_part_name);
    for (const auto & result_part_name : result_part_names)
        res.result_part_names.emplace_back(result_part_name);

    return res;
}

ManipulationInfo::ManipulationInfo(StorageID storage_id_): storage_id(std::move(storage_id_))
{
}

void ManipulationInfo::update(const ManipulationInfo & info)
{
    progress = info.progress;
    bytes_read_uncompressed = info.bytes_read_uncompressed;
    bytes_written_uncompressed = info.bytes_written_uncompressed;
    rows_read = info.rows_read;
    rows_written = info.rows_written;
    columns_written = info.columns_written;
}

ManipulationListElement::~ManipulationListElement()
{
    /// Unplug memory_tracker from current background processing pool thread
    if (background_thread_memory_tracker)
        background_thread_memory_tracker->setParent(background_thread_memory_tracker_prev_parent);
}

}
