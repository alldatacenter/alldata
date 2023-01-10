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

#include <Transaction/CnchWorkerTransaction.h>

#include <Transaction/ICnchTransaction.h>
#include <Transaction/TransactionCommon.h>
#include <Catalog/Catalog.h>
#include <Transaction/TxnTimestamp.h>

namespace DB
{
namespace ErrorCodes
{
    // extern const int BAD_CAST;
    extern const int LOGICAL_ERROR;
    extern const int CNCH_TRANSACTION_ABORTED;
    extern const int CNCH_TRANSACTION_PRECOMMIT_ERROR;
    extern const int BRPC_TIMEOUT;
}

CnchWorkerTransaction::CnchWorkerTransaction(const ContextPtr & context_, CnchServerClientPtr client)
    : ICnchTransaction(context_), server_client(std::move(client))
{
    checkServerClient();
    auto [start_ts, txn_id] = server_client->createTransaction();
    TransactionRecord record;
    record.setID(txn_id).setInitiator(txnInitiatorToString(CnchTransactionInitiator::Worker)).setStatus(CnchTransactionStatus::Running);
    setTransactionRecord(std::move(record));
    is_initiator = true;
}

CnchWorkerTransaction::CnchWorkerTransaction(
    const ContextPtr & context_, CnchServerClientPtr client, StorageID kafka_table_id_, size_t consumer_index_)
    : ICnchTransaction(context_), server_client(std::move(client)),
    kafka_table_id(std::move(kafka_table_id_)), kafka_consumer_index(consumer_index_)
{
    checkServerClient();
    auto [start_ts, txn_id] = server_client->createTransactionForKafka(kafka_table_id, kafka_consumer_index);
    TransactionRecord record;
    record.setID(txn_id).setStatus(CnchTransactionStatus::Running).setInitiator(txnInitiatorToString(CnchTransactionInitiator::Kafka));
    setTransactionRecord(std::move(record));
    is_initiator = true;
}

CnchWorkerTransaction::CnchWorkerTransaction(const ContextPtr & context_, const TxnTimestamp & txn_id, const TxnTimestamp & primary_txn_id)
    : ICnchTransaction(context_)
{
    String initiator = txnInitiatorToString(CnchTransactionInitiator::Server);
    TransactionRecord record;
    record.read_only = true;
    record.setID(txn_id).setPrimaryID(primary_txn_id).setInitiator(initiator).setStatus(CnchTransactionStatus::Running);
    setTransactionRecord(std::move(record));
}

CnchWorkerTransaction::CnchWorkerTransaction(const ContextPtr & context_, StorageID kafka_table_id_)
    : ICnchTransaction(context_), kafka_table_id(std::move(kafka_table_id_)) {}

CnchWorkerTransaction::~CnchWorkerTransaction()
{
    try
    {
        if (is_initiator && getTransactionID())
        {
            checkServerClient();
            server_client->finishTransaction(getTransactionID());
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void CnchWorkerTransaction::checkServerClient() const
{
    if (!server_client)
        throw Exception("Server client is unset", ErrorCodes::LOGICAL_ERROR);
}

CnchServerClientPtr CnchWorkerTransaction::getServerClient() const
{
    checkServerClient();
    return server_client;
}

void CnchWorkerTransaction::setServerClient(CnchServerClientPtr client)
{
    server_client = std::move(client);
}

void CnchWorkerTransaction::precommit()
{
    LOG_DEBUG(log, "Transaction {} starts pre commit\n", txn_record.txnID().toUInt64());
    if (auto status = getStatus(); status != CnchTransactionStatus::Running)
        throw Exception("Cannot precommit a transaction that is " + String(txnStatusToString(status)), ErrorCodes::LOGICAL_ERROR);
    checkServerClient();
    auto lock = getLock();
    server_client->precommitTransaction(getTransactionID(), getMainTableUUID());
    txn_record.prepared = true;
    LOG_DEBUG(log, "Transaction {} successfully finished pre commit.");
}

TxnTimestamp CnchWorkerTransaction::commit()
{
    LOG_DEBUG(log, "Transaction {} starts commit\n", txn_record.txnID().toUInt64());
    if (!txn_record.isPrepared())
        throw Exception("Cannot commit a transaction that is not prepared", ErrorCodes::LOGICAL_ERROR);
    checkServerClient();

    auto lock = getLock();
    TxnTimestamp commit_ts;
    /// Check `consumer_index` here
    if (kafka_consumer_index == SIZE_MAX)
        commit_ts = server_client->commitTransaction(*this);
    else
        commit_ts = server_client->commitTransaction(*this, kafka_table_id, kafka_consumer_index);

    setCommitTime(commit_ts);
    setStatus(CnchTransactionStatus::Finished);
    LOG_DEBUG(log, "Successfully committed transaction {} with ts {}\n", txn_record.txnID().toUInt64(), commit_ts.toString());

    return commit_ts;
}

TxnTimestamp CnchWorkerTransaction::rollback()
{
    LOG_DEBUG(log, "Transaction {} failed, start rollback.");
    checkServerClient();
    auto lock = getLock();

    // Set in memory transaction status first, if rollback kv failed, the in-memory status will be used to reset the conflict.
    setStatus(CnchTransactionStatus::Aborted);
    TxnTimestamp ts;
    try
    {
        ts = server_client->rollbackTransaction(getTransactionID());
        setCommitTime(ts);
        LOG_DEBUG(log, "Successfully rollback transaction: {}\n", txn_record.txnID().toUInt64());
    }
    catch (...)
    {
        LOG_DEBUG(log, "Failed to rollback transaction: {}\n", txn_record.txnID().toUInt64());
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }

    return ts;
}

TxnTimestamp CnchWorkerTransaction::commitV2()
{
    auto lock = getLock();
    try
    {
        precommit();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
        rollback();
        throw Exception(
            "Transaction " + txn_record.txnID().toString() + " pre-commit failed due to: " + getCurrentExceptionMessage(false),
            ErrorCodes::CNCH_TRANSACTION_PRECOMMIT_ERROR);
    }

    try
    {
        return commit();
    }
    catch (Exception & e)
    {
        if (e.code() == ErrorCodes::BRPC_TIMEOUT)
        {
            // TODO: check in catalog
            TxnTimestamp commit_ts = global_context.getTimestamp();
            TransactionRecord prev_record;
            bool success = false;
            try
            {
                TransactionRecord target_record = getTransactionRecord();
                target_record.setStatus(CnchTransactionStatus::Aborted).setCommitTs(commit_ts).setMainTableUUID(getMainTableUUID());

                success = global_context.getCnchCatalog()->setTransactionRecord(txn_record, target_record);
                txn_record = std::move(target_record);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
                e.rethrow();
            }

            if (success)
            {
                throw Exception("Server client times out, and transaction " + txn_record.txnID().toString() + " has been aborted", ErrorCodes::CNCH_TRANSACTION_ABORTED);
            }
            else
            {
                if (txn_record.status() == CnchTransactionStatus::Finished)
                {
                    LOG_DEBUG(log, "Server client times out, but txn has been committed successfully");
                    return txn_record.commitTs();
                }
                else if (txn_record.status() == CnchTransactionStatus::Aborted)
                {
                    LOG_DEBUG(log, "Transaction has been aborted");
                    throw Exception("Commit failed, transaction has been aborted", ErrorCodes::CNCH_TRANSACTION_ABORTED);
                }
                else
                {
                    LOG_WARNING(log, "Server client times out, and abort also failed because of record has been cleaned.");
                    throw;
                }
            }
        }
        else
        {
            LOG_DEBUG(log, "Transaction {} commit failed\n", txn_record.txnID().toUInt64());
            tryLogCurrentException(log, __PRETTY_FUNCTION__);
            rollback();
            throw;
        }
    }
    catch (...)
    {
        rollback();
        throw;
    }
}

}
