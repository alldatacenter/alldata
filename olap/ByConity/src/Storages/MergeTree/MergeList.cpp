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

#include <Storages/MergeTree/MergeList.h>
#include <Storages/MergeTree/MergeTreeDataMergerMutator.h>
#include <Common/CurrentMetrics.h>
#include <common/getThreadId.h>
#include <Common/CurrentThread.h>


namespace DB
{

MergeListElement::MergeListElement(const StorageID & table_id_, const FutureMergedMutatedPart & future_part, const Settings & settings)
    : table_id{table_id_}
    , partition_id{future_part.part_info.partition_id}
    , result_part_name{future_part.name}
    , result_part_path{future_part.path}
    , result_part_info{future_part.part_info}
    , num_parts{future_part.parts.size()}
    , thread_id{getThreadId()}
    , merge_type{future_part.merge_type}
    , merge_algorithm{MergeAlgorithm::Undecided}
{
    for (const auto & source_part : future_part.parts)
    {
        source_part_names.emplace_back(source_part->name);
        source_part_paths.emplace_back(source_part->getFullPath());

        total_size_bytes_compressed += source_part->getBytesOnDisk();
        total_size_marks += source_part->getMarksCount();
        total_rows_count += source_part->index_granularity.getTotalRows();
    }

    if (!future_part.parts.empty())
    {
        source_data_version = future_part.parts[0]->info.getDataVersion();
        is_mutation = (result_part_info.getDataVersion() != source_data_version);
    }

    memory_tracker.setDescription("Mutate/Merge");
    /// MemoryTracker settings should be set here, because
    /// later (see MemoryTrackerThreadSwitcher)
    /// parent memory tracker will be changed, and if merge executed from the
    /// query (OPTIMIZE TABLE), all settings will be lost (since
    /// current_thread::memory_tracker will have Thread level MemoryTracker,
    /// which does not have any settings itself, it relies on the settings of the
    /// thread_group::memory_tracker, but MemoryTrackerThreadSwitcher will reset parent).
    memory_tracker.setProfilerStep(settings.memory_profiler_step);
    memory_tracker.setSampleProbability(settings.memory_profiler_sample_probability);
    if (settings.memory_tracker_fault_probability)
        memory_tracker.setFaultProbability(settings.memory_tracker_fault_probability);

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

    /// Each merge is executed into separate background processing pool thread
    background_thread_memory_tracker = CurrentThread::getMemoryTracker();
    if (background_thread_memory_tracker)
    {
        background_thread_memory_tracker_prev_parent = background_thread_memory_tracker->getParent();
        background_thread_memory_tracker->setParent(&memory_tracker);
    }
}

MergeInfo MergeListElement::getInfo() const
{
    MergeInfo res;
    res.database = table_id.getDatabaseName();
    res.table = table_id.getTableName();
    res.result_part_name = result_part_name;
    res.result_part_path = result_part_path;
    res.partition_id = partition_id;
    res.is_mutation = is_mutation;
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
    res.merge_type = toString(merge_type);
    res.merge_algorithm = toString(merge_algorithm.load(std::memory_order_relaxed));

    for (const auto & source_part_name : source_part_names)
        res.source_part_names.emplace_back(source_part_name);

    for (const auto & source_part_path : source_part_paths)
        res.source_part_paths.emplace_back(source_part_path);

    return res;
}

MergeListElement::~MergeListElement()
{
    /// Unplug memory_tracker from current background processing pool thread
    if (background_thread_memory_tracker)
        background_thread_memory_tracker->setParent(background_thread_memory_tracker_prev_parent);
}

}
