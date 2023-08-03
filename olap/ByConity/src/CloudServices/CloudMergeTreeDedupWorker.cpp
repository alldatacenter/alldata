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
#include <CloudServices/CloudMergeTreeDedupWorker.h>
#include <CloudServices/CnchDedupHelper.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/CnchServerClientPool.h>
#include <CloudServices/DedupWorkerStatus.h>
#include <CloudServices/commitCnchParts.h>
#include <Interpreters/InterpreterDropQuery.h>
#include <MergeTreeCommon/MergeTreeDataDeduper.h>
#include <Parsers/ASTDropQuery.h>
#include <Storages/StorageCloudMergeTree.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Transaction/CnchWorkerTransaction.h>

namespace ProfileEvents
{
extern const Event BackgroundDedupSchedulePoolTask;
}

namespace DB
{
CloudMergeTreeDedupWorker::CloudMergeTreeDedupWorker(StorageCloudMergeTree & storage_)
    : storage(storage_)
    , context(storage.getContext())
    , log_name(storage.getLogName() + "(DedupWorker)")
    , log(&Poco::Logger::get(log_name))
    , interval_scheduler(storage.getSettings()->staged_part_lifetime_threshold_ms_to_block_kafka_consume)
{
    task = storage.getContext()->getUniqueTableSchedulePool().createTask(log_name, [this] { run(); });

    /// The maximum error of staged parts commit time is tso_windows, so it's necessary to reserve that time.
    UInt64 tso_windows_ms = context->getConfigRef().getInt("tso_service.tso_window_ms", 3000);
    if (interval_scheduler.staged_part_max_life_time_ms < tso_windows_ms)
        interval_scheduler.staged_part_max_life_time_ms = 0;
    else
        interval_scheduler.staged_part_max_life_time_ms -= tso_windows_ms;

    status.create_time = time(nullptr);
}

CloudMergeTreeDedupWorker::~CloudMergeTreeDedupWorker()
{
    try
    {
        stop();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);

        std::lock_guard lock(status_mutex);
        status.last_exception = getCurrentExceptionMessage(false);
        status.last_exception_time = time(nullptr);
    }
}

void CloudMergeTreeDedupWorker::run()
{
    {
        std::lock_guard lock(status_mutex);
        status.total_schedule_cnt++;
    }

    if (!isActive())
        return;
    try
    {
        Stopwatch timer;
        interval_scheduler.has_excep_or_timeout = false;
        iterate();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        interval_scheduler.has_excep_or_timeout = true;

        std::lock_guard lock(status_mutex);
        status.last_exception = getCurrentExceptionMessage(false);
        status.last_exception_time = time(nullptr);
    }

    if (isActive())
        task->scheduleAfter(interval_scheduler.getScheduleTime());
}

void CloudMergeTreeDedupWorker::iterate()
{
    if (server_host_ports.empty())
    {
        LOG_DEBUG(log, "server host and ports haven't been set, skip");
        return;
    }

    // ProfileEvents::increment(ProfileEvents::BackgroundDedupSchedulePoolTask);

    DedupWorkerStatus copy_status = getDedupWorkerStatus();

    Stopwatch cost_all;
    auto catalog = context->getCnchCatalog();
    TxnTimestamp ts = context->getTimestamp();
    /// must use cnch table to construct staged parts
    auto table = catalog->tryGetTableByUUID(*context, UUIDHelpers::UUIDToString(storage.getStorageUUID()), ts);
    if (!table)
    {
        LOG_INFO(log, "table have been dropped, skip");
        return;
    }
    auto cnch_table = dynamic_cast<StorageCnchMergeTree *>(table.get());
    if (!cnch_table)
    {
        LOG_ERROR(log, "table {} is not cnch merge tree", table->getStorageID().getNameForLogs());
        detachSelf();
        return;
    }
    if (cnch_table->getStorageID().getFullTableName() != storage.getCnchStorageID().getFullTableName())
    {
        LOG_WARNING(
            log,
            "cnch table storage id has changed, detach dedup worker. Origin storage id: {}, current storage id: {}",
            storage.getCnchStorageID().getNameForLogs(),
            cnch_table->getStorageID().getNameForLogs());
        detachSelf();
        return;
    }

    /// get all the partitions of committed staged parts
    auto staged_parts = cnch_table->getStagedParts(ts);
    if (staged_parts.empty())
    {
        LOG_TRACE(log, "no more staged parts to process, skip");
        return;
    }

    auto server_client = context->getCnchServerClientPool().get(server_host_ports);
    if (!server_client)
        throw Exception("no server client found", ErrorCodes::LOGICAL_ERROR);

    /// create dedup transaction
    auto dedup_context = Context::createCopy(context);
    dedup_context->setSessionContext(dedup_context);
    CnchWorkerTransactionPtr txn;
    try
    {
        txn = std::make_shared<CnchWorkerTransaction>(dedup_context, server_client);
        dedup_context->setCurrentTransaction(txn);
    }
    catch (...)
    {
        tryLogCurrentException(log, "failed to create dedup transaction on server " + server_client->getRPCAddress());
        throw;
    }

    /// acquire all the locks
    NameOrderedSet sorted_partitions;
    for (auto & part : staged_parts)
        sorted_partitions.insert(part->info.partition_id);

    CnchDedupHelper::DedupScope scope = storage.getSettings()->partition_level_unique_keys
        ? CnchDedupHelper::DedupScope::Partitions(sorted_partitions)
        : CnchDedupHelper::DedupScope::Table();

    Stopwatch watch;
    CnchLockHolder cnch_lock(
        *context,
        CnchDedupHelper::getLocksToAcquire(
            scope, txn->getTransactionID(), storage, storage.getSettings()->dedup_acquire_lock_timeout.value.totalMilliseconds()));
    if (!cnch_lock.tryLock())
        return;

    /// get staged parts again after acquired the locks
    ts = context->getTimestamp(); /// must get a new ts after locks are acquired
    staged_parts = CnchDedupHelper::getStagedPartsToDedup(scope, *cnch_table, ts);
    if (staged_parts.empty())
    {
        LOG_INFO(log, "no more staged parts after acquried the locks, they may have been processed by other thread");
        return;
    }

    sorted_partitions.clear();
    UInt64 min_staged_part_timestamp{UINT64_MAX};
    for (auto & part : staged_parts)
    {
        sorted_partitions.insert(part->info.partition_id);
        min_staged_part_timestamp = std::min(min_staged_part_timestamp, part->commit_time.toUInt64());
        LOG_DEBUG(log, "Dedup staged part: {}, commit time: {} ms.", part->name, part->commit_time.toMillisecond());
    }

    if (scope.isPartitions())
    {
        scope = CnchDedupHelper::DedupScope::Partitions(sorted_partitions);
    }
    MergeTreeDataPartsCNCHVector visible_parts = CnchDedupHelper::getVisiblePartsToDedup(scope, *cnch_table, ts);

    watch.restart();
    MergeTreeDataDeduper deduper(storage, context);
    LocalDeleteBitmaps bitmaps_to_dump = deduper.dedupParts(
        txn->getTransactionID(),
        CnchPartsHelper::toIMergeTreeDataPartsVector(visible_parts),
        CnchPartsHelper::toIMergeTreeDataPartsVector(staged_parts));
    LOG_DEBUG(
        log,
        "Dedup took {} ms in total, processed {} staged parts with {} parts",
        watch.elapsedMilliseconds(),
        staged_parts.size(),
        visible_parts.size());

    copy_status.last_task_staged_part_cnt = staged_parts.size();
    copy_status.last_task_visible_part_cnt = visible_parts.size();
    copy_status.last_task_dedup_cost_ms = watch.elapsedMilliseconds();

    size_t staged_parts_total_rows = 0, visible_parts_total_rows = 0;
    for (auto & part : staged_parts)
        staged_parts_total_rows += part->rows_count;
    for (auto & part : visible_parts)
        visible_parts_total_rows += part->rows_count;
    copy_status.last_task_staged_part_total_rows = staged_parts_total_rows;
    copy_status.last_task_visible_part_total_rows = visible_parts_total_rows;

    watch.restart();
    CnchDataWriter cnch_writer(storage, dedup_context, ManipulationType::Insert);
    cnch_writer.publishStagedParts(staged_parts, bitmaps_to_dump);
    copy_status.last_task_publish_cost_ms = watch.elapsedMilliseconds();
    LOG_DEBUG(log, "publishing took {} ms", watch.elapsedMilliseconds());

    txn->commitV2();
    LOG_INFO(log, "Committed dedup txn {}", txn->getTransactionID().toUInt64());

    interval_scheduler.calNextScheduleTime(min_staged_part_timestamp, context->getTimestamp());

    copy_status.total_dedup_cnt++;
    copy_status.last_task_total_cost_ms = cost_all.elapsedMilliseconds();
    copy_status.last_schedule_wait_ms = interval_scheduler.getScheduleTime();

    std::lock_guard lock(status_mutex);
    status = std::move(copy_status);
}

void CloudMergeTreeDedupWorker::setServerHostWithPorts(HostWithPorts host_ports)
{
    {
        std::lock_guard lock(server_mutex);
        server_host_ports = std::move(host_ports);
    }

    heartbeat_task = context->getSchedulePool().createTask(log_name, [this] { heartbeat(); });
    heartbeat_task->activateAndSchedule();
}

HostWithPorts CloudMergeTreeDedupWorker::getServerHostWithPorts()
{
    std::lock_guard lock(server_mutex);
    return server_host_ports;
}

DedupWorkerStatus CloudMergeTreeDedupWorker::getDedupWorkerStatus()
{
    std::lock_guard lock(status_mutex);
    return status;
}

void CloudMergeTreeDedupWorker::heartbeat()
{
    if (!isActive())
        return;
    auto & server_pool = context->getCnchServerClientPool();

    try
    {
        auto server_client = server_pool.get(server_host_ports);
        UInt32 ret = server_client->reportDeduperHeartbeat(storage.getCnchStorageID(), storage.getTableName());
        if (ret == DedupWorkerHeartbeatResult::Kill)
        {
            LOG_DEBUG(log, "Deduper will shutdown as it received a kill signal.");
            detachSelf();
            return;
        }
        else if (ret == DedupWorkerHeartbeatResult::Success)
        {
            last_heartbeat_time = time(nullptr);
        }
        else
            throw Exception("Deduper buffer received invalid response", ErrorCodes::LOGICAL_ERROR);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);

        auto now = time(nullptr);
        LOG_DEBUG(log, "Dedup time interval not receive hb: {}", UInt64(now - last_heartbeat_time));
        {
            std::lock_guard lock(status_mutex);
            status.last_exception = getCurrentExceptionMessage(false);
            status.last_exception_time = time(nullptr);
        }
        if (UInt64(now - last_heartbeat_time) > storage.getSettings()->dedup_worker_max_heartbeat_interval)
        {
            LOG_DEBUG(log, "Deduper will shutdown as heartbeat reached time out.");
            detachSelf();
            return;
        }
    }

    if (isActive())
        heartbeat_task->scheduleAfter(storage.getContext()->getSettingsRef().dedup_worker_heartbeat_ms.totalMilliseconds());
}

void CloudMergeTreeDedupWorker::detachSelf()
{
    is_stopped = true;
    ThreadFromGlobalPool([log = this->log, s = this->storage.shared_from_this(), c = this->context] {
        auto drop_query = std::make_shared<ASTDropQuery>();
        drop_query->database = s->getDatabaseName();
        drop_query->table = s->getTableName();
        drop_query->kind = ASTDropQuery::Drop;
        drop_query->if_exists = true;
        LOG_DEBUG(log, "Detach self: {}", s->getStorageID().getNameForLogs());
        InterpreterDropQuery(drop_query, c).execute();
        LOG_DEBUG(log, "Detached self: {}", s->getStorageID().getNameForLogs());
    }).detach();
}

}
