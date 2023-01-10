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

#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <memory>

#include <Catalog/Catalog.h>
#include <Databases/DatabaseCnch.h>
// #include <MergeTreeCommon/CnchServerClient.h>
// #include <Storages/StorageCnchMergeTree.h>
// #include <TSO/TSOClient.h>
// #include <TSO/TSOImpl.h>
#include <Common/Configurations.h>
#include <Common/Exception.h>
#include <Common/ProfileEvents.h>
#include <Common/ThreadPool.h>
#include <common/getFQDNOrHostName.h>
#include <Interpreters/Context.h>
// #include <MergeTreeCommon/CnchWorkerClientPools.h>

namespace ProfileEvents
{
extern const Event CnchTxnReadTxnCreated;
extern const Event CnchTxnWriteTxnCreated;
extern const Event CnchTxnExpired;
}

namespace DB
{
namespace ErrorCodes
{
    // extern const int BAD_CAST;
    extern const int LOGICAL_ERROR;
    extern const int CNCH_TRANSACTION_NOT_INITIALIZED;
    extern const int CNCH_TRANSACTION_HAS_BEEN_CREATED;
}

TransactionCnchPtr TransactionCoordinatorRcCnch::createTransaction(const CreateTransactionOption & opt)
{
    TxnTimestamp txn_id = opt.txn_hint;
    if (!txn_id)
        txn_id = opt.read_only ? getContext()->tryGetTimestamp(__PRETTY_FUNCTION__) : getContext()->getTimestamp();

    /// fallbackTS is only returned in case of exceptions, thus TSO succeeded if the txn_id is not this value
    bool tso_get_timestamp_succeeded = txn_id != TxnTimestamp::fallbackTS();

    TransactionRecord txn_record;
    txn_record.setID(txn_id)
        .setType(opt.type)
        .setPrimaryID(opt.primary_txn_id)
        .setStatus(CnchTransactionStatus::Running)
        .setPriority(opt.priority)
        .setLocation(getContext()->getHostWithPorts().getRPCAddress())
        .setInitiator(txnInitiatorToString(opt.initiator));

    txn_record.read_only = opt.read_only;

    ContextPtr txn_context;
    if (opt.query_context)
        txn_context = opt.query_context;
    else
        txn_context = getContext();

    TransactionCnchPtr txn = nullptr;
    if (opt.type == CnchTransactionType::Implicit)
        txn = std::make_shared<CnchServerTransaction>(txn_context, txn_record);
    else if (opt.type == CnchTransactionType::Explicit)
        txn = std::make_shared<CnchExplicitTransaction>(txn_context, txn_record);
    else
        throw Exception("Unknown transaction type", ErrorCodes::LOGICAL_ERROR);

    /// Only add txn_id to active_txn_list if TSO succeeded. Otherwise it might lead to multiple maxTS at the same time
    if (tso_get_timestamp_succeeded)
    {
        std::lock_guard<std::mutex> lock(list_mutex);
        if (!active_txn_list.emplace(txn_id, txn).second)
            throw Exception("Transaction (txn_id: " + txn_id.toString() + ") has been created", ErrorCodes::CNCH_TRANSACTION_HAS_BEEN_CREATED);
    }

    if (!txn->isReadOnly() && txn->isSecondary() && opt.initiator != CnchTransactionInitiator::Txn)
    {
        /// add txn to its primary txn's secondary txn list
        auto * primary_txn = getTransaction(txn->getPrimaryTransactionID())->as<CnchExplicitTransaction>();
        if (primary_txn)
            primary_txn->addSecondaryTransaction(txn);
    }

    txn->force_clean_by_dm = opt.force_clean_by_dm;

    ProfileEvents::increment((opt.read_only ? ProfileEvents::CnchTxnReadTxnCreated : ProfileEvents::CnchTxnWriteTxnCreated));
    LOG_DEBUG(log, "Created txn {}\n", txn->getTransactionRecord().toString());
    return txn;
}

ProxyTransactionPtr TransactionCoordinatorRcCnch::createProxyTransaction(
    const HostWithPorts & host_ports,
    TxnTimestamp primary_txn_id)
    {
        /// Get the rpc client of target server
        auto server_cli = getContext()->getCnchServerClient(host_ports);
        auto txn = std::make_shared<CnchProxyTransaction>(getContext(), server_cli, primary_txn_id);
        auto txn_id = txn->getTransactionID();
        /// add to active txn list
        {
            std::lock_guard<std::mutex> lock(list_mutex);
            if (!active_txn_list.emplace(txn_id, txn).second)
                throw Exception("Transaction (txn_id: " + txn_id.toString() + ") has been created", ErrorCodes::LOGICAL_ERROR);
        }
        /// add txn to its primary txn's secondary txn list
        auto *primary_txn = getTransaction(txn->getPrimaryTransactionID())->as<CnchExplicitTransaction>();
        if (primary_txn) primary_txn->addSecondaryTransaction(txn);
        LOG_DEBUG(log, "Created proxy txn {}\n", txn->getTransactionRecord().toString());
        return txn;
    }

TransactionCnchPtr TransactionCoordinatorRcCnch::getTransaction(const TxnTimestamp & txnID) const
{
    std::lock_guard lock(list_mutex);
    auto it = active_txn_list.find(txnID);
    if (it == active_txn_list.end())
    {
        throw Exception("Transaction " + txnID.toString() + " not found", ErrorCodes::CNCH_TRANSACTION_NOT_INITIALIZED);
    }

    return it->second;
}

bool TransactionCoordinatorRcCnch::isActiveTransaction(const TxnTimestamp & txn_id) const
{
    std::lock_guard lock(list_mutex);
    return active_txn_list.find(txn_id) != active_txn_list.end();
}

void TransactionCoordinatorRcCnch::finishTransaction(const TransactionCnchPtr & cur, bool force_finish)
{
    /// If this is secondary write transaction, let its primary txn force finish it when commit or rollback.
    if (!cur || (!force_finish && cur->isSecondary() && !cur->isReadOnly()))
    {
        return;
    }
    try
    {
        {
            std::lock_guard lock(list_mutex);

            if (auto it = active_txn_list.find(cur->getTransactionID()); it != active_txn_list.end())
            {
                active_txn_list.erase(it);
                LOG_DEBUG(log, "Deleted txn {}\n", cur->getTransactionID());
            }
        }
        eraseActiveTimestamp(cur);
        /// TODO: for explicit transaction, at this point, the secondary transaction doesn't play any role
        /// can release them and let them die as normal primary transaction
        if (!cur->isReadOnly())
            txn_cleaner->cleanTransaction(cur);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void TransactionCoordinatorRcCnch::finishTransaction(const TxnTimestamp & txn_id, bool force_finish)
{
    try
    {
        TransactionCnchPtr cur = getTransaction(txn_id);
        return finishTransaction(cur, force_finish);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        throw;
    }
}

TxnTimestamp TransactionCoordinatorRcCnch::commitV1(TransactionCnchPtr & txn) const
{
    return txn->commitV1();
}

TxnTimestamp TransactionCoordinatorRcCnch::commitV2(TransactionCnchPtr & txn) const
{
    return txn->commitV2();
}

CnchTransactionStatus TransactionCoordinatorRcCnch::getTransactionStatus(const TxnTimestamp & txnID) const
{
    TransactionCnchPtr txn;
    {
        std::lock_guard<std::mutex> lock(list_mutex);
        if (auto it = active_txn_list.find(txnID); it != active_txn_list.end())
        {
            txn = it->second;
        }
    }

    if (txn)
        return txn->getStatus();

    return CnchTransactionStatus::Inactive;
}

void TransactionCoordinatorRcCnch::touchActiveTimestampByTable(const StorageID & storage_id, const TransactionCnchPtr & txn)
{
    auto ts = txn->getStartTime();

    std::lock_guard<std::mutex> min_ts_lock(min_ts_mutex);
    timestamp_to_tables[ts].try_emplace(storage_id.uuid, storage_id);
    table_to_timestamps[storage_id.uuid].insert(ts);
}

std::map<UUID, StorageID> TransactionCoordinatorRcCnch::getReadTablesByTimestamp(TxnTimestamp ts) const
{
    std::lock_guard<std::mutex> min_ts_lock(min_ts_mutex);
    auto it = timestamp_to_tables.find(ts);
    if (it != timestamp_to_tables.end())
        return it->second;
    return {};
}

void TransactionCoordinatorRcCnch::eraseActiveTimestamp(const TransactionCnchPtr & txn)
{
    auto ts = txn->getStartTime();

    std::lock_guard<std::mutex> min_ts_lock(min_ts_mutex);
    auto tables_it = timestamp_to_tables.find(ts);
    if (tables_it == timestamp_to_tables.end())
    {
        LOG_TRACE(log, "No need to erase active timestamp for txn {}\n", txn->getTransactionID().toUInt64());
        return;
    }

    for (auto & table : tables_it->second)
    {
        auto uuid = table.first;
        auto timestamps_it = table_to_timestamps.find(uuid);
        if (timestamps_it != table_to_timestamps.end())
        {
            timestamps_it->second.erase(ts);
            if (timestamps_it->second.empty())
                table_to_timestamps.erase(timestamps_it);
        }
    }

    if (tables_it != timestamp_to_tables.end())
        timestamp_to_tables.erase(tables_it);
}

/// Get the minimum active timestamp for table and clean all outdated timestamps.
std::optional<TxnTimestamp> TransactionCoordinatorRcCnch::getMinActiveTimestamp(const StorageID & storage_id)
{
    const UInt64 expired_interval = getContext()->getRootConfig().cnch_transaction_ts_expire_time; // default 2h
    auto now = UInt64(time(nullptr)) * 1000;

    std::lock_guard<std::mutex> lock(min_ts_mutex);
    auto timestamps_it = table_to_timestamps.find(storage_id.uuid);
    if (timestamps_it == table_to_timestamps.end() || timestamps_it->second.empty())
        return std::nullopt;

    if (now - last_time_clean_timestamps < expired_interval)
        return *(timestamps_it->second.begin());

    try
    {
        /// Try to clean all outdated Txns.
        last_time_clean_timestamps = now;
        TxnTimestamp cur_ts = getContext()->getTimestamp();
        for (auto it = timestamps_it->second.begin(); it != timestamps_it->second.end();)
        {
            if ((cur_ts.toMillisecond() - it->toMillisecond()) > expired_interval)
            {
                LOG_TRACE(log, "Clean outdated timestamp {}\n", *it);
                timestamp_to_tables.erase(*it);
                it = timestamps_it->second.erase(it);
            }
            else
            {
                ++it;
            }
        }
    }
    catch (const Exception & e)
    {
        LOG_WARNING(log, "Failed to clean outdated timestamps for {} {}\n", storage_id.getNameForLogs(), e.displayText());
    }

    if (timestamps_it->second.empty())
        return std::nullopt;
    return *(timestamps_it->second.begin());
}

void TransactionCoordinatorRcCnch::scanActiveTransactions()
{
    try
    {
        LOG_INFO(log, "Background txn scan task starts...");
        TxnTimestamp cur_ts = getContext()->getTimestamp();
        const UInt64 expired_interval = getContext()->getConfigRef().getInt("cnch_transaction_expire_time", 24 * 60 * 60 * 1000); // default 24h
        std::vector<TransactionCnchPtr> deleted_txn_list;

        {
            std::lock_guard lock(list_mutex);
            for (auto it = active_txn_list.begin(); it != active_txn_list.end();)
            {
                TransactionCnchPtr txn = it->second;
                if ((cur_ts.toMillisecond() - txn->getStartTime().toMillisecond()) > expired_interval)
                {
                    deleted_txn_list.push_back(std::move(txn));
                    it = active_txn_list.erase(it);
                }
                else
                    ++it;
            }
        }

        LOG_INFO(log, "Background txn scanner expires {} transactions\n", deleted_txn_list.size());
        ProfileEvents::increment(ProfileEvents::CnchTxnExpired, deleted_txn_list.size());

        for (const auto & txn : deleted_txn_list)
        {
            eraseActiveTimestamp(txn);
            if (!txn->isReadOnly())
            {
                txn_cleaner->cleanTransaction(txn);
            }
            LOG_WARNING(log, "Expired transaction: {}", txn->getTransactionID().toUInt64());
        }

        LOG_INFO(log, "Background txn scan task finished");
        scan_active_txns_task->scheduleAfter(scan_interval);
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        scan_active_txns_task->scheduleAfter(scan_interval);
    }
}
}
