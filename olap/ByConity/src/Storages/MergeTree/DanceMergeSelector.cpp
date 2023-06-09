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

#include <Storages/MergeTree/DanceMergeSelector.h>

#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/MergeScheduler.h>
#include <Common/Exception.h>
#include <Common/interpolate.h>

#include <cmath>

namespace DB
{
namespace ErrorCodes
{
    extern const int UNKNOWN_SETTING;
}

IMPLEMENT_SETTINGS_TRAITS(DanceMergeSelectorSettingsTraits, LIST_OF_DANCE_MERGE_SELECTOR_SETTINGS)

void DanceMergeSelectorSettings::loadFromConfig(const Poco::Util::AbstractConfiguration & config)
{
    static std::string config_elem = "dance_merge_selector";
    if (!config.has(config_elem))
        return;

    Poco::Util::AbstractConfiguration::Keys config_keys;
    config.keys(config_elem, config_keys);

    try
    {
        for (auto & key : config_keys)
            set(key, config.getString(config_elem + "." + key));
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::UNKNOWN_SETTING)
            e.addMessage("in DanceMergeSelector config");
        throw;
    }
}


inline auto toPart(const void * data)
{
    return *static_cast<const std::shared_ptr<IMergeTreeDataPart> *>(data);
}

static double score(double count, double sum_size, double sum_size_fixed_cost, double count_exp)
{
    return (sum_size + sum_size_fixed_cost * count) / pow(count - 1.9, count_exp);
}

static double mapPiecewiseLinearToUnit(double value, double min, double max)
{
    return value <= min ? 0 : (value >= max ? 1 : ((value - min) / (max - min)));
}

IMergeSelector::PartsRange DanceMergeSelector::select(const PartsRanges & partitions, const size_t max_total_size_to_merge, MergeScheduler * merge_scheduler)
{
    if (settings.enable_batch_select)
    {
        LOG_ERROR(&Poco::Logger::get("DanceMergeSelector"), "Calling select() with enable_batch_select=1 is not supported!");
        return {};
    }

    for (auto & partition : partitions)
    {
        if (partition.size() >= 2)
            num_parts_of_partitions[toPart(partition.front().data)->info.partition_id] += partition.size();
    }

    for (auto & partition : partitions)
    {
        if (partition.size() >= 2)
            selectWithinPartition(partition, max_total_size_to_merge, merge_scheduler);
    }

    BestRangeWithScore res{};
    for (const auto & [_, ranges_in_partition] : best_ranges)
    {
        if (!ranges_in_partition.empty())
        {
            auto & range = ranges_in_partition.front();
            res.update(range.min_score, range.best_begin, range.best_end);
        }
    }

    if (res.valid())
        return PartsRange(res.best_begin, res.best_end);
    return {};
}

IMergeSelector::PartsRanges DanceMergeSelector::selectMulti(const PartsRanges & partitions, const size_t max_total_size_to_merge, MergeScheduler * merge_scheduler)
{
    for (auto & partition : partitions)
    {
        if (partition.size() >= 2)
            num_parts_of_partitions[toPart(partition.front().data)->info.partition_id] += partition.size();
    }

    for (auto & partition : partitions)
    {
        if (partition.size() >= 2)
            selectWithinPartition(partition, max_total_size_to_merge, merge_scheduler);
    }

    std::vector<BestRangeWithScore *> range_vec;
    for (auto & [_, ranges] : best_ranges)
    {
        for (auto & range : ranges)
        {
            if (range.valid())
                range_vec.push_back(&range);
        }
    }
    std::sort(range_vec.begin(), range_vec.end(), [](auto & lhs, auto & rhs) { return lhs->min_score < rhs->min_score; });

    PartsRanges res;
    for (auto & range : range_vec)
        res.push_back(PartsRange(range->best_begin, range->best_end));

    return res;
}

/**
 * @brief scan range [i, j] of score_table and select at most n ranges ordered by score.
 *
 * @param parts source data parts.
 * @param score_table the score table.
 * @param i begin position (inclusive)
 * @param j end position (inclusive)
 * @param num_max_out the max size of the result vector.
 * @param max_width the max length of a result range.
 * @param out output collector.
 */
void DanceMergeSelector::selectRangesFromScoreTable(
    const PartsRange & parts,
    const std::vector<std::vector<double>> & score_table,
    size_t i,
    size_t j,
    size_t num_max_out,
    size_t max_width,
    std::vector<BestRangeWithScore> & out)
{
    if (i >= j || out.size() >= num_max_out)
        return;
    double min_score = std::numeric_limits<double>::max();
    size_t min_i = 0, min_j = 0;

    for (auto m = i; m <= j; m++)
    {
        if (m >= score_table.size())
            break;

        for (auto n = m + 1; n <= j && n - m + 1 <= max_width; n++)
        {
            if (n - m + 1 >= score_table[m].size())
                break;

            if (score_table[m][n - m + 1] < min_score)
            {
                min_score = score_table[m][n - m + 1];
                min_i = m;
                min_j = n;
            }
        }
    }
    if (min_score == std::numeric_limits<double>::max())
        return;

    BestRangeWithScore range{};
    range.update(min_score, parts.begin() + min_i, parts.begin() + min_j + 1);
    out.push_back(range);

    if (min_i > i + 1)
        selectRangesFromScoreTable(parts, score_table, i, min_i - 1, num_max_out, max_width, out);

    if (min_j < j - 1)
        selectRangesFromScoreTable(parts, score_table, min_j + 1, j, num_max_out, max_width, out);
}

void DanceMergeSelector::selectWithinPartition(const PartsRange & parts, const size_t max_total_size_to_merge, [[maybe_unused]] MergeScheduler * merge_scheduler)
{
    if (parts.size() <= 1)
        return;

    BestRangeWithScore best_range;

    /// If there are too many parts, limit the max_begin and max_end.
    size_t max_parts_to_break = settings.max_parts_to_break;
    size_t max_begin = parts.size() < max_parts_to_break ? parts.size() - 1 : max_parts_to_break - 1;
    size_t max_end = max_begin + 1;

    auto & partition_id = toPart(parts.front().data)->info.partition_id;
    bool enable_batch_select = enable_batch_select_for_partition(partition_id);

    /// score_table[i][j] means begin with i and length is j --> range [i, i + j - 1]
    std::vector<std::vector<double>> score_table;
    size_t max_parts_to_merge = settings.max_parts_to_merge_base;
    if (enable_batch_select)
    {
        for (size_t i = 0; i <= max_begin; i++)
            score_table.emplace_back(std::vector<double>(max_parts_to_merge + 1, std::numeric_limits<double>::max()));
    }

    for (size_t begin = 0; begin < max_begin; ++begin)
    {
        if (!parts[begin].shall_participate_in_merges)
            continue;

        size_t sum_size = parts[begin].size;
        size_t sum_rows = parts[begin].rows;
        size_t min_age = parts[begin].age;
        size_t max_size = parts[begin].size;

        for (size_t end = begin + 2; end <= max_end; ++end)
        {
            if (end - begin > max_parts_to_merge)
                break;

            if (!parts[end - 1].shall_participate_in_merges)
                /// TODO(zuochuang.zema): need to set begin to end ?
                break;

            size_t cur_size = parts[end - 1].size;
            size_t cur_rows = parts[end - 1].rows;
            size_t cur_age = parts[end - 1].age;

            sum_size += cur_size;
            sum_rows += cur_rows;
            min_age = std::min(min_age, cur_age);
            max_size = std::max(max_size, cur_size);

            /// LOG_TRACE(data.getLogger(), "begin " << toPart(parts[end-1].data)->name << " size " << sum_size << " rows " << sum_rows);

            if (settings.max_total_rows_to_merge && sum_rows > settings.max_total_rows_to_merge)
                break;

            if (max_total_size_to_merge && sum_size > max_total_size_to_merge)
                break;

            if (!allow(sum_size, max_size, min_age, end - begin))
                continue;

            double current_score = score(end - begin, sum_size, settings.size_fixed_cost_to_add, settings.score_count_exp);

            size_t size_prev_at_left = begin == 0 ? 0 : parts[begin - 1].size;
            if (settings.enable_heuristic_to_align_parts
                && size_prev_at_left > sum_size * settings.heuristic_to_align_parts_min_ratio_of_sum_size_to_prev_part)
            {
                double difference = std::abs(log2(static_cast<double>(sum_size) / size_prev_at_left));
                if (difference < settings.heuristic_to_align_parts_max_absolute_difference_in_powers_of_two)
                    current_score *= interpolateLinear(
                        settings.heuristic_to_align_parts_max_score_adjustment,
                        1,
                        difference / settings.heuristic_to_align_parts_max_absolute_difference_in_powers_of_two);
            }

            if (end - begin >= 10
                && parts[end - 1].size < (0.001 + settings.enable_penalty_for_small_parts_at_right_ratio_base / (end - begin)))
                current_score *= settings.score_penalty_for_small_parts_at_right;

            best_range.update(current_score, parts.begin() + begin, parts.begin() + end);
            if (enable_batch_select)
                score_table[begin][end - begin] = current_score;
        }
    }

    /// If batch_select is disabled, then only track one best range for each partition.
    if (!enable_batch_select)
    {
        if (!best_range.valid())
            return;

        if (is_small_partition(partition_id))
            best_ranges["all"].front().update(best_range.min_score, best_range.best_begin, best_range.best_end);
        else
        {
            if (best_ranges[partition_id].empty())
                best_ranges[partition_id].push_back({});
            best_ranges[partition_id].front().update(best_range.min_score, best_range.best_begin, best_range.best_end);
        }
        return;
    }

    /// If batch_select is enabled, then get a bundle of ranges from score_table for each partition.
    size_t num_expected_ranges = expected_ranges_num(max_begin);
    std::vector<BestRangeWithScore> res_ranges;
    if (best_range.valid())
    {
        res_ranges.emplace_back(best_range);
        size_t begin = best_range.best_begin - parts.begin();
        size_t end = best_range.best_end - parts.begin() - 1;
        if (begin > 1)
            selectRangesFromScoreTable(parts, score_table, 0, begin - 1, num_expected_ranges, max_parts_to_merge, res_ranges);

        if (end + 2 < max_end)
            selectRangesFromScoreTable(parts, score_table, end + 1, max_end - 1, num_expected_ranges, max_parts_to_merge, res_ranges);
    }

    for (const auto & range : res_ranges)
    {
        if (is_small_partition(partition_id))
            best_ranges["all"].front().update(range.min_score, range.best_begin, range.best_end);
        else
            best_ranges[partition_id].emplace_back(range);
    }
}

bool DanceMergeSelector::allow(double sum_size, double max_size, double min_age, double range_size)
{
    static size_t min_size_to_lower_base_log = log1p(1024 * 1024);
    static size_t max_size_to_lower_base_log = log1p(100ULL * 1024 * 1024 * 1024);

    constexpr time_t min_age_to_lower_base_at_min_size = 10;
    constexpr time_t min_age_to_lower_base_at_max_size = 10;
    constexpr time_t max_age_to_lower_base_at_min_size = 3600;
    constexpr time_t max_age_to_lower_base_at_max_size = 30 * 86400;

    /// Map size to 0..1 using logarithmic scale
    double size_normalized = mapPiecewiseLinearToUnit(log1p(sum_size), min_size_to_lower_base_log, max_size_to_lower_base_log);

    //    std::cerr << "size_normalized: " << size_normalized << "\n";

    /// Calculate boundaries for age
    double min_age_to_lower_base = interpolateLinear(min_age_to_lower_base_at_min_size, min_age_to_lower_base_at_max_size, size_normalized);
    double max_age_to_lower_base = interpolateLinear(max_age_to_lower_base_at_min_size, max_age_to_lower_base_at_max_size, size_normalized);

    //    std::cerr << "min_age_to_lower_base: " << min_age_to_lower_base << "\n";
    //    std::cerr << "max_age_to_lower_base: " << max_age_to_lower_base << "\n";

    /// Map age to 0..1
    double age_normalized = mapPiecewiseLinearToUnit(min_age, min_age_to_lower_base, max_age_to_lower_base);

    //    std::cerr << "age: " << min_age << "\n";
    //    std::cerr << "age_normalized: " << age_normalized << "\n";

    double combined_ratio = std::min(1.0, age_normalized);

    //    std::cerr << "combined_ratio: " << combined_ratio << "\n";

    double lowered_base = interpolateLinear(settings.min_parts_to_merge_base, 2.0, combined_ratio);

    //    std::cerr << "------- lowered_base: " << lowered_base << "\n";

    return (sum_size + range_size * settings.size_fixed_cost_to_add) / (max_size + settings.size_fixed_cost_to_add) >= lowered_base;

}

}
