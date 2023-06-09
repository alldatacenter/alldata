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

#include <Transaction/CnchServerTransaction.h>

#include <Catalog/Catalog.h>
#include <common/scope_guard.h>
#include <Common/Exception.h>
#include <Common/ProfileEvents.h>
#include <common/logger_useful.h>
#include <IO/WriteBuffer.h>
#include <Transaction/TransactionCommon.h>
#include <Transaction/TransactionCleaner.h>
#include <mutex>

namespace ProfileEvents
{
    extern const int CnchTxnCommitted;
    extern const int CnchTxnAborted;
    extern const int CnchTxnCommitV1Failed;
    extern const int CnchTxnCommitV2Failed;
    extern const int CnchTxnCommitV1ElapsedMilliseconds;
    extern const int CnchTxnCommitV2ElapsedMilliseconds;
    extern const int CnchTxnPrecommitElapsedMilliseconds;
    extern const int CnchTxnCommitKVElapsedMilliseconds;
    extern const int CnchTxnCleanFailed;
    extern const int CnchTxnCleanElapsedMilliseconds;
    extern const int CnchTxnFinishedTransactionRecord;
}

namespace CurrentMetrics
{
    extern const Metric CnchTxnActiveTransactions;
    extern const Metric CnchTxnTransactionRecords;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int CNCH_TRANSACTION_COMMIT_TIMEOUT;
    extern const int CNCH_TRANSACTION_COMMIT_ERROR;
    extern const int CNCH_TRANSACTION_ABORT_ERROR;
    extern const int INSERTION_LABEL_ALREADY_EXISTS;
    extern const int FAILED_TO_PUT_INSERTION_LABEL;
    // extern const int BAD_CAST;
}

CnchServerTransaction::CnchServerTransaction(const ContextPtr & context_, TransactionRecord txn_record_)
    : ICnchTransaction(context_, std::move(txn_record_))
    , active_txn_increment{CurrentMetrics::CnchTxnActiveTransactions}
{
    if (!isReadOnly())
    {
        global_context->getCnchCatalog()->createTransactionRecord(getTransactionRecord());
        CurrentMetrics::add(CurrentMetrics::CnchTxnTransactionRecords);
    }
}

void CnchServerTransaction::appendAction(ActionPtr act)
{
    auto lock = getLock();
    actions.push_back(std::move(act));
}

std::vector<ActionPtr> & CnchServerTransaction::getPendingActions()
{
    auto lock = getLock();
    return actions;
}

TxnTimestamp CnchServerTransaction::commitV1()
{
    LOG_DEBUG(log, "Transaction {} starts commit (v1 api)\n", txn_record.txnID().toUInt64());

    if (isReadOnly())
        throw Exception("Invalid commit operation for read only transaction", ErrorCodes::LOGICAL_ERROR);

    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    SCOPE_EXIT({
        ProfileEvents::increment((getStatus() == CnchTransactionStatus::Finished ? ProfileEvents::CnchTxnCommitted : ProfileEvents::CnchTxnCommitV1Failed));
        ProfileEvents::increment(ProfileEvents::CnchTxnCommitV1ElapsedMilliseconds, watch.elapsedMilliseconds());
    });

    auto commit_ts = global_context->getTimestamp();

    try
    {
        auto lock = getLock();
        for (const auto & action : actions)
        {
            action->executeV1(commit_ts);
        }

        setStatus(CnchTransactionStatus::Finished);
        setCommitTime(commit_ts);
        LOG_DEBUG(log, "Successfully committed transaction (v1 api): {}\n", txn_record.txnID().toUInt64());

        return commit_ts;
    }
    catch (...)
    {
        rollbackV1(commit_ts);
        throw;
    }
}

void CnchServerTransaction::rollbackV1(const TxnTimestamp & ts)
{
    LOG_DEBUG(log, "Transaction {} failed, start rollback (v1 api).\n", txn_record.txnID().toUInt64());
    auto lock = getLock();
    setStatus(CnchTransactionStatus::Aborted);
    setCommitTime(ts);
}

TxnTimestamp CnchServerTransaction::commitV2()
{
    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::CnchTxnCommitV2ElapsedMilliseconds, watch.elapsedMilliseconds()); });

    try
    {
        precommit();
        return commit();
    }
    catch (const Exception & e)
    {
        if (!(getContext()->getSettings().ignore_duplicate_insertion_label && e.code() == ErrorCodes::INSERTION_LABEL_ALREADY_EXISTS))
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        rollback();
        throw;
    }
    catch (...)
    {
        LOG_DEBUG(log, "CommitV2 failed for transaction {}\n", txn_record.txnID());
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        rollback();
        throw;
    }
}

void CnchServerTransaction::precommit()
{
    LOG_DEBUG(log, "Transaction {} starts pre commit\n", txn_record.txnID().toUInt64());
    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::CnchTxnPrecommitElapsedMilliseconds, watch.elapsedMilliseconds()); });

    auto lock = getLock();
    if (auto status = getStatus(); status != CnchTransactionStatus::Running)
        throw Exception("Transaction is not in running status, but in " + String(txnStatusToString(status)), ErrorCodes::LOGICAL_ERROR);

    for (auto & action : actions)
        action->executeV2();

    txn_record.prepared = true;
}

TxnTimestamp CnchServerTransaction::commit()
{
    LOG_DEBUG(log, "Transaction {} starts commit", txn_record.txnID().toUInt64());
    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    auto lock = getLock();
    if (isReadOnly() || !txn_record.isPrepared())
        throw Exception("Invalid commit operation", ErrorCodes::LOGICAL_ERROR);

    TxnTimestamp commit_ts = global_context->getTimestamp();
    int retry = MAX_RETRY;
    do
    {
        try
        {
            if (isPrimary() && !consumer_group.empty()) /// Kafka transaction is always primary
            {
                if (tpl.empty())
                    throw Exception("No tpl found for committing Kafka transaction", ErrorCodes::LOGICAL_ERROR);

                // CAS operation
                TransactionRecord target_record = getTransactionRecord();
                target_record.setStatus(CnchTransactionStatus::Finished)
                             .setCommitTs(commit_ts)
                             .setMainTableUUID(getMainTableUUID());
                Stopwatch stop_watch;
                auto success = global_context->getCnchCatalog()->setTransactionRecordStatusWithOffsets(txn_record, target_record, consumer_group, tpl);

                txn_record = std::move(target_record);
                if (success)
                {
                    ProfileEvents::increment(ProfileEvents::CnchTxnCommitted);
                    ProfileEvents::increment(ProfileEvents::CnchTxnFinishedTransactionRecord);
                    LOG_DEBUG(log, "Successfully committed Kafka transaction {} at {} with {} offsets number, elapsed {} ms",
                                     txn_record.txnID().toUInt64(), commit_ts, tpl.size(), stop_watch.elapsedMilliseconds());
                    return commit_ts;
                }
                else
                {
                    LOG_DEBUG(log, "Failed to commit Kafka transaction: {}, abort it directly", txn_record.txnID().toUInt64());
                    setStatus(CnchTransactionStatus::Aborted);
                    retry = 0;
                    throw Exception(
                        "Kafka transaction " + txn_record.txnID().toString()
                            + " commit failed because txn record has been changed by other transactions",
                        ErrorCodes::CNCH_TRANSACTION_COMMIT_ERROR);
                }
            }
            else
            {
                Catalog::BatchCommitRequest requests(true, true);
                Catalog::BatchCommitResponse response;

                String label_key;
                String label_value;
                if (insertion_label)
                {
                    /// Pack the operation creating a new label into CAS operations
                    insertion_label->commit();
                    label_key = global_context->getCnchCatalog()->getInsertionLabelKey(insertion_label);
                    label_value = insertion_label->serializeValue();
                    Catalog::SinglePutRequest put_req(label_key, label_value, true);
                    put_req.callback = [label = insertion_label](int code, const std::string & msg) {
                        if (code == Catalog::CAS_FAILED)
                            throw Exception(
                                "Insertion label " + label->name + " already exists: " + msg, ErrorCodes::INSERTION_LABEL_ALREADY_EXISTS);
                        else if (code != Catalog::OK)
                            throw Exception(
                                "Failed to put insertion label " + label->name + ": " + msg, ErrorCodes::FAILED_TO_PUT_INSERTION_LABEL);
                    };
                    requests.AddPut(put_req);
                }

                // CAS operation
                TransactionRecord target_record = getTransactionRecord();
                target_record.setStatus(CnchTransactionStatus::Finished)
                             .setCommitTs(commit_ts)
                             .setMainTableUUID(getMainTableUUID());

                bool success = global_context->getCnchCatalog()->setTransactionRecordWithRequests(txn_record, target_record, requests, response);

                txn_record = std::move(target_record);

                if (success)
                {
                    ProfileEvents::increment(ProfileEvents::CnchTxnCommitted);
                    ProfileEvents::increment(ProfileEvents::CnchTxnFinishedTransactionRecord);
                    LOG_DEBUG(log, "Successfully committed transaction {} at {}\n", txn_record.txnID().toUInt64(), commit_ts);
                    //unlock();
                    return commit_ts;
                }
                else // CAS failed
                {
                    // Because of retry logic, txn may has been committed, we treat success for this case.
                    if (txn_record.status() == CnchTransactionStatus::Finished)
                    {
                        ProfileEvents::increment(ProfileEvents::CnchTxnCommitted);
                        ProfileEvents::increment(ProfileEvents::CnchTxnFinishedTransactionRecord);
                        LOG_DEBUG(log, "Transaction {} has been successfully committed in previous trials.\n", txn_record.txnID().toUInt64());
                        return txn_record.commitTs();
                    }
                    else
                    {
                        // Except for above case, treat all other cas failed cases failed.
                        throw Exception("Transaction " + txn_record.txnID().toString() + " commit failed because txn record has been changed by other transactions", ErrorCodes::CNCH_TRANSACTION_COMMIT_ERROR);
                    }
                }
            }
        }
        catch (const Exception &)
        {
            ProfileEvents::increment(ProfileEvents::CnchTxnCommitV2Failed);
            throw;
        }

        LOG_WARNING(log, "Catch bytekv request timeout exception. Will try to update transaction record again, number of retries remains: {}\n", retry);
        // slightly increase waiting time
        std::this_thread::sleep_for(std::chrono::milliseconds(200 * (MAX_RETRY - retry)));
    } while (retry-- > 0);

    throw Exception("Transaction" + txn_record.txnID().toString() + " commit timeout", ErrorCodes::CNCH_TRANSACTION_COMMIT_TIMEOUT);
}

TxnTimestamp CnchServerTransaction::rollback()
{
    LOG_DEBUG(log, "Transaction {} failed, start rollback\n", txn_record.txnID().toUInt64());
    auto lock = getLock();
    if (isReadOnly())
        throw Exception("Invalid commit operation", ErrorCodes::LOGICAL_ERROR);

    removeIntermediateData();

    // Set in memory transaction status first, if rollback kv failed, the in-memory status will be used to reset the conflict.
    setStatus(CnchTransactionStatus::Aborted);
    ProfileEvents::increment(ProfileEvents::CnchTxnAborted);
    TxnTimestamp ts;
    try
    {
        ts = global_context->getTimestamp();
        setCommitTime(ts);
        global_context->getCnchCatalog()->rollbackTransaction(txn_record);
        LOG_DEBUG(log, "Successfully rollback transaction: {}\n", txn_record.txnID().toUInt64());
    }
    catch (...)
    {
        LOG_DEBUG(log, "Failed to rollback transaction: {}", txn_record.txnID().toUInt64());
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    return ts;
}

TxnTimestamp CnchServerTransaction::abort()
{
    LOG_DEBUG(log, "Start abort transaction {}\n", txn_record.txnID().toUInt64());
    auto lock = getLock();
    if (isReadOnly())
        throw Exception("Invalid commit operation", ErrorCodes::LOGICAL_ERROR);

    TransactionRecord target_record = getTransactionRecord();
    target_record.setStatus(CnchTransactionStatus::Aborted)
                 .setCommitTs(global_context->getTimestamp())
                 .setMainTableUUID(getMainTableUUID());

    bool success = global_context->getCnchCatalog()->setTransactionRecord(txn_record, target_record);
    txn_record = std::move(target_record);

    if (success)
    {
        ProfileEvents::increment(ProfileEvents::CnchTxnAborted);
        LOG_DEBUG(log, "Successfully abort transaction: {}\n", txn_record.txnID().toUInt64());
    }
    else // CAS failed
    {
        // Don't abort committed txn, treat committed
        if (txn_record.status() == CnchTransactionStatus::Finished)
        {
            LOG_WARNING(log, "Transaction {} has been committed\n", txn_record.txnID().toUInt64());
        }
        else if (txn_record.status() == CnchTransactionStatus::Aborted)
        {
            LOG_WARNING(log, "Transaction {} has been committed\n", txn_record.txnID().toUInt64());
        }
        else
        {
            // Abort failed, throw exception
            throw Exception("Abort transaction " + txn_record.txnID().toString() + " failed (CAS failure).", ErrorCodes::CNCH_TRANSACTION_ABORT_ERROR);
        }
    }

    return txn_record.commitTs();
}

void CnchServerTransaction::clean(TxnCleanTask & task)
{
    if (force_clean_by_dm)
    {
        LOG_DEBUG(log, "Force clean transaction {} from DM", task.txn_id.toUInt64());
        return;
    }
    LOG_DEBUG(log, "Start clean transaction: {}\n", task.txn_id.toUInt64());
    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    SCOPE_EXIT({ ProfileEvents::increment(ProfileEvents::CnchTxnCleanElapsedMilliseconds, watch.elapsedMilliseconds()); });

    try
    {
        // operations are done in sequence, if any operation failed, the remaining will not continue. The background scan thread will finish the remaining job.
        auto lock = getLock();
        auto catalog = global_context->getCnchCatalog();
        auto txn_id = getTransactionID();
        // releaseIntentLocks();

        if (getStatus() == CnchTransactionStatus::Finished)
        {
            // first clear any filesys lock if hold
            catalog->clearFilesysLock(txn_id);
            UInt32 undo_size = 0;
            for (auto & action : actions)
            {
                undo_size += action->getSize();
            }

            {
                std::lock_guard lk(task.mutex);
                task.undo_size = undo_size;
            }

            for (auto & action : actions)
                action->postCommit(getCommitTime());

            catalog->clearUndoBuffer(txn_id);
            /// set clean time in kv, txn_record will be remove by daemon manager
            UInt64 ttl = global_context->getConfigRef().getUInt64("cnch_txn_safe_remove_seconds", 5 * 60);
            catalog->setTransactionRecordCleanTime(txn_record, global_context->getTimestamp(), ttl);
            // TODO: move to dm when metrics ready
            CurrentMetrics::sub(CurrentMetrics::CnchTxnTransactionRecords);
            LOG_DEBUG(log, "Successfully clean a finished transaction: {}\n", txn_record.txnID().toUInt64());
        }
        else
        {
            // clear intermediate parts first since metadata is the source
            for (auto & action : actions)
                action->abort();

            auto undo_buffer = catalog->getUndoBuffer(txn_id);
            UInt32 undo_size = 0;
            for (const auto & buffer : undo_buffer)
                undo_size += buffer.second.size();

            {
                std::lock_guard lk(task.mutex);
                task.undo_size = undo_size;
            }

            for (const auto & [uuid, resources] : undo_buffer)
            {
                StoragePtr table = catalog->getTableByUUID(*getContext(), uuid, txn_id, true);
                auto * storage = dynamic_cast<MergeTreeMetaBase *>(table.get());
                if (!storage)
                    throw Exception("Table is not of MergeTree class", ErrorCodes::LOGICAL_ERROR);

                for (const auto & resource : resources)
                    resource.clean(*catalog, storage);
            }

            catalog->clearUndoBuffer(txn_id);

            /// to this point, can safely clear any lock if hold
            catalog->clearFilesysLock(txn_id);

            // remove transaction record, if remove transaction record is not done, will still be handled by background scan thread.
            catalog->removeTransactionRecord(txn_record);
            CurrentMetrics::sub(CurrentMetrics::CnchTxnTransactionRecords);
            LOG_DEBUG(log, "Successfully clean a failed transaction: {}\n", txn_record.txnID().toUInt64());
        }
    }
    catch (...)
    {
        ProfileEvents::increment(ProfileEvents::CnchTxnCleanFailed);
        LOG_WARNING(log, "Clean txn {" + txn_record.toString() + "} failed.");
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void CnchServerTransaction::removeIntermediateData()
{
    /// for seconday transaction, if commit fails, must clear all written data in kv imediately.
    /// otherwise, next dml within the transaction can see the trash data, or worse, if user try
    /// to commit the transaction again, the junk data will become visible.
    if (isPrimary()) return;
    LOG_DEBUG(log, "Secondary transaction failed, will remove all intermediate data during rollback");
    std::for_each(actions.begin(), actions.end(), [](auto & action) { action->abort(); });
    actions.clear();
}
}
