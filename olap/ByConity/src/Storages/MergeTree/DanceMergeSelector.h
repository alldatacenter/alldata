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

#include <Core/BaseSettings.h>
#include <Storages/MergeTree/MergeSelector.h>
#include <Poco/Util/AbstractConfiguration.h>

namespace DB
{
class MergeScheduler;
class MergeTreeMetaBase;

#define LIST_OF_DANCE_MERGE_SELECTOR_SETTINGS(M) \
    M(Bool, enable_batch_select, false, "", 0) \
    M(UInt64, max_parts_to_merge_base, 100, "", 0) \
    M(UInt64, min_parts_to_merge_base, 5, "", 0) \
\
    M(UInt64, size_fixed_cost_to_add, 5 * 1024 * 1024, "", 0) \
    M(Float, score_count_exp, 1.15, "", 0) \
\
    M(Bool, enable_heuristic_to_align_parts, true, "", 0) \
    M(Float, heuristic_to_align_parts_min_ratio_of_sum_size_to_prev_part, 0.9, "", 0) \
    M(Float, heuristic_to_align_parts_max_absolute_difference_in_powers_of_two, 0.5, "", 0) \
    M(Float, heuristic_to_align_parts_max_score_adjustment, 0.75, "", 0) \
\
    M(Bool, enable_penalty_for_small_parts_at_right, true, "", 0) \
    M(Float, enable_penalty_for_small_parts_at_right_ratio_base, 0.1, "", 0) \
    M(Float, score_penalty_for_small_parts_at_right, 1.15, "", 0) \
\
    M(UInt64, min_parts_to_enable_multi_selection, 200, "", 0) \
    /** Unique table will set it to a value <= 2^32 in order to prevent rowid(UInt32) overflow */ \
    /** Too large part has no advantage since we cannot utilize parallelism. We set max_total_rows_to_merge as 2147483647 **/ \
    M(UInt64, max_total_rows_to_merge, 2147483647, "", 0) \
\
    M(UInt64, max_parts_to_break, 10000, "", 0)

DECLARE_SETTINGS_TRAITS(DanceMergeSelectorSettingsTraits, LIST_OF_DANCE_MERGE_SELECTOR_SETTINGS)


class DanceMergeSelectorSettings : public BaseSettings<DanceMergeSelectorSettingsTraits>
{
public:
    void loadFromConfig(const Poco::Util::AbstractConfiguration & config);
};

class DanceMergeSelector : public IMergeSelector
{
public:
    using Iterator = PartsRange::const_iterator;
    using Settings = DanceMergeSelectorSettings;

    DanceMergeSelector(const MergeTreeMetaBase & data_, const Settings & settings_) : data(data_), settings(settings_)
    {
        best_ranges["all"].push_back(BestRangeWithScore{});
    }

    PartsRange select(const PartsRanges & parts_ranges, const size_t max_total_size_to_merge, MergeScheduler * merge_scheduler = nullptr) override;
    PartsRanges selectMulti(const PartsRanges & parts_ranges, const size_t max_total_size_to_merge, MergeScheduler * merge_scheduler = nullptr) override;

    struct BestRangeWithScore
    {
        double min_score = std::numeric_limits<double>::max();
        Iterator best_begin;
        Iterator best_end;

        bool valid() const { return min_score != std::numeric_limits<double>::max(); }

        void update(double score, Iterator begin, Iterator end)
        {
            if (score < min_score)
            {
                min_score = score;
                best_begin = begin;
                best_end = end;
            }
        }
    };

private:
    void selectWithinPartition(const PartsRange & parts, const size_t max_total_size_to_merge, MergeScheduler * merge_scheduler = nullptr);
    bool allow(double sum_size, double max_size, double min_age, double range_size);

    [[maybe_unused]] const MergeTreeMetaBase & data;
    const Settings settings;

    std::unordered_map<String, size_t> num_parts_of_partitions;
    std::unordered_map<String, std::vector<BestRangeWithScore>> best_ranges;

    void selectRangesFromScoreTable(const PartsRange & parts, const std::vector<std::vector<double>> & score_table, size_t i, size_t j, size_t n, size_t max_width, std::vector<BestRangeWithScore> & out);

    inline bool enable_batch_select_for_partition(const String & partition_id)
    {
        return settings.enable_batch_select && !is_small_partition(partition_id);
    }

    inline bool is_small_partition(const String & partition_id)
    {
        return num_parts_of_partitions[partition_id] < settings.min_parts_to_enable_multi_selection;
    }

    inline size_t expected_ranges_num(const size_t num_parts)
    {
        return (num_parts / settings.max_parts_to_merge_base) + 3;
    }
};

}
