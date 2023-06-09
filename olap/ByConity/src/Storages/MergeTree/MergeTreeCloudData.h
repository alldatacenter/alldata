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

#include <MergeTreeCommon/MergeTreeMetaBase.h>

#include <boost/multi_index/global_fun.hpp>
#include <boost/multi_index/ordered_index.hpp>
#include <boost/multi_index_container.hpp>
#include <boost/range/iterator_range_core.hpp>

namespace DB
{

class MergeTreeCloudData : public MergeTreeMetaBase
{
public:
    std::string getName() const override { return "MergeTreeCloudData"; }

    DataPartPtr getPartIfExists(const MergeTreePartInfo & part_info);

    /// Add preared parts
    void addDataParts(MutableDataPartsVector & parts, UInt64 worker_topology_hash = 0);

    /// XXX:
    void removeDataParts(const DataPartsVector & parts, DataPartsVector * parts_not_found = nullptr);
    void removeDataParts(const Names & names, Names * names_not_found = nullptr);

    /// Load prepared parts, deactivate outdated parts and construct coverage link
    /// [Preallocate Mode] if worker_topology_hash is not empty, need to check whether the given topology is matched with worker's topology
    void loadDataParts(MutableDataPartsVector & parts, UInt64 worker_topology_hash = 0);

    /// Remove Outdated parts of which timestamp is less than expired ts from container.
    /// DO NOT check reference count of parts.
    void unloadOldPartsByTimestamp(Int64 expired_ts);

protected:
    void addPreparedPart(MutableDataPartPtr & part, DataPartsLock &);

    void tryRemovePartContributionToColumnSizes(const DataPartPtr & part);

    DataPartPtr getActivePartToReplace(
        const MergeTreePartInfo & new_part_info,
        const String & new_part_name,
        DataPartPtr & out_covering_part,
        DataPartsLock & /* data_parts_lock */);

    void deactivateOutdatedParts();

    void loadDataPartsInParallel(MutableDataPartsVector & parts);

    static void runOverPartsInParallel(MutableDataPartsVector & parts, size_t threads, const std::function<void(MutableDataPartPtr &)> & op);

    /// TODO: calculateColumnSizesImpl

    MergeTreeCloudData(
        const StorageID & table_id_,
        const String & relative_data_path_,
        const StorageInMemoryMetadata & metadata_,
        ContextMutablePtr context_,
        const String & date_column_name_,
        const MergeTreeMetaBase::MergingParams & merging_params_,
        std::unique_ptr<MergeTreeSettings> settings_);

    ~MergeTreeCloudData() override = default;
};

}
