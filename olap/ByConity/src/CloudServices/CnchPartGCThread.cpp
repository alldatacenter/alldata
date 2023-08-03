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

#include <CloudServices/CnchPartGCThread.h>

#include <random>
#include <Catalog/Catalog.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/commitCnchParts.h>
#include <Interpreters/ServerPartLog.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH.h>
#include <Storages/StorageCnchMergeTree.h>
#include <WorkerTasks/ManipulationType.h>
#include <Poco/Exception.h>

namespace DB
{

CnchPartGCThread::CnchPartGCThread(ContextPtr context_, const StorageID & id) : ICnchBGThread(context_, CnchBGThreadType::PartGC, id)
{
}

void CnchPartGCThread::runImpl()
{
    UInt64 sleep_ms = 30 * 1000;

    try
    {
        auto istorage = getStorageFromCatalog();
        auto & storage = checkAndGetCnchTable(istorage);
        auto storage_settings = storage.getSettings();
        if (istorage->is_dropped)
        {
            LOG_DEBUG(log, "Table was dropped, wait for removing...");
            scheduled_task->scheduleAfter(10 * 1000);
            return;
        }

        try
        {
            clearOldParts(istorage, storage);
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }

        try
        {
            clearOldInsertionLabels(istorage, storage);
        }
        catch (...)
        {
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }

        sleep_ms = storage_settings->cleanup_delay_period * 1000
            + std::uniform_int_distribution<UInt64>(0, storage_settings->cleanup_delay_period_random_add * 1000)(rng);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    scheduled_task->scheduleAfter(sleep_ms);
}

void CnchPartGCThread::clearOldParts(const StoragePtr & istorage, StorageCnchMergeTree & storage)
{
    auto storage_settings = storage.getSettings();
    auto now = time(nullptr);

    bool in_wakeup = inWakeup();
    Strings partitions = catalog->getPartitionIDs(istorage, nullptr);

    /// Only inspect the parts small than gc_timestamp
    TxnTimestamp gc_timestamp = calculateGCTimestamp(storage_settings->old_parts_lifetime.totalSeconds(), in_wakeup);
    if (gc_timestamp <= last_gc_timestamp) /// Skip unnecessary gc
    {
        LOG_DEBUG(log, "Skip unnecessary GC as gc_timestamp {} <= last_gc_timestamp {} ", gc_timestamp, last_gc_timestamp);
        return;
    }

    Stopwatch watch;
    auto all_parts = catalog->getServerDataPartsInPartitions(istorage, partitions, gc_timestamp, nullptr);
    LOG_TRACE(log, "Get parts from Catalog cost {} us, all_parts: {}", watch.elapsedMicroseconds(), all_parts.size());
    watch.restart();

    ServerDataPartsVector visible_alone_drop_ranges;
    ServerDataPartsVector invisible_dropped_parts;
    auto visible_parts = CnchPartsHelper::calcVisiblePartsForGC(all_parts, &visible_alone_drop_ranges, &invisible_dropped_parts);
    LOG_TRACE(log, "Calculate visible parts for GC cost {} us, visible_parts: {}", watch.elapsedMicroseconds(), visible_parts.size());
    watch.stop();

    /// Generate DROP_RANGE for expired partitions by the TTL
    if (!visible_parts.empty())
        tryMarkExpiredPartitions(storage, visible_parts);

    /// Clear special old parts
    UInt64 old_parts_lifetime = in_wakeup ? 0ull : UInt64(storage_settings->old_parts_lifetime.totalSeconds());

    invisible_dropped_parts.erase(
        std::remove_if(
            invisible_dropped_parts.begin(),
            invisible_dropped_parts.end(),
            [&](auto & part) { return UInt64(now) < TxnTimestamp(part->getCommitTime()).toSecond() + old_parts_lifetime; }),
        invisible_dropped_parts.end());
    pushToRemovingQueue(storage, invisible_dropped_parts, "invisible dropped");

    visible_alone_drop_ranges.erase(
        std::remove_if(
            visible_alone_drop_ranges.begin(),
            visible_alone_drop_ranges.end(),
            [&](auto & part) { return UInt64(now) < TxnTimestamp(part->getCommitTime()).toSecond() + old_parts_lifetime; }),
        visible_alone_drop_ranges.end());
    pushToRemovingQueue(storage, visible_alone_drop_ranges, "visible alone drop range");

    if (!visible_alone_drop_ranges.empty() || !invisible_dropped_parts.empty())
    {
        LOG_DEBUG(
            log,
            "Clear old parts with timestamp {}, {} : all_parts {}, visible_alone_drop_ranges {}, invisible_dropped_parts {}",
            gc_timestamp,
            LocalDateTime(gc_timestamp.toSecond()),
            all_parts.size(),
            visible_alone_drop_ranges.size(),
            invisible_dropped_parts.size());
    }


    /// Clear special old bitmaps
    DeleteBitmapMetaPtrVector visible_bitmaps, visible_alone_tombstone_bitmaps, unvisible_bitmaps;
    if (storage.getInMemoryMetadataPtr()->hasUniqueKey())
    {
        auto all_bitmaps = catalog->getDeleteBitmapsInPartitions(istorage, partitions, gc_timestamp);
        // TODO: filter out uncommitted bitmaps
        CnchPartsHelper::calcVisibleDeleteBitmaps(all_bitmaps, visible_bitmaps, true, &visible_alone_tombstone_bitmaps, &unvisible_bitmaps);
    }

    removeDeleteBitmaps(storage, unvisible_bitmaps, "covered by range tombstones");

    visible_alone_tombstone_bitmaps.erase(
        std::remove_if(
            visible_alone_tombstone_bitmaps.begin(),
            visible_alone_tombstone_bitmaps.end(),
            [&](auto & bitmap) { return UInt64(now)  < (bitmap->getCommitTime() >> 18) / 1000 + old_parts_lifetime; }),
        visible_alone_tombstone_bitmaps.end()
    );
    removeDeleteBitmaps(storage, visible_alone_tombstone_bitmaps, "alone tombstone");

    /// Clear staged parts
    if (storage.getInMemoryMetadataPtr()->hasUniqueKey())
    {
        /// staged part is tmp part which is unecessary to last for old_parts_lifetime, delete it directly
        ServerDataPartsVector staged_parts = createServerPartsFromDataParts(
            storage, catalog->getStagedParts(istorage, TxnTimestamp{storage.getContext()->getTimestamp()}));
        staged_parts = CnchPartsHelper::calcVisiblePartsForGC(staged_parts, nullptr, nullptr);
        size_t size = staged_parts.size();
        size_t handle_size = 0;
        for (size_t i = 0; i < size; ++i)
        {
            auto staged_part = staged_parts[i];
            if (!staged_part->deleted())
                continue;
            /// In response to fail situation, it's necessary to handle previous stage part before handle stage part who marks delete state.
            if (staged_part->tryGetPreviousPart())
                pushToRemovingQueue(storage, {staged_part->tryGetPreviousPart()}, "Clear staged parts", true);
            else
            {
                pushToRemovingQueue(storage, {staged_part}, "Clear staged parts", true);
                handle_size++;
            }
        }
        LOG_DEBUG(log, "All staged parts: {}, Clear staged parts {}", staged_parts.size(), handle_size);
    }

    if (!visible_parts.empty())
    {
        LOG_DEBUG(
            log,
            "Will check visible_parts {} with timestamp {}, {}",
            visible_parts.size(),
            gc_timestamp,
            LocalDateTime(gc_timestamp.toSecond()));
    }

    auto checkpoints = getCheckpoints(storage, gc_timestamp);
    for (size_t i = 1; i < checkpoints.size(); ++i)
    {
        collectBetweenCheckpoints(storage, visible_parts, {}, checkpoints[i - 1], checkpoints[i]);
    }

    last_gc_timestamp = gc_timestamp;
}

/// TODO: optimize me
static time_t calcTTLForPartition(
    const MergeTreePartition & partition, const KeyDescription & partition_key_description, const TTLDescription & ttl_description)
{
    auto columns = partition_key_description.sample_block.cloneEmptyColumns();
    for (size_t i = 0; i < partition.value.size(); ++i)
        columns[i]->insert(partition.value[i]);

    auto block = partition_key_description.sample_block.cloneWithColumns(std::move(columns));
    ttl_description.expression->execute(block);

    auto & result_column_with_tn = block.getByName(ttl_description.result_column);
    auto & result_column = result_column_with_tn.column;
    auto & result_type = result_column_with_tn.type;

    if (isDate(result_type))
    {
        auto value = UInt16(result_column->getUInt(0));
        const auto & date_lut = DateLUT::instance();
        return date_lut.fromDayNum(DayNum(value));
    }
    else if (isDateTime(result_type))
    {
        return UInt32(result_column->getUInt(0));
    }
    else
    {
        throw Exception("Logical error in calculate TTL value: unexpected TTL result column type", ErrorCodes::LOGICAL_ERROR);
    }
}

void CnchPartGCThread::tryMarkExpiredPartitions(StorageCnchMergeTree & storage, const ServerDataPartsVector & visible_parts)
{
    auto metadata_snapshot = storage.getInMemoryMetadataPtr();
    if (!metadata_snapshot->hasPartitionLevelTTL())
        return;

    const auto & partition_key_description = metadata_snapshot->partition_key;
    const auto & rows_ttl = metadata_snapshot->table_ttl.rows_ttl;

    time_t now = time(nullptr);

    StorageCnchMergeTree::PartitionDropInfos partition_infos;
    for (const auto & part : visible_parts)
    {
        if (part->deleted())
            continue;

        if (auto iter = partition_infos.find(part->info().partition_id); iter != partition_infos.end())
        {
            iter->second.max_block = std::max(iter->second.max_block, part->info().max_block);
        }
        else
        {
            auto ttl = calcTTLForPartition(part->get_partition(), partition_key_description, rows_ttl);
            if (ttl < now)
            {
                auto it = partition_infos.try_emplace(part->info().partition_id).first;
                it->second.max_block = part->info().max_block;
                it->second.value.assign(part->partition());
            }
        }
    }

    ContextMutablePtr query_context = Context::createCopy(storage.getContext());

    auto txn = query_context->getCnchTransactionCoordinator().createTransaction(CreateTransactionOption().setInitiator(CnchTransactionInitiator::GC));
    SCOPE_EXIT({
        if (txn)
            query_context->getCnchTransactionCoordinator().finishTransaction(txn);
    });

    query_context->setCurrentTransaction(txn, false);
    query_context->getHdfsConnectionParams().lookupOnNeed();
    query_context->setQueryContext(query_context);

    auto drop_ranges = storage.createDropRangesFromPartitions(partition_infos, txn);
    // auto bitmap_tombstones = storage.createDeleteBitmapRangeTombstones(drop_ranges, txn->getTransactionID());

    auto cnch_writer = CnchDataWriter(storage, query_context, ManipulationType::Drop);
    cnch_writer.dumpAndCommitCnchParts(drop_ranges);
}

std::vector<TxnTimestamp> CnchPartGCThread::getCheckpoints(StorageCnchMergeTree & storage, TxnTimestamp max_timestamp)
{
    auto storage_settings = storage.getSettings();

    auto now = time(nullptr);

    std::vector<TxnTimestamp> timestamps{0};

    if (storage_settings->time_travel_retention_days)
    {
        Checkpoints checkpoints = catalog->getCheckpoints();

        for (auto & checkpoint : checkpoints)
        {
            TxnTimestamp ts = checkpoint.timestamp();
            if (checkpoint.status() == Checkpoint::Normal && UInt64(now) > ts.toSecond() + checkpoint.ttl())
                ; /// TODO:
            /// storage.removeCheckpoint(checkpoint);
            else
                timestamps.push_back(ts);
        }
    }

    timestamps.push_back(max_timestamp);
    return timestamps;
}

void CnchPartGCThread::collectBetweenCheckpoints(
    StorageCnchMergeTree & storage,
    const ServerDataPartsVector & visible_parts,
    const DeleteBitmapMetaPtrVector & visible_bitmaps,
    TxnTimestamp begin,
    TxnTimestamp end)
{
    ServerDataPartsVector stale_parts;
    DeleteBitmapMetaPtrVector stale_bitmaps;

    for (const auto & part : visible_parts)
        collectStaleParts(part, begin, end, false, stale_parts);
    for (const auto & bitmap : visible_bitmaps)
        collectStaleBitmaps(bitmap, begin, end, false, stale_bitmaps);

    pushToRemovingQueue(storage, stale_parts, "stale");
    removeDeleteBitmaps(storage, stale_bitmaps, "stale between " + begin.toString() + " and " + end.toString());

    std::unordered_map<String, Int32> remove_parts_num;

    for (auto & part : stale_parts)
        ++remove_parts_num[part->info().partition_id];

    if (!remove_parts_num.empty())
    {
        std::stringstream ss;
        for (auto & [partition_id, count]: remove_parts_num)
            ss << "(" << partition_id << ", " << count << "), ";
        LOG_TRACE(log, "Removed stale parts: {}", ss.str());
    }
}

void CnchPartGCThread::collectStaleParts(
    ServerDataPartPtr parent_part,
    TxnTimestamp begin,
    TxnTimestamp end,
    bool has_visible_ancestor,
    ServerDataPartsVector & stale_parts) const
{
    do
    {
        const auto & prev_part = parent_part->tryGetPreviousPart();

        if (!prev_part)
            break;

        has_visible_ancestor = has_visible_ancestor           // inherit from parent
            || (parent_part->getCommitTime() < end.toUInt64() // parent is visible
                && !parent_part->isPartial());                // parent is a base one

        if (has_visible_ancestor && prev_part->getCommitTime() > begin.toUInt64())
        {
            LOG_DEBUG(
                log, "Will remove part {} covered by {} /partial={}", prev_part->name(), parent_part->name(), parent_part->isPartial());
            stale_parts.push_back(prev_part);
        }

        parent_part = prev_part;
    }
    while (true);
}

void CnchPartGCThread::collectStaleBitmaps(
    DeleteBitmapMetaPtr parent_bitmap,
    TxnTimestamp begin,
    TxnTimestamp end,
    bool has_visible_ancestor,
    DeleteBitmapMetaPtrVector & stale_bitmaps)
{
    do
    {
        const auto & prev_bitmap = parent_bitmap->tryGetPrevious();
        if (!prev_bitmap || prev_bitmap->getCommitTime() <= begin.toUInt64())
            break;

        /// inherit from parent;
        if (!has_visible_ancestor) // 1. parent is visible;                           &&  2. parent is a base bitmap
            has_visible_ancestor = ((parent_bitmap->getCommitTime() <= end.toUInt64()) && !parent_bitmap->isPartial());

        if (has_visible_ancestor)
        {
            LOG_DEBUG(log, "Will remove delete bitmap {} covered by {}", prev_bitmap->getNameForLogs(), parent_bitmap->getNameForLogs());
            stale_bitmaps.push_back(prev_bitmap);
        }

        parent_bitmap = prev_bitmap;
    } while (true);
}

TxnTimestamp CnchPartGCThread::calculateGCTimestamp(UInt64 delay_second, bool in_wakeup)
{
    auto local_context = getContext();

    TxnTimestamp gc_timestamp = local_context->getTimestamp();
    if (in_wakeup) /// XXX: will invalid all running queries
        return gc_timestamp;

    /// Will invalid the running queries of which the timestamp <= gc_timestamp
    auto server_min_active_ts = calculateMinActiveTimestamp();
    TxnTimestamp max_gc_timestamp = ((time(nullptr) - delay_second) * 1000) << 18;

    return std::min({gc_timestamp, server_min_active_ts, max_gc_timestamp});
}

void CnchPartGCThread::pushToRemovingQueue(
    StorageCnchMergeTree & storage, const ServerDataPartsVector & parts, const String & part_type, bool is_staged_part)
{
    auto local_context = getContext();
    auto storage_settings = storage.getSettings();

    /// TODO: async ?
    /// removing_queue.push(std::move(part));
    if (parts.empty())
        return;

    LOG_DEBUG(log, "Try to remove {} {} part(s) by GCThread", parts.size(), part_type);

    /// ThreadPool remove_pool(storage_settings->gc_remove_part_thread_pool_size);
    ThreadPool remove_pool(32);

    auto batch_remove = [&](size_t start, size_t end)
    {
        remove_pool.scheduleOrThrowOnError([&, start, end]
        {
            MergeTreeDataPartsCNCHVector remove_parts;
            for (auto it = parts.begin() + start; it != parts.begin() + end; ++it)
            {
                auto name = (*it)->name();
                try
                {
                    LOG_TRACE(log, "Will remove part: {}", name);

                    auto cnch_part = (*it)->toCNCHDataPart(storage);
                    if (!is_staged_part)
                        cnch_part->remove();
                    remove_parts.emplace_back(cnch_part);
                }
                catch (Poco::FileNotFoundException & e)
                {
                    /// If the file already has been deleted, we can delete it directly from catalog.
                    LOG_ERROR(log, "Error occurs when remove part: " + name + " msg: " + e.displayText());
                    auto cnch_part = (*it)->toCNCHDataPart(storage);
                    remove_parts.emplace_back(cnch_part);
                }
                catch (...)
                {
                    tryLogCurrentException(log, "Error occurs when remove part: " + name);
                }
            }

            LOG_DEBUG(log, "Will remove {} {} part(s) in CnchCatalog", remove_parts.size(), part_type);
            if (!is_staged_part)
                catalog->clearDataPartsMeta(storage.shared_from_this(), remove_parts);
            else
                catalog->clearStagePartsMeta(storage.shared_from_this(), remove_parts);

            if (auto server_part_log = local_context->getServerPartLog())
            {
                auto now = time(nullptr);

                for (auto & part : remove_parts)
                {
                    ServerPartLogElement elem;
                    elem.event_type = ServerPartLogElement::REMOVE_PART;
                    elem.event_time = now;
                    elem.database_name = storage.getDatabaseName();
                    elem.table_name = storage.getTableName();
                    elem.uuid = storage.getStorageUUID();
                    elem.part_name = part->name;
                    elem.partition_id = part->info.partition_id;
                    elem.is_staged_part = is_staged_part;

                    server_part_log->add(elem);
                }
            }
        });
    };

    /// size_t batch_size = storage_settings->gc_remove_part_batch_size;
    constexpr static size_t batch_size = 1000;

    for (size_t start = 0; start < parts.size(); start += batch_size)
    {
        auto end = std::min(start + batch_size, parts.size());
        batch_remove(start, end);
    }

    remove_pool.wait();
}

void CnchPartGCThread::removeDeleteBitmaps(StorageCnchMergeTree & storage, const DeleteBitmapMetaPtrVector & bitmaps, const String & reason)
{
    if (bitmaps.empty())
        return;

    DeleteBitmapMetaPtrVector remove_bitmaps;

    for (const auto & bitmap : bitmaps)
    {
        try
        {
            bitmap->removeFile();
            remove_bitmaps.emplace_back(bitmap);
        }
        catch (...)
        {
            tryLogCurrentException(log, "Error occurs when remove bitmap " + bitmap->getNameForLogs());
        }
    }

    LOG_DEBUG(log, "Will remove {} delete bitmap(s), reason: {}", remove_bitmaps.size(), reason);

    catalog->removeDeleteBitmaps(storage.shared_from_this(), remove_bitmaps);
}

void CnchPartGCThread::clearOldInsertionLabels(const StoragePtr &, StorageCnchMergeTree & storage)
{
    time_t insertion_label_ttl = storage.getSettings()->insertion_label_ttl;

    std::vector<InsertionLabel> labels_to_remove;
    auto now = time(nullptr);
    auto labels = catalog->scanInsertionLabels(storage.getStorageID().uuid);
    for (auto & label : labels)
    {
        if (label.create_time + insertion_label_ttl <= now)
            labels_to_remove.emplace_back(std::move(label));
    }

    if (labels_to_remove.empty())
        return;

    catalog->removeInsertionLabels(labels_to_remove);
    LOG_DEBUG(log, "Removed {} insertion labels.", labels_to_remove.size());
}

}
