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

#include <Transaction/TransactionCommon.h>
#include <Catalog/Catalog.h>
#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Storages/MergeTree/DeleteBitmapMeta.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>

#include <vector>
#include <unordered_map>

namespace DB
{

struct TransactionRecordLite
{
    UInt64 commit_ts;
    CnchTransactionStatus status;
    TransactionRecordLite() = default;
    TransactionRecordLite(UInt64 commit_ts_, CnchTransactionStatus status_) : commit_ts(commit_ts_), status(status_) {}
};

template <typename T, typename Operation>
bool isCommitted(const T & element, std::unordered_map<UInt64, TransactionRecordLite> & transactions)
{
    UInt64 commit_time = Operation::getCommitTime(element);
    UInt64 txn_id = Operation::getTxnID(element);

    if (commit_time != IMergeTreeDataPart::NOT_INITIALIZED_COMMIT_TIME)
    {
        return true;
    }
    else
    {
        const auto & txn_record = transactions[txn_id];
        // for non finished txn, the commit time is still NOT_INITIALIZED_COMMIT_TIME (0) after setCommitTime is called.
        Operation::setCommitTime(element, txn_record.commit_ts);
        return txn_record.status == CnchTransactionStatus::Finished;
    }
}


/// Give a vector whose elements might be in the intermediate state (commit time == 0).
/// Convert elements in the intermediate state into the final state (set commit time for them) if the transaction has been committed
/// and remove intermediate state elements if its corresponding transaction is running.
/// The elements with commit time larger than ts will be ignored.
template <typename T, typename Operation>
void getCommitted(std::vector<T> & elements, const TxnTimestamp & ts, Catalog::Catalog * catalog)
{
    std::unordered_map<UInt64, TransactionRecordLite> transactions; // cache for fetched transactions
    std::set<TxnTimestamp> txn_ids;
    for (const auto & element: elements)
    {
        UInt64 commit_time = Operation::getCommitTime(element);
        UInt64 txn_id = Operation::getTxnID(element);

        if (commit_time != IMergeTreeDataPart::NOT_INITIALIZED_COMMIT_TIME)
        {
            transactions.try_emplace(txn_id, commit_time, CnchTransactionStatus::Finished);
        }
        else
        {
            transactions.try_emplace(txn_id, 0, CnchTransactionStatus::Inactive);
            txn_ids.insert(txn_id);
        }
    }
    // get txn records status in batch
    auto records = catalog->getTransactionRecords(std::vector<TxnTimestamp>(txn_ids.begin(), txn_ids.end()), 100000);
    for (const auto & record : records)
    {
        if (record.status() == CnchTransactionStatus::Finished)
        {
            transactions[record.txnID()] = {record.commitTs(), record.status()};
        }
    }

    elements.erase(
        std::remove_if(
            elements.begin(),
            elements.end(),
            [&](const T & element) {
                return !isCommitted<T, Operation>(element, transactions)
                    || transactions[Operation::getTxnID(element)].commit_ts > ts;
            }),
        elements.end());
}


template <typename T, typename Operation>
bool isAborted(const T & element, Catalog::Catalog * catalog, std::unordered_map<UInt64, TransactionRecordLite> & transactions)
{
    UInt64 commit_time = Operation::getCommitTime(element);
    UInt64 txn_id = Operation::getTxnID(element);

    if (commit_time != IMergeTreeDataPart::NOT_INITIALIZED_COMMIT_TIME)
    {
        transactions.try_emplace(txn_id, commit_time, CnchTransactionStatus::Finished);
        return false;
    }

    // If the txn not exists, need to check from kv to know its status.
    // If the txn exists in this map but with CnchTransactionStatus::Inactive status, it means the txn not exist.
    if (transactions.find(txn_id) == transactions.end())
    {
        auto record = catalog->tryGetTransactionRecord(TxnTimestamp(txn_id));
        if (!record)
            transactions.try_emplace(txn_id, 0, CnchTransactionStatus::Inactive);
        else
            transactions.try_emplace(txn_id, record->commitTs(), record->status());
    }

    auto & txn_record = transactions[txn_id];
    return txn_record.status == CnchTransactionStatus::Aborted || txn_record.status == CnchTransactionStatus::Inactive;
}

struct DataPartOperation
{
    static UInt64 getCommitTime(const MergeTreeDataPartCNCHPtr & part)
    {
        return part->commit_time;
    }

    static UInt64 getTxnID(const MergeTreeDataPartCNCHPtr & part)
    {
        return part->info.mutation;
    }

    static void setCommitTime(const MergeTreeDataPartCNCHPtr & part, const TxnTimestamp & ts)
    {
        part->commit_time = ts;
    }
};

struct ServerDataPartOperation
{
    static UInt64 getCommitTime(const ServerDataPartPtr & server_part_ptr)
    {
        return server_part_ptr->getCommitTime();
    }

    static UInt64 getTxnID(const ServerDataPartPtr & server_part_ptr)
    {
        return server_part_ptr->part_model_wrapper->part_model->part_info().mutation();
    }

    static void setCommitTime(const ServerDataPartPtr & server_part_ptr, const TxnTimestamp & ts)
    {
        server_part_ptr->setCommitTime(ts);
    }
};

struct BitmapOperation
{
    static UInt64 getCommitTime(const DeleteBitmapMetaPtr & bitmap)
    {
        return bitmap->getCommitTime();
    }

    static UInt64 getTxnID(const DeleteBitmapMetaPtr & bitmap)
    {
        return bitmap->getTxnId();
    }

    static void setCommitTime(const DeleteBitmapMetaPtr & bitmap, const TxnTimestamp & ts)
    {
        bitmap->updateCommitTime(ts);
    }
};

constexpr auto getCommittedDataParts = getCommitted<MergeTreeDataPartCNCHPtr, DataPartOperation>;
constexpr auto getCommittedServerDataParts = getCommitted<ServerDataPartPtr, ServerDataPartOperation>;
constexpr auto isCommittedDataPart = isCommitted<MergeTreeDataPartCNCHPtr, DataPartOperation>;
constexpr auto isAbortedDataPart = isAborted<MergeTreeDataPartCNCHPtr, DataPartOperation>;
constexpr auto isAbortedServerDataPart = isAborted<ServerDataPartPtr, ServerDataPartOperation>;
constexpr auto getCommittedBitmaps = getCommitted<DeleteBitmapMetaPtr, BitmapOperation>;

}
