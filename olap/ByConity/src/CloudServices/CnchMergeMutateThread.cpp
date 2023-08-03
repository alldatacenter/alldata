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

#include <CloudServices/CnchMergeMutateThread.h>

#include <Catalog/Catalog.h>
#include <Catalog/DataModelPartWrapper.h>
#include <CloudServices/CnchPartsHelper.h>
#include <CloudServices/selectPartsToMerge.h>
#include <CloudServices/CnchWorkerClient.h>
#include <CloudServices/CnchWorkerClientPools.h>
#include <Common/Configurations.h>
#include <Interpreters/PartMergeLog.h>
#include <Interpreters/ServerPartLog.h>
#include <Storages/MergeTree/MergeTreeData.h>
#include <Storages/MergeTree/MergeTreeDataMergerMutator.h>
#include <Storages/PartCacheManager.h>
#include <Storages/StorageCnchMergeTree.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <WorkerTasks/ManipulationTaskParams.h>

#include <chrono>

namespace DB
{
namespace ErrorCodes
{
    extern const int ABORTED;
    extern const int UNKNOWN_TABLE;
    extern const int TOO_MANY_SIMULTANEOUS_TASKS;
    extern const int CNCH_LOCK_ACQUIRE_FAILED;
}

namespace
{
    constexpr auto DELAY_SCHEDULE_TIME_IN_SECOND = 60ul;

    /// XXX: some settings for MutateTask
    constexpr auto max_mutate_part_num = 100UL;
    constexpr auto max_mutate_part_size = 20UL * 1024 * 1024 * 1024; // 20GB

    ServerCanMergeCallback getMergePred(const NameSet & merging_mutating_parts_snapshot)
    {
        return [&](const ServerDataPartPtr & lhs, const ServerDataPartPtr & rhs) -> bool {
            if (!lhs)
                return !merging_mutating_parts_snapshot.count(rhs->name());

            if (merging_mutating_parts_snapshot.count(lhs->name()) || merging_mutating_parts_snapshot.count(rhs->name()))
                return false;

            auto lhs_commit_time = lhs->getColumnsCommitTime();
            auto rhs_commit_time = rhs->getColumnsCommitTime();

            /// We can't find the right table_schema for parts which column_commit_time = 0
            if (!lhs_commit_time || !rhs_commit_time)
                return false;

            /// Consider this case:
            ///     T0: commit part_1
            ///     T1: alter table => create mutation_1
            ///     T2: commit part_2
            ///     T3: part_1 apply mutation_1 (columns_commit_ts T0 -> T1)
            /// We can't merge part part_1 and part_2 between T2 and T3, but can merge part_1 and part_2 after T3.

            return lhs_commit_time == rhs_commit_time;
        };
    }
}

ManipulationTaskRecord::~ManipulationTaskRecord()
{
    try
    {
        if (!try_execute && !parts.empty())
        {
            std::lock_guard lock(parent.currently_merging_mutating_parts_mutex);
            for (auto & part : parts)
                parent.currently_merging_mutating_parts.erase(part->name());
        }

        {
            std::lock_guard lock(parent.currently_synchronous_tasks_mutex);
            parent.currently_synchronous_tasks.erase(task_id);
            parent.currently_synchronous_tasks_cv.notify_all();
        }

        if (transaction)
            parent.getContext()->getCnchTransactionCoordinator().finishTransaction(transaction);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

FutureManipulationTask::~FutureManipulationTask()
{
    try
    {
        if (!try_execute && !parts.empty())
        {
            std::lock_guard lock(parent.currently_merging_mutating_parts_mutex);
            for (auto & part : parts)
                parent.currently_merging_mutating_parts.erase(part->name());
        }
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }
}

FutureManipulationTask & FutureManipulationTask::assignSourceParts(ServerDataPartsVector && parts_)
{
    for (auto & part : parts_)
    {
        LOG_DEBUG(&Poco::Logger::get("MergeMutateDEBUG"), "assignSourceParts part {} name {}", static_cast<const void*>(part.get()), part->name());
    }

    /// flatten the parts
    CnchPartsHelper::flattenPartsVector(parts_);

    if (!record->try_execute)
    {
        std::lock_guard lock(parent.currently_merging_mutating_parts_mutex);

        for (auto & part : parts_)
            if (parent.currently_merging_mutating_parts.count(part->name()))
                throw Exception("Part '" + part->name() + "' was already in other Task, cancel merge.", ErrorCodes::ABORTED);

        for (auto & part : parts_)
            parent.currently_merging_mutating_parts.emplace(part->name());
    }

    parts = std::move(parts_);
    return *this;
}

TxnTimestamp FutureManipulationTask::calcColumnsCommitTime() const
{
    if (parts.empty())
        throw Exception("The `parts` of manipulation task cannot be empty", ErrorCodes::LOGICAL_ERROR);

    if (mutation_entry)
        return mutation_entry->columns_commit_time;
    else
        return parts.front()->part_model().columns_commit_time();
}

FutureManipulationTask & FutureManipulationTask::prepareTransaction()
{
    if (parts.empty())
        throw Exception("The `parts` of manipulation task cannot be empty", ErrorCodes::LOGICAL_ERROR);

    if ((record->type == ManipulationType::Mutate || record->type == ManipulationType::Clustering) && !mutation_entry)
        throw Exception("The `mutation_entry` is not set for Mutation or Clustering task", ErrorCodes::LOGICAL_ERROR);

    auto & txn_coordinator = parent.getContext()->getCnchTransactionCoordinator();
    record->transaction = txn_coordinator.createTransaction(
        CreateTransactionOption().setInitiator(CnchTransactionInitiator::Merge).setPriority(CnchTransactionPriority::low));

    return *this;
}

std::unique_ptr<ManipulationTaskRecord> FutureManipulationTask::moveRecord()
{
    if (!record->transaction)
        throw Exception("The transaction of manipulation task is not initialized", ErrorCodes::LOGICAL_ERROR);

    record->parts = std::move(parts);
    return std::move(record);
}

CnchMergeMutateThread::CnchMergeMutateThread(ContextPtr context_, const StorageID & id)
    : ICnchBGThread(context_->getGlobalContext(), CnchBGThreadType::MergeMutate, id)
{
}

CnchMergeMutateThread::~CnchMergeMutateThread()
{
    try
    {
        shutdown();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void CnchMergeMutateThread::preStart()
{
    LOG_TRACE(log, "Starting MergeMutateThread for table {}", storage_id.getFullTableName());

    thread_start_time = getContext()->getTimestamp();
    catalog->setMergeMutateThreadStartTime(storage_id, thread_start_time);
    is_stale = false;
}

void CnchMergeMutateThread::shutdown()
{
    shutdown_called = true;

    LOG_DEBUG(log, "Shutting down MergeMutate thread for table {}", storage_id.getFullTableName());

    // stop background task
    stop();

    std::lock_guard lock_merge(try_merge_parts_mutex);
    std::lock_guard lock_mutate(try_mutate_parts_mutex);

    std::unordered_set<CnchWorkerClientPtr> workers;
    {
        std::lock_guard lock(task_records_mutex);
        for (auto & [_, task_record] : task_records)
            workers.emplace(task_record->worker);
    }

    for (const auto & worker : workers)
    {
        try
        {
            worker->shutdownManipulationTasks(storage_id.uuid);
        }
        catch (...)
        {
            tryLogCurrentException(log, "Failed to update status of tasks on " + worker->getHostWithPorts().toDebugString());
        }
    }

    {
        decltype(merge_pending_queue) empty_for_clear;
        merge_pending_queue.swap(empty_for_clear);
    }

    {
        std::lock_guard lock(task_records_mutex);
        LOG_DEBUG(log, "Remove all {} merge tasks when shutdown MergeTask.", task_records.size());
        task_records.clear();

        running_merge_tasks = 0;
        running_mutation_tasks = 0;
    }
}

void CnchMergeMutateThread::runHeartbeatTask()
{
    const auto & root_config = getContext()->getRootConfig();

    {
        std::lock_guard lock(task_records_mutex);
        if (task_records.empty())
            return;
    }

    auto now = time(nullptr);
    if (now - last_heartbeat_time < static_cast<time_t>(root_config.cnch_task_heartbeat_interval))
        return;
    last_heartbeat_time = now;

    std::unordered_map<CnchWorkerClientPtr, Strings> worker_with_tasks;

    {
        std::lock_guard lock(task_records_mutex);
        for (auto & [task_id, task] : task_records)
        {
            worker_with_tasks[task->worker].push_back(task->task_id);
        }
    }

    auto on_failure = [this, &root_config](std::unordered_map<String, TaskRecordPtr>::iterator it, std::lock_guard<std::mutex> & lock) {
        if (auto & task = it->second; ++task->lost_count >= root_config.cnch_task_heartbeat_max_retries)
        {
            LOG_WARNING(log, "Merge task_id: {} is lost, remove it from MergeList.", it->first);
            removeTaskImpl(task->task_id, lock);
        }
    };

    for (auto & [worker, request_tasks] : worker_with_tasks)
    {
        try
        {
            auto response_tasks = worker->touchManipulationTasks(storage_id.uuid, request_tasks);

            std::lock_guard lock(task_records_mutex);
            for (auto & request_task : request_tasks)
            {
                if (auto iter = task_records.find(request_task); iter != task_records.end())
                {
                    if (0 == response_tasks.count(request_task)) /// Not found in response
                        on_failure(iter, lock);
                    else /// Found in response
                        iter->second->lost_count = 0;
                }
            }
        }
        catch (...)
        {
            tryLogCurrentException(log, "Failed to update status of tasks on " + worker->getRPCAddress());

            std::lock_guard lock(task_records_mutex);
            for (auto & request_task : request_tasks)
            {
                if (auto iter = task_records.find(request_task); iter != task_records.end())
                    on_failure(iter, lock);
            }
        }
    }
}

void CnchMergeMutateThread::runImpl()
{
    auto local_context = getContext();

    if (local_context->getRootConfig().debug_disable_merge_mutate_thread.safeGet())
    {
        scheduled_task->scheduleAfter(10 * 1000);
        return;
    }

    UInt64 fetched_thread_start_time = 0;
    UInt64 current_ts = 0;
    try
    {
        current_ts = local_context->getTimestamp() >> 18;
        fetched_thread_start_time = catalog->getMergeMutateThreadStartTime(storage_id);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);

        /// If failed to get current ts or fetch start time from catalog, wait for a while and try again.
        scheduled_task->scheduleAfter(1 * 1000);
        return;
    }

    /// only if the thread_start_time equals to that in catalog the MergeMutateThread can schedule background tasks and accept task result.
    if (fetched_thread_start_time != thread_start_time)
    {
        {
            std::lock_guard lock(currently_merging_mutating_parts_mutex);
            currently_merging_mutating_parts.clear();
        }

        {
            std::lock_guard lock(currently_synchronous_tasks_mutex);
            currently_synchronous_tasks.clear();
            currently_synchronous_tasks_cv.notify_all();
        }

        {
            std::lock_guard lock(task_records_mutex);
            /// If thread_start_time is not equal to that in catalog. The MergeMutateThread will stop running and wait to be removed or scheduled again.
            LOG_ERROR(
                log,
                "Current MergeMutateThread start time {} does not equal to that in catalog {}. Remove all {} merge tasks and stop current "
                "BG thread.",
                thread_start_time,
                fetched_thread_start_time,
                task_records.size());
            task_records.clear();
        }
        return;
    }
    /// now fetched_thread_start_time == thread_start_time

    /// Delay first schedule for some time (60s) to make sure other old duplicate threads has been killed.
    if (last_schedule_time == 0)
    {
        last_schedule_time = current_ts;
        scheduled_task->scheduleAfter(DELAY_SCHEDULE_TIME_IN_SECOND * 1000);
        LOG_DEBUG(log, "Schedule after {}s because of last_schedule_time = 0", DELAY_SCHEDULE_TIME_IN_SECOND);
        return;
    }
    last_schedule_time = current_ts;

    try
    {
        runHeartbeatTask();

        auto istorage = getStorageFromCatalog();
        auto & storage = checkAndGetCnchTable(istorage);
        auto storage_settings = storage.getSettings();

        if (istorage->is_dropped)
        {
            LOG_DEBUG(log, "Table was dropped, wait for removing...");
            scheduled_task->scheduleAfter(10 * 1000);
            return;
        }

        {
            std::lock_guard lock(worker_pool_mutex);
            vw_name = storage.getSettings()->cnch_vw_write;
            /// TODO: pick_worker_algo = storage.getSettings()->cnch_merge_pick_worker_algo;
            vw_handle = getContext()->getVirtualWarehousePool().get(vw_name);
        }

        bool merge_success = false;

        try
        {
            auto max_mutation_task_num = storage_settings->max_addition_mutation_task_num;
            if (running_mutation_tasks < max_mutation_task_num)
            {
                merge_success |= tryMutateParts(istorage, storage);
            }
            else
            {
                LOG_DEBUG(log, "Too many mutation tasks (current: {}, max: {})", running_mutation_tasks, max_mutation_task_num);
            }
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }

        try
        {
            /// Ensure not submit too many merge tasks even if addition_bg_task and batch_select are active.
            auto max_bg_task_num = storage_settings->max_addition_bg_task_num;
            if (running_merge_tasks < max_bg_task_num)
            {
                merge_success |= tryMergeParts(istorage, storage);
            }
            else
            {
                LOG_DEBUG(log, "Too many merge tasks (current: {}, max: {})", running_merge_tasks, max_bg_task_num);
            }
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__);
        }

        if (merge_success)
            scheduled_task->scheduleAfter(1 * 1000);
        else
            scheduled_task->scheduleAfter(10 * 1000);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);

        scheduled_task->scheduleAfter(10 * 1000);
    }
}

PartMergeLogElement CnchMergeMutateThread::createPartMergeLogElement()
{
    PartMergeLogElement elem;
    elem.database = storage_id.database_name;
    elem.table = storage_id.table_name;
    elem.uuid = storage_id.uuid;
    elem.event_type = PartMergeLogElement::MERGE_SELECT;
    elem.event_time = time(nullptr);
    return elem;
}

void CnchMergeMutateThread::writePartMergeLogElement(
    StoragePtr & istorage, PartMergeLogElement & elem, const MergeSelectionMetrics & metrics)
{
    auto local_context = getContext();

    auto part_merge_log = local_context->getPartMergeLog();
    if (!part_merge_log)
        return;

    elem.extended = needCollectExtendedMergeMetrics();
    if (elem.extended)
    {
        std::unique_lock lock(task_records_mutex);
        for (auto & [_, task_record] : task_records)
        {
            elem.future_committed_parts += 1;
            elem.future_covered_parts += task_record->parts.size();
        }
        lock.unlock();

        auto cache_manager = local_context->getPartCacheManager();
        std::unordered_map<String, PartitionFullPtr> partition_metrics;
        cache_manager->getTablePartitionMetrics(*istorage, partition_metrics, false);
        for (auto & [_, p_metric] : partition_metrics)
        {
            elem.current_parts += p_metric->partition_info_ptr->metrics_ptr->total_parts_number;
        }
    }

    elem.duration_us = metrics.watch.elapsedMicroseconds();
    elem.get_parts_duration_us = metrics.elapsed_get_data_parts;
    elem.select_parts_duration_us = metrics.elapsed_select_parts;

    part_merge_log->add(elem);
}

bool CnchMergeMutateThread::tryMergeParts(StoragePtr & istorage, StorageCnchMergeTree & storage)
{
    std::lock_guard lock(try_merge_parts_mutex);

    LOG_TRACE(log, "Try to merge parts... pending task {} running task {}", merge_pending_queue.size(), running_merge_tasks);

    bool result = true;

    auto part_merge_log_elem = createPartMergeLogElement();
    MergeSelectionMetrics metrics;

    if (merge_pending_queue.empty())
    {
        result = trySelectPartsToMerge(istorage, storage, metrics);
    }

    /// At most $max_partition_for_multi_select tasks are expected to be submitted in one round
    auto storage_settings = storage.getSettings();
    size_t max_tasks_in_total = storage_settings->max_addition_bg_task_num;
    size_t max_tasks_in_round = storage_settings->max_partition_for_multi_select;

    for (size_t i = 0; i < max_tasks_in_round //
         && size_t(running_merge_tasks.load(std::memory_order_relaxed)) < max_tasks_in_total //
         && !merge_pending_queue.empty();
         ++i)
    {
        auto future_task = std::move(merge_pending_queue.front());
        merge_pending_queue.pop();

        part_merge_log_elem.new_tasks += 1;
        part_merge_log_elem.source_parts_in_new_tasks += future_task->record->parts.size();

        submitFutureManipulationTask(*future_task);
    }

    try
    {
        /// TODO: catch the exception during tryMergeParts() ?

        writePartMergeLogElement(istorage, part_merge_log_elem, metrics);
    }
    catch (...)
    {
        tryLogCurrentException(__PRETTY_FUNCTION__);
    }

    return result;
}

bool CnchMergeMutateThread::trySelectPartsToMerge(StoragePtr & istorage, StorageCnchMergeTree & storage, MergeSelectionMetrics & metrics)
{
    auto storage_settings = storage.getSettings();
    const bool enable_batch_select = storage_settings->cnch_merge_enable_batch_select;

    SCOPE_EXIT(
        auto total = metrics.elapsed_get_data_parts
            + metrics.elapsed_calc_visible_parts
            + metrics.elapsed_calc_merge_parts
            + metrics.elapsed_select_parts;

        if (total >= 200 * 1000)
        {
            LOG_DEBUG(
                log,
                "trySelectPartsToMerge elapsed ~{} us; get data parts elapsed {} us; calc visible parts elapsed {} us; calc merge parts "
                "elapsed {} us; select parts elapsed {} us;",
                total,
                metrics.elapsed_get_data_parts,
                metrics.elapsed_calc_visible_parts,
                metrics.elapsed_calc_visible_parts,
                metrics.elapsed_calc_merge_parts,
                metrics.elapsed_select_parts);
        });

    /// Step 1: copy currently_merging_mutating_parts
    /// Must do it before getting data parts so that the copy won't change during selection
    /// Because we can accept stale data parts but cannot accept stale merging_mutating_parts
    auto merging_mutating_parts_snapshot = copyCurrentlyMergingMutatingParts();

    /// Step 2: get parts & calc visible parts
    Stopwatch watch;
    ServerDataPartsVector data_parts;
    Strings partitions_from_cache;
    // bool cache_valid = isPartsCacheValid(storage);
    // if (cache_valid)
    // {
    //     partitions_from_cache = selectPartitionsFromCache(storage);
    //     data_parts = catalog->getServerDataPartsInPartitions(istorage, partitions_from_cache, local_context->getTimestamp(), nullptr);
    // }
    // else
    {
        data_parts = catalog->getAllServerDataParts(istorage, getContext()->getTimestamp(), nullptr);
    }
    metrics.elapsed_get_data_parts = watch.elapsedMicroseconds();
    watch.restart();

    auto visible_parts = CnchPartsHelper::calcVisibleParts(data_parts, false);
    metrics.elapsed_calc_visible_parts = watch.elapsedMicroseconds();
    watch.restart();

    if (visible_parts.size() <= 1)
    {
        LOG_TRACE(log, "There is only {} part in visible_parts, exit merge selection.", visible_parts.size());
        return false;
    }

    // Cache is invalid means data parts are all parts from catalog, so we can calculate the expected max task size.
    // if (!cache_valid)
    // {
    //     auto max_merge_task = calcTaskSize(storage, visible_parts.size());
    //     if (running_merge_tasks >= int(max_merge_task))
    //     {
    //         LOG_DEBUG(log, "Too many concurrency merge tasks(curr: {}, max: {}), will retry after.", running_merge_tasks, max_merge_task);
    //         return false;
    //     }
    // }

    /// Calc merge parts
    /// visible_parts = calcServerMergeParts(storage, visible_parts, partitions_from_cache);
    /// metrics.elapsed_calc_merge_parts = watch.elapsedMicroseconds();
    /// watch.restart();

    /// TODO: support checkpoints

    /// Step 3: selection
    std::vector<ServerDataPartsVector> res;
    [[maybe_unused]] auto decision = selectPartsToMerge(
        storage,
        res,
        visible_parts,
        getMergePred(merging_mutating_parts_snapshot),
        storage_settings->max_bytes_to_merge_at_max_space_in_pool,
        false, /// aggressive
        enable_batch_select,
        false, /// merge_with_ttl_allowed
        log); /// log

    metrics.elapsed_select_parts = watch.elapsedMicroseconds();
    watch.restart();

    LOG_DEBUG(log, "Selected {} groups candidate", res.size());

    if (res.empty())
        return false;

    /// Step 4: Save to pending queue
    for (auto & selected_parts : res)
    {
        auto future_task = std::make_unique<FutureManipulationTask>(*this, ManipulationType::Merge);
        future_task->assignSourceParts(std::move(selected_parts));

        merge_pending_queue.push(std::move(future_task));
    }
    LOG_DEBUG(log, "Push {} tasks to pending queue.", res.size());

    return true;
}

String CnchMergeMutateThread::submitFutureManipulationTask(FutureManipulationTask & future_task, bool maybe_sync_task)
{
    auto local_context = getContext();

    auto & task_record = *future_task.record;
    auto type = task_record.type;

    /// Create transaction
    future_task.prepareTransaction();
    auto & transaction = task_record.transaction;
    auto transaction_id = transaction->getTransactionID();

    /// acquire lock to prevent conflict with upsert query

    LockInfoPtr partition_lock = std::make_shared<LockInfo>(transaction_id);
    partition_lock->setMode(LockMode::X);
    partition_lock->setUUID(getStorageID().uuid);

    /*
    if (type == ManipulationType::Merge)
    {
        String partition_id = future_task.parts.front()->info().partition_id;
        partition_lock->setPartition(partition_id);

        if (transaction->tryLock(partition_lock))
            LOG_DEBUG(log, "Acquired lock in successful for partition " << partition_id);
        else
        {
            throw Exception("Failed to acquire lock for partition " + partition_id, ErrorCodes::CNCH_LOCK_ACQUIRE_FAILED);
        }
    }
    else if (type == ManipulationType::Mutate || type == ManipulationType::Clustering)
    {
        if (transaction->tryLock(partition_lock))
            LOG_DEBUG(log, "Acquired lock for table successful");
        else
        {
            throw Exception("Failed to acquire lock for table", ErrorCodes::CNCH_LOCK_ACQUIRE_FAILED);
        }
    }

    SCOPE_EXIT(if (type == ManipulationType::Merge || type == ManipulationType::Mutate || type == ManipulationType::Clustering) {
        try
        {
            transaction->unlock();
            LOG_TRACE(log, "Successful call unlock on transaction");
        }
        catch (...)
        {
            LOG_WARNING(log, "Failed to call unlock() on transaction!");
        }
    });
    */

    /// get specific version storage
    /// TODO: FIXME @yuanquan
    /// auto istorage = catalog->getTableByUUID(*local_context, toString(storage_id.uuid), future_task.calcColumnsCommitTime());
    auto istorage = catalog->getTableByUUID(*local_context, toString(storage_id.uuid), TxnTimestamp::maxTS());
    auto & cnch_table = checkAndGetCnchTable(istorage);

    /// fill task parameters
    ManipulationTaskParams params(istorage);
    params.type = type;
    params.rpc_port = local_context->getRPCPort();
    params.task_id = toString(transaction_id.toUInt64());
    params.txn_id = transaction_id.toUInt64();
    /// storage info
    params.create_table_query = cnch_table.genCreateTableQueryForWorker("");
    params.is_bucket_table = cnch_table.isBucketTable();
    /// parts
    params.assignSourceParts(future_task.parts);
    params.columns_commit_time = future_task.calcColumnsCommitTime();
    /// mutation
    if (future_task.mutation_entry)
    {
        params.mutation_commit_time = future_task.mutation_entry->commit_time;
        params.mutation_commands = std::make_shared<MutationCommands>(future_task.mutation_entry->commands);
    }

    auto worker_client = getWorker(type, future_task.parts);

    task_record.task_id = params.task_id;
    task_record.worker = worker_client;
    task_record.result_part_name = params.new_part_names.front();
    task_record.manipulation_entry = local_context->getGlobalContext()->getManipulationList().insert(params, true);
    task_record.manipulation_entry->get()->related_node = worker_client->getRPCAddress();

    try
    {
        {
            std::lock_guard lock(task_records_mutex);
            task_records[params.task_id] = future_task.moveRecord();

            if (ManipulationType::Merge == type)
                ++running_merge_tasks;
            else
                ++running_mutation_tasks;
        }

        if (maybe_sync_task)
        {
            std::lock_guard lock(currently_synchronous_tasks_mutex);
            currently_synchronous_tasks.emplace(params.task_id);
        }

        worker_client->submitManipulationTask(cnch_table, params, transaction_id, transaction->getStartTime());
        LOG_DEBUG(log, "Submitted manipulation task to {}, {}", worker_client->getHostWithPorts().toDebugString(), params.toDebugString());
    }
    catch (...)
    {
        {
            std::lock_guard lock(task_records_mutex);
            removeTaskImpl(params.task_id, lock);
        }

        throw;
    }

    return params.task_id;
}

String CnchMergeMutateThread::triggerPartMerge(
    StoragePtr & istorage, const String & partition_id, bool aggressive, bool try_select, bool try_execute)
{
    auto local_context = getContext();

    std::lock_guard lock(try_merge_parts_mutex);

    NameSet merging_mutating_parts_snapshot;
    if (!try_execute)
        merging_mutating_parts_snapshot = copyCurrentlyMergingMutatingParts();

    ServerDataPartsVector data_parts;
    if (partition_id.empty() || partition_id == "all")
        data_parts = catalog->getAllServerDataParts(istorage, local_context->getTimestamp(), nullptr);
    else
        data_parts = catalog->getServerDataPartsInPartitions(istorage, {partition_id}, local_context->getTimestamp(), nullptr);

    auto visible_parts = CnchPartsHelper::calcVisibleParts(
        data_parts, false, (try_select ? CnchPartsHelper::EnableLogging : CnchPartsHelper::DisableLogging));
    if (visible_parts.size() <= 1)
    {
        LOG_DEBUG(log, "triggerPartMerge(): size of visible_parts <= 1");
        return {};
    }

    auto & storage = checkAndGetCnchTable(istorage);
    auto storage_settings = storage.getSettings();

    std::vector<ServerDataPartsVector> res;
    [[maybe_unused]] auto decision = selectPartsToMerge(
        storage,
        res,
        visible_parts,
        getMergePred(merging_mutating_parts_snapshot),
        storage_settings->max_bytes_to_merge_at_max_space_in_pool,
        aggressive, /// aggressive
        false, /// enable_batch_select
        false, /// merge_with_ttl_allowed
        log);

    LOG_DEBUG(log, "triggerPartMerge(): Selected {} groups from {} parts.", res.size(), visible_parts.size());

    if (!try_select && !res.empty())
    {
        {
            std::lock_guard pool_lock(worker_pool_mutex);
            vw_name = storage_settings->cnch_vw_write;
            /// pick_worker_algo = storage_settings->cnch_merge_pick_worker_algo;
            vw_handle = getContext()->getVirtualWarehousePool().get(vw_name);
        }

        return submitFutureManipulationTask(
            FutureManipulationTask(*this, ManipulationType::Merge).setTryExecute(try_execute).assignSourceParts(std::move(res.front())), true);
    }

    return {};
}

void CnchMergeMutateThread::waitTasksFinish(const std::vector<String> & task_ids, UInt64 timeout_ms)
{
    Stopwatch watch;
    for (const auto & task_id: task_ids)
    {
        std::unique_lock lock(task_records_mutex);
        if (!timeout_ms)
        {
            currently_synchronous_tasks_cv.wait(lock, [&]() { return !shutdown_called && !currently_synchronous_tasks.count(task_id); });
        }
        else
        {
            auto escaped_time = watch.elapsedMilliseconds();
            if (escaped_time >= timeout_ms)
                throw Exception(ErrorCodes::TIMEOUT_EXCEEDED, "Timeout when wait MergeMutateTask `{}` finished", task_id);

            currently_synchronous_tasks_cv.wait_for(
                lock,
                std::chrono::milliseconds(timeout_ms - escaped_time),
                [&]() { return !shutdown_called && !currently_synchronous_tasks.count(task_id); });
        }
    }

    if (shutdown_called)
        throw Exception(ErrorCodes::ABORTED, "Tasks maybe canceled due to server shutdown");
}

void CnchMergeMutateThread::tryRemoveTask(const String & task_id)
{
    std::lock_guard lock(task_records_mutex);
    removeTaskImpl(task_id, lock);
}

void CnchMergeMutateThread::removeTaskImpl(const String & task_id, std::lock_guard<std::mutex> &, TaskRecordPtr * out_task_record)
{
    auto it = task_records.find(task_id);
    if (it == task_records.end())
        return;

    if (it->second->type == ManipulationType::Merge)
        --running_merge_tasks;
    else
        --running_mutation_tasks;

    if (out_task_record)
        *out_task_record = std::move(it->second);

    task_records.erase(it);
}

void CnchMergeMutateThread::finishTask(const String & task_id, const MergeTreeDataPartPtr & merged_part, std::function<void()> && commit_parts)
{
    auto local_context = getContext();

    Stopwatch watch;

    TaskRecordPtr curr_task;
    {
        std::lock_guard lock(task_records_mutex);
        auto it = task_records.find(task_id);
        if (it == task_records.end())
            throw Exception(ErrorCodes::ABORTED, "Task {} not found in this node.", task_id);
        removeTaskImpl(task_id, lock, &curr_task);
    }

    if (local_context->getRootConfig().debug_disable_merge_commit.safeGet())
        throw Exception("Disable merge commit", ErrorCodes::ABORTED);

    if (curr_task->try_execute)
    {
        LOG_DEBUG(log, "Ignored the `try_execute` task {}", task_id);
        return;
    }

    UInt64 current_ts = local_context->getPhysicalTimestamp();
    /// Check with catalog when the last_schedule_time is not updated for some time. The task can be committed only if current
    /// thread start up time equals to that in catalog.
    if (current_ts - last_schedule_time > DELAY_SCHEDULE_TIME_IN_SECOND)
    {
        UInt64 fetched_start_time = catalog->getMergeMutateThreadStartTime(storage_id);
        if (thread_start_time != fetched_start_time)
            throw Exception(
                ErrorCodes::ABORTED,
                "Task {} cannot be committed because current MergeMutateThread for {} is stale. Drop this task.",
                task_id, storage_id.getFullTableName());
    }

    commit_parts();

    auto now = time(nullptr);

    if (auto part_merge_log = local_context->getPartMergeLog())
    {
        PartMergeLogElement part_merge_log_elem;
        part_merge_log_elem.event_type = PartMergeLogElement::COMMIT;
        part_merge_log_elem.event_time = now;
        part_merge_log_elem.database = storage_id.database_name;
        part_merge_log_elem.table = storage_id.table_name;
        part_merge_log_elem.uuid = storage_id.uuid;
        part_merge_log_elem.duration_us = watch.elapsedMicroseconds();
        part_merge_log_elem.new_tasks = 1;
        part_merge_log_elem.source_parts_in_new_tasks = curr_task->parts.size();

        part_merge_log->add(part_merge_log_elem);
    }

    if (auto server_part_log = local_context->getServerPartLog())
    {
        ServerPartLogElement server_part_log_elem;
        server_part_log_elem.event_type
            = curr_task->type == ManipulationType::Merge ? ServerPartLogElement::MERGE_PARTS : ServerPartLogElement::MUTATE_PART;
        server_part_log_elem.event_time = now;
        server_part_log_elem.txn_id = curr_task->transaction->getTransactionID();
        server_part_log_elem.database_name = storage_id.database_name;
        server_part_log_elem.table_name = storage_id.table_name;
        server_part_log_elem.uuid = storage_id.uuid;
        server_part_log_elem.part_name = curr_task->result_part_name;
        server_part_log_elem.partition_id = curr_task->parts.front()->info().partition_id;
        for (auto & part : curr_task->parts)
            server_part_log_elem.source_part_names.push_back(part->name());
        server_part_log_elem.rows = merged_part->rows_count;
        server_part_log_elem.bytes = merged_part->bytes_on_disk;

        /// TODO: support error & exception
        server_part_log->add(server_part_log_elem);
    }

    LOG_TRACE(log, "Finish manipulation task {}", task_id);

    updatePartCache(curr_task->parts.front()->info().partition_id, curr_task->parts.size());
}

CnchWorkerClientPtr CnchMergeMutateThread::getWorker([[maybe_unused]] ManipulationType type, [[maybe_unused]] const ServerDataPartsVector & all_parts)
{
    std::lock_guard lock(worker_pool_mutex);
    return vw_handle->getWorker();
}

bool CnchMergeMutateThread::needCollectExtendedMergeMetrics()
{
    auto now = time(nullptr);
    if (now - last_time_collect_extended_merge_metrics >= 60 * 10)
    {
        last_time_collect_extended_merge_metrics = now;
        return true;
    }
    return false;
}

/// Mutate
bool CnchMergeMutateThread::tryMutateParts([[maybe_unused]] StoragePtr & istorage, [[maybe_unused]] StorageCnchMergeTree & storage)
{
    std::lock_guard lock(try_mutate_parts_mutex);

    auto catalog = getContext()->getCnchCatalog();
    auto all_mutations = catalog->getAllMutations(storage_id);

    if (all_mutations.empty())
        return false;

    parseMutationEntries(all_mutations, lock);

    if (current_mutations_by_version.empty())
        return false;

    const auto & entry = current_mutations_by_version.begin()->second;
    /// Set the `current_mutate_entry` to the first mutate entry
    if (!current_mutate_entry.has_value())
    {
        current_mutate_entry = std::make_optional<CnchMergeTreeMutationEntry>(entry);
    }
    else if (current_mutate_entry->commit_time != entry.commit_time)
    {
        /// Should not happen
        LOG_WARNING(log, "Current mutation entry missed: {}, found {}", current_mutate_entry->commit_time, entry.commit_time);
        scheduled_mutation_partitions.clear();
        finish_mutation_partitions.clear();
        current_mutate_entry = std::make_optional<CnchMergeTreeMutationEntry>(entry);
    }

    auto generate_tasks = [&](const ServerDataPartsVector & visible_parts, const NameSet & merging_mutating_parts_snapshot)
    {
        auto type = current_mutate_entry->isReclusterMutation() ? ManipulationType::Clustering : ManipulationType::Mutate;

        bool found_tasks = false;
        size_t curr_mutate_part_size = 0;
        ServerDataPartsVector alter_parts;

        for (const auto & part : visible_parts)
        {
            /// We wouldn't commit new storage version for BUILD_BITMAP, MATERIALIZED_INDEX, CLEAR_MAP_KEY command
            /// so we use mutation_commit_time to check whether the part already has executed this alter command.
            if (part->getColumnsCommitTime() >= current_mutate_entry->commit_time
                || part->getMutationCommitTime() >= current_mutate_entry->commit_time)
                continue;

            found_tasks = true;

            if (merging_mutating_parts_snapshot.count(part->name()))
                continue;

            alter_parts.push_back(part);
            curr_mutate_part_size += part->part_model().size();

            if (alter_parts.size() >= max_mutate_part_num || curr_mutate_part_size >= max_mutate_part_size)
            {
                submitFutureManipulationTask(
                    FutureManipulationTask(*this, type).setMutationEntry(*current_mutate_entry).assignSourceParts(std::move(alter_parts)));

                alter_parts.clear();
                curr_mutate_part_size = 0;
                if (running_mutation_tasks >= storage.getSettings()->max_addition_mutation_task_num)
                    return true;
            }
        }

        if (!alter_parts.empty())
        {
            submitFutureManipulationTask(
                FutureManipulationTask(*this, type).setMutationEntry(*current_mutate_entry).assignSourceParts(std::move(alter_parts)));
        }

        return found_tasks;
    };

    auto check_parts = [&](const ServerDataPartsVector & visible_parts, const ServerDataPartsVector & visible_staged_parts)
    {
        const auto & commit_ts = current_mutate_entry->commit_time;

        for (const auto & part : visible_parts)
        {
            if (part->getColumnsCommitTime() < commit_ts && part->getMutationCommitTime() < commit_ts)
                return false;
        }

        for (const auto & part : visible_staged_parts)
        {
            if (part->getColumnsCommitTime() <= commit_ts && part->getMutationCommitTime() <= commit_ts)
                return false;
        }

        return true;
    };

    /// generate mutations tasks for the earliest mutation
    /// TODO: verify tables which don't have partition key
    bool is_finish = true;
    auto partition_id_list = catalog->getPartitionIDs(istorage, nullptr);

    for (const auto & partition_id : partition_id_list)
    {
        if (running_mutation_tasks > storage.getSettings()->max_addition_mutation_task_num)
        {
            is_finish = false;
            break;
        }

        if (finish_mutation_partitions.find(partition_id) != finish_mutation_partitions.end())
            continue;

        auto merging_mutating_parts_snapshot = copyCurrentlyMergingMutatingParts();
        auto parts = catalog->getServerDataPartsInPartitions(istorage, {partition_id}, getContext()->getTimestamp(), nullptr);
        auto visible_parts = CnchPartsHelper::calcVisibleParts(parts, false);

        if (scheduled_mutation_partitions.contains(partition_id))
        {
            /// TODO: check staged parts for unique table.
            if (check_parts(visible_parts, {}))
                finish_mutation_partitions.emplace(partition_id);
        }
        else if (!generate_tasks(visible_parts, merging_mutating_parts_snapshot))
        {
            scheduled_mutation_partitions.emplace(partition_id);
        }
        else
        {
            is_finish = false;
        }
    }

    if (!is_finish)
        return true;

    bool newest_cluster_by = true;
    const auto & commit_ts = current_mutate_entry->commit_time;
    /// Check whether there is any newer recluter mutation
    for (auto & mutation : current_mutations_by_version)
    {
        if (mutation.first > commit_ts && mutation.second.isReclusterMutation())
        {
            newest_cluster_by = false;
            break;
        }
    }

    removeMutationEntry(commit_ts, newest_cluster_by, lock);

    /// clear
    scheduled_mutation_partitions.clear();
    finish_mutation_partitions.clear();
    current_mutate_entry.reset();

    return false;
}

void CnchMergeMutateThread::parseMutationEntries(const Strings & all_mutations, std::lock_guard<std::mutex> &)
{
    for (const auto & mutate : all_mutations)
    {
        try
        {
            auto entry = CnchMergeTreeMutationEntry::parse(mutate);
            current_mutations_by_version.try_emplace(entry.commit_time, entry);
        }
        catch (...)
        {
            tryLogCurrentException(__PRETTY_FUNCTION__, "Error when parse mutation: " + mutate);
        }
    }
}

void CnchMergeMutateThread::removeMutationEntry(const TxnTimestamp & commit_ts, bool recluster_finish, std::lock_guard<std::mutex> &)
{
    auto it = current_mutations_by_version.find(commit_ts);
    if (it == current_mutations_by_version.end())
        return;

    const auto & entry = it->second;

    /// When is mutation allowed to be deleted?
    /// Consider the following case:
    ///     T1: Insert part_1(invisible, using metadata in T1).
    ///     T2: Submit an alter query (Create a mutation which commit_ts = T2).
    ///     T3: Alter query done (According current visible parts).
    ///     T4: Insert part_1 successful.
    /// We can only allow the mutation to be deleted after T4 to ensure part_1 can be modified.
    /// At T3, the MinActiveTimestamp should be T1, T1 < T2, so, the Mutation should not allow to remove.
    /// At T4, the MinActiveTimestamp should be greater than T2, so, the Mutation could be remove.
    auto min_active_ts = calculateMinActiveTimestamp();
    if (min_active_ts <= commit_ts)
        return;

    /// modify cluster status before removing recluster mutation entry.
    if (entry.isReclusterMutation() && recluster_finish)
    {
        LOG_DEBUG(log, "All data parts are clustered in table {}, reset cluster status.", storage_id.getNameForLogs());
        catalog->setTableClusterStatus(storage_id.uuid, true);
    }

    WriteBufferFromOwnString buf;
    entry.commands.writeText(buf);
    LOG_DEBUG(log, "Mutation {}(command: {}) has been done, will remove it from catalog.",  it->first,  buf.str());
    getContext()->getCnchCatalog()->removeMutation(storage_id, entry.txn_id.toString());
    it = current_mutations_by_version.erase(it);
}

void CnchMergeMutateThread::triggerPartMutate(StoragePtr storage)
{
    auto * cnch = typeid_cast<StorageCnchMergeTree *>(storage.get());
    if (!cnch)
        return;

    {
        std::lock_guard pool_lock(worker_pool_mutex);
        vw_name = cnch->getSettings()->cnch_vw_write;
        /// pick_worker_algo = storage_settings->cnch_merge_pick_worker_algo;
        vw_handle = getContext()->getVirtualWarehousePool().get(vw_name);
        if (!vw_handle)
            return;
    }

    tryMutateParts(storage, *cnch);
}

}
