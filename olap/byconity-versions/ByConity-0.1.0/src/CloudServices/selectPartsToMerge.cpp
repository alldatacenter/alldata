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

#include <CloudServices/selectPartsToMerge.h>

#include <Catalog/DataModelPartWrapper.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/DanceMergeSelector.h>
#include <Storages/MergeTree/SimpleMergeSelector.h>

namespace DB
{

ServerSelectPartsDecision selectPartsToMerge(
    const MergeTreeMetaBase & data,
    std::vector<ServerDataPartsVector> & res,
    const ServerDataPartsVector & data_parts,
    ServerCanMergeCallback can_merge_callback,
    size_t max_total_size_to_merge,
    bool aggressive,
    bool enable_batch_select,
    [[maybe_unused]] bool merge_with_ttl_allowed,
    Poco::Logger * log)
{
    const auto data_settings = data.getSettings();
    auto metadata_snapshot = data.getInMemoryMetadataPtr();

    if (data_parts.empty())
    {
        if (log)
            LOG_DEBUG(log, "There are no parts in the table");
        return ServerSelectPartsDecision::NOTHING_TO_MERGE;
    }

    time_t current_time = std::time(nullptr);

    IMergeSelector::PartsRanges parts_ranges;

    /// StoragePolicyPtr storage_policy = data.getStoragePolicy(IStorage::StorageLocation::MAIN);
    /// Volumes with stopped merges are extremely rare situation.
    /// Check it once and don't check each part (this is bad for performance).
    /// bool has_volumes_with_disabled_merges = storage_policy->hasAnyVolumeWithDisabledMerges();

    size_t parts_selected_precondition = 0;

    // split parts into buckets if current table is bucket table.
    std::unordered_map<Int64, ServerDataPartsVector> buckets;
    if (data.isBucketTable())
        groupPartsByBucketNumber(data, buckets, data_parts);
    else
        buckets.emplace(0, data_parts);

    for (auto & bucket: buckets)
    {
        const String * prev_partition_id = nullptr;
        /// Previous part only in boundaries of partition frame
        const ServerDataPartPtr * prev_part = nullptr;

        for (const auto & part : bucket.second)
        {
            const String & partition_id = part->info().partition_id;

            if (!prev_partition_id || partition_id != *prev_partition_id)
            {
                if (parts_ranges.empty() || !parts_ranges.back().empty())
                    parts_ranges.emplace_back();

                /// New partition frame.
                prev_partition_id = &partition_id;
                prev_part = nullptr;
            }

            /// Check predicate only for the first part in each range.
            if (!prev_part)
            {
                /* Parts can be merged with themselves for TTL needs for example.
                * So we have to check if this part is currently being inserted with quorum and so on and so forth.
                * Obviously we have to check it manually only for the first part
                * of each partition because it will be automatically checked for a pair of parts. */
                if (!can_merge_callback(nullptr, part))
                    continue;

                /// This part can be merged only with next parts (no prev part exists), so start
                /// new interval if previous was not empty.
                if (!parts_ranges.back().empty())
                    parts_ranges.emplace_back();
            }
            else
            {
                /// If we cannot merge with previous part we had to start new parts
                /// interval (in the same partition)
                if (!can_merge_callback(*prev_part, part))
                {
                    /// Now we have no previous part
                    prev_part = nullptr;

                    /// Mustn't be empty
                    assert(!parts_ranges.back().empty());

                    /// Some parts cannot be merged with previous parts and also cannot be merged with themselves,
                    /// for example, merge is already assigned for such parts, or they participate in quorum inserts
                    /// and so on.
                    /// Also we don't start new interval here (maybe all next parts cannot be merged and we don't want to have empty interval)
                    if (!can_merge_callback(nullptr, part))
                        continue;

                    /// Starting new interval in the same partition
                    parts_ranges.emplace_back();
                }
            }

            IMergeSelector::Part part_info;
            part_info.size = part->part_model().size();
            part_info.rows = part->rowsCount();
            time_t part_commit_time = TxnTimestamp(part->getCommitTime()).toSecond();
            part_info.age = current_time > part_commit_time ? current_time - part_commit_time : 0;
            part_info.level = part->info().level;
            part_info.data = &part;
            /// TODO:
            /// part_info.ttl_infos = &part->ttl_infos;
            /// part_info.compression_codec_desc = part->default_codec->getFullCodecDesc();
            /// part_info.shall_participate_in_merges = has_volumes_with_disabled_merges ? part->shallParticipateInMerges(storage_policy) : true;
            part_info.shall_participate_in_merges = true;

            ++parts_selected_precondition;

            parts_ranges.back().emplace_back(part_info);

            prev_part = &part;
        }
    }

    if (parts_selected_precondition == 0)
    {
        if (log)
            LOG_DEBUG(log, "No parts satisfy preconditions for merge");
        return ServerSelectPartsDecision::CANNOT_SELECT;
    }

    /*
    if (metadata_snapshot->hasAnyTTL() && merge_with_ttl_allowed && !ttl_merges_blocker.isCancelled())
    {
        IMergeSelector::PartsRange parts_to_merge;

        /// TTL delete is preferred to recompression
        TTLDeleteMergeSelector delete_ttl_selector(
            next_delete_ttl_merge_times_by_partition,
            current_time,
            data_settings->merge_with_ttl_timeout,
            data_settings->ttl_only_drop_parts);

        parts_to_merge = delete_ttl_selector.select(parts_ranges, max_total_size_to_merge);

        future_parts.emplace_back();

        if (!parts_to_merge.empty())
        {
            future_parts.back().merge_type = MergeType::TTL_DELETE;
        }
        else if (metadata_snapshot->hasAnyRecompressionTTL())
        {
            TTLRecompressMergeSelector recompress_ttl_selector(
                next_recompress_ttl_merge_times_by_partition,
                current_time,
                data_settings->merge_with_recompression_ttl_timeout,
                metadata_snapshot->getRecompressionTTLs());

            parts_to_merge = recompress_ttl_selector.select(parts_ranges, max_total_size_to_merge);
            if (!parts_to_merge.empty())
                future_parts.back().merge_type = MergeType::TTL_RECOMPRESS;
        }

        auto parts = toDataPartsVector(parts_to_merge);
        LOG_DEBUG(log, "Selected {} parts from {} to {}", parts.size(), parts.front()->name(), parts.back()->name());
        future_parts.back().assign(std::move(parts));
        return ServerSelectPartsDecision::SELECTED;
    }
    */

    std::unique_ptr<IMergeSelector> merge_selector;
    const auto & config = data.getContext()->getConfigRef();
    auto merge_selector_str = config.getString("merge_selector", "simple");
    if (merge_selector_str == "dance")
    {
        DanceMergeSelector::Settings merge_settings;
        merge_settings.loadFromConfig(config);
        /// Override value from table settings
        merge_settings.max_parts_to_merge_base = data_settings->max_parts_to_merge_at_once;
        merge_settings.enable_batch_select = enable_batch_select;
        if (aggressive)
            merge_settings.min_parts_to_merge_base = 1;
        merge_selector = std::make_unique<DanceMergeSelector>(data, merge_settings);
    }
    else
    {
        SimpleMergeSelector::Settings merge_settings;
        /// Override value from table settings
        merge_settings.max_parts_to_merge_at_once = data_settings->max_parts_to_merge_at_once;
        if (aggressive)
            merge_settings.base = 1;
        merge_selector = std::make_unique<SimpleMergeSelector>(merge_settings);
    }

    auto ranges = merge_selector->selectMulti(parts_ranges, max_total_size_to_merge, nullptr);
    if (ranges.empty())
    {
        LOG_DEBUG(log, "There is no need to merge parts according to merge selector algorithm: {}", merge_selector_str);
        return ServerSelectPartsDecision::CANNOT_SELECT;
    }

    for (auto & range : ranges)
    {
        /// Do not allow to "merge" part with itself for regular merges, unless it is a TTL-merge where it is ok to remove some values with expired ttl
        if (range.size() == 1)
            throw Exception("Logical error: merge selector returned only one part to merge", ErrorCodes::LOGICAL_ERROR);

        res.emplace_back();
        res.back().reserve(range.size());
        for (auto & part : range)
            res.back().push_back(*static_cast<const ServerDataPartPtr *>(part.data));
    }

    return ServerSelectPartsDecision::SELECTED;
}


void groupPartsByBucketNumber(const MergeTreeMetaBase & data, std::unordered_map<Int64, ServerDataPartsVector> & grouped_buckets, const ServerDataPartsVector & data_parts)
{
    auto table_definition_hash = data.getTableHashForClusterBy();
    for (const auto & part : data_parts)
    {
        /// Can only merge those already been clustered parts.
        if (part->part_model().table_definition_hash() != table_definition_hash)
            continue;
        if (auto it = grouped_buckets.find(part->part_model().bucket_number()); it != grouped_buckets.end())
            it->second.push_back(part);
        else
            grouped_buckets.emplace(part->part_model().bucket_number(), ServerDataPartsVector{part});
    }
}

}
