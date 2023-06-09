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
#include <CloudServices/ICnchBGThread.h>

#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Interpreters/VirtualWarehouseHandle.h>
#include <Interpreters/VirtualWarehousePool.h>
#include <Storages/MergeTree/CnchMergeTreeMutationEntry.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Transaction/ICnchTransaction.h>
#include <WorkerTasks/ManipulationList.h>

#include <condition_variable>

namespace DB
{

class CnchWorkerClient;
using CnchWorkerClientPtr = std::shared_ptr<CnchWorkerClient>;

class CnchMergeMutateThread;
struct PartMergeLogElement;

struct ManipulationTaskRecord
{
    explicit ManipulationTaskRecord(CnchMergeMutateThread & p) : parent(p) {}
    ~ManipulationTaskRecord();

    CnchMergeMutateThread & parent;

    ManipulationType type;
    std::unique_ptr<ManipulationListEntry> manipulation_entry;
    bool try_execute{false};

    bool remove_task{false};

    String task_id;
    TransactionCnchPtr transaction;

    ServerDataPartsVector parts;

    /// for heartbeat
    CnchWorkerClientPtr worker;
    size_t lost_count{0};

    /// for system.part_merge_log & system.server_part_log
    String result_part_name;
};

struct FutureManipulationTask
{
    FutureManipulationTask(CnchMergeMutateThread & p, ManipulationType t) : parent(p), record(std::make_unique<ManipulationTaskRecord>(p))
    {
        record->type = t;
    }

    ~FutureManipulationTask();

    FutureManipulationTask & setTryExecute(bool try_execute_)
    {
        record->try_execute = this->try_execute = try_execute_;
        return *this;
    }

    FutureManipulationTask & setMutationEntry(CnchMergeTreeMutationEntry m)
    {
        mutation_entry.emplace(std::move(m));
        return *this;
    }

    TxnTimestamp calcColumnsCommitTime() const;
    FutureManipulationTask & assignSourceParts(ServerDataPartsVector && parts);
    FutureManipulationTask & prepareTransaction();
    std::unique_ptr<ManipulationTaskRecord> moveRecord();

    CnchMergeMutateThread & parent;

    bool try_execute{false};

    std::unique_ptr<ManipulationTaskRecord> record;
    ServerDataPartsVector parts;
    std::optional<CnchMergeTreeMutationEntry> mutation_entry;
};

struct MergeSelectionMetrics
{
    Stopwatch watch;
    size_t elapsed_get_data_parts = 0;
    size_t elapsed_calc_visible_parts = 0;
    size_t elapsed_calc_merge_parts = 0;
    size_t elapsed_select_parts = 0;
};

class CnchMergeMutateThread : public ICnchBGThread
{
    using TaskRecord = ManipulationTaskRecord;
    using TaskRecordPtr = std::unique_ptr<TaskRecord>;

    friend struct ManipulationTaskRecord;
    friend struct FutureManipulationTask;

public:
    CnchMergeMutateThread(ContextPtr context, const StorageID & id);
    ~CnchMergeMutateThread() override;

    void shutdown();

    void tryRemoveTask(const String & task_id);
    void finishTask(const String & task_id, const MergeTreeDataPartPtr & merged_part, std::function<void()> && commit_parts);
    bool removeTasksOnPartition(const String & partition_id);

    String triggerPartMerge(StoragePtr & istorage, const String & partition_id, bool aggressive, bool try_select, bool try_execute);
    void triggerPartMutate(StoragePtr storage);

    void waitTasksFinish(const std::vector<String> & task_ids, UInt64 timeout_ms);

private:
    void preStart() override;

    void runHeartbeatTask();

    void runImpl() override;

    /// Merge
    PartMergeLogElement createPartMergeLogElement();
    void writePartMergeLogElement(StoragePtr & istorage, PartMergeLogElement & elem, const MergeSelectionMetrics & metrics);

    bool tryMergeParts(StoragePtr & istorage, StorageCnchMergeTree & storage);
    bool trySelectPartsToMerge(StoragePtr & istorage, StorageCnchMergeTree & storage, MergeSelectionMetrics & metrics);
    String submitFutureManipulationTask(FutureManipulationTask & future_task, bool maybe_sync_task = false);

    // Mutate
    bool tryMutateParts(StoragePtr & istorage, StorageCnchMergeTree & storage);
    void parseMutationEntries(const Strings & all_mutations, std::lock_guard<std::mutex> &);
    void removeMutationEntry(const TxnTimestamp & commit_ts, bool recluster_finish, std::lock_guard<std::mutex> &);

    void removeTaskImpl(const String & task_id, std::lock_guard<std::mutex> & lock, TaskRecordPtr * out_task_record = nullptr);

    CnchWorkerClientPtr getWorker(ManipulationType type, const ServerDataPartsVector & all_parts);

    bool needCollectExtendedMergeMetrics();

    auto copyCurrentlyMergingMutatingParts()
    {
        std::lock_guard lock(currently_merging_mutating_parts_mutex);
        return currently_merging_mutating_parts;
    }

    std::mutex currently_merging_mutating_parts_mutex;
    NameSet currently_merging_mutating_parts;

    std::condition_variable currently_synchronous_tasks_cv; /// for waitTasksFinish function
    std::mutex currently_synchronous_tasks_mutex;
    NameSet currently_synchronous_tasks;

    std::mutex task_records_mutex;
    std::unordered_map<String, TaskRecordPtr> task_records;

    std::mutex try_merge_parts_mutex; /// protect tryMergeParts(), triggerPartMerge()
    std::queue<std::unique_ptr<FutureManipulationTask>> merge_pending_queue;

    std::mutex try_mutate_parts_mutex; /// protect tryMutateParts(), getMutationStatus()
    NameSet scheduled_mutation_partitions;
    NameSet finish_mutation_partitions;
    std::optional<CnchMergeTreeMutationEntry> current_mutate_entry;
    std::map<TxnTimestamp, CnchMergeTreeMutationEntry> current_mutations_by_version;

    /// Separate quota for merge & mutation tasks
    std::atomic<int> running_merge_tasks{0};
    std::atomic<int> running_mutation_tasks{0};

    time_t last_heartbeat_time = 0;
    time_t last_time_collect_extended_merge_metrics = 0;

    /// the start time of current MergeMutateThread.
    UInt64 thread_start_time{0};
    /// the last schedule time of MergeMutateThread. Its a physical timestamp.
    UInt64 last_schedule_time{0};

    std::mutex worker_pool_mutex;
    String vw_name;
    String pick_worker_algo;
    VirtualWarehouseHandle vw_handle;

    std::atomic_bool shutdown_called{false};

    /// Index for round-robin strategy when picking worker.
    std::atomic<size_t> merge_worker_round_robin_index = 0;
    std::atomic<size_t> mutation_worker_round_robin_index = 0;
};


}
