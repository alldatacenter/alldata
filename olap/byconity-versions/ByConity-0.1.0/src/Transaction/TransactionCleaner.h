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

#include <Transaction/ICnchTransaction.h>
#include <Transaction/TxnTimestamp.h>
#include <Common/Stopwatch.h>
#include <Common/Exception.h>
#include <Common/ThreadPool.h>
#include <Interpreters/Context_fwd.h>

#include <mutex>

namespace DB
{

/// Server transaction clean tasks should have higher execution priority
/// than DM triggered clean tasks.
enum class CleanTaskPriority : int
{
    LOW = 0,
    HIGH = 1,
};

struct TxnCleanTask
{
    TxnCleanTask() = default;

    TxnCleanTask(const TxnTimestamp & txn_id_, CleanTaskPriority priority_, CnchTransactionStatus status)
        : txn_id(txn_id_), priority(priority_), txn_status(status)
    {
    }

    UInt64 elapsed() const
    {
        return watch.elapsedMilliseconds();
    }

    TxnTimestamp txn_id;
    CleanTaskPriority priority;
    CnchTransactionStatus txn_status;
    UInt32 undo_size {0};
    Stopwatch watch;
    mutable std::mutex mutex;
};

using TxnCleanTasks = std::vector<TxnCleanTask>;

/// Run transaction clean task in the background
class TransactionCleaner : WithContext
{
public:
    TransactionCleaner(
        const ContextPtr & global_context, size_t server_max_threads, size_t server_queue_size, size_t dm_max_threads, size_t dm_queue_size)
        : WithContext(global_context)
        , server_thread_pool(std::make_unique<ThreadPool>(server_max_threads, server_max_threads, server_queue_size))
        , dm_thread_pool(std::make_unique<ThreadPool>(dm_max_threads, dm_max_threads, dm_queue_size))
    {
    }

    ~TransactionCleaner();

    TransactionCleaner(const TransactionCleaner &) = delete;
    TransactionCleaner & operator=(const TransactionCleaner &) = delete;

    void cleanTransaction(const TransactionCnchPtr & txn);
    void cleanTransaction(const TransactionRecord & txn_record);

    using TxnCleanTasksMap = std::unordered_map<UInt64, TxnCleanTask>;
    const TxnCleanTasksMap & getAllTasksUnLocked() const {return clean_tasks;}
    std::unique_lock<std::mutex> getLock() const { return std::unique_lock(mutex); }

    TxnCleanTask & getCleanTask(const TxnTimestamp & txn_id);

    void finalize();

private:
    // brpc client default timeout 3 seconds.
    // Wait for maximum 2 seconds to enqueue tasks.
    static constexpr uint64_t wait_for_schedule = 2000000; // in us = 2s

    template <typename... Args>
    bool tryRegisterTask(const TxnTimestamp & txn_id, Args &&... args)
    {
        std::lock_guard lock(mutex);
        return !shutdown && clean_tasks.try_emplace(txn_id.toUInt64(), txn_id, std::forward<Args>(args)...).second;
    }

    void cleanCommittedTxn(const TransactionRecord & txn_record);
    void cleanAbortedTxn(const TransactionRecord & txn_record);

    void removeTask(const TxnTimestamp & txn_id);

    template <typename F, typename... Args>
    void scheduleTask(F && f, CleanTaskPriority priority, TxnTimestamp txn_id, Args &&... args)
    {
        LOG_DEBUG(log, "start schedule clean task for transaction {}\n", txn_id.toUInt64());
        if (!tryRegisterTask(txn_id, priority, std::forward<Args>(args)...))
        {
            LOG_DEBUG(log, "The clean task of txn " + txn_id.toString() + " is already running.");
            return;
        }

        auto & thread_pool = (priority == CleanTaskPriority::HIGH) ? *server_thread_pool : *dm_thread_pool;

        bool res = thread_pool.trySchedule(
            [this, f = std::forward<F>(f), txn_id] {
                try
                {
                    f();
                }
                catch (...)
                {
                    tryLogCurrentException(log, __PRETTY_FUNCTION__);
                }
                removeTask(txn_id);
            },
            wait_for_schedule);

        if (!res)
        {
            removeTask(txn_id);
            LOG_WARNING(log, "TransactionCleaner queue is full. Clean task of transaction {} will be rescheduled by dm\n", txn_id.toUInt64());
        }
        else
            LOG_DEBUG(log, "Successfully schedule clean task in cleaner queue for transaction {}\n", txn_id.toUInt64());
    }

private:
    std::unique_ptr<ThreadPool> server_thread_pool;
    std::unique_ptr<ThreadPool> dm_thread_pool;

    mutable std::mutex mutex;
    TxnCleanTasksMap clean_tasks;
    bool shutdown{false};
    Poco::Logger * log = &Poco::Logger::get("TransactionCleaner");
};

using TransactionCleanerPtr = std::unique_ptr<TransactionCleaner>;
}
