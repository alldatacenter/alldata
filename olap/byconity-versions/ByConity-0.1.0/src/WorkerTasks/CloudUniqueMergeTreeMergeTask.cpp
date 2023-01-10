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

#include <Catalog/Catalog.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/commitCnchParts.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Transaction/ICnchTransaction.h>
#include <Transaction/CnchLock.h>
#include <WorkerTasks/CloudUniqueMergeTreeMergeTask.h>
#include <WorkerTasks/MergeTreeDataMerger.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int ABORTED;
    extern const int LOGICAL_ERROR;
}

CloudUniqueMergeTreeMergeTask::CloudUniqueMergeTreeMergeTask(
    StorageCloudMergeTree & storage_, ManipulationTaskParams params_, ContextPtr context_)
    : ManipulationTask(std::move(params_), std::move(context_))
    , storage(storage_)
    , log_name(storage.getLogName() + "(MergeTask)")
    , log(&Poco::Logger::get(log_name))
    , cnch_writer(storage, getContext(), ManipulationType::Merge)
{
    if (params.source_data_parts.empty())
        throw Exception("Empty source data parts for merge task " + params.task_id, ErrorCodes::LOGICAL_ERROR);
    partition_id = params.source_data_parts[0]->info.partition_id;
}

/// - parts: should belong to `partition_id` and sorted in (partition_id, min_block, max_block) order
DeleteBitmapMetaPtrVector
CloudUniqueMergeTreeMergeTask::getDeleteBitmapMetas(Catalog::Catalog & catalog, const IMergeTreeDataPartsVector & parts, TxnTimestamp ts)
{
    DeleteBitmapMetaPtrVector all_bitmaps = catalog.getDeleteBitmapsInPartitions(params.storage, {partition_id}, ts);

    /// construct bitmap version chain, remove invisible ones
    DeleteBitmapMetaPtrVector bitmaps;
    CnchPartsHelper::calcVisibleDeleteBitmaps(all_bitmaps, bitmaps);

    auto bitmap_it = bitmaps.begin();

    DeleteBitmapMetaPtrVector res;
    res.reserve(parts.size());
    /// collect bitmap meta for input parts
    for (auto & part : parts)
    {
        /// search for the bitmap meta for *part_it
        while (bitmap_it != bitmaps.end() && !(*bitmap_it)->sameBlock(part->info))
            bitmap_it++;

        if (bitmap_it == bitmaps.end())
            throw Exception("Missing delete bitmap metadata for part " + part->name, ErrorCodes::LOGICAL_ERROR);

        res.push_back(*bitmap_it);
        bitmap_it++;
    }
    return res;
}

/// Return delete bitmaps based on `curr_metas`.
/// Can reuse prev_bitmaps[i] if curr_metas[i] == prev_metas[i].
/// If meta is changed, also calculate delta bitmap and store in out_delta_bitmaps[i].
ImmutableDeleteBitmapVector readDeleteBitmaps(
    const MergeTreeMetaBase & storage,
    const DeleteBitmapMetaPtrVector & curr_metas,
    const DeleteBitmapMetaPtrVector & prev_metas,
    const ImmutableDeleteBitmapVector & prev_bitmaps,
    DeleteBitmapVector & out_delta_bitmaps)
{
    out_delta_bitmaps.resize(curr_metas.size());
    ImmutableDeleteBitmapVector res;
    for (size_t i = 0; i < curr_metas.size(); ++i)
    {
        if (curr_metas[i]->getCommitTime() == prev_metas[i]->getCommitTime())
        {
            res.push_back(prev_bitmaps[i]); /// meta didn't change, reuse prev bitmap
        }
        else
        {
            /// read the new bitmap
            DeleteBitmapPtr bitmap = std::make_shared<Roaring>();
            for (DeleteBitmapMetaPtr meta = curr_metas[i]; meta; meta = meta->tryGetPrevious())
            {
                deserializeDeleteBitmapInfo(storage, meta->getModel(), bitmap);
                if (!meta->isPartial())
                    break;
            }
            res.push_back(bitmap);
            /// add delta bitmap
            out_delta_bitmaps[i] = std::make_shared<Roaring>(*bitmap - *prev_bitmaps[i]);
        }
    }
    return res;
}

void CloudUniqueMergeTreeMergeTask::updateDeleteBitmap(
    Catalog::Catalog & catalog, const MergeTreeDataMerger & merger, DeleteBitmapPtr & out_bitmap)
{
    Stopwatch watch;

    auto ts = getContext()->getTimestamp();
    std::swap(prev_bitmaps, curr_bitmaps);
    std::swap(prev_bitmap_metas, curr_bitmap_metas);
    curr_bitmap_metas = getDeleteBitmapMetas(catalog, params.source_data_parts, ts);
    DeleteBitmapVector delta_bitmaps;
    curr_bitmaps = readDeleteBitmaps(storage, curr_bitmap_metas, prev_bitmap_metas, prev_bitmaps, delta_bitmaps);

    size_t total_update_rows = 0;
    for (size_t i = 0; i < delta_bitmaps.size(); ++i)
    {
        if (!delta_bitmaps[i])
            continue; /// bitmap for parts[i] doesn't change

        auto it = delta_bitmaps[i]->begin();
        auto end = delta_bitmaps[i]->end();
        auto part_bitmap = params.source_data_parts[i]->getDeleteBitmap();
        auto & rowid_mapping = merger.getRowidMapping(i);

        while (it != end)
        {
            /// convert rowid of source part into rowid of merged part
            UInt32 src_rowid = *it;
            UInt32 skipped = part_bitmap->rank(src_rowid);
            UInt32 dst_rowid = rowid_mapping[src_rowid - skipped];
            out_bitmap->add(dst_rowid);
            it++;
        }
        total_update_rows += delta_bitmaps[i]->cardinality();
    }

    LOG_DEBUG(log, "Added {} new deletes in {} ms", total_update_rows, watch.elapsedMilliseconds());
}

void CloudUniqueMergeTreeMergeTask::executeImpl()
{
    Stopwatch watch;

    auto txn = getContext()->getCurrentTransaction();
    if (!txn)
        throw Exception("Transaction is not set", ErrorCodes::LOGICAL_ERROR);
    auto txn_id = txn->getTransactionID();

    auto lock = storage.lockForShare(RWLockImpl::NO_QUERY, storage.getSettings()->lock_acquire_timeout_for_background_operations);

    LOG_TRACE(log, "Begin to execute merge task {}", params.task_id);

    auto catalog = getContext()->getCnchCatalog();
    auto t1 = getContext()->getTimestamp();
    /// get and set src part's bitmap meta at t1
    curr_bitmap_metas = getDeleteBitmapMetas(*catalog, params.source_data_parts, t1);
    for (size_t i = 0; i < curr_bitmap_metas.size(); ++i)
    {
        params.source_data_parts[i]->setDeleteBitmapMeta(curr_bitmap_metas[i]);
        curr_bitmaps.push_back(params.source_data_parts[i]->getDeleteBitmap());
    }

    LOG_TRACE(log, "Prepared delete bitmap for source parts");

    /// merge src parts using delete bitmap at t1
    MergeTreeDataMerger merger(
        storage,
        params,
        getContext(),
        getManipulationListElement(),
        [&, this] {
            if (isCancelled())
                return true;

            /// TODO: refactor this
            auto last_touch_time = getManipulationListElement()->last_touch_time.load(std::memory_order_relaxed);
            if (UInt64(time(nullptr) - last_touch_time) > getContext()->getSettingsRef().cloud_task_auto_stop_timeout)
            {
                LOG_TRACE(
                    &Poco::Logger::get("CloudUniqueMergeTreeMergeTask"),
                    "Task {} doesn't receive heartbeat from server, stop it self.",
                    params.task_id);
                setCancelled();
            }
            return isCancelled();
        },
        /*build_rowid_mappings*/ true);
    /// data of temp part will be removed in dtor
    auto merged_part = merger.mergePartsToTemporaryPart();

    DeleteBitmapPtr merged_part_bitmap = std::make_shared<Roaring>();
    /// t2: convert any new deletes of src part into deletes on merged part
    updateDeleteBitmap(*catalog, merger, merged_part_bitmap);

    /// prepare parts and bitmaps to dump
    std::vector<ReservationPtr> reservations;
    IMutableMergeTreeDataPartsVector parts_to_dump;
    LocalDeleteBitmaps bitmaps_to_dump;
    /// create drop part and tombstone bitmap for each src part
    for (auto & part : params.source_data_parts)
    {
        MergeTreePartInfo drop_part_info = part->info.newDropVersion(txn_id.toUInt64());
        reservations.emplace_back(storage.reserveSpace(part->bytes_on_disk));
        auto single_disk_volume = std::make_shared<SingleDiskVolume>("volume_" + part->name, reservations.back()->getDisk(), 0);

        auto drop_part = std::make_shared<MergeTreeDataPartCNCH>(
            storage, drop_part_info.getPartName(), drop_part_info, single_disk_volume, std::nullopt);
        drop_part->partition.assign(part->partition);
        drop_part->deleted = true;

        parts_to_dump.push_back(std::move(drop_part));
        bitmaps_to_dump.push_back(LocalDeleteBitmap::createTombstone(drop_part_info, txn_id.toUInt64()));
    }
    parts_to_dump.push_back(merged_part);

    if (isCancelled())
        throw Exception("Merge task " + params.task_id + " is cancelled", ErrorCodes::ABORTED);

    /// dump parts and bitmaps
    auto dumped_data = cnch_writer.dumpCnchParts(parts_to_dump, bitmaps_to_dump, /*staged parts*/ {});

    /// enter commit phase
    Stopwatch lock_watch;
    LockInfoPtr partition_lock = std::make_shared<LockInfo>(txn_id);
    partition_lock->setMode(LockMode::X);
    partition_lock->setTimeout(10000); // 10s
    partition_lock->setUUID(storage.getStorageUUID());
    /// need to lock table instead of partition for table-level uniqueness
    if (storage.getSettings()->partition_level_unique_keys)
    {
        partition_lock->setPartition(partition_id);
    }
    auto ctx = getContext();
    CnchLockHolder cnch_lock(*ctx, {std::move(partition_lock)});
    cnch_lock.lock();

    lock_watch.restart();

    /// there may be new deletes before we acquired the lock since last update, handle them here
    updateDeleteBitmap(*catalog, merger, merged_part_bitmap);

    /// dump merged part's bitmap
    auto final_bitmap_to_dump = LocalDeleteBitmap::createBase(merged_part->info, merged_part_bitmap, txn_id.toUInt64());
    auto new_dumped_data = cnch_writer.dumpCnchParts(/*parts*/ {}, {final_bitmap_to_dump}, /*staged parts*/ {});
    dumped_data.bitmaps.push_back(new_dumped_data.bitmaps.front());

    cnch_writer.commitDumpedParts(dumped_data);
    /// TODO: make sure txn is rollbacked and lock is released

    LOG_INFO(
        log,
        "Merge task {} succeed in {} ms (with {} ms holding lock)",
        params.task_id,
        watch.elapsedMilliseconds(),
        lock_watch.elapsedMilliseconds());
    /// preload can be done outside the lock
    // tryPreload(context, storage, dumped_data.parts, ManipulationType::Merge);
}

} // namespace DB
