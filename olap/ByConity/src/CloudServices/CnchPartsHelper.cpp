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

#include <CloudServices/CnchPartsHelper.h>

#include <Catalog/DataModelPartWrapper.h>
#include <Interpreters/Context.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>

#include <sstream>

namespace DB::CnchPartsHelper
{
LoggingOption getLoggingOption(const Context & c)
{
    return (c.getSettingsRef().send_logs_level == LogsLevel::none) ? DisableLogging : EnableLogging;
}

IMergeTreeDataPartsVector toIMergeTreeDataPartsVector(const MergeTreeDataPartsCNCHVector & vec)
{
    IMergeTreeDataPartsVector res;
    res.reserve(vec.size());
    for (auto & p : vec)
        res.push_back(p);
    return res;
}

MergeTreeDataPartsCNCHVector toMergeTreeDataPartsCNCHVector(const IMergeTreeDataPartsVector & vec)
{
    MergeTreeDataPartsCNCHVector res;
    res.reserve(vec.size());
    for (auto & p : vec)
        res.push_back(dynamic_pointer_cast<const MergeTreeDataPartCNCH>(p));
    return res;
}

namespace
{
    template <class T>
    std::string partsToDebugString(const std::vector<T> & parts)
    {
        std::ostringstream oss;
        for (auto & p : parts)
            oss << p->get_name() << " d=" << p->get_deleted() << " p=" << bool(p->get_info().hint_mutation)
                << " h=" << p->get_info().hint_mutation << '\n';
        return oss.str();
    }

    template <class T>
    struct PartComparator
    {
        bool operator()(const T & lhs, const T & rhs) const
        {
            auto & l = lhs->get_info();
            auto & r = rhs->get_info();
            return std::forward_as_tuple(l.partition_id, l.min_block, l.max_block, l.level, lhs->get_commit_time(), l.storage_type)
                < std::forward_as_tuple(r.partition_id, r.min_block, r.max_block, r.level, rhs->get_commit_time(), r.storage_type);
        }
    };

    /** Cnch parts classification:
     *  all parts:
     *  1) invisible parts
     *      a) covered by new version parts
     *      b) covered by DROP_RANGE  (invisible_dropped_parts)
     *  2) visible parts
     *      a) with data
     *      b) without data, i.e. DROP_RANGE
     *          i) alone  (visible_alone_drop_ranges)
     *          ii) not alone
     */

    template <class Vec>
    Vec calcVisiblePartsImpl(
        Vec & all_parts,
        bool flatten,
        bool skip_drop_ranges,
        Vec * visible_alone_drop_ranges,
        Vec * invisible_dropped_parts,
        LoggingOption logging)
    {
        using Part = typename Vec::value_type;

        Vec visible_parts;

        if (all_parts.empty())
            return visible_parts;

        if (all_parts.size() == 1)
        {
            if (skip_drop_ranges && all_parts.front()->get_deleted())
                ; /// do nothing
            else
                visible_parts = all_parts;

            if (visible_alone_drop_ranges && all_parts.front()->get_deleted())
                *visible_alone_drop_ranges = all_parts;
            return visible_parts;
        }

        std::sort(all_parts.begin(), all_parts.end(), PartComparator<Part>{});

        /// One-pass algorithm to construct delta chains
        auto prev_it = all_parts.begin();
        auto curr_it = std::next(prev_it);

        while (prev_it != all_parts.end())
        {
            auto & prev_part = *prev_it;

            /// 1. prev_part is a DROP RANGE mark
            if (prev_part->get_info().level == MergeTreePartInfo::MAX_LEVEL)
            {
                /// a. curr_part is in same partition
                if (curr_it != all_parts.end() && prev_part->get_info().partition_id == (*curr_it)->get_info().partition_id)
                {
                    /// i) curr_part is also a DROP RANGE mark, and must be the bigger one
                    if ((*curr_it)->get_info().level == MergeTreePartInfo::MAX_LEVEL)
                    {
                        if (invisible_dropped_parts)
                            invisible_dropped_parts->push_back(*prev_it);

                        if (visible_alone_drop_ranges)
                        {
                            (*prev_it)->setPreviousPart(nullptr); /// reset whatever
                            (*curr_it)->setPreviousPart(*prev_it); /// set previous part for visible_alone_drop_ranges
                        }

                        prev_it = curr_it;
                        ++curr_it;
                        continue;
                    }
                    /// ii) curr_part is marked as dropped by prev_part
                    else if ((*curr_it)->get_info().max_block <= prev_part->get_info().max_block)
                    {
                        if (invisible_dropped_parts)
                            invisible_dropped_parts->push_back(*curr_it);

                        if (visible_alone_drop_ranges)
                            prev_part->setPreviousPart(*curr_it); /// set previous part for visible_alone_drop_ranges

                        ++curr_it;
                        continue;
                    }
                }

                /// a. iii) [fallthrough] same partition, but curr_part is a new part with data after the DROP RANGE mark

                /// b) curr_it is in the end
                /// c) different partition

                if (skip_drop_ranges)
                    ; /// do nothing
                else
                    visible_parts.push_back(prev_part);

                if (visible_alone_drop_ranges && !prev_part->tryGetPreviousPart())
                    visible_alone_drop_ranges->push_back(prev_part);
                prev_part->setPreviousPart(nullptr);
            }
            /// 2. curr_part contains the prev_part
            else if (curr_it != all_parts.end() && (*curr_it)->containsExactly(*prev_part))
            {
                (*curr_it)->setPreviousPart(prev_part);
            }
            /// 3. curr_it is in the end
            /// 4. curr_part is not related to the prev_part which means prev_part must be visible
            else
            {
                if (skip_drop_ranges && prev_part->get_deleted())
                    ; /// do nothing
                else
                    visible_parts.push_back(prev_part);

                if (visible_alone_drop_ranges && !prev_part->tryGetPreviousPart() && prev_part->get_deleted())
                    visible_alone_drop_ranges->push_back(prev_part);
            }

            prev_it = curr_it;
            if (curr_it != all_parts.end())
                ++curr_it;
        }

        if (flatten)
            flattenPartsVector(visible_parts);

        if (logging == EnableLogging)
        {
            auto log = &Poco::Logger::get(__func__);
            LOG_DEBUG(log, "all_parts:\n {}", partsToDebugString(all_parts));
            LOG_DEBUG(log, "visible_parts (skip_drop_ranges={}):\n{}", skip_drop_ranges, partsToDebugString(visible_parts));
            if (visible_alone_drop_ranges)
                LOG_DEBUG(log, "visible_alone_drop_ranges:\n{}", partsToDebugString(*visible_alone_drop_ranges));
            if (invisible_dropped_parts)
                LOG_DEBUG(log, "invisible_dropped_parts:\n{}", partsToDebugString(*invisible_dropped_parts));
        }

        return visible_parts;
    }

} /// end of namespace

MergeTreeDataPartsVector calcVisibleParts(MergeTreeDataPartsVector & all_parts, bool flatten, LoggingOption logging)
{
    return calcVisiblePartsImpl<MergeTreeDataPartsVector>(all_parts, flatten, /* skip_drop_ranges */ true, nullptr, nullptr, logging);
}

ServerDataPartsVector calcVisibleParts(ServerDataPartsVector & all_parts, bool flatten, LoggingOption logging)
{
    return calcVisiblePartsImpl<ServerDataPartsVector>(all_parts, flatten, /* skip_drop_ranges */ true, nullptr, nullptr, logging);
}

MergeTreeDataPartsCNCHVector calcVisibleParts(MergeTreeDataPartsCNCHVector & all_parts, bool flatten, LoggingOption logging)
{
    return calcVisiblePartsImpl<MergeTreeDataPartsCNCHVector>(all_parts, flatten, /* skip_drop_ranges */ true, nullptr, nullptr, logging);
}

ServerDataPartsVector calcVisiblePartsForGC(
    ServerDataPartsVector & all_parts,
    ServerDataPartsVector * visible_alone_drop_ranges,
    ServerDataPartsVector * invisible_dropped_parts,
    LoggingOption logging)
{
    return calcVisiblePartsImpl(
        all_parts,
        /* flatten */ false,
        /* skip_drop_ranges */ false,
        visible_alone_drop_ranges,
        invisible_dropped_parts,
        logging);
}

/// Input
/// - all_bitmaps: list of bitmaps that have been committed (has non-zero CommitTs)
/// - include_tombstone: whether to include tombstone bitmaps in "visible_bitmaps"
/// Output
/// - visible_bitmaps: contains the latest version for all visible bitmaps
/// - visible_alone_tombstones: contains visible tombstone bitmaps that don't have previous version
/// - bitmaps_covered_by_range_tombstones: contains all bitmaps covered by range tombstones
///
/// Example:
///
/// We notate each bitmap as "PartitionID_MinBlock_MaxBlock_Type_CommitTs" below, where Type is one of
/// - 'b' for base bitmap
/// - 'd' for delta bitmap
/// - 'D' for single tombstone
/// - 'R' for range tombstone
///
/// Suppose we have the following inputs (all_bitmaps)
///     all_0_0_D_t0 (previous version may have been removed by gc thread),
///     all_1_1_b_t1, all_2_2_b_t1, all_3_3_b_t1,
///     all_1_1_D_t2, all_2_2_D_t2, all_1_2_b_t2,
///     all_1_2_d_t3, all_4_4_b_t4
///
/// The function will link bitmaps with the same block name (PartitionID_MinBlock_MaxBlock), from new to old.
/// If there are no range tombstones, all bitmaps are visible and the latest version for each bitmap is added to "visible_bitmaps".
///
/// Output when include_tombstone == true:
///     visible_bitmaps = [
///         all_0_0_D_t0,
///         all_1_1_D_t2 (-> all_1_1_b_t1),
///         all_1_2_d_t3 (-> all_1_2_b_t2),
///         all_2_2_D_t2 (-> all_2_2_b_t1),
///         all_3_3_b_t1,
///         all_4_4_b_t4
///     ]
///     visible_alone_tombstones (if not null) = [ all_0_0_D_t0 ]
///
/// Output when include_tombstone == false:
///     visible_bitmaps = [
///         all_1_2_d_t3 (-> all_1_2_b_t2),
///         all_3_3_b_t1,
///         all_4_4_b_t4
///     ]
///
/// If we add a range tombstone all_0_3_R_t2 to the input, all bitmaps in the same partition whose MaxBlock <= 3
/// are covered by the range tombstone and become invisible, so they are not included in "visible_bitmaps".
///
/// Output:
///     visible_bitmaps = [ all_4_4_b_t4 ],
///     bitmaps_covered_by_range_tombstones (if not null) = [
///         all_0_0_D_t0,
///         all_1_1_b_t1, all_2_2_b_t1, all_3_3_b_t1,
///         all_1_1_D_t2, all_2_2_D_t2, all_1_2_b_t2,
///         all_1_2_d_t3
///     ]
void calcVisibleDeleteBitmaps(
    DeleteBitmapMetaPtrVector & all_bitmaps,
    DeleteBitmapMetaPtrVector & visible_bitmaps,
    bool include_tombstone,
    DeleteBitmapMetaPtrVector * visible_alone_tombstones,
    DeleteBitmapMetaPtrVector * bitmaps_covered_by_range_tombstones)
{
    if (all_bitmaps.empty())
        return;
    if (all_bitmaps.size() == 1)
    {
        if (include_tombstone || !all_bitmaps.front()->isTombstone())
            visible_bitmaps = all_bitmaps;

        if (visible_alone_tombstones && all_bitmaps.front()->isTombstone())
            *visible_alone_tombstones = all_bitmaps;
        return;
    }

    std::sort(all_bitmaps.begin(), all_bitmaps.end(), LessDeleteBitmapMeta());

    auto prev_it = all_bitmaps.begin();
    auto curr_it = std::next(prev_it);

    while (prev_it != all_bitmaps.end())
    {
        auto & prev = *prev_it;
        if (prev->isRangeTombstone() && curr_it != all_bitmaps.end()
            && prev->getModel()->partition_id() == (*curr_it)->getModel()->partition_id())
        {
            if ((*curr_it)->isRangeTombstone())
            {
                if (bitmaps_covered_by_range_tombstones)
                    bitmaps_covered_by_range_tombstones->push_back(prev);
                prev_it = curr_it;
                ++curr_it;
                continue;
            }
            else if ((*curr_it)->getModel()->part_max_block() <= prev->getModel()->part_max_block())
            {
                if (bitmaps_covered_by_range_tombstones)
                    bitmaps_covered_by_range_tombstones->push_back(*curr_it);
                ++curr_it;
                continue;
            }
        }

        if (curr_it != all_bitmaps.end() && (*curr_it)->sameBlock(*prev))
        {
            (*curr_it)->setPrevious(prev);
        }
        else
        {
            if (include_tombstone || !prev->isTombstone())
                visible_bitmaps.push_back(prev);

            if (visible_alone_tombstones && prev->isTombstone() && !prev->tryGetPrevious())
                visible_alone_tombstones->push_back(prev);
        }

        prev_it = curr_it;
        ++curr_it;
    }
}
}
