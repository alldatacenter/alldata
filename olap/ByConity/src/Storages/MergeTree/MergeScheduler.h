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

#include <Storages/MergeTree/MergeTreeDataMergerMutator.h>
#include <Storages/MergeTree/SimpleMergeSelector.h>

#include <atomic>
#include <mutex>
#include <queue>
#include <vector>


namespace DB
{

class Context;

class MergeScheduler
{
    struct TimeoutQueue
    {
        size_t max_size = 0;
        std::mutex timeout_mutex;
        std::queue<double> timeout_queue;
        size_t last_n_timeout = 0;
        double avg_timeout_rate = 0;
        double sum_timeout_rate = 0;

        void push(const double & e)
        {
            std::lock_guard<std::mutex> lck(timeout_mutex);
            if (timeout_queue.size() == max_size)
            {
                sum_timeout_rate -= timeout_queue.front();
                timeout_queue.pop();
            }
            sum_timeout_rate += e;
            timeout_queue.push(e);
            avg_timeout_rate = sum_timeout_rate / timeout_queue.size();

            if (e == 0)
                last_n_timeout = 0;
            else
                last_n_timeout++;
            //std::cout<<"timeout_rate: "<<e<<" avg_timeout_rate: "<<avg_timeout_rate<<" last_n_timeout: "<<last_n_timeout<<std::endl;
        }

        size_t size()
        {
            std::lock_guard<std::mutex> lck(timeout_mutex);
            return timeout_queue.size();
        }
    };

    struct MergeTimeQueue
    {
        size_t max_size = 0;
        std::mutex merge_mutex;
        std::queue<double> merge_queue;
        double sum_merge_time = 0;
        double avg_merge_time = 0;

        void push(const double & e)
        {
            std::lock_guard<std::mutex> lck(merge_mutex);
            if (merge_queue.size() == max_size)
            {
                sum_merge_time -= merge_queue.front();
                merge_queue.pop();
            }
            sum_merge_time += e;
            merge_queue.push(e);
            avg_merge_time = sum_merge_time / merge_queue.size();
        }
    };

public:

    explicit MergeScheduler(const ContextPtr & context_, const size_t max_size_ = 16) :context(context_),max_queue_size(max_size_)
    {
        timeout_queue.max_size = max_queue_size;
        merge_queue.max_size = max_queue_size;
    }

    struct QueriesInfo
    {
        size_t num_of_executing_queries = 0;
        size_t num_of_insert_queries = 0;
        size_t num_of_user_queries = 0;
        size_t avg_user_query_time = 0;
        size_t num_of_timeout = 0;
        double timeout_rate = 0;
    };

    struct MergesInfo
    {
        size_t num_of_merges = 0;
        size_t num_of_parts_to_merge = 0;
        size_t avg_merge_time = 0;
        size_t total_rows_in_merge = 0;
    };

    struct PartSizeInfo
    {
        size_t total_rows = 0;
        size_t num_of_parts = 0;
        size_t total_bytes = 0;
    };

    void prepareCountMerges();
    void prepareCountQueries();
    PartSizeInfo countPartSize(const IMergeSelector::PartsRange & parts, size_t begin, size_t end);

    void getEstimatedBytes(size_t & bytes)
    {
        if (!context->getSettingsRef().enable_merge_scheduler)
            return;

        // If the current time is between expired_start_hour_to_merge and expired_end_hour_to_merge
        // lower the max_rows
        size_t max_rows = context->getSettingsRef().max_rows_to_schedule_merge;
        if (MergeScheduler::expiredUTCTime(context))
            max_rows = context->getSettingsRef().strict_rows_to_schedule_merge;

        // reset max_bytes_to_merge if max_rows changed
        if (max_rows != pre_max_rows)
        {
            max_bytes_to_merge = 0;
            pre_max_rows = max_rows;
        }
        else if (max_bytes_to_merge)
        {
            bytes = std::min(bytes, max_bytes_to_merge);
        }
    }

    void prepare();
    bool strategyOfTime();
    bool strategyOfSize(const MergeScheduler::PartSizeInfo & part_size_info, bool only_check = false);
    bool canMerge(const IMergeSelector::PartsRange & parts, size_t begin, size_t end);
    static bool expiredUTCTime(const ContextPtr & context);
    MergeTreeData::DataPartsVector getPartsForOptimize(const MergeTreeData::DataPartsVector & parts);

    static bool inMerge(const String & part_name, const ContextPtr & context);
    static bool tooManyRowsToMerge(size_t rows_to_add, const ContextPtr & context);

private:
    const ContextPtr context;
    TimeoutQueue timeout_queue;
    MergeTimeQueue merge_queue;
    size_t max_queue_size = 0;
    size_t max_bytes_to_merge = 0;
    size_t pre_max_rows = 0;
    QueriesInfo queries_info;
    MergesInfo merges_info;
    std::atomic<bool> prepared = false;
    std::mutex merge_scheduler_mutex;

    struct CandidateRange
    {
        size_t sum_rows;
        size_t sum_ages;
        size_t begin;
        size_t end;
        CandidateRange(size_t sum_rows_, size_t sum_ages_, size_t begin_, size_t end_)
            : sum_rows(sum_rows_), sum_ages(sum_ages_), begin(begin_), end(end_) {}
    };
};

}
