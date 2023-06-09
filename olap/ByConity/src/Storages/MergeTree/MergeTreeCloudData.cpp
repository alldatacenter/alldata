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

#include <Storages/MergeTree/MergeTreeCloudData.h>
#include "Processors/QueryPipeline.h"

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int MEMORY_LIMIT_EXCEEDED;
    extern const int SYNTAX_ERROR;
    extern const int INVALID_PARTITION_VALUE;
    extern const int METADATA_MISMATCH;
    extern const int PART_IS_TEMPORARILY_LOCKED;
    extern const int TOO_MANY_PARTS;
    extern const int INCOMPATIBLE_COLUMNS;
    extern const int CANNOT_UPDATE_COLUMN;
    extern const int CANNOT_ALLOCATE_MEMORY;
    extern const int CANNOT_MUNMAP;
    extern const int CANNOT_MREMAP;
    extern const int BAD_TTL_EXPRESSION;
    extern const int NOT_FOUND_EXPECTED_DATA_PART;
    extern const int TOO_MANY_UNEXPECTED_DATA_PARTS;
    extern const int DUPLICATE_DATA_PART;
    extern const int NO_SUCH_DATA_PART;
}


MergeTreeCloudData::MergeTreeCloudData(
    const StorageID & table_id_,
    const String & relative_data_path_,
    const StorageInMemoryMetadata & metadata_,
    ContextMutablePtr context_,
    const String & date_column_name_,
    const MergeTreeMetaBase::MergingParams & merging_params_,
    std::unique_ptr<MergeTreeSettings> settings_)
    : MergeTreeMetaBase(
        table_id_,
        relative_data_path_,
        metadata_,
        context_,
        date_column_name_,
        merging_params_,
        std::move(settings_),
        false, /// require_part_metadata
        false  /// attach
    )
{
}

void MergeTreeCloudData::addPreparedPart(MutableDataPartPtr & part, DataPartsLock & lock)
{
    DataPartPtr covering_part;
    auto covered_part = getActivePartToReplace(part->info, part->name, covering_part, lock);
    if (covering_part)
        throw Exception(
            "Tried to commit obsolete part " + part->name + " covered by " + covering_part->getNameWithState(), ErrorCodes::LOGICAL_ERROR);

    addPartContributionToColumnSizes(part);
    part->is_temp = false;
    part->state = DataPartState::Committed;
    data_parts_indexes.insert(part);
}

void MergeTreeCloudData::addDataParts(MutableDataPartsVector & parts, UInt64)
{
    // auto current_topology_hash = global_context.getWorkerTopologyHash(getStorageUUID());
    // if (worker_topology_hash && current_topology_hash && worker_topology_hash != current_topology_hash)
    // {
    //     LOG_INFO(log, "Worker_topology_hash not match. Stop loading data parts.");
    //     return;
    // }

    std::sort(parts.begin(), parts.end(), LessDataPart{});

    {
        auto lock = lockParts();
        try
        {
            for (auto & part : parts)
                addPreparedPart(part, lock);
        }
        catch (...)
        {
            for (auto & part : parts)
            {
                data_parts_by_info.erase(part->info);
                tryRemovePartContributionToColumnSizes(part);
            }
            throw;
        }
    }

    LOG_DEBUG(log, "Added data parts ({} items)", parts.size());
}

void MergeTreeCloudData::removeDataParts(const DataPartsVector & parts, DataPartsVector * parts_not_found)
{
    LOG_DEBUG(log, "Removing data parts");
    size_t count = 0;

    auto lock = lockParts();

    for (const auto & part : parts)
    {
        auto it = data_parts_by_info.find(part->info);
        if (it != data_parts_by_info.end())
        {
            if ((*it)->state == IMergeTreeDataPart::State::Committed)
                removePartContributionToColumnSizes(*it);
            data_parts_by_info.erase(it);
            count += 1;
        }
        else
        {
            if (parts_not_found)
                parts_not_found->push_back(part);
        }
    }

    LOG_DEBUG(log, "Removed data parts ({} items)", count);
}

void MergeTreeCloudData::removeDataParts(const Names & names, Names * names_not_found)
{
    LOG_DEBUG(log, "Removing data parts by names");
    size_t count = 0;

    auto lock = lockParts();

    for (const auto & part_name : names)
    {
        auto part_info = MergeTreePartInfo::fromPartName(part_name, format_version);
        auto it = data_parts_by_info.find(part_info);
        if (it != data_parts_by_info.end())
        {
            if ((*it)->state == IMergeTreeDataPart::State::Committed)
                removePartContributionToColumnSizes(*it);
            data_parts_by_info.erase(it);
            count += 1;
        }
        else
        {
            if (names_not_found)
                names_not_found->push_back(part_name);
        }
    }

    LOG_DEBUG(log, "Removed data parts ({} items)", count);
}

void MergeTreeCloudData::loadDataParts(MutableDataPartsVector & parts, UInt64)
{
    Stopwatch stopwatch;
    auto lock = lockParts();

    // if (worker_topology_hash)
    // {
    //     auto current_topology_hash = global_context.getWorkerTopologyHash(getStorageUUID());
    //     if (current_topology_hash && worker_topology_hash != current_topology_hash)
    //     {
    //         LOG_INFO(log, "Worker_topology_hash not match. Stop loading data parts.");
    //         return;
    //     }
    // }

    for (auto & part : parts)
    {
        /// Assume that all parts are Committed, covered parts will be detected and marked as Outdated later
        part->state = DataPartState::Committed;

        if (!data_parts_indexes.insert(part).second)
            throw Exception("Part " + part->name + " already exists", ErrorCodes::DUPLICATE_DATA_PART);
    }

    deactivateOutdatedParts();

    LOG_TRACE(log, "Loading {} parts, prepared part multi-index, elapsed {} ms", parts.size(), stopwatch.elapsedMicroseconds() / 1000.0);

    loadDataPartsInParallel(parts);

    calculateColumnSizesImpl();

    // check bitmap index; reuse cnch_parallel_prefetching to check in parallel.
    // if (settings.cnch_parallel_prefetching > 1)
    // {
    //     size_t pool_size = std::min(parts.size(), UInt64(settings.cnch_parallel_prefetching));
    //     runOverPartsInParallel(parts, pool_size, [](auto & part) { part->checkBitmapIndex(); });
    // }
    // else
    //     std::for_each(parts.begin(), parts.end(), [](auto & part) { part->checkBitmapIndex(); });

    LOG_DEBUG(log, "Loaded data parts ({} items)", data_parts_indexes.size());
}

void MergeTreeCloudData::unloadOldPartsByTimestamp(Int64 expired_ts)
{
    DataPartsVector parts_to_delete;
    std::vector<DataPartIteratorByStateAndInfo> iterators_to_delete;

    {
        auto parts_lock = lockParts();

        auto outdated_parts_range = getDataPartsStateRange(DataPartState::Outdated);
        for (auto it = outdated_parts_range.begin(); it != outdated_parts_range.end(); ++it)
        {
            auto part_ts = (*it)->commit_time;
            if (expired_ts >= Int64(part_ts))
                iterators_to_delete.emplace_back(it);
        }

        parts_to_delete.reserve(iterators_to_delete.size());
        for (auto & it : iterators_to_delete)
        {
            parts_to_delete.emplace_back(*it);
            modifyPartState(it, DataPartState::Deleting);
        }
    }

    if (parts_to_delete.empty())
        return;

    LOG_TRACE(log, "Found {} parts of which timestamp is expired to remove", parts_to_delete.size());

    /// TODO:
    /// removePartsFinally(parts_to_delete);
}

void MergeTreeCloudData::loadDataPartsInParallel(MutableDataPartsVector & parts)
{
    if (parts.empty())
        return;

    auto cnch_parallel_prefetching = getSettings()->cnch_parallel_prefetching ? getSettings()->cnch_parallel_prefetching : 16;

    MutableDataPartsVector partial_parts;
    // auto it = std::remove_if(parts.begin(), parts.end(), [](const auto & part) { return part->isPartial(); });
    // std::copy(it, parts.end(), std::back_inserter(partial_parts));
    // parts.erase(it, parts.end());

    /// load checksums and index_granularity in parallel
    std::atomic<bool> has_adaptive_parts = false;
    std::atomic<bool> has_non_adaptive_parts = false;
    size_t pool_size = std::min(parts.size(), UInt64(cnch_parallel_prefetching));
    runOverPartsInParallel(parts, pool_size, [&](auto & part) {
        part->loadColumnsChecksumsIndexes(false, false);
        if (part->index_granularity_info.is_adaptive)
            has_adaptive_parts.store(true, std::memory_order_relaxed);
        else
            has_non_adaptive_parts.store(true, std::memory_order_relaxed);
    });

    pool_size = std::min(partial_parts.size(), UInt64(cnch_parallel_prefetching));
    runOverPartsInParallel(partial_parts, pool_size, [&](auto & part) {
        part->loadColumnsChecksumsIndexes(false, false);
        if (part->index_granularity_info.is_adaptive)
            has_adaptive_parts.store(true, std::memory_order_relaxed);
        else
            has_non_adaptive_parts.store(true, std::memory_order_relaxed);
    });

    if (has_non_adaptive_parts && has_adaptive_parts && !getSettings()->enable_mixed_granularity_parts)
    {
        throw Exception("Table contains parts with adaptive and non adaptive marks, but `setting enable_mixed_granularity_parts` is disabled", ErrorCodes::LOGICAL_ERROR);
    }

    has_non_adaptive_index_granularity_parts = has_non_adaptive_parts;
}

void MergeTreeCloudData::runOverPartsInParallel(
    MutableDataPartsVector & parts, size_t threads_num, const std::function<void(MutableDataPartPtr &)> & op)
{

    if (parts.empty()) return;
    if (threads_num <= 1)
    {
        for (auto & part : parts)
            op(part);
        return;
    }

    ThreadPool thread_pool(threads_num);
    for (auto & part : parts)
    {
        thread_pool.scheduleOrThrowOnError([&part, &op] {
            op(part);
        });
    }
    thread_pool.wait();
}


void MergeTreeCloudData::tryRemovePartContributionToColumnSizes(const DataPartPtr & part)
{
    if (part->state != IMergeTreeDataPart::State::Committed)
        return;

    try
    {
        removePartContributionToColumnSizes(part);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

MergeTreeDataPartPtr MergeTreeCloudData::getActivePartToReplace(
    const MergeTreePartInfo & new_part_info,
    const String & /* new_part_name */,
    DataPartPtr & out_covering_part,
    DataPartsLock & /* data_parts_lock */)
{
    /// Parts contained in the part are consecutive in data_parts, intersecting the insertion place for the part itself.
    auto it = data_parts_by_state_and_info.lower_bound(DataPartStateAndInfo{DataPartState::Committed, new_part_info});

    auto committed_parts_range = getDataPartsStateRange(DataPartState::Committed);

    /// Go to the right
    if (it != committed_parts_range.end())
    {
        if ((*it)->info == new_part_info)
            throw Exception("Unexpected duplicate part " + (*it)->getNameWithState() + ". It is a bug.", ErrorCodes::LOGICAL_ERROR);

        if ((*it)->info.containsExactly(new_part_info))
        {
            out_covering_part = *it;
            return {};
        }
    }

    /// Go to the left.
    if (it != committed_parts_range.begin())
    {
        auto prev = std::prev(it);
        if (new_part_info.containsExactly((*prev)->info))
            return *prev;
    }

    return {};
}

void MergeTreeCloudData::deactivateOutdatedParts()
{
    if (data_parts_indexes.size() < 2)
        return;

    auto deactivate_part = [&](DataPartIteratorByStateAndInfo it) {
        (*it)->remove_time.store((*it)->modification_time, std::memory_order_relaxed);
        modifyPartState(it, DataPartState::Outdated);
    };

    /// Now all parts are committed, so data_parts_by_state_and_info == committed_parts_range

    /// One-pass algorithm to construct delta chains
    auto prev_jt = data_parts_by_state_and_info.begin();
    auto curr_jt = std::next(prev_jt);

    while (curr_jt != data_parts_by_state_and_info.end() && (*curr_jt)->state == DataPartState::Committed)
    {
        const auto & prev_part = *prev_jt;
        const auto & curr_part = *curr_jt;

        if (curr_part->isPartial() && curr_part->containsExactly(*prev_part))
        {
            if (const auto & p = curr_part->tryGetPreviousPart())
                throw Exception("Part " + curr_part->name + " has already owned prev_part: " + p->name, ErrorCodes::LOGICAL_ERROR);
            curr_part->setPreviousPart(prev_part);
            deactivate_part(prev_jt);
        }

        prev_jt = curr_jt;
        ++curr_jt;
    }
}
}
