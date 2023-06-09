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

#include <Storages/MergeTree/MergeScheduler.h>
#include <Storages/MergeTree/MergeList.h>
#include <Interpreters/Context.h>
#include <Interpreters/ProcessList.h>
#include <Common/StringUtils/StringUtils.h>

namespace DB
{

void MergeScheduler::prepareCountQueries()
{
    ProcessList::Info info = context->getProcessList().getInfo(false, false, false);
    queries_info.num_of_executing_queries = info.size();
    size_t critical = context->getSettingsRef().slow_query_ms;
    for (const auto & process : info)
    {
        // Get query in lower case
        String low_query = Poco::toLower(process.query);
        size_t pos = 0;
        parseSlowQuery(low_query, pos);
        if (pos != std::string::npos || low_query[pos] != '/')
            low_query = low_query.substr(pos);

        if (startsWith(low_query, "insert"))
            ++queries_info.num_of_insert_queries;
        else if (startsWith(low_query, "select"))
        {
            if (low_query.find("system.") == std::string::npos)
            {
                ++queries_info.num_of_user_queries;
                queries_info.avg_user_query_time += process.elapsed_seconds;
                if (critical > 0 && (process.elapsed_seconds * 1000) > critical)
                    ++queries_info.num_of_timeout;
            }
        }
    }

    if (queries_info.num_of_user_queries != 0)
    {
        queries_info.timeout_rate = static_cast<double>(queries_info.num_of_timeout)/queries_info.num_of_user_queries;
        queries_info.avg_user_query_time /= queries_info.num_of_user_queries;
    }
}

void MergeScheduler::prepareCountMerges()
{
    auto merges = context->getMergeList().get();
    merges_info.num_of_merges = merges.size();
    for (const auto & merge : merges)
    {
        merges_info.num_of_parts_to_merge += merge.num_parts;
        merges_info.total_rows_in_merge += merge.rows_read;
        merges_info.avg_merge_time += merge.elapsed;
    }

    if (merges_info.num_of_merges != 0)
        merges_info.avg_merge_time /= merges_info.num_of_merges;
}

MergeScheduler::PartSizeInfo MergeScheduler::countPartSize(const SimpleMergeSelector::PartsRange & parts, size_t begin, size_t end)
{
    PartSizeInfo part_size_info;

    for (size_t i = begin; i < end; ++i)
    {
        const MergeTreeData::DataPartPtr & part = *static_cast<const MergeTreeData::DataPartPtr *>(parts[i].data);
        part_size_info.total_rows += part->rows_count;
        part_size_info.total_bytes += part->getBytesOnDisk();
    }

    part_size_info.num_of_parts = end - begin;
    return part_size_info;
}

// A heuristic algorithm for controlling the merge rate.
// There has a queue storing at most 'max_size' merge try.
// We compute average timeout rate when there is a merge try.
// 1. The merge is allowed if avg_timeout_rate is zero.
// 2. If the current timeout rate is zero and no merge is executing, merge is allowed since
// there may be the case the previous bad status has gone but no merge has a try for a long time
// so that the timeout_queue has not updated.
// 3. If an insert is executed, merge_scheduler always returns true
bool MergeScheduler::strategyOfTime()
{
    // has_insert
    if (queries_info.num_of_insert_queries)
        return true;

    if (timeout_queue.avg_timeout_rate != 0)
    {
        return timeout_queue.last_n_timeout == 0 && merges_info.num_of_merges == 0;
    }
    else
        return true;
}

// Too large part will not be merged. How to define a large part is not determined
// only_check = ture, means check previous merge log can execute after enable merge_schedule
bool MergeScheduler::strategyOfSize(const MergeScheduler::PartSizeInfo & part_size_info, bool only_check)
{
    // If the current time is between expired_start_hour_to_merge and expired_end_hour_to_merge
    // lower the max_rows
    size_t max_rows = context->getSettingsRef().max_rows_to_schedule_merge;
    size_t total_rows = context->getSettingsRef().total_rows_to_schedule_merge;
    if (MergeScheduler::expiredUTCTime(context))
        max_rows = context->getSettingsRef().strict_rows_to_schedule_merge;

    // reset max_bytes_to_merge if max_rows changed
    if (max_rows != pre_max_rows)
    {
        max_bytes_to_merge = 0;
        pre_max_rows = max_rows;
    }

    if (!only_check && total_rows > 0)
    {
        if (merges_info.total_rows_in_merge + part_size_info.total_rows > total_rows)
        {
            return false;
        }
    }

    size_t small_part = max_rows / 9;
    size_t medium_part = max_rows / 5;
    size_t large_part = max_rows / 2;
    size_t denied_part = max_rows * 1.5;

    // First denied merge will not directly return false since we need use this part to estimate max_bytes_to_merge
    // The subsequence denied merge will be blocked and update max_bytes_to_merge. In addition, the estimized max_bytes_to_merge will used
    // to guide part selection in MergeTreeDataMergerMutator
    if (part_size_info.total_rows > denied_part)
    {
        if (max_bytes_to_merge == 0 || part_size_info.total_bytes <= max_bytes_to_merge)
        {
            max_bytes_to_merge = part_size_info.total_bytes;
        }
        // Too large part will not be able to merge
        return false;
    }

    if (only_check || part_size_info.total_rows < small_part)
        return true;

    if (part_size_info.total_rows < medium_part)
        return strategyOfTime();

    // low_priority merge has a strict condition to trigger.
    // 1. no timeout query
    // 2. no merges at the moment
    if (part_size_info.total_rows < large_part)
    {
        return timeout_queue.avg_timeout_rate == 0 && merges_info.num_of_merges == 0;
    }

    // delay merges which are larger than max row to merge
    // delay strategy is simple:
    // 1. no timeout query
    // 2. last 16 merge tries have failed
    // 3. no merges at the moment
    return timeout_queue.avg_timeout_rate == 0 && merge_queue.avg_merge_time == 0;
}

void MergeScheduler::prepare()
{
    if (!context->getSettingsRef().enable_merge_scheduler)
        return;

    prepareCountQueries();
    prepareCountMerges();

    prepared = true;
}

bool MergeScheduler::canMerge(const SimpleMergeSelector::PartsRange & parts, size_t begin, size_t end)
{
    if (!context->getSettingsRef().enable_merge_scheduler)
        return true;

    if (!prepared)
        prepare();

    PartSizeInfo part_size_info = countPartSize(parts, begin, end);

    timeout_queue.push(queries_info.timeout_rate);
    merge_queue.push(merges_info.avg_merge_time);

    return strategyOfSize(part_size_info);
}

bool MergeScheduler::expiredUTCTime(const ContextPtr & context)
{
    const DateLUTImpl & date_lut = DateLUT::instance("UTC");
    time_t now_time = time(nullptr);
    UInt64 now_hour = date_lut.toHour(now_time);
    UInt64 expired_start_hour = context->getSettingsRef().expired_start_hour_to_merge;
    UInt64 expired_end_hour = context->getSettingsRef().expired_end_hour_to_merge;
    return now_hour >= expired_start_hour && now_hour < expired_end_hour;
}

MergeTreeData::DataPartsVector MergeScheduler::getPartsForOptimize(const MergeTreeData::DataPartsVector & parts)
{
    if (!context->getSettingsRef().enable_merge_scheduler)
        return parts;
    // If the current time is between expired_start_hour_to_merge and expired_end_hour_to_merge
    // lower the max_rows
    size_t max_rows = context->getSettingsRef().max_rows_to_schedule_merge;
    if (MergeScheduler::expiredUTCTime(context))
        max_rows = context->getSettingsRef().strict_rows_to_schedule_merge;

    size_t parts_count = parts.size();
    if (parts_count <= 1)
        return parts;

    size_t max_parts = context->getSettingsRef().max_parts_to_optimize;

    std::vector<MergeScheduler::CandidateRange> candidate_range;

    for (size_t begin = 0; begin < parts_count; ++begin)
    {
        size_t sum_size = parts[begin]->rows_count;
        size_t sum_age = parts[begin]->info.level;

        for (size_t end = begin + 2; end <= parts_count && end - begin <= max_parts; ++end)
        {
            size_t cur_size = parts[end - 1]->rows_count;
            if (sum_size + cur_size > max_rows)
            {
                candidate_range.emplace_back(sum_size, sum_age, begin, end - 1);
                break;
            }
            sum_size += cur_size;
            sum_age += parts[end - 1]->info.level;
        }
    }

    if (candidate_range.empty())
        return parts;

    size_t best = 0;
    MergeScheduler::CandidateRange first_range = candidate_range.front();
    double minimum = static_cast<double>(first_range.sum_ages) / (first_range.end - first_range.begin);

    for (size_t i = 1; i < candidate_range.size(); ++i)
    {
        MergeScheduler::CandidateRange range = candidate_range[i];
        double avg_age = static_cast<double>(range.sum_ages) / (range.end - range.begin);
        if (avg_age <= minimum && (range.end - range.begin) > 1 && (range.end - range.begin) <= max_parts)
        {
            best = i;
            minimum = avg_age;
        }
    }

    MergeScheduler::CandidateRange range = candidate_range.at(best);
    MergeTreeData::DataPartsVector res_parts(parts.begin() + range.begin, parts.begin() + range.end);
    return res_parts;
}

bool MergeScheduler::inMerge(const String & part_name, const ContextPtr & context)
{
    auto merges = context->getMergeList().get();
    for (const auto & merge : merges)
    {
        for (const auto & part_field : merge.source_part_names)
        {
            if (part_name == part_field.get<std::string>())
                return true;
        }
    }
    return false;
}

bool MergeScheduler::tooManyRowsToMerge(size_t rows_to_add, const ContextPtr & context)
{
    auto merges = context->getMergeList().get();
    size_t total_rows_to_merge = context->getSettingsRef().total_rows_to_schedule_merge;

    if (total_rows_to_merge == 0)
        return false;

    size_t sum_rows = 0;
    for (const auto & merge : merges)
    {
        sum_rows += merge.rows_read;
        if (sum_rows + rows_to_add > total_rows_to_merge)
            return true;
    }
    return false;
}

}
