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

#include <map>

#include <IO/Progress.h>
#include <Storages/MergeTree/MergeAlgorithm.h>
#include <WorkerTasks/ManipulationList.h>
#include <Common/ProfileEvents.h>

namespace ProfileEvents
{
extern const Event MergedRows;
extern const Event MergedUncompressedBytes;
extern const Event MergesTimeMilliseconds;
}

namespace DB
{

/* Allow to compute more accurate progress statistics */
class ColumnSizeEstimator
{
public:
    using ColumnToSize = std::map<std::string, UInt64>;

    /// Stores approximate size of columns in bytes
    /// Exact values are not required since it used for relative values estimation (progress).
    size_t sum_total = 0;
    size_t sum_index_columns = 0;
    size_t sum_ordinary_columns = 0;

    ColumnSizeEstimator(const ColumnToSize & map_, const Names & key_columns, const Names & ordinary_columns) : map(map_)
    {
        for (const auto & name : key_columns)
            if (!map.count(name))
                map[name] = 0;
        for (const auto & name : ordinary_columns)
            if (!map.count(name))
                map[name] = 0;

        for (const auto & name : key_columns)
            sum_index_columns += map.at(name);

        for (const auto & name : ordinary_columns)
            sum_ordinary_columns += map.at(name);

        sum_total = std::max(static_cast<decltype(sum_index_columns)>(1), sum_index_columns + sum_ordinary_columns);
    }

    Float64 columnWeight(const String & column) const { return static_cast<Float64>(map.at(column)) / sum_total; }

    Float64 keyColumnsWeight() const { return static_cast<Float64>(sum_index_columns) / sum_total; }

private:
    ColumnToSize map;
};

/** Progress callback.
  * What it should update:
  * - approximate progress
  * - amount of read rows
  * - various metrics
  * - time elapsed for current merge.
  */

/// Auxilliary struct that for each merge stage stores its current progress.
/// A stage is: the horizontal stage + a stage for each gathered column (if we are doing a
/// Vertical merge) or a mutation of a single part. During a single stage all rows are read.
struct MergeStageProgress
{
    MergeStageProgress(Float64 weight_) : is_first(true), weight(weight_) { }

    /// XXX: should refactor this
    MergeStageProgress(Float64 initial_progress_, Float64 weight_, bool is_first_ = false)
        : initial_progress(initial_progress_), is_first(is_first_), weight(weight_)
    {
    }

    Float64 initial_progress = 0.0;
    bool is_first;
    Float64 weight;

    UInt64 total_rows = 0;
    UInt64 rows_read = 0;
};

class ManipulationProgressCallback
{
public:
    ManipulationProgressCallback(ManipulationListElement * manipulation_entry_, UInt64 & watch_prev_elapsed_, MergeStageProgress & stage_)
        : manipulation_entry(manipulation_entry_), watch_prev_elapsed(watch_prev_elapsed_), stage(stage_)
    {
        updateWatch();
    }

    ManipulationListElement * manipulation_entry;
    UInt64 & watch_prev_elapsed;
    MergeStageProgress & stage;

    void updateWatch()
    {
        UInt64 watch_curr_elapsed = manipulation_entry->watch.elapsed();
        ProfileEvents::increment(ProfileEvents::MergesTimeMilliseconds, (watch_curr_elapsed - watch_prev_elapsed) / 1000000);
        watch_prev_elapsed = watch_curr_elapsed;
    }

    void operator()(const Progress & value)
    {
        ProfileEvents::increment(ProfileEvents::MergedUncompressedBytes, value.read_bytes);
        if (stage.is_first)
            ProfileEvents::increment(ProfileEvents::MergedRows, value.read_rows);
        updateWatch();

        manipulation_entry->bytes_read_uncompressed += value.read_bytes;
        if (stage.is_first)
            manipulation_entry->rows_read += value.read_rows;

        stage.total_rows += value.total_rows_to_read;
        stage.rows_read += value.read_rows;
        if (stage.total_rows > 0)
        {
            manipulation_entry->progress.store(
                stage.initial_progress + stage.weight * stage.rows_read / stage.total_rows, std::memory_order_relaxed);
        }
    }
};

} /// EOF
