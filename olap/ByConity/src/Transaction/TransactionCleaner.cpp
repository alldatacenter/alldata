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

#include <Transaction/TransactionCleaner.h>

#include <Catalog/Catalog.h>
#include <Common/serverLocality.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
// #include <MergeTreeCommon/CnchServerClientPool.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/TransactionCommon.h>

namespace DB
{
namespace ErrorCodes
{
    // extern const int BAD_CAST;
    extern const int LOGICAL_ERROR;
}

TransactionCleaner::~TransactionCleaner()
{
    try
    {
        finalize();
    }
    catch(...)
    {
        tryLogCurrentException(log);
    }
}

void TransactionCleaner::cleanTransaction(const TransactionCnchPtr & txn)
{
    auto txn_record = txn->getTransactionRecord();
    if (txn_record.read_only)
        return;

    if (!txn_record.ended())
        txn->abort();

    if (!txn->async_post_commit)
    {
        TxnCleanTask task(txn->getTransactionID(), CleanTaskPriority::HIGH, txn_record.status());
        txn->clean(task);
        return;
    }

    scheduleTask(
        [this, txn] {
            TxnCleanTask & task = getCleanTask(txn->getTransactionID());
            txn->clean(task);
        },
        CleanTaskPriority::HIGH,
        txn_record.txnID(),
        txn_record.status());
}

void TransactionCleaner::cleanTransaction(const TransactionRecord & txn_record)
{
    if (txn_record.status() == CnchTransactionStatus::Finished)
    {
        cleanCommittedTxn(txn_record);
    }
    else
    {
        cleanAbortedTxn(txn_record);
    }
}

void TransactionCleaner::cleanCommittedTxn(const TransactionRecord & txn_record)
{
    scheduleTask([this, txn_record, &global_context = *getContext()] {
        LOG_DEBUG(log, "Start to clean the committed transaction {}\n", txn_record.txnID().toUInt64());
        TxnCleanTask & task = getCleanTask(txn_record.txnID());
        auto catalog = global_context.getCnchCatalog();
        // first clear any filesys lock if hold
        catalog->clearFilesysLock(txn_record.txnID());
        catalog->clearZombieIntent(txn_record.txnID());
        auto undo_buffer = catalog->getUndoBuffer(txn_record.txnID());

        cppkafka::TopicPartitionList undo_tpl;
        String consumer_group;

        for (const auto & [uuid, resources] : undo_buffer)
        {
            LOG_DEBUG(log, "Get undo buffer of the table {}\n", uuid);
            auto host_port = global_context.getCnchTopologyMaster()->getTargetServer(uuid, false);
            auto rpc_address = host_port.getRPCAddress();
            if (!isLocalServer(rpc_address, std::to_string(global_context.getRPCPort())))
            {
                // TODO: need to fix for multi-table txn
                LOG_DEBUG(log, "Forward clean task for txn {} to server {}", txn_record.txnID().toUInt64(), rpc_address);
                // global_context.getCnchServerClientPool().get(rpc_address)->cleanTransaction(txn_record);
                return;
            }
            StoragePtr table = catalog->tryGetTableByUUID(global_context, uuid, TxnTimestamp::maxTS(), true);
            if (!table)
                continue;

            UndoResourceNames names = integrateResources(resources);
            auto intermediate_parts = catalog->getDataPartsByNames(names.parts, table, 0);
            auto undo_bitmaps = catalog->getDeleteBitmapByKeys(table, names.bitmaps);
            auto staged_parts = catalog->getStagedDataPartsByNames(names.staged_parts, table, 0);

            {
                std::lock_guard lock(task.mutex);
                task.undo_size = intermediate_parts.size() + undo_bitmaps.size();
            }

            catalog->setCommitTime(table, Catalog::CommitItems{intermediate_parts, undo_bitmaps, staged_parts}, txn_record.commitTs(), txn_record.txnID());
        }

        catalog->clearUndoBuffer(txn_record.txnID());
        UInt64 ttl = global_context.getConfigRef().getUInt64("cnch_txn_safe_remove_seconds", 5 * 60);
        catalog->setTransactionRecordCleanTime(txn_record, global_context.getTimestamp(), ttl);
        LOG_DEBUG(log, "Finish cleaning the committed transaction {}\n", txn_record.txnID().toUInt64());
    }, CleanTaskPriority::LOW, txn_record.txnID(), CnchTransactionStatus::Finished);
}

void TransactionCleaner::cleanAbortedTxn(const TransactionRecord & txn_record)
{
    scheduleTask([this, txn_record, &global_context = *getContext()]() {
        LOG_DEBUG(log, "Start to clean the aborted transaction {}\n", txn_record.txnID().toUInt64());

        // abort transaction if it is running
        if (!txn_record.ended())
        {
            LOG_DEBUG(log, "Abort the running transaction {}\n", txn_record.txnID().toUInt64());
            if (global_context.getCnchTransactionCoordinator().isActiveTransaction(txn_record.txnID()))
            {
                LOG_WARNING(log, "Transaction {} is still running.\n", txn_record.txnID().toUInt64());
                return;
            }

            auto commit_ts = global_context.getTimestamp();
            TransactionRecord target_record = txn_record;
            target_record.setStatus(CnchTransactionStatus::Aborted);
            target_record.commitTs() = commit_ts;
            bool success = global_context.getCnchCatalog()->setTransactionRecord(txn_record, target_record);
            if (!success)
            {
                LOG_WARNING(log, "Transaction has been committed or aborted, current status " + String(txnStatusToString(target_record.status())));
                return;
            }
        }

        TxnCleanTask & task = getCleanTask(txn_record.txnID());
        auto catalog = global_context.getCnchCatalog();
        catalog->clearZombieIntent(txn_record.txnID());
        auto undo_buffer = catalog->getUndoBuffer(txn_record.txnID());

        cppkafka::TopicPartitionList undo_tpl;
        String consumer_group;
        for (const auto & [uuid, resources] : undo_buffer)
        {
            LOG_DEBUG(log, "Get undo buffer of the table ", uuid);
            StoragePtr table = catalog->tryGetTableByUUID(global_context, uuid, TxnTimestamp::maxTS(), true);
            if (!table)
                continue;

            auto * storage = dynamic_cast<MergeTreeMetaBase *>(table.get());
            if (!storage)
                throw Exception("Table is not of MergeTree class", ErrorCodes::LOGICAL_ERROR);

            UndoResourceNames names = integrateResources(resources);
            auto intermediate_parts = catalog->getDataPartsByNames(names.parts, table, 0);
            auto undo_bitmaps = catalog->getDeleteBitmapByKeys(table, names.bitmaps);
            auto staged_parts = catalog->getStagedDataPartsByNames(names.staged_parts, table, 0);

            {
                std::lock_guard lock(task.mutex);
                task.undo_size = intermediate_parts.size() + undo_bitmaps.size();
            }

            // skip part cache to avoid blocking by write lock of part cache for long time
            catalog->clearParts(table, Catalog::CommitItems{intermediate_parts, undo_bitmaps, staged_parts}, true);

            // clean vfs
            for (const auto & resource : resources)
            {
                resource.clean(*catalog, storage);
            }
        }

        // remove directory lock if there's one, this is tricky, because we don't know the directory name
        // current solution: scan all filesys lock and match the transaction id
        catalog->clearFilesysLock(txn_record.txnID());
        catalog->clearUndoBuffer(txn_record.txnID());
        catalog->removeTransactionRecord(txn_record);
        LOG_DEBUG(log, "Finish cleaning aborted transaction {}\n", txn_record.txnID().toUInt64());
    }, CleanTaskPriority::LOW, txn_record.txnID(), CnchTransactionStatus::Aborted);
}

TxnCleanTask & TransactionCleaner::getCleanTask(const TxnTimestamp & txn_id)
{
    std::lock_guard lock(mutex);
    if (auto it = clean_tasks.find(txn_id.toUInt64()); it != clean_tasks.end())
    {
        return it->second;
    }
    else {
        throw Exception("Clean task was not registered, txn_id: " + txn_id.toString(), ErrorCodes::LOGICAL_ERROR);
    }
}

void TransactionCleaner::finalize()
{
    {
        std::lock_guard lock(mutex);
        shutdown = true;
    }

    server_thread_pool->wait();
    dm_thread_pool->wait();
}

void TransactionCleaner::removeTask(const TxnTimestamp & txn_id)
{
    std::lock_guard lock(mutex);
    clean_tasks.erase(txn_id.toUInt64());
}

}
