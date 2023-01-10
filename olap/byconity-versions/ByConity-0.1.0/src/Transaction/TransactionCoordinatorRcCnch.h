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

#include <optional>
#include <Transaction/IntentLock.h>
#include <Transaction/TimestampCacheManager.h>
#include <Transaction/ICnchTransaction.h>
#include <Transaction/TransactionCleaner.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/CnchProxyTransaction.h>
#include <Transaction/CnchServerTransaction.h>
#include <Transaction/CnchExplicitTransaction.h>
#include <Common/HostWithPorts.h>
#include "Interpreters/Context_fwd.h"

namespace DB
{
class Context;


struct CreateTransactionOption
{
        ContextPtr query_context = nullptr;                                     // query context, optional
        bool read_only = false;                                                 // read only txn
        TxnTimestamp primary_txn_id = {0};                                      // primary txn id if create secondary txn
        CnchTransactionType type = CnchTransactionType::Implicit;               // implicit or explicit
        CnchTransactionInitiator initiator = CnchTransactionInitiator::Server;  // initiator of txn
        CnchTransactionPriority priority = CnchTransactionPriority::high;       // priority when cleaning
        TxnTimestamp txn_hint = {0};                                            // hint
        bool force_clean_by_dm = false;                                         // force clean by dm

        CreateTransactionOption() = default;
        ~CreateTransactionOption() = default;
        CreateTransactionOption & setContext(ContextPtr context) { query_context = std::move(context); return *this; }
        CreateTransactionOption & setReadOnly(bool val) { read_only = val; return *this; }
        CreateTransactionOption & setPrimaryTransactionId(TxnTimestamp id) { this->primary_txn_id = id; return *this; }
        CreateTransactionOption & setType(CnchTransactionType type_) { this->type = type_; return *this; }
        CreateTransactionOption & setInitiator(CnchTransactionInitiator initiator_) { this->initiator = initiator_; return *this; }
        CreateTransactionOption & setPriority(CnchTransactionPriority priority_) { this->priority = priority_; return *this; }
        CreateTransactionOption & setTxnHint(TxnTimestamp txn_hint_) { this->txn_hint = txn_hint_; return *this; }
        CreateTransactionOption & setForceCleanByDM(bool force_clean_by_dm_) { this->force_clean_by_dm = force_clean_by_dm_; return *this; }

        void validate() const { } // TODO: throw on invalid options
};

/// TransactionCoordinatorRcCNCH implements the read committed isolation level.
/// Each server includes one transaction to handle the transaction received.
class TransactionCoordinatorRcCnch : WithContext
{
public:
    explicit TransactionCoordinatorRcCnch(const ContextPtr & context_)
        : WithContext(context_)
        , ts_cache_manager(std::make_unique<TimestampCacheManager>(getContext()->getConfigRef().getUInt("max_tscache_size", 1000)))
        , txn_cleaner(std::make_unique<TransactionCleaner>(
              getContext(),
              getContext()->getConfigRef().getUInt("cnch_transaction_cleaner_max_threads", 128),
              getContext()->getConfigRef().getUInt("cnch_transaction_cleaner_queue_size", 10000),
              getContext()->getConfigRef().getUInt("cnch_transaction_cleaner_dm_max_threads", 32),
              getContext()->getConfigRef().getUInt("cnch_transaction_cleaner_dm_queue_size", 10000)))
        , scan_interval(getContext()->getConfigRef().getInt("cnch_transaction_list_scan_interval", 10 * 60 * 1000)) // default 10 mins
        , log(&Poco::Logger::get("TransactionCoordinator"))
    {
        scan_active_txns_task = getContext()->getSchedulePool().createTask("ScanActiveTxnsTask", [this]() { scanActiveTransactions(); });
        scan_active_txns_task->activate();
        scan_active_txns_task->scheduleAfter(scan_interval);
    }

    ~TransactionCoordinatorRcCnch()
    {
        try
        {
            shutdown();
        }
        catch (...)
        {
            tryLogCurrentException(log);
        }
    }

    TransactionCoordinatorRcCnch(const TransactionCoordinatorRcCnch &) = delete;
    TransactionCoordinatorRcCnch & operator=(const TransactionCoordinatorRcCnch &) = delete;


    /// API related to Pessimistic and Optimistic implementation
    /// Implement pessimistic way fist.
    /// Related API

    // Transaction inherits from ICnchTransaction:
    //
    //           --------------------ICnchTransaction ------------------
    //           |                          |                          |
    // CnchExplicitTransaction   CnchImplicitTransaction     CnchProxyTransaction
    //                                      |
    //                           CnchInternalTransaction (Merge, Kafka, Mem-buffer)
    //
    // The explicit txn is created by BEGIN / BEGIN TRANSACTION statement. The implicit txn is created
    // for each query implicitly. In addition, an implicit txn can also be Primary or Secondary:
    // 1. If the query is a single query, then its txn is Primary.
    // 2. If the query is in BEGIN / COMMIT or BEGIN / ROLLBACK block, then its txn is Secondary.
    // 3. (NOT IMPLEMENTED) MV support
    // Secondary txns will have a primary_txn_id, which is the txn_id of an explicit txn. If a txn is
    // primary, then it will have a primary_txn_id of itself (primary_txn_id == txn_id).
    // Secondary txns have two differences from normal implicit txns:
    // 1. They do not own data - data is owned by the explicit txn (parts mutation is primary_txn_id).
    // 2. After commit, their data is only visible to other query that are in the same explicit txn.

    // TODO: @ducle.canh - change CnchServerTransaction to CnchImplicitTransaction, remove CnchWorkerTransaction.
    // If we forward a query forward to worker, then use CnchProxyTransaction on worker. Internal transaction
    // such as kafka related should has their own class.

    // create transactions
    // used for all queries including read only and writes.
    TransactionCnchPtr createTransaction(const CreateTransactionOption & opt = {});

    // create proxy transaction
    // used when forward query to other server (or worker)
    ProxyTransactionPtr createProxyTransaction(
        const HostWithPorts & host_ports,
        TxnTimestamp primary_txn_id = {0}
    );

    TransactionCnchPtr getTransaction(const TxnTimestamp & txn_id) const;
    bool isActiveTransaction(const TxnTimestamp & txn_id) const;
    void finishTransaction(const TransactionCnchPtr & cur, bool force_finsh = false);
    void finishTransaction(const TxnTimestamp & txn_id, bool force_finish = false);

    /// commit is the old API which performs data write and txn commit in one api calls.
    /// commitV2 is the new API which separate data write and txn commit with 2 api calls.
    /// Currently, still keep both of them as ddl is still executed with old api because the db/table metadata still does not support intermediate state.
    TxnTimestamp commitV1(TransactionCnchPtr & txn) const;
    TxnTimestamp commitV2(TransactionCnchPtr & txn) const;

    // clear related api used by background scan task
    bool clearZombieParts(const std::vector<String> & parts);

    TimestampCacheManager & getTsCacheManager() const { return *ts_cache_manager; }
    TransactionCleaner & getTxnCleaner() const { return *txn_cleaner; }

    CnchTransactionStatus getTransactionStatus(const TxnTimestamp & txnID) const;

    void touchActiveTimestampByTable(const StorageID & storage_id, const TransactionCnchPtr & txn);
    std::map<UUID, StorageID> getReadTablesByTimestamp(TxnTimestamp ts) const;
    std::optional<TxnTimestamp> getMinActiveTimestamp(const StorageID & storage_id);

    auto getActiveTransactions() const
    {
        std::lock_guard lock(list_mutex);
        return active_txn_list;
    }

    void shutdown()
    {
        scan_active_txns_task->deactivate();
        txn_cleaner->finalize();
    }

private:
    void eraseActiveTimestamp(const TransactionCnchPtr & txn);

    void scanActiveTransactions();

    mutable std::mutex list_mutex;
    /// transaction related data structure, including txn info, tsCache, mutex.
    std::map<TxnTimestamp, TransactionCnchPtr> active_txn_list;

    mutable std::mutex min_ts_mutex;
    std::map<TxnTimestamp, std::map<UUID, StorageID>> timestamp_to_tables;
    std::map<UUID, std::set<TxnTimestamp>> table_to_timestamps;
    uint64_t last_time_clean_timestamps;

    TimestampCacheManagerPtr ts_cache_manager;
    TransactionCleanerPtr txn_cleaner;

    // background tasks
    UInt64 scan_interval;
    BackgroundSchedulePool::TaskHolder scan_active_txns_task;

    Poco::Logger * log;
};

}
