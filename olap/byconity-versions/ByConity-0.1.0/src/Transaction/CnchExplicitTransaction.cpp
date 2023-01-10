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
#include <Transaction/CnchExplicitTransaction.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Transaction/TxnTimestamp.h>
#include <common/logger_useful.h>
#include <Common/Exception.h>

namespace ProfileEvents
{
extern const int CnchTxnCommitted;
extern const int CnchTxnAborted;
extern const int CnchTxnCommitV2Failed;
extern const int CnchTxnFinishedTransactionRecord;
}
namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int CNCH_TRANSACTION_COMMIT_TIMEOUT;
    extern const int CNCH_TRANSACTION_COMMIT_ERROR;
    extern const int CNCH_TRANSACTION_ABORT_ERROR;
    extern const int CNCH_TRANSACTION_INTERNAL_ERROR;
}

CnchExplicitTransaction::CnchExplicitTransaction(const ContextPtr & context_, TransactionRecord record) : Base(context_, std::move(record))
{
    /// Create txn record
    getContext()->getCnchCatalog()->createTransactionRecord(getTransactionRecord());
}

void CnchExplicitTransaction::precommit()
{
    /// Sync all transaction record; throw exception if status missmatch
    std::for_each(secondary_txns.begin(), secondary_txns.end(), [](auto & txn) {
        if (auto proxy_txn = txn->template as<CnchProxyTransaction>())
            proxy_txn->syncTransactionStatus(/*throw_on_missmatch=*/true);
    });
    /// If a transaction fails, make sure that all parts metadata in bytekv of that txn is removed before commit
    std::for_each(secondary_txns.begin(), secondary_txns.end(), [](auto & txn) {
        if (txn->getStatus() == CnchTransactionStatus::Aborted)
            txn->removeIntermediateData();
    });
    txn_record.prepared = true;
}

TxnTimestamp CnchExplicitTransaction::commit()
{
    /// Set transaction record status to FINISHED
    if (isReadOnly() || !txn_record.isPrepared())
        throw Exception("Invalid commit operation", ErrorCodes::LOGICAL_ERROR);
    LOG_DEBUG(log, "Explicit transaction {} starts commit.\n", txn_record.txnID().toUInt64());
    Stopwatch watch(CLOCK_MONOTONIC_COARSE);
    auto lock = getLock();

    TxnTimestamp commit_ts = getContext()->getTimestamp();
    int retry = MAX_RETRY;
    do
    {
        try
        {
            /// Memory table is not supported in interactive transaction session
            // CAS operation
            TransactionRecord target_record = getTransactionRecord();
                target_record.setStatus(CnchTransactionStatus::Finished)
                             .setCommitTs(commit_ts)
                             .setMainTableUUID(getMainTableUUID());

            bool success = getContext()->getCnchCatalog()->setTransactionRecord(txn_record, target_record);

            txn_record = std::move(target_record);

            if (success)
            {
                ProfileEvents::increment(ProfileEvents::CnchTxnCommitted);
                ProfileEvents::increment(ProfileEvents::CnchTxnFinishedTransactionRecord);
                LOG_DEBUG(log, "Successfully committed explicit transaction: {}\n", txn_record.txnID().toUInt64());
                return commit_ts;
            }
            else // CAS failed
            {
                if (txn_record.status() == CnchTransactionStatus::Finished)
                {
                    ProfileEvents::increment(ProfileEvents::CnchTxnCommitted);
                    ProfileEvents::increment(ProfileEvents::CnchTxnFinishedTransactionRecord);
                    LOG_DEBUG(log, "Explicit transaction {} has been successfully committed in previous trials.\n", txn_record.txnID().toUInt64());
                    return txn_record.commitTs();
                }
                else
                {
                    // Except for above case, treat all other cas failed cases failed.
                    throw Exception(
                        "Explicit transaction " + txn_record.txnID().toString()
                            + " commit failed because txn record has been changed by other transactions",
                        ErrorCodes::CNCH_TRANSACTION_COMMIT_ERROR);
                }
            }
        }
        catch (const Exception & e)
        {
            // if (e.code() != bytekv::sdk::Errorcode::REQUEST_TIMEOUT || !retry)
            // {
            //     ProfileEvents::increment(ProfileEvents::CnchTxnCommitV2Failed);
            //     throw;
            // }
            throw e;
        }

        LOG_WARNING(
            log,
            "Catch bytekv request timeout exception. Will try to update transaction record again, number of retries remains: {}\n", retry);
        // slightly increase waiting time
        std::this_thread::sleep_for(std::chrono::milliseconds(200 * (MAX_RETRY - retry)));
    } while (retry-- > 0);

    throw Exception("Explicit transaction" + txn_record.txnID().toString() + " commit timeout", ErrorCodes::CNCH_TRANSACTION_COMMIT_TIMEOUT);
}

TxnTimestamp CnchExplicitTransaction::commitV2()
{
    try
    {
        precommit();
        return commit();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        rollback();
        throw;
    }
}

TxnTimestamp CnchExplicitTransaction::rollback()
{
    /// TODO: set the transacation record status to aborted
    LOG_DEBUG(log, "Explicit transaction {} failed, start rollback\n", txn_record.txnID().toUInt64());
    /// First, abort all secondary transaction
    /// TODO: exception handling
    std::for_each(secondary_txns.begin(), secondary_txns.end(), [this](auto & txn)
    {
        LOG_TRACE(log, "Aborting secondary transaction {}\n", txn->getTransactionID().toUInt64());
        txn->abort();
    });
    auto lock = getLock();

    // Set in memory transaction status first, if rollback kv failed, the in-memory status will be used to reset the conflict.
    setStatus(CnchTransactionStatus::Aborted);
    TxnTimestamp ts;
    try
    {
        ts = getContext()->getTimestamp();
        setCommitTime(ts);
        getContext()->getCnchCatalog()->rollbackTransaction(txn_record);
        LOG_DEBUG(log, "Successfully rollback explicit transaction: {}\n", txn_record.txnID().toUInt64());
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    return ts;
}

TxnTimestamp CnchExplicitTransaction::abort()
{
    /// Try to abort transaction - not neccessary success
    LOG_DEBUG(log, "Abort explicit transaction {}\n", txn_record.txnID().toUInt64());
    auto lock = getLock();
    /// First, abort all secondary transaction
    std::for_each(secondary_txns.begin(), secondary_txns.end(), [this](auto & txn)
    {
        try
        {
            LOG_TRACE(log, "Aborting secondary transaction {}\n", txn->getTransactionID().toUInt64());
            txn->abort();
        }
        catch (...)
        {
            LOG_WARNING(log, "Aborting secondary transaction {} failed\n", txn->getTransactionID().toUInt64());
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
        }
    });

    /// CAS
    TransactionRecord target_record = getTransactionRecord();
    target_record.setStatus(CnchTransactionStatus::Aborted);
    target_record.setCommitTs(getContext()->getTimestamp());

    bool success = getContext()->getCnchCatalog()->setTransactionRecord(txn_record, target_record);
    txn_record = std::move(target_record);
    if (success)
    {
        /// TODO: metric
    }
    else
    {
        // Don't abort committed txn, treat committed
        if (txn_record.status() == CnchTransactionStatus::Finished)
        {
            LOG_WARNING(log, "Explicit transaction has been committed");
        }
        else if (txn_record.status() == CnchTransactionStatus::Aborted)
        {
            LOG_WARNING(log, "Transaction has been aborted");
        }
        else
        {
            // Abort failed, throw exception
            throw Exception(
                "Abort transaction " + txn_record.txnID().toString() + " failed (CAS failure).", ErrorCodes::CNCH_TRANSACTION_ABORT_ERROR);
        }
    }
    return txn_record.commitTs();
}

void CnchExplicitTransaction::clean(TxnCleanTask &)
{
    auto & coordinator = getContext()->getCnchTransactionCoordinator();
    /// clean finish secondary transactions
    std::for_each(secondary_txns.begin(), secondary_txns.end(), [&coordinator](auto & txn) { coordinator.finishTransaction(txn, true); });
    /// Explicit transaction do not hold any intermediate data, so only need to clean transaction record
    if (txn_record.status() == CnchTransactionStatus::Finished)
    {
        UInt64 ttl = getContext()->getConfigRef().getUInt64("cnch_txn_safe_remove_seconds", 5 * 60);
        getContext()->getCnchCatalog()->setTransactionRecordCleanTime(txn_record, getContext()->getTimestamp(), ttl);
    }
    else
    {
        getContext()->getCnchCatalog()->removeTransactionRecord(getTransactionRecord());
    }
}

bool CnchExplicitTransaction::addSecondaryTransaction(const TransactionCnchPtr & txn)
{
    auto lock = getLock();
    secondary_txns.push_back(txn);
    return true;
}

bool CnchExplicitTransaction::addStatement(const String & statement)
{
    auto lock = getLock();
    statements.push_back(statement);
    return true;
}

}
