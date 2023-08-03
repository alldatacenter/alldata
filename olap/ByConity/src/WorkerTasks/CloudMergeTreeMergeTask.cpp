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

#include <WorkerTasks/CloudMergeTreeMergeTask.h>

#include <CloudServices/commitCnchParts.h>
#include <Interpreters/Context.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/StorageCloudMergeTree.h>
#include <WorkerTasks/MergeTreeDataMerger.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ABORTED;
}

CloudMergeTreeMergeTask::CloudMergeTreeMergeTask(
    StorageCloudMergeTree & storage_,
    ManipulationTaskParams params_,
    ContextPtr context_)
    : ManipulationTask(std::move(params_), std::move(context_))
    , storage(storage_)
{
}

void CloudMergeTreeMergeTask::executeImpl()
{
    auto lock = storage.lockForShare(RWLockImpl::NO_QUERY, storage.getSettings()->lock_acquire_timeout_for_background_operations);

    auto & cloud_table = dynamic_cast<StorageCloudMergeTree &>(*params.storage.get());
    MergeTreeDataMerger merger(cloud_table, params, getContext(), manipulation_entry->get(), [&]() {
        if (isCancelled())
            return true;

        auto last_touch_time = getManipulationListElement()->last_touch_time.load(std::memory_order_relaxed);

        /// TODO: add settings
        if (time(nullptr) - last_touch_time > 600)
            setCancelled();

        return isCancelled();
    });

    auto merged_part = merger.mergePartsToTemporaryPart();

    IMutableMergeTreeDataPartsVector temp_parts;
    std::vector<ReservationPtr> reserved_spaces; // hold space

    for (auto & part : params.source_data_parts)
    {
        /// TODO: Double check, set drop part's mutation to current txnid and hint_mutation to corresponding part's mutation.
        if (part->info.level == MergeTreePartInfo::MAX_LEVEL)
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Drop part info level is MAX_LEVEL");

        MergeTreePartInfo drop_part_info(
            part->info.partition_id,
            part->info.min_block,
            part->info.max_block,
            part->info.level + 1,
            getContext()->getCurrentTransactionID().toUInt64(),
            0 /* must be zero for drop part */);

        reserved_spaces.emplace_back(cloud_table.reserveSpace(0)); /// Drop part is empty part.
        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part->name, reserved_spaces.back()->getDisk(), 0);

        auto drop_part = std::make_shared<MergeTreeDataPartCNCH>(
            storage, drop_part_info.getPartName(), drop_part_info, single_disk_volume, std::nullopt);

        drop_part->partition.assign(part->partition);
        drop_part->bucket_number = part->bucket_number;
        drop_part->deleted = true;
        /// rows_count and bytes_on_disk is required for parts info statistics.
        drop_part->covered_parts_rows = part->rows_count;
        drop_part->covered_parts_size = part->bytes_on_disk;
        temp_parts.push_back(std::move(drop_part));
    }

    temp_parts.push_back(std::move(merged_part));

    if (isCancelled())
        throw Exception("Merge task " + params.task_id + " is cancelled", ErrorCodes::ABORTED);

    CnchDataWriter cnch_writer(storage, getContext(), ManipulationType::Merge, params.task_id);
    DumpedData data = cnch_writer.dumpAndCommitCnchParts(temp_parts);
    cnch_writer.preload(data.parts);
}

}
