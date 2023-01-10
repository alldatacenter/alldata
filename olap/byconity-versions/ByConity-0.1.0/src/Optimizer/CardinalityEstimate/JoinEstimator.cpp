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

#include <Optimizer/CardinalityEstimate/JoinEstimator.h>
#include <Core/Types.h>

namespace DB
{
PlanNodeStatisticsPtr JoinEstimator::estimate(
    PlanNodeStatisticsPtr & opt_left_stats,
    PlanNodeStatisticsPtr & opt_right_stats,
    const JoinStep & join_step,
    bool enable_pk_fk,
    bool is_left_base_table,
    bool is_right_base_table)
{
    if (!opt_left_stats || !opt_right_stats)
    {
        return nullptr;
    }

    PlanNodeStatistics & left_stats = *opt_left_stats;
    PlanNodeStatistics & right_stats = *opt_right_stats;

    const Names & left_keys = join_step.getLeftKeys();
    const Names & right_keys = join_step.getRightKeys();

    ASTTableJoin::Kind kind = join_step.getKind();

    return computeCardinality(left_stats, right_stats, left_keys, right_keys, kind, enable_pk_fk, is_left_base_table, is_right_base_table);
}

PlanNodeStatisticsPtr JoinEstimator::computeCardinality(
    PlanNodeStatistics & left_stats,
    PlanNodeStatistics & right_stats,
    const Names & left_keys,
    const Names & right_keys,
    ASTTableJoin::Kind kind,
    bool enable_pk_fk,
    bool is_left_base_table,
    bool is_right_base_table)
{
    UInt64 left_rows = left_stats.getRowCount();
    UInt64 right_rows = right_stats.getRowCount();

    // init join card, and output column statistics.
    UInt64 join_card = left_rows * right_rows;
    std::unordered_map<String, SymbolStatisticsPtr> join_output_statistics;
    for (auto & item : left_stats.getSymbolStatistics())
    {
        join_output_statistics[item.first] = item.second->copy();
    }
    for (auto & item : right_stats.getSymbolStatistics())
    {
        join_output_statistics[item.first] = item.second->copy();
    }

    // cross join
    if (kind == ASTTableJoin::Kind::Cross)
    {
        for (auto & item : left_stats.getSymbolStatistics())
        {
            join_output_statistics[item.first] = item.second->applySelectivity(right_rows, 1);
        }

        for (auto & item : right_stats.getSymbolStatistics())
        {
            join_output_statistics[item.first] = item.second->applySelectivity(left_rows, 1);
        }

        return std::make_shared<PlanNodeStatistics>(join_card, join_output_statistics);
    }

    // inner/left/right/full join
    bool all_unknown_stat = true;
    for (size_t i = 0; i < left_keys.size(); ++i)
    {
        const String & left_key = left_keys.at(i);
        const String & right_key = right_keys.at(i);

        SymbolStatistics & left_key_stats = *left_stats.getSymbolStatistics(left_key);
        SymbolStatistics & right_key_stats = *right_stats.getSymbolStatistics(right_key);

        if (left_key_stats.isUnknown() || right_key_stats.isUnknown())
        {
            continue;
        }
        all_unknown_stat = false;

        UInt64 left_ndv = left_key_stats.getNdv();
        UInt64 right_ndv = right_key_stats.getNdv();

        String left_db_table_column = left_key_stats.getDbTableColumn();
        String right_db_table_column = right_key_stats.getDbTableColumn();

        // join output cardinality and join output column statistics for every join key
        UInt64 pre_key_join_card;
        std::unordered_map<String, SymbolStatisticsPtr> pre_key_join_output_statistics;

        // case 1 : left join key equals to right join key. (self-join)
        if (left_db_table_column == right_db_table_column)
        {
            pre_key_join_card = computeCardinalityByNDV(
                left_stats, right_stats, left_key_stats, right_key_stats, kind, left_key, right_key, pre_key_join_output_statistics);
        }

        // case 2 : PK join FK
        else if (enable_pk_fk && matchPKFK(left_rows, right_rows, left_ndv, right_ndv))
        {
            pre_key_join_card = computeCardinalityByFKPK(
                right_rows,
                right_ndv,
                left_ndv,
                right_stats,
                left_stats,
                right_key_stats,
                left_key_stats,
                right_key,
                left_key,
                is_right_base_table,
                is_left_base_table,
                pre_key_join_output_statistics);
        }

        // case 3 : FK join PK
        else if (enable_pk_fk && matchFKPK(left_rows, right_rows, left_ndv, right_ndv))
        {
            pre_key_join_card = computeCardinalityByFKPK(
                left_rows,
                left_ndv,
                right_ndv,
                left_stats,
                right_stats,
                left_key_stats,
                right_key_stats,
                left_key,
                right_key,
                is_left_base_table,
                is_right_base_table,
                pre_key_join_output_statistics);
        }

        // case 4 : normal join cases, with histogram exist.
        else if (!left_key_stats.getHistogram().getBuckets().empty() && !right_key_stats.getHistogram().getBuckets().empty())
        {
            pre_key_join_card = computeCardinalityByHistogram(
                left_stats, right_stats, left_key_stats, right_key_stats, kind, left_key, right_key, pre_key_join_output_statistics);
        }
        else
        {
            // case 4 : normal join cases, with histogram not exist.
            pre_key_join_card = computeCardinalityByNDV(
                left_stats, right_stats, left_key_stats, right_key_stats, kind, left_key, right_key, pre_key_join_output_statistics);
        }

        // we choose the smallest one.
        if (pre_key_join_card <= join_card)
        {
            join_card = pre_key_join_card;
            join_output_statistics = pre_key_join_output_statistics;
        }
    }

    if (all_unknown_stat)
        return nullptr;

    // Adjust the number of output rows by join kind.

    // All rows from left side should be in the result.
    if (kind == ASTTableJoin::Kind::Left)
    {
        join_card = std::max(left_rows, join_card);
    }
    // All rows from right side should be in the result.
    if (kind == ASTTableJoin::Kind::Right)
    {
        join_card = std::max(right_rows, join_card);
    }
    // T(A FOJ B) = T(A LOJ B) + T(A ROJ B) - T(A IJ B)
    if (kind == ASTTableJoin::Kind::Full)
    {
        join_card = std::max(left_rows, join_card) + std::max(right_rows, join_card) - join_card;
    }

    // normalize output column NDV
    for (auto & output_statistics : join_output_statistics)
    {
        if (output_statistics.second->getNdv() > join_card)
        {
            output_statistics.second->setNdv(join_card);
        }
    }

    // TODO@lichengxian update statistics for join filters.

    return std::make_shared<PlanNodeStatistics>(join_card, join_output_statistics);
}

bool JoinEstimator::matchPKFK(UInt64 left_rows, UInt64 right_rows, UInt64 left_ndv, UInt64 right_ndv)
{
    if (left_ndv == 0 || right_ndv == 0)
    {
        return false;
    }

    // PK-FK assumption
    bool is_left_pk = left_rows == left_ndv;

    // as NDV is calculate by hyperloglog algorithm, it is not exactly.
    bool is_left_almost_pk = false;
    if (!is_left_pk)
    {
        if (left_rows > left_ndv)
        {
            is_left_almost_pk = (double(left_ndv) / left_rows) > 0.95;
        }
        else
        {
            is_left_almost_pk = (double(left_rows) / left_ndv) > 0.95;
        }
    }
    bool is_right_fk = right_rows > right_ndv;

    return (is_left_pk || is_left_almost_pk) && is_right_fk;
}

bool JoinEstimator::matchFKPK(UInt64 left_rows, UInt64 right_rows, UInt64 left_ndv, UInt64 right_ndv)
{
    if (left_ndv == 0 || right_ndv == 0)
    {
        return false;
    }

    // FK-PK assumption
    bool is_left_fk = left_rows > left_ndv;
    bool is_right_pk = right_rows == right_ndv;

    // as NDV is calculate by hyperloglog algorithm, it is not exactly.
    bool is_right_almost_pk = false;
    if (!is_right_pk)
    {
        if (right_rows > right_ndv)
        {
            is_right_almost_pk = (double(right_ndv) / right_rows) > 0.95;
        }
        else
        {
            is_right_almost_pk = (double(right_rows) / right_ndv) > 0.95;
        }
    }

    return is_left_fk && (is_right_pk || is_right_almost_pk);
}

UInt64 JoinEstimator::computeCardinalityByFKPK(
    UInt64 fk_rows,
    UInt64 fk_ndv,
    UInt64 pk_ndv,
    PlanNodeStatistics & fk_stats,
    PlanNodeStatistics & pk_stats,
    SymbolStatistics & fk_key_stats,
    SymbolStatistics & pk_key_stats,
    String fk_key,
    String pk_key,
    bool is_fk_base_table,
    bool is_pk_base_table,
    std::unordered_map<String, SymbolStatisticsPtr> & join_output_statistics)
{
    // if FK side ndv less then FK side ndv.
    // it means all FKs can be joined, but only part of PK can be joined.

    double join_card = double(fk_rows * pk_ndv) / fk_ndv;

    if (join_card > fk_rows)
    {
        join_card = fk_rows;
    }

    if (is_fk_base_table && !is_pk_base_table)
    {
        join_card = join_card * 0.5;
    }

    // update output statistics

    // FK side all match;
    if (fk_ndv <= pk_ndv)
    {
        join_card = fk_rows;

        for (auto & item : fk_stats.getSymbolStatistics())
        {
            join_output_statistics[item.first] = item.second->copy();
        }

        // PK side partial match, adjust statistics;
        double adjust_rowcount = join_card / pk_stats.getRowCount();
        double adjust_ndv = double(fk_ndv) / pk_ndv;
        for (auto & item : pk_stats.getSymbolStatistics())
        {
            if (item.first == pk_key)
            {
                auto new_pk_key_stats = pk_key_stats.copy();
                new_pk_key_stats = new_pk_key_stats->applySelectivity(adjust_rowcount, adjust_ndv);
                new_pk_key_stats->setNdv(fk_ndv);
                join_output_statistics[item.first] = new_pk_key_stats;
            }
            else
            {
                if (double(item.second->getNdv()) / pk_stats.getRowCount() > 0.8)
                {
                    join_output_statistics[item.first] = item.second->applySelectivity(adjust_rowcount, adjust_ndv);
                }
                else
                {
                    join_output_statistics[item.first] = item.second->applySelectivity(adjust_rowcount, 1);
                }
            }
        }
    }

    // if FK side ndv large then PK side ndv.
    // it means all PKs can be joined, but only part of PK can be joined.
    if (fk_ndv > pk_ndv)
    {
        join_card = double(fk_rows * pk_ndv) / fk_ndv;

        double adjust_fk_rowcount = join_card / fk_stats.getRowCount();
        double adjust_fk_ndv = static_cast<double>(pk_ndv) / fk_ndv;
        for (auto & item : fk_stats.getSymbolStatistics())
        {
            if (item.first == fk_key)
            {
                auto new_fk_key_stats = fk_key_stats.copy();
                new_fk_key_stats = new_fk_key_stats->applySelectivity(adjust_fk_rowcount, adjust_fk_ndv);
                new_fk_key_stats->setNdv(pk_ndv);
                join_output_statistics[item.first] = new_fk_key_stats;
            }
            else
            {
                if (double(item.second->getNdv()) / fk_stats.getRowCount() > 0.8)
                {
                    join_output_statistics[item.first] = item.second->applySelectivity(adjust_fk_rowcount, adjust_fk_ndv);
                }
                else
                {
                    join_output_statistics[item.first] = item.second->applySelectivity(adjust_fk_rowcount, 1);
                }
            }
        }

        double adjust_pk_rowcount = join_card / pk_stats.getRowCount();
        for (auto & item : pk_stats.getSymbolStatistics())
        {
            join_output_statistics[item.first] = item.second->applySelectivity(adjust_pk_rowcount, 1.0);
        }
    }

    return join_card;
}

UInt64 JoinEstimator::computeCardinalityByHistogram(
    PlanNodeStatistics & left_stats,
    PlanNodeStatistics & right_stats,
    SymbolStatistics & left_key_stats,
    SymbolStatistics & right_key_stats,
    ASTTableJoin::Kind kind,
    String left_key,
    String right_key,
    std::unordered_map<String, SymbolStatisticsPtr> & join_output_statistics)
{
    // Choose the maximum of two min values, and the minimum of two max values.
    double min = std::max(left_key_stats.getMin(), right_key_stats.getMin());
    double max = std::min(left_key_stats.getMax(), right_key_stats.getMax());

    auto left_key_ndv = left_key_stats.getNdv();
    auto right_key_ndv = right_key_stats.getNdv();
    UInt64 min_ndv = std::min(left_key_ndv, right_key_ndv);

    const Histogram & left_his = left_key_stats.getHistogram();
    const Histogram & right_his = right_key_stats.getHistogram();

    Buckets join_buckets = left_his.estimateJoin(right_his, min, max);

    double join_card = 1;
    for (auto & bucket : join_buckets)
    {
        join_card += bucket->getCount();
    }

    double adjust_left_rowcount = join_card / left_stats.getRowCount();
    for (auto & item : left_stats.getSymbolStatistics())
    {
        if (item.first == left_key)
        {
            auto new_left_key_stats = left_key_stats.createJoin(join_buckets);
            if (kind == ASTTableJoin::Kind::Inner)
            {
                new_left_key_stats->setNdv(min_ndv);
            }
            join_output_statistics[left_key] = new_left_key_stats;
        }
        else
        {
            join_output_statistics[item.first] = item.second->applySelectivity(adjust_left_rowcount, 1);
        }
    }

    double adjust_right_rowcount = join_card / right_stats.getRowCount();
    for (auto & item : right_stats.getSymbolStatistics())
    {
        if (item.first == right_key)
        {
            auto new_right_key_stats = right_key_stats.createJoin(join_buckets);
            if (kind == ASTTableJoin::Kind::Inner)
            {
                new_right_key_stats->setNdv(min_ndv);
            }
            join_output_statistics[right_key] = new_right_key_stats;
        }
        else
        {
            join_output_statistics[item.first] = item.second->applySelectivity(adjust_right_rowcount, 1);
        }
    }

    return join_card;
}

UInt64 JoinEstimator::computeCardinalityByNDV(
    PlanNodeStatistics & left_stats,
    PlanNodeStatistics & right_stats,
    SymbolStatistics & left_key_stats,
    SymbolStatistics & right_key_stats,
    ASTTableJoin::Kind kind,
    String left_key,
    String right_key,
    std::unordered_map<String, SymbolStatisticsPtr> & join_output_statistics)
{
    UInt64 multiply_card = left_stats.getRowCount() * right_stats.getRowCount();
    UInt64 max_ndv = std::max(left_key_stats.getNdv(), right_key_stats.getNdv());
    UInt64 min_ndv = std::min(left_key_stats.getNdv(), right_key_stats.getNdv());

    double join_card = max_ndv == 0 ? 1 : multiply_card / max_ndv;

    double adjust_left_rowcount = join_card / left_stats.getRowCount();
    for (auto & item : left_stats.getSymbolStatistics())
    {
        join_output_statistics[item.first] = item.second->applySelectivity(adjust_left_rowcount, 1.0);
        if (item.first == left_key && kind == ASTTableJoin::Kind::Inner)
        {
            join_output_statistics[item.first]->setNdv(min_ndv);
        }
    }
    double adjust_right_rowcount = join_card / right_stats.getRowCount();
    for (auto & item : right_stats.getSymbolStatistics())
    {
        join_output_statistics[item.first] = item.second->applySelectivity(adjust_right_rowcount, 1.0);
        if (item.first == right_key && kind == ASTTableJoin::Kind::Inner)
        {
            join_output_statistics[item.first]->setNdv(min_ndv);
        }
    }
    return join_card;
}

}
