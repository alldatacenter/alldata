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

#pragma once

#include <limits>
#include <tuple>
#include <vector>
#include <common/types.h>
#include <common/DayNum.h>
#include <Core/Names.h>
#include <Storages/MergeTree/MergeTreeDataFormatVersion.h>
#include <Storages/IStorage.h>


namespace DB
{

/// Information about partition and the range of blocks contained in the part.
/// Allows determining if parts are disjoint or one part fully contains the other.
struct MergeTreePartInfo
{
    String partition_id;
    Int64 min_block = 0;
    Int64 max_block = 0;
    UInt32 level = 0;
    Int64 mutation = 0;   /// If the part has been mutated or contains mutated parts, is equal to mutation version number.
    Int64 hint_mutation = 0; /// Trace about previous version part.

    StorageType storage_type = StorageType::Local;

    bool use_leagcy_max_level = false;  /// For compatibility. TODO remove it

    MergeTreePartInfo() = default;

    // MergeTreePartInfo(String partition_id_, Int64 min_block_, Int64 max_block_, UInt32 level_)
    //     : partition_id(std::move(partition_id_)), min_block(min_block_), max_block(max_block_), level(level_)
    // {
    // }

    // MergeTreePartInfo(String partition_id_, Int64 min_block_, Int64 max_block_, UInt32 level_, Int64 mutation_)
    //     : partition_id(std::move(partition_id_)), min_block(min_block_), max_block(max_block_), level(level_), mutation(mutation_)
    // {}

    MergeTreePartInfo(String partition_id_, Int64 min_block_, Int64 max_block_, UInt32 level_, UInt64 mutation_ = 0, UInt64 hint_mutation_ = 0, StorageType storage_type_ = StorageType::Local)
        : partition_id(std::move(partition_id_)), min_block(min_block_), max_block(max_block_), level(level_), mutation(mutation_), hint_mutation(hint_mutation_), storage_type(storage_type_)
    {
    }

    MergeTreePartInfo newDropVersion(UInt64 txn_id, StorageType storage_type_ = StorageType::Local) const
    {
        return {partition_id, min_block, max_block, level + 1, txn_id, /*hint_mutation*/ 0, storage_type_};
    }

    bool operator<(const MergeTreePartInfo & rhs) const
    {
        return std::forward_as_tuple(partition_id, min_block, max_block, level, mutation)
            < std::forward_as_tuple(rhs.partition_id, rhs.min_block, rhs.max_block, rhs.level, rhs.mutation);
    }

    bool operator==(const MergeTreePartInfo & rhs) const
    {
        return !(*this != rhs);
    }

    bool operator!=(const MergeTreePartInfo & rhs) const
    {
        return *this < rhs || rhs < *this;
    }

    /// Get block number that can be used to determine which mutations we still need to apply to this part
    /// (all mutations with version greater than this block number).
    Int64 getDataVersion() const { return mutation ? mutation : min_block; }

    /// True if contains rhs (this part is obtained by merging rhs with some other parts or mutating rhs)
    bool contains(const MergeTreePartInfo & rhs) const
    {
        return partition_id == rhs.partition_id        /// Parts for different partitions are not merged
            && min_block <= rhs.min_block
            && max_block >= rhs.max_block
            && level >= rhs.level
            && mutation >= rhs.mutation;
    }

    bool containsExactly(const MergeTreePartInfo & rhs) const
    {
        return partition_id == rhs.partition_id
            && min_block == rhs.min_block
            && max_block == rhs.max_block
            && (level > rhs.level || mutation > rhs.mutation);
    }

    bool sameBlocks(const MergeTreePartInfo & rhs) const
    {
        return partition_id == rhs.partition_id && min_block == rhs.min_block && max_block == rhs.max_block;
    }

    /// Return part mutation version, if part wasn't mutated return zero
    Int64 getMutationVersion() const
    {
        return mutation ? mutation : 0;
    }

    /// True if parts do not intersect in any way.
    bool isDisjoint(const MergeTreePartInfo & rhs) const
    {
        return partition_id != rhs.partition_id
            || min_block > rhs.max_block
            || max_block < rhs.min_block;
    }

    bool isFakeDropRangePart() const
    {
        /// Another max level was previously used for REPLACE/MOVE PARTITION
        auto another_max_level = std::numeric_limits<decltype(level)>::max();
        return level == MergeTreePartInfo::MAX_LEVEL || level == another_max_level;
    }

    /// Block name : PartitionID_MinBlock_MaxBlock
    /// All MVCC parts of the same block shared a block name
    String getBlockName() const;

    String getPartName() const;
    String getPartNameWithHintMutation() const;

    String getBasicPartName() const;
    String getPartNameV0(DayNum left_date, DayNum right_date) const;
    UInt64 getBlocksCount() const
    {
        return static_cast<UInt64>(max_block - min_block + 1);
    }

    /// Simple sanity check for partition ID. Checking that it's not too long or too short, doesn't contain a lot of '_'.
    static void validatePartitionID(const String & partition_id, MergeTreeDataFormatVersion format_version);

    static MergeTreePartInfo fromPartName(const String & part_name, MergeTreeDataFormatVersion format_version);  // -V1071

    static bool tryParsePartName(const String & part_name, MergeTreePartInfo * part_info, MergeTreeDataFormatVersion format_version);

    static void parseMinMaxDatesFromPartName(const String & part_name, DayNum & min_date, DayNum & max_date);

    static bool contains(const String & outer_part_name, const String & inner_part_name, MergeTreeDataFormatVersion format_version);

    String getPartitionKeyInStringAt(size_t index) const;

    static Names splitPartitionKeys(const String & partition_id);

    static constexpr UInt32 MAX_LEVEL = 999999999;
    static constexpr UInt32 MAX_BLOCK_NUMBER = 999999999;
    static constexpr UInt32 MAX_MUTATION = 999999999;

    static constexpr UInt32 LEGACY_MAX_LEVEL = std::numeric_limits<decltype(level)>::max();

private:
    template<bool NameWithHintMutation>
    String getPartNameImpl() const;
};

/// Information about detached part, which includes its prefix in
/// addition to the above fields.
struct DetachedPartInfo : public MergeTreePartInfo
{
    String dir_name;
    String prefix;

    String disk;

    /// If false, MergeTreePartInfo is in invalid state (directory name was not successfully parsed).
    bool valid_name;

    static const std::vector<String> DETACH_REASONS;

    /// NOTE: It may parse part info incorrectly.
    /// For example, if prefix contain '_' or if DETACH_REASONS doesn't contain prefix.
    static bool tryParseDetachedPartName(const String & dir_name, DetachedPartInfo & part_info, MergeTreeDataFormatVersion format_version);
};

using DetachedPartsInfo = std::vector<DetachedPartInfo>;

}
